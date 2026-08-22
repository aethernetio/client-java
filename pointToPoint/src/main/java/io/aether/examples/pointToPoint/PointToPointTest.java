package io.aether.examples.pointToPoint;

import io.aether.StandardUUIDs;
import io.aether.api.common.CryptoLib;

import io.aether.api.common.ServerDescriptor;
import io.aether.api.common.ServerDescriptorWithGeo;

import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;

import io.aether.cloud.client.ClientCloud;
import io.aether.cloud.client.ConnectionWork;
import io.aether.cloud.client.MessageEventListener;
import io.aether.cloud.client.MessageNode;

import io.aether.common.AccessGroupI;
import io.aether.logger.Log;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;

import java.net.URI;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;



import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class PointToPointTest {
    public final List<URI> registrationUri = new ArrayList<>();
    public ClientStateInMemory clientConfig1;
    public ClientStateInMemory clientConfig2;
    public ClientStateInMemory serviceConfig;

    {
        registrationUri.add(URI.create("tcp://registration.aethernet.io:9010"));
    }

    public AFuture p2p() {
//        var parent = StandardUUIDs.TEST_UID;
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
        client1.startFuture.to(() -> Log.info("client is registered uid1: $uid1", "uid1", client1.getUid()));
        client2.startFuture.to(() -> Log.info("client is registered uid2: $uid2", "uid2", client2.getUid()));
        client1.startFuture.onError(Log::error);
        client2.startFuture.onError(Log::error);
        AFuture.all(client1.startFuture, client2.startFuture).to(() -> {
            Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
            AFuture checkReceiveMessage = AFuture.make();
            var message = new byte[]{1, 2, 3, 4};
            AtomicLong sendTime = new AtomicLong();
            client2.onMessage((uid, msg) -> {
                long receiveTime = System.currentTimeMillis();
                long deliveryTime = receiveTime - sendTime.get();
                if (checkReceiveMessage.tryDone()) {
                    Log.info("First message confirm. Delivery time: $time ms", "time", deliveryTime);
                } else {
                    Log.warn("Second message confirm. Delivery time: $time ms", "time", deliveryTime);
                }
            });
            Log.info("START two clients!");
            Thread.currentThread().setName("MAIN THREAD");
            sendTime.set(System.currentTimeMillis());
            client1.sendMessage(client2.getUid(), message).to(() -> {
                client1.destroy(false).onError(testDoneFuture::error);
            });
            checkReceiveMessage.to(() -> {
                Log.info("TEST IS DONE!");
                client2.destroy(false)
                        .to(testDoneFuture)
                        .onError(testDoneFuture::error);
            }).onError(testDoneFuture::error);
        }).onError(testDoneFuture::error);
        return testDoneFuture;
    }


    public AFuture p2pBatchDeliveryTime() {
        var parent = UUID.fromString("B1AC52C8-8D94-BD39-4C01-A631AC594165");
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        clientConfig1.getPingDuration().set(100L);
        clientConfig2.getPingDuration().set(100L);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1, "client1");
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2, "client2");
        AFuture testDoneFuture = AFuture.make();
        client1.startFuture.onError(Log::error);
        client2.startFuture.onError(Log::error);
        // Setup message handler BEFORE start to catch all messages
        AtomicLong msgCounter = new AtomicLong(0);
        AtomicLong totalDeliveryTime = new AtomicLong(0);
        AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        AtomicLong maxTime = new AtomicLong(0);
        ConcurrentHashMap<Integer, Long> sendTimes = new ConcurrentHashMap<>();
        client2.onClientStream((st) -> {
            st.toConsumer(msg -> {
                long receiveTime = System.currentTimeMillis();
                // Extract message index from first byte
                int idx = msg[0] & 0xFF;
                Long sendTime = sendTimes.remove(idx);
                if (sendTime != null) {
                    long deliveryTime = receiveTime - sendTime;
                    long count = msgCounter.incrementAndGet();
                    totalDeliveryTime.addAndGet(deliveryTime);
                    minTime.updateAndGet(v -> Math.min(v, deliveryTime));
                    maxTime.updateAndGet(v -> Math.max(v, deliveryTime));
                    if (count == 50) {
                        long avgTime = totalDeliveryTime.get() / 50;
                        Log.info("Batch test complete. Avg: $avg ms, Min: $min ms, Max: $max ms",
                                "avg", avgTime, "min", minTime.get(), "max", maxTime.get());
                        testDoneFuture.done();
                    }
                }
            });
        });
        AFuture.all(client1.startFuture, client2.startFuture).to(() -> {
            Log.info("clients registered. Starting warmup + timed batch (50 messages)...");
            // Warmup: 10 messages
            for (int i = 0; i < 10; i++) {
                client1.sendMessage(client2.getUid(), new byte[]{(byte) i, 1, 2, 3});
                RU.sleep(20);
            }
            Log.info("Warmup done. Sending 50 timed messages...");
            // Timed batch: 50 messages
            for (int i = 10; i < 60; i++) {
                byte[] msg = new byte[]{(byte) i, 1, 2, 3};
                sendTimes.put(i, System.currentTimeMillis());
                client1.sendMessage(client2.getUid(), msg);
                RU.sleep(10);
            }
            Log.info("All 60 messages sent!");
        }).onError(testDoneFuture::error);
        testDoneFuture.to(() -> {
            Log.info("BATCH TEST DONE!");
            client1.destroy(false).to(() -> client2.destroy(false).to(() -> {
            }));
        });
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
                g.toConsumer(d -> {
                    receiveCounter.addAndGet(d.length);
                });
            });
            var data = new byte[10000];
            var timeBegin = RU.time();
            while (receiveCounter.get() < total) {
                boolean[] abortFlag = new boolean[1];
                ch1.send(data).onError((e) -> {
                    abortFlag[0] = true;
                });
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

    public AFuture p2pAndBack() {
        var parent = UUID.fromString("B0600A31-1ACC-BB39-35C9-F1476C1F40E2");
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);


        long scenarioStartTimeMs = System.currentTimeMillis();
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1, "client1");
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2, "client2");
        AFuture testDoneFuture = AFuture.make();


        AFuture.all(client1.startFuture, client2.startFuture).to(() -> {
            Log.info(
                    "clients is registered uid1: $uid1 uid2: $uid2",
                    "uid1", client1.getUid(),
                    "uid2", client2.getUid()
            );


            AFuture checkReceiveMessageBack = AFuture.make();
            AFuture checkReceiveWarmMessage = AFuture.make();

            byte[] message = new byte[]{1, 2, 3, 4};
            byte[] messageBack = new byte[]{1, 1, 1, 1};
            byte[] warmMessage = new byte[]{2, 2, 2, 2};

            AtomicLong firstSendTimeMs = new AtomicLong();
            AtomicLong client2ReceiveTimeMs = new AtomicLong();
            AtomicLong client1ReceiveTimeMs = new AtomicLong();
            AtomicLong warmSendTimeMs = new AtomicLong();
            AtomicLong warmReceiveTimeMs = new AtomicLong();
            AtomicLong aToBAttempts = new AtomicLong();
            AtomicLong warmAttempts = new AtomicLong();

            AtomicReference<ScheduledFuture<?>> replyResendTask =
                    new AtomicReference<>();
            AtomicReference<ScheduledFuture<?>> warmResendTask =
                    new AtomicReference<>();





            client2.onClientStream(st -> st.toConsumer(newMessage -> {
                if (java.util.Arrays.equals(newMessage, warmMessage)) {
                    long receiveTimeMs = System.currentTimeMillis();
                    if (!warmReceiveTimeMs.compareAndSet(0, receiveTimeMs)) {
                        return;
                    }

                    long registrationAndDeliveryMs =
                            client2ReceiveTimeMs.get() - scenarioStartTimeMs;
                    long deliveryAfterRegistrationMs =
                            client2ReceiveTimeMs.get() - firstSendTimeMs.get();
                    long warmDeliveryMs =
                            receiveTimeMs - warmSendTimeMs.get();
                    long roundTripMs =
                            client1ReceiveTimeMs.get() - firstSendTimeMs.get();

                    System.out.println(
                            "AETHER_JAVA_CLIENT_METRICS"
                                    + " aether_java_client_registration_and_delivery_ms="
                                    + registrationAndDeliveryMs
                                    + " aether_java_client_delivery_after_registration_ms="
                                    + deliveryAfterRegistrationMs
                                    + " aether_java_client_warm_delivery_ms="
                                    + warmDeliveryMs
                                    + " aether_java_client_round_trip_ms="
                                    + roundTripMs
                                    + " aether_java_client_delivery_after_registration_attempts="
                                    + aToBAttempts.get()
                                    + " aether_java_client_warm_delivery_attempts="
                                    + warmAttempts.get()
                    );

                    checkReceiveWarmMessage.tryDone();
                    return;
                }

                if (!java.util.Arrays.equals(newMessage, message)) {
                    return;
                }

                long receiveTimeMs = System.currentTimeMillis();
                if (!client2ReceiveTimeMs.compareAndSet(0, receiveTimeMs)) {
                    return;
                }

                ScheduledFuture<?> replyTask = RU.scheduleAtFixedRate(
                        250,
                        () -> {
                            if (checkReceiveMessageBack.isFinalStatus()) {
                                return;
                            }

                            st.send(messageBack);
                        }
                );
                replyResendTask.set(replyTask);

                if (checkReceiveMessageBack.isFinalStatus()) {
                    replyTask.cancel(false);
                }
            }));




            client1.onClientStream(st -> st.toConsumer(newMessage -> {
                if (!java.util.Arrays.equals(newMessage, messageBack)) {
                    return;
                }

                long receiveTimeMs = System.currentTimeMillis();
                if (!client1ReceiveTimeMs.compareAndSet(0, receiveTimeMs)) {
                    return;
                }

                checkReceiveMessageBack.tryDone();
            }));


            Log.info("START two clients!");
            var chToc2 = client1.getMessageNode(client2.getUid());
            AtomicReference<ScheduledFuture<?>> resendTask = new AtomicReference<>();
            ScheduledFuture<?> scheduledTask = RU.scheduleAtFixedRate(
                    250,
                    () -> {
                        if (checkReceiveMessageBack.isFinalStatus()) return;
                        firstSendTimeMs.compareAndSet(0, System.currentTimeMillis());
                        aToBAttempts.incrementAndGet();
                        chToc2.send(message);
                    }
            );
            resendTask.set(scheduledTask);

            if (checkReceiveMessageBack.isFinalStatus()) scheduledTask.cancel(false);


            checkReceiveMessageBack.to(() -> {
                ScheduledFuture<?> task = resendTask.getAndSet(null);
                if (task != null) {
                    task.cancel(false);
                }

                ScheduledFuture<?> replyTask =
                        replyResendTask.getAndSet(null);
                if (replyTask != null) {
                    replyTask.cancel(false);
                }

                ScheduledFuture<?> warmTask = RU.scheduleAtFixedRate(
                        250,
                        () -> {
                            if (checkReceiveWarmMessage.isFinalStatus()) {
                                return;
                            }

                            warmSendTimeMs.compareAndSet(
                                    0,
                                    System.currentTimeMillis()
                            );
                            warmAttempts.incrementAndGet();
                            chToc2.send(warmMessage);
                        }
                );
                warmResendTask.set(warmTask);

                if (checkReceiveWarmMessage.isFinalStatus()) {
                    warmTask.cancel(false);
                }

            }).onError(error -> failWithCleanup(
                    client1,
                    client2,
                    testDoneFuture,
                    error,
                    "p2p"
            ));


            checkReceiveWarmMessage.to(() -> {
                ScheduledFuture<?> task = resendTask.getAndSet(null);
                if (task != null) {
                    task.cancel(false);
                }

                ScheduledFuture<?> replyTask =
                        replyResendTask.getAndSet(null);
                if (replyTask != null) {
                    replyTask.cancel(false);
                }

                ScheduledFuture<?> warmTask =
                        warmResendTask.getAndSet(null);
                if (warmTask != null) {
                    warmTask.cancel(false);
                }

                Log.info("TEST IS DONE!");



                Thread.startVirtualThread(() -> {
                    try {
                        selfDestructAndDestroy(
                                client1,
                                "p2p client1"
                        );
                        selfDestructAndDestroy(
                                client2,
                                "p2p client2"
                        );
                        testDoneFuture.done();
                    } catch (Throwable error) {
                        failWithCleanup(
                                client1,
                                client2,
                                testDoneFuture,
                                error,
                                "p2p"
                        );
                    }
                });



            }).onError(error -> failWithCleanup(
                    client1,
                    client2,
                    testDoneFuture,
                    error,
                    "p2p"
            ));



        }).onError(error -> failWithCleanup(
                client1,
                client2,
                testDoneFuture,
                error,
                "p2p"
        ));


        return testDoneFuture;
    }


    public AFuture p2pAcrossWorkServers() {
        var parent = UUID.fromString("B0600A31-1ACC-BB39-35C9-F1476C1F40E2");

        if (clientConfig1 == null) {
            clientConfig1 = new ClientStateInMemory(
                    parent,
                    registrationUri,
                    null,
                    CryptoLib.HYDROGEN
            );
        }
        if (clientConfig2 == null) {
            clientConfig2 = new ClientStateInMemory(
                    parent,
                    registrationUri,
                    null,
                    CryptoLib.HYDROGEN
            );
        }

        AetherCloudClient client1 =
                new AetherCloudClient(clientConfig1, "server-canary-client1");
        AetherCloudClient client2 =
                new AetherCloudClient(clientConfig2, "server-canary-client2");

        AFuture testDoneFuture = AFuture.make();

        Thread.startVirtualThread(() -> {
            try {
                awaitFutureSuccess(
                        AFuture.all(client1.startFuture, client2.startFuture),
                        30_000,
                        "connect canary clients"
                );

                ServerDescriptorWithGeo[] servers = awaitResult(
                        client1.getServers(),
                        10_000,
                        "enumerate WORK servers"
                );

                if (servers.length == 0) {
                    throw new IllegalStateException(
                            "No WORK servers returned by getServers()"
                    );
                }

                short[] sids = new short[servers.length];
                for (int i = 0; i < servers.length; i++) {
                    sids[i] = servers[i].getId();
                }

                var client1Api = awaitResult(
                        client1.getClientApi(client1.getUid()),
                        10_000,
                        "open client1 cloud API"
                );
                var client2Api = awaitResult(
                        client2.getClientApi(client2.getUid()),
                        10_000,
                        "open client2 cloud API"
                );

                awaitFutureSuccess(
                        client1Api.addServersToCloud(sids),
                        10_000,
                        "add WORK servers to client1 cloud"
                );
                awaitFutureSuccess(
                        client2Api.addServersToCloud(sids),
                        10_000,
                        "add WORK servers to client2 cloud"
                );

                waitForCloudSids(
                        client1,
                        client1.getUid(),
                        sids,
                        10_000
                );
                waitForCloudSids(
                        client2,
                        client2.getUid(),
                        sids,
                        10_000
                );

                final int probeMagic = 0xA37ECAFE;

                MessageEventListener routeBySid =
                        new MessageEventListener() {
                            @Override
                            public void setConsumerCloud(
                                    MessageNode messageNode,
                                    ClientCloud cloud
                            ) {
                                for (short sid : cloud.getData()) {
                                    messageNode.addConsumerServerOut(sid);
                                }
                            }

                            @Override
                            public void onResolveConsumerServer(
                                    MessageNode messageNode,
                                    ServerDescriptor serverDescriptor
                            ) {
                                messageNode.addConsumerServerOut(
                                        serverDescriptor
                                );
                            }

                            @Override
                            public void onResolveConsumerConnection(
                                    MessageNode messageNode,
                                    ConnectionWork connection
                            ) {
                                messageNode.addConsumerConnectionOut(
                                        connection
                                );
                            }

                            @Override
                            public boolean shouldSendViaConnection(
                                    MessageNode messageNode,
                                    ConnectionWork connection,
                                    byte[] message
                            ) {
                                if (message == null || message.length != 10) {
                                    return false;
                                }

                                ByteBuffer input = ByteBuffer.wrap(message);

                                return input.getInt() == probeMagic
                                        && input.getShort()
                                        == connection
                                        .getServerDescriptor()
                                        .getId();
                            }
                        };

                MessageNode forwardNode = client1.getMessageNode(
                        client2.getUid(),
                        routeBySid
                );
                MessageNode reverseNode = client2.getMessageNode(
                        client1.getUid(),
                        routeBySid
                );

                for (short sid : sids) {
                    forwardNode.addConsumerServerOut(sid);
                    reverseNode.addConsumerServerOut(sid);
                }

                ConcurrentHashMap<Long, ARFuture<Long>> forwardDeliveries =
                        new ConcurrentHashMap<>();
                ConcurrentHashMap<Long, ARFuture<Long>> reverseDeliveries =
                        new ConcurrentHashMap<>();

                client2.onMessage((fromUid, data) -> {
                    if (!client1.getUid().equals(fromUid)
                            || data == null
                            || data.length != 10) {
                        return;
                    }

                    ByteBuffer input = ByteBuffer.wrap(data);

                    if (input.getInt() != probeMagic) {
                        return;
                    }

                    short sid = input.getShort();
                    int sequence = input.getInt();
                    long key = probeKey(sid, sequence);

                    ARFuture<Long> delivery =
                            forwardDeliveries.remove(key);

                    if (delivery != null) {
                        delivery.tryDone(System.nanoTime());
                    }
                });

                client1.onMessage((fromUid, data) -> {
                    if (!client2.getUid().equals(fromUid)
                            || data == null
                            || data.length != 10) {
                        return;
                    }

                    ByteBuffer input = ByteBuffer.wrap(data);

                    if (input.getInt() != probeMagic) {
                        return;
                    }

                    short sid = input.getShort();
                    int sequence = input.getInt();
                    long key = probeKey(sid, sequence);

                    ARFuture<Long> delivery =
                            reverseDeliveries.remove(key);

                    if (delivery != null) {
                        delivery.tryDone(System.nanoTime());
                    }
                });

                Set<Short> failedSids = new ConcurrentHashSet<>();

                List<Thread> probeThreads = new ArrayList<>();

                final int minSamples = 15;
                final int maxSamples = 300;
                final int requiredStableSamples = 3;

                final int maxSampleAttempts = 3;

                final double targetErrorNs =
                        java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(1);


                final long measurementDeadlineNs =
                        System.nanoTime()
                                + java.util.concurrent.TimeUnit.SECONDS.toNanos(240);

                for (int sidIndex = 0; sidIndex < sids.length; sidIndex++) {
                    short sid = sids[sidIndex];

                    long remainingBudgetNs = Math.max(
                            0L,
                            measurementDeadlineNs - System.nanoTime()
                    );

                    int remainingSids = sids.length - sidIndex;

                    long maxProbeDurationNs =
                            remainingBudgetNs / Math.max(1, remainingSids);

                    probeThreads.add(Thread.startVirtualThread(() -> {
                        try {
                            ConnectionWork connection1 =
                                    waitForConnection(client1, sid, 10_000);
                            ConnectionWork connection2 =
                                    waitForConnection(client2, sid, 10_000);

                            int sampleCount = 0;
                            int stableSamples = 0;
                            int negativeSamples = 0;

                            int sampleAttempts = 0;
                            int sampleRetries = 0;
                            int probeSequence = 0;


                            long totalPingANs = 0L;
                            long totalPingBNs = 0L;
                            long totalForwardNs = 0L;
                            long totalReverseNs = 0L;

                            double meanInternalNs = 0.0;
                            double m2InternalNs = 0.0;
                            double error95Ns = Double.POSITIVE_INFINITY;

                            long statisticsStartedNs = System.nanoTime();


                            while (sampleCount < maxSamples
                                    && System.nanoTime() - statisticsStartedNs
                                    < maxProbeDurationNs
                                    && System.nanoTime() < measurementDeadlineNs) {



                                int sampleOrdinal = sampleCount + 1;

                                long pingANs;
                                long pingBNs;
                                long forwardNs;
                                long reverseNs;

                                int attempt = 0;

                                while (true) {
                                    attempt++;
                                    sampleAttempts++;

                                    int sequence = ++probeSequence;

                                    try {
                                        pingANs = awaitResult(
                                                connection1.measurePingNs(),
                                                12_000,
                                                "ping client1 through SID "
                                                        + sid
                                                        + " sample "
                                                        + sampleOrdinal
                                                        + " attempt "
                                                        + attempt
                                        );

                                        pingBNs = awaitResult(
                                                connection2.measurePingNs(),
                                                12_000,
                                                "ping client2 through SID "
                                                        + sid
                                                        + " sample "
                                                        + sampleOrdinal
                                                        + " attempt "
                                                        + attempt
                                        );

                                        forwardNs = measureProbeDelivery(
                                                forwardNode,
                                                forwardDeliveries,
                                                sid,
                                                sequence,
                                                probeMagic,
                                                "A->B"
                                        );

                                        reverseNs = measureProbeDelivery(
                                                reverseNode,
                                                reverseDeliveries,
                                                sid,
                                                sequence,
                                                probeMagic,
                                                "B->A"
                                        );

                                        break;
                                    } catch (Throwable sampleError) {
                                        stableSamples = 0;

                                        if (attempt >= maxSampleAttempts
                                                || System.nanoTime()
                                                >= measurementDeadlineNs) {
                                            throw sampleError;
                                        }

                                        sampleRetries++;

                                        String errorMessage =
                                                String.valueOf(
                                                        sampleError.getMessage()
                                                )
                                                        .replace('\n', ' ')
                                                        .replace('\r', ' ');

                                        System.out.println(
                                                "AETHER_JAVA_CLIENT_SERVER_RETRY"
                                                        + " sid=" + sid
                                                        + " sample=" + sampleOrdinal
                                                        + " attempt=" + attempt
                                                        + " next_attempt="
                                                        + (attempt + 1)
                                                        + " error="
                                                        + sampleError
                                                        .getClass()
                                                        .getSimpleName()
                                                        + " message="
                                                        + errorMessage
                                        );
                                    }
                                }


                                double internalNs = (
                                        (double) forwardNs
                                                + reverseNs
                                                - pingANs
                                                - pingBNs
                                ) / 2.0;

                                sampleCount++;

                                totalPingANs += pingANs;
                                totalPingBNs += pingBNs;
                                totalForwardNs += forwardNs;
                                totalReverseNs += reverseNs;

                                if (internalNs < 0.0) {
                                    negativeSamples++;
                                }

                                double delta =
                                        internalNs - meanInternalNs;
                                meanInternalNs += delta / sampleCount;
                                double delta2 =
                                        internalNs - meanInternalNs;
                                m2InternalNs += delta * delta2;

                                if (sampleCount > 1) {
                                    double variance =
                                            m2InternalNs
                                                    / (sampleCount - 1);

                                    double standardDeviationNs =
                                            Math.sqrt(
                                                    Math.max(0.0, variance)
                                            );

                                    error95Ns =
                                            1.96
                                                    * standardDeviationNs
                                                    / Math.sqrt(sampleCount);
                                }

                                long error95Us = sampleCount > 1
                                        ? Math.round(error95Ns / 1_000.0)
                                        : -1L;

                                System.out.println(
                                        "AETHER_JAVA_CLIENT_SERVER_SAMPLE"
                                                + " sid=" + sid
                                                + " sample=" + sampleCount

                                                + " attempt=" + attempt
                                                + " total_attempts="
                                                + sampleAttempts
                                                + " retries="
                                                + sampleRetries

                                                + " ping_a_us="
                                                + Math.round(
                                                        pingANs / 1_000.0
                                                )
                                                + " ping_b_us="
                                                + Math.round(
                                                        pingBNs / 1_000.0
                                                )
                                                + " forward_us="
                                                + Math.round(
                                                        forwardNs / 1_000.0
                                                )
                                                + " reverse_us="
                                                + Math.round(
                                                        reverseNs / 1_000.0
                                                )
                                                + " internal_us="
                                                + Math.round(
                                                        internalNs / 1_000.0
                                                )
                                                + " mean_internal_us="
                                                + Math.round(
                                                        meanInternalNs / 1_000.0
                                                )
                                                + " error95_us="
                                                + error95Us
                                );

                                if (sampleCount >= minSamples
                                        && error95Ns <= targetErrorNs) {
                                    stableSamples++;
                                } else {
                                    stableSamples = 0;
                                }

                                if (stableSamples
                                        >= requiredStableSamples) {
                                    break;
                                }
                            }

                            if (sampleCount == 0) {
                                throw new IllegalStateException(
                                        "No measurement samples for SID " + sid
                                );
                            }

                            boolean converged =
                                    stableSamples >= requiredStableSamples;

                            long meanPingAUs = Math.round(
                                    totalPingANs
                                            / (double) sampleCount
                                            / 1_000.0
                            );
                            long meanPingBUs = Math.round(
                                    totalPingBNs
                                            / (double) sampleCount
                                            / 1_000.0
                            );
                            long meanForwardUs = Math.round(
                                    totalForwardNs
                                            / (double) sampleCount
                                            / 1_000.0
                            );
                            long meanReverseUs = Math.round(
                                    totalReverseNs
                                            / (double) sampleCount
                                            / 1_000.0
                            );
                            long messageRoundtripUs = Math.round(
                                    (totalForwardNs + totalReverseNs)
                                            / (double) sampleCount
                                            / 1_000.0
                            );

                            double standardDeviationNs =
                                    sampleCount > 1
                                            ? Math.sqrt(
                                                    Math.max(
                                                            0.0,
                                                            m2InternalNs
                                                                    / (sampleCount - 1)
                                                    )
                                            )
                                            : 0.0;

                            long finalError95Us =
                                    sampleCount > 1
                                            ? Math.round(
                                                    error95Ns / 1_000.0
                                            )
                                            : -1L;

                            System.out.println(
                                    "AETHER_JAVA_CLIENT_SERVER_METRICS"
                                            + " sid=" + sid
                                            + " samples=" + sampleCount

                                            + " attempts=" + sampleAttempts
                                            + " retries=" + sampleRetries

                                            + " ping_a_us=" + meanPingAUs
                                            + " ping_b_us=" + meanPingBUs
                                            + " forward_us=" + meanForwardUs
                                            + " reverse_us=" + meanReverseUs
                                            + " message_roundtrip_us="
                                            + messageRoundtripUs
                                            + " aether_delivery_us="
                                            + Math.round(
                                                    meanInternalNs / 1_000.0
                                            )
                                            + " stddev_us="
                                            + Math.round(
                                                    standardDeviationNs
                                                            / 1_000.0
                                            )
                                            + " error95_us="
                                            + finalError95Us
                                            + " negative_samples="
                                            + negativeSamples
                                            + " converged="
                                            + (converged ? 1 : 0)
                                            + " success=1"
                            );

                        } catch (Throwable error) {
                            failedSids.add(sid);

                            System.out.println(
                                    "AETHER_JAVA_CLIENT_SERVER_METRICS"
                                            + " sid=" + sid
                                            + " success=0"
                            );

                            Log.error(
                                    "WORK server canary failed for SID " + sid,
                                    error
                            );
                        }
                    }));

                    try {
                        probeThreads.get(probeThreads.size() - 1).join();
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(
                                "Interrupted while waiting for WORK server probe SID " + sid,
                                error
                        );
                    }

                }

                for (Thread probeThread : probeThreads) {
                    try {
                        probeThread.join();
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(
                                "Interrupted while waiting for WORK server probes",
                                error
                        );
                    }
                }

                if (!failedSids.isEmpty()) {
                    throw new IllegalStateException(
                            "WORK server canary failed for SIDs "
                                    + failedSids
                    );
                }

                selfDestructAndDestroy(
                        client1,
                        "canary client1"
                );
                selfDestructAndDestroy(
                        client2,
                        "canary client2"
                );

                testDoneFuture.done();

            } catch (Throwable error) {
                bestEffortSelfDestructAndDestroy(
                        client1,
                        "canary client1"
                );
                bestEffortSelfDestructAndDestroy(
                        client2,
                        "canary client2"
                );

                testDoneFuture.tryError(error);
            }
        });

        return testDoneFuture;
    }



    private static void failWithCleanup(
            AetherCloudClient client1,
            AetherCloudClient client2,
            AFuture result,
            Throwable error,
            String name
    ) {
        Thread.startVirtualThread(() -> {
            bestEffortSelfDestructAndDestroy(
                    client1,
                    name + " client1"
            );
            bestEffortSelfDestructAndDestroy(
                    client2,
                    name + " client2"
            );
            result.tryError(error);
        });
    }



    private static void selfDestructAndDestroy(
            AetherCloudClient client,
            String name
    ) {
        try {
            var authApi = awaitResult(
                    client.getAuthApiFuture(),
                    8_000,
                    "open " + name + " auth API for self-destruct"
            );

            awaitFutureSuccess(
                    authApi.selfDestruct(),
                    10_000,
                    "self-destruct " + name
            );
        } finally {
            awaitFutureSuccess(
                    client.destroy(true),
                    8_000,
                    "destroy " + name
            );
        }
    }

    private static void bestEffortSelfDestructAndDestroy(
            AetherCloudClient client,
            String name
    ) {
        try {
            var authApi = awaitResult(
                    client.getAuthApiFuture(),
                    3_000,
                    "open " + name + " auth API for cleanup"
            );

            awaitFutureSuccess(
                    authApi.selfDestruct(),
                    5_000,
                    "self-destruct " + name
            );
        } catch (Throwable cleanupError) {
            Log.error(
                    "Best-effort canary cleanup failed for " + name,
                    cleanupError
            );
        }

        client.destroy(true);
    }



    private static void waitForCloudSids(
            AetherCloudClient client,
            UUID uid,
            short[] expectedSids,
            long timeoutMs
    ) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            ClientCloud cloud = client.getCloud(uid).getNow();

            if (cloud != null
                    && containsAllSids(
                    cloud.getData(),
                    expectedSids
            )) {
                return;
            }

            RU.sleep(50);
        }

        throw new IllegalStateException(
                "Cloud did not contain all expected WORK servers for "
                        + uid
        );
    }

    private static boolean containsAllSids(
            short[] actual,
            short[] expected
    ) {
        for (short expectedSid : expected) {
            boolean found = false;

            for (short actualSid : actual) {
                if (actualSid == expectedSid) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }


    private static ConnectionWork waitForConnection(
            AetherCloudClient client,
            short sid,
            long timeoutMs
    ) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            ConnectionWork connection = client
                    .getConnections()
                    .stream()
                    .filter(candidate ->
                            candidate
                                    .getServerDescriptor()
                                    .getId() == sid
                    )
                    .filter(ConnectionWork::isWritable)
                    .findFirst()
                    .orElse(null);

            if (connection != null) {
                return connection;
            }

            RU.sleep(50);
        }

        throw new IllegalStateException(
                "Writable connection was not established for WORK SID "
                        + sid
        );
    }


    private static void awaitFutureSuccess(
            AFuture future,
            long timeoutMs,
            String operation
    ) {
        boolean successful = future.waitSuccessful(timeoutMs);

        if (future.isError()) {
            throw new RuntimeException(
                    operation,
                    future.getError()
            );
        }

        if (!successful) {
            throw new RuntimeException(
                    operation + " timed out after " + timeoutMs + " ms"
            );
        }
    }

    private static <T> T awaitResult(
            ARFuture<T> future,
            long timeoutMs,
            String operation
    ) {
        boolean successful = future.waitSuccessful(timeoutMs);

        if (future.isError()) {
            throw new RuntimeException(
                    operation,
                    future.getError()
            );
        }

        if (!successful) {
            throw new RuntimeException(
                    operation + " timed out after " + timeoutMs + " ms"
            );
        }

        T result = future.getNow();

        if (result == null) {
            throw new RuntimeException(
                    operation + " completed without a result"
            );
        }

        return result;
    }



    @SuppressWarnings("deprecation")
    public AFuture pointToPointWithService() {
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
                    st.toConsumer(newMessage -> {
                        checkReceiveMessage.done();
                    });
                });
                Log.info("START!");
                var chToc2 = client1.getMessageNode(client2.getUid());
                chToc2.send(message);
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

    private AFuture startIteration2() {
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
                    st.toConsumer(newMessage -> {
                        checkReceiveMessage.done();
                    });
                });
                Log.info("START two clients!");
                var chToc2 = client1.getMessageNode(client2.getUid());
                Thread.currentThread().setName("MAIN THREAD");
                chToc2.send(message);
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
                var sendFuture = chToc2.send(message);
                checkReceiveMessage.to(() -> {
                    if (!sendFuture.isDone()) {
                        throw new IllegalStateException();
                    }
                    Log.info("TEST IS DONE!");
                    var f1 = client1.destroy(true);
                    var f2 = client2.destroy(true);
                    AFuture.all(f1, f2).onError(t -> {
                        testDoneFuture.error(new IllegalStateException("Failed to destroy clients after iteration 1: " + f1 + ":" + f2, t));
                    }).to(() -> {
                        Log.debug("ITERATION 2 START");
                        startIteration2().to(testDoneFuture::done)
                                .onError(testDoneFuture::error);
                    });
                }).onError(testDoneFuture::error);
            }).onError(testDoneFuture::error);
        }
        return testDoneFuture;
    }

    public AFuture pointToPointWithReconnect2() {
        var parent = UUID.fromString("84AE8BD0-2BE4-FF65-406C-B1B655444D54");
        clientConfig1 = new ClientStateInMemory(parent, registrationUri);
        clientConfig2 = new ClientStateInMemory(parent, registrationUri);
        AFuture testDoneFuture = AFuture.make();
        AFuture iteration2 = AFuture.make();
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
                var sendFuture = chToc2.send(message);
                checkReceiveMessage.to(() -> {
                    if (!sendFuture.isDone()) {
                        throw new IllegalStateException();
                    }
                    Log.info("TEST 1 IS DONE!");
                    var f1 = client1.destroy(true);
                    var f2 = client2.destroy(true);
                    AFuture.all(f1, f2).onError(t -> {
                        testDoneFuture.error(new IllegalStateException("Failed to destroy clients after iteration 1: " + f1 + ":" + f2, t));
                    }).to(() -> {
                        Log.debug("ITERATION 2 START");
                        iteration2.done();
                    });
                }).onError(testDoneFuture::error);
            }).onError(testDoneFuture::error);
            iteration2.to(() -> {
                Log.info("clients is registered uid1: $uid1 uid2: $uid2", "uid1", client1.getUid(), "uid2", client2.getUid());
                var client1_2 = new AetherCloudClient(clientConfig1, "client1_1");
                var client2_2 = new AetherCloudClient(clientConfig2, "client2_2");
                AFuture checkReceiveMessage = AFuture.make();
                var message = new byte[]{1, 1, 1, 1};
                client2_2.onClientStream((st) -> {
                    st.toConsumer(newMessage -> {
                        checkReceiveMessage.done();
                    });
                });
                Log.info("START two clients!");
                var chToc2 = client1_2.getMessageNode(client2_2.getUid());
                Thread.currentThread().setName("MAIN THREAD");
                var sendFuture = chToc2.send(message);
                checkReceiveMessage.to(() -> {
                    if (!sendFuture.isDone()) {
                        throw new IllegalStateException();
                    }
                    Log.info("TEST 2 IS DONE!");
                    var f1 = client1_2.destroy(true);
                    var f2 = client2_2.destroy(true);
                    AFuture.all(f1, f2).onError(t -> {
                        testDoneFuture.error(new IllegalStateException("Failed to destroy clients after iteration 1: " + f1 + ":" + f2, t));
                    }).to(() -> {
                        Log.debug("ITERATION 2 START");
                    }).to(testDoneFuture);
                }).onError(testDoneFuture::error);
            }).onError(testDoneFuture::error);
        }
        return testDoneFuture;
    }


    public AFuture p2pPeriodicSend() {
        var parent = UUID.fromString("B1AC52C8-8D94-BD39-4C01-A631AC594165");
        if (clientConfig1 == null)
            clientConfig1 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.SODIUM);
        if (clientConfig2 == null)
            clientConfig2 = new ClientStateInMemory(parent, registrationUri, null, CryptoLib.HYDROGEN);
        clientConfig1.getPingDuration().set(100L);
        clientConfig2.getPingDuration().set(100L);
        AetherCloudClient client1 = new AetherCloudClient(clientConfig1, "periodicClient1");
        AetherCloudClient client2 = new AetherCloudClient(clientConfig2, "periodicClient2");
        AFuture testDoneFuture = AFuture.make();
        AtomicLong messageCounter = new AtomicLong(0);
        AtomicLong lastReceivedCounter = new AtomicLong(0);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        AtomicReference<ScheduledFuture<?>> schedulerRef = new AtomicReference<>();
        client2.onMessage((uid, msg) -> {
            long received = lastReceivedCounter.incrementAndGet();
            long sent = messageCounter.get();
            Log.info("Received message #$received from $uid, sent: $sent");
        });
        AFuture.all(client1.startFuture, client2.startFuture).to(() -> {
            Log.info("Both clients registered. UID1: $uid1, UID2: $uid2",
                    "uid1", client1.getUid(), "uid2", client2.getUid());
            // Периодическая отправка раз в секунду через RU.scheduleAtFixedRate
            ScheduledFuture<?> scheduler = RU.scheduleAtFixedRate(1000, () -> {
                if (testDoneFuture.isDone()) {
                    ScheduledFuture<?> s = schedulerRef.get();
                    if (s != null) s.cancel(false);
                    return;
                }
                long idx = messageCounter.incrementAndGet();
                byte[] msg = new byte[8];
                msg[0] = (byte) (idx & 0xFF);
                msg[1] = (byte) ((idx >> 8) & 0xFF);
                msg[2] = (byte) ((idx >> 16) & 0xFF);
                msg[3] = (byte) ((idx >> 24) & 0xFF);
                msg[4] = 1;
                msg[5] = 2;
                msg[6] = 3;
                msg[7] = 4;
                Log.info("Sending message #$idx", "idx", idx);
                client1.sendMessage(client2.getUid(), msg).onError(e -> {
                    Log.error("Failed to send message #$idx", e);
                    errorRef.set(e);
                    testDoneFuture.error(e);
                    ScheduledFuture<?> s = schedulerRef.get();
                    if (s != null) s.cancel(false);
                });
            });
            client1.flush();
            schedulerRef.set(scheduler);
            // Таймаут - 2 минуты
            AFuture timeout = AFuture.make();
            timeout.timeoutMs(1800000, () -> {
                if (!testDoneFuture.isDone()) {
                    Log.info("Test timeout reached. Sent: $sent, Received: $received",
                            "sent", messageCounter.get(), "received", lastReceivedCounter.get());
                    testDoneFuture.done();
                    ScheduledFuture<?> s = schedulerRef.get();
                    if (s != null) s.cancel(false);
                }
            });
        }).onError(e -> {
            Log.error("Failed to start clients", e);
            testDoneFuture.error(e);
        });
        testDoneFuture.to(() -> {
            Log.info("Periodic test completed. Sent: $sent, Received: $received",
                    "sent", messageCounter.get(), "received", lastReceivedCounter.get());
            ScheduledFuture<?> s = schedulerRef.get();
            if (s != null) s.cancel(false);
            client1.destroy(true);
            client2.destroy(true);
        });
        return testDoneFuture;
    }
    private long probeKey(
            short sid,
            int sequence
    ) {
        return (((long) sid & 0xFFFFL) << 32)
                | ((long) sequence & 0xFFFFFFFFL);
    }
    private long measureProbeDelivery(
            MessageNode messageNode,
            ConcurrentHashMap<Long, ARFuture<Long>> deliveries,
            short sid,
            int sequence,
            int probeMagic,
            String direction
    ) {
        long key = probeKey(sid, sequence);
        ARFuture<Long> delivery = ARFuture.make();

        delivery.timeoutError(
                10,
                "Timeout waiting for "
                        + direction
                        + " delivery through SID "
                        + sid
                        + " sample "
                        + sequence
        );

        ARFuture<Long> previous = deliveries.putIfAbsent(key, delivery);

        if (previous != null) {
            throw new IllegalStateException(
                    "Duplicate probe key for SID "
                            + sid
                            + " sample "
                            + sequence
                            + " direction "
                            + direction
            );
        }

        byte[] payload = ByteBuffer
                .allocate(10)
                .putInt(probeMagic)
                .putShort(sid)
                .putInt(sequence)
                .array();

        long sendStartedNs = System.nanoTime();

        try {
            AFuture sendFuture = messageNode.send(payload);

            sendFuture.timeoutError(
                    10,
                    "Timeout sending "
                            + direction
                            + " through SID "
                            + sid
                            + " sample "
                            + sequence
            );

            awaitFutureSuccess(
                    sendFuture,
                    12_000,
                    "send "
                            + direction
                            + " through SID "
                            + sid
                            + " sample "
                            + sequence
            );

            long receivedNs = awaitResult(
                    delivery,
                    12_000,
                    "receive "
                            + direction
                            + " through SID "
                            + sid
                            + " sample "
                            + sequence
            );

            return receivedNs - sendStartedNs;
        } finally {
            deliveries.remove(key, delivery);
        }
    }
}