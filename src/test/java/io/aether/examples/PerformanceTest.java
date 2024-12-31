package io.aether.examples;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.logger.Log;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.streams.Acceptor;
import io.aether.utils.streams.FGate;
import io.aether.utils.streams.Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Disabled
public class PerformanceTest {
    public final List<URI> cloudFactoryURI = new ArrayList<>();
    public ClientConfiguration clientConfig1;
    public ClientConfiguration clientConfig2;

    @Test
    public void timeOneMessage() {
        if (clientConfig1 == null) clientConfig1 = new ClientConfiguration(StandardUUIDs.TEST_UID, cloudFactoryURI);
        if (clientConfig2 == null) clientConfig2 = new ClientConfiguration(StandardUUIDs.TEST_UID, cloudFactoryURI);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
        Assertions.assertTrue(AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(5));
        Log.addFilter(c -> false);
        var ch1 = client1.openStreamToClient(client2.getUid());
        var ch11 = ch1.ofConsumer(v->{});
        final var total = 100_000_000L;
        AtomicLong receiveCounter = new AtomicLong(total);
        var tgc = new TestGateCounter(receiveCounter);
        client2.onClientStream((u, g) -> {
            Log.debug("client channel: " + u);
            g.link(FGate.<byte[],byte[],TestGateCounter>of(tgc).outSide());
        });
        client2.ping();
        var data = new byte[10000];
        var timeBegin = RU.time();
        var cc = 0L;
        long lOut=0;
        tgc.future.to(() -> {
            var ct = RU.time() - timeBegin;
            System.out.println(((total / ct))  + " Kb/s");
            Log.info((total / ct)  + " Kb/s");
        });
        long cFlush=0;
        while (!tgc.future.isDone()) {
            cFlush++;
            if (!ch11.getFGateCast().inSide.isSoftWritable()) {
                Thread.yield();
                continue;
            }
            if (cFlush%100==0) {
                ch11.getFGateCast().inSide.flush();
            } else {
                ch11.getFGateCast().inSide.send(Value.of(data));
                cc += data.length;
                if(cc/STEP!=lOut){
                    Log.info(">>> "+cc);
                    lOut=cc/STEP;
                }
            }
        }

        tgc.future.waitDone();
    }
    static final long STEP=1000_000;
    static final long STEP2=100_0000;
    @Test
    public void main2() {
        if (clientConfig1 == null)
            clientConfig1 = new ClientConfiguration(StandardUUIDs.TEST_UID, cloudFactoryURI);
        if (clientConfig2 == null)
            clientConfig2 = new ClientConfiguration(StandardUUIDs.TEST_UID, cloudFactoryURI);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
        client1.startFuture.waitDoneSeconds(4);
        client2.startFuture.waitDoneSeconds(4);
    }

    private static class TestGateCounter extends Acceptor<byte[]> {
        public final AFuture future = new AFuture();
        final AtomicLong counter;
        @Override
        public boolean isSoftWritable() {
            return true;
        }
        public TestGateCounter(AtomicLong counter) {
            super(null);
            this.counter = counter;
        }
        long lIn=0;

        @Override
        public void close() {

        }

        @Override
        public boolean isWritable() {
            return true;
        }

        @Override
        public void flush() {

        }

        @Override
        public void requestData() {

        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void connect() {

        }

        @Override
        public void send(Value<byte[]> value) {
            var c = counter.addAndGet(-value.data().length);
            if(c<0)return;
            if(c/STEP2!=lIn){
                Log.info("<<< "+c);
                lIn=c/STEP2;
            }
            if (c <= 0) {
                Log.info("<<< "+c);
                future.tryDone();
            }
        }
    }
}
