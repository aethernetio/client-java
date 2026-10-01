
package io.aether.cloud.client;

import io.aether.api.common.ServerDescriptor;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable SID-based work routing table.
 *
 * <p>Maps an original work server SID to a customer-hosted proxy physical URI.
 * The logical server descriptor used for crypto and identity is untouched.
 *
 * <p>Two explicit policies exist:
 * <ul>
 *     <li>{@link #proxyOnly(Map)} - every SID in the client cloud must have a
 *         route; unknown SIDs are rejected instead of silently falling back to
 *         direct connections;</li>
 *     <li>{@link #withDirectFallback(Map)} - unknown SIDs connect directly to
 *         their descriptor URI as in the non-proxied client.</li>
 * </ul>
 */
public final class WorkConnectionRoutes implements WorkConnectionRouter {

    private final Map<Short, URI> routes;
    private final boolean proxyOnly;

    private WorkConnectionRoutes(
            Map<Short, URI> routes,
            boolean proxyOnly
    ) {
        this.routes = Map.copyOf(routes);
        this.proxyOnly = proxyOnly;
    }

    /**
     * Proxy-only mode. Any SID not present in the map fails with a
     * {@link ClientApiException} instead of silently using a direct route.
     */
    public static WorkConnectionRoutes proxyOnly(
            Map<Short, URI> routes
    ) {
        Objects.requireNonNull(routes, "routes");
        return new WorkConnectionRoutes(routes, true);
    }

    /**
     * Mixed mode. Unknown SIDs fall back to the original descriptor URI.
     */
    public static WorkConnectionRoutes withDirectFallback(
            Map<Short, URI> routes
    ) {
        Objects.requireNonNull(routes, "routes");
        return new WorkConnectionRoutes(routes, false);
    }

    @Override
    public URI resolve(ServerDescriptor server) {
        Objects.requireNonNull(server, "server");
        short sid = server.getId();
        URI route = routes.get(sid);
        if (route != null) {
            return route;
        }
        if (proxyOnly) {

            throw new AetherCloudClient.ClientApiException(
                    "No work proxy route configured for SID " + sid
            );

        }
        return null;
    }

    public Map<Short, URI> routes() {
        return routes;
    }

    public boolean isProxyOnly() {
        return proxyOnly;
    }
}