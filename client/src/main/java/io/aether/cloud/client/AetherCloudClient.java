package io.aether.cloud.client;

import io.aether.StandardUUIDs;
import io.aether.api.CryptoUtils;

import io.aether.api.clientserverapi.AuthorizedApi;
import io.aether.api.clientserverapi.AuthorizedApiRemote;
import io.aether.api.clientserverapi.RegistrationDirectResult;
import io.aether.api.clientserverapi.ServerApiByUid;
import io.aether.api.clientserverapi.ServerApiByUidClient;

import io.aether.api.common.*;
import io.aether.api.clienttypes.PeerIdentityCredentials;
import io.aether.common.AccessGroupI;
import io.aether.crypto.AKey;
import io.aether.crypto.CryptoEngine;
import io.aether.crypto.CryptoProviderFactory;
import io.aether.crypto.SignedKey;
import io.aether.crypto.SignChecker;
import io.aether.logger.LNode;
import io.aether.logger.Log;
import io.aether.net.fastMeta.FastMetaApi;
import io.aether.net.fastMeta.MetaContext;
import io.aether.net.fastMeta.RemoteApi;
import io.aether.utils.ConcurrentHashSet;




import io.aether.utils.Destroyer;
import io.aether.utils.RU;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.*;
import io.aether.utils.rcollections.BMap;
import io.aether.utils.rcollections.RCol;
import io.aether.utils.slots.EventBiConsumer;
import io.aether.utils.slots.EventConsumer;
import io.aether.utils.slots.EventConsumerWithQueue;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class AetherCloudClient implements Destroyable {

    private static final int RECOVERY_RETRY_DELAY_MS = 10000;
    private static final URI DEFAULT_REG_URI = URI.create("tcp://registration.aethernet.io:9010");
    public final AFuture startFuture = AFuture.make();
    public final EventConsumer<MessageNode> onClientStream = new EventConsumerWithQueue<>();
    public final Destroyer destroyer = new Destroyer(getClass().getSimpleName());
    public final AtomicBoolean isRecoveryInProgress = new AtomicBoolean(false);
    public final AFuture recoveryFuture = AFuture.make();
    final LNode logClientContext;
    final Map<Integer, ConnectionWork> connections = new ConcurrentHashMap<>();
    public final BMap<UUID, ClientCloud> clouds = RCol.bMap("CloudCache");
    final AtomicReference<RegStatus> regStatus = new AtomicReference<>(RegStatus.NO);
    public final BMap<Integer, ServerDescriptor> servers = RCol.bMap("ServerCache");
    public final BMap<UUID, Set<UUID>> clientGroups = RCol.bMap("ClientGroupsCache");
    public final BMap<UUID, AccessGroup> accessGroups = RCol.bMap("AccessGroupsCache");
    public final BMap<UUID, Set<UUID>> allAccessedClients = RCol.bMap("AllAccessedClientsCache");
    public final BMap<AccessCheckPair, Boolean> accessCheckCache = RCol.bMap("AccessCheckCache");
    final long lastSecond;
    final Map<UUID, MessageNode> messageNodeMap = new ConcurrentHashMap<>();
    final EventConsumer<UUID> onNewChild = new EventConsumer<>();
    final EventBiConsumer<UUID, ServerApiByUid> onNewChildApi = new EventBiConsumer<>();
    final Map<UUID, Map<UUID, ARFuture<Boolean>>> accessOperationsAdd = new ConcurrentHashMap<>();
    final Map<UUID, Map<UUID, ARFuture<Boolean>>> accessOperationsRemove = new ConcurrentHashMap<>();
    final BMap<AppliedConfig, Boolean> appliedConfigsRequests = new BMap<>("AppliedConfigsRequests", 5000, 10000);
    final Queue<AConsumer<AuthorizedApiRemote>> authTasks = new ConcurrentLinkedQueue<>();
    final CloudPriorityManager priorityManager = new CloudPriorityManager();
    private final ClientState clientState;





    private final PeerIdentityManager peerIdentityManager;


    public final io.aether.utils.slots.AMFuture<io.aether.api.common.ServerDescriptorWithGeo[]> serversMFuture
            = new io.aether.utils.slots.AMFuture<>();


    private final java.util.Map<java.util.UUID, io.aether.utils.slots.AMFuture<io.aether.api.clientserverapi.WebRtcSession>> webRtcSessionsByUid
            = new java.util.concurrent.ConcurrentHashMap<>();

    public final io.aether.utils.slots.AMFuture<java.util.UUID> newAccessGroupIdMFuture
            = new io.aether.utils.slots.AMFuture<>();
    public final io.aether.utils.slots.AMFuture<IpInfo> myIpMFuture
            = new io.aether.utils.slots.AMFuture<>();


    public final io.aether.utils.slots.AMFuture<Boolean> pingAckMFuture
            = new io.aether.utils.slots.AMFuture<>();


    public final io.aether.utils.slots.AMFuture<Boolean> openReceiveWindowAckMFuture
            = new io.aether.utils.slots.AMFuture<>();


    public final io.aether.utils.slots.AMFuture<Boolean> addServersToCloudAckMFuture
            = new io.aether.utils.slots.AMFuture<>();


    public final io.aether.utils.slots.AMFuture<Boolean> removeServersFromCloudAckMFuture
            = new io.aether.utils.slots.AMFuture<>();


    public final io.aether.utils.slots.AMFuture<Boolean> selfDestructAckMFuture
            = new io.aether.utils.slots.AMFuture<>();


    public final io.aether.utils.slots.AMFuture<Boolean> setAliasAckMFuture
            = new io.aether.utils.slots.AMFuture<>();


    public final io.aether.utils.slots.AMFuture<Boolean> setParentAckMFuture
            = new io.aether.utils.slots.AMFuture<>();


    public final io.aether.utils.slots.AMFuture<Boolean> blockAckMFuture
            = new io.aether.utils.slots.AMFuture<>();


    public final io.aether.utils.slots.AMFuture<Boolean> unblockAckMFuture
            = new io.aether.utils.slots.AMFuture<>();


    public final io.aether.utils.slots.AMFuture<Boolean> setBeneficiaryAckMFuture
            = new io.aether.utils.slots.AMFuture<>();


    public final io.aether.utils.slots.AMFuture<Boolean> setMsgQueueLimitAckMFuture
            = new io.aether.utils.slots.AMFuture<>();


    public final io.aether.utils.slots.AMFuture<Boolean> setMsgTimeLimitAckMFuture
            = new io.aether.utils.slots.AMFuture<>();

    public final BMap<java.util.UUID, io.aether.api.common.CloudConfig> cloudPositions
            = RCol.bMap("CloudPositionsCache");


    public final io.aether.utils.slots.AMFuture<java.util.UUID> aliasMFuture
            = new io.aether.utils.slots.AMFuture<>();
    public final BMap<java.util.UUID, io.aether.api.clientserverapi.Uap> uaps
            = RCol.bMap("UapsCache");
    public final BMap<java.util.UUID, io.aether.api.clientserverapi.ClientTiming> clientTimings
            = RCol.bMap("ClientTimingsCache");
    public final BMap<java.util.UUID, io.aether.api.clientserverapi.ClientActivity[]> clientActivities
            = RCol.bMap("ClientActivitiesCache");
    public final BMap<java.util.UUID, io.aether.api.clientserverapi.ClientLogEntry[]> clientLogs
            = RCol.bMap("ClientLogsCache");
    public final BMap<java.util.UUID, io.aether.api.clientserverapi.ClientConnectionInfo[]> clientConnections
            = RCol.bMap("ClientConnectionsCache");
    public final BMap<java.util.UUID, io.aether.api.clientserverapi.MessageInfo[]> clientMessages
            = RCol.bMap("ClientMessagesCache");
    public final BMap<java.util.UUID, Boolean> accessCheckAcks
            = RCol.bMap("AccessCheckAcksCache");
    public final BMap<io.aether.utils.tuples.Tuple2<java.util.UUID, java.util.UUID>, Boolean> addToAccessGroupResults
            = RCol.bMap("AddToAccessGroupResultsCache");
    public final BMap<io.aether.utils.tuples.Tuple2<java.util.UUID, java.util.UUID>, Boolean> removeFromAccessGroupResults
            = RCol.bMap("RemoveFromAccessGroupResultsCache");








    private final AtomicReference<ConnectionPeer> connectionPeer = new AtomicReference<>();

    /** uids for which a LAN probe was requested before the peer stack started. */
    private final java.util.Set<UUID> pendingProbes = java.util.concurrent.ConcurrentHashMap.newKeySet();


    public ConnectionPeer getConnectionPeer() {
        return connectionPeer.get();
    }


    /**
     * Pin the long-term signing public key for a peer UID.
     */


    /**
     * Invoked by {@link PeerIdentityManager} once the server-signed uid
     * credential has been issued and persisted. From this point on the
     * peer-to-peer stack may be started.
     */

    void onPeerIdentityReady(PeerIdentityCredentials[] creds) {
        if (creds == null || creds.length == 0 || destroyer.isDestroyed()) {
            return;
        }
        ensureConnectionPeer(creds);
    }


    /**
     * Lazily create the {@link ConnectionPeer} that runs the peer-to-peer
     * broadcast / handshake stack. Idempotent.
     */


    private void ensureConnectionPeer(PeerIdentityCredentials[] creds) {
        if (destroyer.isDestroyed()) {
            return;
        }
        if (connectionPeer.get() != null) {
            return;
        }
        ConnectionPeer fresh = new ConnectionPeer(this, creds);

        if (connectionPeer.compareAndSet(null, fresh)) {
            try {
                fresh.start();

                destroyer.add(fresh::close);
                for (UUID pending : pendingProbes) {
                    try {
                        fresh.ensureProbing(pending);
                    } catch (RuntimeException e) {
                        Log.warn("pending LAN probe failed", e);
                    }
                }
                pendingProbes.clear();

            } catch (RuntimeException error) {
                connectionPeer.compareAndSet(fresh, null);
                try {
                    fresh.close();
                } catch (RuntimeException ignored) {
                }
                Log.warn("ConnectionPeer start failed; peer-to-peer unavailable", error);
            }
            return;
        }
        try {
            fresh.close();
        } catch (RuntimeException ignored) {
        }
    }




    public void probeLanFor(UUID targetUid) {
        ConnectionPeer peer = connectionPeer.get();
        Log.info("probeLanFor: called",
                "targetUid", targetUid,
                "connectionPeer", peer == null ? "null" : "present",
                "myUid", clientState.getUid());
        if (peer != null) {
            peer.ensureProbing(targetUid);
        } else {
            pendingProbes.add(targetUid);
            Log.info("probeLanFor: queued until ConnectionPeer starts", "targetUid", targetUid);
        }
    }








    private final int timeout1 = 6;
    private final int loginApiVersion;

    private final AtomicBoolean startScheduledTaskFlag = new AtomicBoolean();

    private final Set<ConnectionRegistration> connectionRegistrations = new ConcurrentHashSet<>();

    private final WorkConnectionRouter workConnectionRouter;
    private final ARFuture<Connection<?, ?>> anyConnection = ARFuture.make();
    private boolean beginConnect;
    private String name;




    {
        lastSecond = System.currentTimeMillis() / 1000;
        startFuture.to(this::startScheduledTask);






    }

    public AetherCloudClient() {
        this(new ClientStateInMemory(StandardUUIDs.ANONYMOUS_UID, List.of(URI.create("tcp://registration.aethernet.io:9010"))));
    }

    public AetherCloudClient(ClientState state) {
        this(state, null);
    }


    public AetherCloudClient(
            ClientState state,
            String name
    ) {
        this(
                state,
                name,
                0
        );
    }


    public AetherCloudClient(
            ClientState state,
            String name,
            int loginApiVersion
    ) {
        this(state, name, loginApiVersion, WorkConnectionRouter.DIRECT);
    }

    public AetherCloudClient(
            ClientState state,
            String name,
            WorkConnectionRouter workConnectionRouter
    ) {
        this(state, name, 0, workConnectionRouter);
    }


    public AetherCloudClient(
            ClientState state,
            String name,
            int loginApiVersion,
            WorkConnectionRouter workConnectionRouter
    ) {

        Objects.requireNonNull(state);

        if (loginApiVersion < 0) {
            throw new IllegalArgumentException(
                    "loginApiVersion must be non-negative: "
                            + loginApiVersion
            );
        }

        this.clientState = state;

        this.peerIdentityManager = new PeerIdentityManager(this);
        this.destroyer.add(peerIdentityManager);

        this.name = name;
        this.loginApiVersion = loginApiVersion;
        this.workConnectionRouter = Objects.requireNonNull(workConnectionRouter);

        logClientContext =
                Log.of(
                        "SystemComponent",
                        "Client",
                        "ClientName",
                        name
                );

        try (var ln = logClientContext.context()) {
            destroyer.add(this::closeConnections);
            populateCachesFromState();

            onNewChild.add(u -> {
                if (onNewChildApi.hasListener()) {
                    // TODO Я удалил очередь задач. Нужно сделать прямой выбор ConnectionWork
                    // getClientApi(u, api -> {
                    //     onNewChildApi.fire(u, api);
                    // });
                }
            });

            connect();
        }

        startFuture.to(() -> {
            forceUpdateStateFromCache().to(state::saveState);
        });

        startFuture.to(() -> {
            var idFuture = peerIdentityManager.getOrIssue();
            idFuture.onError(e -> Log.warn(
                    "Peer identity issuance failed; will retry on demand",
                    "error", String.valueOf(e)));
        });

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
                clouds.put(c.getUid(), c.getCloud());
                clientState.saveCloud(c.getCloud());
            }
        }
        for (var s : clientState.getServerInfoAll()) {
            if (s.getDescriptor() != null)
                putServerDescriptor(s.getDescriptor());
        }
    }

    public ARFuture<Set<UUID>> getClientGroups(UUID uid) {
        return clientGroups.getFuture(uid);
    }

    public ARFuture<Set<UUID>> getAllAccessedClients(UUID uid) {
        return allAccessedClients.getFuture(uid);
    }

    public ARFuture<Boolean> checkAccess(UUID uid1, UUID uid2) {
        return accessCheckCache.getFuture(new AccessCheckPair(uid1, uid2));
    }

    public ARFuture<AccessGroup> getGroup(UUID groupId) {
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
        int sid = serverDescriptor.getId();
        putServerDescriptor(serverDescriptor);
        return connections.computeIfAbsent(sid, s -> {
            try (var ln = logClientContext.context()) {


                var resolved = workConnectionRouter.resolve(serverDescriptor);
                ConnectionWork conn = (resolved != null)
                        ? new ConnectionWork(
                                this,
                                serverDescriptor,
                                resolved
                        )
                        : new ConnectionWork(this, serverDescriptor);


                conn.stateListeners.add(isWritable -> {
                    if (!isWritable) {
                        UUID uid = getUid();
                        if (uid != null) {
                            Log.info("Connection to server failed or lost. Demoting SID and attempting failover.", "sid", sid);
                            priorityManager.demote(uid, (short) sid);
                            makeFirstConnection();
                        }
                    }
                });
                anyConnection.tryDone(conn);
                return conn;
            }
        });
    }

    public AFuture connect() {
        if (beginConnect) return startFuture;
        beginConnect = true;
        connect(1);
        return startFuture;
    }

    private Flow<ConnectionRegistration> makeConnectionReg() {
        if (connectionRegistrations.isEmpty()) {
            List<java.net.URI> uris = clientState.getRegistrationUri();
            uris.stream()
                    .limit(3)
                    .map(uri -> {
                        var res = new ConnectionRegistration(this, uri);
                        anyConnection.tryDone(res);
                        return res;
                    })
                    .forEach(connectionRegistrations::add);
        }
        return Flow.flow(connectionRegistrations);
    }

    private void connect(int step) {
        if (destroyer.isDestroyed()) {
            startFuture.error(new CancellationException("is destroyed"));
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

                    var anyFuture =
                            AFuture.any(
                                    regs.map(
                                            ConnectionRegistration
                                                    ::registration
                                    )
                            );

                    anyFuture.to(
                            this::startScheduledTask
                    );

                    anyFuture.onError(error -> {
                        if (
                                !(error instanceof
                                        java.util.concurrent
                                                .TimeoutException)
                        ) {
                            startFuture.tryError(error);
                        }
                    });

                    anyFuture.timeoutMs(
                            timeoutForConnect,
                            () -> {
                                regStatus.compareAndSet(
                                        RegStatus.BEGIN,
                                        RegStatus.NO
                                );

                                Log.warn(
                                        "Failed to connect to "
                                                + "registration server: "
                                                + "$uris",
                                        "uris",
                                        clientState
                                                .getRegistrationUri()
                                );


                                RU.schedule(
                                        100,
                                        () -> connect(step)
                                );

                            }
                    );

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
                startFuture.tryDone();
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
            clientState.saveCloud(cloud);
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
        AFuture recoveryFutureLocal;
        if (cloud != null) {
            // Есть облако в кэше - пытаемся разрешить сервера
            recoveryFutureLocal = AFuture.any(regs.map(c -> c.resolveCloud(cloud)));
        } else {
            // Нет облака - выполняем полную регистрацию заново
            Log.info("Cloud missing from cache, performing full re-registration.");
            Log.info("TriggerRecovery: cloud is null, uid=$uid, performing full re-registration.", "uid", getUid());
            regStatus.set(RegStatus.BEGIN);
            AFuture regDone = AFuture.any(regs.map(ConnectionRegistration::registration));
            recoveryFutureLocal = regDone.to(clouds.getFuture(getUid()).toFuture());
            Log.info("TriggerRecovery: re-registration done, new uid=$uid, waiting for cloud in cache.", "uid", getUid());
        }
        recoveryFutureLocal.to(() -> {
            Log.info("Recovery successful.");
            isRecoveryInProgress.set(false);
            recoveryFuture.tryDone();
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

    PeerIdentityManager peerIdentityManager() {
        return peerIdentityManager;
    }


    public UUID getUid() {
        return clientState.getUid();
    }



    public ARFuture<PeerIdentityCredentials> getPeerIdentity() {
        return peerIdentityManager.getOrIssue();
    }




    public void withAuthApi(@NotNull AConsumer<AuthorizedApi> t) {
        getAuthApiRemote(t::accept);
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

    public ARFuture<Connection<?, ?>> getAnyConnection() {
        return anyConnection;
    }


    public void getAuthApi(@NotNull AConsumer<AuthorizedApi> t) {
        getAuthApiRemote(t::accept);
    }

    private void getAuthApiRemote(@NotNull AConsumer<AuthorizedApiRemote> t) {
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
            c.flushBackgroundRequests();
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
            short[] orderedSids = priorityManager.getOrderedSids(uid, cloud.toCloud());
            // Connect to ALL servers in the cloud to ensure message delivery from any path
            for (short sid : orderedSids) {
                getServer((int) sid).to(descriptor -> {
                    ConnectionWork conn = getConnection(descriptor);
                    // For the primary (top) server, link it to the client's start status
                    if (sid == orderedSids[0]) {
                        startFuture.tryDone();
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

    public ARFuture<ClientCloud> getCloud(@NotNull UUID uid) {
        var cached = clientState.getCloud(uid);
        if (cached != null) {
            return ARFuture.of(cached);
        }

        ARFuture<ClientCloud> result = ARFuture.make();

        Future<ClientCloud> request = new Future<>() {
            @Override
            public void onResolved(ClientCloud value) {
                result.tryDone(value);
            }

            @Override
            public void onError(int time, Exception error) {
                if (destroyer.isDestroyed()) {
                    result.cancel();
                    return;
                }

                Log.warn(
                        "Timeout getting cloud, retrying: $uid",
                        "uid", uid
                );

                RU.schedule(1000, () -> {
                    if (result.isFinalStatus() || destroyer.isDestroyed()) {
                        return;
                    }

                    ClientCloud current = clientState.getCloud(uid);
                    if (current != null) {
                        result.tryDone(current);
                        return;
                    }

                    clouds.get(uid, 10, this);
                });
            }
        };

        clouds.get(uid, 10, request);
        return result;
    }


    public int getLoginApiVersion() {
        return loginApiVersion;
    }


    public long getPingTime() {
        return clientState.getPingDuration().getNow();
    }

    public boolean isRegistered() {
        return clientState.getUid() != null;
    }

    public void confirmRegistration(RegistrationDirectResult regResp) {
        if (!regStatus.compareAndSet(RegStatus.BEGIN, RegStatus.CONFIRM)) {
            Log.info("Already registration");
            return;
        }

        Log.info("confirm registration: $uid", "uid", regResp.getUid());
        var clientCloud = new ClientCloud(regResp.getUid(), regResp.getCloud());
        clouds.put(regResp.getUid(), clientCloud);
        // Сохраняем тот же объект, который используется runtime-cache.
        clientState.saveCloud(clientCloud);

        clientState.setUid(regResp.getUid());
        clientState.setAlias(regResp.getAlias());
        var cloud = regResp.getCloud();
        if (cloud != null && cloud.getData().length > 0) {
            for (var serverId : cloud.getData()) {
                getServer(serverId).to(this::getConnection);
            }
        }
        startFuture.tryDone();
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
        newAccessGroupIdMFuture.refresh();

        withAuthApi(c -> c.createAccessGroup(owner, uids));

        return newAccessGroupIdMFuture.mapToARFuture().map(id ->
                new AccessGroupImpl(new AccessGroup(id, RU.timeSeconds(), owner, new UUID[0])) {

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

    public boolean verifySign(SignedKey signedKey) {
        return CryptoUtils.verifySign(signedKey, clientState.getRootSigners());
    }


    public ARFuture<io.aether.api.clientserverapi.WebRtcSession> requestWebRtcSession(UUID uid) {
        io.aether.utils.slots.AMFuture<io.aether.api.clientserverapi.WebRtcSession> future =
                webRtcSessionsByUid.computeIfAbsent(uid, k -> new io.aether.utils.slots.AMFuture<>());
        ARFuture<io.aether.api.clientserverapi.WebRtcSession> result = future.mapToARFuture();
        result.timeoutError(timeout1, "Timeout WebRTC session " + uid);
        result.onError(e -> webRtcSessionsByUid.remove(uid, future));
        getServerDescriptorForUid(uid, sd -> {
            ConnectionWork connection = connections.get(sd.getId());
            if (connection == null) connection = getConnection(sd);
            connection.authorizedApi.requestWebRtcSession(uid);
        });
        return result;
    }

    /** Called from {@code ClientApiSafeImpl.sendWebRtcSession}: routes an incoming
     *  signaling message to the request that is waiting for it. */
    void onWebRtcSessionReceived(UUID fromUid, io.aether.api.clientserverapi.WebRtcSession session) {
        webRtcSessionsByUid.computeIfPresent(fromUid, (k, f) -> {
            if (!f.isDone()) f.set(session);
            return null;
        });
    }

    /** Publishes our local WebRTC session to every server in our own cloud. */
    public AFuture publishWebRtcSession(io.aether.api.clientserverapi.WebRtcSession session) {
        AFuture result = AFuture.make();
        UUID uid = getUid();
        if (uid == null) {
            result.error(new IllegalStateException("Client is not registered"));
            return result;
        }
        getCloud(uid).to(cloud -> {
            if (cloud == null || cloud.getData().length == 0) {
                result.error(new IllegalStateException("Client cloud is empty"));
                return;
            }
            List<AFuture> publishes = new ArrayList<>();
            for (short serverIdShort : cloud.getData()) {
                int serverId = (int) serverIdShort;
                AFuture published = AFuture.make();
                publishes.add(published);
                servers.getFuture(serverId).to(descriptor -> {
                    try {
                        ConnectionWork connection = connections.get(serverId);
                        if (connection == null) connection = getConnection(descriptor);
                        connection.authorizedApi.publishWebRtcSession(session);
                        published.done();
                    } catch (RuntimeException ex) {
                        published.error(ex);
                    }
                }, timeout1, () -> published.error(
                        new java.util.concurrent.TimeoutException("publish timeout server=" + serverId)));
            }
            AFuture.all(publishes.toArray(new AFuture[0])).to(result::done, result::error);
        }, timeout1, () -> result.error(
                new java.util.concurrent.TimeoutException("Cloud lookup for publishWebRtcSession")));
        return result;
    }



    public ARFuture<ServerDescriptorWithGeo[]> getServers() {

        withAuthApi(AuthorizedApi::getServers);

        return serversMFuture.mapToARFuture();
    }



    public AFuture sendMessage(UUID uid, byte[] message) {
        return getMessageNode(uid, MessageEventListener.DEFAULT).send(message);
    }


    public ARFuture<ServerApiByUid> getClientApi(UUID uid) {
        ARFuture<ServerApiByUid> result = ARFuture.make();
        getClientApi(uid, result::done);
        result.timeoutError(8, "Timeout waiting for ServerApiByUid.");
        return result;
    }

    @Deprecated
    public void getClientApi(UUID uid, AConsumer<ServerApiByUid> callback) {
        getAuthApiRemote(api -> callback.accept(
                api.openClient(
                        uid,
                        remote -> new ClientApiSafeImpl(null, this),
                        data -> data,
                        "byClient",
                        uid
                )
        ));
    }


    public CryptoEngine getCryptoEngineForServer(short serverId) {
        var k = getMasterKey();
        if (k == null) {
            Log.error("Cannot create crypto engine for server " + serverId + ": master key is null");
            return null;
        }
        Log.debug("Creating crypto engine for server", "sid", serverId, "masterKeyProvider", k.getCryptoProvider().getCryptoLibName());
        var kk = k.getCryptoProvider().createKeyForServer(k, serverId);
        if (kk == null || kk.getClientToServer() == null || kk.getServerToClient() == null) {
            Log.error("Derived keys are null for server", "sid", serverId, "pair", kk);
            return null;
        }
        var engine = CryptoEngine.of(kk.getClientToServer().toCryptoEngine(), kk.getServerToClient().toCryptoEngine());
        Log.debug("Crypto engine created successfully", "sid", serverId);
        return engine;
    }

    public long getNextPing() {
        return 0;
    }

    public void setCloud(UUID uid, Cloud cloud) {
        ClientCloud cc = clouds.getNow(uid);
        if (cc != null) {
            cc.applyCloudConfig(
                    new CloudConfig(uid, 0, cloud),
                    appliedConfigsRequests
            );
        } else {
            cc = new ClientCloud(uid, cloud);
            clouds.put(uid, cc);
        }

        clientState.saveCloud(cc);
    }


    void applyCloudConfig(CloudConfig config) {
        UUID uid = config.getSubjectUid();
        ClientCloud cloud = clouds.getNow(uid);

        long previousConfigVersion =
                cloud != null ? cloud.getConfigVersion() : -1;


        if (cloud != null) {
            cloud.applyCloudConfig(
                    config,
                    appliedConfigsRequests
            );
        } else {
            cloud = new ClientCloud(uid, config.getCloud());
            cloud.setConfigVersion(config.getConfigVersion());
            clouds.put(uid, cloud);
        }

        clientState.saveCloud(cloud);




        if (config.getConfigVersion() > previousConfigVersion) {
            priorityManager.updateCloudFromWork(
                    uid,
                    cloud.toCloud()
            );

            if (uid.equals(getUid())) {
                makeFirstConnection();
            }
        }


    }


    public void requestCloudConfig(UUID subjectUid) {
        ClientCloud cc = clouds.getNow(subjectUid);
        long version = cc != null ? cc.getConfigVersion() - 1 : -1;
        appliedConfigsRequests.getFuture(new AppliedConfig(subjectUid, version));
    }


    public void putServerDescriptor(ServerDescriptor s) {
        servers.put((int) s.getId(), s);
        clientState.getServerInfo(s.getId()).setDescriptor(s);
        java.util.List<String> keyHexes = new java.util.ArrayList<>();
        if (s.getSigningKeys() != null) {
            for (var k : s.getSigningKeys()) {
                if (k != null) keyHexes.add(io.aether.api.CryptoUtils.of(k.getKey()).getProviderName() + ":" +
                        io.aether.utils.HexUtils.toHexString(k.getKey().getData()));
            }
        }
        Log.info("AetherCloudClient: putServerDescriptor",
                "sid", s.getId(),
                "keys", keyHexes);
    }


    public ARFuture<IpInfo> getMyIp() {
        myIpMFuture.refresh();
        for (var conn : connections.values()) {
            if (conn.isWritable()) {
                conn.getRootApi().getMyIp();
                return myIpMFuture.mapToARFuture();
            }
        }
        getAnyConnection().to(c -> {
            if (c instanceof ConnectionWork cw && cw.isWritable()) {
                cw.getRootApi().getMyIp();
            } else if (c instanceof ConnectionRegistration cr && cr.isWritable()) {
                cr.getRootApi().getMyIp();
            }
        });
        return myIpMFuture.mapToARFuture();
    }



    public void reportAppliedConfig(AppliedConfig[] configs) {
        for (AppliedConfig ac : configs) {
            ClientCloud cc = clouds.getNow(ac.getSubjectUid());
            if (cc != null && ac.getConfigVersion() > cc.getConfirmedConfigVersion()) {
                cc.updateConfirmedConfigVersion(ac.getConfigVersion());
            }
            appliedConfigsRequests.put(ac, true);
        }
    }

    public static AetherCloudClient of(ClientState state) {
        return new AetherCloudClient(state);
    }

    public static <LT, RT extends RemoteApi> AetherCloudClient asClient(
            UUID parentUid, String name,
            FastMetaApi<LT, ? extends LT> localMeta,
            FastMetaApi<?, RT> remoteMeta,
            AFunction<RT, LT> localApiFactory) {
        return asClient(parentUid, DEFAULT_REG_URI, name, localMeta, remoteMeta, localApiFactory);
    }

    public static <LT, RT extends RemoteApi> AetherCloudClient asClient(
            UUID parentUid, URI regUri, String name,
            FastMetaApi<LT, ? extends LT> localMeta,
            FastMetaApi<?, RT> remoteMeta,
            AFunction<RT, LT> localApiFactory) {
        ClientStateInFile state = new ClientStateInFile(parentUid, List.of(regUri), new java.io.File("state-" + name + ".bin"));
        AetherCloudClient client = new AetherCloudClient(state, name);
        client.startFuture.to(() -> {
            MessageNode node = client.getMessageNode(parentUid);
            node.toApiR(localMeta, ctx -> {
                var remoteApi = ctx.makeRemote(remoteMeta);
                return localApiFactory.apply(remoteApi);
            });
        });
        return client;
    }

    public static <LT, RT extends RemoteApi> AetherCloudClient asClient(
            ClientState state, String name,
            FastMetaApi<LT, ? extends LT> localMeta,
            FastMetaApi<?, RT> remoteMeta,
            AFunction<RT, LT> localApiFactory) {
        AetherCloudClient client = new AetherCloudClient(state, name);
        client.startFuture.to(() -> {
            MessageNode node = client.getMessageNode(state.getParentUid());
            node.toApiR(localMeta, ctx -> {
                RT remoteApi = ctx.makeRemote(remoteMeta);
                return localApiFactory.apply(remoteApi);
            });
        });
        return client;
    }

    public static <LT> AetherCloudClient asServer(
            ClientState state, String name,
            FastMetaApi<LT, ? extends LT> serviceMeta,
            AFunction<MetaContext, LT> localApiFactory) {
        AetherCloudClient client = new AetherCloudClient(state, name);
        client.onClientStream(node -> {
            node.toApiR(serviceMeta, localApiFactory);
        });
        return client;
    }


    public static <LT> AetherCloudClient asServer(
            UUID parentUid, String name,
            FastMetaApi<LT, ? extends LT> serviceMeta,
            AFunction<MetaContext, LT> localApiFactory) {
        return asServer(parentUid, DEFAULT_REG_URI, name, serviceMeta, localApiFactory);
    }

    public static <LT> AetherCloudClient asServer(
            UUID parentUid, URI regUri, String name,
            FastMetaApi<LT, ? extends LT> serviceMeta,
            AFunction<MetaContext, LT> localApiFactory) {
        ClientStateInFile state = new ClientStateInFile(parentUid, List.of(regUri), new java.io.File("state-" + name + ".bin"));
        AetherCloudClient client = new AetherCloudClient(state, name);
        client.onClientStream(node -> {
            node.toApiR(serviceMeta, localApiFactory);
        });
        return client;
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
}