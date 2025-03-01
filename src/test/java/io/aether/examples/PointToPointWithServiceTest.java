package io.aether.examples;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.futures.AFuture;
import io.aether.utils.streams.Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PointToPointWithServiceTest {
    public final List<URI> cloudFactoryURI = new ArrayList<>();
    public ClientConfiguration serviceConfig;
    public ClientConfiguration clientConfig1;
    public ClientConfiguration clientConfig2;

    {
        cloudFactoryURI.add(URI.create("tcp://registration.aethernet.io:9010"));
    }

    @Test
    public void main() {
        if (serviceConfig == null) serviceConfig = new ClientConfiguration(StandardUUIDs.TEST_UID, cloudFactoryURI);
        AetherCloudClient service = new AetherCloudClient(serviceConfig);
        service.startFuture.waitDoneSeconds(10);
        System.out.println("service is registered");
        Set<UUID> allChildren = new ConcurrentHashSet<>();
//        AccessGroup group=service.createAccessGroup(new UUID[0]);
//        service.onNewChildrenApi((u,api) -> {
//            for(var e:allChildren){
//                api.addAccessGroup(u).waitDone();
//            }
//            allChildren.add(u);
//            System.out.println("NEW CHILD: " + u);
//        });
        if (clientConfig1 == null) clientConfig1 = new ClientConfiguration(service.getUid(), cloudFactoryURI);
        if (clientConfig2 == null) clientConfig2 = new ClientConfiguration(service.getUid(), cloudFactoryURI);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
        Assertions.assertTrue(AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(10));
        System.out.println("clients is registered");
        client2.ping();
        AFuture checkReceiveMessage = new AFuture();
        var message = new byte[]{0, (byte) 0xff, 0, (byte) 0xff, 0, (byte) 0xff, 0, (byte) 0xff};
        client2.onClientStream((st) -> {
            Assertions.assertEquals(client1.getUid(), st.getConsumerUUID());
            st.up().toConsumer(newMessage -> {
                Assertions.assertArrayEquals(newMessage, message);
                checkReceiveMessage.done();
            });
        });
        System.out.println("START!");
        var chToc2 = client1.openStreamToClient(client2.getUid());
        System.out.println(chToc2);
        chToc2.send(Value.ofForce(message));
        System.out.println(chToc2);

        Assertions.assertTrue(checkReceiveMessage.waitDoneSeconds(10000));
        client1.stop(5);
        client2.stop(5);
    }
}
