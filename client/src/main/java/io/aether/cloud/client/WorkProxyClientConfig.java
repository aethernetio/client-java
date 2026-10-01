
package io.aether.cloud.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Client-side loader that builds a {@link WorkConnectionRouter} from an
 * operator-facing properties file.
 *
 * <p>Every route is keyed by the original work server SID and points at the
 * matching customer proxy physical endpoint:
 *
 * <pre>
 * work.21.proxy=tcp://proxy.example.com:12021
 * work.27.proxy=tcp://proxy.example.com:12027
 * </pre>
 *
 * <p>The same SID is used on the proxy side in
 * {@code work.<sid>.listen} / {@code work.<sid>.upstream}, so the physical
 * listener dedicated to that SID forwards to the original work server that
 * issued that SID.
 */
public final class WorkProxyClientConfig {

    private WorkProxyClientConfig() {
    }

    public static WorkConnectionRouter load(Path path)
            throws IOException {
        return load(path, true);
    }

    /**
     * @param proxyOnly when {@code true} the returned router rejects SIDs
     *                  that have no configured proxy route (no direct
     *                  fallback); when {@code false} unknown SIDs connect
     *                  directly to their descriptor URI.
     */
    public static WorkConnectionRouter load(
            Path path,
            boolean proxyOnly
    ) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        }

        Map<Short, URI> routes = new HashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith("work.") || !key.endsWith(".proxy")) {
                continue;
            }

            String middle = key.substring(
                    "work.".length(),
                    key.length() - ".proxy".length()
            );

            short sid = parseSid(middle);
            String value = properties.getProperty(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(
                        "Empty proxy URI for SID " + sid
                );
            }
            routes.put(sid, URI.create(value.trim()));
        }

        if (routes.isEmpty()) {
            throw new IllegalArgumentException(
                    "No work proxy routes configured: expected "
                            + "work.<sid>.proxy=<uri>"
            );
        }

        return proxyOnly
                ? WorkConnectionRoutes.proxyOnly(routes)
                : WorkConnectionRoutes.withDirectFallback(routes);
    }

    private static short parseSid(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0 || parsed > Short.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Work SID out of range: " + value
                );
            }
            return (short) parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid work SID in property: " + value,
                    e
            );
        }
    }
}
