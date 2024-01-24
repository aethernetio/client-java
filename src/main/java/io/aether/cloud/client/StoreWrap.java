package io.aether.cloud.client;

import io.aether.common.*;
import io.aether.sodium.ChaCha20Poly1305Pair;
import io.aether.sodium.Nonce;
import io.aether.utils.Store;
import io.aether.utils.streams.AStream;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static io.aether.utils.streams.AStream.streamOf;

public class StoreWrap {
	public final Store.PropertyLong pingDuration;
	public final Store.Property<UUID> uid;
	public final Store.Property<UUID> parentUid;
	public final Store.Property<Key> masterKey;
	public final Store.Property<Signer> globalSigner;
	public final Store.Property<List<URI>> cloudFactoryUrl;
	public final Store.PropertyInt countServersForRegistration;
	public final Store.PropertyInt timoutForConnectToRegistrationServer;
	private final Store store;
	public StoreWrap(Store store) {
		this(store, null);
	}
	public StoreWrap(Store store, UUID parent) {
		this.store = store;
		pingDuration = store.getPropertyLong("client.ping.duration");
		countServersForRegistration = store.getPropertyInt("client.countServersForRegistration");
		timoutForConnectToRegistrationServer = store.getPropertyInt("client.timoutForConnectToRegistrationServer");
		uid = store.getProperty("client.uid", UUID::fromString);
		parentUid = store.getProperty("client.parentUid", UUID::fromString);
		if (parent != null) parentUid.set(parent);
		masterKey = store.getProperty("client.masterKey", Key::of);
		globalSigner = store.getProperty("client.globalSigner", Signer::of);
		cloudFactoryUrl = store.getProperty("client.url.cloud", s -> AStream.streamOf(s.split(";")).map(URI::create).toList(), d -> streamOf(d).join(";"));
	}
	public void setNonceForServerDescriptor(int sid, Nonce nonce) {
		var prefix = "client.servers." + sid + ".";
		store.set(prefix + "nonce", nonce);
	}
	public void setServerDescriptor(ServerDescriptorOnClient serverDescriptor) {
		var prefix = "client.servers." + serverDescriptor.getId() + ".";
		store.set(prefix + "descriptor", serverDescriptor.getServerDescriptor());
		store.set(prefix + "nonce", serverDescriptor.getDataPreparerConfig().chaCha20Poly1305Pair.getNonceLocal());
	}
	public ServerDescriptorOnClient getServerDescriptor(int serverId, Key masterKey) {
		assert serverId > 0;
		var prefix = "client.servers." + serverId + ".";
		var res = new ServerDescriptorOnClient(store.get(prefix + "descriptor", ServerDescriptor::of), masterKey);
		var nonce = store.get(prefix + "nonce", null, Nonce::of);
		res.getDataPreparerConfig().chaCha20Poly1305Pair = ChaCha20Poly1305Pair.forClient(masterKey, serverId, nonce);
		return res;
	}
	public void setDefaultPortForCodec(AetherCodec codec, int port) {
		store.set("client.protocol." + codec.getName() + ".defaultPort", port);
	}
	public int getDefaultPortForCodec(AetherCodec codec) {
		return store.get("client.protocol." + codec.getName() + ".defaultPort", codec.getNetworkConfigurator().getDefaultPort());
	}
	public void setCloud(UUID uid, Cloud cloud) {
		if (cloud == null) {
			store.delete("client.clouds." + uid);
		} else {
			store.set("client.clouds." + uid, streamOf(cloud.data()).join(","));
		}
	}
	public int[] getCloud(UUID uid) {
		return store.get("client.clouds." + uid, s -> {
			if (s == null) return null;
			return streamOf(s.split(",")).mapToInt(Integer::parseInt).toArray();
		});
	}
}
