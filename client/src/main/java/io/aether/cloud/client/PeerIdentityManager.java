
package io.aether.cloud.client;

import io.aether.api.CryptoUtils;
import io.aether.api.clienttypes.PeerIdentityCredentials;
import io.aether.api.common.CryptoLib;
import io.aether.api.common.KeySignPrivate;
import io.aether.api.common.KeySignPublic;
import io.aether.api.common.Sign;
import io.aether.crypto.AKey;
import io.aether.crypto.CryptoProvider;
import io.aether.crypto.CryptoProviderFactory;
import io.aether.crypto.PairSignKeys;
import io.aether.crypto.Signer;
import io.aether.logger.Log;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.Destroyable;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

final class PeerIdentityManager implements Destroyable {

    private static final byte[] PROBE =
            "AETHER_PEER_IDENTITY_LOCAL_PAIR_CHECK_V1\0".getBytes(StandardCharsets.US_ASCII);

    private final AetherCloudClient client;
    private final AtomicReference<Attempt> inFlight = new AtomicReference<>();

    private enum AttemptState {
        ACTIVE, COMMITTING, CANCELLED
    }

    private static final class Attempt {
        final ARFuture<PeerIdentityCredentials> future = ARFuture.make();
        final AtomicReference<AttemptState> state = new AtomicReference<>(AttemptState.ACTIVE);
        volatile ARFuture<Sign> pendingSignedUid;
    }

    PeerIdentityManager(AetherCloudClient client) {
        this.client = client;
    }

    ARFuture<PeerIdentityCredentials> getOrIssue() {
        while (true) {
            Attempt existing = inFlight.get();
            if (existing != null) {
                return existing.future;
            }
            PeerIdentityCredentials persisted = validatePersisted();
            existing = inFlight.get();
            if (existing != null) {
                return existing.future;
            }
            if (persisted != null) {
                client.onPeerIdentityReady(persisted);
                return ARFuture.of(persisted);
            }
            Attempt created = new Attempt();
            if (inFlight.compareAndSet(null, created)) {
                beginIssueLifecycle(created);
                return created.future;
            }
        }
    }

    private void beginIssueLifecycle(Attempt attempt) {
        if (client.destroyer.isDestroyed()) {
            failAttempt(attempt, new CancellationException(
                    "Client destroyed before peer identity issuance"));
            return;
        }
        if (client.getUid() == null) {
            client.startFuture.to(() -> proceedWhenReady(attempt));
            client.startFuture.onError(err -> failAttempt(attempt, err));
        } else {
            proceedWhenReady(attempt);
        }
    }

    private void proceedWhenReady(Attempt attempt) {
        if (attempt.state.get() != AttemptState.ACTIVE) {
            return;
        }
        if (client.destroyer.isDestroyed()) {
            failAttempt(attempt, new CancellationException(
                    "Client destroyed before peer identity issuance"));
            return;
        }
        PeerIdentityCredentials revalidated = validatePersisted();
        if (revalidated != null) {
            if (attempt.state.compareAndSet(AttemptState.ACTIVE, AttemptState.COMMITTING)) {
                inFlight.compareAndSet(attempt, null);
                client.onPeerIdentityReady(revalidated);
                attempt.future.tryDone(revalidated);
            }
            return;
        }
        client.getAuthApi(api -> issueViaServer(attempt, api));
    }

    private void issueViaServer(Attempt attempt, io.aether.api.clientserverapi.AuthorizedApi api) {
        if (attempt.state.get() != AttemptState.ACTIVE) {
            return;
        }
        if (client.destroyer.isDestroyed()) {
            failAttempt(attempt, new CancellationException(
                    "Client destroyed before peer identity issuance"));
            return;
        }
        try {
            CryptoLib lib = client.getCryptLib();
            CryptoProvider provider = CryptoProviderFactory.getProvider(lib.name());
            PairSignKeys pair = provider.createSignKeys();
            KeySignPublic publicDto = CryptoUtils.of(pair.getPublicKey());
            AKey.SignPrivate privateKey = pair.getPrivateKey();

            ARFuture<Sign> pending = ARFuture.make();
            attempt.pendingSignedUid = pending;

            api.requestSignedUid(publicDto);

            pending.to(signedUid -> commitIssued(
                    attempt, signedUid, publicDto, privateKey
            )).onError(err -> failAttempt(attempt, err));
        } catch (RuntimeException e) {
            failAttempt(attempt, e);
        }
    }

    private void commitIssued(
            Attempt attempt,
            Sign signedUid,
            KeySignPublic publicDto,
            AKey.SignPrivate privateKey
    ) {
        if (attempt.state.get() != AttemptState.ACTIVE) {
            return;
        }
        if (!validateSignedUid(signedUid, publicDto)) {
            failAttempt(attempt, new IllegalStateException(
                    "Server returned invalid signed uid"));
            return;
        }
        PeerIdentityCredentials creds = new PeerIdentityCredentials(
                signedUid, publicDto, CryptoUtils.of(privateKey));
        if (!attempt.state.compareAndSet(
                AttemptState.ACTIVE, AttemptState.COMMITTING)) {
            return;
        }
        try {
            client.getClientState().setPeerIdentityCredentials(creds);
            try {
                client.getClientState().saveState();
            } catch (RuntimeException persistError) {
                client.getClientState().setPeerIdentityCredentials(null);
                throw persistError;
            }
            inFlight.compareAndSet(attempt, null);
            client.onPeerIdentityReady(creds);
            attempt.future.tryDone(creds);
        } catch (RuntimeException e) {
            inFlight.compareAndSet(attempt, null);
            attempt.future.tryError(e);
        }
    }

    void onSignedUid(Sign sign) {
        if (sign == null) {
            return;
        }
        Attempt current = inFlight.get();
        if (current == null) {
            Log.warn("Received signedUid without a pending attempt; dropping");
            return;
        }
        ARFuture<Sign> pending = current.pendingSignedUid;
        if (pending == null) {
            Log.warn("Received signedUid before requestSignedUid was sent; dropping");
            return;
        }
        pending.tryDone(sign);
    }

    private PeerIdentityCredentials validatePersisted() {
        PeerIdentityCredentials creds = client.getClientState().getPeerIdentityCredentials();
        if (creds == null) {
            return null;
        }
        try {
            Sign sign = creds.getSign();
            KeySignPublic publicDto = creds.getPublicKey();
            KeySignPrivate privKeyDto = creds.getPrivateKey();
            if (sign == null || publicDto == null || privKeyDto == null) {
                clear();
                return null;
            }
            String lib = client.getCryptLib().name();

            AKey.SignPublic publicKey;
            AKey.SignPrivate privateKey;
            try {
                publicKey = CryptoUtils.of(publicDto);
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

            if (!validateSignedUid(sign, publicDto)) {
                clear();
                return null;
            }

            CryptoProvider provider = CryptoProviderFactory.getProvider(lib);
            Signer signer = provider.createSigner(publicKey, privateKey);
io.aether.crypto.Sign probe = signer.sign(PROBE);
            if (!signer.checkSign(PROBE, probe)) {
                clear();
                return null;
            }
            return creds;
        } catch (RuntimeException e) {
            clear();
            return null;
        }
    }

    private boolean validateSignedUid(Sign signedUid, KeySignPublic expectedPublic) {
        if (signedUid == null || expectedPublic == null) {
            return false;
        }
        try {
            AKey.SignPublic expectedAKey = CryptoUtils.of(expectedPublic);
            return expectedAKey.getProviderName() != null
                    && expectedAKey.getProviderName().equalsIgnoreCase(
                            client.getCryptLib().name());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void failAttempt(Attempt attempt, Throwable error) {
        if (attempt.state.compareAndSet(AttemptState.ACTIVE, AttemptState.CANCELLED)) {
            inFlight.compareAndSet(attempt, null);
        }
        attempt.future.tryError(error);
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
        Attempt current = inFlight.get();
        if (current != null) {
            if (current.state.compareAndSet(AttemptState.ACTIVE, AttemptState.CANCELLED)) {
                inFlight.compareAndSet(current, null);
                current.future.tryError(new CancellationException(
                        "Peer identity lifecycle canceled: client destroyed"));
            }
        }
        return AFuture.completed();
    }



}