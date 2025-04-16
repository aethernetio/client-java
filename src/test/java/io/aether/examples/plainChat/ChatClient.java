package io.aether.examples.plainChat;

import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.logger.Log;
import io.aether.net.ApiGate;
import io.aether.net.Remote;
import io.aether.utils.slots.EventConsumer;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatClient implements ServiceClientApi {
    public final AetherCloudClient aether;
    public final EventConsumer<MessageDescriptor> onMessage = new EventConsumer<>();
    private final Map<UUID, UserDescriptor> users = new ConcurrentHashMap<>();
    private final Remote<ServiceServerApi> service;
    private final ApiGate<ServiceClientApi, ServiceServerApi> apiNode;
    private final String name;

    public ChatClient(UUID chatService, List<URI> regUri, String name) {
        this.name = name;
        aether = new AetherCloudClient(new ClientStateInMemory(chatService, regUri))
                .waitStart(10);
        this.apiNode = aether.openStreamToClient(chatService).bufferAutoFlush().toApi(ServiceClientApi.META, ServiceServerApi.META, this);
        service = apiNode.getRemoteApi();
        service.run_flush(a -> a.registration(name));
    }


    @Override
    public void addNewUsers(UserDescriptor[] users) {
        for (var u : users) {
            this.users.put(u.uid, u);
        }
    }

    public Map<UUID, UserDescriptor> getUsers() {
        return users;
    }

    public void sendMessage(String message) {
        service.run_flush(a -> a.sendMessage(message));
    }

    @Override
    public void newMessages(MessageDescriptor[] messages) {
        for (var m : messages) {
            onMessage.fire(m);
            var u = users.get(m.uid);
            if (u == null) {
                Log.info("new message $msg", "msg", m);
            } else {
                Log.info("new message from: $from ($msg)", "from", u.name, "msg", m.message);
            }
        }
    }
}
