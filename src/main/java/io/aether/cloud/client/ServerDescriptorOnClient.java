package io.aether.cloud.client;

import io.aether.api.DataPreparerConfig;
import io.aether.common.*;
import io.aether.sodium.AsymCrypt;
import io.aether.sodium.ChaCha20Poly1305Pair;
import io.aether.sodium.Nonce;

import java.net.InetSocketAddress;
import java.net.URI;

public class ServerDescriptorOnClient {
	DataPreparerConfig dataPreparerConfig;
	private ServerDescriptor serverDescriptor;
	public ServerDescriptorOnClient(ServerDescriptor serverDescriptor, Key masterKey) {
		this.dataPreparerConfig = new DataPreparerConfig();
		this.serverDescriptor = serverDescriptor;
		dataPreparerConfig.chaCha20Poly1305Pair = ChaCha20Poly1305Pair.forClient(masterKey, serverDescriptor.id(), Nonce.of());
		dataPreparerConfig.asymCrypt = new AsymCrypt(serverDescriptor.getKey(KeyType.CURVE25519));
	}
	public static ServerDescriptorOnClient of(ServerDescriptor sd, Key masterKey) {
		return new ServerDescriptorOnClient(sd, masterKey);
	}
	public static ServerDescriptorOnClient of(ServerDescriptorLite sd, Key masterKey, SignType signType) {
		return new ServerDescriptorOnClient(sd.toFull(signType), masterKey);
	}
	public ServerDescriptor getServerDescriptor() {
		return serverDescriptor;
	}
	public void setServerDescriptor(ServerDescriptor serverDescriptor, Key masterKey) {
		this.serverDescriptor = serverDescriptor;
		initChaChaKeys(masterKey);
	}
	public int getId() {
		return serverDescriptor.id();
	}
	public DataPreparerConfig getDataPreparerConfig() {
		if (dataPreparerConfig == null) {
			dataPreparerConfig = new DataPreparerConfig();
		}
		return dataPreparerConfig;
	}
	public void initChaChaKeys(Key masterKey) {
		var c = getDataPreparerConfig();
		c.chaCha20Poly1305Pair = ChaCha20Poly1305Pair.forClient(masterKey, serverDescriptor.id(), Nonce.of());
	}
	public int getPort(AetherCodec codec) {
		return serverDescriptor.getPort(codec);
	}
	public InetSocketAddress getInetSocketAddress(AetherCodec codec) {
		return serverDescriptor.getInetSocketAddress(codec);
	}
	public URI getURI(AetherCodec codec) {
		return serverDescriptor.getURI(codec);
	}
}
