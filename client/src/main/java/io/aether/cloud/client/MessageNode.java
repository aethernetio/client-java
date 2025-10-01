package io.aether.cloud.client;

import io.aether.api.common.*;
import io.aether.logger.Log;
import io.aether.net.fastMeta.FastApiContextLocal;
import io.aether.net.fastMeta.FastFutureContext;
import io.aether.net.fastMeta.FastMetaApi;
import io.aether.utils.AString;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.RU;
import io.aether.utils.ToString;
import io.aether.utils.dataio.DataInOutStatic;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.AConsumer;
import io.aether.utils.interfaces.AFunction;
import io.aether.utils.slots.EventConsumer;
import io.aether.utils.slots.EventConsumerWithQueue;
import io.aether.utils.streams.*;

import java.util.Deque;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MessageNode implements ToString {
    public final EventConsumer<Throwable> onError = new EventConsumer<>();
    private final AetherCloudClient client;
     final UUID consumer;
    final ARFuture<Cloud> consumerCloud;
     final Set<ConnectionWork> connectionsOut = new ConcurrentHashSet<>();
     final Set<ConnectionWork> connectionsIn = new ConcurrentHashSet<>();
    final Deque<Value<byte[]>> bufferOut = new ConcurrentLinkedDeque<>();
    public final EventConsumerWithQueue<Value<byte[]>> bufferIn = new EventConsumerWithQueue<>();
    private volatile MessageEventListener strategy;

    public MessageNode(AetherCloudClient client, UUID consumer, MessageEventListener strategy) {
        Log.trace("open message node ($client) from $uidFrom to $uidTo", "client", client.getName(), "uidTo", consumer, "uidFrom", client.getUid());
        this.client = client;
        this.strategy = strategy;
        this.consumer = consumer;
        consumerCloud = client.getCloud(consumer);
        consumerCloud.to(c ->{
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

    public void send(Value<byte[]> msg){
        bufferOut.add(msg);
    }
    public void send(byte[] msg){
        send(Value.of(msg));
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
            Log.debug("add connection out for messages uid=$uid","uid",consumer);
        }
    }

    public void sendMessageFromServerToClient(Value<byte[]> data) {
        bufferIn.fire(data);
    }

    public void toConsumer(AConsumer<byte[]> o) {
        bufferIn.add(d->{
            o.accept(d.data());
            d.success(this);
        });
    }

    public <LT> FastApiContextLocal<LT> toApiR(
            FastMetaApi<LT, ? extends LT> metaLt,
            AFunction<FastApiContextLocal<LT>,LT> localApi) {
        FastApiContextLocal<LT> ctx = new FastApiContextLocal<>(localApi) {
            @Override
            public AFuture flush() {
                return flushToGate(d->{
                    var res=new AFuture();
                    send(Value.ofForce(d).linkFuture(res));
                    return res;
                });
            }
        };
        toApi(ctx, metaLt, ctx.localApi);
        return ctx;
    }
    public <LT> void toApi(FastFutureContext ctx, FastMetaApi<LT, ? extends LT> metaLt, LT localApi) {
        RU.<Gate<byte[], byte[]>>cast(this).toConsumer("fast api(" + metaLt + ")", v -> {
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
            public AFuture flush() {
                AFuture f = new AFuture();
                var d=remoteDataToArray();
                if(d.length>0){
                    send(Value.ofForce(d).linkFuture(f));
                }else{
                    f.done();
                }
                return f;
            }
        };
        toApi(ctx, metaLt, localApi);
        return ctx;
    }
}
