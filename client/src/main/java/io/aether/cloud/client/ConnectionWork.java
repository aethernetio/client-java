package io.aether.cloud.client;

import io.aether.api.clientserverapi.*;
import io.aether.api.common.AccessCheckPair;
import io.aether.api.common.AetherCodec;
import io.aether.api.common.AppliedConfig;
import io.aether.api.common.ServerDescriptor;
import io.aether.crypto.CryptoEngine;
import io.aether.logger.Log;
import io.aether.utils.RU;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.tuples.Tuple2;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles the specific connection logic for Work Servers, including authentication,
 * batching of API requests, and message routing.
 */
public class ConnectionWork extends Connection<ClientApiUnsafe, LoginApiRemote> implements ClientApiUnsafe {

    public final AtomicLong lastBackPing = new AtomicLong(Long.MAX_VALUE);

    final CryptoEngine cryptoEngine;
    final AuthorizedApiRemote authorizedApi;
    private final ServerDescriptor serverDescriptor;
    final private AtomicBoolean inProcess = new AtomicBoolean();
    boolean basicStatus;
    long lastWorkTime;
    volatile boolean firstAuth;

    public ConnectionWork(AetherCloudClient client, ServerDescriptor s) {
        super(client, s.getIpAddress().getURI(AetherCodec.UDP), ClientApiUnsafe.META, LoginApi.META);
        cryptoEngine = client.getCryptoEngineForServer(s.getId());

        if (cryptoEngine == null) {
            Log.error("ConnectionWork: cryptoEngine is null for server " + s.getId() + ". Authentication will fail.");
        }
        serverDescriptor = s;
        this.basicStatus = false;
        authorizedApi = getRootApi().openLoginByAlias(client.getAlias(), c -> new ClientApiSafeImpl(this, client), cryptoEngine::encrypt, "loginByAlias");
    }

    /**
     * Handles changes in the connection state. Resets the internal authentication
     * flag and fires the state listeners to notify the client for failover logic.
     *
     * @param isWritable True if the connection is active and writable, false otherwise.
     */
    @Override
    protected void onConnectionStateChanged(boolean isWritable) {
        if (cryptoEngine == null) {
            Log.warn("onConnectionStateChanged called before cryptoEngine initialized, deferring flush");
            stateListeners.fire(isWritable);
            return;
        }
        if (isWritable) {
            Log.info("Network restored. Resetting auth state and forcing flush.", "uri", uri);
            this.firstAuth = false;
        } else {
            this.firstAuth = false;
        }
        stateListeners.fire(isWritable);
    }



    public void flushBackgroundRequests() {
        var a = authorizedApi;
        // Запросы облаков через новый механизм
        for (UUID uid : client.clouds.pollAllRequests()) {
            ClientCloud cc = client.clouds.getNow(uid);
            long version = cc != null ? cc.getConfigVersion() - 1 : -1;
            client.pendingAppliedConfigs.add(new AppliedConfig(uid, version));
        }
        AppliedConfig[] pending = client.pendingAppliedConfigs.toArray(new AppliedConfig[0]);
        if (pending.length > 0) {
            a.reportAppliedConfig(pending);
        }

        Integer[] requestServers = client.servers.pollAllRequests().toArray(new Integer[0]);

        if (requestServers.length > 0) {
            short[] serverIds = new short[requestServers.length];
            for (int i = 0; i < requestServers.length; i++) {
                serverIds[i] = requestServers[i].shortValue();
            }
            a.resolverServers(serverIds);
        }
        UUID[] requestClientGroups = client.clientGroups.pollAllRequests().toArray(new UUID[0]);
        if (requestClientGroups.length > 0) {
            a.requestAccessGroupsForClients(requestClientGroups);
        }
        Long[] requestAccessGroups = client.accessGroups.pollAllRequests().toArray(new Long[0]);
        if (requestAccessGroups.length > 0) {
            long[] groupIds = new long[requestAccessGroups.length];
            for (int i = 0; i < requestAccessGroups.length; i++) {
                groupIds[i] = requestAccessGroups[i];
            }
            a.requestAccessGroupsItems(groupIds);
        }
        UUID[] requestAllAccessed = client.allAccessedClients.pollAllRequests().toArray(new UUID[0]);
        if (requestAllAccessed.length > 0) {
            a.requestAllAccessedClients(requestAllAccessed);
        }
        AccessCheckPair[] requestAccessCheck = client.accessCheckCache.pollAllRequests().toArray(new AccessCheckPair[0]);
        if (requestAccessCheck.length > 0) {
            a.requestAccessCheck(requestAccessCheck);
        }
        for (Map.Entry<Long, Map<UUID, ARFuture<Boolean>>> entry : client.accessOperationsAdd.entrySet()) {
            long groupId = entry.getKey();
            UUID[] uidsToAdd = entry.getValue().keySet().toArray(new UUID[0]);
            if (uidsToAdd.length > 0) {
                Log.debug("Flushing ADD request for group $gid: $uids", "gid", groupId, "uids", uidsToAdd);
                a.addItemsToAccessGroup(groupId, uidsToAdd);
            }
        }
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
        List<Message> messagesForSend = null;
        List<Runnable> tasksForCancelMessages = new ArrayList<>();
        List<Tuple2<byte[], AFuture>> messagesForSend2 = new ArrayList<>();
        for (var m : client.messageNodeMap.values()) {
            if (m.connectionsOut.contains(this)) {
                List<Tuple2<byte[], AFuture>> nodeMessages = new ArrayList<>();
                int currentBatchSize = 0;
                final int MAX_BATCH_BYTES = 512 * 1024; // 512 KB для Java
                while (true) {
                    var entry = m.bufferOut.peekFirst();
                    if (entry == null || (currentBatchSize + entry.val1().length > MAX_BATCH_BYTES)) break;
                    nodeMessages.add(m.bufferOut.pollFirst());
                    currentBatchSize += nodeMessages.get(nodeMessages.size() - 1).val1().length;
                }
                if (!nodeMessages.isEmpty()) {
                    Log.debug("message send client to server: $uidFrom -> $uidTo", "uidFrom", client.getUid(), "uidTo", m.consumer);
                    if (messagesForSend == null) {
                        messagesForSend = new ObjectArrayList<>();
                    }
                    Flow.flow(nodeMessages).map(v -> new Message(m.consumer, v.val1())).toCollection(messagesForSend);
                    messagesForSend2.addAll(nodeMessages);
                    tasksForCancelMessages.add(() -> {
                        m.bufferOut.addAll(nodeMessages);
                    });
                }
            }
        }
        if (messagesForSend != null && !messagesForSend.isEmpty()) {
            MessageBatcher batcher = new MessageBatcher();
            for (var msg : messagesForSend) {
                batcher.add(msg.getUid(), msg.getData());
            }
            batcher.flush(a);
        }


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
        data.asIn()
                .convert(cryptoEngine::decrypt)
                .ctx(authorizedApi.getFastMetaContext())
                .accept();
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
        f.timeout(2, () -> {
            Log.warn("connection work flush 1 timeout");
        });
    }

}