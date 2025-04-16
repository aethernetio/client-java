package io.aether.cloud.client;

import io.aether.common.AccessGroup;
import io.aether.common.AccessGroupI;

import java.util.UUID;

public abstract class AccessGroupImpl implements AccessGroupI {
    protected final AccessGroup accessGroup;

    public AccessGroupImpl(AccessGroup accessGroup) {
        this.accessGroup = accessGroup;
    }

    @Override
    public long getId() {
        return accessGroup.id();
    }

    @Override
    public UUID getOwner() {
        return accessGroup.owner;
    }

    @Override
    public boolean contains(UUID uid) {
        return accessGroup.contains(uid);
    }

}
