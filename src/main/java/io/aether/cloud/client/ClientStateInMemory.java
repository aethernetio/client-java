package io.aether.cloud.client;

import io.aether.common.Cloud;
import io.aether.common.ServerDescriptor;
import io.aether.crypt.CryptoLib;
import io.aether.crypt.Key;
import io.aether.crypt.SignChecker;
import io.aether.logger.Log;
import io.aether.net.meta.ApiManager;
import io.aether.net.meta.MetaType;
import io.aether.net.serialization.SerializationContext;
import io.aether.utils.ConcurrentHashSet;
import io.aether.utils.dataio.DataInOut;
import io.aether.utils.dataio.DataInOutStatic;
import io.aether.utils.flow.Flow;
import io.aether.utils.slots.AMFuture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientStateInMemory implements ClientState {
    private final List<URI> registrationUri = new CopyOnWriteArrayList<>();
    private final Map<Integer, ServerInfo> servers = new ConcurrentHashMap<>();
    private final Map<UUID, ClientInfo> clients = new ConcurrentHashMap<>();
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

    public ClientStateInMemory(UUID parentUid, List<URI> registrationUri, Set<SignChecker> rootSigners, CryptoLib cryptoLib) {
        assert parentUid != null;
        this.cryptoLib = cryptoLib;
        this.parentUid = parentUid;
        this.registrationUri.addAll(registrationUri);
        if (rootSigners != null) this.rootSigners.addAll(rootSigners);
        this.rootSigners.addAll(Set.of(
                SignChecker.of("SODIUM_SIGN_PUBLIC:4F202A94AB729FE9B381613AE77A8A7D89EDAB9299C3320D1A0B994BA710CCEB"),
                SignChecker.of("HYDROGEN_SIGN_PUBLIC:883B4D7E0FB04A38CA12B3A451B00942048858263EE6E6D61150F2EF15F40343")
        ));
    }

    public ClientStateInMemory(UUID parentUid, List<URI> registrationUri) {
        this(parentUid, registrationUri, null);
    }

    private ClientStateInMemory(DTO dto) {
        this.uid = dto.uid;
        this.alias = dto.alias;
        Flow.flow(dto.clients).toMapExtractKey(clients, ClientInfo::getUid);
        Flow.flow(dto.servers).toMapExtractKey(servers, ServerInfo::getServerId);
        this.parentUid = dto.parentUid;
        this.masterKey = dto.masterKey;
        this.cryptoLib = dto.cryptoLib;
        this.rootSigners.addAll(Flow.flow(dto.rootSigners).map(SignChecker::of).toSet());
        this.countServersForRegistration = dto.countServersForRegistration;
        this.timeoutForConnectToRegistrationServer = dto.timeoutForConnectToRegistrationServer;
        this.registrationUri.addAll(dto.registrationUri);
        this.pingDuration.set(dto.pingDuration);
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
        return clients.computeIfAbsent(uid, ClientInfo::new);
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

    public ClientInfo getUidInfo(UUID uid) {
        assert uid != null;
        return clients.computeIfAbsent(uid, ClientInfo::new);
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
        DTO.META.getSerializer().put(SerializationContext.STUB, d, toDTO());
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

    private DTO toDTO() {
        return new DTO(
                registrationUri,
                new HashSet<>(servers.values()),
                new HashSet<>(clients.values()),
                rootSigners,
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
            throw new RuntimeException(e);
        }
    }

    public static ClientStateInMemory load(byte[] data) {
        var dto = DTO.META.getDeserializer().put(SerializationContext.STUB, new DataInOutStatic(data));
        return new ClientStateInMemory(dto);
    }

    private static class DTO {
        static final MetaType<DTO> META = ApiManager.getType(DTO.class);
        List<URI> registrationUri;
        Set<ServerInfo> servers;
        Set<ClientInfo> clients;
        Set<String> rootSigners;
        CryptoLib cryptoLib;
        long pingDuration;
        UUID parentUid;
        int countServersForRegistration = 1;
        int timeoutForConnectToRegistrationServer = 10;
        UUID uid;
        UUID alias;
        Key masterKey;

        public DTO() {
        }

        public DTO(List<URI> registrationUri,
                   Set<ServerInfo> servers,
                   Set<ClientInfo> clients,
                   Set<SignChecker> rootSigners,
                   CryptoLib cryptoLib,
                   long pingDuration,
                   UUID parentUid,
                   int countServersForRegistration,
                   int timeoutForConnectToRegistrationServer,
                   UUID uid,
                   UUID alias,
                   Key masterKey) {
            this.registrationUri = registrationUri;
            this.servers = servers;
            this.clients = clients;
            this.rootSigners = Flow.flow(rootSigners).mapToString().toSet();
            this.cryptoLib = cryptoLib;
            this.pingDuration = pingDuration;
            this.parentUid = parentUid;
            this.countServersForRegistration = countServersForRegistration;
            this.timeoutForConnectToRegistrationServer = timeoutForConnectToRegistrationServer;
            this.uid = uid;
            this.alias = alias;
            this.masterKey = masterKey;
        }
    }

    public static class ServerInfo implements ClientState.ServerInfo {
        final int sid;
        volatile ServerDescriptor descriptor;

        public ServerInfo(int sid) {
            this.sid = sid;
        }

        public ServerInfo(ServerDescriptor descriptor) {
            this(descriptor.id());
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

    public static class ClientInfo implements ClientState.ClientInfo {
        public final UUID uid;
        public volatile Cloud cloud;

        public ClientInfo(UUID uid) {
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
