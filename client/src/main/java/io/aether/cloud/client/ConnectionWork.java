package io.aether.cloud.client;

import io.aether.clientServerApi.clientApi.ClientApiSafe;
import io.aether.clientServerApi.clientApi.ClientApiUnsafe;
import io.aether.clientServerApi.serverApi.AuthorizedApi;
import io.aether.clientServerApi.serverApi.LoginApi;
import io.aether.common.AetherCodec;
import io.aether.common.Cloud;
import io.aether.common.ServerDescriptor;
import io.aether.common.UUIDAndCloud;
import io.aether.logger.Log;
import io.aether.net.ApiGate;
import io.aether.net.Remote;
import io.aether.net.StreamManager;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.slots.AMFuture;
import io.aether.utils.streams.CryptoNode;
import io.aether.utils.streams.SCD;
import io.aether.utils.streams.Value;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ConnectionWork extends Connection<ClientApiUnsafe, LoginApi> implements ClientApiUnsafe {
    //region counters
    public final AtomicLong lastBackPing = new AtomicLong(Long.MAX_VALUE);
    public final AMFuture<ConnectionWork> ready = new AMFuture<>();
    private final ServerDescriptor serverDescriptor;
    final private AtomicBoolean inProcess = new AtomicBoolean();
    boolean basicStatus;
    long lastWorkTime;
    volatile ApiGate<ClientApiSafe, AuthorizedApi> safeApiCon;

    public ConnectionWork(AetherCloudClient client, ServerDescriptor s) {
        super(client, s.ipAddress.getURI(AetherCodec.UDP), ClientApiUnsafe.class, LoginApi.class);
        serverDescriptor = s;
        this.basicStatus = false;
        connect();
    }

    public AFuture ping() {
        AFuture res = new AFuture();
        ready.toOnce((aa) -> {
            safeApiCon.getRemoteApi().run_flush(a -> {
                a.ping(client.getPingTime()).to(res);
            });
        });
        return res;
    }

    public void flush() {
        var api = safeApiCon;
        if (api != null) {
            api.flush();
        }
    }

    @Override
    public void sendSafeApiDataMulti(byte backId, Value<byte[]> data) {
        if (!Objects.equals(backId, client.getAlias())) {
            throw new IllegalStateException();
        }
        CryptoNode cp = safeApiCon.findDown(CryptoNode.class);
        cp.down().send(data);
    }

    @Override
    public void sendSafeApiData(Value<byte[]> data) {
        CryptoNode cp = safeApiCon.findDown(CryptoNode.class);
        cp.down().send(data);
    }

    @Override
    protected void onConnect(Remote<LoginApi> remoteApi) {
        try (var ln = Log.context(client.logClientContext)) {
            var mk = client.getMasterKey();
            safeApiCon = ApiGate.of(ClientApiSafe.META, AuthorizedApi.META,
                    new MyClientApiSafe(client), StreamManager.forClient(),
                    CryptoNode.of(mk.getType().cryptoLib().env.symmetricForClient(mk, serverDescriptor.id())).setName("client to workServer"));
            CryptoNode cp = safeApiCon.findDown(CryptoNode.class);
            cp.down().toSubApi(remoteApi, (a, v) -> a.loginByAlias2(client.getAlias(), SCD.of(v)));
            client.servers.addSourceHard().log("resolver servers", "client", "server")
                    .toMethod(safeApiCon, (a, sid) ->
                            a.resolverServers(sid.map2(new short[]{sid.data().shortValue()})));
            client.clouds.addSourceHard().log("resolver clouds", "client", "server",
                            v -> {
                                Log.debug("");
                            }, v -> {
                                Log.debug("");
                            })
                    .toMethod(safeApiCon, (a, uid) -> {
                        a.resolverClouds(uid.map(v -> new UUID[]{v}));
                    });
            ready.set(ConnectionWork.this);
        }
    }

    public ServerDescriptor getServerDescriptor() {
        return serverDescriptor;
    }

    @Override
    public String toString() {
        return "work(" + socketStreamClient + ")";
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
        try {
            lastWorkTime = t;
            var c = safeApiCon;
            if (c.getRemoteApiMgr().isEmpty()) {
                ping();
            } else {
                c.flush();
            }
        } finally {
            inProcess.set(false);
        }
    }

    private static class MyClientApiSafe implements ClientApiSafe {
        private final AetherCloudClient client;

        public MyClientApiSafe(AetherCloudClient client) {
            this.client = client;
        }

        @Override
        public void changeParent(UUID uid) {

        }

        @Override
        public void changeAlias(UUID alias) {

        }

        @Override
        public void requestTelemetric() {

        }

        @Override
        public void sendMessage(UUID uid, Value<byte[]> data) {
            Log.trace("receive message $uid1 <- $uid2", "uid1", client.getUid(), "uid2", uid);
            client.getMessageNode(uid, MessageEventListener.DEFAULT).sendMessageFromServerToClient(data);
        }

        @Override
        public void sendServerDescriptor(ServerDescriptor v) {
            client.servers.set(v);
        }

        @Override
        public void sendCloud(UUID uid, Cloud cloud) {
            client.clouds.set(new UUIDAndCloud(uid, cloud));
        }

        @Override
        public void newChild(UUID uid) {
            client.onNewChild.fire(uid);
        }

    }
}
