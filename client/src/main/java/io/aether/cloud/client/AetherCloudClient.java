package io.aether.cloud.client;

import io.aether.StandardUUIDs;
import io.aether.api.clientserverapi.AuthorizedApi;
import io.aether.api.clientserverapi.ServerApiByUid;
import io.aether.api.clientserverregapi.FinishResult;
import io.aether.api.common.*;
import io.aether.common.AccessGroupI;
import io.aether.crypto.AKey;
import io.aether.crypto.CryptoEngine;
import io.aether.crypto.CryptoProviderFactory;
import io.aether.logger.LNode;
import io.aether.logger.Log;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.Destroyer;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.ABiConsumer;
import io.aether.utils.interfaces.AConsumer;
import io.aether.utils.interfaces.AFunction;
import io.aether.utils.interfaces.Destroyable;
import io.aether.utils.rcollections.BMap;
import io.aether.utils.rcollections.RCol;
import io.aether.utils.rcollections.RFMap;
import io.aether.utils.rcollections.RMap;
import io.aether.utils.slots.EventBiConsumer;
import io.aether.utils.slots.EventConsumer;
import io.aether.utils.slots.EventConsumerWithQueue;
import io.aether.utils.streams.Value;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.aether.utils.flow.Flow.flow;

/**
 * Main client class for connecting to and interacting with the Aether Cloud.
 * Handles connections, registration, API access, and message streaming.
 */
public final class AetherCloudClient implements Destroyable {

    // NESTED EXCEPTION CLASSES FOR DIAGNOSTICS
    // =========================================================================

    /** Exception related to client startup and connection issues. */
    public static class ClientStartException extends RuntimeException {
        public ClientStartException(String message) {
            super(message);
        }
        public ClientStartException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Exception related to errors occurring during API requests. */
    public static class ClientApiException extends RuntimeException {
        public ClientApiException(String message) {
            super(message);
        }
        public ClientApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Exception related to internal asynchronous operation timeouts. */
    public static class ClientTimeoutException extends RuntimeException {
        public ClientTimeoutException(String message) {
            super(message);
        }
    }

    // =========================================================================

    public final AFuture startFuture = AFuture.make();
    public final EventConsumer<MessageNode> onClientStream = new EventConsumerWithQueue<>();
    public final Destroyer destroyer = new Destroyer(getClass().getSimpleName());
    final LNode logClientContext;
    final Map<Integer, ConnectionWork> connections = new ConcurrentHashMap<>();

    // REFACTORED: BMap<UUID, Cloud> заменяет RMap, RFMap и requestCloud
    final BMap<UUID, Cloud> clouds = RCol.bMap(2000, "CloudCache");
    final AtomicReference<RegStatus> regStatus = new AtomicReference<>(RegStatus.NO);
    // REFACTORED: BMap<Integer, ServerDescriptor> заменяет RMap, RFMap и requestServers
    final BMap<Integer, ServerDescriptor> servers = RCol.bMap(2000, "ServerCache");

    final long lastSecond;
    final Map<UUID, MessageNode> messageNodeMap = new ConcurrentHashMap<>();
    final EventConsumer<UUID> onNewChild = new EventConsumer<>();
    final EventBiConsumer<UUID, ServerApiByUid> onNewChildApi = new EventBiConsumer<>();
    final Queue<AConsumer<AuthorizedApi>> queueAuth = new ConcurrentLinkedQueue<>();

    final Map<Long, Map<UUID, ARFuture<Boolean>>> accessOperationsAdd = new ConcurrentHashMap<>();
    final Map<Long, Map<UUID, ARFuture<Boolean>>> accessOperationsRemove = new ConcurrentHashMap<>();
    final Queue<ClientTask> clientTasks = new ConcurrentLinkedQueue<>();

    private final ClientState clientState;
    private final AtomicBoolean startConnection = new AtomicBoolean();
    private final int timeout1 = 5;
    private String name;

    {
        lastSecond = System.currentTimeMillis() / 1000;
    }

    public AetherCloudClient() {
        this(new ClientStateInMemory(StandardUUIDs.ANONYMOUS_UID, List.of(URI.create("tcp://registration.aethernet.io:9010"))));
    }

    public AetherCloudClient(ClientState store) {
        this(store, null);
    }

    public AetherCloudClient(ClientState store, String name) {
        logClientContext = Log.createContext("SystemComponent", "Client", "ClientName", name);
        try (var ln = Log.context(logClientContext)) {
            this.clientState = store;
            destroyer.add(this::closeConnections);

            // ИСПОЛЬЗОВАНИЕ НОВОГО НЕБЛОКИРУЮЩЕГО СОБЫТИЯ forValueUpdate()

            // 1. Слушатель для Cloud'ов: сохраняем Cloud в хранилище (store)
            clouds.forValueUpdate().add(uu -> store.setCloud(uu.key, uu.newValue));

            // 2. Слушатель для ServerDescriptor'ов: сохраняем Descriptor в хранилище
            servers.forValueUpdate().add(s -> {
                var ss = store.getServerInfo(s.key);
                ss.setDescriptor(s.newValue);
            });

            onNewChild.add(u ->  {
                if (onNewChildApi.hasListener()) {
                    getClientApi(u, api -> {
                        onNewChildApi.fire(u, api);
                    });
                }
            });
            connect();
        }
    }

    /**
     * Closes all active connections.
     */
    private void closeConnections() {
        connections.values().forEach(c -> c.destroy(true));
        connections.clear();
    }

    /**
     * Retrieves the access groups for a given client UUID.
     * @param uid The client UUID.
     * @return A future containing the set of group IDs.
     */
    public ARFuture<Set<Long>> getClientGroups(UUID uid) {
        return getAuthApiFuture().mapRFuture(a -> a.getAccessGroups(uid).map(LongSet::of));
    }

    /**
     * Retrieves all client UUIDs this client can access.
     * @param uid The client UUID.
     * @return A future containing the set of accessed client UUIDs.
     */
    public ARFuture<Set<UUID>> getAllAccessedClients(UUID uid) {
        return getAuthApiFuture().mapRFuture(a -> a.getAllAccessedClients(uid).map(ObjectSet::of));
    }

    /**
     * Checks if this client has permission to send messages to another client.
     * @param uid1 The source client UUID.
     * @param uid2 The target client UUID.
     * @return A future containing true if access is granted, false otherwise.
     */
    public ARFuture<Boolean> checkAccess(UUID uid1, UUID uid2) {
        return getAuthApiFuture().mapRFuture(a -> a.checkAccessForSendMessage2(uid1, uid2));
    }

    /**
     * Retrieves the AccessGroup details by its ID.
     * @param groupId The ID of the access group.
     * @return A future containing the AccessGroup.
     */
    public ARFuture<AccessGroup> getGroup(long groupId) {
        return getAuthApiFuture().mapRFuture(a -> a.getAccessGroup(groupId));
    }

    /**
     * Gets the client state storage interface.
     * @return The client state.
     */
    public ClientState getClientState() {
        return clientState;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves the server descriptor by its ID.
     * @param id The server ID.
     * @return A future containing the ServerDescriptor.
     */
    public ARFuture<ServerDescriptor> getServer(int id) {
        var res = servers.getFuture(id);

        res.timeout(5, () -> {
            Log.warn("Timeout waiting for server description: $id. The request is pending or failed to resolve.", "id", id);
        });

        return res;
    }

    public void getServerDescriptorForUid(@NotNull UUID uid, AConsumer<ServerDescriptor> t) {
        if (destroyer.isDestroyed()) return;
        if (uid.equals(getUid())) {
            Cloud cloud = clientState.getCloud(uid);
            if (cloud == null) return;
            for (var pp : cloud.getData()) {
                servers.getFuture((int) pp).to(t);
            }
            return;
        }
        getCloud(uid).to(p -> {
            for (var pp : p.getData()) {
                servers.getFuture((int) pp).to(t, timeout1, () -> Log.warn("timeout server resolve"));
            }
        }, timeout1, () -> Log.warn("timeout cloud resolve: $uid", "uid", uid));
    }

    ConnectionWork getConnection(@NotNull ServerDescriptor serverDescriptor) {
        // Added null check for robustness.
        if (serverDescriptor == null) {
            throw new ClientApiException("Cannot get connection for null ServerDescriptor.");
        }
        servers.putResolved((int) serverDescriptor.getId(), serverDescriptor); // Используем putResolved для BMap
        return connections.computeIfAbsent((int) serverDescriptor.getId(),
                s -> {
                    try (var ln = Log.context(logClientContext)) {
                        return new ConnectionWork(this, serverDescriptor);
                    }
                });
    }

    /**
     * Initiates the client connection process.
     * @return An AFuture that completes when the client is ready (registered/connected).
     */
    public AFuture connect() {
        if (!startConnection.compareAndSet(false, true)) return startFuture;
        connect(10);
        startFuture.to(() -> {
            RU.scheduleAtFixedRate(destroyer, 3, TimeUnit.MILLISECONDS, this::flush);
        }).onError(e -> {
            Log.error("Client failed to start", e);
        }).onCancel(() -> {
            Log.warn("Client start was cancelled");
        });
        return startFuture;
    }

    /**
     * Recursive connection attempt logic.
     * @param step The number of remaining retry attempts.
     */
    private void connect(int step) {
        if (destroyer.isDestroyed()) {
            startFuture.cancel();
            return;
        }
        if (step == 0) {
            Log.error("All connection attempts failed.");
            if (!startFuture.isFinalStatus()) {
                startFuture.error(new ClientStartException("All connection attempts failed to register or connect."));
            }
            return;
        }

        if (getUid()==null) {
            if(regStatus.compareAndSet(RegStatus.NO, RegStatus.BEGIN)) {
                var uris = clientState.getRegistrationUri();
                if (uris == null || uris.isEmpty()) {
                    if (!startFuture.isFinalStatus()) {
                        startFuture.error(new ClientStartException("Registration URI list is void."));
                    }
                    return;
                }
                var timeoutForConnect = clientState.getTimeoutForConnectToRegistrationServer();
                var countServersForRegistration = Math.min(uris.size(), clientState.getCountServersForRegistration());

                try {
                    var startFutures = flow(uris).shuffle().limit(countServersForRegistration)
                            .map(sd -> {
                                try (var ln = Log.context(logClientContext)) {
                                    return new ConnectionRegistration(this, sd).connectFuture.toFuture();
                                }
                            })
                            .toList();

                    var anyFuture = AFuture.any(startFutures);

                    // Propagate statuses to startFuture
                    anyFuture.to(this::startScheduledTask)
                            .onError(startFuture::error)
                            .onCancel(startFuture::cancel);

                    // Timeout logic
                    anyFuture.timeoutMs(timeoutForConnect, () -> {
                        Log.error("Failed to connect to registration server: $uris", "uris", uris);
                        // Retry
                        RU.schedule(4000, () -> this.connect(step - 1));
                    });
                } catch (Exception e) {
                    Log.error("Fatal error during registration setup.", e);
                    if (!startFuture.isFinalStatus()) {
                        startFuture.error(new ClientStartException("Fatal error during registration setup.", e));
                    }
                }
            }
        } else {
            // Logic for connecting to own cloud
            try {
                var uid = getUid();
                if (uid == null) {
                    throw new ClientStartException("UID is null but regStatus is not NO/BEGIN.");
                }
                var cloud = clientState.getCloud(uid);
                if (cloud == null || cloud.getData().length == 0) {
                    throw new ClientStartException("Client is registered but cloud data is empty.");
                }
                for (var serverId : cloud.getData()) {
                    getConnection(clientState.getServerDescriptor(serverId));
                }
                startFuture.done();
            } catch (Exception e) {
                Log.error("Fatal error during connection to own cloud.", e);
                if (!startFuture.isFinalStatus()) {
                    startFuture.error(new ClientStartException("Fatal error during connection to own cloud.", e));
                }
                RU.error(e);
            }
        }
    }

    /**
     * Placeholder for scheduled task initiation.
     */
    private void startScheduledTask() {
        // Implementation omitted for brevity.
    }

    /**
     * Retrieves the client's UUID.
     * @return The client's UUID.
     */
    public UUID getUid() {
        return clientState.getUid();
    }

    /**
     * Helper method to get an authorized API future and map a function over it.
     * @param t The function to execute on the AuthorizedApi.
     * @return A future with the result of the function.
     */
    public <T> ARFuture<T> getAuthApi1(@NotNull AFunction<AuthorizedApi, ARFuture<T>> t) {
        if (destroyer.isDestroyed()) return ARFuture.canceled();
        ARFuture<T> res = ARFuture.of();
        getAuthApiFuture().mapRFuture(t).to(res);
        return res;
    }

    /**
     * Returns a future that completes with the AuthorizedApi instance or an error/cancellation.
     * A timeout is applied to ensure the future is not left incomplete.
     * @return A future containing the AuthorizedApi instance.
     */
    public ARFuture<AuthorizedApi> getAuthApiFuture() {
        ARFuture<AuthorizedApi> res = ARFuture.of();
        if (destroyer.isDestroyed()) {
            res.cancel();
            return res;
        }
        // Enqueue the consumer, which will call res.done(api) later.
        getAuthApi(res::done);

        // Ensures the future doesn't hang indefinitely.
        res.timeoutError(5, "Timeout waiting for AuthorizedApi to become available.");

        return res;
    }

    /**
     * Enqueues a consumer task to be executed when the AuthorizedApi is available.
     * This is a fire-and-forget method and relies on external mechanisms (like getAuthApiFuture)
     * for error and timeout handling.
     * @param t The consumer to execute with the AuthorizedApi.
     */
    public void getAuthApi(@NotNull AConsumer<AuthorizedApi> t) {
        if (destroyer.isDestroyed()) return;
        queueAuth.add(a -> {
            if (destroyer.isDestroyed()) return;
            try {
                t.accept(a);
            } catch (Exception e) {
                Log.error("Error executing AuthorizedApi consumer task.", e);
            }
        });
    }

    /**
     * Flushes pending requests and messages to the network connections.
     */
    public void flush() {
        // Проверяем BMap на наличие pending запросов
        if (connections.isEmpty()) {
            if (!messageNodeMap.isEmpty() || !servers.getPendingRequests().isEmpty() || !clouds.getPendingRequests().isEmpty()) {
                makeFirstConnection();
            }
        }
        for (var c : connections.values()) {
            c.flush();
        }
    }

    void makeFirstConnection() {
        if (destroyer.isDestroyed()) return;
        var uid = getUid();
        if (uid == null) {
            Log.warn("current my uid is null");
            return;
        }
        if (uid.equals(getUid())) {
            Cloud cloud = clientState.getCloud(uid);
            if (cloud == null || cloud.getData().length == 0) return;

            int serverId = (int) cloud.getData()[0];

            getServer(serverId).to(this::getConnection,
                    timeout1, () -> Log.warn("timeout ready connection: failed to get ServerDescriptor for ID: $id", "id", serverId));

            return;
        }
        getServerDescriptorForUid(uid, sd -> {
            getConnection(sd);
        });
    }

    public Collection<ConnectionWork> getConnections() {
        return connections.values();
    }

    /**
     * Retrieves the Cloud descriptor for a given UUID.
     * @param uid The UUID of the cloud owner.
     * @return A future containing the Cloud descriptor.
     */
    public ARFuture<Cloud> getCloud(@NotNull UUID uid) {
        var r=clientState.getCloud(uid);
        if(r!=null) return ARFuture.of(r);
        var res = clouds.getFuture(uid);

        // Propagate error on timeout
        res.timeout(4, () -> {
            Log.error("timeout get cloud: $uid", "uid", uid, "client", AetherCloudClient.this);
            res.error(new ClientTimeoutException("Timeout getting cloud for: " + uid));
        });
        return res;
    }

    public long getPingTime() {
        return clientState.getPingDuration().getNow();
    }

    public boolean isRegistered() {
        return clientState.getUid() != null;
    }

    /**
     * Confirms registration with the received result, updating client state.
     * @param regResp The result from the registration server.
     */
    public void confirmRegistration(FinishResult regResp) {
        if (!regStatus.compareAndSet(RegStatus.BEGIN, RegStatus.CONFIRM)) {
            Log.info("Already registration", "regData", regResp);
            return;
        }
        // Используем putResolved для BMap
        clouds.putResolved(regResp.getUid(), regResp.getCloud());

        clientState.setUid(regResp.getUid());
        clientState.setAlias(regResp.getAlias());
        assert isRegistered();
        Log.info("receive my cloud: $cloud", "cloud", regResp.getCloud());

        // FIX: Ensure immediate connection to WorkServer after registration
        var cloud = regResp.getCloud();
        if (cloud != null && cloud.getData().length > 0) {
            for (var serverId : cloud.getData()) {
                getServer((int) serverId).to(this::getConnection).onError(e -> {
                    Log.warn("Failed to establish WorkServer connection after registration for ID: $id", "id", serverId);
                });
            }
        }

        startFuture.done();
    }

    public MessageNode getMessageNode(@NotNull UUID uid) {
        return getMessageNode(uid, MessageEventListener.DEFAULT);
    }

    public MessageNode getMessageNode(@NotNull UUID uid, MessageEventListener strategy) {
        Log.debug("getMessageNode for: $uid", "uid", uid);
        Objects.requireNonNull(uid);
        return messageNodeMap.computeIfAbsent(uid, k -> {
            // FIX: Передача 'this' для конструктора MessageNode
            var res = new MessageNode(this, k, strategy);
            onClientStream.fire(res);
            return res;
        });
    }

    public MessageNode openStreamToClientDetails(@NotNull UUID uid, MessageEventListener strategy) {
        return getMessageNode(uid, strategy);
    }

    /**
     * Destroys the client and all associated resources.
     * @param force True to force immediate destruction.
     * @return An AFuture that completes when destruction is finished.
     */
    @Override
    public AFuture destroy(boolean force) {
        return destroyer.destroy(force)
                .onError(e -> Log.error("Error during AetherCloudClient destroy.", e))
                .onCancel(() -> Log.warn("AetherCloudClient destroy was cancelled."));
    }

    public boolean isConnected() {
        return getUid() != null;
    }

    public UUID getParent() {
        var res = clientState.getParentUid();
        assert res != null;
        return res;
    }

    public AKey.Symmetric getMasterKey() {
        var res2 = clientState.getMasterKey();
        if (res2 != null) return KeyUtil.of(res2).asSymmetric();
        var res = CryptoProviderFactory.getProvider(getCryptLib().name()).createSymmetricKey();
        clientState.setMasterKey(KeyUtil.of(res));
        return res;
    }

    public AetherCloudClient waitStart(int timeout) {
        startFuture.waitDoneSeconds(timeout);
        return this;
    }

    public CryptoLib getCryptLib() {
        return clientState.getCryptoLib();
    }

    public UUID getAlias() {
        return clientState.getAlias();
    }

    public void onMessage(ABiConsumer<UUID, byte[]> consumer) {
        onClientStream(m -> {
            m.bufferIn.add(d -> {
                consumer.accept(m.getConsumerUUID(), d.data());
            });
        });
    }

    public void onClientStream(AConsumer<MessageNode> consumer) {
        onClientStream.add(consumer);
    }

    public void onNewChildren(AConsumer<UUID> consumer) {
        onNewChild.add(consumer);
    }

    public ARFuture<AccessGroupI> createAccessGroup(UUID... uids) {
        return createAccessGroupWithOwner(getUid(), uids);
    }

    public ARFuture<AccessGroupI> createAccessGroupWithOwner(UUID owner, UUID... uids) {
        return getAuthApiFuture().mapRFuture(c -> c.createAccessGroup(owner, uids))
                .map(id -> new AccessGroupImpl(new AccessGroup(owner, id, new UUID[0])) {
                    @Override
                    public ARFuture<Boolean> add(UUID uuid) {
                        if (data.contains(uuid)) {
                            return ARFuture.of(Boolean.FALSE);
                        }
                        return accessOperationsAdd.computeIfAbsent(id, (k) -> new ConcurrentHashMap<>()).computeIfAbsent(uuid, k -> ARFuture.of());
                    }

                    @Override
                    public ARFuture<Boolean> remove(UUID uuid) {
                        if (!data.contains(uuid)) {
                            return ARFuture.of(Boolean.FALSE);
                        }
                        return accessOperationsRemove.computeIfAbsent(id, (k) -> new ConcurrentHashMap<>()).computeIfAbsent(uuid, k -> ARFuture.of());
                    }
                });
    }

    public void getClientApi(UUID uid, AConsumer<ServerApiByUid> c) {
        clientTasks.add(new ClientTask(uid, c));
    }

    public boolean verifySign(SignedKey signedKey) {
        return SignedKeyUtil.verifySign(signedKey, clientState.getRootSigners());
    }

    /**
     * Sends a data payload value to a specified client.
     * @param uid The target client UUID.
     * @param message The message data wrapped in a Value object.
     */
    public void sendMessage(UUID uid, Value<byte[]> message) {
        getMessageNode(uid, MessageEventListener.DEFAULT).send(message);
    }

    /**
     * Sends a raw byte array message to a specified client.
     * @param uid The target client UUID.
     * @param message The raw byte array message.
     * @return An AFuture that completes when the message is accepted for sending.
     */
    public AFuture sendMessage(UUID uid, byte[] message) {
        AFuture res = AFuture.make();
        // Use linkFuture to connect the AFuture's lifecycle to the Value's success/reject lifecycle.
        sendMessage(uid, Value.of(message).linkFuture(res));
        return res;
    }

    public CryptoEngine getCryptoEngineForServer(short serverId) {
        var k = getMasterKey();
        return k.getCryptoProvider().createKeyForClient(k, serverId).asSymmetric().toCryptoEngine();
    }

    public long getNextPing() {
        return 0;
    }

    /**
     * Используется для сохранения полученного Cloud.
     * Вызывает putResolved(), который запускает событие forValueUpdate(),
     * и оно уже сохраняет Cloud в clientState.
     * @param uid UUID облака.
     * @param cloud Объект Cloud.
     */
    public void setCloud(UUID uid, Cloud cloud) {
        clouds.putResolved(uid, cloud);
    }

    public static AetherCloudClient of(ClientState state) {
        return new AetherCloudClient(state);
    }

    private enum RegStatus {
        NO,
        BEGIN,
        CONFIRM
    }

    private static class ClientTask {
        final UUID uid;
        final AConsumer<ServerApiByUid> task;

        public ClientTask(UUID uid, AConsumer<ServerApiByUid> task) {
            this.uid = uid;
            this.task = task;
        }
    }
}