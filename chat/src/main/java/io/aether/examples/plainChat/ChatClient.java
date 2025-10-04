package io.aether.examples.plainChat;

import io.aether.api.chatdsl.*;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.cloud.client.MessageEventListener;
import io.aether.logger.Log;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.slots.EventConsumer;
import io.aether.utils.slots.EventConsumerWithQueue;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatClient implements ServiceClientApi {
    public final AetherCloudClient aether;
    public final EventConsumer<MessageDescriptor> onMessage = new EventConsumerWithQueue<>();
    private final Map<UUID, UserDescriptor> users = new ConcurrentHashMap<>();
    private final ARFuture<ServiceServerApiRemote> service = ARFuture.of();
    private final String name;

    public ChatClient(UUID chatService, List<URI> regUri, String name) {
        this.name = name;
        aether = new AetherCloudClient(new ClientStateInMemory(chatService, regUri), name);
        aether.startFuture.to(() -> {
            try {
                var s = aether.getMessageNode(chatService)
                        .toApiR(ServiceClientApi.META, c->{
                            service.done(c.makeRemote(ServiceServerApi.META));
                            return this;
                        });
                service.to(a->{
                        a.registration(name).timeout(3, () -> Log.warn("registration timeout: $name", "name", name));
                });
            } catch (Exception e) {
                Log.error(e);
            }
        }).timeout(3, () -> Log.warn("create aether client timeout: $name", "name", name));
        service.timeout(4, () -> Log.warn("get chat service timeout $name", "name", name));
    }


    @Override
    public void addNewUsers(UserDescriptor[] users) {
        for (var u : users) {
            this.users.put(u.getUid(), u);
        }
    }

    public Map<UUID, UserDescriptor> getUsers() {
        return users;
    }

    public void sendMessage(String message) {
        service.to(s -> s.sendMessage(message))
                .timeout(5, () -> {
                    Log.warn("send chat message timeout for: $name", "name", name);
                });
    }

    @Override
    public void newMessages(MessageDescriptor[] messages) {
        for (var m : messages) {
            onMessage.fire(m);
            var u = users.get(m.getUid());
            if (u == null) {
                Log.info("new message $msg", "msg", m);
            } else {
                Log.info("new message from: $from ($msg)", "from", u.getName(), "msg", m.getMessage());
            }
        }
    }
}
