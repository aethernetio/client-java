package io.aether.cloud.client;

import io.aether.clientServerApi.clientApi.ClientApiSafe;
import io.aether.clientServerApi.clientApi.ClientApiUnsafe;
import io.aether.clientServerApi.serverApi.AuthorizedApi;
import io.aether.clientServerApi.serverApi.LoginApi;
import io.aether.common.AetherCodec;
import io.aether.common.ServerDescriptor;
import io.aether.common.UUIDAndCloud;
import io.aether.net.ApiGate;
import io.aether.net.ApiLevelConsumer;
import io.aether.net.meta.ApiManager;
import io.aether.net.serialization.ApiLevel;
import io.aether.utils.Inject;
import io.aether.utils.RU;
import io.aether.utils.slots.ARMultiFuture;
import io.aether.utils.streams.ApiNode;
import io.aether.utils.streams.CryptoNode;
import io.aether.utils.streams.Gate;
import io.aether.utils.streams.SerializerNode;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ConnectionWork extends Connection<ClientApiUnsafe, LoginApi> implements ClientApiUnsafe {
    //region counters
    public final AtomicLong lastBackPing = new AtomicLong(Long.MAX_VALUE);
    public final ARMultiFuture<ConnectionWork> ready = new ARMultiFuture<>();
    private final ServerDescriptor serverDescriptor;
    final private AtomicBoolean inProcess = new AtomicBoolean();
    private final Map<UUID, Gate<byte[], byte[]>> messageGates = new ConcurrentHashMap<>();
    boolean basicStatus;
    long lastWorkTime;
    io.aether.clientServerApi.serverApi.AuthorizedApi authorizedApi;
    ApiGate<ClientApiSafe, AuthorizedApi> safeApiCon;

    public ConnectionWork(AetherCloudClient client, ServerDescriptor s) {
        super(client, s.ipAddress.getURI(AetherCodec.BINARY), ClientApiUnsafe.class, LoginApi.class);
        serverDescriptor = s;
        this.basicStatus = false;
        connect();
    }

    public void ping() {
        safeApiCon.runRt(r -> {
            authorizedApi.ping();
        });
    }

    public Gate<byte[], byte[]> openMessageChannel(UUID uid) {
        return messageGates.computeIfAbsent(uid, k -> {
//            var b = BufferNodeAutoFlush.<byte[], byte[]>of();
//            safeApiCon.runRt(r -> {
            var st = safeApiCon.newStream();
            authorizedApi.openStreamToClient(k, st);
//                b.down().link(st);
//            });
//            return b.up();
            return st;
        });
    }

    public void flush() {
        safeApiCon.flush();
    }

    @Override
    protected void onConnect(LoginApi remoteApi) {
        var mk = client.getMasterKey();
        ApiNode<ClientApiSafe, AuthorizedApi, CryptoNode<?>> authorizedApiNode = ApiNode.of(ClientApiSafe.META, AuthorizedApi.META,
                CryptoNode.of(mk.getType().cryptoLib().env.symmetricForClient(mk, serverDescriptor.id())));
        remoteApi.loginByAlias(authorizedApiNode, client.getAlias());
        safeApiCon = authorizedApiNode.forClient(new MyClientApiSafe(client));
        authorizedApi = safeApiCon.getRemoteApi();
        SerializerNode<Short, ServerDescriptor, ?> serversNode = SerializerNode.of(ApiManager.SHORT, ServerDescriptor.META, safeApiCon.newStream());
        client.servers.addSourceHard(serversNode.forClient().mapRead(Integer::shortValue));
        SerializerNode<UUID, UUIDAndCloud, ?> cloudsNode = SerializerNode.of(ApiManager.UUID, UUIDAndCloud.META, safeApiCon.newStream());
        client.clouds.addSourceHard(cloudsNode.forClient());
        authorizedApi.resolvers(serversNode, cloudsNode);
        safeApiCon.flush();
        ready.set(this);
    }

    public ServerDescriptor getServerDescriptor() {
        return serverDescriptor;
    }

    @Override
    public String toString() {
        return "C(" + lifeTime() + ")";
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
                authorizedApi.ping();
            }
            c.flush();
        } finally {
            inProcess.set(false);
        }
    }

    private class MyClientApiSafe implements ClientApiSafe, ApiLevelConsumer {
        private final AetherCloudClient client;
        @Inject
        private ApiLevel apiProcessor;

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
        public void newChild(UUID uid) {
            client.onNewChild.fire(uid);
        }

        @Override
        public void setApiLevel(ApiLevel apiLevel) {
            this.apiProcessor = apiLevel;
        }

        @Override
        public void streamToClient(@NotNull UUID uid, @NotNull Gate<byte[], byte[]> stream) {
            var mn = client.getMessageNode(uid, MessageEventListener.DEFAULT);
            mn.addConnectionIn(ConnectionWork.this, stream);
            client.onClientStream.fire(mn);
        }

    }
}
