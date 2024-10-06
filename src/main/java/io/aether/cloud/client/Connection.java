package io.aether.cloud.client;

import io.aether.client.AetherClientFactory;
import io.aether.common.AetherCodec;
import io.aether.net.AConnectionConfig;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;

import java.net.URI;

public abstract class Connection<LT, LTS, RT>  {
    protected final AetherCloudClient client;
    protected final URI uri;
    private final Class<LT> lt;
    private final Class<RT> rt;
    protected AFuture connectFuture=new AFuture();
    protected io.aether.net.AConnection<LT, RT> aConnection;
    LTS localSafeApi;
    public Connection(AetherCloudClient client, URI uri, Class<LT> lt, Class<RT> rt, LTS localSafeApi) {
        assert uri != null;
        this.localSafeApi=localSafeApi;
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
                    aConnection.close();
                    res.done();
                })
                .timeout(time, res::done);
        return res;
    }

    protected void connect() {
        var con = AetherClientFactory.make(uri, AConnectionConfig.of(lt, rt, AetherCodec.BINARY), RU.cast(this));
        con.to((p) -> {
            onConnect(p.getRemoteApi());
            p.flush();
        }).to(connectFuture);

    }

    protected abstract void onConnect(RT remoteApi);
}
