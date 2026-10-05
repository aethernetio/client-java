
package io.aether.peer.lan;

import io.aether.api.common.CryptoLib;
import io.aether.api.peerlanapi.PeerLanProbe;
import io.aether.api.peerlanapi.PeerLanSearch;
import io.aether.net.fastMeta.MetaContext;
import io.aether.utils.dataio.DataInOutStatic;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;

/**
 * Strict wire codec for LAN discovery packets.
 *
 * <p>Two packet kinds share one domain prefix and are distinguished by a
 * single tag byte:
 *
 * <pre>
 *     PROBE  (broadcast, tag 0x01):
 *         PeerLanProbe{senderHash1, directPort, cryptoLibs[]}
 *         "I am here, this is my one-byte hash, this is my direct port."
 *
 *     SEARCH (unicast, tag 0x02):
 *         PeerLanSearch{targetHash1, cryptoLibs[], senderUid}
 *         "I am looking for a peer whose hash1(uid) == targetHash1.
 *          This is my uid and my direct port (taken from the SEARCH packet
 *          source address by the receiver)."
 * </pre>
 *
 * <p>All packets are untrusted routing hints: real authentication is done
 * by the subsequent PeerLogin handshake. Sender uid is carried in plain
 * form only in the SEARCH packet, which is sent as a unicast reply to a
 * PROBE's source address. A passive multicast sniffer therefore never sees
 * the uid of a probing peer, only its one-byte hash.
 */
public final class LanPeerCodec {

    private static final byte[] DOMAIN =
            "AETHER_PEER_LAN_V2\0".getBytes(StandardCharsets.US_ASCII);

    public static final byte TAG_PROBE = 0x01;
    public static final byte TAG_SEARCH = 0x02;

    private LanPeerCodec() {
    }

    public static byte[] encodeProbe(PeerLanProbe probe) {
        if (probe == null) {
            throw new LanPeerTransportException("probe is null");
        }
        if (probe.getCryptoLibs() == null || probe.getCryptoLibs().length == 0) {
            throw new LanPeerTransportException("probe cryptoLibs is null or empty");
        }
        if (probe.getDirectPort() < 1 || probe.getDirectPort() > 65535) {
            throw new LanPeerTransportException("probe directPort out of range");
        }
        byte[] body = PeerLanProbe.META.serialize(probe);
        return frame(TAG_PROBE, body);
    }

    public static PeerLanProbe decodeProbe(byte[] frame) {
        byte[] body = stripFrame(frame, TAG_PROBE);
        DataInOutStatic in = new DataInOutStatic(body, 0, body.length);
        PeerLanProbe probe;
        try {
            probe = PeerLanProbe.META.deserialize(MetaContext.STUB, in);
        } catch (LanPeerTransportException e) {
            throw e;
        } catch (Exception e) {
            throw new LanPeerTransportException("Malformed peer LAN probe", e);
        }
        if (in.getSizeForRead() != 0) {
            throw new LanPeerTransportException("Malformed peer LAN probe");
        }
        if (probe == null
                || probe.getCryptoLibs() == null
                || probe.getCryptoLibs().length == 0) {
            throw new LanPeerTransportException("Malformed peer LAN probe");
        }
        if (probe.getDirectPort() < 1 || probe.getDirectPort() > 65535) {
            throw new LanPeerTransportException("Malformed peer LAN probe");
        }
        return probe;
    }

    public static byte[] encodeSearch(PeerLanSearch search) {
        if (search == null) {
            throw new LanPeerTransportException("search is null");
        }
        if (search.getSenderUid() == null) {
            throw new LanPeerTransportException("search senderUid is null");
        }
        if (search.getCryptoLibs() == null || search.getCryptoLibs().length == 0) {
            throw new LanPeerTransportException("search cryptoLibs is null or empty");
        }
        byte[] body = PeerLanSearch.META.serialize(search);
        return frame(TAG_SEARCH, body);
    }

    public static PeerLanSearch decodeSearch(byte[] frame) {
        byte[] body = stripFrame(frame, TAG_SEARCH);
        DataInOutStatic in = new DataInOutStatic(body, 0, body.length);
        PeerLanSearch search;
        try {
            search = PeerLanSearch.META.deserialize(MetaContext.STUB, in);
        } catch (LanPeerTransportException e) {
            throw e;
        } catch (Exception e) {
            throw new LanPeerTransportException("Malformed peer LAN search", e);
        }
        if (in.getSizeForRead() != 0) {
            throw new LanPeerTransportException("Malformed peer LAN search");
        }
        if (search == null
                || search.getSenderUid() == null
                || search.getCryptoLibs() == null
                || search.getCryptoLibs().length == 0) {
            throw new LanPeerTransportException("Malformed peer LAN search");
        }
        return search;
    }

    /**
     * One-byte hash of a uid, used as a coarse filter in PROBE/SEARCH
     * packets. Deterministic so a sender can compute the target hash before
     * talking to a peer; coarse enough that a passive observer who does not
     * already know the uid cannot identify a peer from its hash byte alone.
     *
     * @param uid the uid to hash; must not be null
     * @return first byte of {@code SHA-256(uid bytes)}
     */
    public static byte hash1(UUID uid) {
        if (uid == null) {
            throw new LanPeerTransportException("uid is null");
        }
        byte[] input = new byte[16];
        long msb = uid.getMostSignificantBits();
        long lsb = uid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            input[i] = (byte) (msb >>> (8 * (7 - i)));
            input[8 + i] = (byte) (lsb >>> (8 * (7 - i)));
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input);
            return digest[0];
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new LanPeerTransportException("SHA-256 unavailable", e);
        }
    }

    private static byte[] frame(byte tag, byte[] body) {
        byte[] result = new byte[DOMAIN.length + 1 + body.length];
        System.arraycopy(DOMAIN, 0, result, 0, DOMAIN.length);
        result[DOMAIN.length] = tag;
        System.arraycopy(body, 0, result, DOMAIN.length + 1, body.length);
        return result;
    }

    private static byte[] stripFrame(byte[] frame, byte expectedTag) {
        if (frame == null || frame.length < DOMAIN.length + 1) {
            throw new LanPeerTransportException("Malformed peer LAN packet");
        }
        byte[] actualDomain = Arrays.copyOfRange(frame, 0, DOMAIN.length);
        if (!MessageDigest.isEqual(DOMAIN, actualDomain)) {
            throw new LanPeerTransportException("Malformed peer LAN packet");
        }
        if (frame[DOMAIN.length] != expectedTag) {
            throw new LanPeerTransportException("Unexpected peer LAN packet tag");
        }
        byte[] body = Arrays.copyOfRange(frame, DOMAIN.length + 1, frame.length);
        if (body.length == 0) {
            throw new LanPeerTransportException("Malformed peer LAN packet");
        }
        return body;
    }
}
