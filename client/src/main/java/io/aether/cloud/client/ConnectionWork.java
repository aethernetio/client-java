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
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ConnectionWork extends Connection<ClientApiUnsafe, LoginApi, LoginApiRemote> implements ClientApiUnsafe {
    //region counters
    public final AtomicLong lastBackPing = new AtomicLong(Long.MAX_VALUE);
    public final AMFuture<ConnectionWork> ready = new AMFuture<>();
    private final ServerDescriptor serverDescriptor;
    final private AtomicBoolean inProcess = new AtomicBoolean();
    boolean basicStatus;
    long lastWorkTime;
    final ClientApiSafe apiSafe = new MyClientApiSafe(client);
    final FastApiContext apiSafeCtx;
    final CryptoEngine cryptoEngine;
    final RemoteApiFuture<AuthorizedApi> remoteApiFuture = new RemoteApiFuture<>();

    public ConnectionWork(AetherCloudClient client, ServerDescriptor s) {
        super(client, s.getIpAddress().getURI(AetherCodec.UDP), ClientApiUnsafe.META, LoginApi.META);
        cryptoEngine = client.getCryptoEngineForServer(s.getId());
        serverDescriptor = s;
        this.basicStatus = false;
        remoteApiFuture.addPermanent(this::flushBackgroundRequests);
        apiSafeCtx = new FastApiContext() {
            @Override
            public AFuture flush() {
                var e = new LoginStream(this, cryptoEngine::encrypt, remoteApiFuture::flush);
                if (e.data == null || e.data.length == 0) return AFuture.completed();
                rootApi.loginByAlias(client.getAlias(), e);
                return rootApiContext.flush();
            }
        };
    }

    public final Queue<Message> messageNodeQueue = new ConcurrentLinkedQueue<>();

    private void flushBackgroundRequests(AuthorizedApi a) {
        List<UUID> requestCloud = new ArrayList<>(client.requestCloud);
        if (!requestCloud.isEmpty()) {
            client.requestCloud.removeAll(requestCloud);
            a.resolverClouds(requestCloud.toArray(new UUID[0]));
        }
        ShortList requestServers = new ShortArrayList(client.requestServers);
        if (!requestServers.isEmpty()) {
            client.requestServers.removeAll(requestServers);
            a.resolverServers(Flow.flow(requestServers).mapToShort(Short::shortValue).toArray());
        }
        List<Message> mm = new ArrayList<>();
        RU.readAll(messageNodeQueue, mm::add);
        a.sendMessages(mm.toArray(new Message[0]));
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
            client.servers.set(v);
        }

        @Override
        public void sendCloud(UUID uid, Cloud cloud) {
            client.clouds.set(new UUIDAndCloud(uid, cloud));
        }

        @Override
        public void newChild(UUID uid) {
            client.onNewChild.fire(uid);
        }

    }
}
