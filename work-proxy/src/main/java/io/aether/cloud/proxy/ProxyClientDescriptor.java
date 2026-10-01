
package io.aether.cloud.proxy;

import io.aether.api.CryptoUtils;
import io.aether.api.clientserverapi.ClientApiSafeRemote;
import io.aether.api.clientserverapi.Message;
import io.aether.api.clientserverapi.WebRtcSession;
import io.aether.api.common.Cloud;
import io.aether.api.common.CloudConfig;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.crypto.AKey;
import io.aether.crypto.CryptoEngine;
import io.aether.crypto.PairSymKeys;
import io.aether.logger.Log;
import io.aether.cloud.proxy.util.MessageSlot;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Proxy-side view of one client that registered through this proxy.
 *
 * <p>Holds the client's master key, a corresponding derived work-session
 * crypto engine for {@code proxy.sid}, the upstream {@link AetherCloudClient}
 * that represents the same UID on the original Aether infrastructure, and
 * the local User Activity Profile state used for receive-window scheduling.
 */
public final class ProxyClientDescriptor {

    private final ProxyService service;
    private final UUID uid;
    private final UUID alias;
    private final AKey.Symmetric masterKey;
    private final CryptoEngine symmetricEngine;
    private final AetherCloudClient upstreamClient;
    private final ClientStateInMemory upstreamState;

    public final AtomicInteger pendingMessages = new AtomicInteger();
    public volatile WebRtcSession webRtcSession;
    public volatile int msgMaxBytes = 65536;
    public volatile int msgQueueLimit = 1000;
    public volatile int msgTimeLimit = 60;

    private volatile ClientApiSafeRemote clientApiSafeRemote;
    private final AtomicReference<ProxyClientUapState> uapState =
            new AtomicReference<>(ProxyClientUapState.EMPTY);

    public ProxyClientDescriptor(
            ProxyService service,
            UUID uid,
            UUID alias,
            AKey.Symmetric masterKey,
            AetherCloudClient upstreamClient,
            ClientStateInMemory upstreamState
    ) {
        this.service = service;
        this.uid = uid;
        this.alias = alias;
        this.masterKey = masterKey;
        this.upstreamClient = upstreamClient;
        this.upstreamState = upstreamState;

        short proxySid = service.proxySid();
        PairSymKeys keys = masterKey
                .getCryptoProvider()
                .createKeyForServer(masterKey, proxySid);

        this.symmetricEngine = CryptoEngine.of(
                keys.getServerToClient().asSymmetric().toCryptoEngine(),
                keys.getClientToServer().asSymmetric().toCryptoEngine()
        );
    }

    public UUID uid() {
        return uid;
    }

    public UUID alias() {
        return alias;
    }

    public AKey.Symmetric masterKey() {
        return masterKey;
    }

    public CryptoEngine symmetricEngine() {
        return symmetricEngine;
    }

    public AetherCloudClient upstreamClient() {
        return upstreamClient;
    }


    public ClientStateInMemory upstreamState() {
        return upstreamState;
    }


    public ProxyClientUapState uapState() {
        return uapState.get();
    }

    public void recordActivity(long now) {
        uapState.updateAndGet(current -> current.recordActivity(now));
    }

    public void applyPing(
            long now,
            long nextPingDelayMs,
            long rxWindowMs
    ) {
        uapState.updateAndGet(current ->
                current.applyPing(now, nextPingDelayMs, rxWindowMs)
        );
    }

    public void setReceiveWindow(
            long now,
            long startsInMs,
            long durationMs
    ) {
        uapState.updateAndGet(current ->
                current.setReceiveWindow(now, startsInMs, durationMs)
        );
    }

    public void openTemporaryReceiveWindow(long now, long durationMs) {
        uapState.updateAndGet(current ->
                current.openTemporaryWindow(now, durationMs)
        );
    }

    public boolean isReceiveWindowOpen() {
        return uapState.get().isReceiveWindowOpen(System.currentTimeMillis());
    }

    public void setSluice(ClientApiSafeRemote remote) {
        this.clientApiSafeRemote = remote;
    }

    public ClientApiSafeRemote sluice() {
        return clientApiSafeRemote;
    }

    /**
     * Attempt to deliver one queued message to the downstream client.
     *
     * @return {@code true} if the message was delivered and the slot can be
     * released; {@code false} if the client is not currently reachable.
     */
    public boolean sendMessageToDownstream(MessageSlot slot) {
        ClientApiSafeRemote remote = clientApiSafeRemote;
        if (remote == null) {
            return false;
        }
        if (!isReceiveWindowOpen()) {
            return false;
        }
        try {
            remote.sendMessages(
                    new Message[]{
                            new Message(slot.from, slot.data)
                    }
            );
            return true;
        } catch (RuntimeException error) {
            Log.warn(
                    "ProxyClientDescriptor: downstream send failed",
                    error,
                    "uid",
                    uid
            );
            return false;
        }
    }

    public CloudConfig proxyCloudConfig() {
        return new CloudConfig(
                uid,
                service.proxyCloudVersion(),
                new Cloud(new short[]{service.proxySid()})
        );
    }

    public io.aether.api.common.Key masterKeyAsKey() {
        return CryptoUtils.of(masterKey);
    }
}