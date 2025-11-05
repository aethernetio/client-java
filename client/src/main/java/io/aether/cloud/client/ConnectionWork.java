package io.aether.cloud.client;

import io.aether.api.clientserverapi.*;
import io.aether.api.common.*;
import io.aether.crypto.CryptoEngine;
import io.aether.logger.Log;
import io.aether.net.fastMeta.FastApiContext;
import io.aether.utils.RU;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.tuples.Tuple2;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ConnectionWork extends Connection<ClientApiUnsafe, LoginApiRemote> implements ClientApiUnsafe {
    public final AtomicLong lastBackPing = new AtomicLong(Long.MAX_VALUE);
    final ClientApiSafe apiSafe;
    final FastApiContext apiSafeCtx;
    final CryptoEngine cryptoEngine;
    private final ServerDescriptor serverDescriptor;
    final private AtomicBoolean inProcess = new AtomicBoolean();
    boolean basicStatus;
    long lastWorkTime;
    volatile boolean firstAuth;

    public ConnectionWork(AetherCloudClient client, ServerDescriptor s) {
        super(client, s.getIpAddress().getURI(AetherCodec.TCP), ClientApiUnsafe.META, LoginApi.META);
        this.apiSafe = new MyClientApiSafe(client, this); // Передаем 'this'
        cryptoEngine = client.getCryptoEngineForServer(s.getId());
        serverDescriptor = s;
        this.basicStatus = false;
        apiSafeCtx = new FastApiContext() {
            @Override
            public void flush(AFuture sendFuture) {
                if (fastMetaClient == null || !fastMetaClient.isWritable()) {
                    sendFuture.cancel();
                    return;
                }

                boolean hasWork = true;

                if (!hasWork) {
                    sendFuture.done();
                    return;
                }
                getRootApiFuture().to(api -> {
                    ConnectionWork.this.flushBackgroundRequests(makeRemote(AuthorizedApi.META), sendFuture);
                    var d = remoteDataToArray();
                    if (d.length == 0) {
                        sendFuture.done();
                        return;
                    }
                    var loginStream = new LoginStream(cryptoEngine::encrypt, d);
                    api.loginByAlias(client.getAlias(), loginStream);
                    rootApi.flush(sendFuture);
                }, sendFuture::error).onCancel(sendFuture::cancel);

            }
        };
    }

    /**
     * This method is added as a permanent task to 'remoteApiFuture' and is called
     * during the 'apiSafeCtx.flush()' process. It collects all pending batched
     * requests and sends them to the server.
     */
    private void flushBackgroundRequests(AuthorizedApi a, AFuture sendFuture) {
        UUID[] requestCloud = client.clouds.getRequestsFor(UUID.class, this);
        if (requestCloud.length > 0) {
            a.resolverClouds(requestCloud);
        }

        Integer[] requestServers = client.servers.getRequestsFor(Integer.class, this);
        if (requestServers.length > 0) {
            // Преобразование Integer[] в short[] для API
            short[] serverIds = new short[requestServers.length];
            for (int i = 0; i < requestServers.length; i++) {
                serverIds[i] = requestServers[i].shortValue();
            }
            a.resolverServers(serverIds);
        }

        // ClientGroups (NEW)
        UUID[] requestClientGroups = client.clientGroups.getRequestsFor(UUID.class, this);
        if (requestClientGroups.length > 0) {
            a.requestAccessGroupsForClients(requestClientGroups);
        }

        // AccessGroups (NEW)
        Long[] requestAccessGroups = client.accessGroups.getRequestsFor(Long.class, this);
        if (requestAccessGroups.length > 0) {
            long[] groupIds = new long[requestAccessGroups.length];
            for (int i = 0; i < requestAccessGroups.length; i++) {
                groupIds[i] = requestAccessGroups[i];
            }
            a.requestAccessGroupsItems(groupIds);
        }

        // AllAccessedClients (NEW)
        UUID[] requestAllAccessed = client.allAccessedClients.getRequestsFor(UUID.class, this);
        if (requestAllAccessed.length > 0) {
            a.requestAllAccessedClients(requestAllAccessed);
        }

        // AccessCheckCache (NEW)
        AccessCheckPair[] requestAccessCheck = client.accessCheckCache.getRequestsFor(AccessCheckPair.class, this);
        if (requestAccessCheck.length > 0) {
            a.requestAccessCheck(requestAccessCheck);
        }

        // === 2. Mutation Requests (Access Group Add/Remove) ===

        // Add Operations
        for (Map.Entry<Long, Map<UUID, ARFuture<Boolean>>> entry : client.accessOperationsAdd.entrySet()) {
            long groupId = entry.getKey();
            UUID[] uidsToAdd = entry.getValue().keySet().toArray(new UUID[0]);
            if (uidsToAdd.length > 0) {
                Log.debug("Flushing ADD request for group $gid: $uids", "gid", groupId, "uids", uidsToAdd);
                a.addItemsToAccessGroup(groupId, uidsToAdd);
                // Мы не удаляем фьючерсы отсюда, мы ждем ответа от MyClientApiSafe
            }
        }

        // Remove Operations
        for (Map.Entry<Long, Map<UUID, ARFuture<Boolean>>> entry : client.accessOperationsRemove.entrySet()) {
            long groupId = entry.getKey();
            UUID[] uidsToRemove = entry.getValue().keySet().toArray(new UUID[0]);
            if (uidsToRemove.length > 0) {
                Log.debug("Flushing REMOVE request for group $gid: $uids", "gid", groupId, "uids", uidsToRemove);
                a.removeItemsFromAccessGroup(groupId, uidsToRemove);
            }
        }

        while (true) {
            var t = client.authTasks.poll();
            if (t == null) break;
            t.accept(a);
        }
        AetherCloudClient.ClientTask task;
        while ((task = client.clientTasks.poll()) != null) {
            a.client(task.uid, new ClientApiStream(apiSafeCtx, task.task::accept));
        }

        // === 4. Message Stream Logic (unchanged) ===
        List<Message> messageForSend = null;
        for (var m : client.messageNodeMap.values()) {
            if (m.connectionsOut.contains(this)) {
                List<Tuple2<byte[], AFuture>> mm = new ArrayList<>();
                RU.readAll(m.bufferOut, mm::add);
                if (!mm.isEmpty()) {
                    Log.debug("message send client to server: $uidFrom -> $uidTo",
                            "uidFrom", client.getUid(),
                            "uidTo", m.consumer);
                    if (messageForSend == null) {
                        messageForSend = new ObjectArrayList<>();
                    }
                    Flow.flow(mm)
                            .map(v -> new Message(m.consumer, v.val1()))
                            .toCollection(messageForSend);
                    sendFuture.to(() -> {
                        for (var v : mm) {
                            v.val2().done();
                        }
                    });
                    sendFuture.onCancel(() -> {
                        m.bufferOut.addAll(mm);
                    });
                }
            }
        }
        if (messageForSend != null && !messageForSend.isEmpty()) {
            a.sendMessages(messageForSend.toArray(new Message[0]));
        }

        // 4. Ping Logic (ADDED: complete the ready future)
        if (!firstAuth) {
            firstAuth = true;
            a.ping(0).to(() -> {
                Log.debug("First ping response received. Marking connection ready.");
            }).onError(e -> {
                Log.warn("First ping failed, will retry.", e);
                firstAuth = false;
            });
        }
    }

    @Override
    public void sendSafeApiDataMulti(byte backId, LoginClientStream data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendSafeApiData(LoginClientStream data) {
        data.accept(apiSafeCtx, cryptoEngine::decrypt, apiSafe);
    }

    public ServerDescriptor getServerDescriptor() {
        return serverDescriptor;
    }

    @Override
    public String toString() {
        return "work(" + serverDescriptor.getIpAddress().getURI(AetherCodec.TCP) + ")";
    }

    public void setBasic(boolean basic) {
        this.basicStatus = basic;
    }

    public long lifeTime() {
        return RU.time() - lastBackPing.get();
    }

    public void scheduledWork() {
        var t = RU.time();
        if ((t - lastWorkTime < client.getPingTime() || !inProcess.compareAndSet(false, true))) return;
        lastWorkTime = t;
        var f = AFuture.make();
        f.addListener(v -> inProcess.set(false));
        f.timeout(2, () -> {
            Log.warn("connection work flush 1 timeout");
        });
        apiSafeCtx.flush(f);
    }

    public void flush() {
        if (!inProcess.compareAndSet(false, true)) return;
        lastWorkTime = RU.time();
        var f = AFuture.make();
        f.addListener(v -> inProcess.set(false));
        f.timeout(2, () -> {
            Log.warn("connection work flush 2 timeout");
        });
        apiSafeCtx.flush(f);
    }

    /**
     * Implements the ClientApiSafe interface to handle responses from the server.
     */
    private static class MyClientApiSafe implements ClientApiSafe {
        private final AetherCloudClient client;
        private final ConnectionWork connection; // Ссылка на ConnectionWork

        public MyClientApiSafe(AetherCloudClient client, ConnectionWork connection) {
            this.client = client;
            this.connection = connection;
        }

        @Override
        public void changeParent(UUID uid) {
        }

        @Override
        public void changeAlias(UUID alias) {
        }

        @Override
        public void requestTelemetry() {
        }

        /**
         * Handles response for batched AccessGroup requests.
         */
        @Override
        public void sendAccessGroups(AccessGroup[] groups) {
            Log.debug("Received $count AccessGroups", "count", groups.length);
            for (AccessGroup group : groups) {
                if (group != null) {
                    client.accessGroups.putResolved(group.getId(), group);
                }
            }
        }

        /**
         * Handles response for batched client group list requests.
         */
        @Override
        public void sendAccessGroupForClient(UUID uid, long[] groups) {
            Log.debug("Received AccessGroups for client $uid", "uid", uid);
            client.clientGroups.putResolved(uid, LongSet.of(groups)); //
        }

        /**
         * Handles server push notification for added group items.
         * This confirms a mutation request.
         */
        @Override
        public void addItemsToAccessGroup(long id, UUID[] groups) {
            Log.debug("Server confirmed ADD items to group $id", "id", id);
            Map<UUID, ARFuture<Boolean>> futures = client.accessOperationsAdd.get(id);
            if (futures != null) {
                for (UUID uid : groups) {
                    ARFuture<Boolean> future = futures.remove(uid);
                    if (future != null) {
                        future.tryDone(true);
                    }
                }
                if (futures.isEmpty()) {
                    client.accessOperationsAdd.remove(id);
                }
            }
            // Обновляем BMap-кэш
            client.accessGroups.getFuture(id).to(group -> {
                if (group != null) {
                    // Создаем новый, так как AccessGroup неизменяемый
                    List<UUID> newUuids = new ArrayList<>(List.of(group.getData()));
                    newUuids.addAll(List.of(groups));
                    AccessGroup newGroup = new AccessGroup(group.getOwner(), group.getId(),
                            newUuids.stream().distinct().toArray(UUID[]::new));
                    client.accessGroups.putResolved(id, newGroup);
                }
            });
        }

        /**
         * Handles server push notification for removed group items.
         * This confirms a mutation request.
         */
        @Override
        public void removeItemsFromAccessGroup(long id, UUID[] groups) {
            Log.debug("Server confirmed REMOVE items from group $id", "id", id);
            Map<UUID, ARFuture<Boolean>> futures = client.accessOperationsRemove.get(id);
            if (futures != null) {
                for (UUID uid : groups) {
                    ARFuture<Boolean> future = futures.remove(uid);
                    if (future != null) {
                        future.tryDone(true);
                    }
                }
                if (futures.isEmpty()) {
                    client.accessOperationsRemove.remove(id);
                }
            }
            // Обновляем BMap-кэш
            client.accessGroups.getFuture(id).to(group -> {
                if (group != null) {
                    List<UUID> newUuids = new ArrayList<>(List.of(group.getData()));
                    newUuids.removeAll(List.of(groups));
                    AccessGroup newGroup = new AccessGroup(group.getOwner(), group.getId(),
                            newUuids.toArray(new UUID[0]));
                    client.accessGroups.putResolved(id, newGroup);
                }
            });
        }

        /**
         * Handles server push notification for groups added TO A CLIENT.
         */
        @Override
        public void addAccessGroupsToClient(UUID uid, long[] groups) {
            Log.debug("Server pushed ADD groups to client $uid", "uid", uid);
            // Обновляем BMap-кэш
            client.clientGroups.getFuture(uid).to(existingGroups -> {
                var newGroups = (existingGroups == null) ? LongSet.of() : new LongArraySet(existingGroups);
                for (long g : groups) newGroups.add(g);
            });
        }

        /**
         * Handles server push notification for groups removed FROM A CLIENT.
         */
        @Override
        public void removeAccessGroupsFromClient(UUID uid, long[] groups) {
            Log.debug("Server pushed REMOVE groups from client $uid", "uid", uid);
            // Обновляем BMap-кэш
            client.clientGroups.getFuture(uid).to(existingGroups -> {
                if (existingGroups != null) {
                    var newGroups = new LongArraySet(existingGroups);
                    for (long g : groups) newGroups.remove(g);
                }
            });
        }

        /**
         * Handles response for batched getAllAccessedClients requests.
         */
        @Override
        public void sendAllAccessedClients(UUID uid, UUID[] accessedClients) {
            Log.debug("Received $count AccessedClients for $uid", "count", accessedClients.length, "uid", uid);
            client.allAccessedClients.putResolved(uid, ObjectSet.of(accessedClients)); //
        }

        /**
         * Handles response for batched access check requests.
         */
        @Override
        public void sendAccessCheckResults(AccessCheckResult[] results) {
            Log.debug("Received $count AccessCheckResults", "count", results.length);
            for (AccessCheckResult result : results) {
                if (result != null) {
                    client.accessCheckCache.putResolved(
                            new AccessCheckPair(result.getSourceUid(), result.getTargetUid()), //
                            result.isHasAccess() //
                    );
                }
            }
        }

        @Override
        public void sendMessages(Message[] msg) {
            Log.trace("receive messages: $count", "count", msg.length);
            for (var m : msg) {
                Log.trace("receive message $uid1 <- $uid2", "uid1", client.getUid(), "uid2", m.getUid());
                client.getMessageNode(m.getUid(), MessageEventListener.DEFAULT).sendMessageFromServerToClient(m.getData());
            }
        }

        @Override
        public void sendServerDescriptor(ServerDescriptor v) {
            client.putServerDescriptor(v);
        }

        @Override
        public void sendCloud(UUID uid, Cloud cloud) {
            client.setCloud(uid, cloud);
        }

        @Override
        public void sendServerDescriptors(ServerDescriptor[] serverDescriptors) {
            for (var c : serverDescriptors) {
                sendServerDescriptor(c);
            }
        }

        @Override
        public void sendClouds(UUIDAndCloud[] clouds) {
            for (var c : clouds) {
                sendCloud(c.getUid(), c.getCloud());
            }
        }

        @Override
        public void newChild(UUID uid) {
            client.onNewChild.fire(uid);
        }

    }
}