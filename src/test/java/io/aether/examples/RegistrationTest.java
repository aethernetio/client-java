package io.aether.examples;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class RegistrationTest {
	public ClientConfiguration clientConfig1;
	public final List<URI> cloudFactoryURI = new ArrayList<>();
	@Test
	public void main() {
		if (clientConfig1 == null) clientConfig1 = new ClientConfiguration(StandardUUIDs.TEST_UID,  cloudFactoryURI);
		AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
		client1.startFuture.waitDoneSeconds(10);
		client1.stop(5);
	}
}
