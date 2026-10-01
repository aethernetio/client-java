
package io.aether.cloud.proxy;

import io.aether.api.clientserverapi.RegistrationRootApi;
import io.aether.net.fastMeta.FastMetaNet;
import io.aether.net.fastMeta.FastMetaServer;
import io.aether.utils.futures.AFuture;
import io.aether.utils.interfaces.Destroyable;

import java.net.URI;

/**
 * Customer-hosted registration endpoint. Terminates crypto and forwards the
 * registration flow to the original Aether registration server through the
 * shared {@link ProxyService}.
 */
public final class AetherRegistrationProxy
        implements Destroyable {

    private final URI listenUri;
    private final ProxyService service;
    private FastMetaServer<RegistrationRootApi> server;

    public AetherRegistrationProxy(
            URI listenUri,
            ProxyService service
    ) {
        this.listenUri = listenUri;
        this.service = service;
    }

    public AetherRegistrationProxy start() {
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
                RegistrationRootApi.META,
                context -> new ProxyRegistrationRootApiImpl(
                        context,
                        service
                )
        );

        return this;
    }

    public URI listenUri() {
        return listenUri;
    }

    @Override
    public AFuture destroy(boolean force) {
        FastMetaServer<RegistrationRootApi> current = server;
        if (current == null) {
            return AFuture.completed();
        }
        server = null;
        return current.destroy(force);
    }
}