
package io.aether.peer.lan;




import io.aether.api.peerlanapi.PeerLanBeacon;
import io.aether.net.fastMeta.MetaContext;
import io.aether.utils.dataio.DataInOutStatic;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;


/**
 * Strict wire codec for the LAN discovery beacon.
 *
 * <p>Wire layout:</p>
 * <pre>
 *     ASCII("AETHER_PEER_LAN_DISCOVERY_V1\0") + PeerLanBeacon.META serialization
 * </pre>
 *
 * <p>The beacon is a routing/discovery hint only. It is <strong>not</strong>
 * authenticated: a spoofed beacon can at most create a useless candidate
 * {@code PeerLink}. Real authentication is performed by the PeerLogin
 * handshake.</p>
 */

public final class LanPeerBeaconCodec {

    private static final byte[] DOMAIN =
            "AETHER_PEER_LAN_DISCOVERY_V1\0".getBytes(StandardCharsets.US_ASCII);

    private LanPeerBeaconCodec() {
    }


    /**
     * Encode a beacon into the wire format. Rejects null fields and
     * out-of-range ports.
     */

    public static byte[] encode(PeerLanBeacon beacon) {
        if (beacon == null) {
            throw new LanPeerTransportException("beacon is null");
        }
        if (beacon.getUid() == null) {
            throw new LanPeerTransportException("beacon uid is null");
        }
        if (beacon.getCryptoLib() == null) {
            throw new LanPeerTransportException("beacon cryptoLib is null");
        }
        if (beacon.getDirectPort() < 1 || beacon.getDirectPort() > 65535) {
            throw new LanPeerTransportException("beacon directPort out of range");
        }
        byte[] body = PeerLanBeacon.META.serialize(beacon);
        byte[] result = new byte[DOMAIN.length + body.length];
        System.arraycopy(DOMAIN, 0, result, 0, DOMAIN.length);
        System.arraycopy(body, 0, result, DOMAIN.length, body.length);
        return result;
    }


    /**
     * Strict decode. Rejects bad domain, malformed FastMeta, trailing bytes,
     * null fields, and out-of-range direct ports.
     */

    public static PeerLanBeacon decode(byte[] frame) {
        if (frame == null || frame.length < DOMAIN.length) {
            throw new LanPeerTransportException("Malformed peer LAN beacon");
        }
        byte[] actualDomain = Arrays.copyOfRange(frame, 0, DOMAIN.length);
        if (!MessageDigest.isEqual(DOMAIN, actualDomain)) {
            throw new LanPeerTransportException("Malformed peer LAN beacon");
        }
        byte[] body = Arrays.copyOfRange(frame, DOMAIN.length, frame.length);
        if (body.length == 0) {
            throw new LanPeerTransportException("Malformed peer LAN beacon");
        }
        DataInOutStatic in = new DataInOutStatic(body, 0, body.length);
        PeerLanBeacon beacon;
        try {
            beacon = PeerLanBeacon.META.deserialize(MetaContext.STUB, in);
        } catch (LanPeerTransportException e) {
            throw e;
        } catch (Exception e) {
            throw new LanPeerTransportException("Malformed peer LAN beacon", e);
        }
        if (in.getSizeForRead() != 0) {
            throw new LanPeerTransportException("Malformed peer LAN beacon");
        }
        if (beacon == null
                || beacon.getUid() == null
                || beacon.getCryptoLib() == null) {
            throw new LanPeerTransportException("Malformed peer LAN beacon");
        }
        if (beacon.getDirectPort() < 1 || beacon.getDirectPort() > 65535) {
            throw new LanPeerTransportException("Malformed peer LAN beacon");
        }
        return beacon;
    }
}