package io.aether.cloud.client;

import io.aether.common.Cloud;
import io.aether.common.ServerDescriptor;
import io.aether.utils.AString;
import io.aether.utils.CType;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.flow.Flow;
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
    private final BufferNode<byte[], byte[]> buffer = new BufferNode<byte[],byte[]>() {
        @Override
        public Object findByOwner(Class<?> t) {
            return t.isInstance(this) ? this : null;
        }

        @Override
        protected BufferNode<byte[], byte[]>.BGateUp initUp() {
            return new BGateUp() {
            };
        }

        @Override
        public String toString() {
            return "MessageNode(input buffer)";
        }

        @Override
        public <T> T findByOwner(CType<T> t) {
            return t.isInstance(this) ? t.cast(this) : null;
        }
    };
    private volatile MessageEventListener strategy;

    public MessageNode(AetherCloudClient client, UUID consumer, MessageEventListener strategy) {
        this.client = client;
        this.strategy = strategy;
        this.consumer = consumer;
        consumerCloud = client.getCloud(consumer);
        consumerCloud.add(c -> this.strategy.setConsumerCloud(this, c));
        buffer.down().link(FGate.<byte[], byte[], Acceptor<byte[]>>of(new Acceptor<>(this) {

            @Override
            public AString toString(AString sb) {
                return sb.add("MessageNode(input buffer acceptor)");
            }

            @Override
            public String toString() {
                return "MessageNode(input buffer acceptor)";
            }

            @Override
            public boolean isWritable() {
                return !connectionsOut.isEmpty();
            }

            @Override
            public void send(Value<byte[]> value) {
                for (var e : connectionsOut) {
                    if (e.socketStreamClient.up().isWritable()) {
                        e.safeApiCon.getRemoteApi().run(a -> a.sendMessage(consumer, value.data()));
                        return;
                    }
                }
                Flow.flow(connectionsOut).random().safeApiCon.getRemoteApi().run(a -> a.sendMessage(consumer, value.data()));
            }

            @Override
            public void close() {

            }

            @Override
            public void requestData() {
                for (var e : connectionsIn) {
                    e.ready.toOnce((c) -> {
                        c.safeApiCon.getRemoteApi().run(a -> a.ping(client.getPingTime()));
                    });
                }
            }
        }).outSide());
    }

    @Override
    public AString toString(AString sb) {
        return sb.add("MessageNode(").add(consumer).add(")");
    }

    @Override
    public String toString() {
        return toString(AString.of()).toString();
    }

    public MessageEventListener getStrategy() {
        return strategy;
    }

    public void setStrategy(MessageEventListener strategy) {
        this.strategy = strategy;
    }

    @Override
    public FGate<byte[], byte[], ?> gUp() {
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
            buffer.down().requestData();
        }
    }

    public void addConnectionIn(ConnectionWork connectionWork, Gate<byte[], byte[]> gate) {
        connectionsIn.add(connectionWork);
    }

    public void sendMessageFromServerToClient(byte[] data) {
        buffer.down().send(Value.of(data));
    }
}
