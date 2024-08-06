package io.aether.examples;

import io.aether.Aether;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.common.SignChecker;
import io.aether.utils.futures.AFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class PointToPointTest {
	public ClientConfiguration clientConfig1;
	public ClientConfiguration clientConfig2;
	public SignChecker globalSigner;
	public List<URI> cloudFactoryURI = new ArrayList<>();
	@Test
	public void main() {
		if (clientConfig1 == null) clientConfig1 = new ClientConfiguration(Aether.TEST_UID, globalSigner, cloudFactoryURI);
		if (clientConfig2 == null) clientConfig2 = new ClientConfiguration(Aether.TEST_UID, globalSigner, cloudFactoryURI);
		AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
		client1.startFuture.waitDoneSeconds(10);
		AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
		client2.startFuture.waitDoneSeconds(10);
		var message = "Hello world!".getBytes();
		client1.sendMessage(client2.getUid(), message);
		AFuture checkReceiveMessage = new AFuture();
		client2.onMessage(newMessage -> {
			Assertions.assertArrayEquals(newMessage.data(), message);
			try {
				checkReceiveMessage.done();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		checkReceiveMessage.waitDoneSeconds(10);
		client1.stop(5);
		client2.stop(5);
	}
}
