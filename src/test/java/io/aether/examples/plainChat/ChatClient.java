package io.aether.examples.plainChat;

import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.cloud.client.ClientOverMessages;
import io.aether.net.ApiDeserializerConsumer;
import io.aether.net.RemoteApi;
import io.aether.net.impl.bin.ApiLevelDeserializer;
import io.aether.net.meta.ExceptionUnit;
import io.aether.net.meta.ResultUnit;
import io.aether.utils.slots.EventConsumer;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatClient implements ServiceClientApi, ApiDeserializerConsumer {
	private final Map<UUID, UserDescriptor> users = new ConcurrentHashMap<>();
	private final ServiceServerApi service;
	public final AetherCloudClient aether;
	public final EventConsumer<MessageDescriptor> onMessage = new EventConsumer<>();
	public ChatClient(UUID chatService, String name) {
		aether = new AetherCloudClient(new ClientConfiguration(chatService,  List.of(URI.create("tcp://aethernet.io"))))
				.waitStart(10);
		var clientOverMessages = new ClientOverMessages<>(aether, ServiceClientApi.class, ServiceServerApi.class, this);
		service = clientOverMessages.getRemoteApiBy(chatService);
		service.registration(name);
		flush();
	}
	private ApiLevelDeserializer apiProcessor;
	@Override
	public void setApiDeserializer(ApiLevelDeserializer apiProcessor) {
		this.apiProcessor=apiProcessor;
	}

	@Override
	public void sendResult(ResultUnit unit) {
		apiProcessor.sendResultFromRemote(unit);
	}

	@Override
	public void sendException(ExceptionUnit unit) {
		apiProcessor.sendResultFromRemote(unit);
	}

	private void flush() {
		RemoteApi.of(service).flush();
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
