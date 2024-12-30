package io.aether.cloud.client;

import io.aether.api.serverRegistryApi.RegistrationResponseLite;
import io.aether.common.*;
import io.aether.logger.Log;
import io.aether.net.meta.ApiManager;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.interfaces.ABiConsumer;
import io.aether.utils.interfaces.AConsumer;
import io.aether.utils.slots.ARMultiFuture;
import io.aether.utils.slots.EventBiConsumer;
import io.aether.utils.streams.Gate;
import io.aether.utils.streams.MapBase;
import io.aether.utils.streams.Node;
import io.aether.utils.streams.Serializer2Node;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.aether.utils.flow.Flow.flow;

public final class AetherCloudClient {
    static final Executor executor = RU.newSingleThreadExecutor("AetherCloudClient");
    private static final List<URI> DEFAULT_URL_FOR_CONNECT = List.of(URI.create("registration.aether.io"));
    public final AFuture startFuture = new AFuture();
    public final EventBiConsumer<UUID, Gate<byte[], byte[]>> onClientStream = new EventBiConsumer<>();
    final Map<Integer, ConnectionWork> connections = new ConcurrentHashMap<>();
    final MapBase<UUID, UUIDAndCloud> clouds = new MapBase<>(UUIDAndCloud::uid).withLog("client clouds");
    final AtomicReference<RegStatus> regStatus = new AtomicReference<>(RegStatus.NO);
    final MapBase<Integer, ServerDescriptorLite> servers = new MapBase<>(ServerDescriptorLite::idAsInt);
    final long lastSecond;
    private final ClientConfiguration clientConfiguration;
    private final Collection<ScheduledFuture<?>> scheduledFutures = new HashSet<>();
    private final AtomicBoolean startConnection = new AtomicBoolean();
    private final int timeout1 = 5;
    Serializer2Node<UUID, ?> onNewChildren = Serializer2Node.of(ApiManager.UUID);
    Key masterKey;
    private String name;
    static{
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
    {
        lastSecond = System.currentTimeMillis() / 1000;
    }

    public AetherCloudClient(ClientConfiguration store) {
        this.clientConfiguration = store;
        connect();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void getServerDescriptorForUid(@NotNull UUID uid, AConsumer<ServerDescriptorLite> t) {
        if (uid.equals(getUid())) {
            var cloud = clouds.get(uid).getValue();
            for (var pp : cloud.cloud()) {
                t.accept(servers.get((int) pp).getValue());
            }
        }
        getCloud(uid).to(p -> {
            Log.info(new Log.Info("get cloud for uid") {
                final UUID uid0 = uid;
                final Cloud cloud = p;
            });
            for (var pp : p.data()) {
                servers.get((int) pp).to(t, timeout1, () -> Log.error(new Log.Error("timeout server resolve") {
                    final int sid = pp;
                }));
            }
        }, timeout1, () -> Log.error(new Log.Error("timeout cloud resolve") {
            final UUID uid0 = uid;
        }));
    }

    void getConnection(@NotNull UUID uid, @NotNull AConsumer<ConnectionWork> t) {
        if (uid.equals(getUid())) {
            var cloud = clouds.get(uid).getValue();
            var s = servers.get((int) cloud.cloud().data()[0]).getValue();
            t.accept(getConnection(s));
            return;
        }
        getServerDescriptorForUid(uid, sd -> {
            Log.info(new Log.Info("get connection for uid") {
                final UUID uid0 = uid;
                final ServerDescriptorLite serverDescriptorLite = sd;
            });
            var c = getConnection(sd);
            c.ready.to(t::accept, timeout1, () -> Log.error("timeout ready connection"));
        });
    }

    ConnectionWork getConnection(@NotNull ServerDescriptorLite serverDescriptor) {
        servers.set((int) serverDescriptor.id(), serverDescriptor);
        var c = connections.get((int) serverDescriptor.id());
        if (c == null) {
            c = connections.computeIfAbsent((int) serverDescriptor.id(),
                    s -> new ConnectionWork(this, serverDescriptor));
        }
        return c;
    }

    void startScheduledTask() {
        RU.scheduleAtFixedRate(scheduledFutures, getPingTime(), TimeUnit.MILLISECONDS, () -> {
//			getConnection(Connection::scheduledWork);
            for (ConnectionWork connection : getConnections()) {
                connection.scheduledWork();
            }
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
            Log.info(new Log.Info("try registration") {
                final URI[] uriList = finalUris.toArray(new URI[0]);
            });
            var startFutures = flow(uris).shuffle().limit(countServersForRegistration)
                    .map(sd -> new ConnectionRegistration(this, sd).connectFuture)
                    .toList();
            AFuture.any(startFutures)
                    .to(this::startScheduledTask)
                    .timeout(timeoutForConnect, () -> {
                        Log.error(new Log.Error("Failed to connect to registration server") {
                            final URI[] uriList = finalUris.toArray(new URI[0]);
                        });
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
        }
    }

    public UUID getUid() {
        return clientConfiguration.uid;
    }

    void getConnection(@NotNull AConsumer<ConnectionWork> t) {
        var uid0 = getUid();
        getConnection(Objects.requireNonNull(uid0), c -> {
            Log.info(new Log.Info("get default connection for uid") {
                final UUID uid = uid0;
            });
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
                    Log.info("the first connection has been received");
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
        Log.trace("confirmRegistration: " + regResp);
        clouds.set(new UUIDAndCloud(regResp.uid(), regResp.cloud()));
        clientConfiguration.uid = regResp.uid();
        clientConfiguration.uid(regResp.uid());
        clientConfiguration.alias = regResp.alias();
        clientConfiguration.alias(regResp.alias());
        assert isRegistered();
        for (var c : regResp.cloud()) {
            servers.get((int) c).to(cl -> Log.info("resolve server: " + cl), 5, () -> Log.error("timeout resolve server"));
        }
        servers.flush();
        flow(regResp.cloud().data())
                .mapToInt()
                .mapToObj(sid -> servers.get(sid).toFuture().toFuture())
                .allMap(AFuture::all).to(startFuture::tryDone);
    }

    public Gate<byte[], byte[]> openStreamToClient(@NotNull UUID uid) {
        var buf = Node.bufferBytes();
        getConnection(uid, s -> {
            Log.info("link client channel to api stream: " + uid);
            buf.down().link(s.openStreamToClient(uid));
        });
        return buf.up();
    }

    public AFuture stop(int secondsTimeOut) {
        flow(scheduledFutures).foreach(f -> f.cancel(true));
        return flow(connections.values()).map(c -> c.close(secondsTimeOut)).allMap(AFuture::all);
    }

    public boolean isConnected() {
        return getUid() != null;
    }

    public UUID getParent() {
        return clientConfiguration.parentUid;
    }

    public Key getMasterKey() {
        Key res;
        res = masterKey;
        if (res != null) return res;
        res = clientConfiguration.masterKey;
        if (res == null) {
            res = getCryptLib().env.makeSymmetricKey();
            clientConfiguration.masterKey(res);
        }
        masterKey = res;
        return res;
    }

    public AetherCloudClient waitStart(int timeout) {
        startFuture.waitDoneSeconds(timeout);
        return this;
    }

    public CryptoLib getCryptLib() {
        return CryptoLib.HYDROGEN;
    }

    public UUID getAlias() {
        return clientConfiguration.alias;
    }

    public void onClientStream(ABiConsumer<UUID, Gate<byte[], byte[]>> consumer) {
        onClientStream.add(consumer);
    }

    public Serializer2Node<UUID, ?> getOnNewChildren() {
        return onNewChildren;
    }

    public Executor getExecutor() {
        return executor;
    }

    private enum RegStatus {
        NO,
        BEGIN,
        CONFIRM
    }
}
