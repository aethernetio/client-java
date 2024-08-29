package io.aether.cloud.client;

import io.aether.api.SecurityConfig;
import io.aether.common.AetherCodec;
import io.aether.common.CryptoLib;
import io.aether.common.Key;
import io.aether.common.ServerDescriptor;

import java.net.InetSocketAddress;
import java.net.URI;

public class ServerDescriptorOnClient {
	SecurityConfig securityConfig;
	private ServerDescriptor serverDescriptor;
	public ServerDescriptorOnClient(ServerDescriptor serverDescriptor, Key masterKey) {
		this.securityConfig = new SecurityConfig();
		this.serverDescriptor = serverDescriptor;
		CryptoLib cryptoLib=masterKey.getType().cryptoLib();
		securityConfig.symmetric = cryptoLib.env.symmetricForClient(masterKey, serverDescriptor.id());
		securityConfig.asymmetric = serverDescriptor.keys().makeProviderAsym(cryptoLib);
	}
	public static ServerDescriptorOnClient of(ServerDescriptor sd, Key masterKey) {
		return new ServerDescriptorOnClient(sd, masterKey);
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
	public SecurityConfig getSecurityConfig() {
		if (securityConfig == null) {
			securityConfig = new SecurityConfig();
		}
		return securityConfig;
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
