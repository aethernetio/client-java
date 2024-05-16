package io.aether.examples;

import io.aether.Aether;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.common.SignChecker;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Disabled
public class RegistrationTest {
	public ClientConfiguration clientConfig1;
	public SignChecker globalSigner;
	public List<URI> cloudFactoryURI = new ArrayList<>();
	@Test
	public void main() {
		if (clientConfig1 == null) clientConfig1 = new ClientConfiguration(Aether.TEST_UID, globalSigner, cloudFactoryURI);
		AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
		client1.startFuture.waitDoneSeconds(10);
		client1.stop(5);
	}
}
