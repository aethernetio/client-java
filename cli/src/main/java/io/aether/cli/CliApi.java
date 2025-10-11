package io.aether.cli;

import io.aether.api.common.AccessGroup;
import io.aether.api.common.CryptoLib;
import io.aether.cloud.client.*;
import io.aether.common.AccessGroupI;
import io.aether.logger.Log;
import io.aether.utils.AString;
import io.aether.utils.Destroyer;
import io.aether.utils.RU;
import io.aether.utils.ToString;
import io.aether.utils.consoleCanonical.ConsoleMgrCanonical.*;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.interfaces.AFunction;
import io.aether.utils.slots.EventConsumer;
import io.aether.utils.slots.EventConsumerWithQueue;
import io.aether.utils.streams.Value;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Command Line Interface API for Aether Cloud Client operations.
 */
public class CliApi {
    // UNIFIED EXECUTOR for all asynchronous CLI operations
    private static final Executor CLI_EXECUTOR = Executors.newSingleThreadExecutor(r -> new Thread(r, "CLI-Async-Worker"));

    // Executor for destroy operations to prevent deadlocks
    private static final Executor DESTROY_EXECUTOR = Executors.newSingleThreadExecutor(r -> new Thread(r, "CLI-Destroy-Worker"));

    public final Destroyer destroyer = new Destroyer("CliApi");
    public CreateApi createApi;
    public ShowApi showApi;

    private static void logFlow(String message, Object... args) {
        var l = new ArrayList<>(Arrays.asList(args));
        l.add(Log.SYSTEM_COMPONENT);
        l.add("CLI");
        Log.info(message, l.toArray());
    }

    /**
     * Asynchronously waits for the provided Future to complete (usually client.destroy()),
     * and then calls destroy() on the root CLI Destroyer via an executor to prevent deadlocks.
     */
    private static void completeCliSession(Destroyer cliDestroyer, AFuture future) {
        future.to(CLI_EXECUTOR, () -> {
            logFlow("Asynchronous operation finished. Completing root CLI destroyer.");
            // Execute destroy via a separate executor to prevent deadlocks
            DESTROY_EXECUTOR.execute(() -> {
                cliDestroyer.destroy(true);
            });
        }).onError(e -> {
            logFlow("Error during asynchronous operation. Completing root CLI destroyer with error.", "error", e.getMessage());
            DESTROY_EXECUTOR.execute(() -> {
                cliDestroyer.destroy(true);
            });
        });
    }

    /**
     * Safely destroys resources via an executor
     */
    @Doc("Safely destroys CLI resources")
    public AFuture safeDestroy() {
        AFuture destroyFuture = AFuture.make();
        DESTROY_EXECUTOR.execute(() -> {
            try {
                logFlow("Starting safe destroy of CLI resources");
                destroyer.destroy(true).timeout(10, () -> {
                    Log.warn("Timeout during safe destroy");
                }).to(destroyFuture);
            } catch (Exception e) {
                Log.error("Error during safe destroy", e);
                destroyFuture.error(e);
            }
        });
        return destroyFuture;
    }

    @Doc("Show version of cli instrument")
    @Example("$exCmd version")
    public String version() {
        logFlow("Executing command: version");
        try (var is = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/aether-cli-version.txt")))) {
            return is.readLine();
        } catch (Exception e) {
            RU.error(e);
            return null;
        }
    }

    @Api
    @Doc("Create a client or group")
    public CreateApi create(
            @Doc("Specify a time limit for the object creation")
            @Optional("5")
            int timeout) {
        logFlow("Executing command: create", "timeout", timeout);
        createApi = new CreateApi();
        return createApi;
    }

    @Api
    @Doc("Change properties of a client or group")
    public ChangeApi change(
            @Doc("Previously saved client state file")
            @Optional(value = "state.bin")
            File state) {
        logFlow("Executing command: change", "stateFile", state);
        // Create client locally
        var client = new AetherCloudClient(ClientStateInMemory.load(state));
        destroyer.add(client);
        return new ChangeApi(client);
    }

    @Doc("Check the possibility of sending messages between two clients")
    @Example("$exCmd check-access --uid1 3ac93165-3d37-4970-87a6-fa4ee27744e4 --uid2 1a2b3c4d-5e6f-7080-90a0-b1c2d3e4f5a6")
    public ARFuture<Boolean> checkAccess(
            @Doc("Previously saved client state file")
            @Optional(value = "state.bin")
            File state,
            @Doc("Client 1 UUID")
            UUID uid1,
            @Doc("Client 2 UUID")
            UUID uid2) {
        logFlow("Executing command: checkAccess", "stateFile", state, "uid1", uid1, "uid2", uid2);
        // Create client locally
        var client = new AetherCloudClient(ClientStateInMemory.load(state));
        destroyer.add(client);

        // Capture client for use in lambda expression
        AetherCloudClient finalClient = client;

        // Explicitly specify the generic type for ARFuture.run to avoid error
        return ARFuture.<Boolean>run2(CLI_EXECUTOR, () -> finalClient.checkAccess(uid1, uid2))
                .apply(() -> {
                    // Asynchronously destroy client after operation completion via executor
                    completeCliSession(destroyer, finalClient.destroy(true));
                });
    }

    @Api
    @Doc("Send a message to an address")
    public SendApi send(
            @Doc("Previously saved client state file")
            @Optional(value = "state.bin")
            File state,
            @Doc("Destination UUID address")
            UUID address) {
        logFlow("Executing command: send", "stateFile", state, "address", address);
        // Create client locally
        var client = new AetherCloudClient(ClientStateInMemory.load(state));
        destroyer.add(client);
        var st = client.getMessageNode(address, MessageEventListener.DEFAULT);
        return new SendApi(st, client);
    }

    @Api
    @Doc("Show client state, groups, and incoming messages")
    public ShowApi show(
            @Doc("Previously saved client state file")
            @Optional(value = "state.bin")
            File state) {
        logFlow("Executing command: show", "stateFile", state);
        showApi = new ShowApi(ClientStateInMemory.load(state));
        return showApi;
    }

    /**
     * Message container class.
     */
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
        public void toString(AString sb) {
            sb.add(address).add(":").add(data);
        }
    }

    /**
     * API for changing resources (e.g., groups).
     */
    public class ChangeApi {
        private final AetherCloudClient client;

        public ChangeApi(AetherCloudClient client) {
            this.client = client;
        }

        @Api
        @Doc("Change an access group")
        public ChangeGroupApi group(
                @Doc("Access group ID")
                long id) {
            return new ChangeGroupApi(id, client);
        }

        /**
         * API for changing an access group.
         */
        public class ChangeGroupApi {
            private final long id;
            private final AetherCloudClient client;

            public ChangeGroupApi(long id, AetherCloudClient client) {
                this.id = id;
                this.client = client;
            }

            @Doc("Add clients to an access group")
            @Example("$exCmd change group 123456 add 3ac93165-3d37-4970-87a6-fa4ee27744e4,1a2b3c4d-5e6f-7080-90a0-b1c2d3e4f5a6")
            public AFuture add(
                    @Doc("Set of client UUIDs to add")
                    @StdIn Set<UUID> uid) {
                logFlow("ChangeGroupApi: add started", "groupId", id);
                AFuture res = AFuture.make();

                // CAPTURE: Capture the reference to the Destroyer in the outer (safe) scope
                Destroyer apiDestroyer = CliApi.this.destroyer;

                AFuture.run(CLI_EXECUTOR, () -> {
                    client.getAuthApi(a -> {
                        logFlow("ChangeGroupApi: got AuthApi, starting add op");
                        AFuture.all(Flow.flow(uid).map(u -> a.addToAccessGroup(id, u)).map(ARFuture::toFuture).toList()).to(res);
                    });
                });

                return res.apply(() -> {
                    logFlow("ChangeGroupApi: add finished. Starting asynchronous destroy.");
                    completeCliSession(apiDestroyer, client.destroy(true));
                });
            }

            @Doc("Remove clients from an access group")
            @Example("$exCmd change group 123456 remove 3ac93165-3d37-4970-87a6-fa4ee27744e4")
            public AFuture remove(
                    @Doc("Set of client UUIDs to remove")
                    Set<UUID> uid) {
                logFlow("ChangeGroupApi: remove started", "groupId", id);
                AFuture res = AFuture.make();

                // CAPTURE: Capture the reference to the Destroyer in the outer (safe) scope
                Destroyer apiDestroyer = CliApi.this.destroyer;

                AFuture.run(CLI_EXECUTOR, () -> {
                    client.getAuthApi(a -> {
                        logFlow("ChangeGroupApi: got AuthApi, starting remove op");
                        AFuture.all(Flow.flow(uid).map(u -> a.removeFromAccessGroup(id, u)).map(ARFuture::toFuture).toList()).to(res);
                    });
                });

                return res.apply(() -> {
                    logFlow("ChangeGroupApi: remove finished. Starting asynchronous destroy.");
                    completeCliSession(apiDestroyer, client.destroy(true));
                });
            }
        }
    }

    /**
     * API for showing resources.
     */
    public class ShowApi {
        private final ClientStateInMemory state;
        public EventConsumer<Msg> messages = new EventConsumerWithQueue<>();

        public ShowApi(ClientStateInMemory state) {
            this.state = state;
        }

        @Doc("Show client state")
        @Example("$exCmd show state")
        public ClientStateInMemory state() {
            return state;
        }

        @Doc("Show all access groups for client")
        @Example("$exCmd show groups -c 3ac93165-3d37-4970-87a6-fa4ee27744e4")
        @Alias("g")
        public ARFuture<Set<Long>> groups(
                @Doc("Specified client uid. Default uid from state")
                @Optional
                @Alias("c")
                UUID targetClient) {
            // Create client locally
            var client = AetherCloudClient.of(state);
            CliApi.this.destroyer.add(client); // Add to the root destroyer

            if (targetClient == null) {
                targetClient = client.getUid();
            }
            UUID finalTargetClient = targetClient;

            ARFuture<Set<Long>> res = ARFuture.run(CLI_EXECUTOR, () -> client.getClientGroups(finalTargetClient)).decompose();

            return res.apply(() -> {
                // Asynchronously destroy client after operation completion via executor
                completeCliSession(CliApi.this.destroyer, client.destroy(true));
            });
        }

        @Doc("Show the contents (details) of the access groups for the specified IDs")
        @Alias("gd")
        @Example("$exCmd show groups-details 123456,789012")
        public ARFuture<List<AccessGroup>> groupsDetails(
                @Doc("IDs of access groups")
                Set<Long> ids) {
            // Create client locally
            var client = AetherCloudClient.of(state);
            CliApi.this.destroyer.add(client); // Add to the root destroyer

            ARFuture<List<AccessGroup>> res = ARFuture.run(CLI_EXECUTOR, () -> ARFuture.all(Flow.flow(ids)
                    .map(groupId -> client.getGroup(groupId).map(v -> v))
                    .toList())).decompose();

            return res.apply(() -> {
                // Asynchronously destroy client after operation completion via executor
                completeCliSession(CliApi.this.destroyer, client.destroy(true));
            });
        }

        @Doc("Show all clients that the current client can access. The function does not work for public clients")
        @Alias("aac")
        @Example("$exCmd show all-accessed-clients")
        public ARFuture<Set<UUID>> allAccessedClients(
                @Doc("Specified client uid. Default uid from state")
                @Optional @Alias("c") UUID targetClient) {
            // Create client locally
            var client = AetherCloudClient.of(state);
            CliApi.this.destroyer.add(client); // Add to the root destroyer

            if (targetClient == null) {
                targetClient = client.getUid();
            }
            UUID finalTargetClient = targetClient;

            ARFuture<Set<UUID>> res = ARFuture.run(CLI_EXECUTOR, () -> client.getAllAccessedClients(finalTargetClient)).decompose();

            return res.apply(() -> {
                // Asynchronously destroy client after operation completion via executor
                completeCliSession(CliApi.this.destroyer, client.destroy(true));
            });
        }

        @Doc("Wait for and show incoming messages")
        @Example("$exCmd show messages --wait-time 10000 --filter 3ac93165-3d37-4970-87a6-fa4ee27744e4")
        public EventConsumer<Msg> messages(
                @Doc("Filter messages by sender UUIDs")
                @Optional Set<UUID> filter,
                @Doc("Exclude messages from these sender UUIDs")
                @Optional Set<UUID> not,
                @Optional("5000")
                @Doc("The time in milliseconds to wait for new messages. After timeout, the client session is closed.")
                long waitTime,
                @Optional("bin")
                @Doc("Output format for messages (e.g., bin, json, human)")
                String fileOutFormat,
                @Optional("message")
                @Doc("Specify the file name template for output")
                File fileOut,
                @Optional("utf8")
                @Doc("Console output encoding/format (e.g., utf8, json, bin)")
                String console,
                @Optional("true")
                @Doc("Set true if you want to append data to the file, false to overwrite")
                boolean fileAppend
        ) {
            logFlow("ShowApi: messages command started", "waitTimeMs", waitTime, "filterUids", filter, "notUids", not);
            // Create client locally
            var client = new AetherCloudClient(state);
            CliApi.this.destroyer.add(client);
            logFlow("ShowApi: Client instance created", "clientUid", client.getUid());

            // CAPTURE: Capture the reference to the Destroyer in the outer (safe) scope
            Destroyer apiDestroyer = CliApi.this.destroyer;

            client.onClientStream(m -> {
                UUID consumerUid = m.getConsumerUUID();
                logFlow("ShowApi: received new MessageNode stream", "consumerUid", consumerUid);

                m.toConsumer(d -> {
                    logFlow("ShowApi: Message data received from stream", "from", consumerUid, "dataLength", d.length);

                    boolean isFiltered = false;
                    if (filter != null && !filter.contains(consumerUid)) {
                        logFlow("ShowApi: Message SKIPPED (Filter check failed)", "from", consumerUid);
                        isFiltered = true;
                    }
                    if (!isFiltered && not != null && not.contains(consumerUid)) {
                        logFlow("ShowApi: Message SKIPPED (Exclude check failed)", "from", consumerUid);
                        isFiltered = true;
                    }

                    if (!isFiltered) {
                        var msg = new Msg(consumerUid, d);
                        messages.fire(msg);
                        logFlow("ShowApi: Message PROCESSED and fired to console", "from", consumerUid, "dataLength", d.length);
                    }
                });
            });

            // Start connection in CLI_EXECUTOR
            AFuture.run(CLI_EXECUTOR, client::connect)
                    .to(() -> {
                        logFlow("ShowApi: Client connect successful. Scheduling timeout.", "clientUid", client.getUid());

                        // Schedule forced termination after waitTime.
                        RU.schedule(waitTime, () -> {
                            logFlow("ShowApi: Timeout triggered after %s ms. Starting client destroy.", waitTime);
                            completeCliSession(apiDestroyer, client.destroy(true));
                        });
                        logFlow("ShowApi: Timeout scheduled", "waitTimeMs", waitTime);
                    }).onError(e -> {
                        logFlow("ShowApi: Client connect FAILED. Scheduling exit.", "error", e.getMessage());
                        RU.schedule(10, () -> {
                            logFlow("ShowApi: Client connect failed, completing session.");
                            apiDestroyer.destroy(true);
                        });
                    });

            return messages;
        }
    }

    /**
     * API for creating resources (client, group).
     */
    public class CreateApi {

        @Doc("Create a new client")
        @Example("$exCmd create client --parent 3ac93165-3d37-4970-87a6-fa4ee27744e4 --crypto-lib SODIUM")
        public ARFuture<ClientState> client(
                @Optional("3ac93165-3d37-4970-87a6-fa4ee27744e4")
                @Doc("Parent client UUID")
                UUID parent,
                @Optional("tcp://registration.aethernet.io:9010")
                @Doc("Registration URI")
                URI regUri,
                @Optional("SODIUM")
                @Doc("Cryptographic library to use")
                CryptoLib cryptoLib,
                @Optional("false")
                @Doc("Use development registration URI")
                boolean dev,
                @Optional("bin")
                @Doc("Output format for client state file (e.g., bin)")
                String fileOutFormat,
                @Optional("state.bin")
                @Doc("File path for saving the client state")
                File fileOut
        ) {
            logFlow("CreateApi: client creation started", "parent", parent);
            if (dev) {
                regUri = URI.create("tcp://reg-dev.aethernet.io:9010");
            }
            var state = new ClientStateInMemory(parent, List.of(regUri), null, cryptoLib);
            // Create client locally
            var client = new AetherCloudClient(state);
            CliApi.this.destroyer.add(client);

            // CAPTURE: Capture the reference to the Destroyer in the outer (safe) scope
            Destroyer apiDestroyer = CliApi.this.destroyer;

            return ARFuture.<ClientState>run(CLI_EXECUTOR, () -> {
                client.startFuture.waitDone(); // Wait synchronously in CLI_EXECUTOR
                logFlow("CreateApi: Client startFuture completed successfully", "uid", state.getUid());
                return state;
            }).apply(() -> {
                logFlow("CreateApi: Client creation finished. Starting asynchronous destroy.");
                completeCliSession(apiDestroyer, client.destroy(true));
            });
        }

        @Doc("Create a new access group")
        @Example("$exCmd create group --owner 3ac93165-3d37-4970-87a6-fa4ee27744e4 1a2b3c4d-5e6f-7080-90a0-b1c2d3e4f5a6,c3d4e5f6-7890-1234-5678-90abcdef0123")
        public ARFuture<Long> group(
                @Optional(value = "state.bin")
                @Doc("Client state file used for operation")
                File state,
                @Optional
                @Doc("Owner UUID of the new access group (defaults to client's UID)")
                UUID owner,
                @Doc("Set of client UUIDs to initially include in the group")
                @Optional Set<UUID> uids) {
            logFlow("CreateApi: group creation started");
            // Create client locally
            var client = new AetherCloudClient(ClientStateInMemory.load(state));
            CliApi.this.destroyer.add(client);
            if (owner == null) {
                owner = client.getUid();
            }
            if (uids == null) {
                uids = Set.of();
            }

            UUID finalOwner = owner;
            Set<UUID> finalUids = uids;
            // CAPTURE: Capture the reference to the Destroyer in the outer (safe) scope
            Destroyer apiDestroyer = CliApi.this.destroyer;

            return ARFuture.<AccessGroupI>run(CLI_EXECUTOR, () ->
                    client.createAccessGroupWithOwner(finalOwner, finalUids.toArray(new UUID[0])).get()
            ).apply(() -> {
                logFlow("CreateApi: group creation finished. Starting asynchronous destroy.");
                completeCliSession(apiDestroyer, client.destroy(true));
            }).map(AccessGroupI::getId);
        }
    }

    /**
     * API for sending messages.
     */
    public class SendApi {
        private final MessageNode st;
        private final AetherCloudClient client; // Store the client passed from the send method

        public SendApi(MessageNode st, AetherCloudClient client) {
            this.st = st;
            this.client = client;
        }

        @Doc("Send a text message")
        @Example("$exCmd send 3ac93165-3d37-4970-87a6-fa4ee27744e4 text \"Hello, world!\"")
        public AFuture text(
                @Doc("Text content to send")
                String text) {
            logFlow("SendApi: sending text message", "textLength", text.length());
            AFuture res = AFuture.make();

            // Execute send via executor
            AFuture.run(CLI_EXECUTOR, () -> {
                st.send(Value.ofForce(text.getBytes(StandardCharsets.UTF_8)).linkFuture(res));
            }).onError(res::error);

            // CAPTURE: Capture the reference to the Destroyer in the outer (safe) scope
            Destroyer apiDestroyer = CliApi.this.destroyer;

            return res.to(() -> {
                logFlow("SendApi: send finished. Starting asynchronous destroy.");
                completeCliSession(apiDestroyer, client.destroy(true));
            });
        }

        @Doc("Send file content")
        @Example("$exCmd send 3ac93165-3d37-4970-87a6-fa4ee27744e4 file my_document.pdf")
        public AFuture file(
                @Doc("File to send")
                File file) {
            logFlow("SendApi: sending file", "fileName", file.getName());
            AFuture res = AFuture.make();

            // Execute send via executor
            AFuture.run(CLI_EXECUTOR, () -> {
                try (var is = new FileInputStream(file)) {
                    var data = is.readAllBytes();
                    st.send(Value.ofForce(data, (o) -> {
                        res.done();
                        logFlow("SendApi: file message operation completed (sent to buffer)");
                    }));
                } catch (Exception e) {
                    RU.error(e);
                    res.error(e);
                }
            });

            // CAPTURE: Capture the reference to the Destroyer in the outer (safe) scope
            Destroyer apiDestroyer = CliApi.this.destroyer;

            return res.apply(() -> {
                logFlow("SendApi: send finished. Starting asynchronous destroy.");
                completeCliSession(apiDestroyer, client.destroy(true));
            });
        }

        @Doc("Send data from standard input")
        @Example("echo \"data\" | $exCmd send 3ac93165-3d37-4970-87a6-fa4ee27744e4 stdin")
        public void stdIn(
                @Doc("Data read from standard input")
                @StdIn byte[] data) {
            logFlow("SendApi: sending stdin data", "dataLength", data.length);
            // Execute send via executor
            AFuture.run(CLI_EXECUTOR, () -> {
                st.send(Value.ofForce(data));
            });
        }
    }
}