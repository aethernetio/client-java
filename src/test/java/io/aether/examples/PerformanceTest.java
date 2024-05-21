package io.aether.examples;

import io.aether.Aether;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.cloud.client.MessageRequest;
import io.aether.common.Message;
import io.aether.common.SignChecker;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Disabled
public class PerformanceTest {
	public ClientConfiguration clientConfig1;
	public ClientConfiguration clientConfig2;
	public SignChecker globalSigner;
	public List<URI> cloudFactoryURI = new ArrayList<>();
	@Test
	public void main() throws InterruptedException {
		if (clientConfig1 == null) clientConfig1 = new ClientConfiguration(Aether.TEST_UID, globalSigner, cloudFactoryURI);
		if (clientConfig2 == null) clientConfig2 = new ClientConfiguration(Aether.TEST_UID, globalSigner, cloudFactoryURI);
		AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
		client1.startFuture.waitDoneSeconds(4);
		AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
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
