package io.aether.cloud.client;

import io.aether.api.clientserverapi.*;
import io.aether.api.common.AetherCodec;
import io.aether.api.common.Cloud;
import io.aether.api.common.ServerDescriptor;
import io.aether.api.common.UUIDAndCloud;
import io.aether.crypto.CryptoEngine;
import io.aether.logger.Log;
import io.aether.net.fastMeta.FastApiContext;
import io.aether.net.fastMeta.RemoteApiFuture;
import io.aether.utils.RU;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.AFuture;
import io.aether.utils.slots.AMFuture;
import io.aether.utils.streams.Value;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ConnectionWork extends Connection<ClientApiUnsafe, LoginApi, LoginApiRemote> implements ClientApiUnsafe {
    //region counters
    public final AtomicLong lastBackPing = new AtomicLong(Long.MAX_VALUE);
    public final AMFuture<ConnectionWork> ready = new AMFuture<>();
    final ClientApiSafe apiSafe = new MyClientApiSafe(client);
    final FastApiContext apiSafeCtx;
    final CryptoEngine cryptoEngine;
    final RemoteApiFuture<AuthorizedApi> remoteApiFuture = new RemoteApiFuture<>();
    private final ServerDescriptor serverDescriptor;
    final private AtomicBoolean inProcess = new AtomicBoolean();
    boolean basicStatus;
    long lastWorkTime;

    public ConnectionWork(AetherCloudClient client, ServerDescriptor s) {
        super(client, s.getIpAddress().getURI(AetherCodec.TCP), ClientApiUnsafe.META, LoginApi.META);
        cryptoEngine = client.getCryptoEngineForServer(s.getId());
        serverDescriptor = s;
        this.basicStatus = false;
        remoteApiFuture.addPermanent((a, f) -> {
            try (var ln = Log.context(client.logClientContext)) {
                flushBackgroundRequests(a, f);
            }
        });
        apiSafeCtx = new FastApiContext() {
            @Override
            public AFuture flush() {
                if (remoteApiFuture.isEmpty()) return AFuture.completed();
                var e = new LoginStream(this, cryptoEngine::encrypt, remoteApiFuture);
                if (e.data == null || e.data.length == 0) return AFuture.completed();
                rootApi.loginByAlias(client.getAlias(), e);
                return rootApiContext.flush();
            }
        };
    }

    private void flushBackgroundRequests(AuthorizedApi a, AFuture sendFuture) {
        if (!client.requestCloud.isEmpty()) {
            List<UUID> requestCloud = new ArrayList<>(client.requestCloud);
            if (!requestCloud.isEmpty()) {
                client.requestCloud.removeAll(requestCloud);
                a.resolverClouds(requestCloud.toArray(new UUID[0]));
            }
        }
        if (!client.requestServers.isEmpty()) {
            ShortList requestServers = new ShortArrayList(client.requestServers);
            if (!requestServers.isEmpty()) {
                client.requestServers.removeAll(requestServers);
                a.resolverServers(Flow.flow(requestServers).mapToShort(Short::shortValue).toArray());
            }
        }
        List<Message> messageForSend = null;
        for (var m : client.messageNodeMap.values()) {
            if (m.connectionsOut.contains(this)) {
                if (messageForSend == null) {
                    messageForSend = new ObjectArrayList<>();
                }
                List<Value<byte[]>> mm = new ArrayList<>();
                RU.readAll(m.bufferOut, mm::add);
                Log.debug("message client to server: $uidFrom -> $uidTo",
                        "uidFrom", client.getUid(),
                        "uidTo", m.consumer);
                Flow.flow(mm)
                        .map(v -> new Message(m.consumer, v.data()))
                        .toCollection(messageForSend);
                sendFuture.onCancel(() -> {
                    m.bufferOut.addAll(mm);
                });
            }
        }
        if (messageForSend != null) {
            a.sendMessages(messageForSend.toArray(new Message[0]));
        }
        var p = client.ping;
        if (p != 0) {
            a.ping(p);
        }
    }

    @Override
    public void sendSafeApiDataMulti(byte backId, LoginClientStream data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void sendSafeApiData(LoginClientStream data) {
        data.accept(apiSafeCtx, cryptoEngine::decrypt, apiSafe);
    }

    public ServerDescriptor getServerDescriptor() {
        return serverDescriptor;
    }

    @Override
    public String toString() {
        return "work(" + socketStreamClient + ")";
    }

    public void setBasic(boolean basic) {
        this.basicStatus = basic;
    }

    public long lifeTime() {
        return RU.time() - lastBackPing.get();
    }

    public void scheduledWork() {
        var t = RU.time();
        if ((t - lastWorkTime < client.getPingTime() || !inProcess.compareAndSet(false, true))) return;
        try {
            lastWorkTime = t;
            apiSafeCtx.flush();
        } finally {
            inProcess.set(false);
        }
    }

    public void flush() {
        if (!inProcess.compareAndSet(false, true)) return;
        var t = RU.time();
        try {
            lastWorkTime = t;
            apiSafeCtx.flush();
        } finally {
            inProcess.set(false);
        }
    }

    private static class MyClientApiSafe implements ClientApiSafe {
        private final AetherCloudClient client;

        public MyClientApiSafe(AetherCloudClient client) {
            this.client = client;
        }

        @Override
        public void changeParent(UUID uid) {

        }

        @Override
        public void changeAlias(UUID alias) {

        }

        @Override
        public void requestTelemetry() {

        }

        @Override
        public void sendMessages(Message[] msg) {
            for (var m : msg) {
                Log.trace("receive message $uid1 <- $uid2", "uid1", client.getUid(), "uid2", m.getUid());
                client.getMessageNode(m.getUid(), MessageEventListener.DEFAULT).sendMessageFromServerToClient(Value.of(m.getData()));
            }
        }

        @Override
        public void sendServerDescriptor(ServerDescriptor v) {
            client.servers.put((int) v.getId(), v);
        }

        @Override
        public void sendCloud(UUID uid, Cloud cloud) {
            client.clouds.put(uid, new UUIDAndCloud(uid, cloud));
        }

        @Override
        public void sendServerDescriptors(ServerDescriptor[] serverDescriptors) {
            for (var c : serverDescriptors) {
                sendServerDescriptor(c);
            }
        }

        @Override
        public void sendClouds(UUIDAndCloud[] clouds) {
            for (var c : clouds) {
                sendCloud(c.getUid(), c.getCloud());
            }
        }

        @Override
        public void newChild(UUID uid) {
            client.onNewChild.fire(uid);
        }

    }
}
