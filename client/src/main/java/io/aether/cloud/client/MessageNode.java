package io.aether.cloud.client;

import io.aether.api.common.Cloud;
import io.aether.api.common.ServerDescriptor;
import io.aether.logger.Log;
import io.aether.net.fastMeta.FastApiContextLocal;
import io.aether.net.fastMeta.FastFutureContext;
import io.aether.net.fastMeta.FastMetaApi;
import io.aether.utils.AString;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.ToString;
import io.aether.utils.dataio.DataInOutStatic;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.AConsumer;
import io.aether.utils.interfaces.AFunction;
import io.aether.utils.slots.EventConsumerWithQueue;
import io.aether.utils.tuples.Tuple;
import io.aether.utils.tuples.Tuple2;

import java.util.Deque;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MessageNode implements ToString {
    public final EventConsumerWithQueue<byte[]> bufferIn = new EventConsumerWithQueue<>();
    final UUID consumer;
    final ARFuture<Cloud> consumerCloud;
    final Set<ConnectionWork> connectionsOut = new ConcurrentHashSet<>();
    final Deque<Tuple2<byte[], AFuture>> bufferOut = new ConcurrentLinkedDeque<>();
    private final AetherCloudClient client;
    private volatile MessageEventListener strategy;

    public MessageNode(AetherCloudClient client, UUID consumer, MessageEventListener strategy) {
        Log.trace("open message node ($client) from $uidFrom to $uidTo", "client", client.getName(), "uidTo", consumer, "uidFrom", client.getUid());
        this.client = client;
        this.strategy = strategy;
        this.consumer = consumer;
        consumerCloud = client.getCloud(consumer);
        consumerCloud.to(c -> {
            this.strategy.setConsumerCloud(this, c);
        });
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

    public AFuture send(byte[] msg) {
        AFuture f = AFuture.make();
        bufferOut.add(Tuple.of(msg, f));
        return f;
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
        if (connectionsOut.add(connectionWork)) {
            Log.debug("add connection out for messages uid=$uid", "uid", consumer);
        }
    }

    public void sendMessageFromServerToClient(byte[] data) {
        Log.trace("sendMessageFromServerToClient");
        bufferIn.fire(data);
    }

    public void toConsumer(AConsumer<byte[]> o) {
        bufferIn.add(o::accept);
    }

    public <LT> FastApiContextLocal<LT> toApiR(
            FastMetaApi<LT, ? extends LT> metaLt,
            AFunction<FastApiContextLocal<LT>, LT> localApi) {
        FastApiContextLocal<LT> ctx = new FastApiContextLocal<>(localApi) {
            @Override
            public void flush(AFuture sendFuture) {
                send(remoteDataToArray()).to(sendFuture);
            }
        };
        toApi(ctx, metaLt, ctx.localApi);
        return ctx;
    }

    public <LT> void toApi(FastFutureContext ctx, FastMetaApi<LT, ? extends LT> metaLt, LT localApi) {
        toConsumer(v -> {
            metaLt.makeLocal(ctx, new DataInOutStatic(v), localApi);
        });
    }

    public <LT> FastApiContextLocal<LT> toApi(FastApiContextLocal<LT> ctx, FastMetaApi<LT, ? extends LT> metaLt) {
        toApi(ctx, metaLt, ctx.localApi);
        return ctx;
    }

    public <LT> FastApiContextLocal<LT> toApi(FastMetaApi<LT, ? extends LT> metaLt, LT localApi) {
        FastApiContextLocal<LT> ctx = new FastApiContextLocal<>(localApi) {
            @Override
            public void flush(AFuture sendFuture) {
                var d = remoteDataToArray();
                if (d.length > 0) {
                    send(d).to(sendFuture);
                } else {
                    sendFuture.done();
                }
            }
        };
        toApi(ctx, metaLt, localApi);
        return ctx;
    }
}
