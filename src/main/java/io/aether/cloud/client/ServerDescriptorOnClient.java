package io.aether.cloud.client;

import io.aether.common.*;

import java.net.InetSocketAddress;
import java.net.URI;

public class ServerDescriptorOnClient {
    private ServerDescriptorLite serverDescriptor;
    private CryptoProvider symmetricProvider;

    public CryptoProvider getSymmetricProvider() {
        return symmetricProvider;
    }

    public ServerDescriptorOnClient(ServerDescriptorLite serverDescriptor, Key masterKey) {
        this.serverDescriptor = serverDescriptor;
        initChaChaKeys(masterKey);
    }

    public static ServerDescriptorOnClient of(ServerDescriptorLite sd, Key masterKey) {
        return new ServerDescriptorOnClient(sd, masterKey);
    }

    public ServerDescriptorLite getServerDescriptor() {
        return serverDescriptor;
    }

    public void setServerDescriptor(ServerDescriptorLite serverDescriptor, Key masterKey) {
        this.serverDescriptor = serverDescriptor;
        initChaChaKeys(masterKey);
    }

    public int getId() {
        return serverDescriptor.id();
    }

    public void initChaChaKeys(Key masterKey) {
        this.symmetricProvider = masterKey.getType().cryptoLib().env.symmetricForClient(masterKey, serverDescriptor.id());
    }

    public int getPort(AetherCodec codec) {
        return serverDescriptor.ipAddress().getPort(codec);
    }

    public InetSocketAddress getInetSocketAddress(AetherCodec codec) {
        return serverDescriptor.ipAddress().getDefaultInetSocketAddress(codec);
    }

    public URI getURI(AetherCodec codec) {
        return serverDescriptor.ipAddress().getURI(codec);
    }
}
