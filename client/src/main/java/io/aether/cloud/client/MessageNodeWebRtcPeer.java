
package io.aether.cloud.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.aether.api.clientserverapi.IceCandidate;
import io.aether.api.clientserverapi.WebRtcSession;
import io.aether.logger.Log;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.Destroyable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * WebRTC transport for {@link MessageNode}, ported 1:1 from the TypeScript
 * implementation ({@code aether_fastmeta_webrtc.ts}).
 *
 * <p>The initiator / answerer roles are chosen by lexicographic comparison of
 * the two uids. The peer is fully transparent to the caller: once the data
 * channel opens, {@link #send(byte[])} delivers bytes directly, and every
 * incoming frame is forwarded to {@code onData}.
 *
 * <p>Native {@code dev.onvoid.webrtc} types are hidden behind the small
 * {@link Peer} / {@link Channel} abstraction so that the negotiation logic can
 * be exercised in tests without a native WebRTC runtime.
 */
public class MessageNodeWebRtcPeer implements Destroyable {

    static final long RETRY_DELAY_MS = 2000;
    static final long REQUEST_TIMEOUT_MS = 6000;
    static final long ICE_GATHER_TIMEOUT_MS = 3000;
    static final long CHANNEL_OPEN_TIMEOUT_MS = 15000;
    static final String SESSION_ID_FIELD = "aetherSessionId";

    private static final Pattern OFFER_SETUP =
            Pattern.compile("(?:^|\\r?\\n)a=setup:actpass(?:\\r?\\n|$)");
    private static final Pattern ANSWER_SETUP =
            Pattern.compile("(?:^|\\r?\\n)a=setup:(?:active|passive)(?:\\r?\\n|$)");
    private static final ObjectMapper MAPPER = new ObjectMapper();


    /**
     * Global kill-switch. Tests that assert the ordinary cloud delivery path
     * set this flag so the WebRTC transport does not take precedence over the
     * cloud route. Production code never sets it.
     */
    public static final String DISABLE_PROPERTY = "aether.webrtc.disabled";


    // ------------------------------------------------------------------
    // Signaling seam
    // ------------------------------------------------------------------

    /** Cloud signaling callbacks used by the negotiation. */
    public interface WebRtcSignaling {
        AFuture publish(WebRtcSession session);

        ARFuture<WebRtcSession> request(UUID uid);
    }

    // ------------------------------------------------------------------
    // Transport seam (implemented by OnvoidTransport in production,
    // by in-memory fakes in tests).
    // ------------------------------------------------------------------

    /** Immutable SDP payload; {@code type()} is "offer" or "answer". */
    interface Sdp {
        String type();

        String value();
    }

    /** Plain ICE candidate description, decoupled from the native type. */
    interface IceConfig {
        String candidate();

        String sdpMid();

        int sdpMLineIndex();
    }

    /** Callback for asynchronous SDP construction. */
    interface SdpCallback {
        void onSuccess(Sdp description);

        void onFailure(String error);
    }

    /** Callback for asynchronous SDP application. */
    interface DoneCallback {
        void onSuccess();

        void onFailure(String error);
    }

    /** Events emitted by a {@link Peer}. */
    interface PeerListener {
        void onIceCandidate(IceConfig candidate);

        void onDataChannel(Channel channel);

        void onConnectionFailed();

        void onGatheringComplete();
    }

    /** Events emitted by a {@link Channel}. */
    interface ChannelListener {
        void onOpen();

        void onMessage(byte[] data);

        void onClosed();
    }

    /** A data channel that can carry opaque bytes. */
    interface Channel {
        boolean isOpen();

        void send(byte[] data) throws Exception;

        void close();

        void setListener(ChannelListener listener);
    }

    /** A peer connection capable of SDP negotiation and ICE. */
    interface Peer {
        void createOffer(SdpCallback callback);

        void createAnswer(SdpCallback callback);

        void setLocal(Sdp description, DoneCallback callback);

        void setRemote(Sdp description, DoneCallback callback);

        void addIceCandidate(IceConfig candidate);

        Sdp getLocal();

        boolean gatheringComplete();

        Channel createDataChannel(String label);

        void close();
    }

    /** Factory for {@link Peer} instances. */
    interface PeerProvider {
        Peer create(PeerListener listener);
    }

    /**
     * Test seam. When non-null, {@link #isSupported()} reports {@code true}
     * and all peer connections are built by this provider instead of onvoid.
     */
    static volatile PeerProvider peerProvider;

    public static boolean isSupported() {

        if (Boolean.getBoolean(DISABLE_PROPERTY)) return false;

        if (peerProvider != null) return true;
        try {
            Class.forName("dev.onvoid.webrtc.PeerConnectionFactory");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    private final UUID localUid;
    private final UUID remoteUid;
    private final WebRtcSignaling signaling;
    private final Consumer<byte[]> onData;
    private final Runnable onOpen;

    final boolean initiator;
    volatile Peer peerConnection;
    volatile Channel dataChannel;
    volatile ScheduledFuture<?> retryTimer;
    volatile ScheduledFuture<?> openTimer;
    volatile long generation;
    volatile boolean destroyed;
    volatile boolean started;
    volatile CompletableFuture<Void> iceGatheringFuture;
    final List<IceCandidate> localCandidates = new ArrayList<>();

    public MessageNodeWebRtcPeer(
            UUID localUid,
            UUID remoteUid,
            WebRtcSignaling signaling,
            Consumer<byte[]> onData,
            Runnable onOpen
    ) {
        this.localUid = localUid;
        this.remoteUid = remoteUid;
        this.signaling = signaling;
        this.onData = onData;
        this.onOpen = onOpen;
        this.initiator = localUid.toString().compareTo(remoteUid.toString()) < 0;
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    public void start() {
        if (destroyed || started || !isSupported()) return;
        started = true;
        connect(++generation);
    }

    public boolean isOpen() {
        Channel channel = dataChannel;
        return channel != null && channel.isOpen();
    }

    public boolean send(byte[] data) {
        Channel channel = dataChannel;
        if (channel == null || !channel.isOpen()) return false;
        try {
            channel.send(data);
            return true;
        } catch (Exception e) {
            Log.warn("WebRTC direct message send failed",
                    "uidTo", remoteUid, "error", String.valueOf(e));
            scheduleReconnect();
            return false;
        }
    }

    @Override
    public AFuture destroy(boolean force) {
        destroyed = true;
        generation++;
        clearRetry();
        clearOpenTimeout();
        closeConnection();
        return AFuture.completed();
    }

    // ------------------------------------------------------------------
    // Negotiation
    // ------------------------------------------------------------------

    private void connect(long gen) {
        try {
            closeConnection();
            Peer peer = createPeer(gen);
            if (!isCurrent(gen)) {
                try {
                    peer.close();
                } catch (Exception ignored) {
                }
                return;
            }
            this.peerConnection = peer;

            CompletableFuture<Void> negotiation;
            if (initiator) {
                Channel channel = peer.createDataChannel("message-node");
                attachDataChannel(channel, gen);
                String sessionId = createSessionId();
                negotiation = createAndPublishOffer(peer, gen, sessionId)
                        .thenCompose(v -> {
                            checkCurrent(gen);
                            return waitForRemoteAnswer(peer, gen, sessionId);
                        });
            } else {
                negotiation = waitForRemoteOffer(peer, gen);
            }

            negotiation.whenComplete((v, error) -> {
                if (error != null) {
                    if (isCurrent(gen)) {
                        Log.warn("WebRTC MessageNode connection failed",
                                "uidFrom", localUid, "uidTo", remoteUid,
                                "error", String.valueOf(error));
                        scheduleReconnect();
                    }
                    return;
                }
                if (isCurrent(gen)) scheduleOpenTimeout(gen);
            });
        } catch (Exception e) {
            if (isCurrent(gen)) {
                Log.warn("WebRTC MessageNode connect failed",
                        "uidFrom", localUid, "uidTo", remoteUid,
                        "error", String.valueOf(e));
                scheduleReconnect();
            }
        }
    }

    private Peer createPeer(long gen) {
        CompletableFuture<Void> gathering = new CompletableFuture<>();
        this.iceGatheringFuture = gathering;
        synchronized (localCandidates) {
            localCandidates.clear();
        }

        PeerListener listener = new PeerListener() {
            @Override
            public void onIceCandidate(IceConfig candidate) {
                if (!isCurrent(gen) || candidate == null) return;
                try {
                    Map<String, Object> json = new LinkedHashMap<>();
                    json.put("candidate", candidate.candidate());
                    json.put("sdpMid", candidate.sdpMid());
                    json.put("sdpMLineIndex", candidate.sdpMLineIndex());
                    synchronized (localCandidates) {
                        localCandidates.add(new IceCandidate(MAPPER.writeValueAsBytes(json)));
                    }
                } catch (Exception e) {
                    Log.warn("WebRTC ICE candidate encoding failed",
                            "error", String.valueOf(e));
                }
            }

            @Override
            public void onDataChannel(Channel channel) {
                if (isCurrent(gen) && channel != null) attachDataChannel(channel, gen);
            }

            @Override
            public void onConnectionFailed() {
                if (isCurrent(gen)) scheduleReconnect();
            }

            @Override
            public void onGatheringComplete() {
                gathering.complete(null);
            }
        };

        PeerProvider provider = peerProvider;
        if (provider == null) provider = OnvoidTransport.INSTANCE;
        return provider.create(listener);
    }

    private void attachDataChannel(Channel channel, long gen) {
        this.dataChannel = channel;
        channel.setListener(new ChannelListener() {
            @Override
            public void onOpen() {
                if (!isCurrent(gen)) return;
                clearOpenTimeout();
                Log.debug("WebRTC MessageNode channel opened", "uidTo", remoteUid);
                onOpen.run();
            }

            @Override
            public void onMessage(byte[] data) {
                if (!isCurrent(gen)) return;
                try {
                    onData.accept(data);
                } catch (Exception e) {
                    Log.warn("WebRTC data receive failed", "error", String.valueOf(e));
                }
            }

            @Override
            public void onClosed() {
                if (isCurrent(gen)) scheduleReconnect();
            }
        });
    }

    private CompletableFuture<Void> createAndPublishOffer(Peer peer, long gen, String sessionId) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        peer.createOffer(new SdpCallback() {
            @Override
            public void onSuccess(Sdp offer) {
                try {
                    checkCurrent(gen);
                } catch (Exception e) {
                    result.completeExceptionally(e);
                    return;
                }
                peer.setLocal(offer, new DoneCallback() {
                    @Override
                    public void onSuccess() {
                        waitForIceGathering(peer, gen)
                                .thenCompose(x -> publishLocalSession(peer, gen, sessionId))
                                .whenComplete((x, e) -> {
                                    if (e != null) result.completeExceptionally(e);
                                    else result.complete(null);
                                });
                    }

                    @Override
                    public void onFailure(String error) {
                        result.completeExceptionally(new IllegalStateException(error));
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                result.completeExceptionally(new IllegalStateException(error));
            }
        });
        return result;
    }

    private CompletableFuture<Void> createAndPublishAnswer(Peer peer, long gen, String sessionId) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        peer.createAnswer(new SdpCallback() {
            @Override
            public void onSuccess(Sdp answer) {
                try {
                    checkCurrent(gen);
                } catch (Exception e) {
                    result.completeExceptionally(e);
                    return;
                }
                peer.setLocal(answer, new DoneCallback() {
                    @Override
                    public void onSuccess() {
                        waitForIceGathering(peer, gen)
                                .thenCompose(x -> publishLocalSession(peer, gen, sessionId))
                                .whenComplete((x, e) -> {
                                    if (e != null) result.completeExceptionally(e);
                                    else result.complete(null);
                                });
                    }

                    @Override
                    public void onFailure(String error) {
                        result.completeExceptionally(new IllegalStateException(error));
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                result.completeExceptionally(new IllegalStateException(error));
            }
        });
        return result;
    }

    private CompletableFuture<Void> waitForRemoteAnswer(Peer peer, long gen, String sessionId) {
        return requestRemoteSession(gen, "answer", sessionId).thenCompose(session -> {
            try {
                checkCurrent(gen);
            } catch (Exception e) {
                return failed(e);
            }
            CompletableFuture<Void> result = new CompletableFuture<>();
            ApplySdp answer = new ApplySdp("answer", sdpText(session));
            peer.setRemote(answer, new DoneCallback() {
                @Override
                public void onSuccess() {
                    addRemoteCandidates(peer, session).whenComplete((v, e) -> {
                        if (e != null) result.completeExceptionally(e);
                        else result.complete(null);
                    });
                }

                @Override
                public void onFailure(String error) {
                    result.completeExceptionally(new IllegalStateException(error));
                }
            });
            return result;
        });
    }

    private CompletableFuture<Void> waitForRemoteOffer(Peer peer, long gen) {
        return requestRemoteSession(gen, "offer", null).thenCompose(session -> {
            try {
                checkCurrent(gen);
            } catch (Exception e) {
                return failed(e);
            }
            String sessionId = getSessionId(session);
            if (sessionId == null) {
                return failed(new IllegalStateException("WebRTC offer has no negotiation id"));
            }
            CompletableFuture<Void> result = new CompletableFuture<>();
            ApplySdp offer = new ApplySdp("offer", sdpText(session));
            peer.setRemote(offer, new DoneCallback() {
                @Override
                public void onSuccess() {
                    addRemoteCandidates(peer, session).whenComplete((v, e) -> {
                        if (e != null) {
                            result.completeExceptionally(e);
                            return;
                        }
                        createAndPublishAnswer(peer, gen, sessionId).whenComplete((v2, e2) -> {
                            if (e2 != null) result.completeExceptionally(e2);
                            else result.complete(null);
                        });
                    });
                }

                @Override
                public void onFailure(String error) {
                    result.completeExceptionally(new IllegalStateException(error));
                }
            });
            return result;
        });
    }

    private CompletableFuture<WebRtcSession> requestRemoteSession(
            long gen, String expectedType, String expectedSessionId) {
        CompletableFuture<WebRtcSession> result = new CompletableFuture<>();
        attemptRequest(gen, expectedType, expectedSessionId, result);
        return result;
    }

    private void attemptRequest(
            long gen, String expectedType, String expectedSessionId,
            CompletableFuture<WebRtcSession> result) {
        if (result.isDone()) return;
        if (!isCurrent(gen)) {
            result.completeExceptionally(new IllegalStateException("WebRTC negotiation canceled"));
            return;
        }

        AtomicBoolean handled = new AtomicBoolean(false);
        ARFuture<WebRtcSession> request;
        try {
            request = signaling.request(remoteUid);
        } catch (Exception e) {
            request = null;
        }
        if (request == null) {
            scheduleRetry(gen, expectedType, expectedSessionId, result);
            return;
        }

        RU.schedule(REQUEST_TIMEOUT_MS, () -> {
            if (!handled.compareAndSet(false, true)) return;
            scheduleRetry(gen, expectedType, expectedSessionId, result);
        });

        request.to(session -> {
            if (!handled.compareAndSet(false, true)) return;
            if (matchesDescriptionType(session, expectedType, expectedSessionId)) {
                if (isCurrent(gen)) result.complete(session);
                else result.completeExceptionally(new IllegalStateException("WebRTC negotiation canceled"));
            } else {
                scheduleRetry(gen, expectedType, expectedSessionId, result);
            }
        }, error -> {
            if (!handled.compareAndSet(false, true)) return;
            scheduleRetry(gen, expectedType, expectedSessionId, result);
        });
    }

    private void scheduleRetry(
            long gen, String expectedType, String expectedSessionId,
            CompletableFuture<WebRtcSession> result) {
        RU.schedule(RETRY_DELAY_MS, () -> {
            if (result.isDone()) return;
            if (!isCurrent(gen)) {
                result.completeExceptionally(new IllegalStateException("WebRTC negotiation canceled"));
                return;
            }
            attemptRequest(gen, expectedType, expectedSessionId, result);
        });
    }

    CompletableFuture<Void> publishLocalSession(Peer peer, long gen, String sessionId) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            checkCurrent(gen);
            Sdp local = peer.getLocal();
            if (local == null || local.value() == null) {
                result.completeExceptionally(
                        new IllegalStateException("Local WebRTC description is missing"));
                return result;
            }
            IceCandidate[] candidates = extractCandidates(sessionId);
            WebRtcSession session = new WebRtcSession(
                    local.value().getBytes(StandardCharsets.UTF_8), candidates);

            AFuture published = signaling.publish(session);
            if (published == null) {
                result.completeExceptionally(new IllegalStateException("publish() returned null"));
                return result;
            }
            AtomicBoolean finished = new AtomicBoolean(false);
            published.to(() -> {
                if (!finished.compareAndSet(false, true)) return;
                try {
                    checkCurrent(gen);
                    result.complete(null);
                } catch (Exception e) {
                    result.completeExceptionally(e);
                }
            });
            published.onError(error -> {
                if (!finished.compareAndSet(false, true)) return;
                result.completeExceptionally(new IllegalStateException(String.valueOf(error)));
            });
            RU.schedule(REQUEST_TIMEOUT_MS, () -> {
                if (!finished.compareAndSet(false, true)) return;
                result.completeExceptionally(new TimeoutException("publish timeout"));
            });
        } catch (Exception e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    private IceCandidate[] extractCandidates(String sessionId) {
        List<IceCandidate> result = new ArrayList<>();
        try {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put(SESSION_ID_FIELD, sessionId);
            result.add(new IceCandidate(MAPPER.writeValueAsBytes(json)));
        } catch (Exception ignored) {
        }
        synchronized (localCandidates) {
            result.addAll(localCandidates);
        }
        return result.toArray(new IceCandidate[0]);
    }

    private String getSessionId(WebRtcSession session) {
        for (IceCandidate value : session.getCandidates()) {
            try {
                Object parsed = MAPPER.readValue(value.getData(), Object.class);
                if (!(parsed instanceof Map)) continue;
                Object sessionId = ((Map<?, ?>) parsed).get(SESSION_ID_FIELD);
                if (sessionId instanceof String && !((String) sessionId).isEmpty()) {
                    return (String) sessionId;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    boolean matchesDescriptionType(WebRtcSession session, String expectedType, String expectedSessionId) {
        if (session == null) return false;
        byte[] sdpBytes = session.getSdp();
        if (sdpBytes == null || sdpBytes.length == 0) return false;
        String sessionId = getSessionId(session);
        if (sessionId == null) return false;
        if (expectedSessionId != null && !expectedSessionId.isEmpty()
                && !expectedSessionId.equals(sessionId)) {
            return false;
        }
        String sdp = new String(sdpBytes, StandardCharsets.UTF_8);
        if ("offer".equals(expectedType)) return OFFER_SETUP.matcher(sdp).find();
        if ("answer".equals(expectedType)) return ANSWER_SETUP.matcher(sdp).find();
        return true;
    }

    private CompletableFuture<Void> addRemoteCandidates(Peer peer, WebRtcSession session) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            for (IceCandidate value : session.getCandidates()) {
                try {
                    Object parsed = MAPPER.readValue(value.getData(), Object.class);
                    if (!(parsed instanceof Map)) continue;
                    Map<?, ?> map = (Map<?, ?>) parsed;
                    if (map.get(SESSION_ID_FIELD) instanceof String) continue;
                    Object candidate = map.get("candidate");
                    if (candidate == null) continue;
                    Object mid = map.get("sdpMid");
                    Object lineIndex = map.get("sdpMLineIndex");
                    int index = lineIndex instanceof Number ? ((Number) lineIndex).intValue() : 0;
                    final String sdpMid = mid == null ? null : mid.toString();
                    final String candidateText = candidate.toString();
                    peer.addIceCandidate(new IceConfig() {
                        @Override
                        public String candidate() {
                            return candidateText;
                        }

                        @Override
                        public String sdpMid() {
                            return sdpMid;
                        }

                        @Override
                        public int sdpMLineIndex() {
                            return index;
                        }
                    });
                } catch (Exception e) {
                    Log.warn("Ignoring invalid WebRTC ICE candidate", "error", String.valueOf(e));
                }
            }
            result.complete(null);
        } catch (Exception e) {
            result.completeExceptionally(e);
        }
        return result;
    }

    private CompletableFuture<Void> waitForIceGathering(Peer peer, long gen) {
        if (peer.gatheringComplete()) return CompletableFuture.completedFuture(null);
        CompletableFuture<Void> observed = this.iceGatheringFuture;
        if (observed == null) return CompletableFuture.completedFuture(null);

        CompletableFuture<Void> result = new CompletableFuture<>();
        AtomicBoolean done = new AtomicBoolean(false);
        observed.whenComplete((v, e) -> {
            if (!done.compareAndSet(false, true)) return;
            if (!isCurrent(gen)) {
                result.completeExceptionally(new IllegalStateException("WebRTC negotiation canceled"));
            } else {
                result.complete(null);
            }
        });
        RU.schedule(ICE_GATHER_TIMEOUT_MS, () -> {
            if (!done.compareAndSet(false, true)) return;
            if (!isCurrent(gen)) {
                result.completeExceptionally(new IllegalStateException("WebRTC negotiation canceled"));
            } else {
                result.complete(null);
            }
        });
        return result;
    }

    // ------------------------------------------------------------------
    // Timers / lifecycle
    // ------------------------------------------------------------------

    private synchronized void scheduleReconnect() {
        if (destroyed || retryTimer != null) return;
        generation++;
        clearOpenTimeout();
        closeConnection();
        retryTimer = RU.schedule(RETRY_DELAY_MS, () -> {
            retryTimer = null;
            if (!destroyed) connect(++generation);
        });
    }

    private void scheduleOpenTimeout(long gen) {
        clearOpenTimeout();
        openTimer = RU.schedule(CHANNEL_OPEN_TIMEOUT_MS, () -> {
            openTimer = null;
            if (isCurrent(gen) && !isOpen()) {
                Log.warn("WebRTC MessageNode channel open timed out",
                        "uidFrom", localUid, "uidTo", remoteUid);
                scheduleReconnect();
            }
        });
    }

    private void clearOpenTimeout() {
        ScheduledFuture<?> timer = openTimer;
        if (timer == null) return;
        timer.cancel(false);
        openTimer = null;
    }

    private void clearRetry() {
        ScheduledFuture<?> timer = retryTimer;
        if (timer == null) return;
        timer.cancel(false);
        retryTimer = null;
    }

    private void closeConnection() {
        Channel channel = this.dataChannel;
        Peer peer = this.peerConnection;
        this.dataChannel = null;
        this.peerConnection = null;
        this.iceGatheringFuture = null;
        synchronized (localCandidates) {
            localCandidates.clear();
        }
        if (channel != null) {
            try {
                channel.setListener(nullListener());
                channel.close();
            } catch (Exception ignored) {
            }
        }
        if (peer != null) {
            try {
                peer.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static ChannelListener nullListener() {
        return new ChannelListener() {
            @Override
            public void onOpen() {
            }

            @Override
            public void onMessage(byte[] data) {
            }

            @Override
            public void onClosed() {
            }
        };
    }

    private void checkCurrent(long gen) {
        if (!isCurrent(gen)) throw new IllegalStateException("WebRTC negotiation canceled");
    }

    private boolean isCurrent(long gen) {
        return !destroyed && gen == generation;
    }

    private String createSessionId() {
        return UUID.randomUUID().toString();
    }

    private static String sdpText(WebRtcSession session) {
        return new String(session.getSdp(), StandardCharsets.UTF_8);
    }

    private static <T> CompletableFuture<T> failed(Throwable error) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(error);
        return future;
    }

    /** Simple {@link Sdp} used when applying a remote description. */
    private static final class ApplySdp implements Sdp {
        private final String type;
        private final String value;

        ApplySdp(String type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public String value() {
            return value;
        }
    }
}