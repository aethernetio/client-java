package io.aether.examples.plainChat;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.net.ApiGate;
import io.aether.net.RemoteApi;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.streams.ApiNode;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatService {
	public static final ARFuture<UUID> uid = new ARFuture<>();
	private final Map<UUID, UserDescriptor> users = new ConcurrentHashMap<>();
	public final AetherCloudClient aether;
	public final Map<UUID, ApiGate<ServiceServerApi,ServiceClientApi>> clients=new ConcurrentHashMap<>();
	public ChatService() {
		aether = new AetherCloudClient(new ClientConfiguration(StandardUUIDs.TEST_UID,  null))
				.waitStart(10);
		uid.done(aether.getUid());
		aether.onClientStream((u,s)->{
			var apiNode = ApiNode.of(ServiceServerApi.META, ServiceClientApi.META, s);
			clients.put(u,apiNode.forClient(new MyServiceServerApi(u)));
		});
	}
	private class MyServiceServerApi implements ServiceServerApi {
		private final UUID uid;
		ServiceClientApi remoteApi;
		public MyServiceServerApi(UUID uid) {
			this.uid = uid;
		}
		@Override
		public void registration(String name) {
			var u = new UserDescriptor(uid, name);
			for (var uu : users.values()) {
				var r = clients.get(uu.uid());
				if(r!=null){
					r.getRemoteApi().addNewUsers(new UserDescriptor[]{u});
					r.flush();
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
				r.flush();
			}
		}
	}
}
