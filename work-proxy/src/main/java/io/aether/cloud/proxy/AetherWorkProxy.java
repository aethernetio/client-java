
package io.aether.cloud.proxy;

import io.aether.api.clientserverapi.LoginApi;
import io.aether.net.fastMeta.FastMetaNet;
import io.aether.net.fastMeta.FastMetaServer;
import io.aether.utils.futures.AFuture;
import io.aether.utils.interfaces.Destroyable;

import java.net.URI;

/**
 * Customer-hosted Aether work listener. Uses {@link ProxyLoginApiImpl} as
 * the local API for each downstream work connection.
 */
public final class AetherWorkProxy implements Destroyable {

    private final URI listenUri;
    private final ProxyService service;
    private FastMetaServer<LoginApi> server;

    public AetherWorkProxy(URI listenUri, ProxyService service) {
        this.listenUri = listenUri;
        this.service = service;
    }

    public AetherWorkProxy start() {
        if (server != null) {
            return this;
        }

        FastMetaNet net = FastMetaNet.INSTANCE.get();
        if (net == null) {
            throw new IllegalStateException(
                    "FastMetaNet.INSTANCE is not initialized"
            );
        }

        server = net.makeServer(
                listenUri,
                LoginApi.META,
                context -> new ProxyLoginApiImpl(context, service)
        );

        return this;
    }

    public URI listenUri() {
        return listenUri;
    }

    @Override
    public AFuture destroy(boolean force) {
        FastMetaServer<LoginApi> current = server;
        if (current == null) {
            return AFuture.completed();
        }
        server = null;
        return current.destroy(force);
    }
}