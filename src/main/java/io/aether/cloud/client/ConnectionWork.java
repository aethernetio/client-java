package io.aether.cloud.client;

import io.aether.api.clientApi.ClientApiSafe;
import io.aether.api.clientApi.ClientApiUnsafe;
import io.aether.api.serverApi.AuthorizedApi;
import io.aether.api.serverApi.LoginApi;
import io.aether.utils.CType;
import io.aether.common.AetherCodec;
import io.aether.common.ServerDescriptorLite;
import io.aether.logger.Log;
import io.aether.net.ApiDeserializerConsumer;
import io.aether.net.ApiGateConnection;
import io.aether.net.impl.bin.ApiLevel;
import io.aether.utils.RU;
import io.aether.utils.slots.ARMultiFuture;
import io.aether.utils.streams.CryptoStream;
import io.aether.utils.streams.Gate;
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
    ApiLevel apiLevel;
    ApiGateConnection<ClientApiSafe, AuthorizedApi> safeApiCon;

    public ConnectionWork(AetherCloudClient client, ServerDescriptorLite s) {
        super(client, s.ipAddress().getURI(AetherCodec.BINARY), ClientApiUnsafe.class, LoginApi.class);
        serverDescriptor = s;
        this.basicStatus = false;
        connect();
    }

    public void openStreamToClient(UUID uid, Gate<byte[]> gate) {
        authorizedApi.openStreamToClient(uid, gate);
    }

    public void flush() {
        safeApiCon.flush();
    }

    @Override
    protected void onConnect(LoginApi remoteApi) {
        var authorizedApiStream = remoteApi.loginByAlias(client.getAlias());
        var mk = client.getMasterKey();
        CryptoStream<?> cs = authorizedApiStream.findDown(CType.of(CryptoStream.class));
        cs.setCryptoProvider(mk.getType().cryptoLib().env.symmetricForClient(mk, serverDescriptor.id()));
        safeApiCon = authorizedApiStream.forClient(new MyClientApiSafe(client));
        authorizedApi = safeApiCon.getRemoteApi();
        client.servers.addSource(authorizedApi.serverResolver().forClient().mapKey(Integer::shortValue));
        client.clouds.addSource(authorizedApi.cloudResolver());
//        flush();
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

    private static class MyClientApiSafe implements ClientApiSafe, ApiDeserializerConsumer {
        private final AetherCloudClient client;
        private ApiLevel apiProcessor;

        @Override
        public void changeParent(UUID uid) {

        }

        @Override
        public void changeAlias(UUID alias) {

        }

        @Override
        public void newChild(UUID uid) {

        }

        public MyClientApiSafe(AetherCloudClient client) {
            this.client = client;
        }

        @Override
        public void setApiDeserializer(ApiLevel apiProcessor) {
            this.apiProcessor = apiProcessor;
        }

        @Override
        public void streamToClient(@NotNull UUID uid, @NotNull Gate<byte[]> stream) {
            client.onClientStream.fire(uid, stream);
        }

    }
}
