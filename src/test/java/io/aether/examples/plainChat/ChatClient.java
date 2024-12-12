package io.aether.examples.plainChat;

import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientConfiguration;
import io.aether.net.ApiGate;
import io.aether.net.RemoteApi;
import io.aether.utils.slots.EventConsumer;
import io.aether.utils.streams.ApiNode;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatClient implements ServiceClientApi {
    public final AetherCloudClient aether;
    public final EventConsumer<MessageDescriptor> onMessage = new EventConsumer<>();
    private final Map<UUID, UserDescriptor> users = new ConcurrentHashMap<>();
    private final ServiceServerApi service;
    private final ApiGate<ServiceClientApi, ServiceServerApi> apiNode;
    private final String name;

    public ChatClient(UUID chatService, String name) {
        this.name = name;
        aether = new AetherCloudClient(new ClientConfiguration(chatService, List.of(URI.create("tcp://aethernet.io"))))
                .waitStart(10);
        this.apiNode = ApiNode.of(ServiceClientApi.META, ServiceServerApi.META, aether.openStreamToClient(chatService))
                .forClient(this);
        service = apiNode.getRemoteApi();
        service.registration(name);
        apiNode.flush();
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
