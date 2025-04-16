package io.aether.examples.plainChat;

import io.aether.StandardUUIDs;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatService {
    public static final ARFuture<UUID> uid = new ARFuture<>();
    public final AetherCloudClient aether;
    public final Map<UUID, ApiGate<ServiceServerApi, ServiceClientApi>> clients = new ConcurrentHashMap<>();
    private final Map<UUID, UserDescriptor> users = new ConcurrentHashMap<>();

    public ChatService(List<URI> registrationUri) {
        aether = new AetherCloudClient(new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri))
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
            var api=s.up().bufferAutoFlush().toApi(ServiceServerApi.META, ServiceClientApi.META,new MyServiceServerApi(s.getConsumerUUID()));
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
            var u = new UserDescriptor(uid, name);
            for (var uu : users.values()) {
                var r = clients.get(uu.uid);
                if (r != null) {
                    r.getRemoteApi().run_flush(a ->{
                        a.addNewUsers(new UserDescriptor[]{u});
                    });
                }
            }
            remoteApi.run_flush(a ->{
                a.addNewUsers(Flow.flow(users.values()).toArray(UserDescriptor.class));
            });
            users.put(uid, u);
        }

        @Override
        public void sendMessage(String msg) {
            var md = new MessageDescriptor(uid, msg);
            for (var u : users.values()) {
                var r = clients.get(u.uid);
                r.getRemoteApi().run_flush(a -> a.newMessages(new MessageDescriptor[]{md}));
            }
        }
    }
}
