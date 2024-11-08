package io.aether.cloud.client;

import io.aether.common.AetherCodec;
import io.aether.logger.Log;
import io.aether.net.NettyStreamClient;
import io.aether.net.NetworkConfigurator;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.streams.ApiStream;
import io.aether.utils.streams.BufferedStream;
import io.aether.utils.streams.DownStream;

import java.net.URI;

public abstract class Connection<LT, RT> {
    protected final AetherCloudClient client;
    protected final URI uri;
    private final Class<LT> lt;
    private final Class<RT> rt;
    protected AFuture connectFuture = new AFuture();
    protected ApiStream<LT, RT, DownStream> apiStreamRoot;
    NetworkConfigurator configurator = AetherCodec.BINARY.getNetworkConfigurator();

    public Connection(AetherCloudClient client, URI uri, Class<LT> lt, Class<RT> rt) {
        assert uri != null;
        this.apiStreamRoot = ApiStream.of(lt, rt, BufferedStream.of());
        this.uri = uri;
        this.client = client;
        this.lt = lt;
        this.rt = rt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection that = (Connection) o;
        return uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    public AFuture close(int time) {
        var res = new AFuture();
        connectFuture.to(() -> {
                    apiStreamRoot.close();
                    res.done();
                })
                .timeout(time, res::done);
        return res;
    }

    protected void connect() {
        Log.debug("try to connect " + getClass());
        var nettyStream = new NettyStreamClient(uri, configurator);
        var aConnection = apiStreamRoot.forClient(RU.cast(this));
        apiStreamRoot.setDownBase(BufferedStream.of(nettyStream));
        connectFuture.done();
        var remApi = aConnection.getRemoteApi();
        Log.debug("get remote api: " + rt);
        Log.debug("call onConnect: " + getClass());
        this.onConnect(remApi);
        apiStreamRoot.flush();
    }

    protected abstract void onConnect(RT remoteApi);
}
