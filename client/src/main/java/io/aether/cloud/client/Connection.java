package io.aether.cloud.client;

import io.aether.logger.Log;
import io.aether.net.fastMeta.FastApiContext;
import io.aether.net.fastMeta.FastApiContextLocal;
import io.aether.net.fastMeta.FastMetaApi;
import io.aether.utils.RU;
import io.aether.utils.SocketNIOStreamClient;
import io.aether.utils.futures.AFuture;
import io.aether.utils.interfaces.Destroyable;
import io.aether.utils.streams.Gate;

import java.net.URI;

public abstract class Connection<LT, RT, RT2 extends RT> implements Destroyable {
    protected final AetherCloudClient client;
    protected final URI uri;
    protected final AFuture connectFuture = new AFuture();
    protected FastApiContextLocal<LT> rootApiContext;
    final SocketNIOStreamClient socketStreamClient;
    protected final RT2 rootApi;

    public Connection(AetherCloudClient client, URI uri, FastMetaApi<LT, ? extends LT> lt, FastMetaApi<RT, RT2> rt) {
        assert uri != null;
        this.uri = uri;
        this.client = client;
        if (client.destroyer.isDestroyed()) {
            rootApi = null;
            gate = null;
            socketStreamClient = null;
            return;
        }
        client.destroyer.add(this);
        socketStreamClient = new SocketNIOStreamClient(uri);
        socketStreamClient.connectedFuture.to(connectFuture);
        gate = socketStreamClient.up().bufferAutoFlush();
        this.rootApiContext = gate.toApiR(lt, c->RU.cast(this));
        rootApi = rootApiContext.makeRemote(rt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (Connection<?, ?, ?>) o;
        return uri.equals(that.uri);
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public AFuture destroy(boolean force) {
        var res = new AFuture();
        socketStreamClient.destroy(force);
        connectFuture.to(() -> {
            try {
                rootApiContext.close();
            } catch (Exception e) {
                Log.warn("close connection error", e);
            }
            try {
                socketStreamClient.destroy(force);
            } catch (Exception e) {
                Log.warn("close connection error", e);
            }
            res.done();
        });
        return res;
    }

    final Gate<byte[], byte[]> gate;

}
