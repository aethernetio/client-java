package io.aether.cloud.client;

import io.aether.logger.Log;
import io.aether.net.fastMeta.FastMetaApi;
import io.aether.net.fastMeta.FastMetaClient;
import io.aether.net.fastMeta.FastMetaNet; // <-- Импорт добавлен
import io.aether.net.fastMeta.RemoteApi;
// import io.aether.net.fastMeta.nio.FastMetaClientNIO; // <-- Импорт удален
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.AFunction; // <-- Импорт добавлен
import io.aether.utils.interfaces.Destroyable;

import java.net.URI;

public abstract class Connection<LT, RT extends RemoteApi> implements Destroyable {
    protected final AetherCloudClient client;
    protected final URI uri;
    protected final ARFuture<RT> connectFuture = ARFuture.make();
    protected final FastMetaClient<LT, RT> fastMetaClient;
    protected volatile RT rootApi;
    public Connection(
            AetherCloudClient client,
            URI uri,
            FastMetaApi<LT, ?> localApiMeta,
            FastMetaApi<?, RT> remoteApiMeta
    ) {
        assert uri != null;
        this.uri = uri;
        this.client = client;
        if (client.destroyer.isDestroyed()) {
            connectFuture.cancel();
            rootApi = null;
            this.fastMetaClient = null;
            return;
        }
        client.destroyer.add(this);
        LT localApi = RU.cast(this);
        AFunction<RT, LT> localApiProvider = remoteApi -> {
            this.rootApi = remoteApi;
            return localApi;
        };
        FastMetaNet.WritableConsumer writableConsumer = isWritable -> {
            if (isWritable) {
                if (this.rootApi != null) {
                    this.connectFuture.tryDone(this.rootApi);
                } else {
                    Log.error("Connection is writable but rootApi was not set.", "uri", uri);
                    this.connectFuture.tryError(new IllegalStateException("Connection established but rootApi is null."));
                }
            } else {
                Log.warn("Connection lost.", "uri", uri);
            }
        };
        FastMetaNet factory = FastMetaNet.INSTANCE.get();
        this.fastMetaClient = factory.makeClient(
                uri,
                localApiMeta,
                remoteApiMeta,
                localApiProvider,
                writableConsumer
        );
        client.destroyer.add(fastMetaClient);
    }

    public RT getRootApi() {
        if (!connectFuture.isDone()) {
            Log.warn("Accessing rootApi before connection is established: " + uri);
        }
        return rootApi;
    }

    public ARFuture<RT> getRootApiFuture() {
        return connectFuture;
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
//        new RuntimeException().printStackTrace();
        Log.info("Destroying Connection to " + uri);
        return fastMetaClient.destroy(force);
    }
}