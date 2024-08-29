package io.aether.examples;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.cloud.client.MessageRequest;
import io.aether.common.Message;
import io.aether.common.SignChecker;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class PerformanceTest {
    public ClientConfiguration clientConfig1;
    public ClientConfiguration clientConfig2;
    public List<URI> cloudFactoryURI = new ArrayList<>();

    @Test
    public void timeOneMessage() throws InterruptedException {
        if (clientConfig1 == null) clientConfig1 = new ClientConfiguration(StandardUUIDs.TEST_UID, cloudFactoryURI);
        if (clientConfig2 == null) clientConfig2 = new ClientConfiguration(StandardUUIDs.TEST_UID, cloudFactoryURI);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
        client1.startFuture.waitDoneSeconds(4);
        client2.startFuture.waitDoneSeconds(4);
        long min = Long.MAX_VALUE;
        var message = new byte[8];
        BlockingQueue<Message> mq = new ArrayBlockingQueue<>(10);
        client2.onMessage(mq::add);
        while (true) {
            var t1 = System.nanoTime();
            new MessageRequest(client1, client2.getUid(), message)
                    .requestByStrategy(MessageRequest.STRATEGY_FAST);
//			client1.sendMessage(client2.getUid(), message);
            var m = mq.poll(10, TimeUnit.SECONDS);
            var t2 = System.nanoTime();
            var delta = t2 - t1;
            if (min > delta) {
                min = delta;
                System.out.println(min);
            }
        }
    }

    @Test
    public void main2() throws InterruptedException {
        if (clientConfig1 == null)
            clientConfig1 = new ClientConfiguration(StandardUUIDs.TEST_UID, cloudFactoryURI);
        if (clientConfig2 == null)
            clientConfig2 = new ClientConfiguration(StandardUUIDs.TEST_UID, cloudFactoryURI);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
        client1.startFuture.waitDoneSeconds(4);
        client2.startFuture.waitDoneSeconds(4);
        long min = Long.MAX_VALUE;
        var message = new byte[8];
        BlockingQueue<Message> mq = new ArrayBlockingQueue<>(10);
        client2.onMessage(mq::add);
        while (true) {
            var t1 = System.nanoTime();
            new MessageRequest(client1, client2.getUid(), message)
                    .requestByStrategy(MessageRequest.STRATEGY_FAST);
//			client1.sendMessage(client2.getUid(), message);
            var m = mq.poll(10, TimeUnit.SECONDS);
            var t2 = System.nanoTime();
            var delta = t2 - t1;
            if (min > delta) {
                min = delta;
                System.out.println(min);
            }
        }
    }
}
