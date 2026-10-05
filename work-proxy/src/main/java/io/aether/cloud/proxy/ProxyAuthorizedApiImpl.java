
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
    public ARFuture<io.aether.api.common.PeerIdentity> issuePeerIdentity(
            io.aether.api.common.KeySignPublic key) {
        return ARFuture.of(service.issuePeerIdentity(client.uid(), key));
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
    public AFuture ping(long nextConnectMsDuration, long rxWindowMs) {
        client.applyPing(
                System.currentTimeMillis(),
                nextConnectMsDuration,
                rxWindowMs
        );
        service.flushMessageGrid();
        return AFuture.completed();
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
    public AFuture sendMessageWithResult(Message msg) {
        AFuture result = AFuture.make();
        if (!service.clients().isLocal(msg.getUid())) {
            Log.warn(
                    "Routing error: non-local recipient reached proxy sendMessage",
                    "fromUid", client.uid(),
                    "toUid", msg.getUid()
            );
            result.error(new IllegalStateException(
                    "Recipient is not local to this proxy"));
            return result;
        }
        boolean accepted = service.enqueueMessage(client, msg);
        if (accepted) {
            service.flushMessageGrid();
            result.done();
        } else {
            result.error(new IllegalStateException(
                    "Message was not accepted by proxy queue"));
        }
        return result;
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
    public ARFuture<ServerDescriptorWithGeo[]> getServers() {
        var up = client.upstreamClient();
        if (up == null) {
            return ARFuture.of(new ServerDescriptorWithGeo[]{
                    service.proxyDescriptorWithGeo()
            });
        }
        return up.getServers().map(descriptors -> {
            if (descriptors == null) {
                return new ServerDescriptorWithGeo[]{
                        service.proxyDescriptorWithGeo()
                };
            }
            ServerDescriptorWithGeo[] out =
                    new ServerDescriptorWithGeo[descriptors.length + 1];
            System.arraycopy(descriptors, 0, out, 0, descriptors.length);
            out[descriptors.length] = service.proxyDescriptorWithGeo();
            return out;
        });
    }


    @Override
    public AFuture openReceiveWindow(long durationMs) {
        client.openTemporaryReceiveWindow(
                System.currentTimeMillis(),
                durationMs
        );
        service.flushMessageGrid();
        return AFuture.completed();
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
    public AFuture selfDestruct() {
        AFuture result = AFuture.make();
        var up = client.upstreamClient();
        if (up == null) {
            service.deleteClient(client.uid(), true)
                    .to(() -> result.done(), error -> result.error(error));
            return result;
        }
        up.getAuthApi1(api -> {
            api.selfDestruct().to(
                    () -> service.deleteClient(client.uid(), true)
                            .to(() -> result.done(), error -> result.error(error)),
                    error -> result.error(error)
            );
            return ARFuture.of(null);
        });
        return result;
    }


    // ---------------- FORWARD ----------------

    @Override
    public void client(UUID uid, ClientApiStream stream) {
        forwardVoid(api -> api.client(uid, stream));
    }

    @Override
    public ARFuture<UUID> createAccessGroup(UUID owner, UUID[] uids) {
        return forward(api -> api.createAccessGroup(owner, uids));
    }

    @Override
    public ARFuture<Boolean> addToAccessGroup(UUID groupId, UUID uid) {
        return forward(api -> api.addToAccessGroup(groupId, uid));
    }

    @Override
    public ARFuture<Boolean> removeFromAccessGroup(UUID groupId, UUID uid) {
        return forward(api -> api.removeFromAccessGroup(groupId, uid));
    }

    @Override
    public AFuture checkAccessForSendMessage(UUID uid) {
        return forwardAFuture(api -> api.checkAccessForSendMessage(uid));
    }

    @Override
    public ARFuture<UUID[]> getAccessGroups(UUID uid) {
        return forward(api -> api.getAccessGroups(uid));
    }

    @Override
    public ARFuture<AccessGroup> getAccessGroup(UUID groupId) {
        return forward(api -> api.getAccessGroup(groupId));
    }

    @Override
    public ARFuture<UUID[]> getAllAccessedClients(UUID uid) {
        return forward(api -> api.getAllAccessedClients(uid));
    }

    @Override
    public ARFuture<Boolean> checkAccessForSendMessage2(UUID uid1, UUID uid2) {
        return forward(api -> api.checkAccessForSendMessage2(uid1, uid2));
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
    public ARFuture<ClientActivity[]> getClientActivity(UUID uid, long fromTime, long toTime, int limit) {
        return forward(api -> api.getClientActivity(uid, fromTime, toTime, limit));
    }

    @Override
    public ARFuture<ClientLogEntry[]> searchClientLogs(UUID uid, String query, int limit) {
        return forward(api -> api.searchClientLogs(uid, query, limit));
    }

    @Override
    public ARFuture<ClientConnectionInfo[]> getClientConnections(UUID uid, int limit) {
        return forward(api -> api.getClientConnections(uid, limit));
    }

    @Override
    public ARFuture<MessageInfo[]> getClientMessages(UUID uid, long fromTime, long toTime, int limit) {
        return forward(api -> api.getClientMessages(uid, fromTime, toTime, limit));
    }

    @Override
    public ARFuture<Uap> getUap(UUID uid) {
        return forward(api -> api.getUap(uid));
    }

    @Override
    public ARFuture<WebRtcSession> requestWebRtcSession(UUID uid) {
        if (service.clients().isLocal(uid)) {
            ProxyClientDescriptor local = service.clients().getByUid(uid);
            return ARFuture.of(local == null ? null : local.webRtcSession);
        }
        return forward(api -> api.requestWebRtcSession(uid));
    }

    @Override
    public void publishWebRtcSession(WebRtcSession session) {
        client.webRtcSession = session;
        forwardVoid(api -> api.publishWebRtcSession(session));
    }

    @Override
    public ARFuture<ClientTiming> getClientTiming(UUID uid) {
        return forward(api -> api.getClientTiming(uid));
    }


    @Override
    public ARFuture<UUID> getAlias() {
        return forward(AuthorizedApi::getAlias);
    }

    @Override
    public AFuture setAlias(UUID alias) {
        return forwardAFuture(api -> api.setAlias(alias));
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

    private <T> ARFuture<T> forward(Function<AuthorizedApi, ARFuture<T>> call) {
        var up = client.upstreamClient();
        if (up == null) {
            return ARFuture.ofThrow(new IllegalStateException(
                    "No upstream client for uid " + client.uid()));
        }
        return up.getAuthApi1(call::apply);
    }

    private AFuture forwardAFuture(Function<AuthorizedApi, AFuture> call) {
        AFuture result = AFuture.make();
        var up = client.upstreamClient();
        if (up == null) {
            result.error(new IllegalStateException(
                    "No upstream client for uid " + client.uid()));
            return result;
        }
        up.getAuthApi1(api -> {
            call.apply(api).to(result::done);
            return ARFuture.of(null);
        });
        return result;
    }
}