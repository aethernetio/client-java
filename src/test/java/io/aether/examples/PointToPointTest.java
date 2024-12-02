package io.aether.examples;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.logger.Log;
import io.aether.utils.futures.AFuture;
import io.aether.utils.streams.Gate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class PointToPointTest {
    public ClientConfiguration clientConfig1;
    public ClientConfiguration clientConfig2;
    public final List<URI> cloudFactoryURI = new ArrayList<>();

    @Test
    public void main() {
        if (clientConfig1 == null) clientConfig1 = new ClientConfiguration(StandardUUIDs.TEST_UID, cloudFactoryURI);
        if (clientConfig2 == null) clientConfig2 = new ClientConfiguration(StandardUUIDs.TEST_UID, cloudFactoryURI);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        client1.startFuture.waitDoneSeconds(10);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
        client2.startFuture.waitDoneSeconds(10);
        Log.info("clients is registered");
        client2.ping();
        var message = "Hello world!".getBytes();
        var chToc2 = client1.openStreamToClient(client2.getUid());
        var g=Gate.of(Runnable::run,chToc2,c->{});
        g.inSide().sendAndFlush(message);
        AFuture checkReceiveMessage = new AFuture();
        client2.onClientStream((u, st) -> {
            Assertions.assertEquals(client1.getUid(), u);
            st.link(Gate.of(Runnable::run,newMessage -> {
                Assertions.assertArrayEquals(newMessage, message);
                checkReceiveMessage.done();
            }));
        });
        checkReceiveMessage.waitDoneSeconds(10);
        client1.stop(5);
        client2.stop(5);
    }
}
