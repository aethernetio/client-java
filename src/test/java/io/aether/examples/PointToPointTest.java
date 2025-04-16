package io.aether.examples;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.common.AccessGroupI;
import io.aether.crypt.CryptoLib;
import io.aether.logger.Log;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.streams.Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PointToPointTest {
    public final List<URI> registrationUri = new ArrayList<>();
    public ClientStateInMemory clientConfig1;
    public ClientStateInMemory clientConfig2;
    public byte[] config1Data;
    public byte[] config2Data;
    public ClientStateInMemory serviceConfig;

    {
        registrationUri.add(URI.create("tcp://registration.aethernet.io:9010"));
    }

    @Test
    public void p2p() {
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
        Assertions.assertTrue(AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(1000));
        Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
        AFuture checkReceiveMessage = new AFuture();
        var message = new byte[]{1, 2, 3, 4};
        client2.onClientStream((st) -> {
            Assertions.assertEquals(client1.getUid(), st.getConsumerUUID());
            st.up().toConsumer(newMessage -> {
                Assertions.assertArrayEquals(newMessage, message);
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
        Assertions.assertTrue(checkReceiveMessage.waitDoneSeconds(1000));
        client1.stop(5);
        client2.stop(5);
    }
//    @Test
    public void timeOneMessage() {
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
        Assertions.assertTrue(AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(10));
        var ch1 = client1.openStreamToClient(client2.getUid());
        final var total = 1000000L;
        AtomicLong receiveCounter = new AtomicLong(0);
        client2.onClientStream((g) -> {
            g.up().toConsumer(d -> {
                receiveCounter.addAndGet(d.length);
            });
        });
        client2.ping();
        var data = new byte[10000];
        var timeBegin = RU.time();
        while (receiveCounter.get() < total) {
            if (!ch1.isWritable()) {
                RU.sleep(1);
                continue;
            }
            ch1.send(Value.ofForce(data));
        }
        var timeEnd = RU.time();
        var duration = timeBegin - timeEnd;
        Log.info("Total time: $time. Speed: $speed kB/s",
                "time", duration,
                "timeBegin", timeBegin,
                "timeEnd", timeEnd,
                "speed", (total * 8.0) / (duration / 1000.0));
    }
    @Test
    public void p2pAndBack() {
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
        Assertions.assertTrue(AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(1000));
        Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
        AFuture checkReceiveMessageBack = new AFuture();
        var message = new byte[]{1, 2, 3, 4};
        var messageBack = new byte[]{1, 1, 1, 1};
        client2.onClientStream((st) -> {
            Assertions.assertEquals(client1.getUid(), st.getConsumerUUID());
            st.up().toConsumer(newMessage -> {
                Assertions.assertArrayEquals(newMessage, message);
                st.up().send(Value.of(messageBack));
            });
        });
        client1.onClientStream((st) -> {
            Assertions.assertEquals(client2.getUid(), st.getConsumerUUID());
            st.up().toConsumer(newMessage -> {
                Assertions.assertArrayEquals(newMessage, messageBack);
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
        Assertions.assertTrue(checkReceiveMessageBack.waitDoneSeconds(10));
        client1.stop(5);
        client2.stop(5);
    }

    @Test
    public void pointToPointWithService() {
        if (serviceConfig == null)
            serviceConfig = new ClientStateInMemory(StandardUUIDs.ANONYMOUS_UID, registrationUri);
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
        Assertions.assertTrue(AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(10));
        Log.info("clients is registered");
        AFuture checkReceiveMessage = new AFuture();
        var message = new byte[]{0, 0, 0, 0};
        client2.onClientStream((st) -> {
            Assertions.assertEquals(client1.getUid(), st.getConsumerUUID());
            st.up().toConsumer(newMessage -> {
                Assertions.assertArrayEquals(newMessage, message);
                checkReceiveMessage.done();
            });
        });
        Log.info("START!");
        var chToc2 = client1.openStreamToClient(client2.getUid());
        chToc2.send(Value.ofForce(message));

        Assertions.assertTrue(checkReceiveMessage.waitDoneSeconds(100));
        RU.sleep(20000);
        client1.stop(5);
        client2.stop(5);
    }

    @Test
    public void p2pMany() {
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
        Assertions.assertTrue(AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(1000));
        Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
        AFuture checkReceiveMessage = new AFuture();
        var message = new byte[]{1, 2, 3, 4};
        AtomicInteger counter = new AtomicInteger(1000);
        client2.onClientStream((st) -> {
            Assertions.assertEquals(client1.getUid(), st.getConsumerUUID());
            st.up().toConsumer(newMessage -> {
                Assertions.assertArrayEquals(newMessage, message);
                if (counter.addAndGet(-1) == 0) {
                    checkReceiveMessage.done();
                }
            });
        });
        Log.info("START two clients!");
        var chToc2 = client1.openStreamToClient(client2.getUid());
        Thread.currentThread().setName("MAIN THREAD");
        for (int i = 0; i < 1000; i++) {
            chToc2.send(Value.ofForce(message));
        }
        checkReceiveMessage.to(() -> {
            Log.info("TEST IS DONE!");
        });
        Assertions.assertTrue(checkReceiveMessage.waitDoneSeconds(1000));
        client1.stop(5);
        client2.stop(5);
    }

    @Test
    public void pointToPointWithReconnect() {
        clientConfig1 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri);
        clientConfig2 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri);
        {//iteration 1
            if (clientConfig1 == null)
                clientConfig1 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
            if (clientConfig2 == null)
                clientConfig2 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
            AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
            AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
            Assertions.assertTrue(AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(1000));
            Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
            AFuture checkReceiveMessage = new AFuture();
            var message = new byte[]{1, 2, 3, 4};
            client2.onClientStream((st) -> {
                Assertions.assertEquals(client1.getUid(), st.getConsumerUUID());
                st.up().toConsumer(newMessage -> {
                    Assertions.assertArrayEquals(newMessage, message);
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
            Assertions.assertTrue(checkReceiveMessage.waitDoneSeconds(1000));
            client1.stop(5);
            client2.stop(5);
        }
        Log.debug("ITERATION 2");
        {//iteration 2
            if (clientConfig1 == null)
                clientConfig1 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
            if (clientConfig2 == null)
                clientConfig2 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
            AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
            AetherCloudClient client2 = new AetherCloudClient(clientConfig2);
            Assertions.assertTrue(AFuture.all(client1.startFuture, client2.startFuture).waitDoneSeconds(1000));
            Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
            AFuture checkReceiveMessage = new AFuture();
            var message = new byte[]{1, 1, 1, 1};
            client2.onClientStream((st) -> {
                Assertions.assertEquals(client1.getUid(), st.getConsumerUUID());
                st.up().toConsumer(newMessage -> {
                    Assertions.assertArrayEquals(newMessage, message);
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
            Assertions.assertTrue(checkReceiveMessage.waitDoneSeconds(1000));
            client1.stop(5);
            client2.stop(5);
        }
    }
}
