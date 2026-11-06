
package io.aether.cloud.client;

import dev.onvoid.webrtc.*;

import java.nio.ByteBuffer;

/**
 * Real transport backend backed by the onvoid webrtc-java JNI classes.
 * Isolated in this file so that unit tests never need to (and cannot on
 * modern JVMs) mock the native {@code RTCPeerConnection} / {@code RTCDataChannel}
 * classes.
 */
final class OnvoidTransport implements MessageNodeWebRtcPeer.PeerProvider {

    static final OnvoidTransport INSTANCE = new OnvoidTransport();

    private static volatile PeerConnectionFactory factory;

    private static PeerConnectionFactory factory() {
        PeerConnectionFactory f = factory;
        if (f == null) {
            synchronized (OnvoidTransport.class) {
                f = factory;
                if (f == null) {
                    f = new PeerConnectionFactory();
                    factory = f;
                }
            }
        }
        return f;
    }

    private OnvoidTransport() { }

    @Override
    public MessageNodeWebRtcPeer.Peer create(MessageNodeWebRtcPeer.PeerListener listener) {
        RTCConfiguration config = new RTCConfiguration();
        RTCIceServer stun = new RTCIceServer();
        stun.urls.add("stun:stun.l.google.com:19302");
        config.iceServers.add(stun);

        OnvoidPeer peer = new OnvoidPeer(listener);
        RTCPeerConnection pc = factory().createPeerConnection(config, peer.observer());
        peer.attach(pc);
        return peer;
    }
}

final class OnvoidPeer implements MessageNodeWebRtcPeer.Peer {

    private final MessageNodeWebRtcPeer.PeerListener listener;
    private volatile RTCPeerConnection pc;

    OnvoidPeer(MessageNodeWebRtcPeer.PeerListener listener) {
        this.listener = listener;
    }

    void attach(RTCPeerConnection pc) {
        this.pc = pc;
    }

    PeerConnectionObserver observer() {
        return new PeerConnectionObserver() {
            @Override
            public void onIceCandidate(RTCIceCandidate c) {
                if (c == null) return;
                listener.onIceCandidate(new OnvoidIce(c));
            }

            @Override
            public void onDataChannel(RTCDataChannel ch) {
                if (ch == null) return;
                OnvoidChannel c = new OnvoidChannel(ch);
                c.install();
                listener.onDataChannel(c);
            }

            @Override
            public void onConnectionChange(RTCPeerConnectionState state) {
                if (state == RTCPeerConnectionState.FAILED
                        || state == RTCPeerConnectionState.CLOSED) {
                    listener.onConnectionFailed();
                }
            }

            @Override
            public void onIceGatheringChange(RTCIceGatheringState state) {
                if (state == RTCIceGatheringState.COMPLETE) listener.onGatheringComplete();
            }
        };
    }

    @Override
    public void createOffer(MessageNodeWebRtcPeer.SdpCallback cb) {
        pc.createOffer(new RTCOfferOptions(), new CreateSessionDescriptionObserver() {
            @Override
            public void onSuccess(RTCSessionDescription description) {
                cb.onSuccess(new OnvoidSdp(description));
            }

            @Override
            public void onFailure(String error) {
                cb.onFailure(error);
            }
        });
    }

    @Override
    public void createAnswer(MessageNodeWebRtcPeer.SdpCallback cb) {
        pc.createAnswer(new RTCAnswerOptions(), new CreateSessionDescriptionObserver() {
            @Override
            public void onSuccess(RTCSessionDescription description) {
                cb.onSuccess(new OnvoidSdp(description));
            }

            @Override
            public void onFailure(String error) {
                cb.onFailure(error);
            }
        });
    }

    @Override
    public void setLocal(MessageNodeWebRtcPeer.Sdp description, MessageNodeWebRtcPeer.DoneCallback cb) {
        pc.setLocalDescription(toOnvoid(description), new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                cb.onSuccess();
            }

            @Override
            public void onFailure(String error) {
                cb.onFailure(error);
            }
        });
    }

    @Override
    public void setRemote(MessageNodeWebRtcPeer.Sdp description, MessageNodeWebRtcPeer.DoneCallback cb) {
        pc.setRemoteDescription(toOnvoid(description), new SetSessionDescriptionObserver() {
            @Override
            public void onSuccess() {
                cb.onSuccess();
            }

            @Override
            public void onFailure(String error) {
                cb.onFailure(error);
            }
        });
    }

    @Override
    public void addIceCandidate(MessageNodeWebRtcPeer.IceConfig candidate) {
        pc.addIceCandidate(new RTCIceCandidate(
                candidate.sdpMid(), candidate.sdpMLineIndex(), candidate.candidate()));
    }

    @Override
    public MessageNodeWebRtcPeer.Sdp getLocal() {
        RTCSessionDescription d = pc.getLocalDescription();
        return d == null ? null : new OnvoidSdp(d);
    }

    @Override
    public boolean gatheringComplete() {
        return pc.getIceGatheringState() == RTCIceGatheringState.COMPLETE;
    }

    @Override
    public MessageNodeWebRtcPeer.Channel createDataChannel(String label) {
        RTCDataChannel ch = pc.createDataChannel(label, new RTCDataChannelInit());
        OnvoidChannel channel = new OnvoidChannel(ch);
        channel.install();
        return channel;
    }

    @Override
    public void close() {
        try {
            RTCPeerConnection p = pc;
            if (p != null) p.close();
        } catch (Exception ignored) {
        }
    }

    private static RTCSessionDescription toOnvoid(MessageNodeWebRtcPeer.Sdp description) {
        RTCSdpType type = "offer".equals(description.type()) ? RTCSdpType.OFFER : RTCSdpType.ANSWER;
        return new RTCSessionDescription(type, description.value());
    }
}

final class OnvoidChannel implements MessageNodeWebRtcPeer.Channel {

    private final RTCDataChannel ch;
    private volatile MessageNodeWebRtcPeer.ChannelListener listener;

    OnvoidChannel(RTCDataChannel ch) {
        this.ch = ch;
    }

    void install() {
        ch.registerObserver(new RTCDataChannelObserver() {
            @Override
            public void onBufferedAmountChange(long previousAmount) {
            }

            @Override
            public void onStateChange() {
                MessageNodeWebRtcPeer.ChannelListener l = listener;
                if (l == null) return;
                RTCDataChannelState state = ch.getState();
                if (state == RTCDataChannelState.OPEN) {
                    l.onOpen();
                } else if (state == RTCDataChannelState.CLOSED || state == RTCDataChannelState.CLOSING) {
                    l.onClosed();
                }
            }

            @Override
            public void onMessage(RTCDataChannelBuffer buffer) {
                MessageNodeWebRtcPeer.ChannelListener l = listener;
                if (l == null) return;
                ByteBuffer bb = buffer.data;
                byte[] copy = new byte[bb.remaining()];
                bb.get(copy);
                l.onMessage(copy);
            }
        });
    }

    @Override
    public boolean isOpen() {
        return ch.getState() == RTCDataChannelState.OPEN;
    }

    @Override
    public void send(byte[] data) throws Exception {
        ByteBuffer buf = ByteBuffer.allocateDirect(data.length);
        buf.put(data);
        buf.flip();
        ch.send(new RTCDataChannelBuffer(buf, true));
    }

    @Override
    public void close() {
        try {
            ch.close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void setListener(MessageNodeWebRtcPeer.ChannelListener listener) {
        this.listener = listener;
    }
}

final class OnvoidSdp implements MessageNodeWebRtcPeer.Sdp {

    private final String type;
    private final String value;

    OnvoidSdp(RTCSessionDescription description) {
        if (description.sdpType == RTCSdpType.OFFER) this.type = "offer";
        else if (description.sdpType == RTCSdpType.ANSWER) this.type = "answer";
        else this.type = "unknown";
        this.value = description.sdp;
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

final class OnvoidIce implements MessageNodeWebRtcPeer.IceConfig {

    private final String candidate;
    private final String sdpMid;
    private final int sdpMLineIndex;

    OnvoidIce(RTCIceCandidate candidate) {
        this.candidate = candidate.sdp;
        this.sdpMid = candidate.sdpMid;
        this.sdpMLineIndex = candidate.sdpMLineIndex;
    }

    @Override
    public String candidate() {
        return candidate;
    }

    @Override
    public String sdpMid() {
        return sdpMid;
    }

    @Override
    public int sdpMLineIndex() {
        return sdpMLineIndex;
    }
}
