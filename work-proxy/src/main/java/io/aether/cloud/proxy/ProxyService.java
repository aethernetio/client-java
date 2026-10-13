
package io.aether.cloud.proxy;

import io.aether.api.common.Cloud;
import io.aether.api.common.CloudConfig;
import io.aether.api.common.IPAddress;
import io.aether.api.common.IPAddressAndPortsListUtil;
import io.aether.api.common.ServerDescriptor;

import io.aether.api.common.SignedKey;
import io.aether.api.common.KeySignPrivate;

import io.aether.api.common.ServerDescriptorWithGeo;
import io.aether.api.common.ServerType;
import io.aether.cloud.proxy.util.Grid;
import io.aether.cloud.proxy.util.MessageSlot;
import io.aether.logger.Log;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.aether.api.CryptoUtils;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientState;
import io.aether.cloud.client.ClientStateInFile;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.utils.futures.AFuture;
import io.aether.utils.interfaces.Destroyable;

import java.util.ArrayList;


import io.aether.api.common.CryptoLib;
import io.aether.crypto.AKey;

import io.aether.crypto.CryptoProviderFactory;
import io.aether.crypto.Signer;

import io.aether.crypto.CryptoEngine;
import io.aether.net.fastMeta.AutoFlushContext;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.AConsumer;


/**
 * Central coordinator of the customer-hosted Aether proxy.
 *
 * <p>Owns the local client registry, the local message grid and the proxy's
 * own downstream identity ({@code proxy.sid}). Upstream work sessions and
 * original-infrastructure communication live in
 * {@link ProxyClientDescriptor#upstreamClient()}, one per local client.
 *
 * <p>This class intentionally does not replicate the full
 * {@code io.aether.cloud.server.LService}. It has no P2P, no database, no
 * server-to-server replication, no access-group storage and no GeoIP.
 */
public final class ProxyService {

    public static final int MESSAGE_GRID_CAPACITY = 4096;


    private final short proxySid;
    private final URI proxyWorkPublicUri;
    private final long proxyCloudVersion = 0L;
    private final ProxyClientStore store;
    private final java.util.List<URI> upstreamRegistrationUris;
    private final java.util.Map<
            io.aether.api.common.CryptoLib,
            io.aether.crypto.PairAsymKeysSigned
            > registrationKeys;
    private final java.util.List<io.aether.crypto.SignChecker> rootSigners;

    private final java.util.Map<io.aether.api.common.CryptoLib, SignedKey>
            serverSigningKeys = new java.util.EnumMap<>(io.aether.api.common.CryptoLib.class);
    private final java.util.Map<io.aether.api.common.CryptoLib, Signer>
            serverSigners = new java.util.EnumMap<>(io.aether.api.common.CryptoLib.class);



    private final Grid messageGrid =
            new Grid(MESSAGE_GRID_CAPACITY) {
                @Override
                protected void onProcessMessage(MessageSlot slot) {
                    processLocalMessage(slot);
                }

                @Override
                protected void onMessageTimeout(MessageSlot slot) {
                    ProxyService.this.onMessageTimeout(slot);
                }
            };

    private final AtomicBoolean messageGridProcess =
            new AtomicBoolean();
    private final AtomicLong lastMessageGridFlushMs =
            new AtomicLong();

    private final ServerDescriptor proxyDescriptor;


    public ProxyService(
            short proxySid,
            URI proxyWorkPublicUri,
            java.util.List<URI> upstreamRegistrationUris,
            java.util.Map<
                    io.aether.api.common.CryptoLib,
                    io.aether.crypto.PairAsymKeysSigned
                    > registrationKeys,

            java.util.List<io.aether.crypto.SignChecker> rootSigners,

            SignedKey[] serverSigningKeys,
            KeySignPrivate[] serverSigningPrivateKeys,

            Path stateDir

    ) {
        this.proxySid = proxySid;
        this.proxyWorkPublicUri = proxyWorkPublicUri;
        this.upstreamRegistrationUris =
                java.util.List.copyOf(upstreamRegistrationUris);
        this.registrationKeys =
                java.util.Map.copyOf(registrationKeys);
        this.rootSigners = java.util.List.copyOf(rootSigners);

        this.store = new ProxyClientStore(stateDir, proxySid);
        this.store.initialize();

        initServerSigning(serverSigningKeys, serverSigningPrivateKeys);


        this.proxyDescriptor = new ServerDescriptor(

                proxySid,
                IPAddressAndPortsListUtil.of(List.of(proxyWorkPublicUri)),
                serverSigningKeysArray()

        );
    }


    public short proxySid() {
        return proxySid;
    }

    public URI proxyWorkPublicUri() {
        return proxyWorkPublicUri;
    }

    public long proxyCloudVersion() {
        return proxyCloudVersion;
    }

    public ProxyClientStore clients() {
        return store;
    }

    public java.util.List<URI> upstreamRegistrationUris() {
        return upstreamRegistrationUris;
    }

    public java.util.List<io.aether.crypto.SignChecker> rootSigners() {
        return rootSigners;
    }

    public io.aether.crypto.PairAsymKeysSigned registrationKey(
            io.aether.api.common.CryptoLib cryptoLib
    ) {
        var key = registrationKeys.get(cryptoLib);
        if (key == null) {
            throw new IllegalArgumentException(
                    "No registration key configured for " + cryptoLib
            );
        }
        return key;
    }


    public io.aether.crypto.CryptoEngine registrationPrivateEngine(
            io.aether.api.common.CryptoLib cryptoLib
    ) {
        return registrationKey(cryptoLib).toCryptoEngine();
    }

    /**
     * Canonical pattern for accepting one downstream global registration
     * stream, copied from {@code LService.globalClientApi()}.
     */





    private void initServerSigning(
            SignedKey[] signingKeys,
            KeySignPrivate[] signingPrivateKeys) {
        if (signingKeys == null || signingPrivateKeys == null) {
            throw new IllegalStateException("Proxy server signing keys are not configured");
        }

        java.util.Map<io.aether.api.common.CryptoLib, io.aether.crypto.AKey.SignPublic>
                publicByLib = new java.util.EnumMap<>(io.aether.api.common.CryptoLib.class);
        for (SignedKey signingKey : signingKeys) {
            if (signingKey == null) {
                throw new IllegalStateException("Proxy signing key entry is null");
            }
            io.aether.crypto.SignedKey publicSigned = CryptoUtils.of(signingKey);
            io.aether.crypto.AKey publicKey = publicSigned.getKey();
            if (!(publicKey instanceof io.aether.crypto.AKey.SignPublic)) {
                throw new IllegalStateException("Proxy signing key must be SIGN_PUBLIC");
            }

            if (!publicSigned.getSign().getProviderName()
                    .equalsIgnoreCase(publicKey.getProviderName())) {
                throw new IllegalStateException(
                        "Proxy signing key sign provider mismatch for " + publicKey.getProviderName());
            }

            io.aether.api.common.CryptoLib lib = cryptoLibOf(publicKey);
            if (publicByLib.put(lib, (io.aether.crypto.AKey.SignPublic) publicKey) != null) {
                throw new IllegalStateException("Duplicate proxy signing key for " + lib);
            }
        }

        java.util.Map<io.aether.api.common.CryptoLib, io.aether.crypto.AKey.SignPrivate>
                privateByLib = new java.util.EnumMap<>(io.aether.api.common.CryptoLib.class);
        for (KeySignPrivate signingPrivateKey : signingPrivateKeys) {
            if (signingPrivateKey == null) {
                throw new IllegalStateException("Proxy signing private key entry is null");
            }
            io.aether.crypto.AKey priv = CryptoUtils.of(signingPrivateKey);
            if (!(priv instanceof io.aether.crypto.AKey.SignPrivate)) {
                throw new IllegalStateException("Proxy signing private key must be SIGN_PRIVATE");
            }
            io.aether.api.common.CryptoLib lib = cryptoLibOf(priv);
            if (privateByLib.put(lib, (io.aether.crypto.AKey.SignPrivate) priv) != null) {
                throw new IllegalStateException("Duplicate proxy signing private key for " + lib);
            }
        }

        for (io.aether.api.common.CryptoLib lib : io.aether.api.common.CryptoLib.values()) {
            io.aether.crypto.AKey.SignPublic publicKey = publicByLib.get(lib);
            io.aether.crypto.AKey.SignPrivate privateKey = privateByLib.get(lib);
            if (publicKey == null) {
                throw new IllegalStateException("Missing proxy signing key for " + lib);
            }
            if (privateKey == null) {
                throw new IllegalStateException("Missing proxy signing private key for " + lib);
            }
            io.aether.crypto.CryptoProvider provider =
                    CryptoProviderFactory.getProvider(publicKey.getProviderName());
            Signer signer = provider.createSigner(publicKey, privateKey);
            byte[] probe = ("aether-proxy-signing-probe:" + lib)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            io.aether.crypto.Sign probeSign = signer.sign(probe);
            if (!signer.checkSign(probe, probeSign)) {
                throw new IllegalStateException("Proxy signing key pair mismatch for " + lib);
            }
            serverSigningKeys.put(lib, signingKeyForLib(signingKeys, lib));
            serverSigners.put(lib, signer);
        }
    }

    private static io.aether.api.common.CryptoLib cryptoLibOf(
            io.aether.crypto.AKey key) {
        return io.aether.api.common.CryptoLib.valueOf(
                key.getProviderName().toUpperCase());
    }

    private static SignedKey signingKeyForLib(
            SignedKey[] signingKeys,
            io.aether.api.common.CryptoLib lib) {
        for (SignedKey signingKey : signingKeys) {
            io.aether.crypto.SignedKey publicSigned = CryptoUtils.of(signingKey);
            if (cryptoLibOf(publicSigned.getKey()) == lib) {
                return signingKey;
            }
        }
        throw new IllegalStateException("Missing proxy signing key for " + lib);
    }

    private SignedKey[] serverSigningKeysArray() {
        java.util.List<SignedKey> keys = new java.util.ArrayList<>();
        for (io.aether.api.common.CryptoLib lib : io.aether.api.common.CryptoLib.values()) {
            SignedKey key = serverSigningKeys.get(lib);
            if (key != null) {
                keys.add(key);
            }
        }
        return keys.toArray(new SignedKey[0]);
    }

    /**
     * Issues a proxy-signed peer identity credential binding the supplied
     * peer signing public key to the given authenticated uid.
     *
     * <p>The credential is signed by the proxy's own server signing identity
     * (matching {@link #proxyDescriptor()}), never by an upstream server.
     */

    public io.aether.api.common.Sign requestSignedUid(
            UUID uid,
            io.aether.api.common.KeySignPublic peerPublicKey) {
        if (uid == null) {
            throw new IllegalArgumentException("uid is null");
        }
        if (peerPublicKey == null) {
            throw new IllegalArgumentException("peerPublicKey is null");
        }
        AKey peerKey;
        try {
            peerKey = CryptoUtils.of(peerPublicKey);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Unsupported peer public key", e);
        }
        if (!(peerKey instanceof AKey.SignPublic)) {
            throw new IllegalArgumentException("Peer key must be SIGN_PUBLIC");
        }
        CryptoLib lib = cryptoLibOf(peerKey);
        Signer signer = serverSigners.get(lib);
        if (signer == null) {
            throw new IllegalStateException("No proxy signing identity for " + lib);
        }
        io.aether.utils.dataio.DataInOut out = new io.aether.utils.dataio.DataInOut();
        io.aether.net.fastMeta.FastMeta.META_UUID.serialize(
                io.aether.net.fastMeta.MetaContext.STUB, uid, out);
        io.aether.api.common.KeySignPublic.META.serialize(
                io.aether.net.fastMeta.MetaContext.STUB, peerPublicKey, out);
        io.aether.crypto.Sign sign = signer.sign(out.toArray());
        return CryptoUtils.of(sign);
    }




    public ServerDescriptor proxyDescriptor() {
        return proxyDescriptor;
    }

    public ServerDescriptorWithGeo proxyDescriptorWithGeo() {

        return new ServerDescriptorWithGeo(
                proxySid,
                (int) (System.currentTimeMillis() / 1000L),
                proxyDescriptor.getIpAddress(),
                0.0,
                0.0,

                ServerType.WORK,
                serverSigningKeysArray()

        );

    }

    public CloudConfig proxyCloudConfig(UUID uid) {
        return new CloudConfig(
                uid,
                proxyCloudVersion,
                new Cloud(new short[]{proxySid})
        );
    }


    /**
     * Canonical enqueue pattern copied from
     * {@code AuthorizedApiImpl.enqueueMessage()}. Sender limits apply to the
     * sender; receiver UAP decides queue deadline.
     */
    public boolean enqueueMessage(
            ProxyClientDescriptor sender,
            io.aether.api.clientserverapi.Message msg
    ) {
        if (sender == null || msg == null) {
            return false;
        }
        ProxyClientDescriptor receiver = store.getByUid(msg.getUid());
        if (receiver == null) {
            return false;
        }

        byte[] data = msg.getData();
        if (data == null || data.length > sender.msgMaxBytes) {
            Log.warn(
                    "Message rejected: invalid size",
                    "fromUid", sender.uid(),
                    "toUid", msg.getUid(),
                    "size", data == null ? -1 : data.length,
                    "limit", sender.msgMaxBytes
            );
            return false;
        }

        long now = System.currentTimeMillis();
        long hardLifetimeMs =
                Math.min(
                        60_000L,
                        Math.max(0L, (long) sender.msgTimeLimit * 1_000L)
                );
        long hardDeadlineMs = now + hardLifetimeMs;

        ProxyClientUapState receiverUap = receiver.uapState();
        long uapDeadlineMs = receiverUap.queueDeadlineMs(
                now,
                hardDeadlineMs,
                receiver.isReceiveWindowOpen()
        );

        if (uapDeadlineMs == 0L) {
            return false;
        }

        if (sender.pendingMessages.incrementAndGet()
                > sender.msgQueueLimit) {
            sender.pendingMessages.decrementAndGet();
            return false;
        }

        int hardLifetimeSeconds =
                Math.max(0, Math.min(sender.msgTimeLimit, 60));

        MessageSlot slot = messageGrid.store(
                sender.uid(),
                msg.getUid(),
                data,
                (int) (now / 1000L) + hardLifetimeSeconds,
                uapDeadlineMs,
                receiverUap.generation()
        );

        if (slot == null) {
            sender.pendingMessages.decrementAndGet();
            return false;
        }
        return true;
    }

    /**
     * External inbound message: no local sender descriptor. Lifetime is
     * fixed at 60s; the receiver's UAP still gates delivery.
     */
    public void enqueueExternalInbound(
            UUID fromExternalUid,
            UUID toLocalUid,
            byte[] data
    ) {
        ProxyClientDescriptor receiver = store.getByUid(toLocalUid);
        if (receiver == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long hardDeadlineMs = now + 60_000L;
        ProxyClientUapState receiverUap = receiver.uapState();
        long uapDeadlineMs = receiverUap.queueDeadlineMs(
                now,
                hardDeadlineMs,
                receiver.isReceiveWindowOpen()
        );
        if (uapDeadlineMs == 0L) {
            return;
        }
        MessageSlot slot = messageGrid.store(
                fromExternalUid,
                toLocalUid,
                data,
                (int) (now / 1000L) + 60,
                uapDeadlineMs,
                receiverUap.generation()
        );
        if (slot != null) {
            flushMessageGrid();
        }
    }

    public void completeMessage(MessageSlot slot) {
        UUID senderUid = slot.from;
        if (!slot.free() || senderUid == null) {
            return;
        }
        ProxyClientDescriptor sender = store.getByUid(senderUid);
        if (sender != null) {
            sender.pendingMessages.updateAndGet(
                    value -> value > 0 ? value - 1 : 0
            );
        }
    }


    public boolean hasPendingMessages() {
        return messageGrid.isRequests();
    }

    public void flushMessageGrid() {
        if (!messageGridProcess.compareAndSet(false, true)) {
            return;
        }
        try {
            messageGrid.processGrid();
        } finally {
            messageGridProcess.set(false);
        }
    }


    private void processLocalMessage(MessageSlot slot) {
        UUID to = slot.to;
        ProxyClientDescriptor receiver = store.getByUid(to);
        if (receiver == null) {
            Log.warn(
                    "ProxyService: dropping message to unknown local uid",
                    "to",
                    to
            );
            completeMessage(slot);
            return;
        }

        long now = System.currentTimeMillis();

        if (slot.uapDeadlineMs > 0L
                && now >= slot.uapDeadlineMs) {
            Log.trace(
                    "Dropping queued message after UAP deadline",
                    "fromUid", slot.from,
                    "toUid", slot.to,
                    "slotGeneration", slot.uapGeneration,
                    "currentGeneration", receiver.uapState().generation()
            );
            completeMessage(slot);
            return;
        }

        try {
            if (receiver.sendMessageToDownstream(slot)) {
                completeMessage(slot);
            }
        } catch (Throwable error) {
            Log.error(
                    "Failed to deliver queued message",
                    error,
                    "fromUid", slot.from,
                    "toUid", slot.to
            );
            slot.recoveryToSend();
        }
    }

    private void onMessageTimeout(MessageSlot slot) {
        completeMessage(slot);
    }


    public void recordClientActivity(ProxyClientDescriptor client, long now) {
        client.recordActivity(now);
    }

    public IPAddress clientIpFromContext(
            io.aether.net.fastMeta.MetaContext ctx
    ) {
        Object addr = ctx.getProperty(1);
        if (!(addr instanceof String address)
                || address.isBlank()) {
            return new io.aether.api.common.IPAddressV4(
                    new byte[]{0, 0, 0, 0}
            );
        }
        try {
            String host = address;
            if (address.indexOf(':') >= 0
                    && !address.startsWith("[")) {
                host = "[" + address + "]";
            }
            return io.aether.api.common.IPAddressUtil.of(
                    URI.create("tcp://" + host)
            )[0];
        } catch (RuntimeException error) {
            return new io.aether.api.common.IPAddressV4(
                    new byte[]{0, 0, 0, 0}
            );
        }
    }

    /**
     * Register a freshly created downstream client. When a persistent state
     * directory is configured the caller-supplied state is persisted to
     * {@code <state.dir>/clients/<uid>.state} via {@link ClientStateInFile}
     * BEFORE the runtime descriptor becomes visible to the work listener.
     *
     * <p>Duplicate UID/alias and pre-existing state file are rejected before
     * any bytes are written. On any failure the temporary state file and any
     * runtime resources are cleaned up.
     */
    public ProxyClientDescriptor registerNewClient(ClientStateInMemory source) {
        validateClientState(source);
        UUID uid = source.getUid();
        UUID alias = source.getAlias();

        if (store.getByUid(uid) != null) {
            throw new IllegalStateException(
                    "Proxy client is already registered: " + uid);
        }
        if (alias != null && store.getByAlias(alias) != null) {
            throw new IllegalStateException(
                    "Proxy client alias is already registered: " + alias);
        }

        if (!store.persistent()) {
            return attachClientInternal(source);
        }

        Path target = store.stateFileFor(uid);
        if (Files.exists(target)) {
            throw new IllegalStateException(
                    "Persistent state already exists for uid " + uid);
        }

        ClientStateInFile persistent = null;
        try {

            persistent = new ClientStateInFile(
                    uid,
                    java.util.List.of(),
                    target.toFile());

            persistent.loadState(source.save());
            persistent.saveState();
            store.secureStateFile(target);
            return attachClientInternal(persistent);
        } catch (Throwable error) {
            if (persistent != null) {
                try {
                    persistent.destroy(true);
                } catch (Throwable ignored) {
                    // best effort
                }
            }
            try {
                Files.deleteIfExists(target);
            } catch (java.io.IOException deleteError) {
                error.addSuppressed(deleteError);
            }
            throw error;
        }
    }



    /**
     * Restore every persisted client state found under
     * {@code <state.dir>/clients/} into runtime descriptors. Must be called
     * before the work listener is opened.
     *
     * <p>Two-phase startup: first every state file is loaded and validated;
     * only after all of them are known-good are runtime descriptors created.
     * Any failure rolls back every opened {@link ClientStateInFile} and any
     * runtime resources already created, without deleting files.
     */
    public void restorePersistentClients() {
        if (!store.persistent()) {
            return;
        }
        java.util.List<Path> files = store.listPersistedStateFiles();

        java.util.List<ClientStateInFile> loaded = new ArrayList<>();
        try {
            for (Path file : files) {
                UUID fileNameUid = uidFromStateFile(file);
                if (fileNameUid == null) {
                    throw new IllegalStateException(
                            "Invalid persistent state filename: " + file);
                }


                ClientStateInFile state;
                try {
                    state = new ClientStateInFile(
                            fileNameUid,
                            java.util.List.of(),
                            file.toFile());
                } catch (Throwable error) {
                    throw new IllegalStateException(
                            "Corrupt persistent client state: " + file,
                            error);
                }
                loaded.add(state);
                try {
                    validateClientState(state);
                } catch (RuntimeException error) {
                    throw new IllegalStateException(
                            "Corrupt persistent client state: " + file, error);
                }

                UUID uid = state.getUid();
                if (!fileNameUid.equals(uid)) {
                    throw new IllegalStateException(
                            "Persistent client state uid mismatch: " + file
                                    + " (state uid=" + uid + ")");
                }
                store.secureStateFile(file);
            }
        } catch (Throwable error) {
            for (ClientStateInFile state : loaded) {
                try {
                    state.destroy(true);
                } catch (Throwable ignored) {
                    // best effort
                }
            }
            throw error;
        }

        java.util.List<ProxyClientDescriptor> attached = new ArrayList<>();
        try {
            for (ClientStateInFile state : loaded) {
                attached.add(attachClientInternal(state));
            }
        } catch (Throwable error) {
            for (ProxyClientDescriptor descriptor : attached) {
                destroyRuntime(descriptor, true);
                store.remove(descriptor.uid());
            }
            for (ClientStateInFile state : loaded) {
                try {
                    state.destroy(true);
                } catch (Throwable ignored) {
                    // best effort
                }
            }
            throw error;
        }

        Log.info(
                "Proxy persistent clients restored",
                "count", loaded.size(),
                "stateDir", store.stateDir()
        );
    }


    private static UUID uidFromStateFile(Path file) {
        String name = file.getFileName().toString();
        if (!name.endsWith(".state")) {
            return null;
        }
        String raw = name.substring(0, name.length() - ".state".length());
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }


    /**
     * Create the runtime {@link ProxyClientDescriptor} for an already-valid
     * state and add it to the in-memory registry. On any failure the
     * upstream client and the state scheduler are destroyed before
     * propagating the error.
     */
    private ProxyClientDescriptor attachClientInternal(ClientStateInMemory state) {
        validateClientState(state);
        UUID uid = state.getUid();

        AetherCloudClient upstream = null;
        try {
            AKey.Symmetric masterKey =
                    CryptoUtils.of(state.getMasterKey()).asSymmetric();
            upstream = new AetherCloudClient(
                    state,
                    "proxy-upstream-" + uid);
            upstream.onMessage((fromUid, data) ->
                    enqueueExternalInbound(fromUid, uid, data));
            ProxyClientDescriptor descriptor = new ProxyClientDescriptor(
                    this,
                    uid,
                    state.getAlias(),
                    masterKey,
                    upstream,
                    state);
            store.save(descriptor);
            return descriptor;
        } catch (Throwable error) {
            if (upstream != null) {
                try {
                    upstream.destroy(true);
                } catch (Throwable ignored) {
                    // best effort
                }
            }
            if (state instanceof Destroyable destroyable) {
                try {
                    destroyable.destroy(true);
                } catch (Throwable ignored) {
                    // best effort
                }
            }
            throw error;
        }
    }



    /**
     * Destroy every per-client upstream {@link AetherCloudClient} and stop
     * any periodic state saving. Persistent files are NOT deleted.
     */
    public AFuture destroy(boolean force) {
        java.util.List<AFuture> futures = new ArrayList<>();
        for (ProxyClientDescriptor descriptor :
                new ArrayList<>(store.all())) {
            futures.add(destroyRuntime(descriptor, force));
        }
        return AFuture.all(futures.toArray(new AFuture[0]));
    }

    /**
     * Permanently delete a client: destroy its runtime resources, delete its
     * persisted state file and only then remove it from the registry.
     */
    public AFuture deleteClient(UUID uid, boolean force) {
        AFuture result = AFuture.make();
        ProxyClientDescriptor descriptor = store.getByUid(uid);
        if (descriptor == null) {
            try {
                store.deletePersistentState(uid);
                result.done();
            } catch (java.io.IOException e) {
                result.error(e);
            }
            return result;
        }
        destroyRuntime(descriptor, force).to(
                () -> {
                    try {
                        store.deletePersistentState(uid);
                        store.remove(uid);
                        result.done();
                    } catch (Throwable error) {
                        result.error(error);
                    }
                },
                error -> result.error(error)
        );
        return result;
    }

    /**
     * Destroy the runtime half of one descriptor: upstream client and the
     * {@link ClientStateInFile} scheduler. Does NOT touch persistent files.
     */
    private AFuture destroyRuntime(
            ProxyClientDescriptor descriptor,
            boolean force
    ) {
        java.util.List<AFuture> futures = new ArrayList<>();
        futures.add(descriptor.upstreamClient().destroy(force));
        ClientState state = descriptor.upstreamState();
        if (state instanceof Destroyable destroyable) {
            futures.add(destroyable.destroy(force));
        }
        return AFuture.all(futures.toArray(new AFuture[0]));
    }

    private static void validateClientState(ClientStateInMemory state) {
        UUID uid = state.getUid();
        if (uid == null) {
            throw new IllegalStateException("Client state has no uid");
        }
        if (state.getAlias() == null) {
            throw new IllegalStateException(
                    "Client state has no alias: " + uid);
        }
        if (state.getMasterKey() == null) {
            throw new IllegalStateException(
                    "Client state has no master key: " + uid);
        }
        if (state.getCryptoLib() == null) {
            throw new IllegalStateException(
                    "Client state has no crypto lib: " + uid);
        }
        if (state.getCloud(uid) == null) {
            throw new IllegalStateException(
                    "Client state has no own upstream cloud: " + uid);
        }
    }

}