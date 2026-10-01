
package io.aether.cloud.proxy;

import io.aether.api.CryptoUtils;
import io.aether.api.common.Cloud;
import io.aether.api.common.CryptoLib;
import io.aether.api.common.IPAddressAndPortsListUtil;
import io.aether.api.common.ServerDescriptor;
import io.aether.cloud.client.ClientCloud;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.crypto.AKey;
import io.aether.crypto.CryptoProviderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProxyServicePersistenceTest {

    private static final short PROXY_SID = (short) 30010;
    private static final short ORIGINAL_SID = (short) 1;

    private ProxyService newService(Path stateDir) {
        return new ProxyService(
                PROXY_SID,
                URI.create("udp://localhost:1"),
                List.of(URI.create("tcp://localhost:19010")),
                Map.of(),
                List.of(),
                stateDir
        );
    }

    private static ClientStateInMemory newUpstreamState(
            UUID uid, UUID alias
    ) {
        CryptoProviderFactory.getProvider("SODIUM");
        AKey.Symmetric mk = CryptoProviderFactory
                .getProvider("SODIUM")
                .createSymmetricKey();
        ClientStateInMemory state = new ClientStateInMemory(
                UUID.randomUUID(),
                List.of(URI.create("tcp://localhost:19999")),
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
                        IPAddressAndPortsListUtil.of(
                                List.of(URI.create("udp://localhost:19998")))));
        return state;
    }

    @Test
    void duplicateUidDoesNotOverwriteFile(@TempDir Path dir)
            throws IOException {
        ProxyService service = newService(dir);
        try {
            UUID uid = UUID.randomUUID();
            UUID alias = UUID.randomUUID();
            ClientStateInMemory first =
                    newUpstreamState(uid, alias);
            service.registerNewClient(first);

            Path target = service.clients().stateFileFor(uid);
            assertTrue(Files.exists(target));
            byte[] before = Files.readAllBytes(target);

            // Same uid, different alias -> duplicate preflight must fail
            ClientStateInMemory duplicate =
                    newUpstreamState(uid, UUID.randomUUID());
            IllegalStateException error = assertThrows(
                    IllegalStateException.class,
                    () -> service.registerNewClient(duplicate)
            );
            assertTrue(error.getMessage().contains("already registered"),
                    error.getMessage());

            byte[] after = Files.readAllBytes(target);
            assertArrayEquals(before, after,
                    "state file must not be overwritten");
            assertNotNull(service.clients().getByUid(uid));
        } finally {
            service.destroy(true).waitSuccessful();
        }
    }

    @Test
    void duplicateAliasDoesNotLeak(@TempDir Path dir) {
        ProxyService service = newService(dir);
        try {
            UUID uidA = UUID.randomUUID();
            UUID alias = UUID.randomUUID();
            service.registerNewClient(newUpstreamState(uidA, alias));

            UUID uidB = UUID.randomUUID();
            IllegalStateException error = assertThrows(
                    IllegalStateException.class,
                    () -> service.registerNewClient(
                            newUpstreamState(uidB, alias))
            );
            assertTrue(error.getMessage().contains("alias"),
                    error.getMessage());
            assertFalse(service.clients().isLocal(uidB),
                    "failed registration must not register uid");
            assertFalse(Files.exists(
                    service.clients().stateFileFor(uidB)));
        } finally {
            service.destroy(true).waitSuccessful();
        }
    }

    @Test
    void corruptStateCausesFullRollback(@TempDir Path dir)
            throws IOException {
        ProxyService service = newService(dir);
        UUID uidA = UUID.randomUUID();
        UUID uidB = UUID.randomUUID();
        service.registerNewClient(newUpstreamState(uidA, UUID.randomUUID()));
        service.registerNewClient(newUpstreamState(uidB, UUID.randomUUID()));
        service.destroy(true).waitSuccessful();

        Path bad = service.clients().clientsDir()
                .resolve(UUID.randomUUID() + ".state");
        Files.write(bad, new byte[]{1, 2, 3, 4, 5});

        ProxyService service2 = newService(dir);
        try {
            assertThrows(IllegalStateException.class,
                    service2::restorePersistentClients);
            assertEquals(0, service2.clients().size(),
                    "no partial restore allowed");
        } finally {
            service2.destroy(true).waitSuccessful();
        }
    }

    @Test
    void invalidFilenameStartsNoRuntimeState(@TempDir Path dir)
            throws IOException {
        ProxyService service = newService(dir);
        Files.writeString(
                service.clients().clientsDir().resolve("not-a-uuid.state"),
                "x");
        try {
            assertThrows(IllegalStateException.class,
                    service::restorePersistentClients);
            assertEquals(0, service.clients().size());
        } finally {
            service.destroy(true).waitSuccessful();
        }
    }

    @Test
    void restoreRoundtripKeepsMasterKey(@TempDir Path dir) {

        ProxyService service = newService(dir);
        UUID uidA = UUID.randomUUID();
        UUID uidB = UUID.randomUUID();
        UUID aliasA = UUID.randomUUID();
        UUID aliasB = UUID.randomUUID();
        ClientStateInMemory a = newUpstreamState(uidA, aliasA);
        ClientStateInMemory b = newUpstreamState(uidB, aliasB);
        byte[] expectedMkA = CryptoUtils.of(a.getMasterKey())
                .asSymmetric().getData();
        byte[] expectedMkB = CryptoUtils.of(b.getMasterKey())
                .asSymmetric().getData();
        service.registerNewClient(a);
        service.registerNewClient(b);
        service.destroy(true).waitSuccessful();

        ProxyService service2 = newService(dir);
        try {
            service2.restorePersistentClients();
            assertEquals(2, service2.clients().size());
            ProxyClientDescriptor dA = service2.clients().getByUid(uidA);
            ProxyClientDescriptor dB = service2.clients().getByUid(uidB);
            assertNotNull(dA);
            assertNotNull(dB);
            assertEquals(aliasA, dA.alias());
            assertEquals(aliasB, dB.alias());
            assertEquals(CryptoLib.SODIUM,
                    dA.upstreamState().getCryptoLib());
            assertArrayEquals(
                    new short[]{ORIGINAL_SID},
                    dA.upstreamState().getCloud(uidA).toCloud().getData());
            assertArrayEquals(
                    expectedMkA,
                    CryptoUtils.of(dA.upstreamState().getMasterKey())
                            .asSymmetric().getData(),
                    "master key A must survive restore");
            assertArrayEquals(
                    expectedMkB,
                    CryptoUtils.of(dB.upstreamState().getMasterKey())
                            .asSymmetric().getData(),
                    "master key B must survive restore");
            assertNotNull(
                    dA.upstreamState().getServerDescriptor(ORIGINAL_SID),
                    "original server descriptor must survive restore");
        } finally {
            service2.destroy(true).waitSuccessful();
        }
    }


    @Test
    void deleteClientStopsRuntimeAndRemovesFile(@TempDir Path dir)
            throws IOException {

        ProxyService service = newService(dir);
        try {
            UUID uid = UUID.randomUUID();
            UUID alias = UUID.randomUUID();
            service.registerNewClient(newUpstreamState(uid, alias));
            Path target = service.clients().stateFileFor(uid);
            assertTrue(Files.exists(target));
            assertNotNull(service.clients().getByUid(uid));
            assertNotNull(service.clients().getByAlias(alias));

            service.deleteClient(uid, true).waitSuccessful();

            assertFalse(service.clients().isLocal(uid));
            assertNull(service.clients().getByAlias(alias),
                    "alias index must be cleaned up on delete");
            assertFalse(Files.exists(target));
        } finally {
            service.destroy(true).waitSuccessful();
        }
    }


    @Test
    void nullStateDirInMemoryMode() {
        ProxyService service = new ProxyService(
                PROXY_SID,
                URI.create("udp://localhost:1"),
                List.of(URI.create("tcp://localhost:19010")),
                Map.of(),
                List.of(),
                null
        );
        try {
            UUID uid = UUID.randomUUID();
            service.registerNewClient(newUpstreamState(uid, UUID.randomUUID()));
            assertTrue(service.clients().isLocal(uid));
            assertFalse(service.clients().persistent());
        } finally {
            service.destroy(true).waitSuccessful();
        }
    }

    @Test
    void shutdownKeepsPersistentState(@TempDir Path dir)
            throws IOException {
        ProxyService service = newService(dir);
        UUID uid = UUID.randomUUID();
        service.registerNewClient(newUpstreamState(uid, UUID.randomUUID()));
        Path target = service.clients().stateFileFor(uid);
        assertTrue(Files.exists(target));

        service.destroy(true).waitSuccessful();
        assertTrue(Files.exists(target),
                "shutdown must not delete persistent state");

        ProxyService service2 = newService(dir);
        try {
            service2.restorePersistentClients();
            assertNotNull(service2.clients().getByUid(uid),
                    "client must be restored after shutdown");
        } finally {
            service2.destroy(true).waitSuccessful();
        }
    }

    @Test
    void stateFilesHave0600Permissions(@TempDir Path dir)
            throws IOException {
        Assumptions.assumeTrue(
                java.nio.file.FileSystems.getDefault()
                        .supportedFileAttributeViews()
                        .contains("posix"),
                "POSIX filesystem required");

        ProxyService service = newService(dir);
        try {
            UUID uid = UUID.randomUUID();
            service.registerNewClient(newUpstreamState(uid, UUID.randomUUID()));
            var statePerms = Files.getPosixFilePermissions(
                    service.clients().stateFileFor(uid));
            assertEquals("rw-------",
                    java.nio.file.attribute.PosixFilePermissions
                            .toString(statePerms));
            var metaPerms = Files.getPosixFilePermissions(
                    dir.resolve("proxy.meta"));
            assertEquals("rw-------",
                    java.nio.file.attribute.PosixFilePermissions
                            .toString(metaPerms));
        } finally {
            service.destroy(true).waitSuccessful();
        }
    }

}