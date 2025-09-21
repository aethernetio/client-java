package io.aether.examples.meteostation;

import io.aether.api.metestation.*;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.cloud.client.Remote;
import io.aether.logger.Log;
import io.aether.utils.slots.EventConsumer;
import io.aether.utils.slots.EventConsumerWithQueue;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MeteostationClientConsole implements MetestationClientApi {
    public final AetherCloudClient aether;
    public final EventConsumer<Metric> onMessage = new EventConsumerWithQueue<>();
    private final Map<UUID, SensorDescriptor> users = new ConcurrentHashMap<>();
    private final Remote<MeteostationServiceApi> service;
    private final ApiGate<MetestationClientApi, MeteostationServiceApi> apiNode;

    public MeteostationClientConsole(UUID chatService, List<URI> regUri) {
        aether = new AetherCloudClient(new ClientStateInMemory(chatService, regUri))
                .waitStart(10);
        this.apiNode = aether.openStreamToClient(chatService).bufferAutoFlush().toApi(MetestationClientApi.META, MeteostationServiceApi.META, this);
        service = apiNode.getRemoteApi();
        service.run_flush(a -> a.registration(name));
    }


    @Override
    public void addNewUsers(SensorDescriptor[] users) {
        for (var u : users) {
            this.users.put(u.uid, u);
        }
    }

    public Map<UUID, SensorDescriptor> getUsers() {
        return users;
    }

    public void sendMessage(String message) {
        service.run_flush(a -> a.sendMessage(message));
    }

    @Override
    public void newMessages(Metric[] messages) {
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
