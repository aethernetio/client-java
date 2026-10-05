
package io.aether.cloud.client;

import io.aether.api.clientserverapi.LoginApiRemote;
import io.aether.api.clientserverapi.LoginStream;
import io.aether.api.common.ServerDescriptor;
import io.aether.logger.Log;
import io.aether.peer.PeerConnectionManager;
import io.aether.peer.PeerStreamHandler;
import io.aether.peer.PeerUapRegistry;
import io.aether.utils.interfaces.AFunction;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Application-level relay that lets a peer without cloud access
 * (e.g. Alice on LoRa only) use a well-connected peer (Bob) as its
 * cloud entry point.
 *
 * <p>Alice encrypts a login-stream payload with her own cloud keys and
 * sends it to Bob via {@code toServer(aliceUid, peerBackId, encrypted)}.
 * Bob resolves Alice's cloud by her uid, opens (or reuses) a
 * {@link ConnectionWork} to one server in that cloud, and calls
 * {@code loginByUIDMulti(aliceUid, bobBackId, LoginStream.Out.send(encrypted))}.
 * The server uses Alice's cloud keys to process the stream and returns
 * replies tagged with {@code bobBackId}. Bob maps {@code bobBackId} back to
 * the originating peer (Alice) and forwards the raw reply bytes via
 * {@code fromServer(peerBackId, rawReply)}. Bob never decrypts Alice's
 * payloads: he is a byte-for-byte relay.
 *
 * <p>Bob also tracks each peer's announced receive window through
 * {@code onSetReceiveWindow}. {@link #isAwake(UUID)} lets the application
 * defer outbound peer traffic until the sleeping peer is reachable again.
 * All timing is relative to Bob's own clock, so no peer clocks have to be
 * synchronised.
 */
public final class PeerServerRelay implements AutoCloseable {

    public interface Listener {
        void onPeerMessage(UUID remoteUid, byte[] data);

        void onPeerFromServer(UUID remoteUid, short peerBackId, byte[] data);

        void onRelayError(UUID remoteUid, short peerBackId, Throwable error);
    }

    private record RelayEntry(UUID peerUid, short peerBackId) {
    }

    private final AetherCloudClient client;
    private final PeerUapRegistry uapRegistry;
    private final AtomicInteger nextBackId = new AtomicInteger(1);
    private final ConcurrentHashMap<Short, RelayEntry> backIdToPeer =
            new ConcurrentHashMap<>();

    private volatile PeerConnectionManager manager;
    private volatile Listener listener = NOOP_LISTENER;

    public PeerServerRelay(AetherCloudClient client) {
        if (client == null) {
            throw new IllegalArgumentException("client is null");
        }
        this.client = client;
        this.uapRegistry = new PeerUapRegistry(System::currentTimeMillis);
    }

    public void bindManager(PeerConnectionManager manager) {
        this.manager = manager;
    }

    public void setListener(Listener listener) {
        this.listener = (listener == null) ? NOOP_LISTENER : listener;
    }

    public PeerUapRegistry uapRegistry() {
        return uapRegistry;
    }

    public boolean isAwake(UUID remoteUid) {
        return uapRegistry.isAwake(remoteUid);
    }

    public AFunction<UUID, PeerStreamHandler> handlerFactory() {
        return this::newHandlerFor;
    }

    /**
     * Variant of {@link #handlerFactory()} where direct peer-to-peer
     * messages are delivered to the given sink instead of going through
     * the relay {@link Listener}. Use this in production: direct messages
     * are application traffic, not relay traffic, and must reach the
     * ordinary {@link MessageNode} ingress just like a cloud-delivered
     * message would. The relay remains responsible for
     * {@code toServer} / {@code fromServer} / {@code setReceiveWindow}.
     *
     * @param messageSink called with {@code (remoteUid, data)} for every
     *                    direct peer message; not called with nulls
     */
    public AFunction<UUID, PeerStreamHandler> handlerFactory(
            io.aether.utils.interfaces.ABiConsumer<UUID, byte[]> messageSink
    ) {
        if (messageSink == null) {
            throw new IllegalArgumentException("messageSink is null");
        }
        return remoteUid -> newHandlerFor(remoteUid, messageSink);
    }

    private PeerStreamHandler newHandlerFor(
            UUID remoteUid,
            io.aether.utils.interfaces.ABiConsumer<UUID, byte[]> messageSink
    ) {
        return new PeerStreamHandler() {
            @Override
            public void onMessage(byte[] data) {
                messageSink.accept(remoteUid, data);
            }

            @Override
            public void onToServer(UUID forUid, short peerBackId, byte[] data) {
                relayToServer(remoteUid, forUid, peerBackId, data);
            }

            @Override
            public void onFromServer(short peerBackId, byte[] data) {
                listener.onPeerFromServer(remoteUid, peerBackId, data);
            }

            @Override
            public void onSetReceiveWindow(long startsInMs, long durationMs) {
                uapRegistry.onSetReceiveWindow(remoteUid, startsInMs, durationMs);
            }
        };
    }


    private PeerStreamHandler newHandlerFor(UUID remoteUid) {
        return new PeerStreamHandler() {
            @Override
            public void onMessage(byte[] data) {
                listener.onPeerMessage(remoteUid, data);
            }

            @Override
            public void onToServer(UUID forUid, short peerBackId, byte[] data) {
                relayToServer(remoteUid, forUid, peerBackId, data);
            }

            @Override
            public void onFromServer(short peerBackId, byte[] data) {
                listener.onPeerFromServer(remoteUid, peerBackId, data);
            }

            @Override
            public void onSetReceiveWindow(long startsInMs, long durationMs) {
                uapRegistry.onSetReceiveWindow(remoteUid, startsInMs, durationMs);
            }
        };
    }

    private void relayToServer(UUID peerUid, UUID forUid, short peerBackId, byte[] data) {
        short bobBackId = allocateBackId();
        backIdToPeer.put(bobBackId, new RelayEntry(peerUid, peerBackId));
        try {
            client.getServerDescriptorForUid(
                    forUid,
                    sd -> doRelay(sd, forUid, bobBackId, peerBackId, peerUid, data));
        } catch (RuntimeException e) {
            backIdToPeer.remove(bobBackId);
            listener.onRelayError(peerUid, peerBackId, e);
        }
    }

    private void doRelay(
            ServerDescriptor sd,
            UUID forUid,
            short bobBackId,
            short peerBackId,
            UUID peerUid,
            byte[] data
    ) {
        try {
            ConnectionWork conn = client.getConnection(sd);
            LoginApiRemote api = conn.getRootApi();
            LoginStream stream = LoginStream.Out.send(data);
            api.loginByUIDMulti(forUid, bobBackId, stream);
        } catch (RuntimeException e) {
            backIdToPeer.remove(bobBackId);
            listener.onRelayError(peerUid, peerBackId, e);
        }
    }

    public void onBackIdData(short bobBackId, byte[] rawData) {
        RelayEntry entry = backIdToPeer.get(bobBackId);
        if (entry == null) {
            Log.warn("PeerServerRelay: unknown backId", "backId", bobBackId);
            return;
        }
        PeerConnectionManager m = manager;
        if (m == null) {
            return;
        }
        m.sendFromServer(entry.peerUid(), entry.peerBackId(), rawData);
    }

    private short allocateBackId() {
        while (true) {
            int v = nextBackId.getAndIncrement();
            if (v <= 0 || v > Short.MAX_VALUE) {
                nextBackId.compareAndSet(v, 1);
                continue;
            }
            short s = (short) v;
            if (!backIdToPeer.containsKey(s)) {
                return s;
            }
        }
    }

    @Override
    public void close() {
        backIdToPeer.clear();
    }

    private static final Listener NOOP_LISTENER = new Listener() {
        @Override
        public void onPeerMessage(UUID remoteUid, byte[] data) {
        }

        @Override
        public void onPeerFromServer(UUID remoteUid, short peerBackId, byte[] data) {
        }

        @Override
        public void onRelayError(UUID remoteUid, short peerBackId, Throwable error) {
        }
    };
}