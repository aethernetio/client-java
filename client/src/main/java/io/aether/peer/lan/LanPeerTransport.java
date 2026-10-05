
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



import io.aether.api.common.CryptoLib;
import io.aether.logger.Log;
import io.aether.api.peerapi.PeerFrame;
import io.aether.api.peerapi.PeerFrameType;
import io.aether.api.peerapi.PeerHello;
import io.aether.api.peerlanapi.PeerLanProbe;
import io.aether.api.peerlanapi.PeerLanSearch;
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
import java.util.Set;
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

    private static final long PROBE_INTERVAL_SECONDS = 30L;
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
    private final Set<UUID> wantedUids = ConcurrentHashMap.newKeySet();

    private MulticastSocket discoverySocket;
    private DatagramSocket directSocket;
    private InetAddress multicastAddress;
    private ScheduledExecutorService probeScheduler;
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
            // Bind order matters: create an unbound MulticastSocket, set
            // SO_REUSEADDR, and only then bind. On Linux this lets several
            // instances in the same JVM (or several JVMs on the same host)
            // share the well-known multicast port, which is required for
            // loopback tests and for multi-instance hosts.
            MulticastSocket ms = new MulticastSocket(null);
            ms.setReuseAddress(true);
            ms.setTimeToLive(1);
            ms.bind(new InetSocketAddress(MULTICAST_PORT));
            ms.joinGroup(multicastAddress);
            discoverySocket = ms;
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
            Thread t = new Thread(r, "aether-lan-probe");
            t.setDaemon(true);
            return t;
        };
        probeScheduler = Executors.newSingleThreadScheduledExecutor(tf);
        probeScheduler.scheduleAtFixedRate(this::sendProbeSafely, 0,
                PROBE_INTERVAL_SECONDS, TimeUnit.SECONDS);

    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        if (probeScheduler != null) {
            probeScheduler.shutdownNow();
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


    private void sendProbeSafely() {
        try {
            sendProbeNow();
        } catch (RuntimeException ignored) {
        }
    }


    private void sendProbeNow() {
        MulticastSocket s = discoverySocket;
        DatagramSocket d = directSocket;
        if (s == null || s.isClosed() || d == null || d.isClosed()) {
            return;
        }
        if (wantedUids.isEmpty()) {
            return;
        }
        // One PROBE per wanted uid: the packet carries the target hash so
        // only the intended recipient needs to respond, and no multicast
        // observer can tell which uid we are looking for.
        byte senderHash1 = LanPeerCodec.hash1(manager.getLocalUid());


        CryptoLib[] cryptoLibs = manager.supportedCryptoLibs()
                .toArray(new CryptoLib[0]);
        int directPort = d.getLocalPort();
        for (UUID targetUid : wantedUids) {
            byte targetHash1 = LanPeerCodec.hash1(targetUid);
            PeerLanProbe probe = new PeerLanProbe(
                    senderHash1,
                    directPort,
                    cryptoLibs,
                    targetHash1
            );
            byte[] encoded;
            try {
                encoded = LanPeerCodec.encodeProbe(probe);
            } catch (LanPeerTransportException e) {
                continue;
            }
            DatagramPacket packet = new DatagramPacket(
                    encoded, encoded.length, multicastAddress, MULTICAST_PORT);
            try {
                s.send(packet);
            } catch (IOException ignored) {
            }
        }
    }


    /**
     * Register interest in contacting the given uid on the local network
     * and immediately broadcast a PROBE so any matching peer can respond.
     *
     * <p>Idempotent: repeated calls for the same uid only ensure the uid is
     * in {@link #wantedUids} and trigger one immediate PROBE. Callers can
     * rely on {@code MessageNode.send} to invoke this on every outbound
     * message without flooding the LAN.
     */
    public void probeFor(UUID targetUid) {
        if (targetUid == null || closed.get() || !started.get()) {
            return;
        }
        if (targetUid.equals(manager.getLocalUid())) {
            return;
        }
        wantedUids.add(targetUid);
        sendProbeSafely();
    }

    /**
     * @return true if the given uid is currently in the wanted set
     */
    public boolean isWanted(UUID uid) {
        return uid != null && wantedUids.contains(uid);
    }

    public void forgetWanted(UUID uid) {
        if (uid != null) {
            wantedUids.remove(uid);
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
                handleProbe(data, packet.getAddress());
            } catch (IOException e) {
                if (closed.get()) {
                    return;
                }
            } catch (RuntimeException ignored) {
            }
        }
    }



    private void handleProbe(byte[] data, InetAddress source) {

        PeerLanProbe probe;
        try {
            probe = LanPeerCodec.decodeProbe(data);
        } catch (LanPeerTransportException e) {
            return;
        }


        byte ourHash1 = LanPeerCodec.hash1(manager.getLocalUid());
        // Only respond when the PROBE's target hash points at us.
        if (probe.getTargetHash1() != ourHash1) {
            return;
        }
        // Ignore our own PROBE looped back on the multicast socket.
        if (probe.getSenderHash1() == ourHash1
                && probe.getDirectPort() == getDirectPort()) {
            return;
        }
        // Reply SEARCH: targetHash1 = PROBE.senderHash1 (so the original
        // searcher recognizes itself). We are the target now, and we
        // expect the searcher to open the handshake as responder.
        sendSearch(source, probe.getDirectPort(), probe.getSenderHash1());
    }


    /**
     * Send a unicast SEARCH reply to the source of a PROBE. The SEARCH
     * carries our uid openly (unicast — no multicast sniffer sees it) and
     * the one-byte hash of the target uid we are looking for, so the target
     * can recognize itself without leaking to passive observers.
     */

    /**
     * Send a unicast SEARCH reply to the source of a PROBE.
     *
     * <p>Carries our uid openly (unicast — passive multicast observers never
     * see it) and the target hash copied from the PROBE's {@code senderHash1}.
     * The recipient compares that byte against {@code hash1(ownUid)} to
     * recognize itself as the intended searcher.
     *
     * @param targetAddress source address of the triggering PROBE
     * @param targetPort    direct port advertised in the PROBE
     * @param targetHash1   sender hash from the PROBE
     */
    private void sendSearch(InetAddress targetAddress, int targetPort, byte targetHash1) {
        DatagramSocket d = directSocket;
        if (d == null || d.isClosed()) {
            return;
        }
        if (targetAddress == null || targetPort < 1 || targetPort > 65535) {
            return;
        }
        PeerLanSearch search = new PeerLanSearch(
                targetHash1,
                manager.supportedCryptoLibs().toArray(new CryptoLib[0]),
                manager.getLocalUid()
        );
        byte[] encoded = LanPeerCodec.encodeSearch(search);
        DatagramPacket packet = new DatagramPacket(
                encoded, encoded.length, targetAddress, targetPort);
        try {
            d.send(packet);
        } catch (IOException ignored) {
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

                InetSocketAddress source = new InetSocketAddress(packet.getAddress(), packet.getPort());
                if (!tryHandleSearch(data, source)) {
                    handleDirect(data, source);
                }

            } catch (IOException e) {
                if (closed.get()) {
                    return;
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    /**
     * Attempt to interpret an inbound direct-frame payload as a SEARCH
     * packet. On success and if our uid hash matches the requested target,
     * register ourselves as an outgoing candidate link to the searcher (with
     * forced-initiator role) so we can send the first HELLO.
     *
     * @return {@code true} if the payload was a SEARCH packet (handled or
     *         ignored); {@code false} if it was not a SEARCH and should be
     *         treated as a normal peer frame
     */
    private boolean tryHandleSearch(byte[] data, InetSocketAddress source) {
        PeerLanSearch search;
        try {
            search = LanPeerCodec.decodeSearch(data);
        } catch (LanPeerTransportException e) {
            return false;
        }
        UUID senderUid = search.getSenderUid();
        if (senderUid == null || senderUid.equals(manager.getLocalUid())) {
            return true;
        }


        if (LanPeerCodec.hash1(manager.getLocalUid()) != search.getTargetHash1()) {
            // Not for us; ignore silently.
            return true;
        }
        try {
            observePeerAsInitiator(senderUid, source);
        } catch (RuntimeException ignored) {
        }
        return true;
    }

    /**
     * Create a link to the given peer and force our side to act as the
     * handshake initiator. Used when we received a SEARCH packet addressed
     * to us: the searcher does not yet know we exist, so we must send the
     * first HELLO regardless of UID ordering.
     */
    public PeerLink observePeerAsInitiator(UUID remoteUid, InetSocketAddress endpoint) {
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
        PeerLink candidate = manager.createLink(
                remoteUid,
                frame -> sendDirect(endpoint, frame),
                Boolean.TRUE);
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

    /**
     * Create a link to the given peer and force our side to act as the
     * handshake responder. Used when we received an inbound HELLO from a
     * peer we did not know: the sender already acted as initiator and sent
     * its HELLO, so we must not race it by sending our own HELLO.
     */
    public PeerLink observePeerAsResponder(UUID remoteUid, InetSocketAddress endpoint) {
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
        PeerLink candidate = manager.createLink(
                remoteUid,
                frame -> sendDirect(endpoint, frame),
                Boolean.FALSE);
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
            link = observePeerAsResponder(claimedUid, source);
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