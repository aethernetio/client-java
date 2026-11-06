
package io.aether.cloud.client;

import io.aether.api.clientserverapi.IceCandidate;
import io.aether.api.clientserverapi.WebRtcSession;
import io.aether.api.common.Cloud;
import io.aether.api.common.ServerDescriptor;
import io.aether.utils.Destroyer;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Timeout(20)
class MessageNodeWebRtcPeerTest {

    private static final UUID UID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private final List<FakePeer> createdPeers = Collections.synchronizedList(new ArrayList<>());

    @BeforeEach
    void setUp() {
        createdPeers.clear();
        MessageNodeWebRtcPeer.peerProvider = listener -> {
            FakePeer peer = new FakePeer(listener);
            createdPeers.add(peer);
            return peer;
        };
    }

    @AfterEach
    void tearDown() {
        MessageNodeWebRtcPeer.peerProvider = null;
    }

    // ---------------------------------------------------------------
    // Test 1: initiator publishes offer, accepts answer, exchanges bytes
    // ---------------------------------------------------------------
    @Test
    void initiatorPublishesOfferAndAcceptsAnswer() throws Exception {
        List<WebRtcSession> published = Collections.synchronizedList(new ArrayList<>());
        List<byte[]> received = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch openLatch = new CountDownLatch(1);

        MessageNodeWebRtcPeer.WebRtcSignaling signaling = new MessageNodeWebRtcPeer.WebRtcSignaling() {
            @Override
            public AFuture publish(WebRtcSession session) {
                published.add(session);
                return AFuture.completed();
            }

            @Override
            public ARFuture<WebRtcSession> request(UUID uid) {
                String sessionId = extractSessionId(published.get(0));
                return ARFuture.of(sessionWith("answer", sessionId));
            }
        };

        MessageNodeWebRtcPeer peer = new MessageNodeWebRtcPeer(
                UID_1, UID_2, signaling, received::add, openLatch::countDown);

        assertTrue(peer.initiator, "uid1 < uid2 must make the peer an initiator");
        peer.start();

        assertEquals(1, createdPeers.size(), "exactly one peer connection must be created");
        FakePeer fakePeer = createdPeers.get(0);

        assertEquals(1, published.size(), "the offer must be published synchronously");
        String offerSdp = new String(published.get(0).getSdp(), StandardCharsets.UTF_8);
        assertTrue(offerSdp.contains("a=setup:actpass"),
                "offer SDP must carry a=setup:actpass: " + offerSdp);

        assertNotNull(fakePeer.remote, "the answer must be applied as the remote description");
        assertEquals("answer", fakePeer.remote.type());

        // Open the channel: onOpen fires, isOpen() becomes true.
        fakePeer.channel.openChannel();
        assertTrue(openLatch.await(2, TimeUnit.SECONDS), "channel open must be signalled");
        assertTrue(peer.isOpen());

        // Outgoing bytes.
        assertTrue(peer.send(new byte[]{1, 2, 3}));
        assertEquals(1, fakePeer.channel.sent.size());
        assertArrayEquals(new byte[]{1, 2, 3}, fakePeer.channel.sent.get(0));

        // Incoming bytes.
        fakePeer.channel.receive(new byte[]{4, 5});
        assertEquals(1, received.size());
        assertArrayEquals(new byte[]{4, 5}, received.get(0));

        peer.destroy(true);
        assertFalse(peer.isOpen(), "a destroyed peer must not report an open channel");
    }

    // ---------------------------------------------------------------
    // Test 2: answerer accepts offer and publishes answer
    // ---------------------------------------------------------------
    @Test
    void answererAcceptsOfferAndPublishesAnswer() {
        List<WebRtcSession> published = Collections.synchronizedList(new ArrayList<>());

        MessageNodeWebRtcPeer.WebRtcSignaling signaling = new MessageNodeWebRtcPeer.WebRtcSignaling() {
            @Override
            public AFuture publish(WebRtcSession session) {
                published.add(session);
                return AFuture.completed();
            }

            @Override
            public ARFuture<WebRtcSession> request(UUID uid) {
                return ARFuture.of(sessionWith("offer", "test-session"));
            }
        };

        MessageNodeWebRtcPeer peer = new MessageNodeWebRtcPeer(
                UID_2, UID_1, signaling, data -> { }, () -> { });

        assertFalse(peer.initiator, "uid2 > uid1 must make the peer an answerer");
        peer.start();

        assertEquals(1, createdPeers.size());
        FakePeer fakePeer = createdPeers.get(0);

        assertNotNull(fakePeer.remote, "the offer must be applied as the remote description");
        assertEquals("offer", fakePeer.remote.type());

        assertEquals(1, published.size(), "the answer must be published");
        String answerSdp = new String(published.get(0).getSdp(), StandardCharsets.UTF_8);
        assertTrue(answerSdp.contains("a=setup:active"),
                "answer SDP must carry a=setup:active: " + answerSdp);

        IceCandidate[] candidates = published.get(0).getCandidates();
        assertNotNull(candidates);
        assertTrue(candidates.length >= 1, "the published answer must carry candidates");
        String first = new String(candidates[0].getData(), StandardCharsets.UTF_8);
        assertEquals("test-session", extractSessionId(published.get(0)),
                "the first candidate must carry the negotiated session id: " + first);

        peer.destroy(true);
    }

    // ---------------------------------------------------------------
    // Test 3: refuses an answer that belongs to an older negotiation
    // ---------------------------------------------------------------
    @Test
    void rejectsAnswerFromOlderNegotiation() {
        MessageNodeWebRtcPeer.WebRtcSignaling signaling = new MessageNodeWebRtcPeer.WebRtcSignaling() {
            @Override
            public AFuture publish(WebRtcSession session) {
                return AFuture.completed();
            }

            @Override
            public ARFuture<WebRtcSession> request(UUID uid) {
                return ARFuture.of(sessionWith("answer", "old-session"));
            }
        };

        MessageNodeWebRtcPeer peer = new MessageNodeWebRtcPeer(
                UID_1, UID_2, signaling, data -> { }, () -> { });

        WebRtcSession oldSession = sessionWith("answer", "old-session");
        WebRtcSession newSession = sessionWith("answer", "new-session");

        assertFalse(peer.matchesDescriptionType(oldSession, "answer", "new-session"),
                "an answer from another negotiation must be rejected");
        assertTrue(peer.matchesDescriptionType(newSession, "answer", "new-session"),
                "the matching answer must be accepted");
        assertTrue(peer.matchesDescriptionType(oldSession, "answer", null),
                "without an expected id any valid answer is accepted");

        // Type matching is based on the SDP setup attribute.
        WebRtcSession offerSession = sessionWith("offer", "some-id");
        assertTrue(peer.matchesDescriptionType(offerSession, "offer", null));
        assertFalse(peer.matchesDescriptionType(offerSession, "answer", null),
                "an offer must not be accepted as an answer");

        peer.destroy(true);
    }

    // ---------------------------------------------------------------
    // Test 4: MessageNode prefers the direct channel and keeps the cloud fallback
    // ---------------------------------------------------------------
    @Test
    void messageNodePrefersDirectChannelAndKeepsCloudFallback() throws Exception {
        AetherCloudClient client = mock(AetherCloudClient.class);

        Destroyer destroyed = new Destroyer("test");
        destroyed.destroy(false);
        setField(client, "destroyer", destroyed);
        setField(client, "startFuture", AFuture.completed());

        UUID consumerUid = UID_2;
        ClientCloud cloud = new ClientCloud(consumerUid, new Cloud(new short[]{1}));

        when(client.getCloud(eq(consumerUid))).thenReturn(ARFuture.of(cloud));
        when(client.getUid()).thenReturn(UID_1);

        MessageEventListener strategy = new MessageEventListener() {
            @Override
            public void setConsumerCloud(MessageNode messageNode, ClientCloud cloud) { }

            @Override
            public void onResolveConsumerServer(MessageNode messageNode, ServerDescriptor sd) { }

            @Override
            public void onResolveConsumerConnection(MessageNode messageNode, ConnectionWork cw) { }
        };

        MessageNode node = new MessageNode(client, consumerUid, strategy);

        MessageNodeWebRtcPeer mockPeer = mock(MessageNodeWebRtcPeer.class);
        when(mockPeer.isOpen()).thenReturn(true);
        when(mockPeer.send(any(byte[].class))).thenReturn(true);

        setField(node, "webRtcStarted", true);
        setField(node, "webRtcPeer", mockPeer);

        // 1) Direct channel is available: the message goes through it, nothing is queued.
        AFuture directFuture = node.send(new byte[]{7});
        assertTrue(directFuture.isDone(), "direct channel send must complete immediately");
        assertTrue(node.bufferOut.isEmpty(), "direct send must not enqueue anything");
        verify(mockPeer).send(eq(new byte[]{7}));

        // 2) Direct channel reports failure: the message falls back to the cloud queue.
        when(mockPeer.send(any(byte[].class))).thenReturn(false);
        AFuture fallbackFuture = node.send(new byte[]{8});
        assertFalse(fallbackFuture.isDone(), "the fallback future must stay pending");
        assertEquals(1, node.bufferOut.size(), "the fallback must enqueue the message");

        // 3) The channel recovers: flushing must drain the queue and complete the future.
        when(mockPeer.send(any(byte[].class))).thenReturn(true);
        Method flush = MessageNode.class.getDeclaredMethod("flushBufferedToWebRtc");
        flush.setAccessible(true);
        flush.invoke(node);

        assertTrue(fallbackFuture.isDone(), "flushing must complete buffered futures");
        assertTrue(node.bufferOut.isEmpty(), "flushing must drain the buffer");
    }

    // ---------------------------------------------------------------
    // Fakes
    // ---------------------------------------------------------------

    private static final class FakeSdp implements MessageNodeWebRtcPeer.Sdp {
        private final String type;
        private final String value;

        FakeSdp(String type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public String value() {
            return value;
        }
    }

    private static final class FakeChannel implements MessageNodeWebRtcPeer.Channel {
        final List<byte[]> sent = Collections.synchronizedList(new ArrayList<>());
        volatile MessageNodeWebRtcPeer.ChannelListener listener;
        volatile boolean open;
        volatile boolean closed;

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void send(byte[] data) {
            if (!open) throw new IllegalStateException("data channel is not open");
            sent.add(data.clone());
        }

        @Override
        public void close() {
            open = false;
            closed = true;
        }

        @Override
        public void setListener(MessageNodeWebRtcPeer.ChannelListener listener) {
            this.listener = listener;
        }

        void openChannel() {
            open = true;
            MessageNodeWebRtcPeer.ChannelListener l = listener;
            if (l != null) l.onOpen();
        }

        void receive(byte[] data) {
            MessageNodeWebRtcPeer.ChannelListener l = listener;
            if (l != null) l.onMessage(data.clone());
        }
    }

    private static final class FakePeer implements MessageNodeWebRtcPeer.Peer {
        final MessageNodeWebRtcPeer.PeerListener listener;
        final FakeChannel channel = new FakeChannel();
        final List<MessageNodeWebRtcPeer.IceConfig> remoteCandidates =
                Collections.synchronizedList(new ArrayList<>());

        volatile MessageNodeWebRtcPeer.Sdp local;
        volatile MessageNodeWebRtcPeer.Sdp remote;
        volatile boolean gatheringComplete = true;
        volatile boolean closed;

        FakePeer(MessageNodeWebRtcPeer.PeerListener listener) {
            this.listener = listener;
        }

        @Override
        public void createOffer(MessageNodeWebRtcPeer.SdpCallback callback) {
            callback.onSuccess(new FakeSdp("offer", "v=0\r\na=setup:actpass\r\n"));
        }

        @Override
        public void createAnswer(MessageNodeWebRtcPeer.SdpCallback callback) {
            callback.onSuccess(new FakeSdp("answer", "v=0\r\na=setup:active\r\n"));
        }

        @Override
        public void setLocal(MessageNodeWebRtcPeer.Sdp description, MessageNodeWebRtcPeer.DoneCallback callback) {
            this.local = description;
            callback.onSuccess();
        }

        @Override
        public void setRemote(MessageNodeWebRtcPeer.Sdp description, MessageNodeWebRtcPeer.DoneCallback callback) {
            this.remote = description;
            callback.onSuccess();
        }

        @Override
        public void addIceCandidate(MessageNodeWebRtcPeer.IceConfig candidate) {
            remoteCandidates.add(candidate);
        }

        @Override
        public MessageNodeWebRtcPeer.Sdp getLocal() {
            return local;
        }

        @Override
        public boolean gatheringComplete() {
            return gatheringComplete;
        }

        @Override
        public MessageNodeWebRtcPeer.Channel createDataChannel(String label) {
            return channel;
        }

        @Override
        public void close() {
            closed = true;
            channel.close();
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static WebRtcSession sessionWith(String type, String sessionId) {
        String sdpText = type.equals("offer")
                ? "v=0\r\na=setup:actpass\r\n"
                : "v=0\r\na=setup:active\r\n";
        IceCandidate meta = new IceCandidate(
                ("{\"aetherSessionId\":\"" + sessionId + "\"}").getBytes(StandardCharsets.UTF_8));
        return new WebRtcSession(sdpText.getBytes(StandardCharsets.UTF_8), new IceCandidate[]{meta});
    }

    private static String extractSessionId(WebRtcSession session) {
        String key = "\"aetherSessionId\":\"";
        for (IceCandidate candidate : session.getCandidates()) {
            String json = new String(candidate.getData(), StandardCharsets.UTF_8);
            int start = json.indexOf(key);
            if (start < 0) continue;
            start += key.length();
            int end = json.indexOf('"', start);
            if (end > start) return json.substring(start, end);
        }
        return null;
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = findField(target.getClass(), name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
