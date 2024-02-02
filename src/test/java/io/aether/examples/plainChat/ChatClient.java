package io.aether.examples.plainChat;

import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientOverMessages;
import io.aether.net.RemoteApi;
import io.aether.utils.slots.SlotConsumer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatClient implements ServiceClientApi {
	private final Map<UUID, UserDescriptor> users = new ConcurrentHashMap<>();
	private final ServiceServerApi service;
	public final AetherCloudClient aether;
	public final SlotConsumer<MessageDescriptor> onMessage = new SlotConsumer<>();
	public ChatClient(UUID chatService, String name) {
		aether = new AetherCloudClient(chatService)
				.waitStart(10);
		var clientOverMessages = new ClientOverMessages<>(aether, ServiceClientApi.class, ServiceServerApi.class, this);
		service = clientOverMessages.getRemoteApiBy(chatService);
		service.registration(name);
		flush();
	}
	private void flush() {
		((RemoteApi) service).flush();
	}
	@Override
	public void addNewUsers(UserDescriptor[] users) {
		for (var u : users) {
			this.users.put(u.uid(), u);
		}
	}
	public Map<UUID, UserDescriptor> getUsers() {
		return users;
	}
	public void sendMessage(String message) {
		service.sendMessage(message);
		flush();
	}
	@Override
	public void newMessages(MessageDescriptor[] messages) {
		for (var m : messages) {
			onMessage.fire(m);
			var u = users.get(m.uid());
			if (u == null) {
				System.out.println(m);
			} else {
				System.out.println(u.name() + ": " + m.message());
			}
		}
	}
}
