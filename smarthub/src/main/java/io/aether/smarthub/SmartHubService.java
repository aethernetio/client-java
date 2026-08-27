package io.aether.smarthub;

import io.aether.StandardUUIDs;
import io.aether.api.smarthub.*;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInFile;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.logger.LNode;
import io.aether.logger.Log;
import io.aether.logger.LogFilter;
import io.aether.net.fastMeta.MetaContext;
import io.aether.utils.futures.AFuture;
import org.h2.jdbcx.JdbcConnectionPool;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class SmartHubService {
    private static final String TAG = "SmartHub";
    private static final String DEFAULT_DB_PATH = "smarthub-data/smarthub";
    private static final String STATE_PATH = "smarthub-data/client.bin";
    private final Map<UUID, DeviceSession> devices = new ConcurrentHashMap<>();
    private final Set<UUID> knownDevices = ConcurrentHashMap.newKeySet();
    private final AFuture deviceRegisteredFuture = AFuture.make();

    private JdbcConnectionPool connectionPool;
    private io.aether.cloud.client.AetherCloudClient client;
    private io.aether.cloud.client.ClientState clientState;
    private final String dbPath;


    public SmartHubService(ClientStateInMemory serviceState) {
        this(serviceState, DEFAULT_DB_PATH);
    }

    SmartHubService(
            ClientStateInMemory serviceState,
            String dbPath) {

        this.dbPath = dbPath;
        setClientState(serviceState);
    }


    public void setClientState(io.aether.cloud.client.ClientState state) {
        this.clientState = state;
    }

    public io.aether.cloud.client.AetherCloudClient getClient() {
        return client;
    }

    public org.h2.jdbcx.JdbcConnectionPool getConnectionPool() {
        return connectionPool;
    }


    public AFuture start() throws Exception {
        try (var ctx = LNode.of(Log.SYSTEM_COMPONENT, TAG).context()) {
            Log.info("Starting SmartHub Service...");

            initDatabase();
            loadKnownDevicesFromDb();

            if (clientState == null) {
                throw new IllegalStateException("SmartHub client state is not configured");
            }

            client = new AetherCloudClient(clientState, "SmartHub");

            client.onClientStream(node -> {
                UUID peerUid = node.getConsumerUUID();

                Log.info(
                        "SmartHub: client stream opened",
                        "peerUid", peerUid);

                node.toApiR(
                        SmartHomeHubRegistryApi.META,
                        rootCtx -> createHubRegistryApi(rootCtx, peerUid));
            });

            client.startFuture.to(() -> {
                Log.info("SmartHub started", "uid", client.getUid());
                System.out.println("started with UUID:" + client.getUid());
            });
        }

        return client.startFuture;
    }



    private void initDatabase() throws SQLException {
        try {
            java.nio.file.Path dbFile =
                    Paths.get(dbPath);

            java.nio.file.Path parent =
                    dbFile.getParent();

            if (parent != null) {
                java.nio.file.Files.createDirectories(parent);
            }
        } catch (java.io.IOException e) {
            throw new SQLException(
                    "Failed to create database directory",
                    e);
        }

        String url =
                "jdbc:h2:./"
                        + dbPath
                        + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";

        connectionPool =
                JdbcConnectionPool.create(
                        url,
                        "sa",
                        "");

        connectionPool.setMaxConnections(10);

        try (Connection conn = connectionPool.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET MODE PostgreSQL");
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS devices (
                            UID UUID PRIMARY KEY,
                            NAME VARCHAR(255),
                            TYPE VARCHAR(50),
                            last_seen TIMESTAMP
                        );
                        CREATE TABLE IF NOT EXISTS device_states (
                            id SERIAL PRIMARY KEY,
                            DEVICE_UID UUID,
                            STATE_VALUE SMALLINT,
                            STATE_TIME SMALLINT,
                            STATE_TIMESTAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (DEVICE_UID) REFERENCES devices(UID)
                        );

                    """);
            stmt.execute("""
                    CREATE INDEX IF NOT EXISTS idx_device_states_uid_ts
                    ON device_states(DEVICE_UID, STATE_TIMESTAMP)
                    """);

        }
    }


    private SmartHomeHubRegistryApi createHubRegistryApi(MetaContext rootCtx, UUID peerUid) {
        Log.info("SmartHub: Registry API starting...", "peerUid", peerUid);

        return new SmartHomeHubRegistryApi() {
            @Override
            public void device(DeviceStream stream) {
                Log.info("SmartHub: device stream received", "deviceUid", peerUid);

                stream.asIn()
                        .keys(ctx -> (SmartHomeDeviceApi) value -> {
                            long now = System.currentTimeMillis();
                            boolean isNew = knownDevices.add(peerUid);

                            if (isNew) {
                                try (Connection conn = connectionPool.getConnection();
                                     PreparedStatement stmt = conn.prepareStatement(
                                             "INSERT INTO devices (UID, NAME, TYPE, last_seen) VALUES (?, ?, ?, ?)")) {
                                    stmt.setObject(1, peerUid);
                                    stmt.setString(2, "Emulator");
                                    stmt.setString(3, "TemperatureSensor");
                                    stmt.setTimestamp(4, new Timestamp(now));
                                    stmt.executeUpdate();

                                    Log.info(
                                            "Device registered in DB",
                                            "deviceUid", peerUid);
                                } catch (Exception e) {
                                    knownDevices.remove(peerUid);
                                    Log.error(
                                            "Failed to register device in DB: " + peerUid,
                                            e);
                                    return;
                                }
                            } else {
                                try (Connection conn = connectionPool.getConnection();
                                     PreparedStatement stmt = conn.prepareStatement(
                                             "UPDATE devices SET last_seen = ? WHERE UID = ?")) {
                                    stmt.setTimestamp(1, new Timestamp(now));
                                    stmt.setObject(2, peerUid);
                                    stmt.executeUpdate();
                                } catch (Exception e) {
                                    Log.error(
                                            "Failed to update device last_seen: " + peerUid,
                                            e);
                                }
                            }

                            try (Connection conn = connectionPool.getConnection();
                                 PreparedStatement stmt = conn.prepareStatement(
                                         "INSERT INTO device_states (DEVICE_UID, STATE_VALUE, STATE_TIME, STATE_TIMESTAMP) VALUES (?, ?, ?, ?)")) {
                                stmt.setObject(1, peerUid);
                                stmt.setShort(2, value);
                                stmt.setObject(3, null);
                                stmt.setTimestamp(4, new Timestamp(now));
                                stmt.executeUpdate();

                                deviceRegisteredFuture.tryDone();

                                Log.info(
                                        "Inserted device state",
                                        "deviceUid", peerUid,
                                        "value", value);
                            } catch (Exception e) {
                                Log.error(
                                        "SmartHub SQL error for device " + peerUid,
                                        e);
                            }
                        }, peerUid)
                        .accept();

                Log.info(
                        "SmartHub: device stream accepted",
                        "deviceUid", peerUid);
            }

            @Override
            public void gui(GuiStream stream) {
                Log.info(
                        "SmartHub: gui stream received",
                        "guiUid", peerUid);

                stream.asIn()
                        .keys(
                                ctx -> new MySmartHomeGuiApi(
                                        ctx.makeRemote(SmartHomeClientGuiApi.META)),
                                peerUid)
                        .onFlushData(data -> rootCtx.sendToRemote(data))
                        .accept();

                Log.info(
                        "SmartHub: gui stream accepted",
                        "guiUid", peerUid);
            }
        };
    }


    public AFuture getDeviceRegisteredFuture() {
        return deviceRegisteredFuture;
    }


    private void loadKnownDevicesFromDb() {
        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT UID FROM devices");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                knownDevices.add((UUID) rs.getObject(1));
            }

            Log.info(
                    "Loaded known devices",
                    "count", knownDevices.size());
        } catch (Exception e) {
            Log.error("Failed to load known devices from DB", e);
        }
    }


    public void stop() {
        if (client != null) client.destroy(true);
        if (connectionPool != null) connectionPool.dispose();
    }


    public static void main(String[] args) {
        Log.printPlainConsole(new LogFilter());

        ClientStateInFile state = new ClientStateInFile(
                StandardUUIDs.TEST_UID,
                List.of(URI.create("tcp://registration.aethernet.io:9010")),
                new java.io.File(STATE_PATH));

        SmartHubService service = new SmartHubService(state);

        try {
            service.start();

            Runtime.getRuntime().addShutdownHook(
                    new Thread(service::stop));

            Thread.currentThread().join();
        } catch (Exception e) {
            Log.error("Failed to start service", e);
            System.exit(1);
        }
    }

    public static void clearDatabaseFiles(String basePath) {
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(basePath + ".mv.db"));
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(basePath + ".trace.db"));
            Log.info("Cleared database files", "basePath", basePath);
        } catch (Exception e) {
            Log.warn("Failed to delete database files", "basePath", basePath, "error", e.getMessage());
        }
    }

    private static class DeviceSession {
        final UUID deviceUid;
        SensorRecord[] lastState;
        long lastSeen;

        DeviceSession(UUID deviceUid) {
            this.deviceUid = deviceUid;
            this.lastSeen = System.currentTimeMillis();
        }
    }


    private class MySmartHomeGuiApi implements SmartHomeGuiApi {
        private final SmartHomeClientGuiApiRemote guiRemote;

        public MySmartHomeGuiApi(SmartHomeClientGuiApiRemote guiRemote) {
            this.guiRemote = guiRemote;
        }

        @Override
        public void getDevices() {
            Log.info("getDevices called for gui", "currentKnownCount", knownDevices.size());
            UUID[] devicesArray = knownDevices.toArray(new UUID[0]);
            Log.info("getDevices returning", "count", devicesArray.length);
            for (UUID u : devicesArray) Log.info("device", "uid", u);
            guiRemote.onGetDevicesResult(devicesArray);
        }


        @Override
        public void requestDeviceHistory(UUID d, long c) {
            int limit =
                    (int) Math.max(
                            0L,
                            Math.min(
                                    c,
                                    100L
                            )
                    );

            List<SensorRecord> records =
                    new ArrayList<>();

            try (Connection conn =
                         connectionPool.getConnection();
                 PreparedStatement stmt =
                         conn.prepareStatement(
                                 "SELECT STATE_VALUE, STATE_TIME "
                                         + "FROM device_states "
                                         + "WHERE DEVICE_UID = ? "
                                         + "ORDER BY STATE_TIMESTAMP DESC "
                                         + "LIMIT ?")) {

                stmt.setObject(
                        1,
                        d
                );

                stmt.setInt(
                        2,
                        limit
                );

                try (ResultSet rs =
                             stmt.executeQuery()) {

                    while (rs.next()) {
                        records.add(
                                new SensorRecord(
                                        (byte) rs.getShort(1),
                                        (byte) rs.getShort(2)
                                )
                        );
                    }
                }
            } catch (Exception e) {
                Log.error(e);
            }

            guiRemote.onRequestHistoryResult(
                    d,
                    records.toArray(
                            new SensorRecord[0]
                    )
            );
        }
    }
}