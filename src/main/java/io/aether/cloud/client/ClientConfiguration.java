package io.aether.cloud.client;

import io.aether.common.Cloud;
import io.aether.common.Key;
import io.aether.common.ServerDescriptorLite;
import io.aether.sodium.Nonce;

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
	public volatile long pingDuration = 1000;
	public volatile UUID uid;
	public Key masterKey;
	public volatile int countServersForRegistration = 1;
	public volatile int timoutForConnectToRegistrationServer = 10;
	public ClientConfiguration(UUID parentUid, List<URI> cloudFactoryUrl) {
		this.parentUid = parentUid;
		this.cloudFactoryUrl = cloudFactoryUrl;
	}
	public void uid(UUID uid) {
		this.uid = uid;
	}
	public void masterKey(Key key) {
		this.masterKey = key;
	}
	public ServerConfig getServerConfig(int sid) {
		return servers.computeIfAbsent(sid, ServerConfig::new);
	}
	public ServerDescriptorLite getServerDescriptor(int serverId) {
		assert serverId > 0;
		var ds = getServerConfig(serverId);
		return ds.descriptor;
	}
	public void saveCloud(UUID uid, Cloud cloud) {
		getUidConfig(uid).cloud = cloud;
	}
	public UidConfig getUidConfig(UUID uid) {
		return uidConfigs.computeIfAbsent(uid, UidConfig::new);
	}
	public void setCloud(UUID uid, Cloud cloud) {
		var u = getUidConfig(uid);
		u.cloud = cloud;
		saveCloud(uid, cloud);
	}
	public Cloud getCloud(UUID uid) {
		return getUidConfig(uid).cloud;
	}
	public static class ServerConfig {
		int sid;
		Nonce nonce;
		ServerDescriptorLite descriptor;
		public ServerConfig(int sid) {
			this.sid = sid;
		}
		public ServerConfig(ServerDescriptorLite descriptor) {
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
