package io.aether.cloud.client;

import io.aether.api.clientserverapi.*;
import io.aether.api.common.AccessCheckPair;
import io.aether.api.common.AetherCodec;
import io.aether.api.common.AppliedConfig;
import io.aether.api.common.ServerDescriptor;
import io.aether.crypto.CryptoEngine;
import io.aether.logger.Log;



import io.aether.utils.RU;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.tuples.Tuple2;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles the specific connection logic for Work Servers, including authentication,
 * batching of API requests, and message routing.
 */
public class ConnectionWork extends Connection<ClientApiUnsafe, LoginApiRemote> implements ClientApiUnsafe {

    public final AtomicLong lastBackPing = new AtomicLong(Long.MAX_VALUE);

    final CryptoEngine cryptoEngine;
    final AuthorizedApiRemote authorizedApi;
    private final ServerDescriptor serverDescriptor;

    private static final long PING_RESPONSE_TIMEOUT_MS = 5_000L;
    private static final long PING_ACQUIRE_TIMEOUT_MS =
            PING_RESPONSE_TIMEOUT_MS + 1_000L;

    private static final long RX_WINDOW_MS =
            PING_RESPONSE_TIMEOUT_MS;


    private static final class PingAttempt {
    }

    private final AtomicReference<PingAttempt> activePing =
            new AtomicReference<>();

    boolean basicStatus;

    private final PingRttHistory pingRttHistory =
            new PingRttHistory();

    private final AtomicLong nextPingAtMs =
            new AtomicLong();

    volatile boolean firstAuth;

    public ConnectionWork(AetherCloudClient client, ServerDescriptor s) {
        super(client, s.getIpAddress().getURI(AetherCodec.UDP), ClientApiUnsafe.META, LoginApi.META);
        cryptoEngine = client.getCryptoEngineForServer(s.getId());
        if (cryptoEngine == null) {
            Log.error("ConnectionWork: cryptoEngine is null for server " + s.getId() + ". Authentication will fail.");
        }
        serverDescriptor = s;
        this.basicStatus = false;
        authorizedApi = getRootApi().openLoginByAlias(client.getAlias(), c -> new ClientApiSafeImpl(this, client), cryptoEngine::encrypt, "loginByAlias");
    }

    /**
     * Handles changes in the connection state. Resets the internal authentication
     * flag and fires the state listeners to notify the client for failover logic.
     *
     * @param isWritable True if the connection is active and writable, false otherwise.
     */

    @Override
    protected void onConnectionStateChanged(boolean isWritable) {
        firstAuth = false;

        if (cryptoEngine == null) {
            Log.warn(
                    "onConnectionStateChanged called before cryptoEngine initialized, deferring flush"
            );
            stateListeners.fire(isWritable);
            return;
        }

        if (isWritable) {
            nextPingAtMs.set(0L);

            Log.info(
                    "Network restored. Resetting auth state and forcing flush.",
                    "uri",
                    uri
            );
        } else {
            activePing.set(null);
        }

        stateListeners.fire(isWritable);
    }


    @Override
    public boolean isWritable() {
        return super.isWritable();
    }



    public void flushBackgroundRequests() {
        var a = authorizedApi;
        // Запросы облаков через новый механизм
        for (UUID uid : client.clouds.pollAllRequests()) {
            ClientCloud cc = client.clouds.getNow(uid);
            long version = cc != null ? cc.getConfigVersion() - 1 : -1;
            client.appliedConfigsRequests.getFuture(new AppliedConfig(uid, version));
        }
        for (ClientCloud cc : client.clouds.values()) {
            if (cc.getConfigVersion() > cc.getConfirmedConfigVersion()) {
                client.appliedConfigsRequests.getFuture(new AppliedConfig(cc.getUid(), cc.getConfigVersion()));
            }
        }
        List<AppliedConfig> pendingList = new ArrayList<>();
        AppliedConfig req;
        while ((req = client.appliedConfigsRequests.pollNextRequest()) != null) {
            pendingList.add(req);
        }
        if (!pendingList.isEmpty()) {
            a.reportAppliedConfig(pendingList.toArray(new AppliedConfig[0]));
        }
        Integer[] requestServers = client.servers.pollAllRequests().toArray(new Integer[0]);
        if (requestServers.length > 0) {
            short[] serverIds = new short[requestServers.length];
            for (int i = 0; i < requestServers.length; i++) {
                serverIds[i] = requestServers[i].shortValue();
            }
            a.resolverServers(serverIds);
        }
        UUID[] requestClientGroups = client.clientGroups.pollAllRequests().toArray(new UUID[0]);
        if (requestClientGroups.length > 0) {
            a.requestAccessGroupsForClients(requestClientGroups);
        }
        UUID[] requestAccessGroups = client.accessGroups.pollAllRequests().toArray(new UUID[0]);
        if (requestAccessGroups.length > 0) {
            UUID[] groupIds = new UUID[requestAccessGroups.length];
            for (int i = 0; i < requestAccessGroups.length; i++) {
                groupIds[i] = requestAccessGroups[i];
            }
            a.requestAccessGroupsItems(groupIds);
        }
        UUID[] requestAllAccessed = client.allAccessedClients.pollAllRequests().toArray(new UUID[0]);
        if (requestAllAccessed.length > 0) {
            a.requestAllAccessedClients(requestAllAccessed);
        }
        AccessCheckPair[] requestAccessCheck = client.accessCheckCache.pollAllRequests().toArray(new AccessCheckPair[0]);
        if (requestAccessCheck.length > 0) {
            a.requestAccessCheck(requestAccessCheck);
        }
        for (Map.Entry<UUID, Map<UUID, ARFuture<Boolean>>> entry : client.accessOperationsAdd.entrySet()) {
            UUID groupId = entry.getKey();
            UUID[] uidsToAdd = entry.getValue().keySet().toArray(new UUID[0]);
            if (uidsToAdd.length > 0) {
                Log.debug("Flushing ADD request for group $gid: $uids", "gid", groupId, "uids", uidsToAdd);
                a.addItemsToAccessGroup(groupId, uidsToAdd);
            }
        }
        for (Map.Entry<UUID, Map<UUID, ARFuture<Boolean>>> entry : client.accessOperationsRemove.entrySet()) {
            UUID groupId = entry.getKey();
            UUID[] uidsToRemove = entry.getValue().keySet().toArray(new UUID[0]);
            if (uidsToRemove.length > 0) {
                Log.debug("Flushing REMOVE request for group $gid: $uids", "gid", groupId, "uids", uidsToRemove);
                a.removeItemsFromAccessGroup(groupId, uidsToRemove);
            }
        }
        while (true) {
            var t = client.authTasks.poll();
            if (t == null) break;
            t.accept(a);
        }


        for (var messageNode : client.messageNodeMap.values()) {


            if (!messageNode.connectionsOut.contains(this)) {
                continue;
            }



            List<Tuple2<byte[], AFuture>> nodeMessages =
                    new ArrayList<>();
            int currentBatchSize = 0;
            final int maxBatchBytes = 512 * 1024;


            for (Tuple2<byte[], AFuture> pending : messageNode.bufferOut) {
                byte[] message = pending.val1();

                if (!messageNode.getStrategy().shouldSendViaConnection(
                        messageNode,
                        this,
                        message
                )) {
                    continue;
                }

                if (currentBatchSize + message.length > maxBatchBytes) {
                    break;
                }

                if (!messageNode.bufferOut.remove(pending)) {
                    continue;
                }

                nodeMessages.add(pending);
                currentBatchSize += message.length;
            }


            if (nodeMessages.isEmpty()) {
                continue;
            }

            Log.debug(
                    "message send client to server: $uidFrom -> $uidTo",
                    "uidFrom", client.getUid(),
                    "uidTo", messageNode.consumer
            );



            for (Tuple2<byte[], AFuture> pending : nodeMessages) {
                AFuture messageFuture = pending.val2();

                Message outboundMessage = new Message(
                        messageNode.consumer,
                        pending.val1()
                );

                try {
                    a.sendMessageWithResult(outboundMessage)
                            .to(messageFuture::tryDone)
                            .onError(messageFuture::tryError);
                } catch (Throwable error) {
                    messageFuture.tryError(error);
                }
            }


        }



        sendPingIfNeeded(a);
    }



    private long resolvedPingIntervalMs() {
        long pingInterval =
                client.getPingTime();

        return pingInterval > 0L
                ? pingInterval
                : 6_000L;
    }

    private void scheduleNextPing(
            long sentAtMs,
            long fullPingIntervalMs
    ) {
        long delayMs =
                pingRttHistory.nextPingDelayMs(
                        fullPingIntervalMs
                );

        long nextPingTime =
                delayMs > Long.MAX_VALUE - sentAtMs
                        ? Long.MAX_VALUE
                        : sentAtMs + delayMs;

        nextPingAtMs.set(nextPingTime);
    }

    private long recordSuccessfulPingRtt(
            long startedNs,
            long sentAtMs,
            long fullPingIntervalMs
    ) {
        long rttNs =
                Math.max(
                        1L,
                        System.nanoTime() - startedNs
                );

        pingRttHistory.record(rttNs);

        scheduleNextPing(
                sentAtMs,
                fullPingIntervalMs
        );

        return rttNs;
    }


    private void sendPingIfNeeded(AuthorizedApiRemote api) {
        long now = RU.time();

        long scheduledPingTime =
                nextPingAtMs.get();

        if (scheduledPingTime != 0L
                && now < scheduledPingTime) {
            return;
        }

        PingAttempt pingAttempt =
                new PingAttempt();

        if (!isWritable()
                || !activePing.compareAndSet(
                null,
                pingAttempt
        )) {
            return;
        }

        final long fullPingIntervalMs =
                resolvedPingIntervalMs();

        final long rxWindowMs =
                RX_WINDOW_MS;

        final long sentAtMs =
                RU.time();

        final long startedNs =
                System.nanoTime();

        scheduleNextPing(
                sentAtMs,
                fullPingIntervalMs
        );

        startBackgroundPingTimeout(
                pingAttempt
        );

        try {
            api.ping(
                    fullPingIntervalMs,
                    rxWindowMs
            ).to(() -> completeBackgroundPing(
                    pingAttempt,
                    startedNs,
                    sentAtMs,
                    fullPingIntervalMs,
                    rxWindowMs
            )).onError(error -> failBackgroundPing(
                    pingAttempt,
                    error,
                    "Ping failed, will retry after ping interval"
            ));
        } catch (Throwable error) {
            failBackgroundPing(
                    pingAttempt,
                    error,
                    "Failed to send ping, will retry after ping interval"
            );
        }
    }











    @Override
    public void sendSafeApiDataMulti(byte backId, LoginClientStream data) {
        throw new UnsupportedOperationException();
    }


    @Override
    public void sendSafeApiData(LoginClientStream data) {
        data.asIn()
                .convert(cryptoEngine::decrypt)
                .ctx(authorizedApi.getFastMetaContext())
                .accept();
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
        sendPingIfNeeded(authorizedApi);
    }

    private PingAttempt acquireMeasuredPing(
            ARFuture<Long> result
    ) {
        PingAttempt pingAttempt =
                new PingAttempt();

        long waitDeadline =
                RU.time() + PING_ACQUIRE_TIMEOUT_MS;

        while (!activePing.compareAndSet(
                null,
                pingAttempt
        )) {
            if (RU.time() >= waitDeadline) {
                result.tryError(
                        new IllegalStateException(
                                "Timeout waiting for idle connection before measured ping"
                        )
                );
                return null;
            }

            try {
                Thread.sleep(1L);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                result.tryError(error);
                return null;
            }
        }

        if (!isWritable()) {
            activePing.compareAndSet(
                    pingAttempt,
                    null
            );

            result.tryError(
                    new IllegalStateException(
                            "Connection is not writable before measured ping"
                    )
            );
            return null;
        }

        return pingAttempt;
    }


    private void startMeasuredPingTimeout(
            PingAttempt pingAttempt,
            ARFuture<Long> result
    ) {
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(
                        PING_RESPONSE_TIMEOUT_MS
                );
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return;
            }

            if (activePing.compareAndSet(
                    pingAttempt,
                    null
            )) {
                firstAuth = false;
            }

            result.tryError(
                    new IllegalStateException(
                            "Timeout waiting for measured ping response"
                    )
            );
        });
    }


    private void completeMeasuredPing(
            PingAttempt pingAttempt,
            ARFuture<Long> result,
            long startedNs,
            long sentAtMs,
            long fullPingIntervalMs
    ) {
        if (!activePing.compareAndSet(
                pingAttempt,
                null
        )) {
            result.tryError(
                    new IllegalStateException(
                            "Measured ping attempt is no longer active"
                    )
            );
            return;
        }

        long rttNs =
                recordSuccessfulPingRtt(
                        startedNs,
                        sentAtMs,
                        fullPingIntervalMs
                );

        firstAuth = true;

        result.tryDone(rttNs);
    }


    private void failMeasuredPing(
            PingAttempt pingAttempt,
            ARFuture<Long> result,
            Throwable error
    ) {
        if (activePing.compareAndSet(
                pingAttempt,
                null
        )) {
            firstAuth = false;
        }

        result.tryError(error);
    }


    private void runMeasuredPing(ARFuture<Long> result) {
        PingAttempt pingAttempt =
                acquireMeasuredPing(result);

        if (pingAttempt == null) {
            return;
        }

        long fullPingIntervalMs =
                resolvedPingIntervalMs();

        long rxWindowMs =
                RX_WINDOW_MS;

        long sentAtMs =
                RU.time();

        long startedNs =
                System.nanoTime();

        scheduleNextPing(
                sentAtMs,
                fullPingIntervalMs
        );

        startMeasuredPingTimeout(
                pingAttempt,
                result
        );

        try {
            authorizedApi.ping(
                    fullPingIntervalMs,
                    rxWindowMs
            ).to(() -> completeMeasuredPing(
                    pingAttempt,
                    result,
                    startedNs,
                    sentAtMs,
                    fullPingIntervalMs
            )).onError(error -> failMeasuredPing(
                    pingAttempt,
                    result,
                    error
            ));
        } catch (Throwable error) {
            failMeasuredPing(
                    pingAttempt,
                    result,
                    error
            );
        }
    }

    public ARFuture<Long> measurePingNs() {
        ARFuture<Long> result = ARFuture.make();

        Thread.startVirtualThread(
                () -> runMeasuredPing(result)
        );

        return result;
    }
    private void startBackgroundPingTimeout(
            PingAttempt pingAttempt
    ) {
        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(
                        PING_RESPONSE_TIMEOUT_MS
                );
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return;
            }

            if (!activePing.compareAndSet(
                    pingAttempt,
                    null
            )) {
                return;
            }

            firstAuth = false;

            Log.warn(
                    "Ping response timed out, will retry after ping interval"
            );
        });
    }
    private void completeBackgroundPing(
            PingAttempt pingAttempt,
            long startedNs,
            long sentAtMs,
            long fullPingIntervalMs,
            long rxWindowMs
    ) {
        if (!activePing.compareAndSet(
                pingAttempt,
                null
        )) {
            return;
        }

        long rttNs =
                recordSuccessfulPingRtt(
                        startedNs,
                        sentAtMs,
                        fullPingIntervalMs
                );

        firstAuth = true;

        Log.debug(
                "Ping response received",
                "nextConnectMsDuration",
                fullPingIntervalMs,
                "rxWindowMs",
                rxWindowMs,
                "rttNs",
                rttNs,
                "nextPingAtMs",
                nextPingAtMs.get()
        );
    }
    private void failBackgroundPing(
            PingAttempt pingAttempt,
            Throwable error,
            String message
    ) {
        if (!activePing.compareAndSet(
                pingAttempt,
                null
        )) {
            return;
        }

        firstAuth = false;

        Log.warn(
                message,
                error
        );
    }
}