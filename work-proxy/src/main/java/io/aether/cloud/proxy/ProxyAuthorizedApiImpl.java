
package io.aether.cloud.proxy;

import io.aether.api.clientserverapi.AuthorizedApi;
import io.aether.api.clientserverapi.ClientApiSafeRemote;
import io.aether.api.clientserverapi.ClientApiStream;
import io.aether.api.clientserverapi.ClientActivity;
import io.aether.api.clientserverapi.ClientConnectionInfo;
import io.aether.api.clientserverapi.ClientLogEntry;
import io.aether.api.clientserverapi.ClientTiming;
import io.aether.api.clientserverapi.Message;
import io.aether.api.clientserverapi.MessageInfo;

import io.aether.api.clientserverapi.Telemetry;
import io.aether.api.clientserverapi.Uap;
import io.aether.api.clientserverapi.WebRtcSession;
import io.aether.api.common.AccessCheckPair;
import io.aether.api.common.AccessGroup;
import io.aether.api.common.AppliedConfig;
import io.aether.api.common.CloudConfig;
import io.aether.api.common.ServerDescriptor;
import io.aether.api.common.ServerDescriptorWithGeo;
import io.aether.logger.Log;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Downstream AuthorizedApi facade of the proxy.
 *
 * <p>LOCAL: ping / receive windows / messages / resolveClouds / resolverServers
 * / reportAppliedConfig / getServers (proxy descriptor added).
 *
 * <p>FORWARD: everything else to the per-client upstream AetherCloudClient.
 */

public class ProxyAuthorizedApiImpl
        implements AuthorizedApi,
        ProxyLoginApiImpl.LoginStreamRouting {

    protected final ProxyService service;
    protected final ProxyClientDescriptor client;
    protected final ClientApiSafeRemote safeRemoteApi;
    protected final ProxyLoginApiImpl.LoginStreamTransport transport;
    protected final ProxyLoginApiImpl.LoginStreamSession loginStreamSession;
    public short backId = -1;

    public ProxyAuthorizedApiImpl(
            ProxyService service,
            ProxyClientDescriptor client,
            ClientApiSafeRemote safeRemoteApi,
            ProxyLoginApiImpl.LoginStreamSession loginStreamSession,
            ProxyLoginApiImpl.LoginStreamTransport transport
    ) {
        this.service = service;
        this.client = client;
        this.safeRemoteApi = safeRemoteApi;
        this.loginStreamSession = loginStreamSession;
        this.transport = transport;
    }


    // ---------------- LOCAL ----------------


    @Override
    public void backId(short id) {
        this.backId = id;
    }


    @Override
    public void requestSignedUid(io.aether.api.common.KeySignPublic[] keys) {
        if (keys == null || keys.length == 0) {
            throw new io.aether.net.fastMeta.AetherException("keys is empty");
        }
        io.aether.api.common.Sign[] signs = new io.aether.api.common.Sign[keys.length];
        for (int i = 0; i < keys.length; i++) {
            signs[i] = service.requestSignedUid(client.uid(), keys[i]);
        }
        safeRemoteApi.sendSignedUid(keys, signs);
    }




    @Override
    public void flushLoginStream(byte[] data) {
        if (backId != -1) {
            transport.sendMulti(backId, data);
            return;
        }
        transport.send(data);
    }



    @Override
    public void ping(long nextConnectMsDuration, long rxWindowMs) {
        client.applyPing(System.currentTimeMillis(), nextConnectMsDuration, rxWindowMs);
        service.flushMessageGrid();
        safeRemoteApi.sendPingAck();
    }




    @Override
    public void sendMessage(Message msg) {
        sendMessageWithResult(msg);
    }

    @Override
    public void sendMessages(Message[] msg) {
        if (msg == null) return;
        for (Message m : msg) {
            sendMessage(m);
        }
    }

    @Override
    public void sendMulticast(UUID[] uids, byte[] data) {
        if (uids == null) return;
        for (UUID uid : uids) {
            sendMessage(new Message(uid, data));
        }
    }

    @Override
    public void sendMessageWithResult(Message msg) {
        if (!service.clients().isLocal(msg.getUid())) {
            Log.warn("Routing error: non-local recipient reached proxy sendMessage",
                    "fromUid", client.uid(),
                    "toUid", msg.getUid());
            return;
        }
        if (service.enqueueMessage(client, msg)) {
            service.flushMessageGrid();
        }
    }




    @Override
    public void resolverServers(short[] sid) {
        if (sid == null) return;
        for (short s : sid) {
            if (s == service.proxySid()) {
                safeRemoteApi.sendServerDescriptors(
                        new ServerDescriptor[]{
                                service.proxyDescriptor()
                        });
            } else {
                var up = client.upstreamClient();
                if (up != null) {
                    up.getServer(s).to(descriptor -> {
                        if (descriptor != null) {
                            safeRemoteApi.sendServerDescriptors(
                                    new ServerDescriptor[]{descriptor}
                            );
                        }
                    });
                }
            }
        }
    }



    @Override
    public void resolveClouds(UUID[] uids) {
        if (uids == null) return;
        List<UUID> localUids = new ArrayList<>();
        List<UUID> remoteUids = new ArrayList<>();
        for (UUID uid : uids) {
            if (service.clients().isLocal(uid)) {
                localUids.add(uid);
            } else {
                remoteUids.add(uid);
            }
        }
        if (!localUids.isEmpty()) {
            List<CloudConfig> localConfigs = new ArrayList<>();
            for (UUID uid : localUids) {
                localConfigs.add(service.proxyCloudConfig(uid));
            }
            safeRemoteApi.sendCloudConfigs(
                    localConfigs.toArray(new CloudConfig[0])
            );
        }
        var up = client.upstreamClient();
        if (up == null || remoteUids.isEmpty()) {
            return;
        }
        for (UUID uid : remoteUids) {
            up.getCloud(uid).to(cloud -> {
                if (cloud != null) {
                    safeRemoteApi.sendCloudConfigs(new CloudConfig[]{
                            new CloudConfig(
                                    uid,
                                    cloud.getConfigVersion(),
                                    cloud.toCloud()
                            )
                    });
                }
            });
        }
    }



    @Override
    public void reportAppliedConfig(AppliedConfig[] configs) {
        if (configs == null || configs.length == 0) {
            return;
        }

        List<UUID> requested = new ArrayList<>();

        for (AppliedConfig config : configs) {
            if (config == null) {
                continue;
            }
            if (config.getConfigVersion() == -1L) {
                requested.add(config.getSubjectUid());
            }
        }

        if (!requested.isEmpty()) {
            resolveClouds(requested.toArray(new UUID[0]));
        }
    }


    @Override
    public void getServers() {
        var up = client.upstreamClient();
        if (up == null) {
            safeRemoteApi.sendServers(new ServerDescriptorWithGeo[]{
                    service.proxyDescriptorWithGeo()
            });
            return;
        }
        up.serversMFuture.mapToARFuture().to(descriptors -> {
            if (descriptors == null) {
                safeRemoteApi.sendServers(new ServerDescriptorWithGeo[]{
                        service.proxyDescriptorWithGeo()
                });
                return;
            }
            ServerDescriptorWithGeo[] out = new ServerDescriptorWithGeo[descriptors.length + 1];
            System.arraycopy(descriptors, 0, out, 0, descriptors.length);
            out[descriptors.length] = service.proxyDescriptorWithGeo();
            safeRemoteApi.sendServers(out);
        });
        up.getAuthApi(AuthorizedApi::getServers);
    }


    @Override
    public void openReceiveWindow(long durationMs) {
        client.openTemporaryReceiveWindow(System.currentTimeMillis(), durationMs);
        service.flushMessageGrid();
        safeRemoteApi.sendOpenReceiveWindowAck();
    }



    @Override
    public void switchVersion(int version) {
        if (version == loginStreamSession.version) {
            return;
        }
        if (version != 0) {
            throw new IllegalArgumentException(
                    "Unsupported login stream API version: " + version
            );
        }
        ProxyAuthorizedApiV0Impl next =
                new ProxyAuthorizedApiV0Impl(
                        service,
                        client,
                        safeRemoteApi,
                        loginStreamSession,
                        transport
                );
        next.backId = backId;
        io.aether.api.clientserverapi.LoginStream.V0.switchLocalApi(
                safeRemoteApi.getFastMetaContext(),
                next
        );
        loginStreamSession.version = 0;
    }



    @Override
    public void setReceiveWindow(long startsInMs, long durationMs) {
        client.setReceiveWindow(
                System.currentTimeMillis(),
                startsInMs,
                durationMs
        );
        if (startsInMs == 0L && durationMs > 0L) {
            service.flushMessageGrid();
        }
    }


    @Override
    public void setNextReadDelay(long delayMillis) {
    }

    @Override
    public void probePacket(int testId, int sequence, byte[] payload) {
    }

    @Override
    public void requestProbeReport(int testId, int firstSequence, int count) {
    }


    @Override
    public void selfDestruct() {
        var up = client.upstreamClient();
        if (up == null) {
            service.deleteClient(client.uid(), true).to(() -> safeRemoteApi.sendSelfDestructAck());
            return;
        }
        up.withAuthApi(AuthorizedApi::selfDestruct);
        service.deleteClient(client.uid(), true).to(() -> safeRemoteApi.sendSelfDestructAck());
    }


    // ---------------- FORWARD ----------------

    @Override
    public void client(UUID uid, ClientApiStream stream) {
        forwardVoid(api -> api.client(uid, stream));
    }

    @Override
    public void createAccessGroup(UUID owner, UUID[] uids) {
        var up = client.upstreamClient();
        if (up == null) return;
        up.newAccessGroupIdMFuture.mapToARFuture().to(id ->
                safeRemoteApi.sendNewAccessGroupId(owner, id));
        up.getAuthApi(api -> api.createAccessGroup(owner, uids));
    }

    @Override
    public void addToAccessGroup(UUID groupId, UUID uid) {
        var up = client.upstreamClient();
        if (up == null) return;
        up.addToAccessGroupResults.getFuture(
                io.aether.utils.tuples.Tuple2.of(groupId, uid))
                .to(ok -> safeRemoteApi.sendAddToAccessGroupResult(groupId, uid, ok));
        up.getAuthApi(api -> api.addToAccessGroup(groupId, uid));
    }

    @Override
    public void removeFromAccessGroup(UUID groupId, UUID uid) {
        var up = client.upstreamClient();
        if (up == null) return;
        up.removeFromAccessGroupResults.getFuture(
                io.aether.utils.tuples.Tuple2.of(groupId, uid))
                .to(ok -> safeRemoteApi.sendRemoveFromAccessGroupResult(groupId, uid, ok));
        up.getAuthApi(api -> api.removeFromAccessGroup(groupId, uid));
    }

    @Override
    public void checkAccessForSendMessage(UUID uid) {
        var up = client.upstreamClient();
        if (up == null) return;
        up.accessCheckAcks.getFuture(uid)
                .to(ok -> safeRemoteApi.sendAccessCheckAck(uid));
        up.getAuthApi(api -> api.checkAccessForSendMessage(uid));
    }

    @Override
    public void getAccessGroups(UUID uid) {
        var up = client.upstreamClient();
        if (up == null) return;
        up.clientGroups.getFuture(uid).to(groups ->
                safeRemoteApi.sendAccessGroupsOfClient(
                        uid, groups.toArray(new UUID[0])));
        up.getAuthApi(api -> api.getAccessGroups(uid));
    }

    @Override
    public void getAccessGroup(UUID groupId) {
        var up = client.upstreamClient();
        if (up == null) return;
        up.accessGroups.getFuture(groupId).to(safeRemoteApi::sendAccessGroup);
        up.getAuthApi(api -> api.getAccessGroup(groupId));
    }

    @Override
    public void getAllAccessedClients(UUID uid) {
        var up = client.upstreamClient();
        if (up == null) return;
        up.allAccessedClients.getFuture(uid).to(clients ->
                safeRemoteApi.sendAccessedClients(
                        uid, clients.toArray(new UUID[0])));
        up.getAuthApi(api -> api.getAllAccessedClients(uid));
    }

    @Override
    public void checkAccessForSendMessage2(UUID uid1, UUID uid2) {
        var up = client.upstreamClient();
        if (up == null) return;
        up.accessCheckCache.getFuture(new AccessCheckPair(uid1, uid2)).to(ok ->
                safeRemoteApi.sendAccessCheckResult2(uid1, uid2, ok));
        up.getAuthApi(api -> api.checkAccessForSendMessage2(uid1, uid2));
    }

    @Override
    public void sendTelemetry(Telemetry telemetry) {
        forwardVoid(api -> api.sendTelemetry(telemetry));
    }

    @Override
    public void requestAccessGroupsForClients(UUID[] uids) {
        forwardVoid(api -> api.requestAccessGroupsForClients(uids));
    }

    @Override
    public void requestAccessGroupsItems(UUID[] ids) {
        forwardVoid(api -> api.requestAccessGroupsItems(ids));
    }

    @Override
    public void sendAccessGroupForClient(UUID uid, UUID[] groups) {
        forwardVoid(api -> api.sendAccessGroupForClient(uid, groups));
    }

    @Override
    public void addItemsToAccessGroup(UUID id, UUID[] groups) {
        forwardVoid(api -> api.addItemsToAccessGroup(id, groups));
    }

    @Override
    public void removeItemsFromAccessGroup(UUID id, UUID[] groups) {
        forwardVoid(api -> api.removeItemsFromAccessGroup(id, groups));
    }

    @Override
    public void addAccessGroupsToClient(UUID uid, UUID[] groups) {
        forwardVoid(api -> api.addAccessGroupsToClient(uid, groups));
    }

    @Override
    public void removeAccessGroupsFromClient(UUID uid, UUID[] groups) {
        forwardVoid(api -> api.removeAccessGroupsFromClient(uid, groups));
    }

    @Override
    public void requestAllAccessedClients(UUID[] uids) {
        forwardVoid(api -> api.requestAllAccessedClients(uids));
    }

    @Override
    public void requestAccessCheck(AccessCheckPair[] requests) {
        forwardVoid(api -> api.requestAccessCheck(requests));
    }

    @Override
    public void getClientActivity(UUID uid, long fromTime, long toTime, int limit) {
        var up = client.upstreamClient();
        if (up == null) return;
        up.clientActivities.getFuture(uid).to(a -> safeRemoteApi.sendClientActivity(uid, a));
        up.getAuthApi(api -> api.getClientActivity(uid, fromTime, toTime, limit));
    }

    @Override
    public void searchClientLogs(UUID uid, String query, int limit) {
        var up = client.upstreamClient();
        if (up == null) return;
        up.clientLogs.getFuture(uid).to(l -> safeRemoteApi.sendClientLogs(uid, l));
        up.getAuthApi(api -> api.searchClientLogs(uid, query, limit));
    }

    @Override
    public void getClientConnections(UUID uid, int limit) {
        var up = client.upstreamClient();
        if (up == null) return;
        up.clientConnections.getFuture(uid).to(c -> safeRemoteApi.sendClientConnections(uid, c));
        up.getAuthApi(api -> api.getClientConnections(uid, limit));
    }

    @Override
    public void getClientMessages(UUID uid, long fromTime, long toTime, int limit) {
        var up = client.upstreamClient();
        if (up == null) return;
        up.clientMessages.getFuture(uid).to(m -> safeRemoteApi.sendClientMessages(uid, m));
        up.getAuthApi(api -> api.getClientMessages(uid, fromTime, toTime, limit));
    }

    @Override
    public void getUap(UUID uid) {
        var up = client.upstreamClient();
        if (up == null) return;
        up.uaps.getFuture(uid).to(u -> safeRemoteApi.sendUap(uid, u));
        up.getAuthApi(api -> api.getUap(uid));
    }

    @Override
    public void requestWebRtcSession(UUID uid) {
        if (service.clients().isLocal(uid)) {
            ProxyClientDescriptor local = service.clients().getByUid(uid);
            safeRemoteApi.sendWebRtcSession(uid, local == null ? null : local.webRtcSession);
            return;
        }
        var up = client.upstreamClient();
        if (up == null) return;

        up.requestWebRtcSession(uid).to(session ->
                safeRemoteApi.sendWebRtcSession(uid, session));

    }

    @Override
    public void publishWebRtcSession(WebRtcSession session) {
        client.webRtcSession = session;
        forwardVoid(api -> api.publishWebRtcSession(session));
    }

    @Override
    public void getClientTiming(UUID uid) {
        var up = client.upstreamClient();
        if (up == null) return;
        up.clientTimings.getFuture(uid).to(t -> safeRemoteApi.sendClientTiming(uid, t));
        up.getAuthApi(api -> api.getClientTiming(uid));
    }


    @Override
    public void getAlias() {
        var up = client.upstreamClient();
        if (up == null) return;
        up.aliasMFuture.mapToARFuture().to(safeRemoteApi::sendAlias);
        up.getAuthApi(AuthorizedApi::getAlias);
    }

    @Override
    public void setAlias(UUID alias) {
        var up = client.upstreamClient();
        if (up == null) {
            safeRemoteApi.sendSetAliasAck();
            return;
        }
        up.getAuthApi(api -> api.setAlias(alias));
        safeRemoteApi.sendSetAliasAck();
    }



    // ---------------- Helpers ----------------

    private void forwardVoid(Consumer<AuthorizedApi> call) {
        var up = client.upstreamClient();
        if (up == null) {
            Log.warn("ProxyAuthorizedApiImpl: no upstream client for uid",
                    "uid", client.uid());
            return;
        }
        up.getAuthApi(call::accept);
    }


}