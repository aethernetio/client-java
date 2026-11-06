package io.aether.cloud.client;


import io.aether.api.clientserverapi.*;

import io.aether.api.common.*;
import io.aether.logger.Log;
import io.aether.utils.RU;
import io.aether.utils.futures.ARFuture;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.*;

/**
 * Implements the ClientApiSafe interface to handle responses from the server.
 */
class ClientApiSafeImpl implements ClientApiSafe, ServerApiByUidClient {


    private final AetherCloudClient client;

    private final ConnectionWork connection;

    public ClientApiSafeImpl(ConnectionWork connection, AetherCloudClient client) {
        this.client = client;
        this.connection = connection;
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
    public void sendAccessGroups(AccessGroup[] groups) {
        Log.debug("Received $count AccessGroups", "count", groups.length);
        for (AccessGroup group : groups) {
            if (group != null) {
                client.accessGroups.put(group.getId(), group);
            }
        }
    }

    @Override
    public void sendAccessGroupForClient(UUID uid, UUID[] groups) {
        Log.debug("Received AccessGroups for client $uid", "uid", uid);
        client.clientGroups.put(uid, Set.of(groups));
    }

    @Override
    public void addItemsToAccessGroup(UUID id, UUID[] groups) {
        Log.debug("Server confirmed ADD items to group $id", "id", id);
        Map<UUID, ARFuture<Boolean>> futures = client.accessOperationsAdd.get(id);
        if (futures != null) {
            for (UUID uid : groups) {
                ARFuture<Boolean> future = futures.remove(uid);
                if (future != null) {
                    future.tryDone(true);
                }
            }
            if (futures.isEmpty()) {
                client.accessOperationsAdd.remove(id);
            }
        }
        client.accessGroups.getFuture(id).to(group -> {
            if (group != null) {
                List<UUID> newUuids = new ArrayList<>(List.of(group.getData()));
                newUuids.addAll(List.of(groups));
                AccessGroup newGroup = new AccessGroup(group.getId(), RU.timeSeconds(), group.getOwner(), newUuids.stream().distinct().toArray(UUID[]::new));
                client.accessGroups.put(id, newGroup);
            }
        });
    }

    @Override
    public void removeItemsFromAccessGroup(UUID id, UUID[] groups) {
        Log.debug("Server confirmed REMOVE items from group $id", "id", id);
        Map<UUID, ARFuture<Boolean>> futures = client.accessOperationsRemove.get(id);
        if (futures != null) {
            for (UUID uid : groups) {
                ARFuture<Boolean> future = futures.remove(uid);
                if (future != null) {
                    future.tryDone(true);
                }
            }
            if (futures.isEmpty()) {
                client.accessOperationsRemove.remove(id);
            }
        }
        client.accessGroups.getFuture(id).to(group -> {
            if (group != null) {
                List<UUID> newUuids = new ArrayList<>(List.of(group.getData()));
                newUuids.removeAll(List.of(groups));
                AccessGroup newGroup = new AccessGroup(group.getId(), RU.timeSeconds(), group.getOwner(), newUuids.toArray(new UUID[0]));
                client.accessGroups.put(id, newGroup);
            }
        });
    }

    @Override
    public void addAccessGroupsToClient(UUID uid, UUID[] groups) {
        Log.debug("Server pushed ADD groups to client $uid", "uid", uid);
        client.clientGroups.getFuture(uid).to(existingGroups -> {
            //TODO
        });
    }

    @Override
    public void removeAccessGroupsFromClient(UUID uid, UUID[] groups) {
        Log.debug("Server pushed REMOVE groups from client $uid", "uid", uid);
        client.clientGroups.getFuture(uid).to(existingGroups -> {
            //TODO
        });
    }

    @Override
    public void sendAllAccessedClients(UUID uid, UUID[] accessedClients) {
        Log.debug("Received $count AccessedClients for $uid", "count", accessedClients.length, "uid", uid);
        client.allAccessedClients.put(uid, ObjectSet.of(accessedClients));
    }

    @Override
    public void sendAccessCheckResults(AccessCheckResult[] results) {
        Log.debug("Received $count AccessCheckResults", "count", results.length);
        for (AccessCheckResult result : results) {
            if (result != null) {
                client.accessCheckCache.put(new AccessCheckPair(result.getSourceUid(), result.getTargetUid()), result.isHasAccess());
            }
        }
    }



    /**
     * Completes the matching client-side diagnostic probe request.
     *
     * @param report server-side probe observation snapshot
     */
    @Override
    public void probeReport(ProbeReport report) {
        connection.onProbeReport(report);
    }



    public void sendMessages(Message[] msg) {
        Log.trace("receive messages: $count", "count", msg.length);
        // Adaptive Cloud: Promote connection on data receipt
        client.priorityManager.promote(client.getUid(), connection.getServerDescriptor().getId());
        // Adaptive Cloud: Promote on data receipt
        client.priorityManager.promote(client.getUid(), connection.getServerDescriptor().getId());
        for (var m : msg) {
            sendMessage(m);
        }
    }

    @Override
    public void sendMessage(Message m) {
        Log.trace("receive message $uid1 <- $uid2", "uid1", client.getUid(), "uid2", m.getUid());
        client.getMessageNode(m.getUid(), MessageEventListener.DEFAULT).sendMessageFromServerToClient(m.getData());
    }

    @Override
    public void sendServerDescriptor(ServerDescriptor v) {
        client.putServerDescriptor(v);
    }

    @Override
    public void sendCloud(UUIDAndCloud uidAndCloud) {
        client.setCloud(uidAndCloud.getUid(), uidAndCloud.getCloud());
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
            sendCloud(c);
        }
    }

    @Override
    public void newChildren(UUID[] uid) {
        for (var u : uid) {
            client.onNewChild.fire(u);
        }
    }


    @Override
    public void sendCloudConfigs(CloudConfig[] configs) {
        Log.debug("receive clouds");
        for (CloudConfig cc : configs) {
            sendCloudConfig(cc);
        }
    }


    @Override
    public void clientInteraction(
            UUID uid,
            ClientInteractionClientStream stream
    ) {
        stream
                .asIn()
                .keys(
                        ctx -> this,
                        "byClient",
                        uid
                )
                .accept();
    }



    private void sendCloudConfig(CloudConfig cc) {
        client.applyCloudConfig(cc);
    }

    @Override
    public void sendSignedUid(KeySignPublic[] keys, Sign[] signs) {
        client.peerIdentityManager().onSignedUid(signs);
    }


    @Override
    public void sendServers(ServerDescriptorWithGeo[] servers) {
        client.serversMFuture.set(servers);
    }



    // ===== Response handlers (new C-ABI-style) =====

    @Override
    public void sendPingAck() {
        connection.onPingAck();
        client.pingAckMFuture.set(Boolean.TRUE);
    }

    @Override
    public void sendAddToAccessGroupResult(UUID groupId, UUID uid, boolean ok) {
        client.addToAccessGroupResults.put(
                io.aether.utils.tuples.Tuple2.of(groupId, uid), ok);
    }

    @Override
    public void sendRemoveFromAccessGroupResult(UUID groupId, UUID uid, boolean ok) {
        client.removeFromAccessGroupResults.put(
                io.aether.utils.tuples.Tuple2.of(groupId, uid), ok);
    }

    @Override
    public void sendAccessGroupsOfClient(UUID uid, UUID[] groupIds) {
        Log.debug("Received AccessGroups of client $uid: $count", "uid", uid, "count", groupIds.length);
        client.clientGroups.put(uid, Set.of(groupIds));
    }

    @Override
    public void sendAccessGroup(AccessGroup group) {
        if (group != null) {
            client.accessGroups.put(group.getId(), group);
        }
    }

    @Override
    public void sendAccessedClients(UUID uid, UUID[] clients) {
        Log.debug("Received $count accessed clients for $uid", "count", clients.length, "uid", uid);
        client.allAccessedClients.put(uid, ObjectSet.of(clients));
    }

    @Override
    public void sendAccessCheckResult2(UUID uid1, UUID uid2, boolean ok) {
        client.accessCheckCache.put(new AccessCheckPair(uid1, uid2), ok);
    }

    @Override
    public void sendAccessCheckAck(UUID uid) {
        client.accessCheckAcks.put(uid, Boolean.TRUE);
    }

    @Override

    public void sendWebRtcSession(UUID uid, WebRtcSession session) {
        client.onWebRtcSessionReceived(uid, session);
    }


    @Override
    public void sendNewAccessGroupId(UUID owner, UUID groupId) {
        client.newAccessGroupIdMFuture.set(groupId);
    }

    @Override
    public void sendSelfDestructAck() {
        client.selfDestructAckMFuture.set(Boolean.TRUE);
    }

    @Override
    public void sendOpenReceiveWindowAck() {
        client.openReceiveWindowAckMFuture.set(Boolean.TRUE);
    }

    @Override
    public void sendAlias(UUID alias) {
        client.aliasMFuture.set(alias);
    }

    @Override
    public void sendSetAliasAck() {
        client.setAliasAckMFuture.set(Boolean.TRUE);
    }

    @Override
    public void sendBalance(long balance) {
        Log.trace("ClientApiSafeImpl: sendBalance (unused)", "balance", balance);
    }

    @Override
    public void sendSetParentAck() {
        client.setParentAckMFuture.set(Boolean.TRUE);
    }

    @Override
    public void sendBlockAck() {
        client.blockAckMFuture.set(Boolean.TRUE);
    }

    @Override
    public void sendParent(UUID uid) {
        Log.trace("ClientApiSafeImpl: sendParent (unused)", "uid", uid);
    }

    @Override
    public void sendBeneficiary(UUID uid) {
        Log.trace("ClientApiSafeImpl: sendBeneficiary (unused)", "uid", uid);
    }

    @Override
    public void sendSetBeneficiaryAck() {
        client.setBeneficiaryAckMFuture.set(Boolean.TRUE);
    }

    @Override
    public void sendBlockTime(java.util.Date time) {
        Log.trace("ClientApiSafeImpl: sendBlockTime (unused)", "time", time);
    }

    @Override
    public void sendUnblockAck() {
        client.unblockAckMFuture.set(Boolean.TRUE);
    }

    @Override
    public void sendCreateTime(java.util.Date time) {
        Log.trace("ClientApiSafeImpl: sendCreateTime (unused)", "time", time);
    }

    @Override
    public void sendOnlineTime(java.util.Date time) {
        Log.trace("ClientApiSafeImpl: sendOnlineTime (unused)", "time", time);
    }

    @Override
    public void sendAddAccessGroupResult(UUID groupId, boolean ok) {
        Log.trace("ClientApiSafeImpl: sendAddAccessGroupResult", "groupId", groupId, "ok", ok);
    }

    @Override
    public void sendRemoveAccessGroupResult(UUID groupId, boolean ok) {
        Log.trace("ClientApiSafeImpl: sendRemoveAccessGroupResult", "groupId", groupId, "ok", ok);
    }

    @Override
    public void sendSetMsgQueueLimitAck() {
        client.setMsgQueueLimitAckMFuture.set(Boolean.TRUE);
    }

    @Override
    public void sendSetMsgTimeLimitAck() {
        client.setMsgTimeLimitAckMFuture.set(Boolean.TRUE);
    }

    @Override
    public void sendAddServersToCloudAck() {
        client.addServersToCloudAckMFuture.set(Boolean.TRUE);
    }

    @Override
    public void sendRemoveServersFromCloudAck() {
        client.removeServersFromCloudAckMFuture.set(Boolean.TRUE);
    }

    @Override
    public void sendClientActivity(UUID uid, ClientActivity[] activities) {
        client.clientActivities.put(uid, activities);
    }

    @Override
    public void sendClientLogs(UUID uid, ClientLogEntry[] logs) {
        client.clientLogs.put(uid, logs);
    }

    @Override
    public void sendClientConnections(UUID uid, ClientConnectionInfo[] connections) {
        client.clientConnections.put(uid, connections);
    }

    @Override
    public void sendClientMessages(UUID uid, MessageInfo[] messages) {
        client.clientMessages.put(uid, messages);
    }

    @Override
    public void sendUap(UUID uid, Uap uap) {
        client.uaps.put(uid, uap);
    }

    @Override
    public void sendClientTiming(UUID uid, ClientTiming timing) {
        client.clientTimings.put(uid, timing);
    }

    @Override
    public void sendCloudPosition(CloudConfig config) {
        if (config != null) {
            client.cloudPositions.put(config.getSubjectUid(), config);
            client.applyCloudConfig(config);
        }
    }

}