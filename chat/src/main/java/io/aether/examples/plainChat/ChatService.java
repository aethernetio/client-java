package io.aether.examples.plainChat;

import io.aether.api.chatdsl.*;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.common.AccessGroupI;
import io.aether.logger.Log;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChatService {
    public static final ARFuture<UUID> uid = ARFuture.of();
    public final AetherCloudClient aether;
    public final Map<UUID, ServiceClientApiRemote> clients = new ConcurrentHashMap<>();
    final Queue<MessageDescriptor> allMessages = new ConcurrentLinkedQueue<>();
    private final Map<UUID, UserDescriptor> users = new ConcurrentHashMap<>();

    public ChatService(List<URI> registrationUri) {
        aether = new AetherCloudClient(new ClientStateInMemory(UUID.fromString("B30AD9CA-FF20-E851-B11F-AED62C584AD2"), registrationUri), "ChatService")
                .waitStart(10);
        uid.done(aether.getUid());
        ARFuture<AccessGroupI> groupFuture = aether.createAccessGroup();
        aether.onNewChildren((u) -> {
            groupFuture.to(group -> {
                group.add(u).toFuture().to(() -> {
                    Log.info("NEW CHILD DONE: $uid", "uid", u);
                });
                Log.info("NEW CHILD: $uid", "uid", u);
            });
        });
        aether.onClientStream((s) -> {
            var api = s.toApiR(ServiceServerApi.META,
                    c -> {
                        var r = c.makeRemote(ServiceClientApi.META);
                        clients.put(s.getConsumerUUID(), r);
                        return new MyServiceServerApi(s.getConsumerUUID(), r);
                    });
        });
    }

    private class MyServiceServerApi extends ServiceServerApiLocal<ServiceClientApiRemote> {
        private final UUID uid;

        public MyServiceServerApi(UUID consumerUUID, ServiceClientApiRemote remoteApi) {
            super(remoteApi);
            this.uid = consumerUUID;
        }

        @Override
        public AFuture registration(String name) {
            Log.info("registration: $name", "name", name);
            var u = new UserDescriptor(uid, name);
            users.put(uid, u);
            for (var uu : users.values()) {
                var r = clients.get(uu.getUid());
                if (r != null) {
                    r.addNewUsers(new UserDescriptor[]{u});
                }
            }
            remoteApi.addNewUsers(Flow.flow(users.values()).toArray(UserDescriptor.class));
            remoteApi.newMessages(Flow.flow(allMessages).toArray(MessageDescriptor.class));
            return AFuture.completed();
        }

        @Override
        public void sendMessage(String msg) {
            Log.info("send message to chat: $msg", "msg", msg);
            var md = new MessageDescriptor(uid, msg);
            allMessages.add(md);
            var vv = users.values();
            if (vv.isEmpty()) {
                Log.warn("no users for send message to chat: $msg", "msg", msg);
            } else {
                Log.info("Task chat message: $msg -> [$users]", "msg", msg, "users", vv);
            }
            for (var u : vv) {
                var r = clients.get(u.getUid());
                Log.info("try send newMessages to remote: $uid", "uid", u.getUid());

                Log.info("send newMessages to remote: $uid", "uid", u.getUid());
                r.newMessages(new MessageDescriptor[]{md});
            }
        }
    }
}
