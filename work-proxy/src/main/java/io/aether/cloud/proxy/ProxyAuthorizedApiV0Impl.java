
package io.aether.cloud.proxy;

import io.aether.api.clientserverapi.ClientApiSafeRemote;

/**
 * V0 concrete implementation of the proxy's downstream AuthorizedApi.
 */
public final class ProxyAuthorizedApiV0Impl extends ProxyAuthorizedApiImpl {

    public ProxyAuthorizedApiV0Impl(
            ProxyService service,
            ProxyClientDescriptor client,
            ClientApiSafeRemote safeRemoteApi,
            ProxyLoginApiImpl.LoginStreamSession loginStreamSession,
            ProxyLoginApiImpl.LoginStreamTransport transport
    ) {
        super(
                service,
                client,
                safeRemoteApi,
                loginStreamSession,
                transport
        );
    }
}
