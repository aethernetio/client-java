package io.aether.examples;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.cloud.client.MessageEventListener;
import io.aether.cloud.client.MessageNode;
import io.aether.utils.futures.AFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

class MessageNodeTest {
    public final List<URI> cloudFactoryURI = new ArrayList<>();
    public ClientStateInMemory clientConfig1;
    public ClientStateInMemory clientConfig2;

    {
        cloudFactoryURI.add(URI.create("tcp://registration.aethernet.io:9010"));
    }

    @Test
    public void plain() {
        if (clientConfig1 == null) clientConfig1 = new ClientStateInMemory(StandardUUIDs.TEST_UID, cloudFactoryURI);
        if (clientConfig2 == null) clientConfig2 = new ClientStateInMemory(StandardUUIDs.TEST_UID, cloudFactoryURI);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
        Assertions.assertTrue(AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(1000));
        MessageNode mn=client1.openStreamToClientDetails(client2.getUid(), MessageEventListener.DEFAULT);
    }

}