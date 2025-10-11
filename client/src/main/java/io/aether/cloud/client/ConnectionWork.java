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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ConnectionWork extends Connection<ClientApiUnsafe, LoginApiRemote> implements ClientApiUnsafe {
    public final AtomicLong lastBackPing = new AtomicLong(Long.MAX_VALUE);
    public final AMFuture<ConnectionWork> ready = new AMFuture<>();
    final ClientApiSafe apiSafe = new MyClientApiSafe(client);
    final FastApiContext apiSafeCtx;
    final CryptoEngine cryptoEngine;
    final RemoteApiFuture<AuthorizedApiRemote> remoteApiFuture = new RemoteApiFuture<>(AuthorizedApiRemote.META);
    private final ServerDescriptor serverDescriptor;
    final private AtomicBoolean inProcess = new AtomicBoolean();
    boolean basicStatus;
    long lastWorkTime;
    volatile boolean firstAuth;

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
            public void flush(AFuture sendFuture) {
                // Используем isRequestsFor(this) для проверки, есть ли у этого ConnectionWork
                // запросы, которые он должен отправить (новые или таймаутнувшие).
                if (remoteApiFuture.isEmpty() && !client.clouds.isRequestsFor(ConnectionWork.this) && !client.servers.isRequestsFor(ConnectionWork.this)) {
                    sendFuture.done();
                    return;
                }

                getRootApiFuture().to(api -> {
                    remoteApiFuture.executeAll(this, sendFuture);
                    var d = remoteDataToArray();
                    if (d.length == 0) {
                        sendFuture.done();
                        return;
                    }
                    var loginStream = new LoginStream(cryptoEngine::encrypt, d);
                    api.loginByAlias(client.getAlias(), loginStream);
                    rootApi.flush(sendFuture);
                }, sendFuture::error).onCancel(sendFuture::cancel);

            }
        };
    }

    private void flushBackgroundRequests(AuthorizedApi a, AFuture sendFuture) {
        UUID[] requestCloud = client.clouds.getRequestsFor(UUID.class, this);
        if (requestCloud.length > 0) {
            a.resolverClouds(requestCloud);
        }

        Integer[] requestServers = client.servers.getRequestsFor(Integer.class, this);
        if (requestServers.length > 0) {
            // Преобразование Integer[] в short[] для API
            short[] serverIds = new short[requestServers.length];
            for (int i = 0; i < requestServers.length; i++) {
                serverIds[i] = requestServers[i].shortValue();
            }
            a.resolverServers(serverIds);
        }

        // 3. Message Stream Logic (unchanged)
        List<Message> messageForSend = null;
        for (var m : client.messageNodeMap.values()) {
            if (m.connectionsOut.contains(this)) {
                List<Value<byte[]>> mm = new ArrayList<>();
                RU.readAll(m.bufferOut, mm::add);
                if (!mm.isEmpty()) {
                    Log.debug("message send client to server: $uidFrom -> $uidTo",
                            "uidFrom", client.getUid(),
                            "uidTo", m.consumer);
                    if (messageForSend == null) {
                        messageForSend = new ObjectArrayList<>();
                    }
                    Flow.flow(mm)
                            .map(v -> new Message(m.consumer, v.data()))
                            .toCollection(messageForSend);
                    sendFuture.to(() -> {
                        for (var v : mm) {
                            v.success(this);
                        }
                    });
                    sendFuture.onCancel(() -> {
                        m.bufferOut.addAll(mm);
                    });
                }
            }
        }
        if (messageForSend != null && !messageForSend.isEmpty()) {
            a.sendMessages(messageForSend.toArray(new Message[0]));
        }

        // 4. Ping Logic (unchanged)
        if (!firstAuth) {
            a.ping(0).to(() -> {
                firstAuth = true;
            });
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
        return "work(" + serverDescriptor.getIpAddress().getURI(AetherCodec.TCP) + ")";
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
        lastWorkTime = t;
        var f = AFuture.make();
        f.addListener(v -> inProcess.set(false));
        f.timeout(2, () -> {
            Log.warn("connection work flush 1 timeout");
        });
        apiSafeCtx.flush(f);
    }

    public void flush() {
        if (!inProcess.compareAndSet(false, true)) return;
        lastWorkTime = RU.time();
        var f = AFuture.make();
        f.addListener(v -> inProcess.set(false));
        f.timeout(2, () -> {
            Log.warn("connection work flush 2 timeout");
        });
        apiSafeCtx.flush(f);
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
            Log.trace("receive messages: $count", "count", msg.length);
            for (var m : msg) {
                Log.trace("receive message $uid1 <- $uid2", "uid1", client.getUid(), "uid2", m.getUid());
                client.getMessageNode(m.getUid(), MessageEventListener.DEFAULT).sendMessageFromServerToClient(Value.of(m.getData()));
            }
        }

        @Override
        public void sendServerDescriptor(ServerDescriptor v) {
            client.servers.putResolved((int) v.getId(), v);
        }

        @Override
        public void sendCloud(UUID uid, Cloud cloud) {
            client.setCloud(uid, cloud);
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