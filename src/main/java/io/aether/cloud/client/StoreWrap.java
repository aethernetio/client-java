package io.aether.cloud.client;

import io.aether.common.AetherCodec;
import io.aether.common.CoderAndPort;
import io.aether.common.IPAddress;
import io.aether.common.Key;
import io.aether.sodium.ChaCha20Poly1305;
import io.aether.utils.Store;

import java.util.List;
import java.util.UUID;

import static io.aether.utils.streams.AStream.streamOf;

public class StoreWrap {
	public final Store.PropertyLong pingDuration;
	public final Store.Property<UUID> uid;
	public final Store.Property<UUID> parentUid;
	public final Store.Property<Key> masterKey;
	public final Store.Property<String> cloudFactoryUrl;
	public final Store.PropertyInt countServersForRegistration;
	private final Store store;
	public StoreWrap(Store store) {
		this(store, null);
	}
	public StoreWrap(Store store, UUID parent) {
		this.store = store;
		pingDuration = store.getPropertyLong("settings.ping.duration");
		countServersForRegistration = store.getPropertyInt("settings.countServersForRegistration");
		uid = store.getProperty("main.uid", UUID::fromString);
		parentUid = store.getProperty("main.parentUid", UUID::fromString);
		if (parent != null) parentUid.set(parent);
		masterKey = store.getProperty("main.masterKey", Key::of);
		cloudFactoryUrl = store.getProperty("main.url.cloud");
	}
	public void setServerDescriptor(ServerDescriptorOnClient serverDescriptor) {
		var prefix = "main.servers." + serverDescriptor.getId() + ".";
		store.set(prefix + "clientKey", serverDescriptor.clientKey.toHexString());
		store.set(prefix + "clientNonce", serverDescriptor.nonce.toHexString());
		store.set(prefix + "serverKey", serverDescriptor.serverKey.toHexString());
		store.set(prefix + "serverAsymPublicKey", serverDescriptor.serverAsymPublicKey.toHexString());
		store.set(prefix + "ipAddresses", streamOf(serverDescriptor.ipAddress).joinD());
		store.set(prefix + "codersAndPorts", streamOf(serverDescriptor.codersAndPorts).joinD());
	}
	public ServerDescriptorOnClient getServerDescriptor(int serverId) {
		var prefix = "main.servers." + serverId + ".";
		var res = new ServerDescriptorOnClient();
		res.id = serverId;
		res.clientKey = store.get(prefix + "clientKey", null, Key::of);
		res.serverKey = store.get(prefix + "serverKey", null, Key::of);
		res.serverAsymPublicKey = store.get(prefix + "serverAsymPublicKey", null, Key::of);
		res.nonce = store.get(prefix + "clientNonce", null, ChaCha20Poly1305.Nonce::of);
		res.keyAndNonce = new ChaCha20Poly1305.KeyAndNonce(res.clientKey, res.nonce);
		var ipAddresses = streamOf(store.get(prefix + "ipAddresses", "").split(",")).map(IPAddress::of).toList();
		List<CoderAndPort> codersAndPorts = streamOf(store.get(prefix + "codersAndPorts", "").split(",")).map(e -> {
			var ee = e.split(":");
			return new CoderAndPort(AetherCodec.valueOf(ee[0]), Integer.parseInt(ee[1]));
		}).toList();
		res.ipAddress = ipAddresses;
		res.codersAndPorts = codersAndPorts;
		return res;
	}
	public void setDefaultPortForCodec(AetherCodec codec, int port) {
		store.set("main.protocol." + codec.getName() + ".defaultPort", port);
	}
	public int getDefaultPortForCodec(AetherCodec codec) {
		return store.get("main.protocol." + codec.getName() + ".defaultPort", codec.getNetworkConfigurator().getDefaultPort());
	}
	public void setCloud(UUID uid, int[] cloud) {
		if (cloud == null) {
			store.delete("main.clouds." + uid);
		} else {
			store.set("main.clouds." + uid, streamOf(cloud).join(","));
		}
	}
	public int[] getCloud(UUID uid) {
		return store.get("main.clouds." + uid, s -> {
			if (s == null) return null;
			return streamOf(s.split(",")).mapToInt(Integer::parseInt).toArray();
		});
	}
}
