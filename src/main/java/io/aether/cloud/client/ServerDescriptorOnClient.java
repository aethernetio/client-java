package io.aether.cloud.client;

import io.aether.api.EncryptionApiConfig;
import io.aether.common.*;

import java.net.InetSocketAddress;
import java.net.URI;

public class ServerDescriptorOnClient {
	EncryptionApiConfig encryptionApiConfig;
	private ServerDescriptor serverDescriptor;
	public ServerDescriptorOnClient(ServerDescriptor serverDescriptor, Key masterKey) {
		this.encryptionApiConfig = new EncryptionApiConfig();
		this.serverDescriptor = serverDescriptor;
		CryptoLib cryptoLib=masterKey.getType().cryptoLib();
		encryptionApiConfig.symmetric = cryptoLib.env.symmetricForClient(masterKey, serverDescriptor.id());
	}
	public static ServerDescriptorOnClient of(ServerDescriptor sd, Key masterKey) {
		return new ServerDescriptorOnClient(sd, masterKey);
	}

	public static ServerDescriptorOnClient of(ServerDescriptorLite sd, Key masterKey) {
		return new ServerDescriptorOnClient(new ServerDescriptor(sd.id(),sd.ipAddress(),new KeysBase()),masterKey);
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
	public EncryptionApiConfig getSecurityConfig() {
		if (encryptionApiConfig == null) {
			encryptionApiConfig = new EncryptionApiConfig();
		}
		return encryptionApiConfig;
	}
	public void initChaChaKeys(Key masterKey) {
		var c = getSecurityConfig();
		c.symmetric = masterKey.getType().cryptoLib().env.symmetricForClient(masterKey, serverDescriptor.id());
	}
	public int getPort(AetherCodec codec) {
		return serverDescriptor.ipAddress().getPort(codec);
	}
	public InetSocketAddress getInetSocketAddress(AetherCodec codec) {
		return serverDescriptor.getInetSocketAddress(codec);
	}
	public URI getURI(AetherCodec codec) {
		return serverDescriptor.getURI(codec);
	}
}
