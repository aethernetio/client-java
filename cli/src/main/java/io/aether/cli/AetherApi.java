package io.aether.cli;

import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientState;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.common.AccessGroup;
import io.aether.common.AccessGroupI;
import io.aether.crypt.CryptoLib;
import io.aether.utils.RU;
import io.aether.utils.consoleCanonical.ConsoleMgrCanonical.*;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.streams.Gate;
import io.aether.utils.streams.Value;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class AetherApi {
    @Api
    public CreateApi create(
            @Doc("Specify a time limit for the time limit for object creation")
            @Optional("5")
            int timeout) {
        return new CreateApi();
    }

    @Api
    public ChangeApi change(
            @Doc("Previously saved client state")
            @Optional(value = "state.bin")
            File state) {
        AetherCloudClient client = new AetherCloudClient(ClientStateInMemory.load(state));
        return new ChangeApi(client);
    }

    @Doc("Check the possibility of sending messages between two clients")
    public ARFuture<Boolean> checkAccess(
            @Doc("Previously saved client state")
            @Optional(value = "state.bin")
            File state, UUID uid1, UUID uid2) {
        AetherCloudClient client = new AetherCloudClient(ClientStateInMemory.load(state));
        return client.checkAccess(uid1, uid2);
    }

    @Api
    public SendApi send(@Doc("Previously saved client state") @Optional(value = "state.bin") File state, UUID address) {
        AetherCloudClient client = new AetherCloudClient(ClientStateInMemory.load(state));
        var st = client.openStreamToClient(address);
        return new SendApi(st);
    }

    @Api
    public ShowApi show(
            @Doc("Previously saved client state")
            @Optional(value = "state.bin")
            File state) {
        return new ShowApi(ClientStateInMemory.load(state));
    }


    public static class ShowApi {
        private final ClientStateInMemory state;

        public ShowApi(ClientStateInMemory state) {
            this.state = state;
        }

        @Doc("Show client state")
        public ClientStateInMemory state() {
            return state;
        }

        @Doc("Show all access groups for client")
        public ARFuture<Set<Long>> groups(
                @Doc("Specified client uid. Default uid from state")
                @Optional("client")
                UUID targetClient) {
            var c = AetherCloudClient.of(state);
            if (targetClient == null) {
                targetClient = c.getUid();
            }
            return c.getClientGroups(targetClient);
        }

        @Doc("Show the contents of the access groups for the specified IDs")
        @Alias("gd")
        public ARFuture<List<AccessGroup>> groupsDetails(
                @Doc("IDs of access groups")
                Set<Long> ids) {
            var client = AetherCloudClient.of(state);
            return ARFuture.all(Flow.flow(ids)
                    .map(groupId -> client.getGroup(groupId).map(v -> v))
                    .toList());
        }

        @Doc("Show all available clients. The function does not work for public clients")
        public ARFuture<Set<UUID>> showAllAccessedClients(@Optional("client") UUID targetClient) {
            var c = AetherCloudClient.of(state);
            if (targetClient == null) {
                targetClient = c.getUid();
            }
            var client = AetherCloudClient.of(state);
            return client.getAllAccessedClients(targetClient);
        }

        public BlockingQueue<Msg> messages(@Optional Set<UUID> filter, @Optional Set<UUID> not, @Optional long waitTime) {
            BlockingQueue<Msg> result = new ArrayBlockingQueue<>(100);
            AetherCloudClient client = new AetherCloudClient(state);
            client.onClientStream(m -> {
                if (filter != null && !filter.contains(m.getConsumerUUID())) return;
                if (not != null && not.contains(m.getConsumerUUID())) return;
                m.up().toConsumer(d -> {
                    result.add(new Msg(m.getConsumerUUID(), d));
                });
            });
            client.startFuture.to(() -> {
                RU.schedule(waitTime, () -> client.destroy(true));
            });
            return result;
        }

        public static class Msg {
            public final UUID address;
            public final byte[] data;

            public Msg(UUID address, byte[] data) {
                this.address = address;
                this.data = data;
            }
        }
    }

    public static class CreateApi {
        @Doc("Create a new client")
        public ARFuture<ClientState> client(UUID parent,
                                            @Optional("tcp://registration.aethernet.io:9001") URI regUri,
                                            @Optional("SODIUM") CryptoLib cryptoLib, @Optional("false") boolean dev
        ) {
            if (dev) {
                regUri = URI.create("tcp://reg-dev.aethernet.io:9001");
            }
            var state = new ClientStateInMemory(parent, List.of(regUri), null, cryptoLib);
            AetherCloudClient client = new AetherCloudClient(state);
            return client.startFuture.apply(() -> client.destroy(true)).mapRFuture(() ->{
                return state;
            });
        }

        public ARFuture<Long> group(@Optional(value = "state.bin") File state, @Optional UUID owner, @Optional Set<UUID> uids) {
            AetherCloudClient client = new AetherCloudClient(ClientStateInMemory.load(state));
            if (owner == null) {
                owner = client.getUid();
            }
            var future = client.createAccessGroupWithOwner(owner, uids.toArray(new UUID[0]));
            return future.map(AccessGroupI::getId).to(() -> {
                client.destroy(true);
            });
        }
    }

    public static class ChangeApi {
        private final AetherCloudClient client;

        public ChangeApi(AetherCloudClient client) {
            this.client = client;
        }

        @Api
        public ChangeGroupApi group(long id) {
            return new ChangeGroupApi(id);
        }

        public class ChangeGroupApi {
            private final long id;

            public ChangeGroupApi(long id) {
                this.id = id;
            }

            @Doc("Add clients to access group")
            public void add(@StdIn Set<UUID> uid) {
                client.getGroup(id).to(g -> g.addAll(uid.toArray(new UUID[0])));
            }

            @Doc("Remove clients from access group")
            public void remove(Set<UUID> uid) {
                client.getGroup(id).to(g -> g.removeAll(uid.toArray(new UUID[0])));
            }
        }
    }

    public static class SendApi {
        private final Gate<byte[], byte[]> st;

        public SendApi(Gate<byte[], byte[]> st) {
            this.st = st;
        }

        public void text(String text) {
            st.send(Value.ofForce(text.getBytes(StandardCharsets.UTF_8)));
        }

        public void file(File file) {
            try (var is = new FileInputStream(file)) {
                var data = is.readAllBytes();
                st.send(Value.ofForce(data));
            } catch (Exception e) {
                RU.error(e);
            }
        }

        public void stdIn(@StdIn byte[] data) {
            st.send(Value.ofForce(data));
        }
    }
}
