package io.aether.cloud.client;

import io.aether.clientServerApi.serverApi.ServerApiByUid;
import io.aether.clientServerRegApi.serverRegistryApi.RegistrationResponseLite;
import io.aether.common.AccessGroup;
import io.aether.common.Cloud;
import io.aether.common.ServerDescriptor;
import io.aether.common.UUIDAndCloud;
import io.aether.crypt.CryptoLib;
import io.aether.crypt.Key;
import io.aether.crypt.Sign;
import io.aether.crypt.SignedKey;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.ABiConsumer;
import io.aether.utils.interfaces.AConsumer;
import io.aether.utils.interfaces.AFunction;
import io.aether.utils.slots.ARMultiFuture;
import io.aether.utils.slots.EventBiConsumer;
import io.aether.utils.slots.EventConsumer;
import io.aether.utils.streams.Gate;
import io.aether.utils.streams.MapBase;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.aether.utils.flow.Flow.flow;

public final class AetherCloudClient {
    private static final List<URI> DEFAULT_URL_FOR_CONNECT = List.of(URI.create("registration.aether.io"));
    public final AFuture startFuture = new AFuture();
    public final EventConsumer<MessageNode> onClientStream = new EventConsumer<>();
    final Map<Integer, ConnectionWork> connections = new ConcurrentHashMap<>();
    final MapBase<UUID, UUIDAndCloud> clouds = new MapBase<>(UUIDAndCloud::uid).withLog("client clouds");
    final AtomicReference<RegStatus> regStatus = new AtomicReference<>(RegStatus.NO);
    final MapBase<Integer, ServerDescriptor> servers = new MapBase<>(ServerDescriptor::idAsInt);
    final long lastSecond;
    final Map<UUID, MessageNode> messageNodeMap = new ConcurrentHashMap<>();
    final EventConsumer<UUID> onNewChild = new EventConsumer<>();
    final EventBiConsumer<UUID, ServerApiByUid> onNewChildApi = new EventBiConsumer<>();
    private final ClientConfiguration clientConfiguration;
    private final Collection<ScheduledFuture<?>> scheduledFutures = new HashSet<>();
    private final AtomicBoolean startConnection = new AtomicBoolean();
    private final int timeout1 = 5;
    private String name;

    {
        lastSecond = System.currentTimeMillis() / 1000;
    }

    public AetherCloudClient(ClientConfiguration store) {
        this.clientConfiguration = store;
        clouds.output.linkUpHard().toConsumer(uu -> store.setCloud(uu.uid, uu.cloud));
        servers.output.linkUpHard().toConsumer(s -> {
            var ss=store.getServerConfig(s.idAsInt());
            ss.descriptor=s;
        });
        onNewChild.add(u -> getConnection(c -> {
            onNewChildApi.fire(u, c.authorizedApi.client(u));
            c.safeApiCon.flush();
        }));
        connect();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ARMultiFuture<ServerDescriptor> getServer(int id) {
        return servers.get(id);
    }

    public void getServerDescriptorForUid(@NotNull UUID uid, AConsumer<ServerDescriptor> t) {
        if (uid.equals(getUid())) {
            var cloud = clouds.get(uid).getValue();
            for (var pp : cloud.cloud()) {
                t.accept(servers.get((int) pp).getValue());
            }
        }
        getCloud(uid).to(p -> {
            for (var pp : p.data()) {
                servers.get((int) pp).to(t, timeout1, () -> System.out.println("timeout server resolve"));
            }
        }, timeout1, () -> System.out.println("timeout cloud resolve: " + uid));
    }

    void getConnection(@NotNull UUID uid, @NotNull AConsumer<ConnectionWork> t) {
        if (uid.equals(getUid())) {
            var cloud = clouds.get(uid).getValue();
            var s = servers.get((int) cloud.cloud().data()[0]).getValue();
            t.accept(getConnection(s));
            return;
        }
        getServerDescriptorForUid(uid, sd -> {
            var c = getConnection(sd);
            c.ready.to(t::accept, timeout1, () -> System.out.println("timeout ready connection"));
        });
    }

    ConnectionWork getConnection(@NotNull ServerDescriptor serverDescriptor) {
        servers.set((int) serverDescriptor.id(), serverDescriptor);
        var c = connections.get((int) serverDescriptor.id());
        if (c == null) {
            c = connections.computeIfAbsent((int) serverDescriptor.id(),
                    s -> new ConnectionWork(this, serverDescriptor));
        }
        return c;
    }

    void startScheduledTask() {
        startFuture.to(() -> {
            RU.scheduleAtFixedRate(scheduledFutures, getPingTime(), TimeUnit.MILLISECONDS, () -> {
                if (getConnections().isEmpty()) {
                    getConnection(AConsumer.stub());
                }
                for (ConnectionWork connection : getConnections()) {
                    connection.scheduledWork();
                }
            });
        });
    }

    public void ping() {
        getConnection(c -> {
            c.authorizedApi.ping();
            c.flush();
        });
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
            var uris = clientConfiguration.cloudFactoryUrl;
            if (uris == null || uris.isEmpty()) {
                uris = DEFAULT_URL_FOR_CONNECT;
            }
            var timeoutForConnect = clientConfiguration.timoutForConnectToRegistrationServer;
            var countServersForRegistration = Math.min(uris.size(), clientConfiguration.countServersForRegistration);
            if (uris.isEmpty()) throw new RuntimeException("No urls");
            List<URI> finalUris = uris;
            var startFutures = flow(uris).shuffle().limit(countServersForRegistration)
                    .map(sd -> new ConnectionRegistration(this, sd).connectFuture)
                    .toList();
            AFuture.any(startFutures)
                    .to(this::startScheduledTask)
                    .timeout(timeoutForConnect, () -> {
                        System.out.println("Failed to connect to registration server: " + finalUris);
                        RU.schedule(1000, () -> this.connect(step - 1));
                    });
        } else {
            var uid = getUid();
            assert uid != null;
            var cloud = clientConfiguration.getCloud(uid);
            if (cloud == null || cloud.isEmpty()) throw new UnsupportedOperationException();
            for (var serverId : cloud) {
                getConnection(clientConfiguration.getServerDescriptor(serverId));
            }
            startFuture.done();
        }
    }

    public UUID getUid() {
        return clientConfiguration.uid;
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

    public ARMultiFuture<Cloud> getCloud(@NotNull UUID uid) {
        var res = clouds.get(uid).map(UUIDAndCloud::cloud);
        if (!res.isDone()) {
            if (!clouds.requestsOut.existsLinks()) {
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
        return clientConfiguration.pingDuration;
    }

    public boolean isRegistered() {
        return clientConfiguration.uid != null;
    }

    public void confirmRegistration(RegistrationResponseLite regResp) {
        assert !isRegistered();
        if (!regStatus.compareAndSet(RegStatus.BEGIN, RegStatus.CONFIRM)) return;
        clouds.set(new UUIDAndCloud(regResp.uid, regResp.cloud));
        clientConfiguration.uid = regResp.uid;
        clientConfiguration.uid(regResp.uid);
        clientConfiguration.alias = regResp.alias;
        clientConfiguration.alias(regResp.alias);
        assert isRegistered();
        for (var c : regResp.cloud) {
            servers.get((int) c).to(cl -> System.out.println("resolve server: " + cl), 5, () -> System.out.println("timeout resolve server"));
        }
        servers.flush();
        startFuture.done();
    }

    MessageNode getMessageNode(@NotNull UUID uid, MessageEventListener strategy) {
        return messageNodeMap.computeIfAbsent(uid, k -> new MessageNode(this, k, strategy));
    }

    public MessageNode openStreamToClientDetails(@NotNull UUID uid, MessageEventListener strategy) {
        return getMessageNode(uid, strategy);
    }

    public Gate<byte[], byte[]> openStreamToClient(@NotNull UUID uid) {
        return openStreamToClientDetails(uid, MessageEventListener.DEFAULT).up();
    }

    public AFuture stop(int secondsTimeOut) {
        flow(scheduledFutures).foreach(f -> f.cancel(true));
        return flow(connections.values()).map(c -> c.close(secondsTimeOut)).allMap(AFuture::all);
    }

    public boolean isConnected() {
        return getUid() != null;
    }

    public UUID getParent() {
        var res = clientConfiguration.parentUid;
        assert res != null;
        return res;
    }

    public Key getMasterKey() {
        Key res;
        res = clientConfiguration.masterKey;
        if (res != null) return res;
        res = getCryptLib().env.makeSymmetricKey();
        clientConfiguration.masterKey(res);
        return res;
    }

    public AetherCloudClient waitStart(int timeout) {
        startFuture.waitDoneSeconds(timeout);
        return this;
    }

    public CryptoLib getCryptLib() {
        return CryptoLib.SODIUM;
    }

    public UUID getAlias() {
        return clientConfiguration.alias;
    }

    public void onClientStream(AConsumer<MessageNode> consumer) {
        onClientStream.add(consumer);
    }

    public void onNewChildrenApi(ABiConsumer<UUID, ServerApiByUid> consumer) {
        onNewChildApi.add(consumer);
    }

    public void onNewChildren(AConsumer<UUID> consumer) {
        onNewChild.add(consumer);
    }

    public ARFuture<AccessGroup> createAccessGroup(UUID... uids) {
        return getConnectionFun2(c -> c.authorizedApi.createAccessGroup(uids))
                .map(id -> new AccessGroup(id) {
                    @Override
                    public ARFuture<Boolean> add(UUID uid) {
                        if (!data.add(uid)) {
                            return ARFuture.completed(Boolean.FALSE);
                        }
                        return getConnectionFun2(cc -> cc.authorizedApi.addToAccessGroup(id, uid));
                    }

                    @Override
                    public ARFuture<Boolean> remove(UUID uid) {
                        if (!data.remove(uid)) {
                            return ARFuture.completed(Boolean.FALSE);
                        }
                        return getConnectionFun2(cc -> cc.authorizedApi.removeFromAccessGroup(id, uid));
                    }
                });
    }


    static {
        CryptoLib.SODIUM.env.init(
                Key.of("SODIUM_SIGN_PUBLIC:4F202A94AB729FE9B381613AE77A8A7D89EDAB9299C3320D1A0B994BA710CCEB"),
                SignedKey.of(Key.of("SODIUM_CURVE25519_PUBLIC:FC84831B947A12F8F64A4E3A9B5A41AEFB22E9065E6299A07098A69E44AF7B2F"),
                        Sign.of("SODIUM:E493A4FE76130850EBF71A00B509169A1925278911C28734BE5A789AC29F8D0AD13617649477F7BB1F2B8E740FB598D1AC2994F382AC481CFCFDCB1F43B2780E")),
                null
        );
        CryptoLib.HYDROGEN.env.init(
                Key.of("HYDROGEN_SIGN_PUBLIC:883B4D7E0FB04A38CA12B3A451B00942048858263EE6E6D61150F2EF15F40343"),
                SignedKey.of(Key.of("HYDROGEN_CURVE_PUBLIC:FC84831B947A12F8F64A4E3A9B5A41AEFB22E9065E6299A07098A69E44AF7B2F"),
                        Sign.of("HYDROGEN:56798FAD375881FB410814E6C5DFE019426567240117C7C1E3905240E67B490F2789341D08BD4E607AF3B0D30F1E2BD8B7F7DDB1A7A93A0FAA3A7837560CF004")),
                null
        );
    }

    private enum RegStatus {
        NO,
        BEGIN,
        CONFIRM
    }
}
