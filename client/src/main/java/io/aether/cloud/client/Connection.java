package io.aether.cloud.client;

import io.aether.common.AetherCodec;
import io.aether.common.NetworkConfigurator;
import io.aether.logger.Log;
import io.aether.net.ApiGate;
import io.aether.net.Remote;
import io.aether.net.StreamManager;
import io.aether.net.meta.ApiManager;
import io.aether.net.meta.MetaApi;
import io.aether.utils.RU;
import io.aether.utils.SocketNIOStreamClient;
import io.aether.utils.futures.AFuture;

import java.net.URI;

public abstract class Connection<LT, RT> {
    protected final AetherCloudClient client;
    protected final URI uri;
    protected final AFuture connectFuture = new AFuture();
    final NetworkConfigurator configurator = AetherCodec.BINARY.getNetworkConfigurator();
    private final MetaApi<LT> lt;
    private final MetaApi<RT> rt;
    protected ApiGate<LT, RT> apiRoot;
    SocketNIOStreamClient socketStreamClient;

    public Connection(AetherCloudClient client, URI uri, Class<LT> lt, Class<RT> rt) {
        assert uri != null;
        this.lt = ApiManager.getApi(lt);
        this.rt = ApiManager.getApi(rt);
        this.apiRoot = ApiGate.of(this.lt, this.rt, StreamManager.forClient());
        this.uri = uri;
        this.client = client;
        apiRoot.linkLocalApi(RU.cast(this));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (Connection<?, ?>) o;
        return uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    public AFuture close(int time) {
        var res = new AFuture();
        socketStreamClient.close();
        connectFuture.to(() -> {
                    try {
                        apiRoot.close();
                    } catch (Exception e) {
                        Log.warn("close connection error", e);
                    }
                    try {
                        socketStreamClient.close();
                    } catch (Exception e) {
                        Log.warn("close connection error", e);
                    }
                    res.done();
                })
                .timeout(time, () -> {
                    Log.warn("timeout close socket", "connection", Connection.this);
                });
        return res;
    }

    protected void connect() {
        socketStreamClient = new SocketNIOStreamClient(uri, configurator);
        connectFuture.done();
        apiRoot.down().link(socketStreamClient.up());
        var remApi = apiRoot.getRemoteApi();
        onConnect(remApi);
    }

    protected abstract void onConnect(Remote<RT> remoteApi);
}
