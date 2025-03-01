package io.aether.examples;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.utils.futures.AFuture;
import io.aether.utils.streams.Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class PointToPointTest {
    public final List<URI> cloudFactoryURI = new ArrayList<>();
    public ClientConfiguration clientConfig1;
    public ClientConfiguration clientConfig2;

    {
        cloudFactoryURI.add(URI.create("tcp://registration.aethernet.io:9010"));
    }

    @Test
    public void main() {
        if (clientConfig1 == null) clientConfig1 = new ClientConfiguration(StandardUUIDs.TEST_UID, cloudFactoryURI);
        if (clientConfig2 == null) clientConfig2 = new ClientConfiguration(StandardUUIDs.TEST_UID, cloudFactoryURI);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
        Assertions.assertTrue(AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(1000));
        System.out.println("clients is registered\nuid1: "+client1.getUid()+"\nuid2: "+client2.getUid());
        client2.ping();
        AFuture checkReceiveMessage = new AFuture();
        var message = new byte[]{1, 2, 3, 4};
        client2.onClientStream((st) -> {
            Assertions.assertEquals(client1.getUid(), st.getConsumerUUID());
            st.up().toConsumer(newMessage -> {
                Assertions.assertArrayEquals(newMessage, message);
                checkReceiveMessage.done();
            });
        });
        System.out.println("START!");
        var chToc2 = client1.openStreamToClient(client2.getUid());
        var g = chToc2.toConsumer(v -> {
        });
        Thread.currentThread().setName("MAIN THREAD");
        var st=g.getFGateCast().inSide;
        while (!st.isSoftWritable()){
            Thread.yield();
        }
        st.send(Value.ofForce(message));
        checkReceiveMessage.to(()->{
            System.out.println("DONE!");
        });
        Assertions.assertTrue(checkReceiveMessage.waitDoneSeconds(1000));
        client1.stop(5);
        client2.stop(5);
    }
}
