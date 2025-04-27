package io.aether.examples.plainChat;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class RegistrationTest {
    public final List<URI> cloudFactoryURI = new ArrayList<>();
    public ClientStateInMemory clientConfig1;

    @Test
    public void main() {
        if (clientConfig1 == null) clientConfig1 = new ClientStateInMemory(StandardUUIDs.TEST_UID, cloudFactoryURI);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        client1.startFuture.waitDoneSeconds(10);
        Assertions.assertTrue(client1.destroy(true).waitDoneSeconds(5));
    }
}
