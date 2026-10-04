package io.aether.peer.lan;

/**
 * Runtime exception thrown by the LAN peer transport implementation for
 * local/configuration/UDP-socket level failures.
 *
 * <p>This is a transport-layer exception, not a peer protocol exception.
 * Peer protocol errors (authentication, malformed peer frames, etc.) are
 * surfaced via {@code io.aether.peer.PeerConnectionException},
 * {@code io.aether.peer.PeerLoginException}, {@code io.aether.peer.PeerPacketException}
 * and {@code io.aether.peer.PeerStreamException}.</p>
 */


public final class LanPeerTransportException extends RuntimeException {

    public LanPeerTransportException(String message) {
        super(message);
    }

    public LanPeerTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}