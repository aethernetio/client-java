package io.aether.cloud.client;

import io.aether.common.Cloud;
import io.aether.common.ServerDescriptor;
import io.aether.utils.CType;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.slots.ARMultiFuture;
import io.aether.utils.streams.*;

import java.util.Set;
import java.util.UUID;

public class MessageNode implements NodeDown<byte[], byte[]> {
    private final AetherCloudClient client;
    private final UUID consumer;
    private final ARMultiFuture<Cloud> consumerCloud;
    private final Set<ConnectionWork> connectionsOut = new ConcurrentHashSet<>();
    private final Set<ConnectionWork> connectionsIn = new ConcurrentHashSet<>();
    private final Switcher<byte[], byte[]> gatesOut = new Switcher<>();
    private final Switcher<byte[], byte[]> gatesIn = new Switcher<>();
    private final BufferNode<byte[], byte[]> buffer = new BufferNode<>() {
        @Override
        public Object findByOwner(Class<?> t) {
            return t.isInstance(this) ? this : null;
        }

        @Override
        public Object findByOwner(CType t) {
            return t.isInstance(this) ? this : null;
        }
    };

    @Override
    public String toString() {
        return "message node ("+consumer+")";
    }

    private volatile MessageEventListener strategy;

    public MessageNode(AetherCloudClient client, UUID consumer, MessageEventListener strategy) {
        this.client = client;
        this.strategy = strategy;
        this.consumer = consumer;
        consumerCloud = client.getCloud(consumer);
        consumerCloud.add(c -> this.strategy.setConsumerCloud(this, c));
        buffer.down().link(FGate.<byte[],byte[],Acceptor<byte[]>>of(new Acceptor<>(this) {
            @Override
            public boolean isSoftWritable() {
                return gatesOut.down().isSoftWritable();
            }

            @Override
            public boolean isWritable() {
                return gatesOut.down().isWritable();
            }

            @Override
            public void send(Value<byte[]> value) {
                gatesOut.down().send(value);
            }

            @Override
            public void close() {

            }

            @Override
            public void requestData() {
                gatesIn.down().requestData();
            }
        }).outSide());
        gatesOut.down().link(FGate.<byte[],byte[],Acceptor<byte[]>>of(new Acceptor<>(this) {
            @Override
            public boolean isSoftWritable() {
                return buffer.down().isSoftWritable();
            }

            @Override
            public boolean isWritable() {
                return true;
            }

            @Override
            public void send(Value<byte[]> value) {
                buffer.down().send(value);
            }

            @Override
            public void close() {

            }

            @Override
            public void requestData() {
                buffer.down().requestData();
            }
        }).outSide());
        gatesIn.down().link(FGate.<byte[],byte[],Acceptor<byte[]>>of(new Acceptor<>(this) {
            @Override
            public boolean isSoftWritable() {
                return buffer.down().isSoftWritable();
            }

            @Override
            public boolean isWritable() {
                return true;
            }

            @Override
            public void send(Value<byte[]> value) {
                buffer.down().send(value);
            }

            @Override
            public void close() {

            }

            @Override
            public void requestData() {
                buffer.down().requestData();
            }
        }).outSide());
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
        client.getServer(id).to(s -> strategy.onResolveConsumerServer(this, s));
    }

    public void addConsumerServerOut(ServerDescriptor serverDescriptor) {
        strategy.onResolveConsumerConnection(this, client.getConnection(serverDescriptor));
    }

    public void addConsumerConnectionOut(ConnectionWork connectionWork) {
        connectionsOut.add(connectionWork);
        var st = connectionWork.openMessageChannel(consumer);
        gatesOut.linkUp(st);
    }

    public void addConnectionIn(ConnectionWork connectionWork, Gate<byte[], byte[]> gate) {
        connectionsIn.add(connectionWork);
        gatesIn.linkUp(gate);

    }

}
