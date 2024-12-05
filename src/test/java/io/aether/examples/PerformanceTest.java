package io.aether.examples;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.logger.Log;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.streams.Gate;
import io.aether.utils.streams.OutputWR;
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
        Assertions.assertTrue(AFuture.all(client1.startFuture,client2.startFuture).waitDoneSeconds(5));
        Log.addIgnoreRule(c->c instanceof Log.Trace);
        var ch1 = client1.openStreamToClient(client2.getUid());
        var ch11 = Gate.of(ch1);
        AtomicLong receiveCounter = new AtomicLong();
        client2.onClientStream((u, g) -> {
            Log.debug("client channel: " + u);
            g.link(new TestGateCounter(receiveCounter));
        });
        client2.ping();
        RU.sleep(1000);
        var durationSeconds = 10;
        var data = new byte[100];
        var timeBegin = RU.time();
        while (RU.time() - timeBegin < durationSeconds *1000) {
            ch11.sendAndFlush(data);
        }
        Log.info((receiveCounter.get() / durationSeconds)/1000  + " Kb/s");
    }

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

    private static class TestGateCounter extends Gate.WithoutConfirm<byte[], byte[]> implements OutputWR<byte[]> {
        @Override
        protected Object getOwner() {
            return null;
        }

        AtomicLong counter;

        public TestGateCounter(AtomicLong counter) {
            this.counter = counter;
        }

        @Override
        public void sendIn(byte[] value) {
            counter.addAndGet(value.length);
        }

    }
}
