
package io.aether.cloud.client;

import io.aether.api.common.Cloud;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CloudPriorityManager {
    private final Map<UUID, ClientCloud> clouds = new ConcurrentHashMap<>();

    public void updateCloudFromWork(UUID uid, Cloud cloud) {
        clouds.computeIfAbsent(uid, k -> new ClientCloud(k, cloud)).smartMerge(cloud);
    }

    public void promote(UUID uid, short sid) {
        ClientCloud cc = clouds.get(uid);
        if (cc != null) cc.promote(sid);
    }

    public void demote(UUID uid, short sid) {
        ClientCloud cc = clouds.get(uid);
        if (cc != null) cc.demote(sid);
    }

    public short[] getOrderedSids(UUID uid, Cloud raw) {
        return clouds.computeIfAbsent(uid, k -> new ClientCloud(k, raw)).getOrderedSids();
    }
}
