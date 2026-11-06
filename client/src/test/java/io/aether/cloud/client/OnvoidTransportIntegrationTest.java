
package io.aether.cloud.client;

import io.aether.api.clientserverapi.IceCandidate;
import io.aether.api.clientserverapi.WebRtcSession;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REAL end-to-end WebRTC test using the actual onvoid JNI backend.
 *
 * <p>Two {@link MessageNodeWebRtcPeer} instances negotiate a real
 * offer/answer/ICE exchange in-process and push bytes through a real
 * {@code RTCDataChannel}. This test exists specifically to prove that the
 * production JNI path (native library loaded, data channel opens, bytes
 * arrive) works — the fake-transport test alone does not.
 */
@Timeout(60)
class OnvoidTransportIntegrationTest {

    private static final UUID UID_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID UID_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    /** Shared in-memory signaling: uid → the session that peer published LAST. */
    private final Map<UUID, WebRtcSession> published =
            new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        MessageNodeWebRtcPeer.peerProvider = OnvoidTransport.INSTANCE;
        published.clear();
    }

    @AfterEach
    void tearDown() {
        MessageNodeWebRtcPeer.peerProvider = null;
        published.clear();
    }

    @Test
    void realDataChannelDeliversBytesBetweenTwoPeers() throws Exception {
        assertTrue(MessageNodeWebRtcPeer.isSupported(),
                "onvoid webrtc-java must be on the classpath");
        // This test relies on host (localhost) ICE candidates only, no STUN.
        System.setProperty("webrtc.noStun", "1");

        CountDownLatch openA = new CountDownLatch(1);
        CountDownLatch openB = new CountDownLatch(1);
        CountDownLatch receivedByB = new CountDownLatch(1);
        CountDownLatch receivedByA = new CountDownLatch(1);

        List<byte[]> bytesAtB = new ArrayList<>();
        List<byte[]> bytesAtA = new ArrayList<>();

        MessageNodeWebRtcPeer.WebRtcSignaling signA = new Signaling(UID_A);
        MessageNodeWebRtcPeer.WebRtcSignaling signB = new Signaling(UID_B);

        MessageNodeWebRtcPeer peerA = new MessageNodeWebRtcPeer(
                UID_A, UID_B, signA,
                data -> { bytesAtA.add(data); receivedByA.countDown(); },
                openA::countDown);
        MessageNodeWebRtcPeer peerB = new MessageNodeWebRtcPeer(
                UID_B, UID_A, signB,
                data -> { bytesAtB.add(data); receivedByB.countDown(); },
                openB::countDown);

        peerA.start();
        peerB.start();



        assertTrue(openA.await(30, TimeUnit.SECONDS),
                "peer A data channel did not open within 30s (SDP/ICE/reconnect loop failed)");
        assertTrue(openB.await(30, TimeUnit.SECONDS),
                "peer B data channel did not open within 30s");

        String offer = published.get(UID_A) == null ? "" : new String(
                published.get(UID_A).getSdp(), StandardCharsets.UTF_8);
        assertTrue(offer.contains("a=ice-ufrag:"), "offer SDP must contain ice-ufrag (Chrome/W3C compat)");
        assertTrue(offer.contains("a=fingerprint:"), "offer SDP must contain dtls fingerprint (Chrome/W3C compat)");
        assertTrue(offer.contains("m=application"), "offer SDP must contain m=application (SCTP data channel)");
        System.out.println("OFFER_SDP_HEAD=" + offer.substring(0, Math.min(200, offer.length())).replace("\r\n", "|"));


        byte[] payload = "hello-over-real-webrtc".getBytes(StandardCharsets.UTF_8);
        assertTrue(peerA.send(payload), "peer A send() returned false — channel closed");

        assertTrue(receivedByB.await(15, TimeUnit.SECONDS),
                "peer B never received the payload over the real data channel");
        assertArrayEquals(payload, bytesAtB.get(0),
                "bytes delivered to peer B are corrupted");

        byte[] reply = "reply-over-real-webrtc".getBytes(StandardCharsets.UTF_8);
        assertTrue(peerB.send(reply), "peer B send() returned false — channel closed");

        assertTrue(receivedByA.await(15, TimeUnit.SECONDS),
                "peer A never received the reply over the real data channel");
        assertArrayEquals(reply, bytesAtA.get(0),
                "bytes delivered to peer A are corrupted");

        peerA.destroy(true).to(() -> {});
        peerB.destroy(true).to(() -> {});
    }

    /** In-memory signaling bound to one uid; hands back sessions other peer published. */
    private final class Signaling implements MessageNodeWebRtcPeer.WebRtcSignaling {
        private final UUID uid;
        Signaling(UUID uid) { this.uid = uid; }

        @Override
        public AFuture publish(WebRtcSession session) {
            published.put(uid, session);
            return AFuture.completed();
        }

        @Override
        public ARFuture<WebRtcSession> request(UUID remote) {
            WebRtcSession s = published.get(remote);
            if (s == null) {
                ARFuture<WebRtcSession> err = ARFuture.make();
                err.error(new IllegalStateException("no session for " + remote));
                return err;
            }
            return ARFuture.of(s);
        }
    }
}