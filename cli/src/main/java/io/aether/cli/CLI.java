package io.aether.cli;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.crypt.CryptoLib;
import io.aether.logger.Log;
import io.aether.utils.AString;
import io.aether.utils.Destroyer;
import io.aether.utils.RU;
import io.aether.utils.flow.Flow;
import io.aether.utils.streams.Value;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CLI {
    public void getMessages(String[] args) {
        var cfg = new ConfigGetMessages(args);
        ClientStateInMemory state;
        state = ClientStateInMemory.load(cfg.clientState);
        Destroyer destroyer = new Destroyer();
        AetherCloudClient client = new AetherCloudClient(state);
        destroyer.add(client);
        if (cfg.formatOutput == FormatOutput.BIN) {
            client.onClientStream(m -> {
                FileOutputStream os = new FileOutputStream(new File(cfg.fileOutput.getParentFile(), cfg.fileOutput.getName() + "-" + m.getConsumerUUID()));
                destroyer.add(os);
                m.up().toConsumer(d -> {
                    os.write(d);
                    os.flush();
                });
            });
        } else if (cfg.formatOutput == FormatOutput.TEXT_UTF8) {
            client.onClientStream(m -> {
                m.up().toConsumer(d -> {
                    System.out.println(m.getConsumerUUID() + "\n" + new String(d, StandardCharsets.UTF_8));
                });
            });
        } else {
            printError("Unsupported format: " + cfg.formatOutput + "\nsupport: text_utf8, bin");
        }
        if (!client.startFuture.waitDoneSeconds(cfg.timeout)) {
            printError("timeout exception");
        }
    }

    public void show(String[] args) {
        var cfg = new ConfigShow(args);
        ClientStateInMemory state = ClientStateInMemory.load(cfg.clientState);
        show(state, cfg);
    }

    public void show(ClientStateInMemory state, Config cfg) {
        var res = AString.of();
        switch (cfg.formatOutput) {
            case JSON:
                res.add("{")
                        .add("\"UUID\":\"").add(state.getUid()).add("\",")
                        .add("\"cryptoLib\":\"").add(state.getCryptoLib()).add("\",")
                        .add("\"masterKey\":\"").add(state.getMasterKey()).add("\",");
                res.add("\"cloud\":[");
                boolean f1 = true;
                for (var c : state.getCloud(state.getUid())) {
                    var s = state.getServerDescriptor(c);
                    if (f1) {
                        f1 = false;
                    } else {
                        res.add(',');
                    }
                    res.add('{');
                    res.add('"').add("id").add('"').add(":").add(s.id).add(',').add("\"addresses\":[");
                    boolean f2 = true;
                    for (var a : s.ipAddress.addresses) {
                        if (f2) {
                            f2 = false;
                        } else {
                            res.add(',');
                        }
                        res.add("{");
                        res.add("\"host\":\"").add(a.address.toInetAddress().getHostAddress()).add("\",");
                        res.add("\"ports\":{");
                        boolean f3 = true;
                        for (var p : a.coderAndPorts) {
                            if (f3) {
                                f3 = false;
                            } else {
                                res.add(',');
                            }
                            res.add("\"").add(p.codec().getName()).add("\":").add(p.port());
                        }
                        res.add("}");
                        res.add("}");
                    }
                    res.add(']');
                    res.add('}');
                }
                res.add("]}");
                System.out.println(res);
                break;
            case BIN:
                state.save(cfg.fileOutput);
                break;
            case HUMAN:
                res.add("UUID: ").add(state.getUid()).add("\n")
                        .add("Crypto Lib: ").add(state.getCryptoLib()).add("\n")
                        .add("Master key: ").add(state.getMasterKey()).add("\n");
                res.add("Cloud:\n");
                for (var c : state.getCloud(state.getUid())) {
                    var s = state.getServerDescriptor(c);
                    res.repeat(4, ' ').add("id: ").add(s.id).add("\n");
                    res.repeat(4, ' ').add("Addresses:\n");
                    for (var a : s.ipAddress.addresses) {
                        res.repeat(8, ' ').add("host: ").add(a.address.toInetAddress().getHostAddress()).add('\n');
                        res.repeat(8, ' ').add("ports:\n");
                        for (var p : a.coderAndPorts) {
                            res.repeat(12, ' ').add(p.codec().getName()).add(":").add(p.port()).add('\n');
                        }
                    }
                }
                System.out.println(res);
                break;
        }

    }

    public void sendMessage(String[] args) {
        var cfg = new ConfigSendMessage(args);
        ClientStateInMemory state = ClientStateInMemory.load(cfg.clientState);
        AetherCloudClient client = new AetherCloudClient(state);
        if (!client.startFuture.waitDoneSeconds(cfg.timeout)) {
            printError("timeout exception");
        }
        var channel = client.openStreamToClient(cfg.receiver);
        if (cfg.sendText != null) {
            channel.send(Value.ofForce(cfg.sendText.getBytes(StandardCharsets.UTF_8)));
        } else if (cfg.sendFile != null) {
            try (var is = new BufferedInputStream(new FileInputStream(cfg.sendFile))) {
                var data = is.readAllBytes();
                channel.send(Value.ofForce(data));
            } catch (Exception e) {
                printError("Send file exception: " + e);
            }
        }
        RU.sleep(1000);
    }

    public ClientStateInMemory create(String[] args) {
        if (args.length == 1) {
            printError("specify the object to create (client|group)");
        }
        if (args[1].equals("client")) {
            return createClient(args);
        } else if (args[1].equals("group")) {
            return createGroup(args);
        } else {
            printError("Cannot create unknown type object: " + args[1]);
            return null;
        }
    }

    public ClientStateInMemory createGroup(String[] args) {
        var cfg = new ConfigCreateGroup(args);
        var state = ClientStateInMemory.load(cfg.clientState);
        AetherCloudClient client = new AetherCloudClient(state);
        if (!client.startFuture.waitDoneSeconds(cfg.timeout)) {
            printError("timeout exception");
        }
        List<UUID> uids = new ObjectArrayList<>();
        if (cfg.stdIn) {
            try (var r = new BufferedReader(new InputStreamReader(System.in))) {
                while (r.ready()) {
                    var l = r.readLine();
                    if (l == null) break;
                    try {
                        uids.add(UUID.fromString(l));
                    } catch (Exception e) {
                        printError("format UUID exception: " + l);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        UUID owner = cfg.owner;
        if (owner == null) {
            owner = client.getUid();
        }
        var future = client.createAccessGroupWithOwner(owner, uids.toArray(new UUID[0]));
        future.waitDoneSeconds(cfg.timeout);
        Log.info("Access group ID: $ag", "ag", future.getNow().getId());
        client.destroy(true).waitDoneSeconds(cfg.timeout);
        return state;
    }

    public ClientStateInMemory createClient(String[] args) {
        var cfg = new ConfigCreateClient(args);
        var state = new ClientStateInMemory(cfg.parentUid, List.of(cfg.registrationURI), null, cfg.cryptoLib);
        AetherCloudClient client = new AetherCloudClient(state);
        if (!client.startFuture.waitDoneSeconds(cfg.timeout)) {
            printError("timeout exception");
        }
        show(state, cfg);
        client.destroy(true).waitDoneSeconds(cfg.timeout);
        return state;
    }

    private static void printHelp() {
        System.out.println("CLI instrument:" +
                           "create <parent uid> - create aether client");
    }

    private static void printError(String s) {
        System.err.println(AString.of().color(AString.Color.RED).add(s).styleClear());
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }
        if (Objects.equals(args[0], "create")) {
            new CLI().create(args);
        } else if (Objects.equals(args[0], "send")) {
            new CLI().sendMessage(args);
        } else if (Objects.equals(args[0], "get")) {
            new CLI().getMessages(args);
        } else if (Objects.equals(args[0], "show")) {
            new CLI().show(args);
        }
    }

    enum FormatOutput {
        JSON,
        HUMAN,
        TEXT_UTF8,
        BIN,
    }

    public static class Config {
        File fileOutput;
        int logLevel;
        int timeout;
        FormatOutput formatOutput;

        Config(String[] args) {
            var logLevelStr = getValue(args, "--log-level");
            if (logLevelStr != null) {
                try {
                    logLevel = Integer.parseInt(logLevelStr);
                } catch (Exception e) {
                    printError("Unparseable log level");
                }
            }
            var timeoutStr = getValue(args, "--timeout");
            if (timeoutStr != null) {
                try {
                    timeout = Integer.parseInt(timeoutStr);
                } catch (Exception e) {
                    printError("Unparseable timeout");
                }
            } else {
                timeout = 5;
            }
            var formatOutputStr = getValue(args, "--format");
            if (formatOutputStr != null) {
                try {
                    formatOutput = FormatOutput.valueOf(formatOutputStr.toUpperCase());
                } catch (Exception e) {
                    printError("Unparseable format: " + formatOutputStr + "\nPossible values: " + Flow.flow(FormatOutput.values()).join(", "));
                }
            } else {
                formatOutput = FormatOutput.HUMAN;
            }
            var fileOutputStr = getValue(args, "--output");
            if (fileOutputStr != null) {
                try {
                    fileOutput = new File(fileOutputStr);
                } catch (Exception e) {
                    printError("Unparseable path file: " + fileOutputStr);
                }
            } else {
                fileOutput = new File("aether-client-state.bin");
            }
        }

        protected boolean getFlag(String[] args, String key) {
            for (var e : args) {
                if (e.equals(key)) {
                    return true;
                }
            }
            return false;
        }

        String getValue(String[] args, String key) {
            for (int i = 0; i < args.length; i++) {
                if (Objects.equals(args[i], key)) {
                    if (i == args.length - 1) {
                        printError("Bad argument value: void");
                    } else if (args[i + 1].startsWith("-")) {
                        printError("Bad argument value: " + args[i + 1]);
                    }
                    return args[i + 1];
                } else if (args[i].startsWith(key + "=")) {
                    var res = args[i].split("=")[1];
                    if (res.isBlank()) return null;
                }
            }
            return null;
        }
    }

    private static class ConfigCreateGroup extends Config {
        File clientState;
        boolean stdIn;
        UUID owner;

        public ConfigCreateGroup(String[] args) {
            super(args);
            var clientStateStr = getValue(args, "--state-bin");
            if (clientStateStr != null) {
                try {
                    clientState = new File(clientStateStr);
                    if (!clientState.exists()) {
                        printError("Client state is not specified --state-bin\ndefault value: aether-client-state.bin");
                    }
                } catch (Exception e) {
                    printError("Unparseable path file: " + clientStateStr);
                }
            } else {
                clientState = new File("aether-client-state.bin");
            }
            var ownerStr = getValue(args, "--owner");
            if (ownerStr != null) {
                try {
                    owner = UUID.fromString(ownerStr);
                } catch (Exception e) {
                    printError("Unparseable UUID: " + ownerStr);
                }
            }
            stdIn = getFlag(args, "--stdIn");
        }
    }

    private static class ConfigShow extends Config {
        File clientState;

        public ConfigShow(String[] args) {
            super(args);
            var clientStateStr = getValue(args, "--state-bin");
            if (clientStateStr != null) {
                try {
                    clientState = new File(clientStateStr);
                    if (!clientState.exists()) {
                        printError("Client state is not specified --state-bin\ndefault value: aether-client-state.bin");
                    }
                } catch (Exception e) {
                    printError("Unparseable path file: " + clientStateStr);
                }
            } else {
                clientState = new File("aether-client-state.bin");
            }
        }
    }

    private static class ConfigGetMessages extends Config {
        File clientState;

        public ConfigGetMessages(String[] args) {
            super(args);
            var clientStateStr = getValue(args, "--state-bin");
            if (clientStateStr != null) {
                try {
                    clientState = new File(clientStateStr);
                    if (!clientState.exists()) {
                        printError("Client state is not specified --state-bin\ndefault value: aether-client-state.bin");
                    }
                } catch (Exception e) {
                    printError("Unparseable path file: " + clientStateStr);
                }
            } else {
                clientState = new File("aether-client-state.bin");
            }
        }
    }

    private static class ConfigSendMessage extends Config {
        File clientState;
        String sendText;
        File sendFile;
        UUID receiver;

        public ConfigSendMessage(String[] args) {
            super(args);
            var clientStateStr = getValue(args, "--state-bin");
            if (clientStateStr != null) {
                try {
                    clientState = new File(clientStateStr);
                    if (!clientState.exists()) {
                        printError("Client state is not specified --state-file-bin\ndefault value: aether-client-state.bin");
                    }
                } catch (Exception e) {
                    printError("Unparseable path file: " + clientStateStr);
                }
            } else {
                clientState = new File("aether-client-state.bin");
            }
            var sendTextStr = getValue(args, "--send-text");
            if (sendTextStr != null) {
                sendText = sendTextStr;
            }
            var sendFileStr = getValue(args, "--send-file");
            if (sendFileStr != null) {
                try {
                    sendFile = new File(sendFileStr);
                    if (!sendFile.exists()) {
                        printError("File is not found: " + sendFile.getAbsolutePath());
                    }
                } catch (Exception e) {
                    printError("Unparseable path file: " + sendFileStr);
                }
            }
            var pStr = getValue(args, "--receiver");
            if (pStr == null) {
                printError("receiver is not specified --receiver");
                return;
            }
            try {
                receiver = UUID.fromString(pStr);
            } catch (Exception e) {
                printError("Unparseable uuid: " + pStr);
            }
        }
    }

    private static class ConfigCreateClient extends Config {
        CryptoLib cryptoLib;
        UUID parentUid;
        URI registrationURI;

        ConfigCreateClient(String[] args) {
            super(args);
            var pStr = getValue(args, "--parent");
            if (pStr != null) {
                if (pStr.equals("test")) {
                    parentUid = StandardUUIDs.TEST_UID;
                } else if (pStr.equals("anonymous")) {
                    parentUid = StandardUUIDs.ANONYMOUS_UID;
                } else {
                    try {
                        parentUid = UUID.fromString(pStr);
                    } catch (Exception e) {
                        printError("Unparseable uuid");
                    }
                }
            } else {
                parentUid = StandardUUIDs.TEST_UID;
            }
            var regUriStr = getValue(args, "--reg-uri");
            if (regUriStr != null) {
                if (regUriStr.equals("dev")) {
                    registrationURI = URI.create("tcp://reg-dev.aethernet.io:9010");
                } else {
                    try {
                        registrationURI = URI.create(regUriStr);
                    } catch (Exception e) {
                        printError("Unparseable registration uri");
                    }
                }
            } else {
                registrationURI = URI.create("tcp://registration.aethernet.io:9010");
            }
            var cryptoLibStr = getValue(args, "--crypto-lib");
            if (cryptoLibStr != null) {
                try {
                    cryptoLib = CryptoLib.valueOf(cryptoLibStr);
                } catch (Exception e) {
                    printError("Unparseable crypto lib");
                }
            } else {
                cryptoLib = CryptoLib.SODIUM;
            }
            var fileOutputStr = getValue(args, "--output");
            if (fileOutputStr != null) {
                try {
                    fileOutput = new File(fileOutputStr);
                } catch (Exception e) {
                    printError("Unparseable path file: " + fileOutputStr);
                }
            } else {
                fileOutput = new File("aether-client-state.bin");
            }

        }

    }
}