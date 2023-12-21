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
	public ServerDescriptor getServerDescriptor() {
		return serverDescriptor;
	}
	public static ServerDescriptorOnClient of(ServerDescriptor sd, Key masterKey) {
		var r = of(sd);
		r.initChaChaKeys(masterKey);
		return r;
	}
	public ServerDescriptorOnClient(ServerDescriptor serverDescriptor) {
		this.serverDescriptor = serverDescriptor;
	}
	public void setServerDescriptor(ServerDescriptor serverDescriptor, Key masterKey) {
		this.serverDescriptor = serverDescriptor;
		initChaChaKeys(masterKey);
	}
	public static ServerDescriptorOnClient of(ServerDescriptor sd) {
		return new ServerDescriptorOnClient(sd);
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
	public void setAsymKey(SignedKey key) {
		var c = getDataPreparerConfig();
		c.asymCrypt = new AsymCrypt(key.key());
	}
	public void setSignKey(SignedKey key) {
		var c = getDataPreparerConfig();
		c.signer = new SignChecker(key.key());
	}
	public void initChaChaKeys(Key masterKey) {
		var c = getDataPreparerConfig();
		c.chaCha20Poly1305Pair = ChaCha20Poly1305Pair.forClient(masterKey, serverDescriptor.id(), Nonce.of());
	}
	public SignedKey getServerAsymPublicKey() {
		return serverDescriptor.publicKey();
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
