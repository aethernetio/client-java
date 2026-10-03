
package io.aether.cloud.proxy;

import io.aether.api.CryptoUtils;
import io.aether.api.common.Cloud;
import io.aether.api.common.CryptoLib;
import io.aether.api.common.IPAddressAndPortsListUtil;
import io.aether.api.common.ServerDescriptor;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientCloud;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.crypto.AKey;
import io.aether.crypto.CryptoProviderFactory;

import io.aether.api.common.KeySignPrivate;
import io.aether.api.common.SignedKey;
import io.aether.crypto.PairSignKeys;
import io.aether.crypto.Signer;

import io.aether.net.virtual.VirtualFastMetaNet;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Restart E2E: master key persisted by {@link ProxyService} must survive a
 * process restart, and an already-registered downstream state must be able to
 * log in to the work listener again without re-registering. After restart a
 * local A -> B message must still be routed through the proxy.
 */
class ProxyRestartWorkLoginTest {

    private static final short PROXY_SID = (short) 30011;
    private static final short ORIGINAL_SID = (short) 7;

    static {
        // Touching VirtualFastMetaNet installs it as FastMetaNet.INSTANCE.
        new VirtualFastMetaNet();
    }

    private ProxyService newService(URI workUri, Path stateDir) {
        return new ProxyService(
                PROXY_SID,
                workUri,
                List.of(URI.create("tcp://localhost:23990")),

                Map.of(),

                List.of(),

                PROXY_SIGNING_KEYS.toArray(new SignedKey[0]),
                PROXY_SIGNING_PRIVATE_KEYS.toArray(new KeySignPrivate[0]),


                stateDir

        );
    }

    private static ClientStateInMemory upstreamState(
            UUID uid, UUID alias, AKey.Symmetric mk, URI originalWorkUri
    ) {
        CryptoProviderFactory.getProvider("SODIUM");
        ClientStateInMemory state = new ClientStateInMemory(
                UUID.randomUUID(),
                List.of(URI.create("tcp://localhost:23991")),
                null,
                CryptoLib.SODIUM
        );
        state.setUid(uid);
        state.setAlias(alias);
        state.setMasterKey(CryptoUtils.of(mk));
        state.saveCloud(new ClientCloud(
                uid, new Cloud(new short[]{ORIGINAL_SID})));
        state.getServerInfo(ORIGINAL_SID).setDescriptor(
                new ServerDescriptor(
                        ORIGINAL_SID,
                        IPAddressAndPortsListUtil.of(List.of(originalWorkUri)),
                        ORIGINAL_SID_SIGNING_KEYS));
        return state;
    }

    private static ClientStateInMemory downstreamState(
            UUID uid, UUID alias, AKey.Symmetric mk, URI proxyWorkUri
    ) {
        CryptoProviderFactory.getProvider("SODIUM");
        ClientStateInMemory state = new ClientStateInMemory(
                UUID.randomUUID(),
                List.of(),
                null,
                CryptoLib.SODIUM
        );
        state.setUid(uid);
        state.setAlias(alias);
        state.setMasterKey(CryptoUtils.of(mk));
        state.saveCloud(new ClientCloud(
                uid, new Cloud(new short[]{PROXY_SID})));
        state.getServerInfo(PROXY_SID).setDescriptor(
                new ServerDescriptor(
                        PROXY_SID,
                        IPAddressAndPortsListUtil.of(List.of(proxyWorkUri)),
                        PROXY_SID_SIGNING_KEYS));
        return state;
    }

    @Test

    void restartAllowsWorkLoginAndLocalMessage(@TempDir Path dir)
            throws Exception {

        URI workUri = URI.create("udp://localhost:23911");
        URI originalUri = URI.create("udp://localhost:23912");

        UUID uidA = UUID.randomUUID();
        UUID uidB = UUID.randomUUID();
        UUID aliasA = UUID.randomUUID();
        UUID aliasB = UUID.randomUUID();
        AKey.Symmetric mkA = CryptoProviderFactory
                .getProvider("SODIUM").createSymmetricKey();
        AKey.Symmetric mkB = CryptoProviderFactory
                .getProvider("SODIUM").createSymmetricKey();

        // ---- Phase 1: register, persist, shutdown ----
        ProxyService s1 = newService(workUri, dir);
        s1.registerNewClient(upstreamState(uidA, aliasA, mkA, originalUri));
        s1.registerNewClient(upstreamState(uidB, aliasB, mkB, originalUri));
        assertEquals(2, s1.clients().size());
        s1.destroy(true).waitSuccessful();

        // ---- Phase 2: "restart" - restore from disk and start work listener ----
        ProxyService s2 = newService(workUri, dir);
        s2.restorePersistentClients();
        assertEquals(2, s2.clients().size());
        assertNotNull(s2.clients().getByUid(uidA));
        assertNotNull(s2.clients().getByUid(uidB));

        AetherWorkProxy work = new AetherWorkProxy(workUri, s2).start();
        try {
            ClientStateInMemory downA =
                    downstreamState(uidA, aliasA, mkA, workUri);
            ClientStateInMemory downB =
                    downstreamState(uidB, aliasB, mkB, workUri);

            AetherCloudClient cA = new AetherCloudClient(downA, "restart-A");
            AetherCloudClient cB = new AetherCloudClient(downB, "restart-B");
            try {
                awaitFuture(cA.connect(), Duration.ofSeconds(30));
                awaitFuture(cB.connect(), Duration.ofSeconds(30));

                assertEquals(uidA, downA.getUid());
                assertEquals(uidB, downB.getUid());


                var cloudB = awaitResult(
                        cA.getCloud(uidB),
                        Duration.ofSeconds(10));
                assertNotNull(cloudB, "cloud of B must be resolvable after restart");
                assertArrayEquals(
                        new short[]{PROXY_SID},
                        cloudB.getData(),
                        "B must appear to A as a local proxy client");

                AtomicReference<byte[]> received = new AtomicReference<>();
                AtomicReference<UUID> receivedFrom = new AtomicReference<>();
                CountDownLatch latch = new CountDownLatch(1);
                cB.onMessage((fromUid, data) -> {
                    receivedFrom.set(fromUid);
                    received.set(data);
                    latch.countDown();
                });

                byte[] payload = "hello-after-restart"
                        .getBytes(StandardCharsets.UTF_8);
                awaitFuture(
                        cA.sendMessage(uidB, payload),
                        Duration.ofSeconds(10));
                assertTrue(latch.await(10, TimeUnit.SECONDS),
                        "B must receive locally routed message after restart");
                assertEquals(uidA, receivedFrom.get(),
                        "sender UID must be restored A identity");
                assertArrayEquals(payload, received.get());

            } finally {
                cA.destroy(true).waitSuccessful();
                cB.destroy(true).waitSuccessful();
            }
        } finally {
            work.destroy(true).waitSuccessful();
            s2.destroy(true).waitSuccessful();
        }
    }

    private static void awaitFuture(AFuture future, Duration timeout) {
        CountDownLatch latch = new CountDownLatch(1);
        future.addListener(f -> latch.countDown());
        try {
            if (!latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new AssertionError(
                        "Future timed out after " + timeout);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (future.isError()) {
            throw new RuntimeException("Future failed",
                    future.getError());
        }
    }

    private static <T> T awaitResult(
            ARFuture<T> future,
            Duration timeout
    ) {
        CountDownLatch latch = new CountDownLatch(1);
        future.addListener(f -> latch.countDown());
        try {
            if (!latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new AssertionError(
                        "Future timed out after " + timeout);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (future.isError()) {
            throw new RuntimeException("Future failed",
                    future.getError());
        }
        return future.getNow();
    }


    private static final java.util.Map<io.aether.api.common.CryptoLib, Signer> TEST_ROOT_SIGNERS =
            newTestRootSigners();
    private static final java.util.Map<io.aether.api.common.CryptoLib, PairSignKeys>
            PROXY_PAIR = newServerPairs();
    private static final java.util.Map<io.aether.api.common.CryptoLib, PairSignKeys>
            ORIGINAL_SID_PAIR = newServerPairs();

    private static final List<SignedKey> PROXY_SIGNING_KEYS = signedKeys(PROXY_PAIR);
    private static final List<KeySignPrivate> PROXY_SIGNING_PRIVATE_KEYS =
            privateKeys(PROXY_PAIR);
    private static final SignedKey[] PROXY_SID_SIGNING_KEYS =
            signedKeys(PROXY_PAIR).toArray(new SignedKey[0]);
    private static final SignedKey[] ORIGINAL_SID_SIGNING_KEYS =
            signedKeys(ORIGINAL_SID_PAIR).toArray(new SignedKey[0]);

    private static java.util.Map<io.aether.api.common.CryptoLib, Signer> newTestRootSigners() {
        java.util.Map<io.aether.api.common.CryptoLib, Signer> result =
                new java.util.EnumMap<>(io.aether.api.common.CryptoLib.class);
        for (io.aether.api.common.CryptoLib lib : io.aether.api.common.CryptoLib.values()) {
            result.put(lib, CryptoProviderFactory.getProvider(lib.name())
                    .createSignKeys().toSigner());
        }
        return result;
    }

    private static java.util.Map<io.aether.api.common.CryptoLib, PairSignKeys> newServerPairs() {
        java.util.Map<io.aether.api.common.CryptoLib, PairSignKeys> result =
                new java.util.EnumMap<>(io.aether.api.common.CryptoLib.class);
        for (io.aether.api.common.CryptoLib lib : io.aether.api.common.CryptoLib.values()) {
            result.put(lib, CryptoProviderFactory.getProvider(lib.name()).createSignKeys());
        }
        return result;
    }

    private static List<SignedKey> signedKeys(
            java.util.Map<io.aether.api.common.CryptoLib, PairSignKeys> pairs) {
        List<SignedKey> result = new java.util.ArrayList<>();
        for (io.aether.api.common.CryptoLib lib : io.aether.api.common.CryptoLib.values()) {
            result.add(CryptoUtils.of(new io.aether.crypto.SignedKey(
                    pairs.get(lib).getPublicKey(), TEST_ROOT_SIGNERS.get(lib))));
        }
        return result;
    }

    private static List<KeySignPrivate> privateKeys(
            java.util.Map<io.aether.api.common.CryptoLib, PairSignKeys> pairs) {
        List<KeySignPrivate> result = new java.util.ArrayList<>();
        for (io.aether.api.common.CryptoLib lib : io.aether.api.common.CryptoLib.values()) {
            result.add(CryptoUtils.of(pairs.get(lib).getPrivateKey()));
        }
        return result;
    }


}