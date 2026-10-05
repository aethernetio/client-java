
package io.aether.peer.lan;

import io.aether.api.CryptoUtils;
import io.aether.api.PeerIdentityCrypto;
import io.aether.api.common.CryptoLib;
import io.aether.api.common.KeySignPrivate;
import io.aether.api.common.KeySignPublic;
import io.aether.api.common.PeerIdentity;
import io.aether.api.common.PeerIdentityData;

import io.aether.api.peerlanapi.PeerLanProbe;
import io.aether.api.peerlanapi.PeerLanSearch;

import io.aether.crypto.CryptoProvider;
import io.aether.crypto.CryptoProviderFactory;
import io.aether.crypto.PairSignKeys;
import io.aether.crypto.Sign;
import io.aether.crypto.SignChecker;
import io.aether.crypto.Signer;
import io.aether.peer.PeerConnectionManager;
import io.aether.peer.PeerLocalIdentity;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.*;

class LanPeerTransportTest {

    private static final UUID ALICE_UID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOB_UID =
            UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static class TestAuthority {
        final CryptoLib lib;
        final CryptoProvider provider;
        final Signer rootSigner;
        final SignChecker trustedRoot;
        final Signer serverSigner;
        final io.aether.api.common.SignedKey serverSigned;
        final List<SignChecker> defaultRoots;

        TestAuthority(CryptoLib lib) {
            this.lib = lib;
            this.provider = CryptoProviderFactory.getProvider(lib.name());
            PairSignKeys rootPair = provider.createSignKeys();
            this.rootSigner = provider.createSigner(rootPair);
            this.trustedRoot = provider.createSigner(rootPair.getPublicKey());
            PairSignKeys serverPair = provider.createSignKeys();
            this.serverSigner = provider.createSigner(serverPair);
            this.serverSigned = CryptoUtils.of(serverPair.getPublicKey().toSignedKey(rootSigner));
            this.defaultRoots = Collections.singletonList(trustedRoot);
        }

        PeerLocalIdentity issue(UUID uid) {
            PairSignKeys peerPair = provider.createSignKeys();
            KeySignPublic peerPublic = CryptoUtils.of(peerPair.getPublicKey());
            PeerIdentityData data = new PeerIdentityData(uid, peerPublic);
            Sign sign = serverSigner.sign(PeerIdentityCrypto.signingData(data));
            PeerIdentity identity = new PeerIdentity(data, serverSigned, CryptoUtils.of(sign));
            KeySignPrivate privateDto = CryptoUtils.of(peerPair.getPrivateKey());
            return new PeerLocalIdentity(identity, privateDto, defaultRoots);
        }
    }

    private static class Receiver {
        final List<byte[]> messages = Collections.synchronizedList(new ArrayList<>());
        final List<UUID> senders = Collections.synchronizedList(new ArrayList<>());
    }

    private static Receiver receiver() {
        return new Receiver();
    }

    private static boolean waitFor(java.util.function.BooleanSupplier cond, long millis) {
        long deadline = System.nanoTime() + millis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            if (cond.getAsBoolean()) return true;
            LockSupport.parkNanos(20_000_000L);
        }
        return cond.getAsBoolean();
    }


    @Test
    void lanProbeCodecRoundTripsAndRejectsMalformedData() {
        PeerLanProbe probe = new PeerLanProbe((byte) 0x42, 54321, new CryptoLib[]{CryptoLib.SODIUM}, (byte) 0);
        byte[] encoded = LanPeerCodec.encodeProbe(probe);
        PeerLanProbe decoded = LanPeerCodec.decodeProbe(encoded);
        assertEquals((byte) 0x42, decoded.getSenderHash1());
        assertEquals(54321, decoded.getDirectPort());
        assertArrayEquals(new CryptoLib[]{CryptoLib.SODIUM}, decoded.getCryptoLibs());

        // wrong domain
        byte[] wrongDomain = encoded.clone();
        wrongDomain[0] ^= 0x01;
        assertThrows(LanPeerTransportException.class, () -> LanPeerCodec.decodeProbe(wrongDomain));

        // trailing bytes
        byte[] withTrailing = java.util.Arrays.copyOf(encoded, encoded.length + 1);
        withTrailing[encoded.length] = 0x42;
        assertThrows(LanPeerTransportException.class, () -> LanPeerCodec.decodeProbe(withTrailing));

        // port 0
        assertThrows(LanPeerTransportException.class,
                () -> LanPeerCodec.encodeProbe(new PeerLanProbe((byte) 0, 0, new CryptoLib[]{CryptoLib.SODIUM}, (byte) 0)));

        // port > 65535
        assertThrows(LanPeerTransportException.class,
                () -> LanPeerCodec.encodeProbe(new PeerLanProbe((byte) 0, 70000, new CryptoLib[]{CryptoLib.SODIUM}, (byte) 0)));

        // empty cryptoLibs
        assertThrows(LanPeerTransportException.class,
                () -> LanPeerCodec.encodeProbe(new PeerLanProbe((byte) 0, 12345, new CryptoLib[0], (byte) 0)));
    }

    @Test
    void lanSearchCodecRoundTrips() {
        UUID sender = UUID.fromString("00000000-0000-0000-0000-00000000abcd");
        PeerLanSearch search = new PeerLanSearch((byte) 0x77, new CryptoLib[]{CryptoLib.HYDROGEN}, sender);
        byte[] encoded = LanPeerCodec.encodeSearch(search);
        PeerLanSearch decoded = LanPeerCodec.decodeSearch(encoded);
        assertEquals((byte) 0x77, decoded.getTargetHash1());
        assertEquals(sender, decoded.getSenderUid());
        assertArrayEquals(new CryptoLib[]{CryptoLib.HYDROGEN}, decoded.getCryptoLibs());

        // wrong tag — probe must not decode as search
        byte[] probeBytes = LanPeerCodec.encodeProbe(
                new PeerLanProbe((byte) 1, 12345, new CryptoLib[]{CryptoLib.SODIUM}, (byte) 0));
        assertThrows(LanPeerTransportException.class, () -> LanPeerCodec.decodeSearch(probeBytes));
    }

    @Test
    void hash1IsDeterministicAndDiffersForDifferentUids() {
        byte a1 = LanPeerCodec.hash1(ALICE_UID);
        byte a2 = LanPeerCodec.hash1(ALICE_UID);
        assertEquals(a1, a2, "same uid must hash to same byte");
        byte b1 = LanPeerCodec.hash1(BOB_UID);
        // 1-byte hash; collision possible but these two fixed test uids don't collide
        assertNotEquals(a1, b1);
    }


    @Test
    void realUdpPeerConnectionWorksForEveryCryptoLib() throws Exception {
        for (CryptoLib lib : CryptoLib.values()) {
            TestAuthority authority = new TestAuthority(lib);
            PeerLocalIdentity alice = authority.issue(ALICE_UID);
            PeerLocalIdentity bob = authority.issue(BOB_UID);

            Receiver aliceRecv = receiver();
            Receiver bobRecv = receiver();

            PeerConnectionManager aliceMgr = new PeerConnectionManager(
                    alice, (uid, data) -> { aliceRecv.senders.add(uid); aliceRecv.messages.add(data); });
            PeerConnectionManager bobMgr = new PeerConnectionManager(
                    bob, (uid, data) -> { bobRecv.senders.add(uid); bobRecv.messages.add(data); });

            LanPeerTransport aliceTransport = new LanPeerTransport(aliceMgr);
            LanPeerTransport bobTransport = new LanPeerTransport(bobMgr);
            try {
                aliceTransport.start();
                bobTransport.start();

                InetAddress loopback = InetAddress.getLoopbackAddress();
                aliceTransport.observePeer(BOB_UID,
                        new InetSocketAddress(loopback, bobTransport.getDirectPort()));
                bobTransport.observePeer(ALICE_UID,
                        new InetSocketAddress(loopback, aliceTransport.getDirectPort()));

                assertTrue(waitFor(() -> aliceMgr.isConnected(BOB_UID) && bobMgr.isConnected(ALICE_UID), 5000),
                        "handshake failed for " + lib);

                byte[] payload = ("hello-" + lib).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(aliceMgr.sendMessage(BOB_UID, payload));
                assertTrue(waitFor(() -> bobRecv.messages.size() >= 1, 3000));
                assertArrayEquals(payload, bobRecv.messages.get(0));
                assertEquals(ALICE_UID, bobRecv.senders.get(0));

                byte[] reply = ("reply-" + lib).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(bobMgr.sendMessage(ALICE_UID, reply));
                assertTrue(waitFor(() -> aliceRecv.messages.size() >= 1, 3000));
                assertArrayEquals(reply, aliceRecv.messages.get(0));
                assertEquals(BOB_UID, aliceRecv.senders.get(0));
            } finally {
                aliceTransport.close();
                bobTransport.close();
            }
        }
    }

    @Test
    void inboundHelloBootstrapsResponderWithoutPriorBeacon() throws Exception {
        TestAuthority authority = new TestAuthority(CryptoLib.SODIUM);
        PeerLocalIdentity alice = authority.issue(ALICE_UID);
        PeerLocalIdentity bob = authority.issue(BOB_UID);

        Receiver aliceRecv = receiver();
        Receiver bobRecv = receiver();

        PeerConnectionManager aliceMgr = new PeerConnectionManager(
                alice, (uid, data) -> { aliceRecv.senders.add(uid); aliceRecv.messages.add(data); });
        PeerConnectionManager bobMgr = new PeerConnectionManager(
                bob, (uid, data) -> { bobRecv.senders.add(uid); bobRecv.messages.add(data); });

        LanPeerTransport aliceTransport = new LanPeerTransport(aliceMgr);
        LanPeerTransport bobTransport = new LanPeerTransport(bobMgr);
        try {
            aliceTransport.start();
            bobTransport.start();

            InetAddress loopback = InetAddress.getLoopbackAddress();
            aliceTransport.observePeer(BOB_UID,
                    new InetSocketAddress(loopback, bobTransport.getDirectPort()));
            // No bobTransport.observePeer: responder bootstraps from inbound HELLO

            assertTrue(waitFor(() -> aliceMgr.isConnected(BOB_UID) && bobMgr.isConnected(ALICE_UID), 5000),
                    "asymmetric bootstrap handshake failed");

            byte[] payload = "asym".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(aliceMgr.sendMessage(BOB_UID, payload));
            assertTrue(waitFor(() -> bobRecv.messages.size() >= 1, 3000));
            assertArrayEquals(payload, bobRecv.messages.get(0));
        } finally {
            aliceTransport.close();
            bobTransport.close();
        }
    }

    @Test
    void discoveryHintDoesNotCreateAuthenticatedRoute() throws Exception {
        TestAuthority authority = new TestAuthority(CryptoLib.SODIUM);
        PeerLocalIdentity alice = authority.issue(ALICE_UID);

        PeerConnectionManager aliceMgr = new PeerConnectionManager(alice, (uid, data) -> {});

        LanPeerTransport aliceTransport = new LanPeerTransport(aliceMgr);
        try {
            aliceTransport.start();

            UUID fakeUid = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
            InetAddress loopback = InetAddress.getLoopbackAddress();
            // unused udp port likely unused (ephemeral)
            aliceTransport.observePeer(fakeUid, new InetSocketAddress(loopback, 1));

            Thread.sleep(500);
            assertFalse(aliceMgr.isConnected(fakeUid));
        } finally {
            aliceTransport.close();
        }
    }
}