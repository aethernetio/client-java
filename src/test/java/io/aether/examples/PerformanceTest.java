package io.aether.examples;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.logger.Log;
import io.aether.utils.RU;
import io.aether.utils.streams.Gate;
import io.aether.utils.streams.GateImpl0;
import io.aether.utils.streams.GatePlain;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
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
        client1.startFuture.waitDoneSeconds(4);
        client2.startFuture.waitDoneSeconds(4);
        var ch1 = client1.openStreamToClient(client2.getUid());
        Executor executor = RU.newSingleThreadExecutor("test");
        var ch11 = new GatePlain<byte[], byte[]>(executor) {
            @Override
            public void send(byte[] value) {

            }
        };
        ch11.link(ch1);
        AtomicLong receiveCounter = new AtomicLong();
        client2.onClientStream((u, g) -> {
            Log.debug("client channel: " + u);
            g.link(new TestGateCounter(receiveCounter));
        });
        client2.ping();
        RU.sleep(1000);
        var duration = 20_000;
        var data = new byte[100];
        var timeBegin = RU.time();
        while (RU.time() - timeBegin < duration) {
            ch11.inSide().sendAndFlush(data);
        }
        Log.info(receiveCounter.get() / duration + " b/s");
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

    private class TestGateCounter extends GateImpl0<byte[], byte[]> implements Gate.OutputWR<byte[]> {

        AtomicLong counter;

        public TestGateCounter(AtomicLong counter) {
            super(null);
            this.counter = counter;
        }

        @Override
        public void send(byte[] value) {
            counter.addAndGet(value.length);
        }

        @Override
        public void flush() {

        }

        @Override
        protected void onFire() {
            inSide().receiveAll(this);
        }
    }
}
