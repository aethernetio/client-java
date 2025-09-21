package io.aether.cloud.client;

import io.aether.api.common.AccessGroup;
import io.aether.common.AccessGroupI;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.futures.ARFuture;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

public abstract class AccessGroupImpl implements AccessGroupI {
    protected final long id;
    protected final Set<UUID> data;
    protected final UUID owner;

    public AccessGroupImpl(AccessGroup accessGroup) {
        this.id = accessGroup.getId();
        this.data = new ConcurrentHashSet<>(Arrays.asList(accessGroup.getData()));
        this.owner = accessGroup.getOwner();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public ARFuture<Boolean> contains(UUID uid) {
        return ARFuture.completed(data.contains(uid));
    }

}
