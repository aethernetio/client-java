package io.aether.examples;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.logger.Log;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.streams.FGate;
import io.aether.utils.streams.Gate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

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
        Log.addIgnoreRule(c -> c instanceof Log.Trace);
        var ch1 = client1.openStreamToClient(client2.getUid());
        var ch11 = Gate.ofConsumer(ch1,v->{});
        final var total = 100_000_000L;
        AtomicLong receiveCounter = new AtomicLong(total);
        var tgc = new TestGateCounter(receiveCounter);
        client2.onClientStream((u, g) -> {
            Log.debug("client channel: " + u);
            g.link(FGate.<byte[],byte[],TestGateCounter>of(tgc).outSide());
        });
        client2.ping();
        var data = new byte[1000];
        var timeBegin = RU.time();
        var cc = 0L;
        long lOut=0;
        while (cc < total) {
            if (!ch11.isWritable()) {
                Thread.yield();
            } else {
                ch11.sendAndFlush(data);
                cc += data.length;
                if(cc/STEP!=lOut){
                    Log.info(">>> "+cc);
                    lOut=cc/STEP;
                }
            }
        }
        tgc.future.to(() -> {
            var ct = RU.time() - timeBegin;
            Log.info((total / ct)  + " b/s");
        });
        tgc.future.waitDone();
    }
    static final long STEP=1000_0000;
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

    private static class TestGateCounter extends FGate.Acceptor<byte[]> {
        public final AFuture future = new AFuture();
        final AtomicLong counter;

        public TestGateCounter(AtomicLong counter) {
            super(null);
            this.counter = counter;
        }
        long lIn=0;

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
        public boolean inThread() {
            return true;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void connect() {

        }

        @Override
        public void send(byte[] value) {
            var c = counter.addAndGet(-value.length);
            if(c/STEP2!=lIn){
                Log.info("<<< "+c);
                lIn=c/STEP2;
            }
            if (c <= 0) {
                future.tryDone();
            }
        }
    }
}
