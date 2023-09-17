package com.aether.examples;

import com.aether.cloud.client.AetherCloudClient;
import com.aether.utils.futures.AFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PointToPointTest {
	@Test
	void main() {
		AetherCloudClient client1 = new AetherCloudClient();
		AetherCloudClient client2 = new AetherCloudClient();
		AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(10);
		var message = "Hello world!".getBytes();
		client1.sendMessage(client2.getUid(), message);
		AFuture checkReceiveMessage = new AFuture();
		client2.onMessage(newMessage -> {
			Assertions.assertEquals(newMessage.data(), message);
			checkReceiveMessage.done();
		});
		checkReceiveMessage.waitDoneSeconds(5);
		client1.stop(5);
		client2.stop(5);
	}
}
