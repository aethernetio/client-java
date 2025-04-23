package io.aether.cli;

import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientState;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.common.AccessGroup;
import io.aether.common.AccessGroupI;
import io.aether.crypt.CryptoLib;
import io.aether.utils.AString;
import io.aether.utils.RU;
import io.aether.utils.ToString;
import io.aether.utils.consoleCanonical.ConsoleMgrCanonical.*;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.slots.EventConsumer;
import io.aether.utils.slots.EventConsumerWithQueue;
import io.aether.utils.streams.Gate;
import io.aether.utils.streams.Value;
import io.aether.utils.streams.ValueListener;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AetherApi {
    public CreateApi createApi;
    public ShowApi showApi;
    public AetherCloudClient client;

    @Api
    public CreateApi create(
            @Doc("Specify a time limit for the time limit for object creation")
            @Optional("5")
            int timeout) {
        createApi = new CreateApi();
        return createApi;
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
        client = new AetherCloudClient(ClientStateInMemory.load(state));
        var st = client.openStreamToClient(address);
        return new SendApi(st);
    }

    @Api
    public ShowApi show(
            @Doc("Previously saved client state")
            @Optional(value = "state.bin")
            File state) {
        showApi = new ShowApi(ClientStateInMemory.load(state));
        return showApi;
    }


    public static class ShowApi {
        private final ClientStateInMemory state;
        public EventConsumer<Msg> messages = new EventConsumerWithQueue<>();

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

        public EventConsumer<Msg> messages(@Optional Set<UUID> filter, @Optional Set<UUID> not, @Optional("3000") long waitTime) {
            AetherCloudClient client = new AetherCloudClient(state);
            client.onClientStream(m -> {
                m.up().toConsumer(d -> {
                    if (filter != null && !filter.contains(m.getConsumerUUID())) return;
                    if (not != null && not.contains(m.getConsumerUUID())) return;
                    var msg = new Msg(m.getConsumerUUID(), d);
                    messages.fire(msg);
                });
            });
            client.ping();
            client.startFuture.to(() -> {
                RU.schedule(waitTime, () -> client.destroy(true).waitDone());
            });
            return messages;
        }

        public static class Msg implements ToString {
            public final UUID address;
            public final byte[] data;

            public Msg(UUID address, byte[] data) {
                this.address = address;
                this.data = data;
            }

            @Override
            public String toString() {
                return toString2();
            }

            @Override
            public AString toString(AString sb) {
                sb.add(address).add(":").add(data);
                return sb;
            }
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
            public AFuture add(@StdIn Set<UUID> uid) {
                AFuture res = new AFuture();
                client.getAuthApi(a -> {
                    AFuture.all(Flow.flow(uid).map(u -> a.addToAccessGroup(id, u)).map(ARFuture::toFuture).toList()).to(res);
                });
                return res.apply(() -> client.destroy(true).waitDone());
            }

            @Doc("Remove clients from access group")
            public AFuture remove(Set<UUID> uid) {
                AFuture res = new AFuture();
                client.getAuthApi(a -> {
                    AFuture.all(Flow.flow(uid).map(u -> a.removeFromAccessGroup(id, u)).map(ARFuture::toFuture).toList()).to(res);
                });
                return res.apply(() -> client.destroy(true).waitDone());
            }
        }
    }

    public class CreateApi {

        @Doc("Create a new client")
        public ARFuture<ClientState> client(UUID parent,
                                            @Optional("tcp://registration.aethernet.io:9010") URI regUri,
                                            @Optional("SODIUM") CryptoLib cryptoLib, @Optional("false") boolean dev
        ) {
            if (dev) {
                regUri = URI.create("tcp://reg-dev.aethernet.io:9010");
            }
            var state = new ClientStateInMemory(parent, List.of(regUri), null, cryptoLib);
            client = new AetherCloudClient(state);
            return client.startFuture.apply(() -> {
                client.destroy(true).waitDone();
            }).mapRFuture(() -> {
                return state;
            });
        }

        public ARFuture<Long> group(@Optional(value = "state.bin") File state, @Optional UUID owner, @Optional Set<UUID> uids) {
            AetherCloudClient client = new AetherCloudClient(ClientStateInMemory.load(state));
            if (owner == null) {
                owner = client.getUid();
            }
            var future = client.createAccessGroupWithOwner(owner, uids.toArray(new UUID[0]));
            return future.map(AccessGroupI::getId).apply(() -> {
                client.destroy(true).waitDone();
            });
        }
    }

    public class SendApi {
        private final Gate<byte[], byte[]> st;

        public SendApi(Gate<byte[], byte[]> st) {
            this.st = st;
        }

        public AFuture text(String text) {
            AFuture res = new AFuture();
            st.send(Value.ofForce(text.getBytes(StandardCharsets.UTF_8), new ValueListener() {
                @Override
                public void drop(Object owner) {
                    res.done();
                }
            }));
            return res;
        }

        public AFuture file(File file) {
            AFuture res = new AFuture();
            try (var is = new FileInputStream(file)) {
                var data = is.readAllBytes();
                st.send(Value.ofForce(data, new ValueListener() {
                    @Override
                    public void drop(Object owner) {
                        res.done();
                    }
                }));
            } catch (Exception e) {
                RU.error(e);
            }
            return res.apply(() -> client.destroy(true).waitDone());
        }

        public void stdIn(@StdIn byte[] data) {
            st.send(Value.ofForce(data));
        }
    }
}
