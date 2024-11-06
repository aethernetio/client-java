package io.aether.cloud.client;

import io.aether.api.clientApi.ClientApiSafe;
import io.aether.api.clientApi.ClientApiUnsafe;
import io.aether.api.serverApi.AuthorizedApi;
import io.aether.api.serverApi.LoginApi;
import io.aether.common.AetherCodec;
import io.aether.common.ServerDescriptorLite;
import io.aether.logger.Log;
import io.aether.net.ApiDeserializerConsumer;
import io.aether.net.ApiStreamConnection;
import io.aether.net.impl.bin.ApiLevel;
import io.aether.utils.RU;
import io.aether.utils.slots.ARMultiFuture;
import io.aether.utils.streams.ApiStream;
import io.aether.utils.streams.BufferedStream;
import io.aether.utils.streams.CryptoStream;
import io.aether.utils.streams.DownStream;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ConnectionWork extends Connection<ClientApiUnsafe, LoginApi> implements ClientApiUnsafe {

    //region counters
    public final AtomicLong lastBackPing = new AtomicLong(Long.MAX_VALUE);
    final ApiStream<ClientApiSafe, AuthorizedApi, CryptoStream<DownStream>> safeApiStream;
    final ApiStreamConnection<ClientApiSafe, AuthorizedApi,CryptoStream<DownStream>> safeApiCon;
    private final ServerDescriptorLite serverDescriptor;
    final private AtomicBoolean inProcess = new AtomicBoolean();
    boolean basicStatus;
    long lastWorkTime;
    AuthorizedApi authorizedApi;
    ApiLevel apiLevel;

    public ConnectionWork(AetherCloudClient client, ServerDescriptorLite s) {
        super(client, s.ipAddress().getURI(AetherCodec.BINARY), ClientApiUnsafe.class, LoginApi.class);
        this.basicStatus = false;
        var mk = client.getMasterKey();
        safeApiStream = RU.cast(ApiStream.of(
                ClientApiSafe.class,
                AuthorizedApi.class,
                BufferedStream.of(CryptoStream.of(mk.getType().cryptoLib().env.symmetricForClient(mk, s.id())))
        ));
        safeApiCon=safeApiStream.forClient(new MyClientApiSafe(client));
        authorizedApi = safeApiCon.getRemoteApi();
        serverDescriptor = s;
        client.servers.addSource(authorizedApi.serverResolver().mapKey(
                Integer::shortValue,
                Short::intValue
        ));
        client.clouds.addSource(authorizedApi.cloudResolver());
        connect();
    }

    public DownStream openStreamToClient(UUID uid) {
        return authorizedApi.openStreamToClient(uid);
    }

    public final ARMultiFuture<ConnectionWork> ready = new ARMultiFuture<>();
    public void flush(){
        safeApiCon.flush();
    }
    @Override
    protected void onConnect(LoginApi remoteApi) {
        Log.debug("set down stream");
        safeApiCon.setDownBase(remoteApi.loginByAlias(client.getAlias()).getDownBase());
        Log.debug("set ready");
        ready.set(this);
    }

    public ServerDescriptorLite getServerDescriptor() {
        return serverDescriptor;
    }

    @Override
    public String toString() {
        return "C(" + lifeTime() + ")";
    }

    public void clearRequests() {
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

        public MyClientApiSafe(AetherCloudClient client) {
            this.client = client;
        }

        @Override
        public void setApiDeserializer(ApiLevel apiProcessor) {
            this.apiProcessor = apiProcessor;
        }

        @Override
        public void streamToClient(@NotNull UUID uid, @NotNull DownStream message) {
            client.onClientStream.fire(uid, message);
        }
    }
}
