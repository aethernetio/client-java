package io.aether.cloud.client;

import io.aether.common.Cloud;
import io.aether.common.ServerDescriptor;
import io.aether.logger.Log;
import io.aether.utils.AString;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.slots.AMFuture;
import io.aether.utils.slots.EventConsumer;
import io.aether.utils.streams.*;

import java.util.Set;
import java.util.UUID;

public class MessageNode implements NodeDown<byte[], byte[]> {
    public final EventConsumer<Throwable> onError = new EventConsumer<>();
    private final AetherCloudClient client;
    private final UUID consumer;
    private final AMFuture<Cloud> consumerCloud;
    private final Set<ConnectionWork> connectionsOut = new ConcurrentHashSet<>();
    private final Set<ConnectionWork> connectionsIn = new ConcurrentHashSet<>();
    private final BufferNode<byte[], byte[]> buffer = new BufferNode<byte[], byte[]>() {

        @Override
        protected BufferNode<byte[], byte[]>.BGateUp initUp() {
            return new BGateUp() {
            };
        }

        @Override
        public String toString() {
            return "MessageNode(input buffer)";
        }

    };
    private volatile MessageEventListener strategy;

    public MessageNode(AetherCloudClient client, UUID consumer, MessageEventListener strategy) {
        Log.trace("open message node ($client) from $uidFrom to $uidTo", "client", client.getName(), "uidTo", consumer, "uidFrom", client.getUid());
        this.client = client;
        this.strategy = strategy;
        this.consumer = consumer;
        consumerCloud = client.getCloud(consumer);
        consumerCloud.add(c ->{
            this.strategy.setConsumerCloud(this, c);
        });
        buffer.down().link(FGate.of(new AcceptorI<byte[], byte[]>() {

            @Override
            public void toString(AString sb) {
                sb.add("MessageNode(input buffer acceptor)");
            }

            @Override
            public String toString() {
                return "MessageNode(input buffer acceptor)";
            }

            @Override
            public void send(FGate<byte[], byte[]> fGate, Value<byte[]> value) {
                if (connectionsOut.isEmpty()) {
                    value.abort(MessageNode.this);
                    return;
                }
                if (value.isData()) {
                    for (var e : connectionsOut) {
                        if (value.isForce()) {
                            e.safeApiCon.getRemoteApi().run_flush(a -> a.sendMessage(consumer, value));
                        } else {
                            e.safeApiCon.getRemoteApi().run(a -> a.sendMessage(consumer, value));
                        }
                        return;
                    }
                }
                if (value.isRequestData()) {
                    for (var e : connectionsIn) {
                        e.ready.toOnce((c) -> {
                            c.safeApiCon.getRemoteApi().run(a -> a.ping(client.getPingTime()));
                        });
                    }
                }
            }

        }).outSide());
    }

    @Override
    public void toString(AString sb) {
        sb.add("MessageNode(").add(consumer).add(")");
    }

    @Override
    public String toString() {
        return toString2();
    }

    public MessageEventListener getStrategy() {
        return strategy;
    }

    public void setStrategy(MessageEventListener strategy) {
        this.strategy = strategy;
    }

    @Override
    public FGate<byte[], byte[]> gUp() {
        return buffer.gUp();
    }

    public UUID getConsumerUUID() {
        return consumer;
    }

    public void addConsumerServerOut(int id) {
        client.getServer(id).once(s -> strategy.onResolveConsumerServer(this, s));
    }

    public void addConsumerServerOut(ServerDescriptor serverDescriptor) {
        strategy.onResolveConsumerConnection(this, client.getConnection(serverDescriptor));
    }

    public void addConsumerConnectionOut(ConnectionWork connectionWork) {
        if (connectionsOut.add(connectionWork)) {
            buffer.down().send(Value.ofRequest());
        }
    }

    public void addConnectionIn(ConnectionWork connectionWork, Gate<byte[], byte[]> gate) {
        connectionsIn.add(connectionWork);
    }

    public void sendMessageFromServerToClient(Value<byte[]> data) {
        buffer.down().send(data);
    }
}
