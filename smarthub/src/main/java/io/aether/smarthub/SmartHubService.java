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
    private static final String DB_PATH = "smarthub-data/smarthub";
    private static final String STATE_PATH = "smarthub-data/client.bin";
    private final Map<UUID, DeviceSession> devices = new ConcurrentHashMap<>();
    private final Set<UUID> knownDevices = ConcurrentHashMap.newKeySet();
    private final AFuture deviceRegisteredFuture = AFuture.make();

    private JdbcConnectionPool connectionPool;
    private io.aether.cloud.client.AetherCloudClient client;
    private io.aether.cloud.client.ClientState clientState;

    public SmartHubService(ClientStateInMemory serviceState) {
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

            java.io.File stateFile = new java.io.File(STATE_PATH);
            ClientStateInFile state = new ClientStateInFile(
                    clientState != null ? clientState.getParentUid() : StandardUUIDs.TEST_UID,
                    List.of(URI.create("tcp://registration.aethernet.io:9010")),
                    stateFile);
            client = AetherCloudClient.asServer(state, "SmartHub",
                    SmartHomeHubRegistryApi.META,
                    this::createHubRegistryApi);
        }
        return client.startFuture;
    }


    private void initDatabase() throws SQLException {
        try {
            java.nio.file.Files.createDirectories(Paths.get("smarthub-data"));
        } catch (java.io.IOException e) {
            throw new SQLException("Failed to create database directory", e);
        }


        String url = "jdbc:h2:./" + DB_PATH + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE";
        connectionPool = JdbcConnectionPool.create(url, "sa", "");

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
        }
    }


    private SmartHomeHubRegistryApi createHubRegistryApi(MetaContext rootCtx) {
        Log.info("SmartHub: Registry API starting...");
        return new SmartHomeHubRegistryApi() {

            SmartHomeClientDeviceApi api2DeviceRemote;
            SmartHomeClientGuiApiRemote api2GuiRemote;
            SmartHomeDeviceApi api2DeviceLocal;
            SmartHomeGuiApi api2GuiLocal;

            @Override
            public void device(DeviceStream stream) {
                Log.info("SmartHub: device method called");
                if (api2DeviceRemote == null) {

                    api2DeviceLocal = (value) -> {
                        Log.info("api2DeviceLocal called", "value", value);
                        boolean isNew = knownDevices.add(UUID.randomUUID());//FIXME: use correct uid
                        if (isNew) {
                            Log.info("New device detected");
                            try (Connection conn = connectionPool.getConnection();
                                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO devices (UID, NAME, TYPE, last_seen) VALUES (?, ?, ?, ?)")) {
                                stmt.setObject(1, UUID.randomUUID());//FIXME: use correct uid
                                stmt.setString(2, "Emulator");
                                stmt.setString(3, "TemperatureSensor");
                                stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                                stmt.executeUpdate();
                                Log.info("Device registered in DB");
                            } catch (Exception e) {
                                Log.error("Failed to register device in DB", e);
                            }

                            deviceRegisteredFuture.tryDone();
                        }
                        try (Connection conn = connectionPool.getConnection();
                             PreparedStatement stmt = conn.prepareStatement("INSERT INTO device_states (DEVICE_UID, STATE_VALUE, STATE_TIME, STATE_TIMESTAMP) VALUES (?, ?, ?, ?)")) {
                            stmt.setObject(1, UUID.randomUUID());//FIXME: use correct uid
                            stmt.setShort(2, value);
                            stmt.setNull(3, 0);//TODO
                            stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                            stmt.addBatch();
                            stmt.executeBatch();
                            Log.info("Inserted device states", "value", value);

                        } catch (Exception e) {
                            Log.error("SmartHub: SQL Error", e);
                        }
                    };

                }
                Log.info("SmartHub: Device stream connected");
                stream.asIn().accept();
                Log.info("SmartHub: stream.accept finished successfully");
            }


            public void gui(GuiStream stream) {
                Log.info("SmartHub: gui method entered");
                stream.asIn()
                        .keys(c -> new MySmartHomeGuiApi(stream.asIn().remoteApi()))
                        .accept();
            }

        };
    }


    public AFuture getDeviceRegisteredFuture() {
        return deviceRegisteredFuture;
    }


    private void loadKnownDevicesFromDb() {
        try (Connection conn = connectionPool.getConnection();
             Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT DISTINCT DEVICE_UID FROM device_states")) {
            while (rs.next()) {
                Object obj = rs.getObject("DEVICE_UID");
                if (obj instanceof UUID uid) knownDevices.add(uid);
                else if (obj instanceof String str) knownDevices.add(UUID.fromString(str));
            }
            Log.info("Loaded known devices from DB", "count", knownDevices.size());
        } catch (Exception e) {
            Log.error("Failed to load known devices", e);
        }
    }


    public void stop() {
        if (client != null) client.destroy(true);
        if (connectionPool != null) connectionPool.dispose();
    }


    public static void main(String[] args) {
        Log.printPlainConsole(new LogFilter());
        // Используем ClientStateInFile, который автоматически сохраняет состояние
        ClientStateInFile state = new ClientStateInFile(StandardUUIDs.TEST_UID,
                List.of(URI.create("tcp://registration.aethernet.io:9010")),
                new File("state.bin"));
        SmartHubService service = new SmartHubService(state);
        try {
            service.start();
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
            List<SensorRecord> records = new ArrayList<>();
            try (Connection conn = connectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT STATE_VALUE, STATE_TIME FROM device_states WHERE DEVICE_UID = ? ORDER BY STATE_TIMESTAMP DESC LIMIT ?")) {
                stmt.setObject(1, d);
                stmt.setLong(2, c);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        records.add(new SensorRecord((byte) rs.getShort(1), (byte) rs.getShort(2)));
                    }
                }
            } catch (Exception e) {
                Log.error(e);
            }
            guiRemote.onRequestHistoryResult(d, records.toArray(new SensorRecord[0]));
        }
    }
}