package io.aether.examples.plainChat;

import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.common.AccessGroupI;
import io.aether.logger.Log;
import io.aether.net.ApiGate;
import io.aether.net.Remote;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.ARFuture;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChatService {
    public static final ARFuture<UUID> uid = new ARFuture<>();
    public final AetherCloudClient aether;
    public final Map<UUID, ApiGate<ServiceServerApi, ServiceClientApi>> clients = new ConcurrentHashMap<>();
    final Queue<MessageDescriptor> allMessages = new ConcurrentLinkedQueue<>();
    private final Map<UUID, UserDescriptor> users = new ConcurrentHashMap<>();

    public ChatService(List<URI> registrationUri) {
        aether = new AetherCloudClient(new ClientStateInMemory(UUID.fromString("B30AD9CA-FF20-E851-B11F-AED62C584AD2"), registrationUri))
                .waitStart(10);
        uid.done(aether.getUid());
        ARFuture<AccessGroupI> groupFuture = aether.createAccessGroup();
        aether.onNewChildren((u) -> {
            groupFuture.to(group -> {
                aether.getClientApi(u).to(api -> {
                    api.run_flush(a -> a.addAccessGroup(group.getId()).to(f -> {
                        Log.info("NEW CHILD DONE: $uid", "uid", u, "result", f);
                    }));
                }, 5, () -> Log.warn("timeout get client api for $uid", "uid", u));
                Log.info("NEW CHILD: $uid", "uid", u);
            });
        });
        aether.onClientStream((s) -> {
            var api = s.up().bufferAutoFlush().toApi(ServiceServerApi.META, ServiceClientApi.META, new MyServiceServerApi(s.getConsumerUUID()));
            clients.put(s.getConsumerUUID(), api);
        });
    }

    private class MyServiceServerApi implements ServiceServerApi {
        private final UUID uid;
        Remote<ServiceClientApi> remoteApi;

        public MyServiceServerApi(UUID uid) {
            this.uid = uid;
        }

        @Override
        public void registration(String name) {
            Log.info("registration: $name", "name", name);
            var u = new UserDescriptor(uid, name);
            users.put(uid, u);
            for (var uu : users.values()) {
                var r = clients.get(uu.uid);
                if (r != null) {
                    r.getRemoteApi().run_flush(a -> {
                        a.addNewUsers(new UserDescriptor[]{u});
                    });
                }
            }
            remoteApi.run_flush(a -> {
                a.addNewUsers(Flow.flow(users.values()).toArray(UserDescriptor.class));
                a.newMessages(Flow.flow(allMessages).toArray(MessageDescriptor.class));
            });

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
                var r = clients.get(u.uid);
                Log.info("try send newMessages to remote: $uid", "uid", u.uid);
                r.getRemoteApi().run_flush(a -> {
                    Log.info("send newMessages to remote: $uid", "uid", u.uid);
                    a.newMessages(new MessageDescriptor[]{md});
                });
            }
        }
    }
}
