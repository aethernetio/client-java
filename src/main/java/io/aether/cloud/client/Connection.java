package io.aether.cloud.client;

import io.aether.classBuilder.CType;
import io.aether.net.NettyStreamClient;
import io.aether.net.streams.ApiStream;
import io.aether.net.streams.DownStream;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;

import java.net.URI;

public abstract class Connection<LT, RT> {
    protected final AetherCloudClient client;
    protected final URI uri;
    private final Class<LT> lt;
    private final Class<RT> rt;
    protected AFuture connectFuture = new AFuture();
    protected ApiStream<LT, RT, DownStream> apiStream;

    public Connection(AetherCloudClient client, URI uri, Class<LT> lt, Class<RT> rt) {
        assert uri != null;
        this.apiStream = new ApiStream<>(null, CType.of(lt), CType.of(rt));
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
                    apiStream.close();
                    res.done();
                })
                .timeout(time, res::done);
        return res;
    }

    protected void connect() {
        var nettyStream = new NettyStreamClient(uri);
        nettyStream.onConnect.add(s -> {
            var aConnection = apiStream.forClient(RU.cast(this));
            this.onConnect(aConnection.getRemoteApi());
            apiStream.flush();
            connectFuture.done();
        });
        apiStream.setDownBaseStream(nettyStream);
    }

    protected abstract void onConnect(RT remoteApi);
}
