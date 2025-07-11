package io.aether.examples.pointToPoint;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.cloud.client.MessageEventListener;
import io.aether.common.AccessGroupI;
import io.aether.crypt.CryptoLib;
import io.aether.logger.Log;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.RU;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.streams.Value;
import io.aether.utils.streams.ValueOfData;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PointToPointTest {
    public final List<URI> registrationUri = new ArrayList<>();
    public ClientStateInMemory clientConfig1;
    public ClientStateInMemory clientConfig2;
    public ClientStateInMemory serviceConfig;

    {
        registrationUri.add(URI.create("tcp://registration.aethernet.io:9010"));
    }

    public void p2p() {
        var parent = UUID.fromString("B1AC52C8-8D94-BD39-4C01-A631AC594165");
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        clientConfig1.getPingDuration().set(9999999L);
        clientConfig2.getPingDuration().set(9999999L);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1, "client1");
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2, "client2");
        client1.startFuture.to(() -> Log.info("client is registered uid2: $uid1", "uid1", client1.getUid()));
        client2.startFuture.to(() -> Log.info("client is registered uid2: $uid2", "uid2", client2.getUid()));
        if (!AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(5)) {
            throw new IllegalStateException("Timeout connect to Aether");
        }
        Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
        AFuture checkReceiveMessage = new AFuture();
        var message = new byte[]{1, 2, 3, 4};
        client2.onMessage((uid, msg) -> checkReceiveMessage.done());
        Log.info("START two clients!");
        Thread.currentThread().setName("MAIN THREAD");
        AFuture f = client1.sendMessage(client2.getUid(), message);
        checkReceiveMessage.to(() -> {
            Log.info("TEST IS DONE!");
        });
        if (!checkReceiveMessage.waitDoneSeconds(1000)) {
            throw new IllegalStateException();
        }
        client1.destroy(true).waitDoneSeconds(5);
        client2.destroy(true).waitDoneSeconds(5);
    }

    //    @Test
    public void timeOneMessage() {
        var parent = UUID.fromString("9128C7D0-4BA1-8D1C-AC9F-71074A014FC5");
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
        AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(10);
        var ch1 = client1.openStreamToClient(client2.getUid());
        final var total = 1000000L;
        AtomicLong receiveCounter = new AtomicLong(0);
        client2.onClientStream((g) -> {
            g.up().toConsumer("timeOneMessage", d -> {
                receiveCounter.addAndGet(d.length);
            });
        });
        client2.ping();
        var data = new byte[10000];
        var timeBegin = RU.time();
        while (receiveCounter.get() < total) {
            var v = Value.ofForce(data);
            boolean[] abortFlag = new boolean[1];
            v.onReject((o, id) -> {
                abortFlag[0] = true;
            });
            ch1.send(v);
            if (!abortFlag[0]) {
                RU.sleep(10);
            }
        }
        var timeEnd = RU.time();
        var duration = timeBegin - timeEnd;
        Log.info("Total time: $time. Speed: $speed kB/s",
                "time", duration,
                "timeBegin", timeBegin,
                "timeEnd", timeEnd,
                "speed", (total * 8.0) / (duration / 1000.0));
    }

    public void p2pAndBack() {
        var parent = UUID.fromString("B0600A31-1ACC-BB39-35C9-F1476C1F40E2");
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1, "client1");
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2, "client2");
        if (!AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(1000)) {
            throw new IllegalStateException();
        }
        Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
        AFuture checkReceiveMessageBack = new AFuture();
        var message = new byte[]{1, 2, 3, 4};
        var messageBack = new byte[]{1, 1, 1, 1};
        client2.onClientStream((st) -> {
            st.up().toConsumer("p2pAndBack c2", newMessage -> {
                st.up().send(Value.of(messageBack));
            });
        });
        client1.onClientStream((st) -> {
            st.up().toConsumer("p2pAndBack c1", newMessage -> {
                checkReceiveMessageBack.done();
            });
        });
        Log.info("START two clients!");
        var chToc2 = client1.openStreamToClient(client2.getUid());
        Thread.currentThread().setName("MAIN THREAD");
        chToc2.send(Value.ofForce(message));
        checkReceiveMessageBack.to(() -> {
            Log.info("TEST IS DONE!");
        });
        if (!checkReceiveMessageBack.waitDoneSeconds(10)) {
            throw new IllegalStateException();
        }
        client1.destroy(true).waitDoneSeconds(5);
        client2.destroy(true).waitDoneSeconds(5);
    }

    public void pointToPointWithService() {
        var parent = UUID.fromString("A8348A48-64CC-A8EF-6902-090F446247C8");
        if (serviceConfig == null)
            serviceConfig = new ClientStateInMemory(parent, registrationUri);
        AetherCloudClient service = new AetherCloudClient(serviceConfig);
        if (!service.startFuture.waitDoneSeconds(2000)) {
            throw new IllegalStateException("timeout registration");
        }

        Log.info("service is registered");
        Set<UUID> allChildren = new ConcurrentHashSet<>();
        ARFuture<AccessGroupI> groupFuture = service.createAccessGroup();
        service.onNewChildren((u) -> {
            groupFuture.to(group -> {
                service.getClientApi(u).to(api -> {
                    api.run_flush(a -> a.addAccessGroup(group.getId()).to(f -> {
                        allChildren.add(u);
                        Log.info("NEW CHILD DONE: $uid", "uid", u, "result", f);
                    }));
                }, 5, () -> Log.warn("timeout get client api for $uid", "uid", u));
                Log.info("NEW CHILD: $uid", "uid", u);
            });
        });
        var parentUid = service.getUid();
        assert parentUid != null;
        if (clientConfig1 == null) clientConfig1 = new ClientStateInMemory(parentUid, registrationUri);
        if (clientConfig2 == null) clientConfig2 = new ClientStateInMemory(parentUid, registrationUri);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
        AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(10);
        Log.info("clients is registered");
        AFuture checkReceiveMessage = new AFuture();
        var message = new byte[]{0, 0, 0, 0};
        client2.onClientStream((st) -> {
            st.up().toConsumer("pointToPointWithService c2", newMessage -> {
                checkReceiveMessage.done();
            });
        });
        Log.info("START!");
        var chToc2 = client1.openStreamToClient(client2.getUid());
        chToc2.send(Value.ofForce(message));

        if (!checkReceiveMessage.waitDoneSeconds(10)) {
            throw new IllegalStateException();
        }
        client1.destroy(true).waitDoneSeconds(5);
        client2.destroy(true).waitDoneSeconds(5);
    }

    public void p2pMany() {
        var parent = UUID.fromString("d1401d8c-674d-4948-8d41-c395334ad391");
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1, "client1");
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2, "client2");
        AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(1000);
        Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
        AFuture checkReceiveMessage = new AFuture();
        var message = new byte[]{1, 2, 3, 4};
        int ITERATIONS = 10;
        List<MValue> values = new ArrayList<>();
        for (int i = 0; i < ITERATIONS; i++) {
            values.add(new MValue(message));
        }
        AtomicInteger counter = new AtomicInteger(ITERATIONS);
        client2.onClientStream((st) -> {
            Log.debug("onClientStream");
            st.up().toConsumer("p2pMany c2", newMessage -> {
                Log.debug("on new message");
                if (counter.addAndGet(-1) == 0) {
                    checkReceiveMessage.done();
                }
            });
        });
        Log.info("START two clients!");
        var chToc2n = client1.openStreamToClientDetails(client2.getUid(), MessageEventListener.DEFAULT);
        var chToc2 = chToc2n.up();
        Thread.currentThread().setName("MAIN THREAD");
        for (var v : values) {
            chToc2.send(v);
        }
        checkReceiveMessage.to(() -> {
            Log.info("TEST IS DONE!");
        });
        if (!checkReceiveMessage.waitDoneSeconds(5)) {
            Flow.flow(values).map(e -> (e.abort ? "abort" : (e.drop ? "drop" : "")) + ": " + Flow.flow(e.enters).mapToString().join(", ")).distinct().to(System.out::println);
            throw new IllegalStateException();
        }
        client1.destroy(true).waitDoneSeconds(5);
        client2.destroy(true).waitDoneSeconds(5);
    }

    public void pointToPointWithReconnect() {
        var parent = UUID.fromString("84AE8BD0-2BE4-FF65-406C-B1B655444D54");
        clientConfig1 = new ClientStateInMemory(parent, registrationUri);
        clientConfig2 = new ClientStateInMemory(parent, registrationUri);
        {//iteration 1
            if (clientConfig1 == null)
                clientConfig1 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
            if (clientConfig2 == null)
                clientConfig2 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
            AetherCloudClient client1 = new AetherCloudClient(clientConfig1, "client1");
            AetherCloudClient client2 = new AetherCloudClient(clientConfig2, "client2");
            AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(1000);
            Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
            AFuture checkReceiveMessage = new AFuture();
            var message = new byte[]{1, 1, 1, 1};
            client2.onClientStream((st) -> {
                st.up().toConsumer("pointToPointWithReconnect c2", newMessage -> {
                    checkReceiveMessage.done();
                });
            });
            Log.info("START two clients!");
            var chToc2 = client1.openStreamToClient(client2.getUid());
            Thread.currentThread().setName("MAIN THREAD");
            chToc2.send(Value.ofForce(message));
            checkReceiveMessage.to(() -> {
                Log.info("TEST IS DONE!");
            });
            if (!checkReceiveMessage.waitDoneSeconds(10)) {
                throw new IllegalStateException();
            }
            var f1 = client1.destroy(true);
            var f2 = client2.destroy(true);
            if (!f1.waitDoneSeconds(5) ||
                !f2.waitDoneSeconds(5)) {
                throw new IllegalStateException(f1 + ":" + f2);
            }
        }
        Log.debug("ITERATION 2");
        {//iteration 2
            if (clientConfig1 == null)
                clientConfig1 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
            if (clientConfig2 == null)
                clientConfig2 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
            AetherCloudClient client1 = new AetherCloudClient(clientConfig1, "client1_2");
            AetherCloudClient client2 = new AetherCloudClient(clientConfig2, "client2_2");
            AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(1000);
            Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
            AFuture checkReceiveMessage = new AFuture();
            var message = new byte[]{2, 2, 2, 2};
            client2.onClientStream((st) -> {
                st.up().toConsumer("pointToPointWithReconnect c2 it2", newMessage -> {
                    checkReceiveMessage.done();
                });
            });
            Log.info("START two clients!");
            var chToc2 = client1.openStreamToClient(client2.getUid());
            Thread.currentThread().setName("MAIN THREAD");
            chToc2.send(Value.ofForce(message));
            checkReceiveMessage.to(() -> {
                Log.info("TEST IS DONE!");
            });
            if (!checkReceiveMessage.waitDoneSeconds(10)) {
                throw new IllegalStateException();
            }
            AFuture.all(client1.destroy(true),client2.destroy(true)).waitDoneSeconds(5);
        }
    }

    private static class MValue extends ValueOfData<byte[]> {
        public final List<Object> enters;
        volatile boolean abort;
        volatile boolean drop;

        public MValue(byte[] message) {
            super(message);
            enters = new ArrayList<>();
        }

        @Override
        public void reject(Object owner, long blockId) {
            enters.add(owner);
            abort = true;
        }

        @Override
        public void success(Object owner) {
            enters.add(owner);
            drop = true;
        }

        @Override
        public void enter(Object owner) {
            enters.add(owner);
        }
    }
}
