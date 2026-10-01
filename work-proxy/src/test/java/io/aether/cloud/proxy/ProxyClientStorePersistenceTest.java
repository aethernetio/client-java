
package io.aether.cloud.proxy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxyClientStorePersistenceTest {

    @Test
    void createsProxyMetaOnFirstStart(@TempDir Path tempDir) {
        ProxyClientStore store = new ProxyClientStore(tempDir, (short) 30010);
        store.initialize();

        assertTrue(store.persistent());
        assertTrue(Files.exists(tempDir.resolve("proxy.meta")));
        assertTrue(Files.isDirectory(tempDir.resolve("clients")));
    }

    @Test
    void acceptsSameSidOnRestart(@TempDir Path tempDir) {
        new ProxyClientStore(tempDir, (short) 30010).initialize();

        ProxyClientStore store =
                new ProxyClientStore(tempDir, (short) 30010);
        store.initialize();

        assertTrue(store.persistent());
    }

    @Test
    void rejectsDifferentSidOnRestart(@TempDir Path tempDir) {
        new ProxyClientStore(tempDir, (short) 30010).initialize();

        ProxyClientStore store =
                new ProxyClientStore(tempDir, (short) 30011);
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                store::initialize
        );
        assertTrue(error.getMessage().contains("SID mismatch"),
                error.getMessage());
    }

    @Test
    void nullStateDirRunsInMemoryMode() {
        ProxyClientStore store = new ProxyClientStore(null, (short) 1);
        store.initialize();

        assertFalse(store.persistent());
    }

    @Test
    void listsPersistedStateFilesSortedAndFiltered(@TempDir Path tempDir)
            throws IOException {
        ProxyClientStore store = new ProxyClientStore(tempDir, (short) 1);
        store.initialize();

        UUID uidA = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID uidB = UUID.fromString("00000000-0000-0000-0000-000000000002");

        Files.writeString(store.stateFileFor(uidB), "x");
        Files.writeString(store.stateFileFor(uidA), "x");
        Files.writeString(store.clientsDir().resolve("ignored.txt"), "x");

        List<Path> listed = store.listPersistedStateFiles();
        assertEquals(2, listed.size());
        assertEquals(uidA + ".state", listed.get(0).getFileName().toString());
        assertEquals(uidB + ".state", listed.get(1).getFileName().toString());
    }

    @Test
    void deletesPersistedStateFile(@TempDir Path tempDir)
            throws IOException {
        ProxyClientStore store = new ProxyClientStore(tempDir, (short) 1);
        store.initialize();

        UUID uid = UUID.randomUUID();
        Files.writeString(store.stateFileFor(uid), "x");
        assertTrue(store.persistentStateExists(uid));

        store.deletePersistentState(uid);
        assertFalse(store.persistentStateExists(uid));
    }
}
