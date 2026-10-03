
package io.aether.cloud.client;

import io.aether.api.CryptoUtils;
import io.aether.api.PeerIdentityCrypto;
import io.aether.api.clienttypes.PeerIdentityCredentials;
import io.aether.api.common.CryptoLib;
import io.aether.api.common.KeySignPrivate;
import io.aether.api.common.KeySignPublic;
import io.aether.api.common.PeerIdentity;
import io.aether.crypto.AKey;
import io.aether.crypto.CryptoProvider;
import io.aether.crypto.CryptoProviderFactory;
import io.aether.crypto.PairSignKeys;
import io.aether.crypto.Sign;
import io.aether.crypto.Signer;
import io.aether.logger.Log;
import io.aether.utils.futures.ARFuture;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import io.aether.utils.futures.AFuture;
import io.aether.utils.interfaces.Destroyable;
import java.util.concurrent.CancellationException;


/**
 * Package-private helper responsible for the peer identity lifecycle:
 * validating persisted credentials, generating a fresh signing pair,
 * issuing a server-signed {@link PeerIdentity}, verifying the returned
 * credential and persisting the successful result.
 *
 * <p>Concurrent callers share a single in-flight issuance (lock-free CAS).
 * Failed issuance clears the in-flight marker so the next caller can retry.
 */
final class PeerIdentityManager implements Destroyable {


    private static final byte[] PROBE =
            "AETHER_PEER_IDENTITY_LOCAL_PAIR_CHECK_V1\0".getBytes(StandardCharsets.US_ASCII);

    private final AetherCloudClient client;
    private final AtomicReference<ARFuture<PeerIdentity>> inFlight = new AtomicReference<>();

    PeerIdentityManager(AetherCloudClient client) {
        this.client = client;
    }



    ARFuture<PeerIdentity> getOrIssue() {
        while (true) {
            ARFuture<PeerIdentity> existing = inFlight.get();
            if (existing != null) {
                return existing;
            }

            PeerIdentity persisted = validatePersisted();

            existing = inFlight.get();
            if (existing != null) {
                return existing;
            }
            if (persisted != null) {
                return ARFuture.of(persisted);
            }

            ARFuture<PeerIdentity> created = ARFuture.make();
            if (inFlight.compareAndSet(null, created)) {
                beginIssueLifecycle(created);
                return created;
            }
        }
    }


    private void beginIssueLifecycle(ARFuture<PeerIdentity> result) {
        if (client.destroyer.isDestroyed()) {
            inFlight.compareAndSet(result, null);
            result.tryError(new java.util.concurrent.CancellationException(
                    "Client destroyed before peer identity issuance"));
            return;
        }
        if (client.getUid() == null) {
            client.startFuture.to(() -> proceedWhenReady(result));
            client.startFuture.onError(err -> {
                inFlight.compareAndSet(result, null);
                result.tryError(err);
            });
        } else {
            proceedWhenReady(result);
        }
    }


    private void proceedWhenReady(ARFuture<PeerIdentity> result) {
        if (result.isFinalStatus()) {
            return;
        }
        if (client.destroyer.isDestroyed()) {
            inFlight.compareAndSet(result, null);
            result.tryError(new java.util.concurrent.CancellationException(
                    "Client destroyed before peer identity issuance"));
            return;
        }

        PeerIdentity revalidated = validatePersisted();
        if (revalidated != null) {
            inFlight.compareAndSet(result, null);
            result.tryDone(revalidated);
            return;
        }

        client.getAuthApi(api -> {
            if (result.isFinalStatus() || client.destroyer.isDestroyed()) {
                return;
            }
            try {

                CryptoLib lib = client.getCryptLib();
                CryptoProvider provider = CryptoProviderFactory.getProvider(lib.name());
                PairSignKeys pair = provider.createSignKeys();
                KeySignPublic publicDto = CryptoUtils.of(pair.getPublicKey());
                AKey.SignPrivate privateKey = pair.getPrivateKey();

                api.issuePeerIdentity(publicDto).to(identity -> {
                    if (!validateIssued(identity, publicDto, privateKey)) {
                        inFlight.compareAndSet(result, null);
                        result.tryError(new IllegalStateException(
                                "Server returned invalid PeerIdentity"));
                        return;
                    }
                    try {
                        PeerIdentityCredentials creds = new PeerIdentityCredentials(
                                identity,
                                CryptoUtils.of(privateKey));
                        client.getClientState().setPeerIdentityCredentials(creds);
                        try {
                            client.getClientState().saveState();
                        } catch (RuntimeException persistError) {
                            client.getClientState().setPeerIdentityCredentials(null);
                            throw persistError;
                        }
                        inFlight.compareAndSet(result, null);
                        result.tryDone(identity);
                    } catch (RuntimeException e) {
                        inFlight.compareAndSet(result, null);
                        result.tryError(e);
                    }
                }).onError(err -> {
                    inFlight.compareAndSet(result, null);
                    result.tryError(err);
                });
            } catch (RuntimeException e) {
                inFlight.compareAndSet(result, null);
                result.tryError(e);
            }
        });
    }


    private PeerIdentity validatePersisted() {
        PeerIdentityCredentials creds = client.getClientState().getPeerIdentityCredentials();
        if (creds == null) {
            return null;
        }
        try {
            PeerIdentity identity = creds.getIdentity();
            KeySignPrivate privKeyDto = creds.getPrivateKey();
            if (identity == null || privKeyDto == null) {
                clear();
                return null;
            }
            var data = identity.getData();
            if (data == null || data.getUid() == null || data.getKey() == null) {
                clear();
                return null;
            }
            if (!data.getUid().equals(client.getUid())) {
                clear();
                return null;
            }
            String lib = client.getCryptLib().name();

            AKey.SignPublic publicKey;
            AKey.SignPrivate privateKey;
            try {
                publicKey = CryptoUtils.of(data.getKey());
                privateKey = CryptoUtils.of(privKeyDto);
            } catch (RuntimeException e) {
                clear();
                return null;
            }
            if (publicKey.getProviderName() == null
                    || !publicKey.getProviderName().equalsIgnoreCase(lib)
                    || privateKey.getProviderName() == null
                    || !privateKey.getProviderName().equalsIgnoreCase(lib)) {
                clear();
                return null;
            }

            if (!PeerIdentityCrypto.verify(identity, client.getClientState().getRootSigners())) {
                clear();
                return null;
            }
            CryptoProvider provider = CryptoProviderFactory.getProvider(lib);
            Signer signer = provider.createSigner(publicKey, privateKey);
            Sign sign = signer.sign(PROBE);
            if (!signer.checkSign(PROBE, sign)) {
                clear();
                return null;
            }
            return identity;
        } catch (RuntimeException e) {
            clear();
            return null;
        }
    }

    private boolean validateIssued(
            PeerIdentity identity,
            KeySignPublic expectedPublic,
            AKey.SignPrivate privateKey) {
        if (identity == null) {
            return false;
        }
        var data = identity.getData();
        if (data == null || data.getUid() == null || data.getKey() == null) {
            return false;
        }
        if (!data.getUid().equals(client.getUid())) {
            return false;
        }
        String lib = client.getCryptLib().name();

        AKey.SignPublic publicKey;
        AKey.SignPublic expectedAKey;
        try {
            publicKey = CryptoUtils.of(data.getKey());
            expectedAKey = CryptoUtils.of(expectedPublic);
        } catch (RuntimeException e) {
            return false;
        }
        if (publicKey.getProviderName() == null
                || !publicKey.getProviderName().equalsIgnoreCase(lib)) {
            return false;
        }
        if (expectedAKey.getProviderName() == null
                || !expectedAKey.getProviderName().equalsIgnoreCase(lib)) {
            return false;
        }
        if (!Arrays.equals(publicKey.getData(), expectedAKey.getData())) {
            return false;
        }

        if (!PeerIdentityCrypto.verify(identity, client.getClientState().getRootSigners())) {
            return false;
        }
        try {
            CryptoProvider provider = CryptoProviderFactory.getProvider(lib);
            Signer signer = provider.createSigner(publicKey, privateKey);
            Sign sign = signer.sign(PROBE);
            if (!signer.checkSign(PROBE, sign)) {
                return false;
            }
        } catch (RuntimeException e) {
            return false;
        }
        return true;
    }

    private void clear() {
        try {
            client.getClientState().setPeerIdentityCredentials(null);
        } catch (RuntimeException e) {
            Log.warn("Failed to clear invalid peer identity credentials",
                    "error", String.valueOf(e));
        }
    }

    @Override
    public AFuture destroy(boolean force) {
        ARFuture<PeerIdentity> current = inFlight.getAndSet(null);
        if (current != null && !current.isFinalStatus()) {
            current.tryError(new CancellationException(
                    "Peer identity lifecycle canceled: client destroyed"));
        }
        return AFuture.completed();
    }

}