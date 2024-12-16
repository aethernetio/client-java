package io.aether.cloud.client;

import io.aether.common.AetherCodec;
import io.aether.logger.Log;
import io.aether.net.ApiGate;
import io.aether.net.NettyStreamClient;
import io.aether.net.NetworkConfigurator;
import io.aether.net.meta.ApiManager;
import io.aether.net.meta.MetaApi;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;

import java.net.URI;

public abstract class Connection<LT, RT> {
    protected final AetherCloudClient client;
    protected final URI uri;
    private final MetaApi<LT> lt;
    private final MetaApi<RT> rt;
    protected final AFuture connectFuture = new AFuture();
    protected ApiGate<LT, RT> apiRoot;
    final NetworkConfigurator configurator = AetherCodec.BINARY.getNetworkConfigurator();

    public Connection(AetherCloudClient client, URI uri, Class<LT> lt, Class<RT> rt) {
        assert uri != null;
        this.lt = ApiManager.getApi(lt);
        this.rt = ApiManager.getApi(rt);
        this.apiRoot = new ApiGate<>(this.lt, this.rt);
        this.uri = uri;
        this.client = client;
        apiRoot.up().ofConsumer(cmd->cmd.accept(RU.cast(this)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (Connection<?,?>) o;
        return uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    public AFuture close(int time) {
        var res = new AFuture();
        connectFuture.to(() -> {
                    apiRoot.close();
                    res.done();
                })
                .timeout(time, res::done);
        return res;
    }

    protected void connect() {
        Log.debug("try to connect " + getClass());
        apiRoot.down().link(new NettyStreamClient(uri, configurator).up().buffer());
        connectFuture.done();
        var remApi = apiRoot.getRemoteApi();
        Log.debug("get remote api: " + rt);
        Log.debug("call onConnect: " + getClass());
        this.onConnect(remApi);
        apiRoot.flush();
    }

    protected abstract void onConnect(RT remoteApi);
}
