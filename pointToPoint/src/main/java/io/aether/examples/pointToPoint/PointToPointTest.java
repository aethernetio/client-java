package io.aether.examples.pointToPoint;

import io.aether.StandardUUIDs;
import io.aether.api.common.CryptoLib;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.cloud.client.MessageEventListener;
import io.aether.common.AccessGroupI;
import io.aether.logger.Log;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.RU;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.streams.Value;
import io.aether.utils.streams.ValueOfData;

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
    public ClientStateInMemory serviceConfig;

    {
        registrationUri.add(URI.create("tcp://registration.aethernet.io:9010"));
    }

    public AFuture p2p() {
        var parent = UUID.fromString("B1AC52C8-8D94-BD39-4C01-A631AC594165");
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.SODIUM);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        clientConfig1.getPingDuration().set(100L);
        clientConfig2.getPingDuration().set(100L);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1, "client1");
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2, "client2");

        AFuture testDoneFuture = AFuture.make();
        client1.startFuture.to(() -> Log.info("client is registered uid2: $uid1", "uid1", client1.getUid()));
        client2.startFuture.to(() -> Log.info("client is registered uid2: $uid2", "uid2", client2.getUid()));
        client1.startFuture.onError(Log::error);
        client2.startFuture.onError(Log::error);
        client1.startFuture.onCancel(()->Log.error("cancel"));
        client2.startFuture.onCancel(()->Log.error("cancel"));
        client2.startFuture.onError(Log::error);
        AFuture.all(client1.startFuture, client2.startFuture).to(() -> {
            Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
            AFuture checkReceiveMessage = AFuture.make();
            var message = new byte[]{1, 2, 3, 4};
            client2.onMessage((uid, msg) -> {
                if(checkReceiveMessage.tryDone()){
                    Log.info("First message confirm");
                }else{
                    Log.warn("Second message confirm");
                }
            });
            Log.info("START two clients!");
            Thread.currentThread().setName("MAIN THREAD");
            var m = Value.of(message).timeout(30000, (v) -> {
                Log.error("timeout message: $v", "v", v);
            });

            client1.sendMessage(client2.getUid(), m);

            checkReceiveMessage.to(() -> {
                Log.info("TEST IS DONE!");
                client1.destroy(false).to(() -> {
                    client2.destroy(false).to(testDoneFuture)
                            .onError(testDoneFuture::error);
                }).onError(testDoneFuture::error);
            }).onError(testDoneFuture::error);

        }).onError(testDoneFuture::error);

        return testDoneFuture;
    }

    //    @Test
    public AFuture timeOneMessage() { // ИСПРАВЛЕНО: удален дженерик
        var parent = UUID.fromString("9128C7D0-4BA1-8D1C-AC9F-71074A014FC5");
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2);

        AFuture testDoneFuture = AFuture.make();

        AFuture.all(client1.startFuture, client2.startFuture).to(() -> {
            var ch1 = client1.getMessageNode(client2.getUid());
            final var total = 1000000L;
            AtomicLong receiveCounter = new AtomicLong(0);
            client2.onClientStream((g) -> {
                g.toConsumer( d -> {
                    receiveCounter.addAndGet(d.length);
                });
            });
            var data = new byte[10000];
            var timeBegin = RU.time();

            while (receiveCounter.get() < total) {
                var v = Value.ofForce(data);
                boolean[] abortFlag = new boolean[1];

                v.onReject((owner, id) -> {
                    abortFlag[0] = true;
                });

                ch1.send(v);
                if (!abortFlag[0]) {
                    RU.sleep(10);
                }
            }
            var timeEnd = RU.time();
            var duration = timeEnd - timeBegin;
            Log.info("Total time: $time. Speed: $speed kB/s",
                    "time", duration,
                    "timeBegin", timeBegin,
                    "timeEnd", timeEnd,
                    "speed", (total * data.length * 8.0) / (duration / 1000.0) / 1024.0);

            testDoneFuture.done();

        }).onError(testDoneFuture::error);

        return testDoneFuture;
    }

    public AFuture p2pAndBack() { // ИСПРАВЛЕНО: удален дженерик
        var parent = UUID.fromString("B0600A31-1ACC-BB39-35C9-F1476C1F40E2");
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1, "client1");
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2, "client2");

        AFuture testDoneFuture = AFuture.make();

        AFuture.all(client1.startFuture, client2.startFuture).to(() -> {
            Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
            AFuture checkReceiveMessageBack = AFuture.make();
            var message = new byte[]{1, 2, 3, 4};
            var messageBack = new byte[]{1, 1, 1, 1};
            client2.onClientStream((st) -> {
                st.toConsumer( newMessage -> {
                    st.send(Value.of(messageBack));
                });
            });
            client1.onClientStream((st) -> {
                st.toConsumer( newMessage -> {
                    checkReceiveMessageBack.done();
                });
            });
            Log.info("START two clients!");
            var chToc2 = client1.getMessageNode(client2.getUid());
            Thread.currentThread().setName("MAIN THREAD");
            chToc2.send(Value.ofForce(message));

            checkReceiveMessageBack.to(() -> {
                Log.info("TEST IS DONE!");
                client1.destroy(true).to(() -> {
                    client2.destroy(true).to(testDoneFuture::done)
                            .onError(testDoneFuture::error);
                }).onError(testDoneFuture::error);
            }).onError(testDoneFuture::error);

        }).onError(testDoneFuture::error);

        return testDoneFuture;
    }

    public AFuture pointToPointWithService() { // ИСПРАВЛЕНО: удален дженерик
        var parent = UUID.fromString("A8348A48-64CC-A8EF-6902-090F446247C8");
        if (serviceConfig == null)
            serviceConfig = new ClientStateInMemory(parent, registrationUri);
        AetherCloudClient service = new AetherCloudClient(serviceConfig);

        AFuture testDoneFuture = AFuture.make();

        service.startFuture.to(() -> {
            Log.info("service is registered");
            Set<UUID> allChildren = new ConcurrentHashSet<>();
            ARFuture<AccessGroupI> groupFuture = service.createAccessGroup();
            service.onNewChildren((u) -> {
                groupFuture.to(group -> {
                    service.getClientApi(u, a -> {
                        a.addAccessGroup(group.getId()).to(f -> {
                            allChildren.add(u);
                            Log.info("NEW CHILD DONE: $uid", "uid", u, "result", f);
                        }).onError(e -> Log.error("Failed to add access group: $e", "e", e));
                    });
                    Log.info("NEW CHILD: $uid", "uid", u);
                });
            });
            var parentUid = service.getUid();
            assert parentUid != null;
            if (clientConfig1 == null) clientConfig1 = new ClientStateInMemory(parentUid, registrationUri);
            if (clientConfig2 == null) clientConfig2 = new ClientStateInMemory(parentUid, registrationUri);
            AetherCloudClient client1 = new AetherCloudClient(clientConfig1);
            AetherCloudClient client2 = new AetherCloudClient(clientConfig2);

            AFuture.all(client1.startFuture, client2.startFuture).to(() -> {
                Log.info("clients is registered");
                AFuture checkReceiveMessage = AFuture.make();
                var message = new byte[]{0, 0, 0, 0};
                client2.onClientStream((st) -> {
                    st.toConsumer( newMessage -> {
                        checkReceiveMessage.done();
                    });
                });
                Log.info("START!");
                var chToc2 = client1.getMessageNode(client2.getUid());
                chToc2.send(Value.ofForce(message));

                checkReceiveMessage.to(() -> {
                    client1.destroy(true).to(() -> {
                        client2.destroy(true).to(testDoneFuture::done)
                                .onError(testDoneFuture::error);
                    }).onError(testDoneFuture::error);
                }).onError(testDoneFuture::error);
            }).onError(testDoneFuture::error);
        }).onError(testDoneFuture::error);

        return testDoneFuture;
    }

    public AFuture p2pMany() { // ИСПРАВЛЕНО: удален дженерик
        var parent = UUID.fromString("d1401d8c-674d-4948-8d41-c395334ad391");
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1, "client1");
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2, "client2");

        AFuture testDoneFuture = AFuture.make();

        AFuture.all(client1.startFuture, client2.startFuture).to(() -> {
            Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
            AFuture checkReceiveMessage = AFuture.make();
            var message = new byte[]{1, 2, 3, 4};
            int ITERATIONS = 10;
            List<MValue> values = new ArrayList<>();
            for (int i = 0; i < ITERATIONS; i++) {
                values.add(new MValue(message));
            }
            AtomicInteger counter = new AtomicInteger(ITERATIONS);
            client2.onClientStream((st) -> {
                Log.debug("onClientStream");
                st.toConsumer( newMessage -> {
                    Log.debug("on new message");
                    if (counter.addAndGet(-1) == 0) {
                        checkReceiveMessage.done();
                    }
                });
            });
            Log.info("START two clients!");
            var chToc2n = client1.openStreamToClientDetails(client2.getUid(), MessageEventListener.DEFAULT);
            Thread.currentThread().setName("MAIN THREAD");
            for (var v : values) {
                chToc2n.send(v);
            }

            checkReceiveMessage.to(() -> {
                Log.info("TEST IS DONE!");
                client1.destroy(true).to(() -> {
                    client2.destroy(true).to(testDoneFuture::done)
                            .onError(testDoneFuture::error);
                }).onError(testDoneFuture::error);
            }).onError(testDoneFuture::error);

        }).onError(testDoneFuture::error);

        return testDoneFuture;
    }

    private AFuture startIteration2() { // ИСПРАВЛЕНО: удален дженерик
        AFuture iteration2DoneFuture = AFuture.make();
        // iteration 2
        {
            if (clientConfig1 == null)
                clientConfig1 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
            if (clientConfig2 == null)
                clientConfig2 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
            AetherCloudClient client1 = new AetherCloudClient(clientConfig1, "client1_2");
            AetherCloudClient client2 = new AetherCloudClient(clientConfig2, "client2_2");

            AFuture.all(client1.startFuture, client2.startFuture).to(() -> {
                Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
                AFuture checkReceiveMessage = AFuture.make();
                var message = new byte[]{2, 2, 2, 2};
                client2.onClientStream((st) -> {
                    st.toConsumer( newMessage -> {
                        checkReceiveMessage.done();
                    });
                });
                Log.info("START two clients!");
                var chToc2 = client1.getMessageNode(client2.getUid());
                Thread.currentThread().setName("MAIN THREAD");
                chToc2.send(Value.ofForce(message));

                checkReceiveMessage.to(() -> {
                    Log.info("TEST IS DONE!");
                    AFuture.all(client1.destroy(true), client2.destroy(true)).to(iteration2DoneFuture::done)
                            .onError(iteration2DoneFuture::error);
                }).onError(iteration2DoneFuture::error);
            }).onError(iteration2DoneFuture::error);
        }
        return iteration2DoneFuture;
    }

    public AFuture pointToPointWithReconnect() { // ИСПРАВЛЕНО: удален дженерик
        var parent = UUID.fromString("84AE8BD0-2BE4-FF65-406C-B1B655444D54");
        clientConfig1 = new ClientStateInMemory(parent, registrationUri);
        clientConfig2 = new ClientStateInMemory(parent, registrationUri);

        AFuture testDoneFuture = AFuture.make();

        {//iteration 1
            if (clientConfig1 == null)
                clientConfig1 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
            if (clientConfig2 == null)
                clientConfig2 = new ClientStateInMemory(StandardUUIDs.TEST_UID, registrationUri, null, CryptoLib.HYDROGEN);
            AetherCloudClient client1 = new AetherCloudClient(clientConfig1, "client1");
            AetherCloudClient client2 = new AetherCloudClient(clientConfig2, "client2");

            AFuture.all(client1.startFuture, client2.startFuture).to(() -> {
                Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
                AFuture checkReceiveMessage = AFuture.make();
                var message = new byte[]{1, 1, 1, 1};
                client2.onClientStream((st) -> {
                    st.toConsumer(newMessage -> {
                        checkReceiveMessage.done();
                    });
                });
                Log.info("START two clients!");
                var chToc2 = client1.getMessageNode(client2.getUid());
                Thread.currentThread().setName("MAIN THREAD");
                chToc2.send(Value.ofForce(message));

                checkReceiveMessage.to(() -> {
                    Log.info("TEST IS DONE!");
                    var f1 = client1.destroy(true);
                    var f2 = client2.destroy(true);

                    AFuture.all(f1, f2).onError(t -> {
                        // Передаем ошибку в final Future
                        testDoneFuture.error(new IllegalStateException("Failed to destroy clients after iteration 1: " + f1 + ":" + f2, t));
                    }).to(() -> {
                        Log.debug("ITERATION 2 START");
                        // Запуск второй итерации и передача ее Future для финализации общего Future
                        startIteration2().to(testDoneFuture::done)
                                .onError(testDoneFuture::error);
                    });
                }).onError(testDoneFuture::error);
            }).onError(testDoneFuture::error);
        }

        return testDoneFuture;
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