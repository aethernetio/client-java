package io.aether.cloud.client;

import io.aether.common.AetherCodec;
import io.aether.common.CoderAndPort;
import io.aether.common.IPAddress;
import io.aether.common.Key;
import io.aether.sodium.ChaCha20Poly1305;

import java.net.InetSocketAddress;
import java.util.List;

public class ServerDescriptorOnClient {
	public int getId() {
		return id;
	}
	public List<IPAddress> getIpAddress() {
		return ipAddress;
	}
	public List<CoderAndPort> getCodersAndPorts() {
		return codersAndPorts;
	}
	public Key getClientKey() {
		return clientKey;
	}
	public Key getServerKey() {
		return serverKey;
	}
	public Key getServerAsymPublicKey() {
		return serverAsymPublicKey;
	}
	public ChaCha20Poly1305.Nonce getNonce() {
		return nonce;
	}
	public ChaCha20Poly1305.KeyAndNonce getKeyAndNonce() {
		return keyAndNonce;
	}
	int id = 0;
	List<IPAddress> ipAddress;
	List<CoderAndPort> codersAndPorts;
	Key clientKey;
	Key serverKey;
	Key serverAsymPublicKey;
	ChaCha20Poly1305.Nonce nonce;
	ChaCha20Poly1305.KeyAndNonce keyAndNonce;
	public ServerDescriptorOnClient() {
	}
	public ServerDescriptorOnClient(int serverId, List<IPAddress> ipAddress, List<CoderAndPort> codersAndPorts) {
		this.id = serverId;
		this.ipAddress = ipAddress;
		this.codersAndPorts = codersAndPorts;
	}
	public void initClientKeyAndNonce(Key masterKey) {
		clientKey = ChaCha20Poly1305.generateSyncClientKeyByMasterKey(masterKey, id);
		serverKey = ChaCha20Poly1305.generateSyncServerKeyByMasterKey(masterKey, id);
		nonce = ChaCha20Poly1305.Nonce.of();
		keyAndNonce = new ChaCha20Poly1305.KeyAndNonce(clientKey, nonce);
	}
	public int getPortByCodec(AetherCodec codec) {
		for (var p : codersAndPorts) {
			if (p.codec() == codec) return p.port();
		}
		return 0;
	}
	public InetSocketAddress getInetSocketAddress(AetherCodec codec) {
		return ipAddress.get(0).toInetSocketAddress(getPortByCodec(codec));
	}
}
