
package io.aether.peer.lan;

/**
 * Java-specific LAN peer transport based on UDP sockets.
 *
 * <p>Two sockets are used:</p>
 * <ul>
 *   <li>a multicast {@link java.net.MulticastSocket} on
 *       {@code 239.255.72.65:57321} for periodic discovery beacons
 *       ({@link PeerLanBeacon});</li>
 *   <li>an ephemeral unicast {@link java.net.DatagramSocket} for direct
 *       peer frames (one {@code PeerFrame} per UDP datagram).</li>
 * </ul>
 *
 * <p>Beacons are <strong>untrusted</strong>: they carry only
 * {@code uid}, {@code cryptoLib} hint and {@code directPort}. Real peer
 * authentication is always performed by the PeerLogin handshake
 * implemented in {@code io.aether.peer.PeerConnection}.</p>
 *
 * <p>The transport uses {@link PeerConnectionManager} to create and track
 * {@link PeerLink} instances. For every physical candidate
 * {@code (remoteUid, remoteAddress, remoteDirectPort)} at most one
 * {@code PeerLink} is registered.</p>
 *
 * <p>This class does <strong>not</strong> implement any retry/timeout
 * scheduling: repeated beacons can eventually trigger
 * {@link PeerLink#restart()} on failed links, but no dedicated reconnect
 * timer is started here.</p>
 */


import io.aether.api.peerapi.PeerFrame;
import io.aether.api.peerapi.PeerFrameType;
import io.aether.api.peerapi.PeerHello;
import io.aether.api.peerlanapi.PeerLanBeacon;
import io.aether.peer.PeerConnection;
import io.aether.peer.PeerConnectionException;
import io.aether.peer.PeerConnectionManager;
import io.aether.peer.PeerFrameCodec;
import io.aether.peer.PeerLink;
import io.aether.peer.PeerLoginException;
import io.aether.peer.PeerPacketException;
import io.aether.peer.PeerStreamException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LanPeerTransport implements AutoCloseable {

    private static final String MULTICAST_ADDRESS = "239.255.72.65";
    private static final int MULTICAST_PORT = 57321;
    private static final long BEACON_INTERVAL_SECONDS = 5L;
    private static final int MAX_DIRECT_DATAGRAM = 65507;

    private static final class LanLinkKey {
        private final UUID remoteUid;
        private final InetAddress address;
        private final int port;

        LanLinkKey(UUID remoteUid, InetAddress address, int port) {
            this.remoteUid = remoteUid;
            this.address = address;
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LanLinkKey)) return false;
            LanLinkKey that = (LanLinkKey) o;
            return port == that.port
                    && remoteUid.equals(that.remoteUid)
                    && address.equals(that.address);
        }

        @Override
        public int hashCode() {
            int h = remoteUid.hashCode();
            h = 31 * h + address.hashCode();
            h = 31 * h + port;
            return h;
        }
    }

    private static final class LanLinkEntry {
        final UUID remoteUid;
        final InetSocketAddress endpoint;
        final PeerLink link;

        LanLinkEntry(UUID remoteUid, InetSocketAddress endpoint, PeerLink link) {
            this.remoteUid = remoteUid;
            this.endpoint = endpoint;
            this.link = link;
        }
    }

    private final PeerConnectionManager manager;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Map<LanLinkKey, LanLinkEntry> registry = new ConcurrentHashMap<>();

    private MulticastSocket discoverySocket;
    private DatagramSocket directSocket;
    private InetAddress multicastAddress;
    private ScheduledExecutorService beaconScheduler;
    private Thread discoveryThread;
    private Thread directThread;


    /**
     * Create a LAN transport attached to the given peer connection manager.
     * The manager is used to derive the local uid, crypto lib and to
     * register {@link PeerLink} instances.
     */

    public LanPeerTransport(PeerConnectionManager manager) {
        if (manager == null) {
            throw new LanPeerTransportException("manager is null");
        }
        this.manager = manager;
    }


    /**
     * @return the ephemeral local direct UDP port assigned by the OS,
     *         or {@code 0} before {@link #start()}.
     */

    public int getDirectPort() {
        DatagramSocket s = directSocket;
        return s == null ? 0 : s.getLocalPort();
    }


    /**
     * Start discovery multicast and direct UDP receive threads, plus the
     * periodic beacon scheduler. Must be called at most once.
     */

    public void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("LanPeerTransport already started");
        }
        if (closed.get()) {
            throw new IllegalStateException("LanPeerTransport is closed");
        }
        try {
            multicastAddress = InetAddress.getByName(MULTICAST_ADDRESS);
            discoverySocket = new MulticastSocket(MULTICAST_PORT);
            discoverySocket.setReuseAddress(true);
            discoverySocket.setTimeToLive(1);
            discoverySocket.joinGroup(multicastAddress);
            directSocket = new DatagramSocket(0);
        } catch (IOException e) {
            closeQuietly();
            started.set(false);
            throw new LanPeerTransportException("Failed to start LAN transport", e);
        }

        discoveryThread = new Thread(this::discoveryLoop, "aether-lan-discovery");
        discoveryThread.setDaemon(true);
        discoveryThread.start();

        directThread = new Thread(this::directLoop, "aether-lan-direct");
        directThread.setDaemon(true);
        directThread.start();

        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "aether-lan-beacon");
            t.setDaemon(true);
            return t;
        };
        beaconScheduler = Executors.newSingleThreadScheduledExecutor(tf);
        beaconScheduler.scheduleAtFixedRate(this::sendBeaconSafely, 0,
                BEACON_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (beaconScheduler != null) {
            beaconScheduler.shutdownNow();
        }
        if (discoverySocket != null) {
            discoverySocket.close();
        }
        if (directSocket != null) {
            directSocket.close();
        }
        for (LanLinkEntry entry : registry.values()) {
            try {
                entry.link.close();
            } catch (RuntimeException ignored) {
            }
        }
        registry.clear();
    }

    private void closeQuietly() {
        if (discoverySocket != null) {
            discoverySocket.close();
        }
        if (directSocket != null) {
            directSocket.close();
        }
    }


    /**
     * Register a candidate physical endpoint for the given remote UID and
     * return the corresponding {@link PeerLink}, creating it lazily.
     *
     * <p>Idempotent for the same {@code (uid, address, port)} key. If an
     * existing link is currently {@code FAILED}, this call triggers
     * {@link PeerLink#restart()}.</p>
     *
     * <p>This method is the deterministic test/bootstrap entry point for
     * the LAN transport: it does not depend on multicast delivery.</p>
     */

    public PeerLink observePeer(UUID remoteUid, InetSocketAddress endpoint) {
        if (closed.get()) {
            throw new LanPeerTransportException("LanPeerTransport is closed");
        }
        if (remoteUid == null) {
            throw new LanPeerTransportException("remoteUid is null");
        }
        if (endpoint == null || endpoint.getAddress() == null
                || endpoint.getPort() < 1 || endpoint.getPort() > 65535) {
            throw new LanPeerTransportException("Invalid endpoint");
        }
        if (remoteUid.equals(manager.getLocalUid())) {
            throw new LanPeerTransportException("Cannot observe self");
        }
        LanLinkKey key = new LanLinkKey(remoteUid, endpoint.getAddress(), endpoint.getPort());
        LanLinkEntry existing = registry.get(key);
        if (existing != null) {
            PeerLink link = existing.link;
            if (link.isFailed()) {
                try {
                    link.restart();
                } catch (RuntimeException ignored) {
                }
            }
            return link;
        }

        PeerLink candidate = manager.createLink(remoteUid, frame -> sendDirect(endpoint, frame));
        LanLinkEntry entry = new LanLinkEntry(remoteUid, endpoint, candidate);
        LanLinkEntry winner = registry.putIfAbsent(key, entry);
        if (winner != null) {
            try {
                candidate.close();
            } catch (RuntimeException ignored) {
            }
            return winner.link;
        }
        try {
            candidate.start();
        } catch (RuntimeException ignored) {
        }
        return candidate;
    }

    private void sendDirect(InetSocketAddress endpoint, byte[] frame) {
        if (frame == null || frame.length > MAX_DIRECT_DATAGRAM) {
            throw new LanPeerTransportException("Peer frame exceeds UDP datagram limit");
        }
        DatagramSocket s = directSocket;
        if (s == null || s.isClosed()) {
            throw new LanPeerTransportException("Direct socket is not open");
        }
        try {
            DatagramPacket packet = new DatagramPacket(frame, frame.length, endpoint);
            s.send(packet);
        } catch (IOException e) {
            throw new LanPeerTransportException("Failed to send peer frame", e);
        }
    }

    private void sendBeaconSafely() {
        try {
            sendBeacon();
        } catch (RuntimeException ignored) {
        }
    }

    private void sendBeacon() {
        MulticastSocket s = discoverySocket;
        DatagramSocket d = directSocket;
        if (s == null || s.isClosed() || d == null || d.isClosed()) {
            return;
        }
        PeerLanBeacon beacon = new PeerLanBeacon(
                manager.getLocalUid(),
                manager.getCryptoLib(),
                d.getLocalPort()
        );
        byte[] encoded = LanPeerBeaconCodec.encode(beacon);
        DatagramPacket packet = new DatagramPacket(
                encoded, encoded.length, multicastAddress, MULTICAST_PORT);
        try {
            s.send(packet);
        } catch (IOException ignored) {
        }
    }

    private void discoveryLoop() {
        byte[] buffer = new byte[2048];
        while (!closed.get()) {
            MulticastSocket s = discoverySocket;
            if (s == null || s.isClosed()) {
                return;
            }
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                s.receive(packet);
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                handleBeacon(data, packet.getAddress());
            } catch (IOException e) {
                if (closed.get()) {
                    return;
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void handleBeacon(byte[] data, InetAddress source) {
        PeerLanBeacon beacon;
        try {
            beacon = LanPeerBeaconCodec.decode(data);
        } catch (LanPeerTransportException e) {
            return;
        }
        if (beacon.getUid().equals(manager.getLocalUid())) {
            return;
        }
        if (beacon.getCryptoLib() != manager.getCryptoLib()) {
            return;
        }
        InetSocketAddress endpoint = new InetSocketAddress(source, beacon.getDirectPort());
        try {
            observePeer(beacon.getUid(), endpoint);
        } catch (RuntimeException ignored) {
        }
    }

    private void directLoop() {
        byte[] buffer = new byte[MAX_DIRECT_DATAGRAM];
        while (!closed.get()) {
            DatagramSocket s = directSocket;
            if (s == null || s.isClosed()) {
                return;
            }
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                s.receive(packet);
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                handleDirect(data, new InetSocketAddress(packet.getAddress(), packet.getPort()));
            } catch (IOException e) {
                if (closed.get()) {
                    return;
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void handleDirect(byte[] data, InetSocketAddress source) {
        LanLinkEntry routed = findEntryByEndpoint(source);
        if (routed != null) {
            try {
                routed.link.acceptFrame(data);
            } catch (PeerConnectionException
                    | PeerLoginException
                    | PeerPacketException
                    | PeerStreamException
                    | IllegalStateException
                    | LanPeerTransportException ignored) {
            }
            return;
        }

        PeerFrame outer;
        try {
            outer = PeerFrameCodec.decode(data);
        } catch (PeerConnectionException e) {
            return;
        }
        if (outer.getType() != PeerFrameType.HELLO) {
            return;
        }
        PeerHello hello;
        try {
            hello = PeerFrameCodec.decodeHello(outer);
        } catch (PeerConnectionException e) {
            return;
        }
        if (hello.getIdentity() == null || hello.getIdentity().getData() == null) {
            return;
        }
        UUID claimedUid = hello.getIdentity().getData().getUid();
        if (claimedUid == null || claimedUid.equals(manager.getLocalUid())) {
            return;
        }
        PeerLink link;
        try {
            link = observePeer(claimedUid, source);
        } catch (RuntimeException e) {
            return;
        }
        try {
            link.acceptFrame(data);
        } catch (PeerConnectionException
                | PeerLoginException
                | PeerPacketException
                | PeerStreamException
                | IllegalStateException
                | LanPeerTransportException ignored) {
        }
    }

    private LanLinkEntry findEntryByEndpoint(InetSocketAddress source) {
        for (LanLinkEntry entry : registry.values()) {
            if (entry.endpoint.equals(source)) {
                return entry;
            }
        }
        return null;
    }
}