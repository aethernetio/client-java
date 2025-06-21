package io.aether.cloud.client;

import io.aether.StandardUUIDs;
import io.aether.clientServerApi.serverApi.AuthorizedApi;
import io.aether.clientServerApi.serverApi.ServerApiByUid;
import io.aether.clientServerApi.serverApi.ServerApiByUidClient;
import io.aether.clientServerRegApi.serverRegistryApi.RegistrationResponseLite;
import io.aether.common.*;
import io.aether.crypt.CryptoLib;
import io.aether.crypt.Key;
import io.aether.crypt.SignedKey;
import io.aether.logger.Log;
import io.aether.net.ApiGate;
import io.aether.net.Remote;
import io.aether.net.StreamManager;
import io.aether.utils.Destroyer;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.ABiConsumer;
import io.aether.utils.interfaces.AConsumer;
import io.aether.utils.interfaces.AFunction;
import io.aether.utils.interfaces.Destroyable;
import io.aether.utils.slots.AMFuture;
import io.aether.utils.slots.EventBiConsumer;
import io.aether.utils.slots.EventConsumer;
import io.aether.utils.slots.EventConsumerWithQueue;
import io.aether.utils.streams.Gate;
import io.aether.utils.streams.MapBase;
import io.aether.utils.streams.Value;
import io.aether.utils.streams.ValueListener;
import io.aether.utils.streams.rcollections.RCol;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.aether.utils.flow.Flow.flow;

public final class AetherCloudClient implements Destroyable {
    private static final Log.Node logClientContext = Log.createContext("SystemComponent", "Client");
    public final AFuture startFuture = new AFuture();
    public final EventConsumer<MessageNode> onClientStream = new EventConsumerWithQueue<>();
    public final Destroyer destroyer = new Destroyer(getClass().getSimpleName());
    final Map<Integer, ConnectionWork> connections = new ConcurrentHashMap<>();
    final MapBase<UUID, UUIDAndCloud> clouds = new MapBase<>(UUIDAndCloud::uid).withLog("client clouds");
    final AtomicReference<RegStatus> regStatus = new AtomicReference<>(RegStatus.NO);
    final MapBase<Integer, ServerDescriptor> servers = new MapBase<>(ServerDescriptor::idAsInt);
    final long lastSecond;
    final Map<UUID, MessageNode> messageNodeMap = new ConcurrentHashMap<>();
    final EventConsumer<UUID> onNewChild = new EventConsumer<>();
    final EventBiConsumer<UUID, Remote<ServerApiByUid>> onNewChildApi = new EventBiConsumer<>();
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
        try (var ln = Log.context(logClientContext)) {
            this.clientState = store;
            clouds.addInputHard().toConsumer("Client clouds map input up hard",uu -> store.setCloud(uu.uid, uu.cloud));
            servers.addInputHard().toConsumer("Client servers map input up hard",s -> {
                var ss = store.getServerInfo(s.idAsInt());
                ss.setDescriptor(s);
            });
            onNewChild.add(u -> getConnection(c -> {
                if (onNewChildApi.hasListener()) {
                    getClientApi(u).to(api -> {
                        onNewChildApi.fire(u, api);
                    });
                }
            }));
            connect();
        }
    }

    public ARFuture<Set<Long>> getClientGroups(UUID uid) {
        return getAuthApi1(a -> a.getAccessGroups(uid));
    }

    public ARFuture<Set<UUID>> getAllAccessedClients(UUID uid) {
        return getAuthApi1(a -> a.getAllAccessedClients(uid));
    }

    public ARFuture<Boolean> checkAccess(UUID uid1, UUID uid2) {
        return getAuthApi1(a -> a.checkAccessForSendMessage2(uid1, uid2));
    }

    public ARFuture<AccessGroup> getGroup(long groupId) {
        return getAuthApi1(a -> a.getAccessGroup(groupId));
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

    public AMFuture<ServerDescriptor> getServer(int id) {
        return servers.get(id);
    }

    public void getServerDescriptorForUid(@NotNull UUID uid, AConsumer<ServerDescriptor> t) {
        if (destroyer.isDestroyed()) return;
        if (uid.equals(getUid())) {
            var cloud = clouds.get(uid).getNow();
            for (var pp : cloud.cloud()) {
                t.accept(servers.get((int) pp).getNow());
            }
        }
        getCloud(uid).once(p -> {
            for (var pp : p.data()) {
                servers.get((int) pp).once(t, timeout1, () -> Log.warn("timeout server resolve"));
            }
        }, timeout1, () -> Log.warn("timeout cloud resolve: $uid", "uid", uid));
    }

    void getConnection(@NotNull UUID uid, @NotNull AConsumer<ConnectionWork> t) {
        if (destroyer.isDestroyed()) return;
        if (uid.equals(getUid())) {
            UUIDAndCloud cloud0 = clouds.get(uid).getNow();
            Cloud cloud = null;
            if (cloud0 == null) {
                cloud = clientState.getCloud(uid);
            } else {
                cloud = cloud0.cloud;
            }
            var s = servers.get((int) cloud.data()[0]).getNow();
            t.accept(getConnection(s));
            return;
        }
        getServerDescriptorForUid(uid, sd -> {
            var c = getConnection(sd);
            c.ready.once(t::accept, timeout1, () -> Log.warn("timeout ready connection"));
        });
    }

    ConnectionWork getConnection(@NotNull ServerDescriptor serverDescriptor) {
        servers.set((int) serverDescriptor.id(), serverDescriptor);
        var c = connections.get((int) serverDescriptor.id());
        if (c == null) {
            c = connections.computeIfAbsent((int) serverDescriptor.id(),
                    s -> {
                        try (var ln = Log.context(logClientContext)) {
                            return new ConnectionWork(this, serverDescriptor);
                        }
                    });
        }
        return c;
    }

    void startScheduledTask() {
        startFuture.to(() -> {
            RU.scheduleAtFixedRate(destroyer, getPingTime(), TimeUnit.MILLISECONDS, () -> {
                if (getConnections().isEmpty()) {
                    getConnection(AConsumer.stub());
                }
                for (ConnectionWork connection : getConnections()) {
                    connection.scheduledWork();
                }
            });
            RU.scheduleAtFixedRate(destroyer, 5, TimeUnit.MILLISECONDS, () -> {
                for (ConnectionWork connection : getConnections()) {
                    connection.safeApiCon.flush();
                }
            });
        });
    }

    public AFuture ping() {
        AFuture res = new AFuture();
        getConnection(connectionWork -> connectionWork.ping().to(res));
        return res;
    }

    public AFuture connect() {
        if (!startConnection.compareAndSet(false, true)) return startFuture;
        connect(10);
        return startFuture;
    }

    private void connect(int step) {
        if (step == 0) {
            return;
        }
        if (!isRegistered() && regStatus.compareAndSet(RegStatus.NO, RegStatus.BEGIN)) {
            var uris = clientState.getRegistrationUri();
            if (uris == null || uris.isEmpty()) {
                throw new IllegalStateException("Registration uri is void");
            }
            var timeoutForConnect = clientState.getTimeoutForConnectToRegistrationServer();
            var countServersForRegistration = Math.min(uris.size(), clientState.getCountServersForRegistration());
            if (uris.isEmpty()) throw new RuntimeException("No urls");
            var startFutures = flow(uris).shuffle().limit(countServersForRegistration)
                    .map(sd -> {
                        try (var ln = Log.context(logClientContext)) {
                            return new ConnectionRegistration(this, sd).connectFuture;
                        }
                    })
                    .toList();
            AFuture.any(startFutures)
                    .to(this::startScheduledTask)
                    .timeoutMs(timeoutForConnect, () -> {
                        Log.error("Failed to connect to registration server: $uris", "uris", uris);
                        RU.schedule(1000, () -> this.connect(step - 1));
                    });
        } else {
            var uid = getUid();
            assert uid != null;
            var cloud = clientState.getCloud(uid);
            if (cloud == null || cloud.isEmpty()) throw new UnsupportedOperationException();
            for (var serverId : cloud) {
                getConnection(clientState.getServerDescriptor(serverId));
            }
            startFuture.done();
        }
    }

    public UUID getUid() {
        return clientState.getUid();
    }

    public <T> ARFuture<T> getAuthApi1(@NotNull AFunction<AuthorizedApi, ARFuture<T>> t) {
        if (destroyer.isDestroyed()) return ARFuture.canceled();
        ARFuture<T> res = new ARFuture<>();
        getAuthApi(a -> t.apply(a).to(res));
        return res;
    }

    public void getAuthApi(@NotNull AConsumer<AuthorizedApi> t) {
        if (destroyer.isDestroyed()) return;
        getConnection(c -> {
            var a = c.safeApiCon;
            if (a == null) {
                c.ready.toOnce((aa) -> {
                    aa.safeApiCon.runRt(t::accept);
                });
            } else {
                a.runRt(t::accept);
            }
        });
    }

    public <T> ARFuture<T> getAuthApi2(@NotNull AFunction<AuthorizedApi, T> t) {
        if (destroyer.isDestroyed()) return ARFuture.canceled();
        ARFuture<T> res = new ARFuture<>();
        getAuthApi(a -> res.done(t.apply(a)));
        return res;
    }

    <T> ARFuture<T> getConnectionFun2(@NotNull AFunction<ConnectionWork, ARFuture<T>> t) {
        ARFuture<T> res = new ARFuture<>();
        getConnection(c -> t.apply(c).to(res));
        return res;
    }

    <T> ARFuture<T> getConnectionFun(@NotNull AFunction<ConnectionWork, T> t) {
        ARFuture<T> res = new ARFuture<>();
        getConnection(c -> res.done(t.apply(c)));
        return res;
    }

    void getConnection(@NotNull AConsumer<ConnectionWork> t) {
        if (destroyer.isDestroyed()) return;
        var uid0 = getUid();
        getConnection(Objects.requireNonNull(uid0), c -> {
            c.setBasic(true);
            t.accept(c);
            c.flush();
        });
    }

    public Collection<ConnectionWork> getConnections() {
        return connections.values();
    }

    public AMFuture<Cloud> getCloud(@NotNull UUID uid) {
        var f = clouds.get(uid);
        var res = f.map(UUIDAndCloud::cloud);
        if (!res.isDone()) {
            if (connections.isEmpty()) {
                getConnection(conWork -> {
                    clouds.flush();
                    conWork.flush();
                });
            } else {
                clouds.flush();
            }
        }
        return res;
    }

    public long getPingTime() {
        return clientState.getPingDuration().getNow();
    }

    public boolean isRegistered() {
        return clientState.getUid() != null;
    }

    public void confirmRegistration(RegistrationResponseLite regResp) {
        assert !isRegistered();
        if (!regStatus.compareAndSet(RegStatus.BEGIN, RegStatus.CONFIRM)) return;
        clouds.set(new UUIDAndCloud(regResp.uid, regResp.cloud));
        clientState.setUid(regResp.uid);
        clientState.setAlias(regResp.alias);
        assert isRegistered();
        Log.info("receive my cloud: $cloud", "cloud", regResp.cloud);
        for (var c : regResp.cloud) {
            servers.get((int) c).once(cl -> Log.info("resolve server", "server", cl), 5, () -> Log.warn("timeout resolve server"));
        }
        servers.flush();
        startFuture.done();
    }

    MessageNode getMessageNode(@NotNull UUID uid, MessageEventListener strategy) {
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

    public Gate<byte[], byte[]> openStreamToClient(@NotNull UUID uid) {
        return openStreamToClientDetails(uid, MessageEventListener.DEFAULT).up();
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
        assert res != null;
        return res;
    }

    public Key getMasterKey() {
        Key res;
        res = clientState.getMasterKey();
        if (res != null) return res;
        res = getCryptLib().env.makeSymmetricKey();
        clientState.setMasterKey(res);
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
            m.up().toConsumer(d -> {
                consumer.accept(m.getConsumerUUID(), d);
            });
        });
    }

    public void onClientStream(AConsumer<MessageNode> consumer) {
        onClientStream.add(consumer);
        ping();
    }

    public void onNewChildrenApi(ABiConsumer<UUID, Remote<ServerApiByUid>> consumer) {
        onNewChildApi.add(consumer);
        ping();
    }

    public void onNewChildren(AConsumer<UUID> consumer) {
        onNewChild.add(consumer);
        ping();
    }

    public ARFuture<AccessGroupI> createAccessGroup(UUID... uids) {
        return createAccessGroupWithOwner(getUid(), uids);
    }

    public ARFuture<AccessGroupI> createAccessGroupWithOwner(UUID owner, UUID... uids) {
        return getAuthApi1(c -> c.createAccessGroup(owner, uids))
                .map(id -> new AccessGroupImpl(new AccessGroup(owner, id, RCol.set())) {
                    @Override
                    public ARFuture<Boolean> add(UUID uuid) {
                        if (accessGroup.contains(uuid)) {
                            return ARFuture.completed(Boolean.FALSE);
                        }
                        return getAuthApi1(cc -> cc.addToAccessGroup(id, uuid).apply((v) -> accessGroup.add(uuid)));
                    }

                    @Override
                    public ARFuture<Boolean> remove(UUID uuid) {
                        if (!accessGroup.contains(uuid)) {
                            return ARFuture.completed(Boolean.FALSE);
                        }
                        return getAuthApi1(cc -> cc.removeFromAccessGroup(id, uuid).apply((v) -> accessGroup.add(uuid)));
                    }
                });
    }

    public ARFuture<Remote<ServerApiByUid>> getClientApi(UUID uid) {
        ARFuture<Remote<ServerApiByUid>> res = new ARFuture<>();
        getAuthApi(auth -> {
            var apiFuture = auth.client(uid);
            apiFuture.to(apiCon -> {
                var a = RU.<ApiGate<ServerApiByUidClient, ServerApiByUid>>cast(apiCon.gate.find(ApiGate.class));
                if (a == null) {
                    a = ApiGate.of(ServerApiByUidClient.META, ServerApiByUid.META, ServerApiByUidClient.INSTANCE, StreamManager.forClient(), apiCon.gate);
                }
                res.done(a.getRemoteApi());
            });
        });
        return res;
    }

    public boolean verifySign(SignedKey signedKey) {
        for (var e : clientState.getRootSigners()) {
            if (e.cryptoLib() == signedKey.cryptoLib() && e.check(signedKey)) return true;
        }
        return false;
    }

    public AFuture sendMessage(UUID uid, byte[] message) {
        AFuture res = new AFuture();
        openStreamToClient(uid).send(Value.ofForce(message).link(new ValueListener() {
            @Override
            public void drop(Object owner) {
                res.done();
            }
        }));
        return res;
    }

    public static AetherCloudClient of(ClientState state) {
        return new AetherCloudClient(state);
    }

    private enum RegStatus {
        NO,
        BEGIN,
        CONFIRM
    }
}
