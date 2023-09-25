package io.aether.examples.plainChat;

import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ProtocolOverMsg;
import io.aether.common.AetherCodec;
import io.aether.net.ProtocolConfig;

import java.util.UUID;

public class ChatService {
	public UUID uid;
	public ChatService() {
		var client = new AetherCloudClient();
		client.startFuture.waitDone();
		uid = client.getUid();
		ProtocolConfig<ServiceServerApi, ServiceClientApi> pc = ProtocolConfig.of(ServiceServerApi.class, ServiceClientApi.class, AetherCodec.BINARY);
		client.onMessage(m -> {
			ProtocolOverMsg<ServiceServerApi, ServiceClientApi> protocol =
					new ProtocolOverMsg<>(pc, client, new ServiceServerApi() {
					}, m.uid());
		});
	}
}
