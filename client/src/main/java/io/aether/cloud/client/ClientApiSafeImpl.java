package io.aether.cloud.client;

import io.aether.api.clientserverapi.ClientApiSafe;
import io.aether.api.clientserverapi.Message;
import io.aether.api.common.*;
import io.aether.logger.Log;
import io.aether.utils.futures.ARFuture;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements the ClientApiSafe interface to handle responses from the server.
 */
class ClientApiSafeImpl implements ClientApiSafe {

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
    public void sendAccessGroupForClient(UUID uid, long[] groups) {
        Log.debug("Received AccessGroups for client $uid", "uid", uid);
        client.clientGroups.put(uid, LongSet.of(groups));
    }

    @Override
    public void addItemsToAccessGroup(long id, UUID[] groups) {
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
                AccessGroup newGroup = new AccessGroup(group.getOwner(), group.getId(), newUuids.stream().distinct().toArray(UUID[]::new));
                client.accessGroups.put(id, newGroup);
            }
        });
    }

    @Override
    public void removeItemsFromAccessGroup(long id, UUID[] groups) {
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
                AccessGroup newGroup = new AccessGroup(group.getOwner(), group.getId(), newUuids.toArray(new UUID[0]));
                client.accessGroups.put(id, newGroup);
            }
        });
    }

    @Override
    public void addAccessGroupsToClient(UUID uid, long[] groups) {
        Log.debug("Server pushed ADD groups to client $uid", "uid", uid);
        client.clientGroups.getFuture(uid).to(existingGroups -> {
            var newGroups = (existingGroups == null) ? LongSet.of() : new LongArraySet(existingGroups);
            for (long g : groups) newGroups.add(g);
        });
    }

    @Override
    public void removeAccessGroupsFromClient(UUID uid, long[] groups) {
        Log.debug("Server pushed REMOVE groups from client $uid", "uid", uid);
        client.clientGroups.getFuture(uid).to(existingGroups -> {
            if (existingGroups != null) {
                var newGroups = new LongArraySet(existingGroups);
                for (long g : groups) newGroups.remove(g);
            }
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

    @Override
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
        for (CloudConfig cc : configs) {
            client.clouds.get(cc.getSubjectUid(), new io.aether.utils.interfaces.Future<>() {
                @Override
                public void onResolved(ClientCloud clientCloud) {
                    clientCloud.applyCloudConfig(cc, client.pendingAppliedConfigs);
                }

                @Override
                public void onError(int time, Exception error) {
                    client.clouds.put(cc.getSubjectUid(), new ClientCloud(cc.getSubjectUid(), cc.getCloud()));
                }
            });
        }
    }


}