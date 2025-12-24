package io.aether.cloud.client;

import io.aether.StandardUUIDs;
import io.aether.api.CryptoUtils;
import io.aether.api.clientserverapi.AuthorizedApi;
import io.aether.api.clientserverapi.ServerApiByUid;
import io.aether.api.clientserverregapi.FinishResult;
import io.aether.api.common.*;
import io.aether.common.AccessGroupI;
import io.aether.crypto.AKey;
import io.aether.crypto.CryptoEngine;
import io.aether.crypto.CryptoProviderFactory;
import io.aether.crypto.SignedKey;
import io.aether.logger.LNode;
import io.aether.logger.Log;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.Destroyer;
import io.aether.utils.RU;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.ABiConsumer;
import io.aether.utils.interfaces.AConsumer;
import io.aether.utils.interfaces.AFunction;
import io.aether.utils.interfaces.Destroyable;
import io.aether.utils.rcollections.BMap;
import io.aether.utils.rcollections.RCol;
import io.aether.utils.slots.EventBiConsumer;
import io.aether.utils.slots.EventConsumer;
import io.aether.utils.slots.EventConsumerWithQueue;
import org.jetbrains.annotations.NotNull;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static io.aether.utils.flow.Flow.flow;

public final class AetherCloudClient implements Destroyable {

    private static final int RECOVERY_RETRY_DELAY_MS = 10000;

    public final AFuture startFuture = AFuture.make();

    public final EventConsumer<MessageNode> onClientStream = new EventConsumerWithQueue<>();

    public final Destroyer destroyer = new Destroyer(getClass().getSimpleName());

    final LNode logClientContext;

    final Map<Integer, ConnectionWork> connections = new ConcurrentHashMap<>();

    final BMap<UUID, Cloud> clouds = RCol.bMap(2000, "CloudCache");

    final AtomicReference<RegStatus> regStatus = new AtomicReference<>(RegStatus.NO);

    final BMap<Integer, ServerDescriptor> servers = RCol.bMap(2000, "ServerCache");

    final BMap<UUID, Set<Long>> clientGroups = RCol.bMap(1000, "ClientGroupsCache");

    final BMap<Long, AccessGroup> accessGroups = RCol.bMap(1000, "AccessGroupsCache");

    final BMap<UUID, Set<UUID>> allAccessedClients = RCol.bMap(1000, "AllAccessedClientsCache");

    final BMap<AccessCheckPair, Boolean> accessCheckCache = RCol.bMap(2000, "AccessCheckCache");

    final long lastSecond;

    final Map<UUID, MessageNode> messageNodeMap = new ConcurrentHashMap<>();

    final EventConsumer<UUID> onNewChild = new EventConsumer<>();

    final EventBiConsumer<UUID, ServerApiByUid> onNewChildApi = new EventBiConsumer<>();

    final Map<Long, Map<UUID, ARFuture<Boolean>>> accessOperationsAdd = new ConcurrentHashMap<>();

    final Map<Long, Map<UUID, ARFuture<Boolean>>> accessOperationsRemove = new ConcurrentHashMap<>();

    final Queue<ClientTask> clientTasks = new ConcurrentLinkedQueue<>();

    public final AtomicBoolean isRecoveryInProgress = new AtomicBoolean(false);

    public final AFuture recoveryFuture = AFuture.make();

    final Queue<AConsumer<AuthorizedApi>> authTasks = new ConcurrentLinkedQueue<>();

    final CloudPriorityManager priorityManager = new CloudPriorityManager();

    private final ClientState clientState;

    private final AtomicBoolean startConnection = new AtomicBoolean();

    private final int timeout1 = 6;

    private final AtomicBoolean startScheduledTaskFlag = new AtomicBoolean();

    private final Set<ConnectionRegistration> connectionRegistrations = new ConcurrentHashSet<>();

    private String name;

    {
        lastSecond = System.currentTimeMillis() / 1000;
    }

    {
        startFuture.to(this::startScheduledTask);
    }

    public AetherCloudClient() {
        this(new ClientStateInMemory(StandardUUIDs.ANONYMOUS_UID, List.of(URI.create("tcp://registration.aethernet.io:9010"))));
    }

    public AetherCloudClient(ClientState store) {
        this(store, null);
    }

    public AetherCloudClient(ClientState store, String name) {
        Objects.requireNonNull(store);
        this.clientState = store;
        this.name = name;
        logClientContext = Log.of("SystemComponent", "Client", "ClientName", name);
        try (var ln = logClientContext.context()) {
            destroyer.add(this::closeConnections);
            populateCachesFromState();
            clouds.forValueUpdate().add(uu -> store.setCloud(uu.key, new ClientCloud(uu.key, uu.newValue)));
            servers.forValueUpdate().add(s -> {
                var ss = store.getServerInfo(s.key);
                ss.setDescriptor(s.newValue);
            });
            onNewChild.add(u -> {
                if (onNewChildApi.hasListener()) {
                    getClientApi(u, api -> {
                        onNewChildApi.fire(u, api);
                    });
                }
            });
            connect();
        }
    }

    private void closeConnections() {
        connections.values().forEach(c -> c.destroy(true));
        connections.clear();
    }

    public void populateCachesFromState() {
        if (getUid() == null)
            return;
        for (var c : clientState.getClientInfoAll()) {
            if (c.getCloud() != null) {
                priorityManager.updateCloudFromWork(c.getUid(), c.getCloud().toCloud());
                clouds.putResolved(c.getUid(), c.getCloud().toCloud());
            }
        }
        for (var s : clientState.getServerInfoAll()) {
            if (s.getDescriptor() != null)
                servers.putResolved(s.getServerId(), s.getDescriptor());
        }
    }

    public ARFuture<Set<Long>> getClientGroups(UUID uid) {
        return clientGroups.getFuture(uid);
    }

    public ARFuture<Set<UUID>> getAllAccessedClients(UUID uid) {
        return allAccessedClients.getFuture(uid);
    }

    public ARFuture<Boolean> checkAccess(UUID uid1, UUID uid2) {
        return accessCheckCache.getFuture(new AccessCheckPair(uid1, uid2));
    }

    public ARFuture<AccessGroup> getGroup(long groupId) {
        return accessGroups.getFuture(groupId);
    }

    public ClientState getClientState() {
        return clientState;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ARFuture<ServerDescriptor> getServer(int id) {
        var res = servers.getFuture(id);
        res.timeout(7, () -> {
            Log.warn("Timeout waiting for server description: $id.", "id", id);
        });
        return res;
    }

    public void getServerDescriptorForUid(@NotNull UUID uid, AConsumer<ServerDescriptor> t) {
        if (destroyer.isDestroyed())
            return;
        if (uid.equals(getUid())) {
            var cloud = clientState.getCloud(uid);
            if (cloud == null)
                return;
            for (var pp : cloud.getOrderedSids()) {
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

    /**
     * Retrieves an existing ConnectionWork or creates a new one for the specified server.
     * Configures a state listener to detect connection loss, triggering a priority demotion
     * in the CloudPriorityManager and an automatic failover to the next available server.
     *
     * @param serverDescriptor The metadata of the server to connect to.
     * @return A ConnectionWork instance with an attached failover listener.
     */
    ConnectionWork getConnection(@NotNull ServerDescriptor serverDescriptor) {
        if (serverDescriptor == null) {
            throw new ClientApiException("Cannot get connection for null ServerDescriptor.");
        }
        int sid = (int) serverDescriptor.getId();
        putServerDescriptor(serverDescriptor);
        return connections.computeIfAbsent(sid, s -> {
            try (var ln = logClientContext.context()) {
                ConnectionWork conn = new ConnectionWork(this, serverDescriptor);
                // Adaptive Cloud: Listen for connection state changes to handle failures.
                conn.stateListeners.add(isWritable -> {
                    if (!isWritable) {
                        UUID uid = getUid();
                        if (uid != null) {
                            Log.info("Connection to server failed or lost. Demoting SID and attempting failover.", "sid", sid);
                            // Lower the priority of the failed server using the verified demote method.
                            priorityManager.demote(uid, (short) sid);
                            // Re-initiate connection process to find and connect to the next best SID.
                            makeFirstConnection();
                        }
                    }
                });
                return conn;
            }
        });
    }

    public AFuture connect() {
        if (!startConnection.compareAndSet(false, true))
            return startFuture;
        connect(10);
        return startFuture;
    }

    private Flow<ConnectionRegistration> makeConnectionReg() {
        if (!connectionRegistrations.isEmpty()) {
            return Flow.flow(connectionRegistrations);
        }
        var uris = clientState.getRegistrationUri();
        if (uris == null || uris.isEmpty()) {
            throw new ClientStartException("Registration URI list is void.");
        }
        var countServersForRegistration = Math.min(uris.size(), clientState.getCountServersForRegistration());
        flow(uris).shuffle().limit(countServersForRegistration).map(sd -> new ConnectionRegistration(this, sd)).toCollection(connectionRegistrations);
        return Flow.flow(connectionRegistrations);
    }

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
        if (getUid() == null) {
            if (regStatus.compareAndSet(RegStatus.NO, RegStatus.BEGIN)) {
                var regs = makeConnectionReg();
                var timeoutForConnect = clientState.getTimeoutForConnectToRegistrationServer();
                try {
                    var anyFuture = AFuture.any(regs.map(ConnectionRegistration::registration));
                    anyFuture.to(this::startScheduledTask).onError(startFuture::error).onCancel(startFuture::cancel);
                    anyFuture.timeoutMs(timeoutForConnect, () -> {
                        Log.warn("Failed to connect to registration server: $uris", "uris", clientState.getRegistrationUri());
                        RU.schedule(1000, () -> this.connect(step - 1));
                    });
                } catch (Exception e) {
                    Log.error("Fatal error during registration setup.", e);
                    if (!startFuture.isFinalStatus()) {
                        startFuture.error(new ClientStartException("Fatal error during registration setup.", e));
                    }
                }
            }
        } else {
            try {
                var uid = getUid();
                if (uid == null) {
                    throw new ClientStartException("UID is null but regStatus is not NO/BEGIN.");
                }
                var cloudData = clientState.getCloud(uid);
                var cloud = (cloudData != null) ? cloudData.toCloud() : null;
                boolean isCacheMissingDescriptors = false;
                if (cloud != null && cloud.getData().length > 0) {
                    if (Flow.flow(cloud.getData()).mapToObj(id -> servers.getFuture((int) id).getNow()).filterNotNull().isEmpty()) {
                        isCacheMissingDescriptors = true;
                    }
                }
                if (cloud == null) {
                    Log.info("Recovery required: Cloud missing from cache. Starting recovery.");
                    triggerRecovery().to(startFuture);
                    return;
                }
                if (isCacheMissingDescriptors) {
                    Log.info("Recovery required: ServerDescriptors missing from cache. Starting recovery.");
                    triggerRecovery().to(startFuture);
                    return;
                }
                makeFirstConnection();
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

    public AFuture forceUpdateStateFromCache() {
        AFuture resultFuture = AFuture.make();
        UUID uid = getUid();
        if (uid == null) {
            Log.warn("forceUpdateStateFromCache called, but client UID is null.");
            resultFuture.error(new IllegalStateException("Client is not registered."));
            return resultFuture;
        }
        getCloud(uid).to(cloud -> {
            if (cloud == null) {
                resultFuture.error(new IllegalStateException("Fetched cloud was null for UID: " + uid));
                return;
            }
            clientState.setCloud(uid, new ClientCloud(uid, cloud));
            List<ARFuture<ServerDescriptor>> serverFutures = new ArrayList<>();
            for (var sid : cloud.getData()) {
                serverFutures.add(getServer(sid));
            }
            ARFuture.all(serverFutures).to(() -> {
                for (var sid : cloud.getData()) {
                    ServerDescriptor descriptor = getServer(sid).getNow();
                    if (descriptor != null) {
                        clientState.getServerInfo(descriptor.getId()).setDescriptor(descriptor);
                    }
                }
                resultFuture.done();
            }).onError(resultFuture::error);
        }).onError(resultFuture::error);
        return resultFuture;
    }

    public AFuture triggerRecovery() {
        if (isRecoveryInProgress.get()) {
            return recoveryFuture;
        }
        Log.info("Starting recovery process...");
        var regs = makeConnectionReg();
        var cloudData = clientState.getCloud(getUid());
        var cloud = (cloudData != null) ? cloudData.toCloud() : null;
        AFuture recoveryFutureLocal = AFuture.any(regs.map(c -> (AFuture) c.resolveCloud(cloud)));
        recoveryFutureLocal.to(() -> {
            Log.info("Recovery successful.");
            isRecoveryInProgress.set(false);
            recoveryFuture.done();
        }).onError(e -> {
            Log.error("Recovery attempt failed.", e);
            RU.schedule(RECOVERY_RETRY_DELAY_MS, () -> isRecoveryInProgress.set(false));
        });
        return recoveryFutureLocal;
    }

    private void startScheduledTask() {
        if (startScheduledTaskFlag.compareAndSet(false, true)) {
            RU.scheduleAtFixedRate(destroyer, 3, TimeUnit.MILLISECONDS, this::flush);
        }
    }

    public UUID getUid() {
        return clientState.getUid();
    }

    public <T> ARFuture<T> getAuthApi1(@NotNull AFunction<AuthorizedApi, ARFuture<T>> t) {
        if (destroyer.isDestroyed())
            return ARFuture.canceled();
        ARFuture<T> res = ARFuture.make();
        getAuthApiFuture().mapRFuture(t).to(res);
        return res;
    }

    public ARFuture<AuthorizedApi> getAuthApiFuture() {
        ARFuture<AuthorizedApi> res = ARFuture.make();
        if (destroyer.isDestroyed()) {
            res.cancel();
            return res;
        }
        getAuthApi(res::done);
        res.timeoutError(8, "Timeout waiting for AuthorizedApi.");
        return res;
    }

    private ConnectionWork getAnyConnection() {
        if (connections.isEmpty()) {
            makeFirstConnection();
            return null;
        }
        for (var c : connections.values()) {
            if (c.firstAuth)
                return c;
        }
        return connections.values().stream().findFirst().orElse(null);
    }

    public void getAuthApi(@NotNull AConsumer<AuthorizedApi> t) {
        if (destroyer.isDestroyed())
            return;
        authTasks.add(t);
    }

    public void flush() {
        if (connections.isEmpty()) {
            if (getUid() == null)
                return;
            var cloud = clientState.getCloud(getUid());
            if (cloud == null)
                return;
            makeFirstConnection();
        }
        for (var c : connections.values()) {
            c.flush();
        }
    }

    /**
     * Establishes connections to all servers in the client's cloud.
     * This ensures the client is reachable via any server in their cloud (recipient strategy).
     * If any connection fails, its SID is demoted and the process continues.
     */
    void makeFirstConnection() {
        if (destroyer.isDestroyed())
            return;
        var uid = getUid();
        if (uid == null)
            return;
        getCloud(uid).to(cloud -> {
            if (cloud == null || cloud.getData().length == 0) {
                triggerRecovery();
                return;
            }
            // Get Sids ordered by Experience Weight (Primary first)
            short[] orderedSids = priorityManager.getOrderedSids(uid, cloud);
            // Connect to ALL servers in the cloud to ensure message delivery from any path
            for (short sid : orderedSids) {
                getServer((int) sid).to(descriptor -> {
                    ConnectionWork conn = getConnection(descriptor);
                    // For the primary (top) server, link it to the client's start status
                    if (sid == orderedSids[0]) {
                        conn.connectFuture.to(startFuture::tryDone);
                    }
                }).onError(e -> {
                    priorityManager.demote(uid, sid);
                    // Recursively try to fix the list if the primary node is unreachable
                    if (sid == orderedSids[0]) {
                        makeFirstConnection();
                    }
                });
            }
        });
    }

    public Collection<ConnectionWork> getConnections() {
        return connections.values();
    }

    public ARFuture<Cloud> getCloud(@NotNull UUID uid) {
        var r = clientState.getCloud(uid);
        if (r != null)
            return ARFuture.of(r.toCloud());
        var res = clouds.getFuture(uid);
        res.timeout(4, () -> {
            Log.error("timeout get cloud: $uid", "uid", uid);
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

    public void confirmRegistration(FinishResult regResp) {
        if (!regStatus.compareAndSet(RegStatus.BEGIN, RegStatus.CONFIRM)) {
            Log.info("Already registration");
            return;
        }
        clouds.putResolved(regResp.getUid(), regResp.getCloud());
        clientState.setUid(regResp.getUid());
        clientState.setAlias(regResp.getAlias());
        var cloud = regResp.getCloud();
        if (cloud != null && cloud.getData().length > 0) {
            for (var serverId : cloud.getData()) {
                getServer((int) serverId).to(this::getConnection);
            }
        }
        startFuture.done();
    }

    public MessageNode getMessageNode(@NotNull UUID uid) {
        return getMessageNode(uid, MessageEventListener.DEFAULT);
    }

    public MessageNode getMessageNode(@NotNull UUID uid, MessageEventListener strategy) {
        Objects.requireNonNull(uid);
        return messageNodeMap.computeIfAbsent(uid, k -> {
            var res = new MessageNode(this, k, strategy);
            onClientStream.fire(res);
            return res;
        });
    }

    public MessageNode openStreamToClientDetails(@NotNull UUID uid, MessageEventListener strategy) {
        return getMessageNode(uid, strategy);
    }

    @Override
    public AFuture destroy(boolean force) {
        return destroyer.destroy(force);
    }

    public boolean isConnected() {
        return getUid() != null;
    }

    public UUID getParent() {
        var res = clientState.getParentUid();
        return res;
    }

    public AKey.Symmetric getMasterKey() {
        var res2 = clientState.getMasterKey();
        if (res2 != null)
            return CryptoUtils.of(res2).asSymmetric();
        var res = CryptoProviderFactory.getProvider(getCryptLib().name()).createSymmetricKey();
        clientState.setMasterKey(CryptoUtils.of(res));
        return res;
    }

    public AetherCloudClient waitStart(int timeout) {
        try {
            startFuture.toCompletableFuture().get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                consumer.accept(m.getConsumerUUID(), d);
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
        return getAuthApi1(c -> c.createAccessGroup(owner, uids)).map(id -> new AccessGroupImpl(new AccessGroup(owner, id, new UUID[0])) {

            @Override
            public ARFuture<Boolean> add(UUID uuid) {
                if (data.contains(uuid))
                    return ARFuture.of(Boolean.FALSE);
                ARFuture<Boolean> future = accessOperationsAdd.computeIfAbsent(id, (k) -> new ConcurrentHashMap<>()).computeIfAbsent(uuid, k -> ARFuture.make());
                AetherCloudClient.this.flush();
                return future;
            }

            @Override
            public ARFuture<Boolean> remove(UUID uuid) {
                if (!data.contains(uuid))
                    return ARFuture.of(Boolean.FALSE);
                ARFuture<Boolean> future = accessOperationsRemove.computeIfAbsent(id, (k) -> new ConcurrentHashMap<>()).computeIfAbsent(uuid, k -> ARFuture.make());
                AetherCloudClient.this.flush();
                return future;
            }
        });
    }

    public void getClientApi(UUID uid, AConsumer<ServerApiByUid> c) {
        clientTasks.add(new ClientTask(uid, c));
        flush();
    }

    public boolean verifySign(SignedKey signedKey) {
        return CryptoUtils.verifySign(signedKey, clientState.getRootSigners());
    }

    public AFuture sendMessage(UUID uid, byte[] message) {
        return getMessageNode(uid, MessageEventListener.DEFAULT).send(message);
    }

    public CryptoEngine getCryptoEngineForServer(short serverId) {
        var k = getMasterKey();
        var kk = k.getCryptoProvider().createKeyForServer(k, serverId);
        return CryptoEngine.of(kk.getClientToServer().toCryptoEngine(), kk.getServerToClient().toCryptoEngine());
    }

    public long getNextPing() {
        return 0;
    }

    public void setCloud(UUID uid, Cloud cloud) {
        clouds.putResolved(uid, cloud);
    }

    public void putServerDescriptor(ServerDescriptor s) {
        servers.putResolved((int) s.getId(), s);
        clientState.getServerInfo(s.getId()).setDescriptor(s);
    }

    public static AetherCloudClient of(ClientState state) {
        return new AetherCloudClient(state);
    }

    private enum RegStatus {

        NO, BEGIN, CONFIRM
    }

    public static class ClientStartException extends RuntimeException {

        public ClientStartException(String message) {
            super(message);
        }

        public ClientStartException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ClientApiException extends RuntimeException {

        public ClientApiException(String message) {
            super(message);
        }
    }

    public static class ClientTimeoutException extends RuntimeException {

        public ClientTimeoutException(String message) {
            super(message);
        }
    }

    static class ClientTask {

        final UUID uid;

        final AConsumer<ServerApiByUid> task;

        public ClientTask(UUID uid, AConsumer<ServerApiByUid> task) {
            this.uid = uid;
            this.task = task;
        }
    }
}
