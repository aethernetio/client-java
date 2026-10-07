
package io.aether.cloud.client;

import io.aether.api.CryptoUtils;
import io.aether.api.clienttypes.PeerIdentityCredentials;
import io.aether.api.common.SignedKey;
import io.aether.api.peerapi.PeerRootApi;
import io.aether.api.peerapi.PeerRootApiLocal;
import io.aether.api.peerapi.PeerRootApiRemote;
import io.aether.api.peerapi.PeerSessionStream;
import io.aether.crypto.AKey;
import io.aether.crypto.SignChecker;
import io.aether.logger.Log;

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



    private FastMetaServer<PeerRootApi> server;
    private int listenPort = -1;
    private final List<FastMetaBroadcast<PeerRootApi>> broadcasts = new ArrayList<>();

    private volatile int myNonce = 0;


    private final Map<UUID, PeerRootApiRemote> remotes = new ConcurrentHashMap<>();



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
                Log.info("ConnectionPeer: incoming enter is not implemented yet");
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
            // Session opening goes here (next step).
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

        // Step 1: collect every serverKey that is signed by one of our roots.
        java.util.List<SignedKey> trustedServerKeys = new ArrayList<>();
        for (SignedKey sk : serverKeys) {
            String lib = providerOf(sk);
            for (SignChecker root : roots) {
                if (!lib.equalsIgnoreCase(root.getCryptoProvider().getCryptoLibName())) {
                    continue;
                }
                if (root.checkSign(sk.getKey().getData(), CryptoUtils.of(sk.getSign()))) {
                    trustedServerKeys.add(sk);
                    break;
                }
            }
        }
        if (trustedServerKeys.isEmpty()) {
            throw new IllegalStateException("no trusted server key");
        }
        Log.info("ConnectionPeer: chain step 1/3 OK — server key trusted",
                "theirUid", remoteUid, "trustedServerKeys", trustedServerKeys.size());

        // Step 2: at least one peerSign is signed by one of the trusted server
        // keys over the payload (META_UUID(remoteUid) || META(peerSign.key)).
        // The identity issued to the peer may have been signed by a different
        // server instance than the first matching key, so try every trusted
        // server key whose lib matches the peer sign lib.
        SignedKey verifiedPeerSign = null;
        String trustedLib = null;

        for (SignedKey ps : peerSigns) {
            String psLib = providerOf(ps);
            for (SignedKey tsk : trustedServerKeys) {
                String tskLib = providerOf(tsk);
                if (!psLib.equalsIgnoreCase(tskLib)) {
                    Log.info("ConnectionPeer: step2 lib mismatch",
                            "psLib", psLib, "tskLib", tskLib);
                    continue;
                }
                SignChecker serverChecker = CryptoUtils.of(tsk.getKey())
                        .asSignPublic().toSignChecker();

                byte[] signedBytes = serverAttestationBytes(remoteUid, ps);
                io.aether.crypto.SignChecker serverCheckerForLog = CryptoUtils.of(tsk.getKey())
                        .asSignPublic().toSignChecker();
                boolean ok = serverCheckerForLog.checkSign(signedBytes, CryptoUtils.of(ps.getSign()));
                Log.info("ConnectionPeer: step2 hex",
                        "serverKeyHex", io.aether.utils.HexUtils.toHexString(tsk.getKey().getData()),
                        "peerSignKeyHex", io.aether.utils.HexUtils.toHexString(((io.aether.api.common.KeySignPublic) ps.getKey()).getData()),
                        "payloadHex", io.aether.utils.HexUtils.toHexString(signedBytes),
                        "signHex", io.aether.utils.HexUtils.toHexString(ps.getSign().getData()));

                Log.info("ConnectionPeer: step2 try",
                        "psLib", psLib, "tskLib", tskLib,
                        "signedBytesLen", signedBytes.length,
                        "signLen", ps.getSign().getData().length,
                        "ok", ok);
                if (ok) {
                    verifiedPeerSign = ps;
                    trustedLib = psLib;
                    break;
                }
            }
            if (verifiedPeerSign != null) {
                break;
            }
        }

        if (verifiedPeerSign == null) {
            throw new IllegalStateException("peer sign not signed by server");
        }
        Log.info("ConnectionPeer: chain step 2/3 OK — peer sign trusted",
                "theirUid", remoteUid, "lib", trustedLib);

        // Step 3: at least one peerKey is signed by the verified peerSign.
        SignChecker peerSignChecker = CryptoUtils.of(verifiedPeerSign.getKey())
                .asSignPublic().toSignChecker();
        SignedKey verifiedPeerKey = null;
        for (SignedKey pk : peerKeys) {
            if (!trustedLib.equalsIgnoreCase(providerOf(pk))) {
                continue;
            }
            if (peerSignChecker.checkSign(pk.getKey().getData(), CryptoUtils.of(pk.getSign()))) {
                verifiedPeerKey = pk;
                break;
            }
        }
        if (peerKeys.length > 0 && verifiedPeerKey == null) {
            throw new IllegalStateException("peer key not signed by peer sign");
        }
        Log.info("ConnectionPeer: chain step 3/3 OK — peer keys trusted",
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
        return remotes.containsKey(remoteUid);
    }

    boolean sendMessage(UUID remoteUid, byte[] data) {
        return false;
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