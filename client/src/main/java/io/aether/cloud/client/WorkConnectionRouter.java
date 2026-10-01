
package io.aether.cloud.client;

import io.aether.api.common.ServerDescriptor;

import java.net.URI;

/**
 * Immutable client-side policy that decides which physical transport URI is
 * used for a work {@link ConnectionWork} created for a logical
 * {@link ServerDescriptor}.
 *
 * <p>The router only changes the physical FastMeta endpoint. It must never be
 * used to alter:
 * <ul>
 *     <li>{@link ServerDescriptor#getId()} (logical server SID);</li>
 *     <li>the server crypto key selected by
 *         {@link AetherCloudClient#getCryptoEngineForServer(short)};</li>
 *     <li>any wire format or nested client stream semantics.</li>
 * </ul>
 *
 * <p>The router is set once, before the first {@code connect()} call, and is
 * never mutated afterwards.
 */
public interface WorkConnectionRouter {

    /**
     * Default direct routing: physical URI is derived from the
     * {@link ServerDescriptor} exactly as before proxy support was added.
     */
    WorkConnectionRouter DIRECT = server -> null;

    /**
     * Resolve the physical transport URI for the given logical server.
     *
     * @param server logical server descriptor; never {@code null}
     * @return physical URI to connect to, or {@code null} to fall back to the
     * descriptor-provided URI
     */
    URI resolve(ServerDescriptor server);
}
