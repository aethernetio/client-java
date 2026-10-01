
package io.aether.cloud.proxy;


import io.aether.logger.Log;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local registry of clients registered through this proxy.
 *
 * <p>Keeps in-memory indexes by uid and by alias for O(1) resolution, and —
 * when a persistent state directory is configured — owns the on-disk layout:
 * {@code <state.dir>/proxy.meta} and {@code <state.dir>/clients/<uid>.state}.
 * The contents of {@code <uid>.state} are produced and consumed by
 * {@link io.aether.cloud.client.ClientStateInFile}; this class only manages
 * file locations, metadata, listing and deletion.
 */
public final class ProxyClientStore {

    private final Path stateDir;
    private final Path clientsDir;
    private final Path metadataFile;
    private final short proxySid;

    private final ConcurrentHashMap<UUID, ProxyClientDescriptor> byUid =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ProxyClientDescriptor> byAlias =
            new ConcurrentHashMap<>();

    public ProxyClientStore(Path stateDir, short proxySid) {
        this.stateDir = stateDir == null
                ? null
                : stateDir.toAbsolutePath().normalize();
        this.clientsDir = this.stateDir == null
                ? null
                : this.stateDir.resolve("clients");
        this.metadataFile = this.stateDir == null
                ? null
                : this.stateDir.resolve("proxy.meta");
        this.proxySid = proxySid;
    }

    public boolean persistent() {
        return stateDir != null;
    }

    public Path stateDir() {
        return stateDir;
    }

    public Path clientsDir() {
        return clientsDir;
    }

    public Path stateFileFor(UUID uid) {
        if (clientsDir == null) {
            throw new IllegalStateException(
                    "ProxyClientStore is not persistent");
        }
        return clientsDir.resolve(uid + ".state");
    }

    public void initialize() {
        if (stateDir == null) {
            return;
        }
        try {
            Files.createDirectories(clientsDir);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot create proxy state directory: " + stateDir, e);
        }
        applyPermissions();
        if (Files.exists(metadataFile)) {
            verifyMetadata();
        } else {
            writeMetadata();
        }
    }

    private void applyPermissions() {
        try {
            var perms = PosixFilePermissions.fromString("rwx------");
            Files.setPosixFilePermissions(stateDir, perms);
            Files.setPosixFilePermissions(clientsDir, perms);
        } catch (UnsupportedOperationException | IOException e) {
            Log.warn(
                    "ProxyClientStore: POSIX permissions not enforced",
                    "stateDir", stateDir);
        }
    }

    private void verifyMetadata() {
        short stored;
        try {
            stored = Short.parseShort(Files.readString(metadataFile).trim());
        } catch (IOException | NumberFormatException e) {
            throw new IllegalStateException(
                    "Cannot read proxy.meta at " + metadataFile, e);
        }
        if (stored != proxySid) {
            throw new IllegalStateException(
                    "Proxy SID mismatch: persistent state belongs to SID "
                            + stored + ", configuration requests SID "
                            + proxySid);
        }
    }

    private void writeMetadata() {
        Path tmp = metadataFile.resolveSibling(
                metadataFile.getFileName() + ".tmp");
        try {
            Files.writeString(
                    tmp,
                    Short.toString(proxySid),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            atomicMove(tmp, metadataFile);
            try {
                Files.setPosixFilePermissions(
                        metadataFile,
                        PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException | IOException ignored) {
                // best effort
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot write proxy.meta at " + metadataFile, e);
        }
    }

    private static void atomicMove(Path from, Path to) throws IOException {
        try {
            Files.move(
                    from,
                    to,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public List<Path> listPersistedStateFiles() {
        if (clientsDir == null || !Files.isDirectory(clientsDir)) {
            return List.of();
        }
        List<Path> out = new ArrayList<>();
        try (var stream = Files.list(clientsDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".state"))
                    .forEach(out::add);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot list persisted client states in " + clientsDir, e);
        }
        out.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return out;
    }

    public void deletePersistentState(UUID uid) throws IOException {
        if (clientsDir == null) {
            return;
        }
        Files.deleteIfExists(stateFileFor(uid));
    }

    public ProxyClientDescriptor getByUid(UUID uid) {
        if (uid == null) {
            return null;
        }
        return byUid.get(uid);
    }

    public ProxyClientDescriptor getByAlias(UUID alias) {
        if (alias == null) {
            return null;
        }
        return byAlias.get(alias);
    }

    public boolean isLocal(UUID uid) {
        return uid != null && byUid.containsKey(uid);
    }

    public void save(ProxyClientDescriptor client) {
        ProxyClientDescriptor existing =
                byUid.putIfAbsent(client.uid(), client);
        if (existing != null && existing != client) {
            throw new IllegalStateException(
                    "ProxyClientStore: duplicate UID " + client.uid());
        }
        if (client.alias() != null) {
            ProxyClientDescriptor aliasConflict =
                    byAlias.putIfAbsent(client.alias(), client);
            if (aliasConflict != null && aliasConflict != client) {
                byUid.remove(client.uid(), client);
                throw new IllegalStateException(
                        "ProxyClientStore: duplicate alias "
                                + client.alias());
            }
        }
    }


    public void remove(UUID uid) {
        ProxyClientDescriptor removed = byUid.remove(uid);
        if (removed != null && removed.alias() != null) {
            byAlias.remove(removed.alias(), removed);
        }
    }

    public boolean persistentStateExists(UUID uid) {
        return persistent() && Files.exists(stateFileFor(uid));
    }

    public void secureStateFile(Path stateFile) {
        try {
            Files.setPosixFilePermissions(
                    stateFile,
                    PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException e) {
            Log.warn(
                    "Proxy client state permissions could not be restricted",
                    "file", stateFile);
        }
    }


    public Collection<ProxyClientDescriptor> all() {
        return byUid.values();
    }

    public int size() {
        return byUid.size();
    }
}