package io.aether.cloud.client;

import io.aether.api.clientApi.ClientApiSafe;
import io.aether.api.clientApi.ClientApiUnsafe;
import io.aether.api.serverApi.AuthorizedApi;
import io.aether.api.serverApi.LoginApi;
import io.aether.common.AetherCodec;
import io.aether.common.ServerDescriptorLite;
import io.aether.common.UUIDAndCloud;
import io.aether.logger.Log;
import io.aether.net.ApiGate;
import io.aether.net.ApiLevelConsumer;
import io.aether.net.RemoteApi;
import io.aether.net.impl.bin.ApiLevel;
import io.aether.net.meta.ApiManager;
import io.aether.utils.RU;
import io.aether.utils.slots.ARMultiFuture;
import io.aether.utils.streams.ApiNode;
import io.aether.utils.streams.CryptoNode;
import io.aether.utils.streams.Gate;
import io.aether.utils.streams.SerializerNode;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ConnectionWork extends Connection<ClientApiUnsafe, LoginApi> implements ClientApiUnsafe {

    //region counters
    public final AtomicLong lastBackPing = new AtomicLong(Long.MAX_VALUE);
    public final ARMultiFuture<ConnectionWork> ready = new ARMultiFuture<>();
    private final ServerDescriptorLite serverDescriptor;
    final private AtomicBoolean inProcess = new AtomicBoolean();
    boolean basicStatus;
    long lastWorkTime;
    AuthorizedApi authorizedApi;
    ApiGate<ClientApiSafe, AuthorizedApi> safeApiCon;

    public ConnectionWork(AetherCloudClient client, ServerDescriptorLite s) {
        super(client, s.ipAddress().getURI(AetherCodec.BINARY), ClientApiUnsafe.class, LoginApi.class);
        Log.info(new Log.Info("new connection") {
            final ServerDescriptorLite serverDescriptor = s;
        });
        serverDescriptor = s;
        this.basicStatus = false;
        connect();
    }

    public Gate<byte[], byte[]> openStreamToClient(UUID uid) {
        var con = RemoteApi.of(authorizedApi).getConnection();
        var res = con.newStream().base.up();
        authorizedApi.openStreamToClient(uid, res);
        con.flush();
        return res;
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
        SerializerNode<Short, ServerDescriptorLite, ?> serversNode = SerializerNode.of(ApiManager.SHORT, ServerDescriptorLite.META, safeApiCon.newStream().base.up());
        client.servers.addSource(serversNode.forServer().mapRead(Integer::shortValue));
        SerializerNode<UUID, UUIDAndCloud, ?> cloudsNode = SerializerNode.of(ApiManager.UUID, UUIDAndCloud.META, safeApiCon.newStream().base.up());
        client.clouds.addSource(cloudsNode.forServer());
        authorizedApi.resolvers(serversNode, cloudsNode);
        Log.debug("work connection is ready");
        ready.set(this);
    }

    public ServerDescriptorLite getServerDescriptor() {
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
        } finally {
            inProcess.set(false);
        }
    }

    private static class MyClientApiSafe implements ClientApiSafe, ApiLevelConsumer {
        private final AetherCloudClient client;
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

        }

        @Override
        public void setApiLevel(ApiLevel apiLevel) {
            this.apiProcessor = apiLevel;
        }

        @Override
        public void streamToClient(@NotNull UUID uid, @NotNull Gate<byte[], byte[]> stream) {
            client.onClientStream.fire(uid, stream);
        }

    }
}
