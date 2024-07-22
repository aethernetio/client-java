package io.aether.examples.plainChat;

import io.aether.Aether;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.cloud.client.ServerOverMessages;
import io.aether.net.ApiProcessorConsumer;
import io.aether.net.RemoteApi;
import io.aether.net.impl.bin.ApiLevelDeserializer;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.streams.AStream;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatService {
	public static ARFuture<UUID> uid = new ARFuture<>();
	private final Map<UUID, UserDescriptor> users = new ConcurrentHashMap<>();
	private final ServerOverMessages<ServiceServerApi, ServiceClientApi> serverOverMessages;
	public final AetherCloudClient aether;
	public ChatService() {
		aether = new AetherCloudClient(new ClientConfiguration(Aether.TEST_UID, null, null))
				.waitStart(10);
		uid.done(aether.getUid());
		serverOverMessages = new ServerOverMessages<>(aether, ServiceServerApi.class, ServiceClientApi.class, (uid, message) -> new MyServiceServerApi(uid));
	}
	private class MyServiceServerApi implements ServiceServerApi, ApiProcessorConsumer {
		private final UUID uid;
		ServiceClientApi remoteApi;
		public MyServiceServerApi(UUID uid) {
			this.uid = uid;
		}
		@Override
		public void setApiProcessor(ApiLevelDeserializer apiProcessor) {
			remoteApi = apiProcessor.getRemoteApi();
		}
		@Override
		public void registration(String name) {
			var u = new UserDescriptor(uid, name);
			for (var uu : users.values()) {
				var r = serverOverMessages.getRemoteApiBy(uu.uid());
				r.addNewUsers(new UserDescriptor[]{u});
				RemoteApi.of(r).flush();
			}
			remoteApi.addNewUsers(AStream.streamOf(users.values()).toArray(UserDescriptor.class));
			users.put(uid, u);
			RemoteApi.of(remoteApi).flush();
		}
		@Override
		public void sendMessage(String msg) {
			var md = new MessageDescriptor(uid, msg);
			for (var u : users.values()) {
				var r = serverOverMessages.getRemoteApiBy(u.uid());
				r.newMessages(new MessageDescriptor[]{md});
				RemoteApi.of(r).flush();
			}
		}
	}
}
