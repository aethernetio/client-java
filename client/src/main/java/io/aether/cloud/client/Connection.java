package io.aether.cloud.client;

import io.aether.logger.Log;
import io.aether.net.fastMeta.FastMetaApi;
import io.aether.net.fastMeta.FastMetaNet;
import io.aether.net.fastMeta.MetaContext;
import io.aether.net.fastMeta.RemoteApi;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.interfaces.Destroyable;
import io.aether.utils.slots.EventConsumer;

import java.net.URI;

/**
 * Abstract base class for handling connections in the Aether Cloud client.
 * Manages the lifecycle of the underlying connection and connection state.
 */
public abstract class Connection<LT, RT extends RemoteApi> implements Destroyable {

    /**
     * Observable state listeners triggered when the connection writability or availability changes.
     */
    public final EventConsumer<Boolean> stateListeners = new EventConsumer<>();
    protected final AetherCloudClient client;
    protected final URI uri;
    protected final MetaContext ctx;
    protected volatile RT rootApi;


    public Connection(AetherCloudClient client, URI uri, FastMetaApi<LT, ?> localApiMeta, FastMetaApi<?, RT> remoteApiMeta) {
        assert uri != null;
        this.uri = uri;
        this.client = client;
        if (client.destroyer.isDestroyed()) {
            rootApi = null;
            this.ctx = null;
            return;
        }
        client.destroyer.add(this);
        try (var ignored = Log.of("ServerHost", uri.getHost(), "ServerPort", uri.getPort()).context()) {
            this.rootApi = FastMetaNet.INSTANCE.get().makeClient(uri, localApiMeta, remoteApiMeta, ctx -> RU.cast(this));
        }
        this.ctx = rootApi.getFastMetaContext();
        ctx.onWritable(isWritable -> {
            onConnectionStateChanged(isWritable);
            if (!isWritable) {
                Log.trace("Connection lost.", "uri", uri);
            }
        });
    }


    protected boolean isWritable() {
        return ctx != null && ctx.isActive();
    }

    protected void onConnectionStateChanged(boolean isWritable) {
    }

    public RT getRootApi() {
        return rootApi;
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

    @Override
    public AFuture destroy(boolean force) {
        Log.trace("Destroying Connection to " + uri);
        return ctx.close();
    }
}