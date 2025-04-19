package io.aether.cloud.client;

import io.aether.common.Cloud;
import io.aether.common.ServerDescriptor;
import io.aether.crypt.CryptoLib;
import io.aether.crypt.Key;
import io.aether.crypt.SignChecker;
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

    default ServerDescriptor getServerDescriptor(int serverId) {
        return getServerInfo(serverId).getDescriptor();
    }

    ClientInfo getClientInfo(UUID uid);

    default void setCloud(UUID uid, Cloud cloud) {
        getClientInfo(uid).setCloud(cloud);
    }

    Cloud getCloud(UUID uid);

    List<URI> getRegistrationUri();

    long getTimeoutForConnectToRegistrationServer();

    int getCountServersForRegistration();

    AMFuture<Long> getPingDuration();

    UUID getParentUid();

    void setParentUid(UUID uid);

    Key getMasterKey();

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
