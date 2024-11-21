package io.aether.examples.plainChat;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.net.ApiGateConnection;
import io.aether.net.ApiDeserializerConsumer;
import io.aether.net.RemoteApi;
import io.aether.net.impl.bin.ApiLevel;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.streams.ApiStream;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatService {
	public static final ARFuture<UUID> uid = new ARFuture<>();
	private final Map<UUID, UserDescriptor> users = new ConcurrentHashMap<>();
	public final AetherCloudClient aether;
	public final Map<UUID, ApiGateConnection<ServiceServerApi,ServiceClientApi>> clients=new ConcurrentHashMap<>();
	public ChatService() {
		aether = new AetherCloudClient(new ClientConfiguration(StandardUUIDs.TEST_UID,  null))
				.waitStart(10);
		uid.done(aether.getUid());
		aether.onClientStream((u,s)->{
			var apiStream = ApiStream.of(ServiceServerApi.class, ServiceClientApi.class, s);
			clients.put(u,apiStream.forClient(new MyServiceServerApi(u)));
		});
	}
	private class MyServiceServerApi implements ServiceServerApi, ApiDeserializerConsumer {
		private final UUID uid;
		ServiceClientApi remoteApi;
		public MyServiceServerApi(UUID uid) {
			this.uid = uid;
		}
		ApiLevel apiProcessor;
		@Override
		public void setApiDeserializer(ApiLevel apiProcessor) {
			remoteApi = apiProcessor.getRemoteApi();
			this.apiProcessor=apiProcessor;
		}
		@Override
		public void registration(String name) {
			var u = new UserDescriptor(uid, name);
			for (var uu : users.values()) {
				var r = clients.get(uu.uid());
				if(r!=null){
					r.getRemoteApi().addNewUsers(new UserDescriptor[]{u});
					r.flushOut();
				}
			}
			remoteApi.addNewUsers(Flow.flow(users.values()).toArray(UserDescriptor.class));
			users.put(uid, u);
			RemoteApi.of(remoteApi).flush();
		}

		@Override
		public void sendMessage(String msg) {
			var md = new MessageDescriptor(uid, msg);
			for (var u : users.values()) {
				var r = clients.get(u.uid());
				r.getRemoteApi().newMessages(new MessageDescriptor[]{md});
				r.flushOut();
			}
		}
	}
}
