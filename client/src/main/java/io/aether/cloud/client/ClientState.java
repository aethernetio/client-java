package io.aether.cloud.client;

import io.aether.api.common.*;
import io.aether.crypto.SignChecker;
import io.aether.utils.slots.AMFuture;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ClientState {

    UUID getUid();

    void setUid(UUID uid);

    UUID getAlias();

    void setAlias(UUID alias);

    void setMasterKey(Key key);

    ServerInfo getServerInfo(int sid);
    Iterable<ServerInfo> getServerInfoAll();

    default ServerDescriptor getServerDescriptor(int serverId) {
        return getServerInfo(serverId).getDescriptor();
    }

    ClientInfo getClientInfo(UUID uid);

    default void setCloud(UUID uid, Cloud cloud) {
        getClientInfo(uid).setCloud(cloud);
    }

    Cloud getCloud(UUID uid);
    Iterable<ClientInfo> getClientInfoAll();

    List<URI> getRegistrationUri();

    long getTimeoutForConnectToRegistrationServer();

    int getCountServersForRegistration();

    AMFuture<Long> getPingDuration();

    UUID getParentUid();

    void setParentUid(UUID uid);

    io.aether.api.common.Key getMasterKey();

    CryptoLib getCryptoLib();

    Set<SignChecker> getRootSigners();

    interface ServerInfo {
        int getServerId();

        ServerDescriptor getDescriptor();

        void setDescriptor(ServerDescriptor serverDescriptor);
    }

    interface ClientInfo {
        UUID getUid();

        Cloud getCloud();

        void setCloud(Cloud cloud);
    }
}
