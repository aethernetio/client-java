package io.aether.cloud.client;

import io.aether.api.common.ServerDescriptor;
import io.aether.logger.Log;
import io.aether.net.fastMeta.AutoFlushContext;
import io.aether.net.fastMeta.FastMetaApi;
import io.aether.net.fastMeta.MetaContext;
import io.aether.utils.AString;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.ToString;
import io.aether.utils.dataio.DataInOutStatic;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.AConsumer;
import io.aether.utils.interfaces.AFunction;
import io.aether.utils.slots.EventConsumerWithQueue;
import io.aether.utils.tuples.Tuple;
import io.aether.utils.tuples.Tuple2;

import java.util.Deque;
import java.util.Set;

import java.util.ArrayList;
import java.util.List;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MessageNode implements ToString {
    private static final int MAX_BUFFER_SIZE = 1000;
    public final EventConsumerWithQueue<byte[]> bufferIn = new EventConsumerWithQueue<>();
    final UUID consumer;
    final ARFuture<ClientCloud> consumerCloud;
    final Set<ConnectionWork> connectionsOut = new ConcurrentHashSet<>();
    final Deque<Tuple2<byte[], AFuture>> bufferOut = new ConcurrentLinkedDeque<>();
    private final AetherCloudClient client;


    /**
     * Grace window (milliseconds) after a fresh LAN probe during which the
     * message stays queued instead of falling back to the cloud path. This
     * gives the LAN handshake a chance to complete on the first contact
     * with a peer. Subsequent messages for the same uid do not re-enter
     * this grace window — the probe keeps running in the background, but
     * messages are queued for cloud delivery immediately.
     */
    private static final long PEER_PROBE_GRACE_MS = 3000L;

    /**
     * How often to re-trigger a LAN probe for a uid that has not yet been
     * reached via the peer link. When this interval elapses, the next
     * outbound message re-arms the grace window so the peer gets another
     * chance to be discovered.
     */
    private static final long PEER_PROBE_RECHECK_MS = 10_000L;

    private volatile long peerProbeGraceUntilMs = 0L;
    private volatile long lastPeerProbeAtMs = 0L;


    private volatile MessageEventListener strategy;

    public MessageNode(AetherCloudClient client, UUID consumer, MessageEventListener strategy) {
        Log.trace("open message node ($client) from $uidFrom to $uidTo", "client", client.getName(), "uidTo", consumer, "uidFrom", client.getUid());
        this.client = client;
        this.strategy = strategy;
        this.consumer = consumer;
        consumerCloud = client.getCloud(consumer);
        consumerCloud.to(c -> {
            this.strategy.setConsumerCloud(this, c);
        });
    }

    @Override
    public AString toAString(AString sb) {
        sb.add("MessageNode(").add(consumer).add(")");
        return sb;
    }

    @Override
    public String toString() {
        return toString2();
    }

    public MessageEventListener getStrategy() {
        return strategy;
    }

    public void setStrategy(MessageEventListener strategy) {
        this.strategy = strategy;
    }



    public AFuture send(byte[] msg) {

        Log.info("MessageNode.send: entry",
                "from", client.getUid(), "to", consumer,
                "bytes", msg.length);

        if (msg == null || msg.length == 0) return AFuture.completed();

        AFuture peerAttempt = trySendViaPeer(msg);

        Log.info("MessageNode.send: trySendViaPeer result",
                "to", consumer, "peerAttempt", peerAttempt == null ? "null" : "present");

        if (peerAttempt != null) {
            return peerAttempt;
        }


        long now = System.currentTimeMillis();
        long sinceLastProbeMs = now - lastPeerProbeAtMs;
        if (sinceLastProbeMs >= PEER_PROBE_RECHECK_MS) {
            // Fresh interest in this uid (first message, or recheck
            // interval elapsed): arm the grace window so this message is
            // held while LAN discovery + handshake have a chance to
            // complete.



            Log.info("MessageNode.send: arming peer probe",
                    "to", consumer, "sinceLastProbeMs", sinceLastProbeMs);

            client.probeLanFor(consumer);
            lastPeerProbeAtMs = now;
            // Arm the grace window: hold this message while LAN discovery
            // (including the async LAN stack bootstrap and the LAN
            // handshake) completes. If nothing is found within the window,
            // ConnectionWork falls back to the ordinary cloud path.
            peerProbeGraceUntilMs = now + PEER_PROBE_GRACE_MS;


        }
        // Otherwise: a probe is already in flight (or recently completed
        // without success). Do not hold this message — it goes to the
        // cloud queue immediately; the background peer link, if it comes
        // up before flush, will still be preferred by tryFlushAllViaPeer.


        if (bufferOut.size() >= 50) {
            Tuple2<byte[], AFuture> oldest = bufferOut.pollFirst();
            if (oldest != null) {
                Log.warn("MessageNode buffer pressure, dropping oldest message", "uidTo", consumer);
                oldest.val2().error(new RuntimeException("Outgoing message queue overflow"));
            }
        }
        AFuture f = AFuture.make();
        if (bufferOut.size() < MAX_BUFFER_SIZE) {
            bufferOut.addLast(Tuple.of(msg, f));
        } else {
            f.error(new RuntimeException("Critical buffer overflow"));
        }
        return f;
    }

    /**
     * @return true while a LAN probe initiated by {@link #send} is still
     *         within its grace window — the caller should keep the message
     *         queued and retry {@link #tryFlushAllViaPeer} on each tick.
     */
    public boolean isPeerProbeGraceActive() {
        return System.currentTimeMillis() < peerProbeGraceUntilMs;
    }

    /**
     * Attempt to flush every queued message via the peer link in one pass.
     *
     * <p>Called from {@link ConnectionWork#flushBackgroundRequests} while a
     * peer-probe grace window is open. If the peer link has become
     * established since the last tick, this delivers all pending messages
     * directly through {@link ConnectionPeer#sendMessage} and
     * completes their futures. If the link is still not available, the
     * messages stay queued and this method returns {@code false} so the
     * caller keeps waiting.
     *
     * @return {@code true} if every queued message was handed off through
     *         the peer link; {@code false} if the peer link is not ready
     */
    public boolean tryFlushAllViaPeer() {
        if (bufferOut.isEmpty()) {
            return true;
        }


        ConnectionPeer manager = client.getConnectionPeer();
        if (manager == null || !manager.isConnected(consumer)) {
            return false;
        }


        List<Tuple2<byte[], AFuture>> snapshot = new ArrayList<>(bufferOut);
        for (Tuple2<byte[], AFuture> pending : snapshot) {
            if (!bufferOut.remove(pending)) {
                continue;
            }
            try {
                if (manager.sendMessage(consumer, pending.val1())) {
                    pending.val2().tryDone();
                } else {
                    bufferOut.addLast(pending);
                    return false;
                }
            } catch (RuntimeException e) {
                Log.warn("Peer-first grace flush failed, keeping message queued", e);
                bufferOut.addLast(pending);
                return false;
            }
        }
        return true;
    }


    /**
     * Peer-first attempt: if a direct LAN/peer link to the consumer is
     * currently established, hand the payload off to it instead of
     * enqueueing for cloud delivery. Returns {@code null} when the peer
     * route is not currently usable, so the caller falls back to the
     * ordinary cloud path.
     *
     * <p>The returned future mirrors the cloud path's contract: it is
     * completed when the payload has been accepted by a transport (peer
     * channel here), not when the recipient has actually received it.
     */

    private AFuture trySendViaPeer(byte[] msg) {
        AetherCloudClient owner = client;

        Log.info("MessageNode.trySendViaPeer: entry",
                "from", client.getUid(), "to", consumer,
                "owner", owner == null ? "null" : "present");

        if (owner == null) return null;

        ConnectionPeer manager = owner.getConnectionPeer();
        if (manager == null) return null;
        if (!manager.isConnected(consumer)) return null;

        Log.info("MessageNode.trySendViaPeer: connectionPeer state",
                "to", consumer,
                "manager", manager == null ? "null" : "present",
                "connected", manager != null && manager.isConnected(consumer));

        try {
            if (!manager.sendMessage(consumer, msg)) {
                return null;
            }
        } catch (RuntimeException e) {
            Log.warn("Peer-first send failed, falling back to cloud", e);
            return null;
        }



        AFuture f = AFuture.make();
        f.done();
        return f;
    }



    public UUID getConsumerUUID() {
        return consumer;
    }

    public void addConsumerServerOut(int id) {
        client.getServer(id).to(s -> strategy.onResolveConsumerServer(this, s));
    }

    public void addConsumerServerOut(ServerDescriptor serverDescriptor) {
        strategy.onResolveConsumerConnection(this, client.getConnection(serverDescriptor));
    }

    public void addConsumerConnectionOut(ConnectionWork connectionWork) {
        if (connectionsOut.add(connectionWork)) {
            Log.debug("add connection out for messages uid=$uid", "uid", consumer);
        }
    }

    public void sendMessageFromServerToClient(byte[] data) {
        Log.trace("sendMessageFromServerToClient");
        try {
            bufferIn.fire(data);
        } catch (Exception e) {
            Log.warn("Read message exception");
        }
    }

    public void toConsumer(AConsumer<byte[]> o) {
        if (bufferIn.hasListener()) throw new RuntimeException("Already add listener");
        bufferIn.add(o::accept);
    }


    public <LT> MetaContext toApiR(
            FastMetaApi<LT, ? extends LT> metaLt,
            AFunction<MetaContext, LT> localApiFactory) {
        AutoFlushContext ctx = new AutoFlushContext();
        ctx.localApi = localApiFactory.apply(ctx);
        ctx.onFlushData(data -> send(data));
        toApi(ctx, metaLt);
        return ctx;
    }

    public <LT> void toApi(MetaContext ctx, FastMetaApi<LT, ? extends LT> metaLt) {
        toConsumer(v -> {
            try {
                metaLt.makeLocal(ctx, new DataInOutStatic(v));
            } catch (Exception e) {
                Log.error("Read message api exception", "data", v);
            }
        });
    }


    public <LT> MetaContext toApi(FastMetaApi<LT, ? extends LT> metaLt, LT localApi) {
        AutoFlushContext ctx = new AutoFlushContext();
        ctx.localApi = localApi;
        ctx.onFlushData(this::send);
        toApi(ctx, metaLt);
        return ctx;
    }
}