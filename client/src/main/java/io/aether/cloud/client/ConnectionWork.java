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
    final AuthorizedApiRemote authorizedApiV0;

    final ClientApiSafeImpl clientApiSafe;

    volatile AuthorizedApiRemote authorizedApi;
    private volatile int negotiatedLoginApiVersion = -1;
    private final ServerDescriptor serverDescriptor;


    private static final long PING_RESPONSE_TIMEOUT_MS = 5_000L;
    private static final long PROBE_REPORT_TIMEOUT_MS = 120_000L;

    private static final long RX_WINDOW_MS = 60_000L;

    private static final long AUTO_TAG_THRESHOLD_MS = 5_000L;
    private static final long AUTO_TAG_MAX = 1_000_000L;
    private final java.util.concurrent.atomic.AtomicLong lastRequestAtMs =
            new java.util.concurrent.atomic.AtomicLong(0L);
    private final java.util.concurrent.atomic.AtomicLong autoTagCounter =
            new java.util.concurrent.atomic.AtomicLong(0L);
    private volatile boolean autoTagActive = false;



    private static final class PingAttempt {
        private final AFuture ack = AFuture.make();
    }

    private record ProbeReportKey(
            int testId,
            int firstSequence,
            int count
    ) {
    }

    private final java.util.concurrent.ConcurrentHashMap<
            ProbeReportKey,
            ARFuture<ProbeReport>
            > pendingProbeReports =
            new java.util.concurrent.ConcurrentHashMap<>();


    private final AtomicReference<PingAttempt> activePing =
            new AtomicReference<>();

    boolean basicStatus;

    private final PingRttHistory pingRttHistory =
            new PingRttHistory();

    private final AtomicLong nextPingAtMs =
            new AtomicLong();

    volatile boolean firstAuth;



    public ConnectionWork(
            AetherCloudClient client,
            ServerDescriptor s
    ) {
        this(
                client,
                s,
                s.getIpAddress().getURI(AetherCodec.UDP)
        );
    }

    public ConnectionWork(
            AetherCloudClient client,
            ServerDescriptor s,
            java.net.URI physicalUri
    ) {
        super(
                client,
                physicalUri,
                ClientApiUnsafe.META,
                LoginApi.META
        );


        cryptoEngine =
                client.getCryptoEngineForServer(
                        s.getId()
                );

        if (cryptoEngine == null) {
            Log.error(
                    "ConnectionWork: cryptoEngine is null for server "
                            + s.getId()
                            + ". Authentication will fail."
            );
        }

        serverDescriptor = s;
        this.basicStatus = false;


        ClientApiSafeImpl safe = new ClientApiSafeImpl(this, client);
        clientApiSafe = safe;
        authorizedApiV0 =
                getRootApi().openLoginByAlias(
                        client.getAlias(),
                        c -> safe,
                        cryptoEngine::encrypt,
                        "loginByAlias"
                );



        authorizedApi = wrapForAutoTag(authorizedApiV0);


        this.rootApi = wrapRootForAutoTag(this.rootApi);



        negotiateLoginApiVersion();
        markReady();

    }

    private void negotiateLoginApiVersion() {
        if (negotiatedLoginApiVersion != -1) {
            return;
        }

        AuthorizedApiRemote v0 = authorizedApiV0;
        if (v0 == null) {
            return;
        }

        int version = client.getLoginApiVersion();
        if (version != 0) {
            throw new IllegalArgumentException(
                    "Unsupported LoginStream API version: "
                            + version
            );
        }


        authorizedApi = wrapForAutoTag(v0);

        negotiatedLoginApiVersion = 0;
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

        if (!isWritable) {
            negotiatedLoginApiVersion = -1;

            if (authorizedApiV0 != null) {

                authorizedApi = wrapForAutoTag(authorizedApiV0);

            }
        }

        if (cryptoEngine == null && isWritable) {
            Log.warn(
                    "onConnectionStateChanged called before cryptoEngine initialized, deferring flush"
            );
            stateListeners.fire(true);
            return;
        }


        if (isWritable) {
            if (nextPingAtMs != null) {
                nextPingAtMs.set(0L);
            }
            negotiateLoginApiVersion();


            Log.info(
                    "Network restored. Resetting auth state and forcing flush.",
                    "uri",
                    uri,
                    "loginApiVersion",
                    negotiatedLoginApiVersion
            );
        } else {
            PingAttempt attempt = activePing == null ? null : activePing.getAndSet(null);
            if (attempt != null) {
                attempt.ack.tryError(
                        new IllegalStateException("Connection lost during ping")
                );
            }
        }

        if (!isWritable && pendingProbeReports != null) {
            pendingProbeReports.forEach((key, pending) -> {
                if (pendingProbeReports.remove(key, pending)) {
                    pending.tryError(
                            new IllegalStateException(
                                    "Connection lost while waiting for probe report"
                            )
                    );
                }
            });
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

        if (!firstAuth) {
            sendPingIfNeeded(a);
            return;
        }


        for (var messageNode : client.messageNodeMap.values()) {

            if (!messageNode.connectionsOut.contains(this)) {
                continue;
            }


            // Peer-first attempt: always try to drain the node's buffer via
            // the peer link first. If the link is up, everything is
            // delivered directly and we skip the cloud path entirely.
            if (messageNode.tryFlushAllViaPeer()) {
                continue;
            }
            // If a probe is still in its grace window, hold the node for a
            // bit longer so the LAN handshake can finish. After the window
            // expires we fall through to the ordinary cloud path.
            if (messageNode.isPeerProbeGraceActive()) {
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

                    a.sendMessageWithResult(outboundMessage);
                    messageFuture.tryDone();

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




    /**
     * Preserves the legacy immediate RX window while explicitly scheduling
     * the next receive window for queued server-to-client data.
     */

    private AFuture sendPingWithReceiveWindow(
            AuthorizedApiRemote api,
            long nextConnectMsDuration,
            long rxWindowMs
    ) {
        PingAttempt attempt = activePing.get();
        if (attempt == null) {
            AFuture failed = AFuture.make();
            failed.tryError(
                    new IllegalStateException("No active ping attempt")
            );
            return failed;
        }

        try {
            api.ping(nextConnectMsDuration, rxWindowMs);
            api.setReceiveWindow(nextConnectMsDuration, rxWindowMs);
        } catch (Throwable error) {
            attempt.ack.tryError(error);
        }

        return attempt.ack;
    }



    /**
     * Handles a server ping acknowledgement for this connection.
     */
    void onPingAck() {
        PingAttempt attempt = activePing.get();
        if (attempt != null && activePing.compareAndSet(attempt, null)) {
            attempt.ack.tryDone();
        }
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
            sendPingWithReceiveWindow(
                    api,
                    fullPingIntervalMs,
                    rxWindowMs
            ).to(() -> completeBackgroundPing(
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
    public void sendSafeApiDataMulti(short backId, LoginClientStream data) {
        throw new UnsupportedOperationException(
                "Multi-backId send requires a configured peer relay");
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

        if (!activePing.compareAndSet(null, pingAttempt)) {
            result.tryError(
                    new IllegalStateException(
                            "Another ping is already in progress"
                    )
            );
            return null;
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
        java.util.concurrent.CompletableFuture.delayedExecutor(
                PING_RESPONSE_TIMEOUT_MS,
                java.util.concurrent.TimeUnit.MILLISECONDS
        ).execute(() -> {
            if (activePing.compareAndSet(pingAttempt, null)) {
                firstAuth = false;
                IllegalStateException error =
                        new IllegalStateException("Measured ping ACK timeout");
                pingAttempt.ack.tryError(error);
                result.tryError(error);
            }
        });
    }


    private void completeMeasuredPing(
            ARFuture<Long> result,
            long startedNs,
            long sentAtMs,
            long fullPingIntervalMs
    ) {
        // The ACK handler has already claimed and completed this attempt.

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
        if (activePing.compareAndSet(pingAttempt, null)) {
            firstAuth = false;
            pingAttempt.ack.tryError(error);
            result.tryError(error);
        }
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
            sendPingWithReceiveWindow(
                    authorizedApi,
                    fullPingIntervalMs,
                    rxWindowMs
            ).to(() -> completeMeasuredPing(
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


    /**
     * Sends one diagnostic probe packet and immediately flushes its FastMeta
     * context so consecutive probes are emitted as separate transport packets.
     *
     * @param testId probe run identifier
     * @param sequence sequence assigned by the caller
     * @param payload arbitrary bytes used to control probe packet size
     */
    public void sendProbePacket(
            int testId,
            int sequence,
            byte[] payload
    ) {
        java.util.Objects.requireNonNull(payload, "payload");

        if (!isWritable()) {
            throw new IllegalStateException(
                    "Connection is not writable for probe packet"
            );
        }

        AuthorizedApiRemote api = authorizedApi;
        api.probePacket(testId, sequence, payload);
        api.getFastMetaContext().flush();
    }


    /**
     * Requests a server snapshot for a previously sent probe range.
     *
     * <p>The returned future completes when the matching reverse
     * {@link ProbeReport} arrives. The server may defer that report until the
     * next declared receive window.
     *
     * @param testId probe run identifier
     * @param firstSequence first requested sequence
     * @param count number of consecutive sequence values
     * @return future completed by the matching reverse probe report
     */
    public ARFuture<ProbeReport> requestProbeReport(
            int testId,
            int firstSequence,
            int count
    ) {
        ARFuture<ProbeReport> result = ARFuture.make();

        if (count < 0) {
            result.tryError(
                    new IllegalArgumentException(
                            "Probe report count must be non-negative: " + count
                    )
            );
            return result;
        }

        ProbeReportKey key =
                new ProbeReportKey(testId, firstSequence, count);

        ARFuture<ProbeReport> existing =
                pendingProbeReports.putIfAbsent(key, result);

        if (existing != null) {
            return existing;
        }

        if (!isWritable()) {
            pendingProbeReports.remove(key, result);
            result.tryError(
                    new IllegalStateException(
                            "Connection is not writable for probe report request"
                    )
            );
            return result;
        }

        try {
            AuthorizedApiRemote api = authorizedApi;
            api.requestProbeReport(testId, firstSequence, count);
            api.getFastMetaContext().flush();
        } catch (Throwable error) {
            pendingProbeReports.remove(key, result);
            result.tryError(error);
            return result;
        }

        java.util.concurrent.CompletableFuture.delayedExecutor(
                PROBE_REPORT_TIMEOUT_MS,
                java.util.concurrent.TimeUnit.MILLISECONDS
        ).execute(() -> {
            if (pendingProbeReports.remove(key, result)) {
                result.tryError(
                        new IllegalStateException(
                                "Timeout waiting for probe report: " + key
                        )
                );
            }
        });


        return result;
    }


    /**
     * Completes the client-side diagnostic request matching a reverse report.
     *
     * @param report report delivered by ClientApiSafe
     */
    void onProbeReport(ProbeReport report) {
        if (report == null) {
            return;
        }

        ProbeReportKey key = new ProbeReportKey(
                report.getTestId(),
                report.getFirstSequence(),
                report.getCount()
        );

        ARFuture<ProbeReport> pending =
                pendingProbeReports.remove(key);

        if (pending != null) {
            pending.tryDone(report);
        }
    }



    public ARFuture<Long> measurePingNs() {
        ARFuture<Long> result = ARFuture.make();
        runMeasuredPing(result);
        return result;
    }
    private void startBackgroundPingTimeout(
            PingAttempt pingAttempt
    ) {
        java.util.concurrent.CompletableFuture.delayedExecutor(
                PING_RESPONSE_TIMEOUT_MS,
                java.util.concurrent.TimeUnit.MILLISECONDS
        ).execute(() -> {
            if (!activePing.compareAndSet(pingAttempt, null)) {
                return;
            }

            firstAuth = false;
            pingAttempt.ack.tryError(
                    new IllegalStateException("Background ping ACK timeout")
            );
            Log.warn("Ping response timed out, will retry after ping interval");
        });
    }
    private void completeBackgroundPing(
            long startedNs,
            long sentAtMs,
            long fullPingIntervalMs,
            long rxWindowMs
    ) {
        // Invoked only after successful completion of the ACK future.

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
        if (!activePing.compareAndSet(pingAttempt, null)) {
            return;
        }

        firstAuth = false;
        pingAttempt.ack.tryError(error);
        Log.warn(message, error);
    }


    @Override
    public void sendCurrentTimeUtc(long time) {
        Log.warn("ConnectionWork: unexpected sendCurrentTimeUtc");
    }

    @Override
    public void sendMyIpInfo(io.aether.api.common.IpInfo info) {
        client.myIpMFuture.set(info);
    }

    @Override
    public void enterRegistration(ClientApiRegSafeStream stream) {
        Log.warn("ConnectionWork: unexpected enterRegistration");
    }

    @Override
    public void sendAsymmetricPublicKey(io.aether.api.common.SignedKey key) {
        Log.warn("ConnectionWork: unexpected sendAsymmetricPublicKey");
    }

    @Override
    public void sendRegistrationDirect(RegistrationDirectResult result) {
        Log.warn("ConnectionWork: unexpected sendRegistrationDirect");
    }

    @Override
    public void sendWorkProofData(java.util.UUID parent, io.aether.api.common.WorkProofDTO dto) {
        Log.warn("ConnectionWork: unexpected sendWorkProofData");
    }

    @Override
    public void sendResolvedServers(io.aether.api.common.ServerDescriptor[] servers) {
        Log.warn("ConnectionWork: unexpected sendResolvedServers");
    }

    @Override
    public void sendRecoveredClientState(RecoveryResult result) {
        Log.warn("ConnectionWork: unexpected sendRecoveredClientState");
    }



    /**
     * Session id assigned to this connection (0 means "no session tagging").
     */
    public long getSessionId() {
        ClientApiSafeImpl safe = clientApiSafe;
        return safe == null ? 0L : safe.getSessionId();
    }

    /**
     * Session id of the most recently received response (0 if untagged).
     */
    public long getCurrentResponseSessionId() {
        ClientApiSafeImpl safe = clientApiSafe;
        return safe == null ? 0L : safe.getCurrentResponseSessionId();
    }




    /**
     * Enables or disables wire tagging automatically based on request rate.
     * When two consecutive outbound requests are closer than
     * {@link #AUTO_TAG_THRESHOLD_MS}, tagging turns on and the counter advances.
     * A quieter gap resets the tag back to zero exactly once.
     * Never throws: any failure is logged and ignored.
     */
    private void maybeAutoTag() {
        try {
            long now = RU.time();
            long prev = lastRequestAtMs.getAndSet(now);
            boolean frequent = prev != 0L && (now - prev) < AUTO_TAG_THRESHOLD_MS;
            AuthorizedApiRemote api = authorizedApi;
            if (api == null) {
                return;
            }
            if (frequent) {
                long next = autoTagCounter.incrementAndGet();
                if (next >= AUTO_TAG_MAX) {
                    autoTagCounter.set(1L);
                    next = 1L;
                }
                api.setSessionId(next);
                api.getFastMetaContext().flush();
                autoTagActive = true;
            } else if (autoTagActive) {
                api.setSessionId(0L);
                api.getFastMetaContext().flush();
                autoTagCounter.set(0L);
                autoTagActive = false;
            }
        } catch (Throwable t) {
            Log.warn("autoTag failed", "error", t);
        }
    }



    /**
     * Wraps the authorized remote API so every outbound application call funnels
     * through {@link #maybeAutoTag()}. Service calls are passed through untouched
     * to avoid recursion.
     */
    private AuthorizedApiRemote wrapForAutoTag(AuthorizedApiRemote original) {
        if (original == null) {
            return null;
        }
        if (java.lang.reflect.Proxy.isProxyClass(original.getClass())) {
            return original;
        }
        return (AuthorizedApiRemote) java.lang.reflect.Proxy.newProxyInstance(
                ConnectionWork.class.getClassLoader(),
                new Class<?>[]{AuthorizedApiRemote.class},
                (proxy, method, args) -> {
                    String n = method.getName();
                    boolean isService = "setSessionId".equals(n) || "getFastMetaContext".equals(n)
                            || "flush".equals(n) || "isActive".equals(n)
                            || "destroy".equals(n) || "as".equals(n);
                    if (!isService) {
                        maybeAutoTag();
                    }
                    try {
                        return method.invoke(original, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    /**
     * Wraps the login (root) remote API. Root API carries transport-level requests
     * such as {@code getMyIp} and {@code getTimeUTC}; these are application requests
     * for the purposes of rate counting, so they must feed the same auto-tag logic.
     * Handshake helpers that open the authorized API are treated as service calls.
     */
    private LoginApiRemote wrapRootForAutoTag(LoginApiRemote original) {
        if (original == null) {
            return null;
        }
        if (java.lang.reflect.Proxy.isProxyClass(original.getClass())) {
            return original;
        }
        return (LoginApiRemote) java.lang.reflect.Proxy.newProxyInstance(
                ConnectionWork.class.getClassLoader(),
                new Class<?>[]{LoginApiRemote.class},
                (proxy, method, args) -> {
                    String n = method.getName();
                    boolean isService = n.startsWith("open")
                            || "getFastMetaContext".equals(n)
                            || "flush".equals(n) || "isActive".equals(n)
                            || "destroy".equals(n) || "as".equals(n);
                    if (!isService) {
                        maybeAutoTag();
                    }
                    try {
                        return method.invoke(original, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }



    /**
     * Assigns a logical session identifier to this connection. All outbound responses
     * will then be prefixed with onResponseFor(sessionId) so the peer can attribute them.
     * Never throws: any failure is logged and tagging is simply disabled/kept unchanged.
     */
    public void setSessionId(long sessionId) {
        try {
            AuthorizedApiRemote api = authorizedApi;
            if (api == null) api = authorizedApiV0;
            if (api == null) {
                Log.warn("setSessionId: authorized api is null, tag ignored", "sessionId", sessionId);
                return;
            }
            api.setSessionId(sessionId);
            api.getFastMetaContext().flush();
        } catch (Throwable t) {
            Log.warn("setSessionId failed", "sessionId", sessionId, "error", t);
        }
    }


}