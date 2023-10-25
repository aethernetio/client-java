package io.aether.cloud.client;

import io.aether.common.AetherCodec;
import io.aether.common.Key;
import io.aether.common.ServerDescriptor;
import io.aether.common.SignedKey;
import io.aether.sodium.ChaCha20Poly1305Pair;
import io.aether.sodium.Nonce;

import java.net.InetSocketAddress;
import java.net.URI;

public class ServerDescriptorOnClient {
	final ServerDescriptor serverDescriptor;
	ChaCha20Poly1305Pair chaCha20Poly1305Pair;
	public ServerDescriptorOnClient(ServerDescriptor serverDescriptor) {
		this.serverDescriptor = serverDescriptor;
	}
	public static ServerDescriptorOnClient of(ServerDescriptor sd) {
		return new ServerDescriptorOnClient(sd);
	}
	public int getId() {
		return serverDescriptor.id();
	}
	public SignedKey getServerAsymPublicKey() {
		return serverDescriptor.publicKey();
	}
	public void initClientKeyAndNonce(Key masterKey, Nonce nonce) {
		chaCha20Poly1305Pair = ChaCha20Poly1305Pair.forClient(masterKey, getId(), nonce);
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
