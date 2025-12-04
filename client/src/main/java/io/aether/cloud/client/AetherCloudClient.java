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

/**
 * Main client class for connecting to and interacting with the Aether Cloud.
 * Handles connections, registration, API access, and message streaming.
 */
public final class AetherCloudClient implements Destroyable {

    // NESTED EXCEPTION CLASSES FOR DIAGNOSTICS
    // =========================================================================

    /**
     * Delay in ms before resetting the recovery flag after a failure.
     */
    private static final int RECOVERY_RETRY_DELAY_MS = 10000;
    public final AFuture startFuture = AFuture.make();
    public final EventConsumer<MessageNode> onClientStream = new EventConsumerWithQueue<>();
    public final Destroyer destroyer = new Destroyer(getClass().getSimpleName());
    // =========================================================================
    final LNode logClientContext;
    final Map<Integer, ConnectionWork> connections = new ConcurrentHashMap<>();
    final BMap<UUID, Cloud> clouds = RCol.bMap(2000, "CloudCache");
    final AtomicReference<RegStatus> regStatus = new AtomicReference<>(RegStatus.NO);

    // --- Новые BMap для пакетных запросов ---
    final BMap<Integer, ServerDescriptor> servers = RCol.bMap(2000, "ServerCache");
    /**
     * Batched cache for client access groups.
     * Key: Client UUID, Value: Set of Group IDs
     */
    final BMap<UUID, Set<Long>> clientGroups = RCol.bMap(1000, "ClientGroupsCache");
    /**
     * Batched cache for AccessGroup definitions.
     * Key: Group ID, Value: AccessGroup object
     */
    final BMap<Long, AccessGroup> accessGroups = RCol.bMap(1000, "AccessGroupsCache");
    /**
     * Batched cache for all clients accessible by a given client.
     * Key: Client UUID, Value: Set of accessible Client UUIDs
     */
    final BMap<UUID, Set<UUID>> allAccessedClients = RCol.bMap(1000, "AllAccessedClientsCache");
    /**
     * Batched cache for access check results.
     * Key: AccessCheckPair (source, target), Value: Boolean (hasAccess)
     */
    final BMap<AccessCheckPair, Boolean> accessCheckCache = RCol.bMap(2000, "AccessCheckCache");
    final long lastSecond;
    final Map<UUID, MessageNode> messageNodeMap = new ConcurrentHashMap<>();
    final EventConsumer<UUID> onNewChild = new EventConsumer<>();
    final EventBiConsumer<UUID, ServerApiByUid> onNewChildApi = new EventBiConsumer<>();
    // --- Очереди для мутаций ---
    // (Эти очереди будут обрабатываться в ConnectionWork.flushBackgroundRequests)
    final Map<Long, Map<UUID, ARFuture<Boolean>>> accessOperationsAdd = new ConcurrentHashMap<>();
    final Map<Long, Map<UUID, ARFuture<Boolean>>> accessOperationsRemove = new ConcurrentHashMap<>();
    /**
     * Queue for tasks requiring a ServerApiByUid instance.
     * Processed in ConnectionWork.flushBackgroundRequests.
     */
    final Queue<ClientTask> clientTasks = new ConcurrentLinkedQueue<>();
    /**
     * Flag to prevent concurrent recovery storms when state is missing.
     */
    final AtomicBoolean isRecoveryInProgress = new AtomicBoolean(false);
    final AFuture recoveryFuture = AFuture.make();
    final Queue<AConsumer<AuthorizedApi>> authTasks = new ConcurrentLinkedQueue<>();
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
        startFuture.to(this::startScheduledTask); //
    }

    public AetherCloudClient() {
        this(new ClientStateInMemory(StandardUUIDs.ANONYMOUS_UID, List.of(URI.create("tcp://registration.aethernet.io:9010"))));
    }

    public AetherCloudClient(ClientState store) {
        this(store, null);
    }

    public AetherCloudClient(ClientState store, String name) {
        Objects.requireNonNull(store);
        logClientContext = Log.of("SystemComponent", "Client", "ClientName", name);
        try (var ln = logClientContext.context()) {
            this.clientState = store;
            destroyer.add(this::closeConnections);

            /**
             * Pre-populate internal caches from the loaded state.
             * This prevents pending-futures (and deadlocks) if the
             * data is already available.
             */
            populateCachesFromState();

            clouds.forValueUpdate().add(uu -> store.setCloud(uu.key, uu.newValue));

            servers.forValueUpdate().add(s -> {
                var ss = store.getServerInfo(s.key);
                ss.setDescriptor(s.newValue);
            });

            // TODO: Добавить логику сохранения/загрузки для новых BMap (clientGroups, accessGroups) в ClientState,
            // если требуется персистентность. На данный момент они будут только в памяти.

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

    /**
     * Closes all active connections.
     */
    private void closeConnections() {
        connections.values().forEach(c -> c.destroy(true));
        connections.clear();
    }

    /**
     * Populates internal BMap caches (clouds and servers) from the
     * clientState upon initialization. This uses putResolved to
     * avoid triggering update listeners unnecessarily.
     */
    private void populateCachesFromState() {
        if (clientState.getUid() == null) {
            return; // Nothing to populate if not registered
        }

        UUID uid = clientState.getUid();
        for (var clientInfo : clientState.getClientInfoAll()) {
            if (clientInfo != null && clientInfo.getCloud() != null) {
                clouds.putResolved(clientInfo.getUid(), clientInfo.getCloud());
            }
        }
        for (var serverInfo : clientState.getServerInfoAll()) {
            if (serverInfo != null && serverInfo.getDescriptor() != null) {
                servers.putResolved(serverInfo.getServerId(), serverInfo.getDescriptor());
            }
        }

        // TODO: Добавить здесь логику для populateCachesFromState
        // для accessGroups, clientGroups и т.д., если они будут сохраняться.
    }

    /**
     * Retrieves the access groups for a given client UUID using batched requests.
     *
     * @param uid The client UUID.
     * @return A future containing the set of group IDs.
     */
    public ARFuture<Set<Long>> getClientGroups(UUID uid) {
        // Запрос теперь идет через BMap
        return clientGroups.getFuture(uid);
    }

    /**
     * Retrieves all client UUIDs this client can access using batched requests.
     *
     * @param uid The client UUID.
     * @return A future containing the set of accessed client UUIDs.
     */
    public ARFuture<Set<UUID>> getAllAccessedClients(UUID uid) {
        // Запрос теперь идет через BMap
        return allAccessedClients.getFuture(uid);
    }

    /**
     * Checks if this client has permission to send messages to another client using batched requests.
     *
     * @param uid1 The source client UUID.
     * @param uid2 The target client UUID.
     * @return A future containing true if access is granted, false otherwise.
     */
    public ARFuture<Boolean> checkAccess(UUID uid1, UUID uid2) {
        // Используем DTO в качестве ключа для BMap
        return accessCheckCache.getFuture(new AccessCheckPair(uid1, uid2));
    }

    /**
     * Retrieves the AccessGroup details by its ID using batched requests.
     *
     * @param groupId The ID of the access group.
     * @return A future containing the AccessGroup.
     */
    public ARFuture<AccessGroup> getGroup(long groupId) {
        // Запрос теперь идет через BMap
        return accessGroups.getFuture(groupId);
    }

    /**
     * Gets the client state storage interface.
     *
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
     *
     * @param id The server ID.
     * @return A future containing the ServerDescriptor.
     */
    public ARFuture<ServerDescriptor> getServer(int id) {
        var res = servers.getFuture(id); //

        res.timeout(7, () -> { //
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
                servers.getFuture((int) pp).to(t); //
            }
            return;
        }
        getCloud(uid).to(p -> { //
            for (var pp : p.getData()) {
                servers.getFuture((int) pp).to(t, timeout1, () -> Log.warn("timeout server resolve")); //
            }
        }, timeout1, () -> Log.warn("timeout cloud resolve: $uid", "uid", uid));
    }

    /**
     * Retrieves or creates a connection to a work server.
     * NOTE: This method is designed to be called asynchronously after fetching the ServerDescriptor.
     *
     * @param serverDescriptor The descriptor of the server to connect to.
     * @return The existing or newly created ConnectionWork instance.
     * @throws ClientApiException if the provided ServerDescriptor is null.
     */
    ConnectionWork getConnection(@NotNull ServerDescriptor serverDescriptor) {
        // Line 241: This null check is what throws the exception. We fix this
        // by making sure we wait for the descriptor in the caller (connect).
        if (serverDescriptor == null) {
            throw new ClientApiException("Cannot get connection for null ServerDescriptor.");
        }
        putServerDescriptor(serverDescriptor);
        return connections.computeIfAbsent((int) serverDescriptor.getId(),
                s -> {
                    try (var ln = logClientContext.context()) {
                        return new ConnectionWork(this, serverDescriptor);
                    }
                });
    }

    /**
     * Initiates the client connection process.
     *
     * @return An AFuture that completes when the client is ready (registered/connected).
     */
    public AFuture connect() {
        if (!startConnection.compareAndSet(false, true)) return startFuture;
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
        flow(uris).shuffle().limit(countServersForRegistration)
                .map(sd -> new ConnectionRegistration(this, sd)).toCollection(connectionRegistrations);
        return Flow.flow(connectionRegistrations);
    }

    /**
     * Recursive connection attempt logic.
     *
     * @param step The number of remaining retry attempts.
     */
    private void connect(int step) {
        if (destroyer.isDestroyed()) {
            startFuture.cancel(); //
            return;
        }
        if (step == 0) {
            Log.error("All connection attempts failed.");
            if (!startFuture.isFinalStatus()) { //
                startFuture.error(new ClientStartException("All connection attempts failed to register or connect.")); //
            }
            return;
        }

        if (getUid() == null) {
            if (regStatus.compareAndSet(RegStatus.NO, RegStatus.BEGIN)) {
                var regs = makeConnectionReg();
                var timeoutForConnect = clientState.getTimeoutForConnectToRegistrationServer();

                try {
                    var anyFuture = AFuture.any(regs.map(ConnectionRegistration::registration)); //
                    // Propagate statuses to startFuture
                    anyFuture.to(this::startScheduledTask) //
                            .onError(startFuture::error) //
                            .onCancel(startFuture::cancel); //
                    // Timeout logic
                    anyFuture.timeoutMs(timeoutForConnect, () -> { //
                        Log.warn("Failed to connect to registration server: $uris", "uris", clientState.getRegistrationUri());
                        // Retry
                        RU.schedule(1000, () -> this.connect(step - 1));
                    });
                } catch (Exception e) {
                    Log.error("Fatal error during registration setup.", e);
                    if (!startFuture.isFinalStatus()) { //
                        startFuture.error(new ClientStartException("Fatal error during registration setup.", e)); //
                    }
                }
            }
        } else {
            try {
                var uid = getUid();
                if (uid == null) {
                    throw new ClientStartException("UID is null but regStatus is not NO/BEGIN.");
                }
                var cloud = clouds.getFuture(uid).getNow(); //
                boolean isCacheMissingDescriptors = false;
                if (cloud != null && cloud.getData().length > 0) {
                    if (Flow.flow(cloud.getData()).mapToObj(id -> servers.getFuture((int) id).getNow()).filterNotNull().isEmpty()) {
                        isCacheMissingDescriptors = true;
                    }
                }
                if (cloud == null) {
                    Log.info("Recovery required: Cloud missing from cache. Starting recovery.");
                    triggerRecovery().to(startFuture); //
                    return;
                }
                if (isCacheMissingDescriptors) {
                    Log.info("Recovery required: ServerDescriptors missing from cache. Starting recovery.");
                    triggerRecovery().to(startFuture); //
                    return;
                }
                makeFirstConnection();
                startFuture.done();
            } catch (Exception e) {
                Log.error("Fatal error during connection to own cloud.", e);
                if (!startFuture.isFinalStatus()) { //
                    startFuture.error(new ClientStartException("Fatal error during connection to own cloud.", e)); //
                }
                RU.error(e);
            }
        }
    }
    public AFuture forceUpdateStateFromCache() {
        AFuture resultFuture = AFuture.make();
        UUID uid = getUid();

        if (uid == null) {
            Log.warn("forceUpdateStateFromCache called, but client UID is null. Cannot update state.");
            resultFuture.error(new IllegalStateException("Client is not registered, UID is null."));
            return resultFuture;
        }

        Log.debug("forceUpdateStateFromCache: Forcing fetch and save for own UID...", "uid", uid);

        // 1. Получить Cloud (из кэша или сети)
        getCloud(uid).to(cloud -> {
            if (cloud == null) {
                Log.error("forceUpdateStateFromCache: Fetched cloud was null.", "uid", uid);
                resultFuture.error(new IllegalStateException("Fetched cloud was null for UID: " + uid));
                return;
            }

            // 2. Синхронно сохранить Cloud в state
            // Это решает первую часть гонки
            clientState.setCloud(uid, cloud);
            Log.trace("forceUpdateStateFromCache: Own cloud saved to state.", "uid", uid);

            var sids = cloud.getData();
            if (sids == null || sids.length == 0) {
                Log.warn("forceUpdateStateFromCache: Client's own cloud is empty. State updated.", "uid", uid);
                resultFuture.done(); // Завершаем, так как серверов нет
                return;
            }

            // 3. Собрать futures для всех ServerDescriptors
            List<ARFuture<ServerDescriptor>> serverFutures = new ArrayList<>();
            for (var sid : sids) {
                serverFutures.add(getServer(sid));
            }

            // 4. Дождаться получения ВСЕХ дескрипторов (они попадут в BMap-кэш "servers")
            ARFuture.all(serverFutures).to(() -> {
                Log.trace("forceUpdateStateFromCache: All server descriptors fetched. Saving to state...", "uid", uid);

                // 5. Теперь, когда все в кэше, СИНХРОННО сохранить их из кэша в state
                // Это решает вторую часть гонки
                for (var sid : sids) {
                    ServerDescriptor descriptor = getServer(sid).getNow(); // Безопасно, т.к. all() завершился
                    if (descriptor != null) {
                        clientState.getServerInfo(descriptor.getId()).setDescriptor(descriptor);
                    } else {
                        Log.warn("forceUpdateStateFromCache: ServerDescriptor null in cache after fetch.", "sid", sid);
                    }
                }

                // 6. Все данные для "own UID" сохранены в state. Завершаем future.
                Log.debug("forceUpdateStateFromCache: Force update for own UID complete.", "uid", uid);
                resultFuture.done();

            }).onError(e -> {
                Log.error("forceUpdateStateFromCache: Failed to fetch server descriptors.", e, "uid", uid);
                resultFuture.error(e);
            });

        }).onError(e -> {
            Log.error("forceUpdateStateFromCache: Failed to fetch own cloud.", e, "uid", uid);
            resultFuture.error(e);
        });

        return resultFuture;
    }
    /**
     * Initiates the process of resolving the Cloud and ServerDescriptors
     * from a registration server.
     * <p>
     * This method is protected by an AtomicBoolean flag to prevent
     * concurrent "storms" of recovery attempts.
     *
     * @return An AFuture that completes when recovery is successful.
     */
    private AFuture triggerRecovery() {
        if (isRecoveryInProgress.get()) {
            return recoveryFuture;
        }
        Log.info("Starting recovery process to resolve Cloud/ServerDescriptors...");
        var regs = makeConnectionReg();
        var cloud = clientState.getCloud(getUid());
        AFuture recoveryFuture = AFuture.any(regs.map(c -> c.resolveCloud(cloud)));
        recoveryFuture.to(() -> {
            Log.info("Recovery successful. Descriptors resolved.");
            isRecoveryInProgress.set(false);
        }).onError(e -> { //
            Log.error("Recovery attempt failed. Will retry later.", e);
            RU.schedule(RECOVERY_RETRY_DELAY_MS, () -> {
                Log.warn("Resetting recovery flag after failure timeout.");
                isRecoveryInProgress.set(false);
            });
        });
        return recoveryFuture;
    }

    /**
     * Placeholder for scheduled task initiation.
     */
    private void startScheduledTask() {
        if (startScheduledTaskFlag.compareAndSet(false, true)) {
            RU.scheduleAtFixedRate(destroyer, 3, TimeUnit.MILLISECONDS, this::flush);
        }
    }

    /**
     * Retrieves the client's UUID.
     *
     * @return The client's UUID.
     */
    public UUID getUid() {
        return clientState.getUid();
    }

    /**
     * Helper to get an authorized API future and map a function over it.
     * This is used for mutations like createAccessGroup.
     *
     * @param t The function to execute on the AuthorizedApi.
     * @return A future with the result of the function.
     */
    public <T> ARFuture<T> getAuthApi1(@NotNull AFunction<AuthorizedApi, ARFuture<T>> t) {
        if (destroyer.isDestroyed()) return ARFuture.canceled(); //
        ARFuture<T> res = ARFuture.make(); //
        getAuthApiFuture().mapRFuture(t).to(res); //
        return res;
    }

    /**
     * Returns a future that completes with the AuthorizedApi instance.
     * This is now backed by the ConnectionWork's RemoteApiFuture.
     * This method is intended for non-batched calls (mutations) like createAccessGroup.
     *
     * @return A future containing the AuthorizedApi instance.
     */
    public ARFuture<AuthorizedApi> getAuthApiFuture() {
        ARFuture<AuthorizedApi> res = ARFuture.make(); //
        if (destroyer.isDestroyed()) {
            res.cancel(); //
            return res;
        }
        // Enqueue the consumer, which will call res.done(api) later.
        getAuthApi(res::done); //

        // Ensures the future doesn't hang indefinitely.
        res.timeoutError(8, "Timeout waiting for AuthorizedApi to become available."); //

        return res;
    }

    /**
     * Gets a connection, prioritizing one that is already established.
     * If no connection is active, it triggers `makeFirstConnection` and returns null.
     *
     * @return An active ConnectionWork, or null if none are active.
     */
    private ConnectionWork getAnyConnection() {
        if (connections.isEmpty()) {
            // No connections exist, try to make one.
            // The task will be picked up later by the flush mechanism.
            makeFirstConnection();
            return null;
        }

        // Prefer an already-authed connection
        for (var c : connections.values()) {
            if (c.firstAuth) {
                return c;
            }
        }

        // Otherwise, return the first available one
        return connections.values().stream().findFirst().orElse(null);
    }

    /**
     * Enqueues a task to be executed on an active AuthorizedApi.
     * This is used for mutations (like createAccessGroup) that cannot be batched.
     *
     * @param t The consumer to execute with the AuthorizedApi.
     */
    public void getAuthApi(@NotNull AConsumer<AuthorizedApi> t) {
        if (destroyer.isDestroyed()) return;
        authTasks.add(t);
    }

    /**
     * Flushes pending requests and messages to the network connections.
     */
    public void flush() {
        // Проверяем BMap на наличие pending запросов
        if (connections.isEmpty()) {
            if (getUid() == null) return;
            var c = getCloud(getUid()).getNow();
            if (c == null) return;
            var s = getServer(c.getData()[0]).getNow();
            if (s == null) return;
            makeFirstConnection();
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

        // Check BMap cache
        Cloud cloud = clouds.getFuture(uid).getNow(); //

        if (cloud == null || cloud.getData().length == 0) {
            // Cloud cache is empty or cloud has no servers. Trigger recovery.
            Log.warn("makeFirstConnection: Cloud is missing or empty. Triggering recovery.");
            triggerRecovery();
            return;
        }

        int serverId = (int) cloud.getData()[0];

        // Check server cache directly.
        var descriptor = servers.getFuture(serverId).getNow(); //

        if (descriptor != null) {
            // Descriptor is in cache. Proceed with connection.
            getConnection(descriptor).connectFuture.to(startFuture::tryDone);
        } else {
            // Descriptor is missing. Trigger recovery (if not already running).
            // The flush() task will call this method again later.
            Log.warn("makeFirstConnection: ServerDescriptor missing for $id. Triggering recovery.", "id", serverId);
            triggerRecovery();
        }
        return;

    }

    public Collection<ConnectionWork> getConnections() {
        return connections.values();
    }

    /**
     * Retrieves the Cloud descriptor for a given UUID.
     *
     * @param uid The UUID of the cloud owner.
     * @return A future containing the Cloud descriptor.
     */
    public ARFuture<Cloud> getCloud(@NotNull UUID uid) {
        var r = clientState.getCloud(uid);
        if (r != null) return ARFuture.of(r); //
        var res = clouds.getFuture(uid); //

        // Propagate error on timeout
        res.timeout(4, () -> { //
            Log.error("timeout get cloud: $uid", "uid", uid, "client", AetherCloudClient.this);
            res.error(new ClientTimeoutException("Timeout getting cloud for: " + uid)); //
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
     *
     * @param regResp The result from the registration server.
     */
    public void confirmRegistration(FinishResult regResp) {
        if (!regStatus.compareAndSet(RegStatus.BEGIN, RegStatus.CONFIRM)) {
            Log.info("Already registration", "regData", regResp);
            return;
        }
        // Используем putResolved для BMap
        clouds.putResolved(regResp.getUid(), regResp.getCloud()); //

        clientState.setUid(regResp.getUid());
        clientState.setAlias(regResp.getAlias());
        assert isRegistered();
        Log.info("receive my cloud: $cloud", "cloud", regResp.getCloud());

        // FIX: Ensure immediate connection to WorkServer after registration
        var cloud = regResp.getCloud();
        if (cloud != null && cloud.getData().length > 0) {
            for (var serverId : cloud.getData()) {
                getServer((int) serverId).to(this::getConnection).onError(e -> { //
                    Log.warn("Failed to establish WorkServer connection after registration for ID: $id", "id", serverId);
                });
            }
        }

        startFuture.done(); //
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
     *
     * @param force True to force immediate destruction.
     * @return An AFuture that completes when destruction is finished.
     */
    @Override
    public AFuture destroy(boolean force) {
        return destroyer.destroy(force)
                .onError(e -> Log.error("Error during AetherCloudClient destroy.", e)) //
                .onCancel(() -> Log.warn("AetherCloudClient destroy was cancelled.")); //
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
        if (res2 != null) return CryptoUtils.of(res2).asSymmetric();
        var res = CryptoProviderFactory.getProvider(getCryptLib().name()).createSymmetricKey();
        clientState.setMasterKey(CryptoUtils.of(res));
        return res;
    }

    public AetherCloudClient waitStart(int timeout) {
        try {
            startFuture.toCompletableFuture().get(timeout, TimeUnit.SECONDS); //
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
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
        // Используем getAuthApi1 для "мутаций", которые возвращают результат
        return getAuthApi1(c -> c.createAccessGroup(owner, uids)) //
                .map(id -> new AccessGroupImpl(new AccessGroup(owner, id, new UUID[0])) { //
                    @Override
                    public ARFuture<Boolean> add(UUID uuid) {
                        if (data.contains(uuid)) {
                            return ARFuture.of(Boolean.FALSE); //
                        }
                        // Добавляем операцию в очередь
                        ARFuture<Boolean> future = accessOperationsAdd
                                .computeIfAbsent(id, (k) -> new ConcurrentHashMap<>())
                                .computeIfAbsent(uuid, k -> ARFuture.make());

                        // Триггер для немедленной отправки
                        AetherCloudClient.this.flush();
                        return future;
                    }

                    @Override
                    public ARFuture<Boolean> remove(UUID uuid) {
                        if (!data.contains(uuid)) {
                            return ARFuture.of(Boolean.FALSE); //
                        }
                        // Добавляем операцию в очередь
                        ARFuture<Boolean> future = accessOperationsRemove
                                .computeIfAbsent(id, (k) -> new ConcurrentHashMap<>())
                                .computeIfAbsent(uuid, k -> ARFuture.make());

                        // Триггер для немедленной отправки
                        AetherCloudClient.this.flush();
                        return future;
                    }
                });
    }

    public void getClientApi(UUID uid, AConsumer<ServerApiByUid> c) {
        // Добавляем задачу в очередь, ConnectionWork ее обработает
        clientTasks.add(new ClientTask(uid, c));
        // Триггерим flush, чтобы отправить задачу
        flush();
    }

    public boolean verifySign(SignedKey signedKey) {
        return CryptoUtils.verifySign(signedKey, clientState.getRootSigners());
    }

/**
 * Sends a data payload value to a specified client.
 *
 * @param uid     The target client UUID.
 * @param message The message data wrapped in a Value object.
 */
    /**
     * Sends a raw byte array message to a specified client.
     *
     * @param uid     The target client UUID.
     * @param message The raw byte array message.
     * @return An AFuture that completes when the message is accepted for sending.
     */
    public AFuture sendMessage(UUID uid, byte[] message) {
        return getMessageNode(uid, MessageEventListener.DEFAULT).send(message);
    }

    public CryptoEngine getCryptoEngineForServer(short serverId) {
        var k = getMasterKey();
        var kk = k.getCryptoProvider().createKeyForServer(k, serverId);
        return CryptoEngine.of(
                kk.getClientToServer().toCryptoEngine(),
                kk.getServerToClient().toCryptoEngine()
        );
    }

    public long getNextPing() {
        return 0;
    }

    /**
     * Используется для сохранения полученного Cloud.
     * Вызывает putResolved(), который запускает событие forValueUpdate(),
     * и оно уже сохраняет Cloud в clientState.
     *
     * @param uid   UUID облака.
     * @param cloud Объект Cloud.
     */
    public void setCloud(UUID uid, Cloud cloud) {
        clouds.putResolved(uid, cloud); //
    }

    public void putServerDescriptor(ServerDescriptor s) {
        servers.putResolved((int) s.getId(), s); //
        clientState.getServerInfo(s.getId()).setDescriptor(s);
    }

    public static AetherCloudClient of(ClientState state) {
        return new AetherCloudClient(state);
    }

    private enum RegStatus {
        NO,
        BEGIN,
        CONFIRM
    }

    /**
     * Exception related to client startup and connection issues.
     */
    public static class ClientStartException extends RuntimeException {
        public ClientStartException(String message) {
            super(message);
        }

        public ClientStartException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception related to errors occurring during API requests.
     */
    public static class ClientApiException extends RuntimeException {
        public ClientApiException(String message) {
            super(message);
        }

        public ClientApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception related to internal asynchronous operation timeouts.
     */
    public static class ClientTimeoutException extends RuntimeException {
        public ClientTimeoutException(String message) {
            super(message);
        }
    }

    /**
     * Internal class to hold tasks for getClientApi.
     */
    static class ClientTask { // Сделал static, так как нет доступа к non-static AetherCloudClient
        final UUID uid;
        final AConsumer<ServerApiByUid> task;

        public ClientTask(UUID uid, AConsumer<ServerApiByUid> task) {
            this.uid = uid;
            this.task = task;
        }
    }
}