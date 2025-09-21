package io.aether.cloud.client;

import io.aether.api.clienttypes.ClientStateForSave;
import io.aether.api.common.Cloud;
import io.aether.api.common.CryptoLib;
import io.aether.api.common.Key;
import io.aether.api.common.ServerDescriptor;
import io.aether.crypto.SignChecker;
import io.aether.logger.Log;
import io.aether.net.fastMeta.FastFutureContext;
import io.aether.utils.AString;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.RU;
import io.aether.utils.ToString;
import io.aether.utils.dataio.DataInOut;
import io.aether.utils.dataio.DataInOutStatic;
import io.aether.utils.slots.AMFuture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.aether.utils.flow.Flow.flow;

public class ClientStateInMemory implements ClientState, ToString {
    private final List<URI> registrationUri = new CopyOnWriteArrayList<>();
    private final Map<Integer, ServerInfo> servers = new ConcurrentHashMap<>();
    private final Map<UUID, ClientInfoMutable> clients = new ConcurrentHashMap<>();
    private final Set<SignChecker> rootSigners = new ConcurrentHashSet<>();
    private final CryptoLib cryptoLib;
    private final AMFuture<Long> pingDuration = new AMFuture<>(1000L);
    private UUID parentUid;
    private int countServersForRegistration = 1;
    private int timeoutForConnectToRegistrationServer = 10;
    private volatile UUID uid;
    private volatile UUID alias;
    private volatile Key masterKey;

    public ClientStateInMemory(UUID parentUid, List<URI> registrationUri, Set<SignChecker> rootSigners) {
        this(parentUid, registrationUri, rootSigners, CryptoLib.HYDROGEN);
    }

    @Override
    public String toString() {
        return toString2();
    }

    @Override
    public void toString(AString sb) {
        sb.add("Client State:\n");
        sb.add("uid: ").add(uid).add("\n");
        sb.add("alias: ").add(alias).add("\n");
        sb.add("parent: ").add(parentUid).add("\n");
        sb.add("master key: ").add(masterKey).add("\n");
        sb.add("crypto lib: ").add(cryptoLib).add("\n");
        sb.add("cloud: ").add(getCloud(uid)).add("\n");
        for (var c : getCloud(uid).getData()) {
            sb.addSpace(4).add(getServerDescriptor(c)).add("\n");
        }
    }

    public ClientStateInMemory(UUID parentUid, List<URI> registrationUri, Set<SignChecker> rootSigners, CryptoLib cryptoLib) {
        assert parentUid != null;
        this.cryptoLib = cryptoLib;
        this.parentUid = parentUid;
        this.registrationUri.addAll(registrationUri);
        if (rootSigners != null) this.rootSigners.addAll(rootSigners);
        this.rootSigners.addAll(Set.of(
                SignChecker.of("SODIUM:4F202A94AB729FE9B381613AE77A8A7D89EDAB9299C3320D1A0B994BA710CCEB"),
                SignChecker.of("HYDROGEN:883B4D7E0FB04A38CA12B3A451B00942048858263EE6E6D61150F2EF15F40343")
        ));
    }

    public ClientStateInMemory(UUID parentUid, List<URI> registrationUri) {
        this(parentUid, registrationUri, null);
    }

    private ClientStateInMemory(ClientStateForSave dto) {
        this.uid = dto.getUid();
        this.alias = dto.getAlias();
        flow(dto.getClients()).map(ClientInfoMutable::new).toMapExtractKey(clients, ClientInfoMutable::getUid);
        flow(dto.getServers()).map(ServerInfo::new).toMapExtractKey(servers, ServerInfo::getServerId);
        this.parentUid = dto.getParentUid();
        this.masterKey = dto.getMasterKey();
        this.cryptoLib = dto.getCryptoLib();
        this.rootSigners.addAll(flow(dto.getRootSigners()).map(SignChecker::of).toSet());
        this.countServersForRegistration = dto.getCountServersForRegistration();
        this.timeoutForConnectToRegistrationServer = dto.getTimeoutForConnectToRegistrationServer();
        this.registrationUri.addAll(Arrays.asList(dto.getRegistrationUri()));
        this.pingDuration.set(dto.getPingDuration());
    }

    @Override
    public UUID getUid() {
        return uid;
    }

    @Override
    public void setUid(UUID uid) {
        this.uid = uid;
    }

    @Override
    public UUID getAlias() {
        return alias;
    }

    @Override
    public void setAlias(UUID alias) {
        this.alias = alias;
    }

    @Override
    public ClientState.ClientInfo getClientInfo(UUID uid) {
        return clients.computeIfAbsent(uid, ClientInfoMutable::new);
    }

    @Override
    public List<URI> getRegistrationUri() {
        return registrationUri;
    }

    @Override
    public long getTimeoutForConnectToRegistrationServer() {
        return timeoutForConnectToRegistrationServer;
    }

    public void setTimeoutForConnectToRegistrationServer(int timeoutForConnectToRegistrationServer) {
        this.timeoutForConnectToRegistrationServer = timeoutForConnectToRegistrationServer;
    }

    @Override
    public int getCountServersForRegistration() {
        return countServersForRegistration;
    }

    public void setCountServersForRegistration(int countServersForRegistration) {
        this.countServersForRegistration = countServersForRegistration;
    }

    @Override
    public AMFuture<Long> getPingDuration() {
        return pingDuration;
    }

    @Override
    public UUID getParentUid() {
        return parentUid;
    }

    @Override
    public void setParentUid(UUID uid) {
        this.parentUid = uid;
    }

    @Override
    public Key getMasterKey() {
        return masterKey;
    }

    @Override
    public void setMasterKey(Key key) {
        this.masterKey = key;
    }

    @Override
    public CryptoLib getCryptoLib() {
        return cryptoLib;
    }

    @Override
    public Set<SignChecker> getRootSigners() {
        return rootSigners;
    }

    public ServerInfo getServerInfo(int sid) {
        return servers.computeIfAbsent(sid, ServerInfo::new);
    }

    public ServerDescriptor getServerDescriptor(int serverId) {
        assert serverId > 0;
        var ds = getServerInfo(serverId);
        return ds.descriptor;
    }

    public void saveCloud(UUID uid, Cloud cloud) {
        getUidInfo(uid).cloud = cloud;
    }

    public ClientInfoMutable getUidInfo(UUID uid) {
        assert uid != null;
        return clients.computeIfAbsent(uid, ClientInfoMutable::new);
    }

    public void setCloud(UUID uid, Cloud cloud) {
        var u = getUidInfo(uid);
        u.cloud = cloud;
        saveCloud(uid, cloud);
    }

    public Cloud getCloud(UUID uid) {
        assert uid != null;
        return getUidInfo(uid).cloud;
    }

    public byte[] save() {
        DataInOut d = new DataInOut();
        ClientStateForSave.META.serialize(FastFutureContext.STUB, toDTO(), d);
        return d.toArray();
    }

    public void save(File file) {
        try (var out = new FileOutputStream(file)) {
            out.write(save());
        } catch (IOException e) {
            Log.error("Cannot save a store", e);
            throw new RuntimeException(e);
        }
    }

    private ClientStateForSave toDTO() {
        return new ClientStateForSave(
                flow(registrationUri).toArray(URI.class),
                flow(servers.values()).map(s -> s.descriptor).filterNotNull().toArray(ServerDescriptor.class),
                flow(clients.values())
                        .filter(s -> s.getCloud() != null)
                        .map(s -> new io.aether.api.clienttypes.ClientInfo(s.getUid(), s.getCloud()))
                        .toArray(io.aether.api.clienttypes.ClientInfo.class),
                flow(rootSigners).join(", "),
                cryptoLib,
                pingDuration.getNow(),
                parentUid,
                countServersForRegistration,
                timeoutForConnectToRegistrationServer,
                uid,
                alias,
                masterKey
        );
    }

    public static ClientStateInMemory load(File file) {
        try (var in = new FileInputStream(file)) {
            return load(in.readAllBytes());
        } catch (IOException e) {
            Log.error("Cannot load state", e);
            return RU.error(e);
        }
    }

    public static ClientStateInMemory load(byte[] data) {
        try {
            var dto = ClientStateForSave.META.deserialize(FastFutureContext.STUB, new DataInOutStatic(data));
            return new ClientStateInMemory(dto);
        } catch (Exception e) {
            throw new IllegalStateException("Unparsable format state");
        }
    }

    public static class ServerInfo implements ClientState.ServerInfo {
        final int sid;
        volatile ServerDescriptor descriptor;

        public ServerInfo(int sid) {
            this.sid = sid;
        }

        public ServerInfo(ServerDescriptor descriptor) {
            this(descriptor.getId());
            this.descriptor = descriptor;
        }

        @Override
        public int getServerId() {
            return sid;
        }

        @Override
        public ServerDescriptor getDescriptor() {
            return descriptor;
        }

        @Override
        public void setDescriptor(ServerDescriptor serverDescriptor) {
            this.descriptor = serverDescriptor;
        }
    }

    public static class ClientInfoMutable implements ClientState.ClientInfo {
        public final UUID uid;
        public volatile Cloud cloud;

        public ClientInfoMutable(io.aether.api.clienttypes.ClientInfo c) {
            this(c.getUid(), c.getCloud());
        }

        public ClientInfoMutable(UUID uid, Cloud cloud) {
            this.uid = uid;
            this.cloud = cloud;
        }

        public ClientInfoMutable(UUID uid) {
            this.uid = uid;
        }

        @Override
        public UUID getUid() {
            return uid;
        }

        @Override
        public Cloud getCloud() {
            return cloud;
        }

        @Override
        public void setCloud(Cloud cloud) {
            this.cloud = cloud;
        }
    }
}
