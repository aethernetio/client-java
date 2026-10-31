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

    private volatile boolean ready = false;
    private volatile Boolean pendingWritable = null;
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
            // Subclass constructor has not finished yet — remember the state
            // and deliver it from markReady() to avoid NPE on not-yet
            // initialised subclass fields.
            if (!ready) {
                pendingWritable = isWritable;
                return;
            }
            onConnectionStateChanged(isWritable);
            if (!isWritable) {
                Log.trace("Connection lost.", "uri", uri);
            }
        });

    }



    /**
     * Marks the connection as fully constructed. Must be called at the very end
     * of the subclass constructor so that deferred writability state can be
     * delivered once all subclass fields are initialised.
     */
    protected void markReady() {
        ready = true;
        Boolean pending = pendingWritable;
        if (pending != null) {
            pendingWritable = null;
            onConnectionStateChanged(pending);
        }
    }


    protected boolean isWritable() {
        return ctx != null && ctx.isActive();
    }

    protected void onConnectionStateChanged(boolean isWritable) {
    }

    public RT getRootApi() {
        return rootApi;
    }

    public URI getUri() {
        return uri;
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