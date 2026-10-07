
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
    private final PeerIdentityCredentials creds;
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


    ConnectionPeer(AetherCloudClient client, PeerIdentityCredentials creds) {
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



        try {
            verifyChain(serverKeys, peerSigns, peerKeys);
        } catch (RuntimeException e) {
            Log.warn("ConnectionPeer: hello chain verification failed", e);
            return;
        }

        Log.info("ConnectionPeer: hello chain verified",
                "theirHash", uidHash);
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
        Log.info("ConnectionPeer: hello routing decision",
                "theirHash", uidHash,
                "theyWantUs", theyWantUs,
                "weWantThem", wantedHashes.containsKey(uidHash));

    }

    private void verifyChain(
            SignedKey[] serverKeys,
            SignedKey[] peerSigns,
            SignedKey[] peerKeys
    ) {
        if (serverKeys == null || peerSigns == null || peerKeys == null) {
            throw new IllegalArgumentException("null chain component");
        }
        Iterable<SignChecker> roots = client.getClientState().getRootSigners();
        boolean serverOk = false;
        for (SignedKey sk : serverKeys) {
            for (SignChecker root : roots) {
                if (root.checkSign(sk.getKey().getData(),
                        CryptoUtils.of(sk.getSign()))) {
                    serverOk = true;
                    break;
                }
            }
            if (serverOk) {
                break;
            }
        }
        if (!serverOk) {
            throw new IllegalStateException("no trusted server key");
        }
        for (SignedKey ps : peerSigns) {
            boolean ok = false;
            for (SignedKey sk : serverKeys) {
                AKey.SignPublic serverPub;
                try {
                    serverPub = CryptoUtils.of(sk.getKey()).asSignPublic();
                } catch (RuntimeException e) {
                    continue;
                }
                SignChecker checker = serverPub.toSignChecker();
                if (checker.checkSign(ps.getKey().getData(),
                        CryptoUtils.of(ps.getSign()))) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw new IllegalStateException("peer sign not signed by server");
            }
        }
        for (SignedKey pk : peerKeys) {
            boolean ok = false;
            for (SignedKey ps : peerSigns) {
                AKey.SignPublic peerSignPub;
                try {
                    peerSignPub = CryptoUtils.of(ps.getKey()).asSignPublic();
                } catch (RuntimeException e) {
                    continue;
                }
                SignChecker checker = peerSignPub.toSignChecker();
                if (checker.checkSign(pk.getKey().getData(),
                        CryptoUtils.of(pk.getSign()))) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw new IllegalStateException("peer key not signed by peer sign");
            }
        }
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
        SignedKey[] peerSigns = new SignedKey[]{
                new io.aether.api.common.SignedKey(creds.getPublicKey(), creds.getSign())
        };
        SignedKey[] peerKeys = new SignedKey[0];
        SignedKey[] serverKeys = localServerKeys();

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
                }
            }
        }
        return result.toArray(new SignedKey[0]);
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