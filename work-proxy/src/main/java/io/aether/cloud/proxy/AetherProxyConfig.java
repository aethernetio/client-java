
package io.aether.cloud.proxy;

import io.aether.api.common.CryptoLib;
import io.aether.crypto.PairAsymKeysSigned;
import io.aether.crypto.SignChecker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Immutable configuration of a customer-hosted Aether proxy.
 *
 * <p>Properties layout:
 * <pre>
 * proxy.sid=30000
 * registration.listen=tcp://0.0.0.0:19010
 * work.listen=udp://0.0.0.0:19020
 * work.public=udp://proxy.example.com:19020
 * upstream.registration=tcp://registration.aethernet.io:9010
 * state.dir=./proxy-state
 * proxy.registrationKey.SODIUM=...
 * proxy.registrationKey.HYDROGEN=...
 * </pre>
 *
 * <p>{@code state.dir} is required by {@link #load(Path)} and selects a
 * persistent directory where {@code proxy.meta} and per-client
 * {@code <uid>.state} files are stored. The programmatic constructor
 * without a {@code stateDir} keeps the proxy in in-memory mode.
 */
public record AetherProxyConfig(
        short proxySid,
        URI registrationListen,
        URI workListen,
        URI workPublic,
        List<URI> upstreamRegistrationUris,
        Map<CryptoLib, PairAsymKeysSigned> registrationKeys,
        List<SignChecker> rootSigners,
        Path stateDir
) {

    public AetherProxyConfig {
        upstreamRegistrationUris =
                List.copyOf(upstreamRegistrationUris);
        registrationKeys = Map.copyOf(registrationKeys);
        rootSigners = List.copyOf(rootSigners);
    }


    /**
     * Backward-compatible constructor without a persistent state directory.
     * The proxy then runs in in-memory mode with no persistence.
     */
    public AetherProxyConfig(
            short proxySid,
            URI registrationListen,
            URI workListen,
            URI workPublic,
            List<URI> upstreamRegistrationUris,
            Map<CryptoLib, PairAsymKeysSigned> registrationKeys,
            List<SignChecker> rootSigners
    ) {
        this(
                proxySid,
                registrationListen,
                workListen,
                workPublic,
                upstreamRegistrationUris,
                registrationKeys,
                rootSigners,
                null
        );
    }


    public static AetherProxyConfig load(Path path)
            throws IOException {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        }

        short proxySid = parseSid(
                required(properties, "proxy.sid"));

        URI registrationListen =
                URI.create(required(properties, "registration.listen"));
        URI workListen =
                URI.create(required(properties, "work.listen"));
        URI workPublic =
                URI.create(required(properties, "work.public"));

        Path stateDir =
                Path.of(required(properties, "state.dir"));
        List<URI> upstreamRegistrationUris = new ArrayList<>();
        upstreamRegistrationUris.add(
                URI.create(required(properties, "upstream.registration"))
        );

        Map<CryptoLib, PairAsymKeysSigned> registrationKeys =
                new EnumMap<>(CryptoLib.class);
        for (CryptoLib lib : CryptoLib.values()) {
            String key = "proxy.registrationKey." + lib.name();
            String value = properties.getProperty(key);
            if (value != null && !value.isBlank()) {
                registrationKeys.put(
                        lib,
                        PairAsymKeysSigned.of(value.trim())
                );
            }
        }

        if (registrationKeys.isEmpty()) {
            throw new IllegalArgumentException(
                    "No proxy.registrationKey.* configured"
            );
        }

        // Root signers: for MVP use the default client root signers
        // (same as ClientStateInMemory default).
        List<SignChecker> rootSigners = List.of(
                SignChecker.of(
                        "SODIUM:4F202A94AB729FE9B381613AE77A8A7D89EDAB9299C3320D1A0B994BA710CCEB"),
                SignChecker.of(
                        "HYDROGEN:883B4D7E0FB04A38CA12B3A451B00942048858263EE6E6D61150F2EF15F40343")
        );

        return new AetherProxyConfig(
                proxySid,
                registrationListen,
                workListen,
                workPublic,
                upstreamRegistrationUris,
                registrationKeys,
                rootSigners,
                stateDir
        );
    }

    public ProxyService createService() {
        return new ProxyService(
                proxySid,
                workPublic,
                upstreamRegistrationUris,
                registrationKeys,
                rootSigners,
                stateDir
        );
    }

    private static String required(
            Properties properties,
            String key
    ) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing proxy configuration property: " + key
            );
        }
        return value.trim();
    }

    private static short parseSid(String value) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0 || parsed > Short.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "proxy.sid out of range: " + value
                );
            }
            return (short) parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid proxy.sid: " + value,
                    e
            );
        }
    }
}