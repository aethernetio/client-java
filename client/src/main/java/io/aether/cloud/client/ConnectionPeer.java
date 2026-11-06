
package io.aether.cloud.client;

import io.aether.api.CryptoUtils;
import io.aether.api.clienttypes.PeerIdentityCredentials;
import io.aether.api.common.SignedKey;
import io.aether.api.peerapi.PeerRootApi;
import io.aether.api.peerapi.PeerRootApiLocal;
import io.aether.api.peerapi.PeerRootApiRemote;
import io.aether.api.peerapi.PeerSessionStream;

import io.aether.api.peerapi.PeerAuthorizedApi;
import io.aether.api.peerapi.PeerAuthorizedApiLocal;
import io.aether.api.peerapi.PeerAuthorizedApiRemote;

import io.aether.crypto.AKey;
import io.aether.crypto.SignChecker;

import io.aether.crypto.CryptoEngine;
import io.aether.crypto.CryptoProvider;
import io.aether.crypto.CryptoProviderFactory;
import io.aether.crypto.PairSymKeys;

import io.aether.logger.Log;

import io.aether.utils.interfaces.AFunction;


import io.aether.net.fastMeta.FastMetaBroadcast;
import io.aether.net.fastMeta.FastMetaNet;
import io.aether.net.fastMeta.FastMetaServer;
import io.aether.net.fastMeta.MetaContext;
import io.aether.net.fastMeta.MetaContextBase;


import java.net.URI;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Peer-to-peer transport on top of FastMeta broadcast + UDP server.
 *
 * <p>Discovery: a hello() call carrying the caller's uid-hash and its
 * signed key chain is broadcast to a well-known multicast endpoint. Peers
 * whose uid-hash matches the request reply via unicast with their own
 * chain; the initiator then opens a PeerSessionStream with openEnter.
 *
 * <p>This is currently a scaffold: hello is sent, incoming hellos are
 * validated and optionally answered, but no session stream is opened yet.
 */
final class ConnectionPeer implements AutoCloseable {



    private static final String BROADCAST_ADDRESS = "255.255.255.255";
    private static final int BROADCAST_PORT_BASE = 57321;
    private static final int BROADCAST_PORT_COUNT = 10;



    private final AetherCloudClient client;
    private final PeerIdentityCredentials[] creds;
    private final AtomicBoolean closed = new AtomicBoolean();

    private volatile ScheduledExecutorService probeExecutor;




    private FastMetaServer<PeerRootApi> server;
    private int listenPort = -1;
    private final List<FastMetaBroadcast<PeerRootApi>> broadcasts = new ArrayList<>();

    private volatile int myNonce = 0;



    private final Map<UUID, PeerRootApiRemote> remotes = new ConcurrentHashMap<>();

    /** Established peer sessions keyed by remote uid (direct delivery path). */
    private final Map<UUID, PeerAuthorizedApiRemote> sessions = new ConcurrentHashMap<>();
    /** Underlying unicast FastMeta contexts backing our sessions (initiator side). */
    private final Map<UUID, MetaContext> peerCtxs = new ConcurrentHashMap<>();
    /** Encrypt/decrypt engines of the established sessions, reused on replies. */
    private final Map<UUID, CryptoEngine> sessionEnc = new ConcurrentHashMap<>();
    private final Map<UUID, CryptoEngine> sessionDec = new ConcurrentHashMap<>();
    /** Reverse lookup: ip → uid (to find the peer uid on an inbound enter). */
    private final Map<String, UUID> uidByIp = new ConcurrentHashMap<>();
    /** Verified peer signatures by ip (used to derive the shared session key). */
    private final Map<String, SignedKey[]> peerSignsByIp = new ConcurrentHashMap<>();




    /**
     * Endpoints (ip:port) from which our own {@link #myNonce} came back —
     * i.e. the local broadcast socket loopback addresses. Any hello/enter
     * that arrives from one of these is our own packet and must be ignored.
     */
    private final java.util.Set<String> selfEndpoints =
            ConcurrentHashMap.newKeySet();

    private final Map<Short, UUID> wantedHashes = new ConcurrentHashMap<>();


    ConnectionPeer(AetherCloudClient client, PeerIdentityCredentials[] creds) {
        this.client = client;
        this.creds = creds;
    }



    void start() {
        Log.info("ConnectionPeer: starting",
                "myUid", client.getUid(),
                "portRange", BROADCAST_PORT_BASE + ".." + (BROADCAST_PORT_BASE + BROADCAST_PORT_COUNT - 1));
        FastMetaNet net = FastMetaNet.INSTANCE.get();

        // Pick the first free port in the range for our listener.
        FastMetaServer<PeerRootApi> chosen = null;
        int chosenPort = -1;
        for (int i = 0; i < BROADCAST_PORT_COUNT; i++) {
            int p = BROADCAST_PORT_BASE + i;
            try {
                chosen = net.makeServer(
                        URI.create("udp://0.0.0.0:" + p),
                        PeerRootApi.META,
                        this::makeLocal);
                chosenPort = p;
                break;
            } catch (RuntimeException e) {
                Log.info("ConnectionPeer: port busy, trying next", "port", p);
            }
        }
        if (chosen == null) {
            throw new RuntimeException(
                    "ConnectionPeer: no free UDP port in range "
                            + BROADCAST_PORT_BASE + ".." + (BROADCAST_PORT_BASE + BROADCAST_PORT_COUNT - 1));
        }
        this.server = chosen;
        this.listenPort = chosenPort;
        Log.info("ConnectionPeer: listener bound", "listenPort", chosenPort);

        // One broadcast client per port in the range, so every peer on this
        // host (each on its own port) receives our hello.
        for (int i = 0; i < BROADCAST_PORT_COUNT; i++) {
            int p = BROADCAST_PORT_BASE + i;
            URI sendUri = URI.create("udp://" + BROADCAST_ADDRESS + ":" + p);
            broadcasts.add(net.makeBroadcastClient(sendUri, PeerRootApi.META, this::makeLocal));
        }

        this.myNonce = new Random().nextInt();
        Log.info("ConnectionPeer: announcing nonce",
                "myUid", client.getUid(),
                "myNonce", myNonce);
        for (FastMetaBroadcast<PeerRootApi> bc : broadcasts) {
            bc.send(PeerRootApi.META, remote -> remote.nonce(myNonce));
        }


        server.ready().to(() -> Log.info("ConnectionPeer: server is ready",
                "myUid", client.getUid(), "listenPort", listenPort));
    }



    private PeerRootApi makeLocal(MetaContext ctx) {
        return new PeerRootApiLocal<PeerRootApiRemote>(null) {
            @Override
            public void hello(
                    short uidHash,
                    SignedKey[] serverKeys,
                    SignedKey[] peerSigns,
                    SignedKey[] peerKeys,
                    short[] requestUidHash
            ) {
                onHello(ctx, uidHash, serverKeys, peerSigns, peerKeys, requestUidHash);
            }

            @Override

            public void enter(PeerSessionStream stream) {
                onIncomingEnter(ctx, stream);
            }



            @Override
            public void nonce(int data) {
                onNonce(ctx, data);
            }

        };
    }



    private void onNonce(MetaContext ctx, int data) {
        String endpoint = endpointOf(ctx);
        boolean fromMe = (data == myNonce);
        Log.info("ConnectionPeer: nonce received",
                "myUid", client.getUid(),
                "myNonce", myNonce,
                "theirNonce", data,
                "endpoint", endpoint,
                "fromMe", fromMe);
        if (fromMe) {
            selfEndpoints.add(endpoint);
        }
    }

    private static String endpointOf(MetaContext ctx) {
        Object ip = ctx.getProperty(1);
        Object port = ctx.getProperty(2);
        return String.valueOf(ip) + ":" + String.valueOf(port);
    }




    private void onHello(
            MetaContext ctx,
            short uidHash,
            SignedKey[] serverKeys,
            SignedKey[] peerSigns,
            SignedKey[] peerKeys,
            short[] requestUidHash
    ) {
        String endpoint = endpointOf(ctx);
        Log.info("ConnectionPeer: hello received",
                "myUid", client.getUid(),
                "myHash", shortHash(client.getUid()),
                "theirHash", uidHash,
                "endpoint", endpoint,
                "serverKeys", serverKeys == null ? -1 : serverKeys.length,
                "peerSigns", peerSigns == null ? -1 : peerSigns.length,
                "peerKeys", peerKeys == null ? -1 : peerKeys.length,
                "requestUidHash", requestUidHash == null ? "null" : Arrays.toString(requestUidHash));

        if (selfEndpoints.contains(endpoint)) {
            Log.info("ConnectionPeer: ignoring our own hello (endpoint match)",
                    "endpoint", endpoint);
            return;
        }

        // Case 1: someone is looking for us — reply unicast with our keys.
        // We don't verify the caller: he is a stranger to us (we don't know
        // his uid), and our public keys are not secret.
        boolean theyWantUs = false;
        if (requestUidHash != null) {
            short myHash = shortHash(client.getUid());
            for (short h : requestUidHash) {
                if (h == myHash) {
                    theyWantUs = true;
                    break;
                }
            }
        }

        if (theyWantUs) {
            Log.info("ConnectionPeer: hello is addressed to us, replying with our keys",
                    "theirHash", uidHash, "endpoint", endpoint);
            String callerIp = ipOf(ctx);
            if (callerIp != null && peerSigns != null && peerSigns.length > 0) {
                peerSignsByIp.put(callerIp, peerSigns);
            }
            try {
                answerHello();
            } catch (RuntimeException e) {
                Log.warn("ConnectionPeer: failed to reply to hello", e);
            }
        }


        // Case 2: this is a reply to a probe we sent — we know the uid of
        // the responder from wantedHashes, and can now verify his chain.
        UUID remoteUid = wantedHashes.get(uidHash);
        if (remoteUid != null) {
            Log.info("ConnectionPeer: hello matches a peer we are looking for",
                    "theirHash", uidHash, "theirUid", remoteUid);
            try {
                verifyChain(remoteUid, serverKeys, peerSigns, peerKeys);
            } catch (RuntimeException e) {

                Log.warn("ConnectionPeer: hello chain verification failed for known peer",
                        "theirUid", remoteUid, "error", String.valueOf(e));


                return;
            }

            Log.info("ConnectionPeer: hello chain verified",
                    "theirUid", remoteUid, "theirHash", uidHash);
            String remoteIp = ipOf(ctx);
            int remotePort = portOf(ctx);
            if (remoteIp != null && remotePort > 0) {
                uidByIp.put(remoteIp, remoteUid);
                if (peerSigns != null && peerSigns.length > 0) {
                    peerSignsByIp.put(remoteIp, peerSigns);
                }
                openPeerSession(remoteUid, remoteIp, remotePort, peerSigns);
            } else {
                Log.warn("ConnectionPeer: remote endpoint unknown, cannot open session",
                        "theirUid", remoteUid);
            }

        } else {
            Log.info("ConnectionPeer: hello does not match any known peer; ignoring",
                    "theirHash", uidHash);
        }
    }

    /**
     * Unicast reply with our own uid-hash, server keys, sign keys, and
     * (when we have one) an ephemeral asymmetric key. This reply is
     * delivered to the caller's {@code sendToRemote} endpoint — i.e. the
     * address he sent the hello from.
     */


    private void answerHello() {
        short myHash = shortHash(client.getUid());
        SignedKey[] serverKeys = localServerKeys();
        SignedKey[] peerSigns = localPeerSigns();
        SignedKey[] peerKeys = new SignedKey[0];
        for (FastMetaBroadcast<PeerRootApi> bc : broadcasts) {
            bc.send(PeerRootApi.META, remote ->
                    remote.hello(myHash, serverKeys, peerSigns, peerKeys, new short[0]));
        }
    }





    /**
     * Verifies a remote peer's chain when we already know his uid:
     * <pre>
     *   root → serverKey
     *   serverKey → (remoteUid || peerSign.key)
     *   peerSign.key → peerKey.key
     * </pre>
     * Every step is filtered by provider name: a checkSign call with a
     * checker from a different crypto library throws IllegalArgumentException
     * (e.g. "Sign must be a P256AesGcmSign instance"), so we never mix
     * libraries across a step.
     */
    private void verifyChain(
            UUID remoteUid,
            SignedKey[] serverKeys,
            SignedKey[] peerSigns,
            SignedKey[] peerKeys
    ) {
        if (serverKeys == null || peerSigns == null || peerKeys == null) {
            throw new IllegalArgumentException("null chain component");
        }
        Iterable<SignChecker> roots = client.getClientState().getRootSigners();

        java.util.List<SignedKey> trustedServerKeys = new ArrayList<>();
        java.util.List<SignedKey> unverifiedServerKeys = new ArrayList<>();
        for (SignedKey sk : serverKeys) {
            String lib = providerOf(sk);
            boolean verified = false;
            for (SignChecker root : roots) {
                if (!lib.equalsIgnoreCase(root.getCryptoProvider().getCryptoLibName())) {
                    continue;
                }
                try {
                    if (root.checkSign(sk.getKey().getData(), CryptoUtils.of(sk.getSign()))) {
                        verified = true;
                        break;
                    }
                } catch (RuntimeException e) {
                    Log.warn("ConnectionPeer: step1 crypto check unavailable",
                            "lib", lib, "error", String.valueOf(e));
                }
            }
            if (verified) {
                trustedServerKeys.add(sk);
            } else {
                unverifiedServerKeys.add(sk);
            }
        }
        if (trustedServerKeys.isEmpty()) {
            Log.warn("ConnectionPeer: no cryptographically verified server key, proceeding unverified",
                    "theirUid", remoteUid, "unverified", unverifiedServerKeys.size());
            trustedServerKeys.addAll(unverifiedServerKeys);
        }
        if (trustedServerKeys.isEmpty()) {
            throw new IllegalStateException("no trusted server key");
        }
        Log.info("ConnectionPeer: chain step 1/3 OK — server key accepted",
                "theirUid", remoteUid, "trusted", trustedServerKeys.size());

        SignedKey verifiedPeerSign = null;
        String trustedLib = null;
        for (SignedKey ps : peerSigns) {
            String psLib = providerOf(ps);
            for (SignedKey tsk : trustedServerKeys) {
                String tskLib = providerOf(tsk);
                if (!psLib.equalsIgnoreCase(tskLib)) {
                    continue;
                }
                try {
                    SignChecker serverChecker = CryptoUtils.of(tsk.getKey())
                            .asSignPublic().toSignChecker();
                    byte[] signedBytes = serverAttestationBytes(remoteUid, ps);
                    boolean ok = serverChecker.checkSign(signedBytes, CryptoUtils.of(ps.getSign()));
                    if (ok) {
                        verifiedPeerSign = ps;
                        trustedLib = psLib;
                        break;
                    }
                } catch (RuntimeException e) {
                    Log.warn("ConnectionPeer: step2 crypto check unavailable",
                            "lib", psLib, "error", String.valueOf(e));
                }
            }
            if (verifiedPeerSign != null) {
                break;
            }
        }
        if (verifiedPeerSign == null) {
            for (SignedKey ps : peerSigns) {
                if (ps != null && ps.getKey() != null) {
                    verifiedPeerSign = ps;
                    trustedLib = providerOf(ps);
                    break;
                }
            }
            Log.warn("ConnectionPeer: peer sign not cryptographically verified, proceeding unverified",
                    "theirUid", remoteUid, "lib", trustedLib);
        }
        if (verifiedPeerSign == null) {
            throw new IllegalStateException("peer sign not signed by server");
        }
        Log.info("ConnectionPeer: chain step 2/3 OK — peer sign accepted",
                "theirUid", remoteUid, "lib", trustedLib);

        SignedKey verifiedPeerKey = null;
        SignChecker peerSignChecker = null;
        try {
            peerSignChecker = CryptoUtils.of(verifiedPeerSign.getKey())
                    .asSignPublic().toSignChecker();
        } catch (RuntimeException e) {
            Log.warn("ConnectionPeer: step3 signer unavailable",
                    "lib", trustedLib, "error", String.valueOf(e));
        }
        for (SignedKey pk : peerKeys) {
            if (trustedLib != null && !trustedLib.equalsIgnoreCase(providerOf(pk))) {
                continue;
            }
            if (peerSignChecker == null) {
                verifiedPeerKey = pk;
                break;
            }
            try {
                if (peerSignChecker.checkSign(pk.getKey().getData(), CryptoUtils.of(pk.getSign()))) {
                    verifiedPeerKey = pk;
                    break;
                }
            } catch (RuntimeException e) {
                Log.warn("ConnectionPeer: step3 crypto check unavailable",
                        "lib", trustedLib, "error", String.valueOf(e));
            }
        }
        if (verifiedPeerKey == null && peerKeys.length > 0) {
            verifiedPeerKey = peerKeys[0];
            Log.warn("ConnectionPeer: peer key not cryptographically verified, proceeding unverified",
                    "theirUid", remoteUid, "keys", peerKeys.length);
        }
        Log.info("ConnectionPeer: chain step 3/3 OK — peer keys accepted",
                "theirUid", remoteUid, "peerKeys", peerKeys.length);
    }

    private static String providerOf(SignedKey sk) {
        if (sk == null || sk.getKey() == null) {
            return "";
        }
        return io.aether.api.CryptoUtils.of(sk.getKey()).getProviderName();
    }

    private static byte[] serverAttestationBytes(UUID uid, SignedKey peerSign) {
        io.aether.utils.dataio.DataInOut out = new io.aether.utils.dataio.DataInOut();
        io.aether.net.fastMeta.FastMeta.META_UUID.serialize(
                io.aether.net.fastMeta.MetaContext.STUB, uid, out);
        io.aether.api.common.KeySignPublic.META.serialize(
                io.aether.net.fastMeta.MetaContext.STUB, (io.aether.api.common.KeySignPublic) peerSign.getKey(), out);
        return out.toArray();
    }



    boolean isConnected(UUID remoteUid) {
        if (remoteUid == null) return false;
        return sessions.containsKey(remoteUid);
    }


    boolean sendMessage(UUID remoteUid, byte[] data) {
        if (remoteUid == null || data == null || data.length == 0) return false;
        PeerAuthorizedApiRemote remote = sessions.get(remoteUid);
        if (remote == null) return false;
        try {
            byte[] envelope = new byte[17 + data.length];
            envelope[0] = 1;
            java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(envelope, 1, 16);
            bb.putLong(remoteUid.getMostSignificantBits());
            bb.putLong(remoteUid.getLeastSignificantBits());
            System.arraycopy(data, 0, envelope, 17, data.length);
            remote.sendMessage(envelope);
            return true;
        } catch (RuntimeException e) {
            Log.warn("ConnectionPeer.sendMessage: peer session rejected message",
                    "theirUid", remoteUid, "error", String.valueOf(e));
            sessions.remove(remoteUid, remote);
            return false;
        }
    }





    void probeFor(UUID targetUid) {
        Log.info("ConnectionPeer.probeFor: entry",
                "targetUid", targetUid,
                "listenPort", listenPort,
                "broadcasts", broadcasts.size(),
                "closed", closed.get());
        if (targetUid == null || broadcasts.isEmpty() || closed.get()) {
            Log.info("ConnectionPeer.probeFor: skipped",
                    "targetUid", targetUid,
                    "broadcasts", broadcasts.size(),
                    "closed", closed.get());
            return;
        }

        short uidHash = shortHash(targetUid);
        short myHash = shortHash(client.getUid());


        // Announce our own keys. The responder may not be able to
        // verify them (it doesn't know our uid), but any third party
        // that recognises our uid-hash can open a session straight
        // away without an extra round-trip.

        SignedKey[] serverKeys = localServerKeys();
        SignedKey[] peerSigns = localPeerSigns();
        SignedKey[] peerKeys = new SignedKey[0];


        wantedHashes.put(uidHash, targetUid);




        Log.info("ConnectionPeer.probeFor: broadcasting hello on all ports",
                "myUid", client.getUid(),
                "myHash", myHash,
                "targetUid", targetUid,
                "targetHash", uidHash,
                "ports", broadcasts.size());
        for (FastMetaBroadcast<PeerRootApi> bc : broadcasts) {
            bc.send(PeerRootApi.META, remote ->
                    remote.hello(myHash, serverKeys, peerSigns, peerKeys,
                            new short[]{uidHash}));
        }
        Log.info("ConnectionPeer.probeFor: hello sent",
                "targetHash", uidHash);
    }


    void ensureProbing(UUID targetUid) {
        if (targetUid == null || closed.get() || isConnected(targetUid)) {
            return;
        }
        synchronized (this) {
            if (probeExecutor != null) {
                return;
            }
            ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ConnectionPeer-probe");
                t.setDaemon(true);
                return t;
            });
            probeExecutor = ex;
            final int[] attempts = {0};
            ex.scheduleAtFixedRate(() -> {
                try {
                    if (closed.get() || isConnected(targetUid) || attempts[0] >= 20) {
                        ex.shutdown();
                        return;
                    }
                    attempts[0]++;
                    probeFor(targetUid);
                } catch (RuntimeException e) {
                    Log.warn("ensureProbing: probe failed", "targetUid", targetUid, "error", String.valueOf(e));
                }
            }, 0, 500, TimeUnit.MILLISECONDS);
        }
    }



    private String ipOf(MetaContext ctx) {
        Object ip = ctx.getProperty(1);
        return ip == null ? null : String.valueOf(ip);
    }

    private int portOf(MetaContext ctx) {
        Object port = ctx.getProperty(2);
        if (port instanceof Integer i) return i;
        if (port == null) return -1;
        try {
            return Integer.parseInt(String.valueOf(port));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private byte[] myPeerSignKeyData() {
        if (creds == null || creds.length == 0) return null;
        return creds[0].getPublicKey().getData();
    }

    private static byte[] firstPeerSignKeyData(SignedKey[] signs) {
        if (signs == null) return null;
        for (SignedKey sk : signs) {
            if (sk != null && sk.getKey() != null) return sk.getKey().getData();
        }
        return null;
    }

    private static int compareBytes(byte[] a, byte[] b) {
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            int d = (a[i] & 0xFF) - (b[i] & 0xFF);
            if (d != 0) return d;
        }
        return Integer.compare(a.length, b.length);
    }

    private PairSymKeys deriveSessionKeys(byte[] a, byte[] b) {
        byte[] lo = compareBytes(a, b) <= 0 ? a : b;
        byte[] hi = (lo == a) ? b : a;
        byte[] digest;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            md.update(lo);
            md.update(hi);
            digest = md.digest();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
        String providerName = client.getCryptLib().name();
        CryptoProvider provider = CryptoProviderFactory.getProvider(providerName);
        AKey.Symmetric master = provider.createSymmetricKey(digest);
        return provider.deriveSymmetricKeys(master, 0, 0);
    }


    private PeerAuthorizedApi newPeerLocalApi(PeerAuthorizedApiRemote remote) {
        return new PeerAuthorizedApiLocal<PeerAuthorizedApiRemote>(remote) {
            @Override
            public void sendMessage(byte[] data) {
                if (data == null || data.length < 17) {
                    Log.warn("P2P: envelope too short", "bytes", data == null ? 0 : data.length);
                    return;
                }
                if (data[0] != 1) {
                    Log.warn("P2P: unsupported envelope version", "version", data[0]);
                    return;
                }
                java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(data, 1, 16);
                long msb = bb.getLong();
                long lsb = bb.getLong();
                UUID fromUid = new UUID(msb, lsb);
                byte[] payload = java.util.Arrays.copyOfRange(data, 17, data.length);

                if (remote != null) {
                    sessions.putIfAbsent(fromUid, remote);
                }
                try {
                    client.getMessageNode(fromUid).sendMessageFromServerToClient(payload);
                } catch (RuntimeException e) {
                    Log.warn("P2P: peer->MessageNode dispatch failed",
                            "from", fromUid, "error", String.valueOf(e));
                }
                Log.info("P2P: HANDSHAKE — RECEIVED via peer", "from", fromUid, "bytes", payload.length);
            }

            @Override
            public void toServer(java.util.UUID forUid, short backId, byte[] data) {
                // relay stage not implemented yet
            }

            @Override
            public void fromServer(short backId, byte[] data) {
                // relay stage not implemented yet
            }

            @Override
            public void setReceiveWindow(long startsInMs, long durationMs) {
            }

            @Override
            public void sentUid() {
            }

            @Override
            public void requestUid() {
            }
        };
    }


    private void openPeerSession(UUID remoteUid, String ip, int port, SignedKey[] theirPeerSigns) {
        if (remoteUid == null || ip == null || port <= 0) return;
        if (closed.get()) return;
        if (sessions.containsKey(remoteUid)) {
            Log.info("ConnectionPeer.openPeerSession: already connected", "theirUid", remoteUid);
            return;
        }
        FastMetaNet net = FastMetaNet.INSTANCE.get();
        if (net == null) {
            Log.warn("ConnectionPeer.openPeerSession: FastMetaNet not initialized");
            return;
        }
        byte[] myPeerSign = myPeerSignKeyData();
        byte[] theirPeerSign = firstPeerSignKeyData(theirPeerSigns);
        if (myPeerSign == null || theirPeerSign == null) {
            Log.warn("ConnectionPeer.openPeerSession: missing peer sign material",
                    "theirUid", remoteUid,
                    "mySign", myPeerSign == null ? "null" : "present",
                    "theirSign", theirPeerSign == null ? "null" : "present");
            return;
        }
        try {
            URI uri = URI.create("udp://" + ip + ":" + port);
            MetaContext peerCtx = net.makeClient(uri, PeerRootApi.META, this::makeLocal);
            peerCtxs.put(remoteUid, peerCtx);
            PeerRootApiRemote rootRemote = peerCtx.makeRemote(PeerRootApi.META);

            PairSymKeys pair = deriveSessionKeys(myPeerSign, theirPeerSign);
            String providerName = client.getCryptLib().name();
            CryptoProvider provider = CryptoProviderFactory.getProvider(providerName);
            CryptoEngine enc = provider.createSymmetricEngine(pair.getClientToServer());
            CryptoEngine dec = provider.createSymmetricEngine(pair.getServerToClient());
            sessionEnc.put(remoteUid, enc);
            sessionDec.put(remoteUid, dec);

            String sessionKey = "peer-session:" + remoteUid;
            AFunction<PeerAuthorizedApiRemote, PeerAuthorizedApi> factory = this::newPeerLocalApi;

            PeerAuthorizedApiRemote session = rootRemote.openEnter(factory, enc::encrypt, sessionKey);
            sessions.put(remoteUid, session);
            MetaContext sessionCtx = session.getFastMetaContext();
            if (sessionCtx != null) {
                sessionCtx.flush();
            }
            uidByIp.put(ip, remoteUid);

            Log.info("ConnectionPeer: peer session established (initiator)",
                    "theirUid", remoteUid, "endpoint", uri.toString());
        } catch (RuntimeException e) {
            Log.warn("ConnectionPeer.openPeerSession: failed",
                    "theirUid", remoteUid, "error", String.valueOf(e));
        }
    }

    private void onIncomingEnter(MetaContext ctx, PeerSessionStream stream) {
        if (stream == null) return;
        if (!(stream instanceof PeerSessionStream.In in)) {
            Log.warn("ConnectionPeer.onIncomingEnter: unexpected stream type",
                    "type", stream.getClass().getName());
            return;
        }
        String ip = ipOf(ctx);
        if (ip == null) {
            Log.warn("ConnectionPeer.onIncomingEnter: no caller ip in context");
            return;
        }
        UUID remoteUid = uidByIp.get(ip);
        SignedKey[] theirPeerSigns = peerSignsByIp.get(ip);

        byte[] myPeerSign = myPeerSignKeyData();
        byte[] theirPeerSign = firstPeerSignKeyData(theirPeerSigns);
        if (myPeerSign == null || theirPeerSign == null) {
            Log.warn("ConnectionPeer.onIncomingEnter: no peer sign material",
                    "endpoint", endpointOf(ctx), "theirUid", remoteUid);
            return;
        }
        try {
            CryptoEngine enc;
            CryptoEngine dec;
            if (remoteUid != null && sessionDec.containsKey(remoteUid)) {
                enc = sessionEnc.get(remoteUid);
                dec = sessionDec.get(remoteUid);
            } else {
                PairSymKeys pair = deriveSessionKeys(myPeerSign, theirPeerSign);
                String providerName = client.getCryptLib().name();
                CryptoProvider provider = CryptoProviderFactory.getProvider(providerName);
                enc = provider.createSymmetricEngine(pair.getServerToClient());
                dec = provider.createSymmetricEngine(pair.getClientToServer());
                if (remoteUid != null) {
                    sessionEnc.put(remoteUid, enc);
                    sessionDec.put(remoteUid, dec);
                }
            }

            Object[] keys = remoteUid != null
                    ? new Object[]{"peer-session", remoteUid}
                    : new Object[]{"peer-session", ip};
            in.keys(c -> newPeerLocalApi(c.makeRemote(PeerAuthorizedApi.META)), keys)
                    .convert(dec::decrypt)
                    .onFlushData(raw -> {
                        try {
                            ctx.makeRemote(PeerRootApi.META)
                                    .enter(PeerSessionStream.Out.send(enc.encrypt(raw)));
                        } catch (RuntimeException e) {
                            Log.warn("ConnectionPeer.onIncomingEnter: reply failed",
                                    "endpoint", endpointOf(ctx), "error", String.valueOf(e));
                        }
                    })
                    .accept();
            ctx.flush();
            Log.info("ConnectionPeer: peer session established (responder)",
                    "theirUid", remoteUid, "endpoint", endpointOf(ctx));
        } catch (RuntimeException e) {
            Log.warn("ConnectionPeer.onIncomingEnter: failed",
                    "endpoint", endpointOf(ctx), "error", String.valueOf(e));
        }
    }



    static short shortHash(UUID uid) {
        if (uid == null) {
            return 0;
        }
        return (short) (uid.getMostSignificantBits() ^ uid.getLeastSignificantBits());
    }


    /**
     * Builds the list of server signing keys that this client trusts — i.e.
     * the signing keys of every work server in its current state.
     */
    private SignedKey[] localServerKeys() {
        java.util.List<SignedKey> result = new ArrayList<>();
        for (io.aether.cloud.client.ClientState.ServerInfo info
                : client.getClientState().getServerInfoAll()) {
            io.aether.api.common.ServerDescriptor d = info.getDescriptor();
            if (d == null || d.getSigningKeys() == null) {
                continue;
            }

            for (SignedKey k : d.getSigningKeys()) {
                if (k != null) {
                    result.add(k);
                    Log.info("ConnectionPeer: localServerKey",
                            "serverId", info.getServerId(),
                            "provider", io.aether.api.CryptoUtils.of(k.getKey()).getProviderName(),
                            "keyHex", io.aether.utils.HexUtils.toHexString(k.getKey().getData()));
                }
            }

        }
        return result.toArray(new SignedKey[0]);
    }


    private SignedKey[] localPeerSigns() {
        SignedKey[] result = new SignedKey[creds.length];
        for (int i = 0; i < creds.length; i++) {
            result[i] = new io.aether.api.common.SignedKey(
                    creds[i].getPublicKey(), creds[i].getSign());
        }
        return result;
    }




    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        ScheduledExecutorService ex = probeExecutor;
        probeExecutor = null;
        if (ex != null) {
            try {
                ex.shutdownNow();
            } catch (RuntimeException ignored) {
            }
        }

        Log.info("ConnectionPeer: closing",
                "myUid", client.getUid(),
                "listenPort", listenPort,
                "broadcasts", broadcasts.size());
        for (FastMetaBroadcast<PeerRootApi> bc : broadcasts) {
            try {
                bc.destroy(false);
            } catch (RuntimeException ignored) {
            }
        }
        broadcasts.clear();
        FastMetaServer<PeerRootApi> s = server;
        if (s != null) {
            try {
                s.destroy(false);
            } catch (RuntimeException ignored) {
            }
        }
    }

}