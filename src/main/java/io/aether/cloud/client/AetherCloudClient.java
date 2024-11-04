package io.aether.cloud.client;

import io.aether.api.serverRegistryApi.RegistrationResponseLite;
import io.aether.common.Cloud;
import io.aether.common.CryptoLib;
import io.aether.common.Key;
import io.aether.common.ServerDescriptorLite;
import io.aether.logger.Log;
import io.aether.net.meta.ApiManager;
import io.aether.utils.RU;
import io.aether.utils.ThreadSafe;
import io.aether.utils.futures.AFuture;
import io.aether.utils.interfaces.ABiConsumer;
import io.aether.utils.interfaces.AConsumer;
import io.aether.utils.slots.EventBiConsumer;
import io.aether.utils.slots.ARMultiFuture;
import io.aether.utils.streams.BufferedStream;
import io.aether.utils.streams.DownStream;
import io.aether.utils.streams.ElementsStream;
import io.aether.utils.streams.RefreshedValue;
import io.aether.utils.streams.impl.MapBase;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.aether.utils.flow.Flow.flow;

public final class AetherCloudClient {
    private static final List<URI> DEFAULT_URL_FOR_CONNECT = List.of(URI.create("registration.aether.io"));
    public final AtomicBoolean beginCreateUser = new AtomicBoolean();
    public final AFuture startFuture = new AFuture();
    final Map<Integer, ConnectionWork> connections = new ConcurrentHashMap<>();
    final Map<UUID, RefreshedValue<Cloud>> clouds=new ConcurrentHashMap<>();
    final AtomicBoolean tryReg = new AtomicBoolean();
    final AtomicBoolean successfulAuthorization = new AtomicBoolean();
    final MapBase<Integer, ServerDescriptorLite> servers=new MapBase<>(ServerDescriptorLite::idAsInt);
    final AFuture registrationFuture = new AFuture();
    private final ClientConfiguration clientConfiguration;
    private final Collection<ScheduledFuture<?>> scheduledFutures = new HashSet<>();
    private final AtomicBoolean startConnection = new AtomicBoolean();
    public ElementsStream<UUID,DownStream> onNewChildren = ElementsStream.of(ApiManager.UUID);
    public EventBiConsumer<UUID, DownStream> onClientStream=new EventBiConsumer<>();
    Key masterKey;
    long lastSecond;
    private String name;

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

    public ARMultiFuture<ServerDescriptorLite> resolveServer(int serverId) {
        return servers.get(serverId);
    }

    @ThreadSafe
    public void getCloudForUid(@NotNull UUID uid, AConsumer<ServerDescriptorLite> t) {
        getCloud(uid).get(p -> {
            for (var pp : p.data()) {
                servers.get((int)pp).to(t);
            }
        });
    }

    void getConnection(@NotNull UUID uid, @NotNull AConsumer<ConnectionWork> t) {
        getCloudForUid(uid, sd -> {
            t.accept(getConnection(sd));
        });
    }

    ConnectionWork getConnection(@NotNull ServerDescriptorLite serverDescriptor) {
        servers.set((int)serverDescriptor.id(), serverDescriptor);
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
        RU.scheduleAtFixedRate(scheduledFutures, 3, TimeUnit.SECONDS, () -> {
            for (ConnectionWork connection : getConnections()) {
                connection.clearRequests();
            }
        });
    }

    public void ping() {
        getConnection(ConnectionWork::scheduledWork);
    }

    public void changeCloud(Cloud cloud) {
        var uid = getUid();
        assert uid != null;
        updateCloud(uid, cloud);
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
        if (!isRegistered() && tryReg.compareAndSet(false, true)) {
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
            var cloud = clientConfiguration.getCloud(getUid());
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
        getConnection(Objects.requireNonNull(getUid()), c -> {
            c.setBasic(true);
            t.accept(c);
        });
    }

    public Collection<ConnectionWork> getConnections() {
        return connections.values();
    }

    public RefreshedValue<Cloud> getCloud(@NotNull UUID uid) {
        return clouds.get(uid);
    }

    public void updateCloud(@NotNull UUID uid, @NotNull Cloud serverIds) {
        clientConfiguration.setCloud(uid, serverIds);
        getCloud(uid).set(serverIds);
    }

    public long getPingTime() {
        return clientConfiguration.pingDuration;
    }

    public boolean isRegistered() {
        return clientConfiguration.uid != null;
    }

    public void confirmRegistration(RegistrationResponseLite regResp) {
        if (!successfulAuthorization.compareAndSet(false, true)) return;
        Log.trace("confirmRegistration: " + regResp);
        clientConfiguration.uid = regResp.uid();
        clientConfiguration.uid(regResp.uid());
        beginCreateUser.set(false);
        registrationFuture.done();
        assert isRegistered();
        for(var c:regResp.cloud()){
            var server=servers.get((int)c);
        }
        servers.flush();
        flow(regResp.cloud().data())
                .mapToObj(sid -> servers.get((int)sid).toFuture().toFuture())
                .allMap(AFuture::all).to(startFuture::tryDone);
    }

    public void updateCloud(@NotNull UUID uid, @NotNull ServerDescriptorLite @NotNull [] cloud) {
        for (var s : cloud) {
            resolveServer(s.id()).set(s);
        }
        getCloud(uid).set(Cloud.of(flow(cloud).mapToInt(ServerDescriptorLite::id).toShortArray()));
    }

    public DownStream openStreamToClient(@NotNull UUID uid) {
        DownStream res= BufferedStream.of();
        getConnection(uid,s->{
            res.setDownBase(s.openStreamToClient(uid));
        });
        return res;
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
        return getUid();
    }

    public void onClientStream(ABiConsumer<UUID,DownStream>consumer) {
        onClientStream.add(consumer);
    }
}
