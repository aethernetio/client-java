package io.aether.cloud.client;

import io.aether.common.Cloud;
import io.aether.common.ServerDescriptor;
import io.aether.crypt.Key;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientConfiguration {
    public final UUID parentUid;
    public final List<URI> cloudFactoryUrl;
    public final Map<Integer, ServerConfig> servers = new ConcurrentHashMap<>();
    public final Map<UUID, UidConfig> uidConfigs = new ConcurrentHashMap<>();
    public final int countServersForRegistration = 1;
    public final int timoutForConnectToRegistrationServer = 10;
    public long pingDuration = 100;
    public volatile UUID uid;
    public volatile UUID alias;
    public Key masterKey;

    public ClientConfiguration(UUID parentUid, List<URI> cloudFactoryUrl) {
        assert parentUid != null;
        this.parentUid = parentUid;
        this.cloudFactoryUrl = cloudFactoryUrl;
    }

    public void uid(UUID uid) {
        this.uid = uid;
    }

    public void alias(UUID alias) {
        this.alias = alias;
    }

    public void masterKey(Key key) {
        this.masterKey = key;
    }

    public ServerConfig getServerConfig(int sid) {
        return servers.computeIfAbsent(sid, ServerConfig::new);
    }

    public ServerDescriptor getServerDescriptor(int serverId) {
        assert serverId > 0;
        var ds = getServerConfig(serverId);
        return ds.descriptor;
    }

    public void saveCloud(UUID uid, Cloud cloud) {
        getUidConfig(uid).cloud = cloud;
    }

    public UidConfig getUidConfig(UUID uid) {
        assert uid != null;
        return uidConfigs.computeIfAbsent(uid, UidConfig::new);
    }

    public void setCloud(UUID uid, Cloud cloud) {
        var u = getUidConfig(uid);
        u.cloud = cloud;
        saveCloud(uid, cloud);
    }

    public Cloud getCloud(UUID uid) {
        assert uid != null;
        return getUidConfig(uid).cloud;
    }

    public static class ServerConfig {
        final int sid;
        ServerDescriptor descriptor;

        public ServerConfig(int sid) {
            this.sid = sid;
        }

        public ServerConfig(ServerDescriptor descriptor) {
            this(descriptor.id());
            this.descriptor = descriptor;
        }
    }

    public static class UidConfig {
        public final UUID uid;
        public Cloud cloud;

        public UidConfig(UUID uid) {
            this.uid = uid;
        }
    }
}
