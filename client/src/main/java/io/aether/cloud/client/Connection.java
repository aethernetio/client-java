package io.aether.cloud.client;

import io.aether.logger.Log;
import io.aether.net.fastMeta.FastMetaApi;
import io.aether.net.fastMeta.FastMetaClient;
import io.aether.net.fastMeta.RemoteApi;
import io.aether.net.fastMeta.nio.NIOFastMetaClient;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.Destroyable;

import java.net.URI;

public abstract class Connection<LT, RT extends RemoteApi> implements Destroyable {

    protected final AetherCloudClient client;
    protected final URI uri;
    protected final ARFuture<RT> connectFuture = ARFuture.of();
    protected final FastMetaClient<LT, RT> fastMetaClient;
    protected volatile RT rootApi;

    protected Connection(
            AetherCloudClient client,
            URI uri,
            FastMetaApi<LT, ?> localApiMeta,
            FastMetaApi<?, RT> remoteApiMeta,
            FastMetaClient<LT, RT> clientImpl
    ) {
        assert uri != null;
        this.uri = uri;
        this.client = client;
        this.fastMetaClient = clientImpl;

        if (client.destroyer.isDestroyed()) {
            fastMetaClient.close();
            connectFuture.cancel();
            rootApi = null;
            return;
        }

        client.destroyer.add(this);
        client.destroyer.add(fastMetaClient);

        LT localApi = RU.cast(this);

        fastMetaClient.connect(uri, localApiMeta, remoteApiMeta, r -> {
            rootApi = r;
            return localApi;
        }).map(remoteApiMeta::makeRemote).to(connectFuture);

    }

    public Connection(
            AetherCloudClient client,
            URI uri,
            FastMetaApi<LT, ?> localApiMeta,
            FastMetaApi<?, RT> remoteApiMeta
    ) {
        this(client, uri, localApiMeta, remoteApiMeta, new NIOFastMetaClient<>());
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
        Log.info("Destroying Connection to " + uri);
        return fastMetaClient.close();
    }
}