package io.aether.cli;

import com.google.gson.GsonBuilder;
import io.aether.StandardUUIDs;
import io.aether.api.common.AccessGroup;
import io.aether.api.common.Cloud;
import io.aether.api.common.CryptoLib;
import io.aether.api.common.ServerDescriptor;
import io.aether.cloud.client.*;
import io.aether.common.AccessGroupI;
import io.aether.logger.Log;
import io.aether.utils.AString;
import io.aether.utils.Destroyer;
import io.aether.utils.RU;
import io.aether.utils.ToString;
import io.aether.utils.consoleCanonical.ConsoleMgrCanonical.*;
import io.aether.utils.consoleCanonical.ConsoleMgrCanonical.Optional;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.slots.EventConsumer;
import io.aether.utils.slots.EventConsumerWithQueue;
import io.aether.utils.streams.Value;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Command Line Interface API for Aether Cloud Client operations.
 */
@Doc("Command Line Interface API for Aether Cloud Client operations.")
public class CliApi {
    // UNIFIED EXECUTOR for all asynchronous CLI operations
    private static final Executor CLI_EXECUTOR = Executors.newSingleThreadExecutor(r -> new Thread(r, "CLI-Async-Worker"));

    // Executor for destroy operations to prevent deadlocks
    private static final Executor DESTROY_EXECUTOR = Executors.newSingleThreadExecutor(r -> new Thread(r, "CLI-Destroy-Worker"));

    public final Destroyer destroyer = new Destroyer("CliApi");
    private final CliState cliState;
    /**
     * Known static UUID aliases and format details for documentation.
     */
    private final String UUID_ALIASES_DOC = "Known static aliases: \n" +
                                            "    TEST\u00A0(3ac93165-3d37-4970-87a6-fa4ee27744e4)\n" +
                                            "    ROOT\u00A0(ed307ca7-8369-4342-91ee-60c8fc6f9b6b)\n" +
                                            "    ANONYMOUS\u00A0(237e2dc0-21a4-4e83-8184-c43052f93b79)\n" +
                                            "    (Also supports user aliases. Use 'show aliases' to list them)";
    public CreateApi createApi;
    public ShowApi showApi;
    public SetApi setApi;

    /**
     * Constructor receiving the persistent state
     *
     * @param cliState The loaded CLI state
     */
    public CliApi(CliState cliState) {
        this.cliState = cliState;
    }

    /**
     * Resolves UUID aliases (user-defined first, then static) or raw UUID strings.
     * This method is used by ConsoleMgrCanonical as a type converter.
     *
     * @param uuidOrAlias The string to resolve.
     * @return The resolved UUID.
     * @throws IllegalArgumentException if the string is neither a valid alias nor a UUID.
     */
    public UUID resolveUuid(String uuidOrAlias) {
        if (uuidOrAlias == null) {
            return null;
        }

        // 1. Check user-defined aliases (case-sensitive and then uppercase)
        if (cliState.hasAlias(uuidOrAlias)) {
            return UUID.fromString(cliState.getUuidForAlias(uuidOrAlias));
        }

        String upper = uuidOrAlias.toUpperCase();
        if (cliState.hasAlias(upper)) {
            return UUID.fromString(cliState.getUuidForAlias(upper));
        }

        // 2. Check static aliases
        switch (upper) {
            case "TEST":
                return StandardUUIDs.TEST_UID;
            case "ROOT":
                return StandardUUIDs.ROOT_UID;
            case "ANONYMOUS":
                return StandardUUIDs.ANONYMOUS_UID;
        }

        // 3. Try to parse as a raw UUID
        try {
            return UUID.fromString(uuidOrAlias);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID or alias: '" + uuidOrAlias + "'", e);
        }
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

        ClientStateInMemory loadedState = ClientStateInMemory.load(state);
        if (loadedState == null) {
            throw new IllegalStateException("Command requires a valid state file, but it was not found or was corrupted. " +
                                            "Default path is '" + state.getName() + "'. " +
                                            "Please run 'create client' first, or specify a valid state file with --state.");
        }

        // Create client locally
        var client = new AetherCloudClient(loadedState);
        destroyer.add(client);
        return new ChangeApi(client);
    }

    @Doc("Check the possibility of sending messages between two clients")
    @Example("$exCmd check-access --uid1 TEST --uid2 my-friend-alias")
    public ARFuture<Boolean> checkAccess(
            @Doc("Previously saved client state file")
            @Optional(value = "state.bin")
            File state,
            @Doc("Client 1 UUID or alias. \n" + UUID_ALIASES_DOC)
            UUID uid1,
            @Doc("Client 2 UUID or alias. \n" + UUID_ALIASES_DOC)
            UUID uid2) {
        logFlow("Executing command: checkAccess", "stateFile", state, "uid1", uid1, "uid2", uid2);

        ClientStateInMemory loadedState = ClientStateInMemory.load(state);
        if (loadedState == null) {
            return ARFuture.doThrow(new IllegalStateException("Command requires a valid state file, but it was not found or was corrupted. " +
                                                              "Default path is '" + state.getName() + "'. " +
                                                              "Please run 'create client' first, or specify a valid state file with --state."));
        }

        // Create client locally
        var client = new AetherCloudClient(loadedState);
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
            @Doc("Destination UUID address or alias. \n" + UUID_ALIASES_DOC)
            UUID address) {
        logFlow("Executing command: send", "stateFile", state, "address", address);

        ClientStateInMemory loadedState = ClientStateInMemory.load(state);
        if (loadedState == null) {
            throw new IllegalStateException("Command requires a valid state file, but it was not found or was corrupted. " +
                                            "Default path is '" + state.getName() + "'. " +
                                            "Please run 'create client' first, or specify a valid state file with --state.");
        }

        // Create client locally
        var client = new AetherCloudClient(loadedState);
        destroyer.add(client);

        // Wait for client to be fully connected before allowing message sending
        ARFuture<AetherCloudClient> readyClient = client.connect().mapRFuture(() -> client);

        var st = client.getMessageNode(address, MessageEventListener.DEFAULT);
        return new SendApi(st, client, readyClient);
    }

    @Api
    @Doc("Show client state, groups, aliases, and incoming messages")
    public ShowApi show(/* --- state parameter removed --- */) {
        logFlow("Executing command: show");
        showApi = new ShowApi(this.cliState);
        return showApi;
    }

    @Api
    @Doc("Set properties, like user-defined aliases")
    public SetApi set() {
        logFlow("Executing command: set");
        setApi = new SetApi(this.cliState);
        return setApi;
    }

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
                cliDestroyer.destroy(true).to(() -> {
                    // Log active threads after Destroyer completes
                    logFlow("Root CLI destroyer completed. Checking active threads.");
                    Thread.getAllStackTraces().keySet().stream()
                            .filter(t -> t.isAlive() && !t.isDaemon())
                            .forEach(t -> Log.warn("Non-daemon thread still active: $name ($group)", "name", t.getName(), "group", t.getThreadGroup().getName()));
                });
            });
        }).onError(e -> {
            logFlow("Error during asynchronous operation. Completing root CLI destroyer with error.", "error", e.getMessage());
            DESTROY_EXECUTOR.execute(() -> {
                cliDestroyer.destroy(true);
            });
        });
    }

    /**
     * Message container class.
     */
    @Doc("A container for a received message, holding the sender's address and data.")
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
    @Doc("API for changing resources (e.g., groups).")
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
        @Doc("API for changing a specific access group.")
        public class ChangeGroupApi {
            private final long id;
            private final AetherCloudClient client;

            public ChangeGroupApi(long id, AetherCloudClient client) {
                this.id = id;
                this.client = client;
            }

            @Doc("Add clients to an access group")
            @Example("$exCmd change group 123456 add TEST,my-friend-alias")
            public AFuture add(
                    @Doc("Set of client UUIDs or aliases to add. \n" + UUID_ALIASES_DOC)
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
            @Example("$exCmd change group 123456 remove my-friend-alias")
            public AFuture remove(
                    @Doc("Set of client UUIDs or aliases to remove. \n" + UUID_ALIASES_DOC)
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
    @Doc("API for showing resources.")
    public class ShowApi {
        private final CliState cliState; // ALWAYS NOT NULL
        public EventConsumer<Msg> messages = new EventConsumerWithQueue<>();

        /**
         * @param cliState The loaded CLI state (aliases)
         */
        public ShowApi(CliState cliState) {
            this.cliState = cliState;
        }

        /**
         * Internal helper that loads and checks if state.bin exists.
         *
         * @param stateFile The state file to load.
         * @return The loaded ClientStateInMemory
         * @throws IllegalStateException if state.bin was not found or corrupted.
         */
        private ClientStateInMemory loadRequiredState(File stateFile) {
            ClientStateInMemory state = ClientStateInMemory.load(stateFile);
            if (state == null) {
                throw new IllegalStateException("Command requires a valid state file, but it was not found or was corrupted. " +
                                                "Default path is '" + stateFile.getName() + "'. " +
                                                "Please run 'create client' first, or specify a valid state file with --state.");
            }
            return state;
        }

        @Doc("Show all user-defined aliases from ~/.aether-cli-state.json")
        @Example("$exCmd show aliases")
        public String aliases() {
            // This command depends ONLY on cliState, so it will ALWAYS work.
            return new GsonBuilder().setPrettyPrinting().create().toJson(cliState.getAliases());
        }

        @Doc("Show client state. Use --console json for JSON output.")
        @Example("$exCmd show state")
        public ClientStateInMemory state(
                @Doc("Previously saved client state file")
                @Optional(value = "state.bin")
                File state) {
            // This command REQUIRES state.bin
            return loadRequiredState(state);
        }

        @Doc("Show all access groups for a client")
        @Example("$exCmd show groups -c TEST")
        @Alias("g")
        public ARFuture<Set<Long>> groups(
                @Doc("Previously saved client state file")
                @Optional(value = "state.bin")
                File state,
                @Doc("Specified client UID or alias. Defaults to client's UID from state file.\n" + UUID_ALIASES_DOC)
                @Optional
                @Alias("c")
                UUID targetClient) {

            // --- LOAD AND CHECK HERE ---
            ClientStateInMemory requiredState;
            try {
                requiredState = loadRequiredState(state); // Check if state.bin is loaded
            } catch (Exception e) {
                return ARFuture.doThrow(e); // Return error if state.bin is missing
            }
            // -------------------------

            // Create client locally
            var client = AetherCloudClient.of(requiredState); // Use the checked state
            CliApi.this.destroyer.add(client); // Add to the root destroyer

            UUID targetUuid = targetClient;
            if (targetUuid == null) {
                targetUuid = client.getUid();
            }
            UUID finalTargetClient = targetUuid;
            ARFuture<Set<Long>> res = ARFuture.make();
            CLI_EXECUTOR.execute(() -> {
                client.getClientGroups(finalTargetClient).to(res);
            });

            return res.apply(() -> {
                // Asynchronously destroy client after operation completion via executor
                completeCliSession(destroyer, client.destroy(true));
            });
        }

        @Doc("Show the contents (details) of the access groups for the specified IDs")
        @Alias("gd")
        @Example("$exCmd show groups-details 123456,789012")
        public ARFuture<List<AccessGroup>> groupsDetails(
                @Doc("Previously saved client state file")
                @Optional(value = "state.bin")
                File state,
                @Doc("IDs of access groups, separated by comma")
                Set<Long> ids) {

            // --- LOAD AND CHECK HERE ---
            ClientStateInMemory requiredState;
            try {
                requiredState = loadRequiredState(state);
            } catch (Exception e) {
                return ARFuture.doThrow(e);
            }
            // -------------------------

            // Create client locally
            var client = AetherCloudClient.of(requiredState);
            CliApi.this.destroyer.add(client); // Add to the root destroyer

            ARFuture<List<AccessGroup>> res = ARFuture.make();
            CLI_EXECUTOR.execute(() -> {
                ARFuture.all(Flow.flow(ids)
                        .map(groupId -> client.getGroup(groupId).map(v -> v))
                        .toList()).to(res);
            });
            return res.apply(() -> {
                // Asynchronously destroy client after operation completion via executor
                completeCliSession(destroyer, client.destroy(true));
            });
        }

        @Doc("Show all clients that the current client can access. This function does not work for public clients.")
        @Alias("aac")
        @Example("$exCmd show all-accessed-clients")
        public ARFuture<Set<UUID>> allAccessedClients(
                @Doc("Previously saved client file")
                @Optional(value = "state.bin")
                File state,
                @Doc("Specified client UID or alias. Defaults to client's UID from state file.\n" + UUID_ALIASES_DOC)
                @Optional @Alias("c") UUID targetClient) {

            // --- LOAD AND CHECK HERE ---
            ClientStateInMemory requiredState;
            try {
                requiredState = loadRequiredState(state);
            } catch (Exception e) {
                return ARFuture.doThrow(e);
            }
            // -------------------------

            // Create client locally
            var client = AetherCloudClient.of(requiredState);
            CliApi.this.destroyer.add(client); // Add to the root destroyer

            UUID targetUuid = targetClient;
            if (targetUuid == null) {
                targetUuid = client.getUid();
            }
            UUID finalTargetClient = targetUuid;
            ARFuture<Set<UUID>> res = ARFuture.make();
            CLI_EXECUTOR.execute(() -> {
                client.getAllAccessedClients(finalTargetClient).to(res);
            });

            return res.apply(() -> {
                // Asynchronously destroy client after operation completion via executor
                completeCliSession(destroyer, client.destroy(true));
            });
        }

        @Doc("Wait for and show incoming messages")
        @Example("$exCmd show messages --wait-time 10000 --filter my-friend-alias")
        public EventConsumer<Msg> messages(
                @Doc("Previously saved client state file")
                @Optional(value = "state.bin")
                File state,
                @Doc("Filter messages by sender UUIDs or aliases. \n" + UUID_ALIASES_DOC)
                @Optional Set<UUID> filter,
                @Doc("Exclude messages from these sender UUIDs or aliases. \n" + UUID_ALIASES_DOC)
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
            // --- LOAD AND CHECK HERE ---
            // (This command does not return an AFuture, so it throws an exception directly)
            ClientStateInMemory requiredState = loadRequiredState(state);
            // -------------------------

            logFlow("ShowApi: messages command started", "waitTimeMs", waitTime, "filterUids", filter, "notUids", not);
            // Create client locally
            var client = new AetherCloudClient(requiredState); // Use the checked state
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
                        logFlow("ShowApi: Message SKIPPBLED (Exclude check failed)", "from", consumerUid);
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
    @Doc("API for creating resources (client, group).")
    public class CreateApi {

        @Doc("Create a new client")
        @Example("$exCmd create client --parent TEST --alias my-new-client")
        public ARFuture<ClientState> client(
                @Optional("TEST")
                @Doc("Parent client UUID or alias. Defaults to TEST.\n" + UUID_ALIASES_DOC)
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
                File fileOut,
                @Optional
                @Doc("A custom alias to save for the new client's UUID in ~/.aether-cli-state.json")
                String alias
        ) {
            logFlow("CreateApi: client creation started", "parent", parent, "alias", alias);

            if (dev) {
                regUri = URI.create("tcp://reg-dev.aethernet.io:9010");
            }
            var state = new ClientStateInMemory(parent, List.of(regUri), null, cryptoLib);
            // Create client locally
            var client = new AetherCloudClient(state);
            CliApi.this.destroyer.add(client);

            // CAPTURE: Capture the reference to the Destroyer in the outer (safe) scope
            Destroyer apiDestroyer = CliApi.this.destroyer;

            ARFuture<ClientState> resultFuture = ARFuture.make();

            /**
             * ASYNCHRONOUS CHAIN: (Registration -> Cloud/Server Resolution -> Completion)
             * 1. Convert AFuture (client.startFuture) into ARFuture<ClientState> (the result value).
             * 2. Use mapRFuture to build the sequence: (state) -> getCloud(uid) -> mapRFuture(cloud) -> getServerFutures.
             */

            // Step 1: Convert AFuture (registration flag) to ARFuture<ClientState>
            ARFuture<ClientState> registrationChain = client.startFuture.mapRFuture(() -> {
                // When startFuture is done (registration succeeded), return the ClientState object
                return state;
            });

            // Step 2 & 3: Cloud and Server Resolution
            ARFuture<ClientState> resolutionChain = registrationChain.mapRFuture(stateValue -> {
                // stateValue is the result of the registration (ClientState)
                UUID selfUid = stateValue.getUid();

                // 2a. Get the Cloud descriptor (ARFuture<Cloud>)
                ARFuture<Cloud> cloudFuture = client.getCloud(selfUid);

                // 2b, 2c. Map the Cloud result to a flow of ServerDescriptor futures, and wait for all.
                return cloudFuture.mapRFuture(cloud -> {

                    // If Cloud data is empty, skip server resolution and proceed.
                    if (cloud == null || cloud.getData().length == 0) {
                        logFlow("CreateApi: Cloud is empty, skipping server resolution.");
                        return ARFuture.of(stateValue);
                    }

                    logFlow("CreateApi: Cloud found, resolving $serversCount server descriptors.", "serversCount", cloud.getData().length);

                    // Collect futures for all server IDs in the cloud
                    List<ARFuture<ServerDescriptor>> serverFutures = Flow.flow(cloud.getData())
                            .mapToInt()
                            .mapToObj(client::getServer)
                            .toList();

                    // Wait for all server descriptors to resolve
                    return ARFuture.all(ServerDescriptor.class, Flow.flow(serverFutures))
                            .map(descriptions -> stateValue); // Return the original state object after resolution
                });
            });

            resolutionChain.to(resultFuture).onError(resultFuture::tryError);


            // Return a future that handles resource cleanup upon completion
            return resultFuture.apply(() -> {
                if (alias != null && !alias.isBlank()) {
                    cliState.addAlias(alias, state.getUid().toString());
                    logFlow("CreateApi: Saved new alias '" + alias + "' for UUID " + state.getUid());
                }
                logFlow("CreateApi: Client creation finished. Starting asynchronous destroy.");
                completeCliSession(apiDestroyer, client.destroy(true));
            });
        }

        @Doc("Create a new access group")
        @Example("$exCmd create group --owner TEST TEST,ROOT,my-friend-alias")
        public ARFuture<Long> group(
                @Optional(value = "state.bin")
                @Doc("Client state file used for operation")
                File state,
                @Optional
                @Doc("Owner UUID or alias of the new group. Defaults to client's UID from state file.\n" + UUID_ALIASES_DOC)
                UUID owner,
                @Doc("Set of client UUIDs or aliases to initially include in the group. \n" + UUID_ALIASES_DOC)
                @Optional Set<UUID> uids) {
            logFlow("CreateApi: group creation started");

            ClientStateInMemory loadedState = ClientStateInMemory.load(state);
            if (loadedState == null) {
                return ARFuture.doThrow(new IllegalStateException("Command requires a valid state file, but it was not found or was corrupted. " +
                                                                  "Default path is '" + state.getName() + "'. " +
                                                                  "Please run 'create client' first, or specify a valid state file with --state."));
            }

            // Create client locally
            var client = new AetherCloudClient(loadedState); // Use the loaded state
            CliApi.this.destroyer.add(client);

            UUID ownerUuid = owner;
            if (ownerUuid == null) {
                ownerUuid = client.getUid();
            }

            if (uids == null) {
                uids = Set.of();
            }

            UUID finalOwner = ownerUuid;
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
    @Doc("API for sending messages.")
    public class SendApi {
        private final MessageNode st;
        private final AetherCloudClient client; // Store the client passed from the send method
        private final ARFuture<AetherCloudClient> readyClient; // Future that completes when client is ready

        public SendApi(MessageNode st, AetherCloudClient client, ARFuture<AetherCloudClient> readyClient) {
            this.st = st;
            this.client = client;
            this.readyClient = readyClient;
        }

        @Doc("Send a text message")
        @Example("$exCmd send TEST text \"Hello, world!\"")
        public AFuture text(
                @Doc("Text content to send")
                String text) {
            logFlow("SendApi: sending text message", "textLength", text.length());
            AFuture res = AFuture.make();

            // Wait for client to be ready before sending
            readyClient.to(CLI_EXECUTOR, ready -> {
                        st.send(Value.ofForce(text.getBytes(StandardCharsets.UTF_8)).linkFuture(res));
                    })
                    .onError(res::error);

            // CAPTURE: Capture the reference to the Destroyer in the outer (safe) scope
            Destroyer apiDestroyer = CliApi.this.destroyer;

            return res.to(() -> {
                logFlow("SendApi: send finished. Starting asynchronous destroy.");
                completeCliSession(apiDestroyer, client.destroy(true));
            });
        }

        @Doc("Send file content")
        @Example("$exCmd send TEST file my_document.pdf")
        public AFuture file(
                @Doc("File to send")
                File file) {
            logFlow("SendApi: sending file", "fileName", file.getName());
            AFuture res = AFuture.make();

            // Wait for client to be ready before sending
            readyClient.to(CLI_EXECUTOR, ready -> {
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
            }).onError(res::error);

            // CAPTURE: Capture the reference to the Destroyer in the outer (safe) scope
            Destroyer apiDestroyer = CliApi.this.destroyer;

            return res.apply(() -> {
                logFlow("SendApi: send finished. Starting asynchronous destroy.");
                completeCliSession(apiDestroyer, client.destroy(true));
            });
        }

        @Doc("Send data from standard input")
        @Example("echo \"data\" | $exCmd send TEST stdin")
        public AFuture stdIn(
                @Doc("Data read from standard input")
                @StdIn byte[] data) {
            logFlow("SendApi: sending stdin data", "dataLength", data.length);
            AFuture res = AFuture.make();

            // Wait for client to be ready before sending
            readyClient.to(CLI_EXECUTOR, ready -> {
                // Execute send via executor
                AFuture.run(CLI_EXECUTOR, () -> {
                    st.send(Value.ofForce(data).linkFuture(res));
                }).onError(res::error);
            }).onError(res::error);

            // CAPTURE: Capture the reference to the Destroyer in the outer (safe) scope
            Destroyer apiDestroyer = CliApi.this.destroyer;

            return res.apply(() -> {
                logFlow("SendApi: send (stdin) finished. Starting asynchronous destroy.");
                completeCliSession(apiDestroyer, client.destroy(true));
            });
        }
    }

    /**
     * API for setting properties.
     */
    @Doc("API for setting properties.")
    public class SetApi {
        private final CliState cliState;

        public SetApi(CliState cliState) {
            this.cliState = cliState;
        }

        @Doc("Set or update a user-defined alias for a UUID")
        @Example("$exCmd set alias my-friend 3ac93165-3d37-4970-87a6-fa4ee27744e4")
        @Example("$exCmd set alias my-server TEST")
        public void alias(
                @Doc("The name for the alias")
                String aliasName,
                @Doc("The UUID or an existing alias (static or user-defined) to associate with the new alias name. \n" + UUID_ALIASES_DOC)
                UUID uuid
        ) {
            if (aliasName == null || aliasName.isBlank()) {
                throw new IllegalArgumentException("Alias name cannot be empty.");
            }
            if (uuid == null) {
                throw new IllegalArgumentException("UUID cannot be null.");
            }

            // Check if the name conflicts with static aliases
            String upperName = aliasName.toUpperCase();
            if (upperName.equals("TEST") || upperName.equals("ROOT") || upperName.equals("ANONYMOUS")) {
                throw new IllegalArgumentException("Alias name '" + aliasName + "' conflicts with a built-in static alias and cannot be used.");
            }

            cliState.addAlias(aliasName, uuid.toString());
            logFlow("SetApi: Saved alias '" + aliasName + "' for UUID " + uuid);
            // Returns void, as the operation is synchronous
        }
    }
}