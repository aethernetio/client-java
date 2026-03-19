package io.aether.smarthub;

import io.aether.api.smarthub.*;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.logger.LNode;
import io.aether.logger.Log;
import io.aether.logger.LogFilter;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;
import org.h2.jdbcx.JdbcConnectionPool;

import java.io.File;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SmartHubService {
    private static final String TAG = "SmartHub";
    private static final String DB_PATH = "smarthub-data/smarthub";
    private static final String STATE_PATH = "smarthub-data/client.bin";
    private final Map<UUID, DeviceSession> devices = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> deviceSubscriptions = new ConcurrentHashMap<>();
    private final Map<UUID, SmartHomeClientGuiApiRemote> guiContexts = new ConcurrentHashMap<>();
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

            java.io.File stateFile = new java.io.File(STATE_PATH);
            if (stateFile.exists() && clientState == null) {
                Log.info("Loading identity from " + STATE_PATH);
                clientState = io.aether.cloud.client.ClientStateInMemory.load(stateFile);
            }

            client = (clientState != null)
                    ? new io.aether.cloud.client.AetherCloudClient(clientState, "SmartHub")
                    : new io.aether.cloud.client.AetherCloudClient();

            client.connect().to(() -> {
                if (client.getClientState() instanceof io.aether.cloud.client.ClientStateInMemory inMemoryState) {
                    try {
                        client.forceUpdateStateFromCache();
                        inMemoryState.save(stateFile);
                    } catch (Exception e) {
                        Log.error(e);
                    }
                }
                registerApis();
                Log.info("SmartHub Service started with UUID: " + client.getUid());
            });
        }
        return client.startFuture;
    }

    private void initDatabase() throws SQLException {
        try {
            java.nio.file.Files.createDirectories(Paths.get("smarthub-data"));
        } catch (java.io.IOException e) {
            throw new SQLException("Failed to create database directory", e);
        }

        String url = "jdbc:h2:./" + DB_PATH + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
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
                        CREATE TABLE IF NOT EXISTS subscriptions (
                            CLIENT_UID UUID,
                            DEVICE_UID UUID,
                            PRIMARY KEY (CLIENT_UID, DEVICE_UID)
                        );
                    """);
        }
    }

    private void registerApis() {
        Log.info("SmartHub: Registry API starting...");
        client.onClientStream(node -> {
            // Создаем реализацию API отдельно
            SmartHomeHubRegistryApi registryImpl = new SmartHomeHubRegistryApi() {
                @Override
                public void device(DeviceStream stream) {
                    UUID devUid = node.getConsumerUUID();
                    Log.info("SmartHub: Device stream connected", "uid", devUid);
                    // Получаем контекст из текущего узла для этого стрима
                    stream.accept(node.toApi(SmartHomeHubRegistryApi.META, this), new SmartHomeDeviceApi() {
                        @Override
                        public ARFuture<Boolean> reportState(UUID deviceUid, DeviceRecord[] value) {
                            ensureDeviceExists(deviceUid);
                            try (Connection conn = connectionPool.getConnection();
                                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO device_states (DEVICE_UID, STATE_VALUE, STATE_TIME, STATE_TIMESTAMP) VALUES (?, ?, ?, ?)")) {
                                for (DeviceRecord record : value) {
                                    stmt.setObject(1, deviceUid);
                                    stmt.setShort(2, record.getValue());
                                    stmt.setShort(3, record.getTime());
                                    stmt.setTimestamp(4, new java.sql.Timestamp(System.currentTimeMillis()));
                                    stmt.addBatch();
                                }
                                stmt.executeBatch();
                                Optional.ofNullable(deviceSubscriptions.get(deviceUid)).ifPresent(subs -> subs.forEach(guiUid -> {
                                    var remote = guiContexts.get(guiUid);
                                    if (remote != null) remote.deviceStateUpdated(deviceUid, value);
                                }));
                                return ARFuture.TRUE;
                            } catch (Exception e) {
                                Log.error("SmartHub: SQL Error", e);
                                return ARFuture.FALSE;
                            }
                        }
                    });
                }

                @Override
                public void gui(GuiStream stream) {
                    UUID guiUid = node.getConsumerUUID();
                    Log.info("SmartHub: GUI stream connected", "uid", guiUid);
                    var regCtx = node.toApi(SmartHomeHubRegistryApi.META, this);
                    stream.accept(regCtx, new SmartHomeGuiApi() {
                        @Override
                        public ARFuture<UUID[]> getDevices() {
                            List<UUID> list = new ArrayList<>();
                            try (Connection conn = connectionPool.getConnection();
                                 Statement s = conn.createStatement();
                                 ResultSet rs = s.executeQuery("SELECT UID FROM devices ORDER BY last_seen DESC")) {
                                while (rs.next()) {
                                    Object uid = rs.getObject("UID");
                                    if (uid instanceof UUID) list.add((UUID) uid);
                                    else if (uid instanceof String) list.add(UUID.fromString((String) uid));
                                }
                            } catch (Exception e) {
                                Log.error(e);
                            }
                            return ARFuture.of(list.toArray(new UUID[0]));
                        }

                        @Override
                        public ARFuture<Boolean> subscribeToDevice(UUID d) {
                            deviceSubscriptions.computeIfAbsent(d, k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(guiUid);
                            return ARFuture.TRUE;
                        }

                        @Override
                        public ARFuture<Boolean> unsubscribeFromDevice(UUID d) {
                            Optional.ofNullable(deviceSubscriptions.get(d)).ifPresent(s -> s.remove(guiUid));
                            return ARFuture.TRUE;
                        }

                        @Override
                        public ARFuture<DeviceRecord[]> requestDeviceHistory(UUID d, long c) {
                            return ARFuture.of(new DeviceRecord[0]);
                        }
                    });
                    guiContexts.put(guiUid, SmartHomeClientGuiApi.META.makeRemote(regCtx));
                }
            };
            // Привязываем реализацию к узлу
            node.toApi(SmartHomeHubRegistryApi.META, registryImpl);
        });
    }

    private void ensureDeviceExists(UUID deviceUid) {
        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO devices (UID, last_seen) VALUES (?, ?) ON CONFLICT (UID) DO UPDATE SET last_seen = ?")) {
            java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
            stmt.setObject(1, deviceUid);
            stmt.setTimestamp(2, now);
            stmt.setTimestamp(3, now);
            stmt.executeUpdate();
        } catch (SQLException e) {
            Log.error("SmartHub: DB Error", e);
        }
    }

    public void stop() {
        guiContexts.clear();
        if (client != null) client.destroy(true);
        if (connectionPool != null) connectionPool.dispose();
    }

    public static void main(String[] args) {
        Log.printPlainConsole(new LogFilter());
        SmartHubService service = new SmartHubService(ClientStateInMemory.load(new File("state.bin")));
        try {
            service.start();
            Thread.currentThread().join();
        } catch (Exception e) {
            Log.error("Failed to start service", Log.EXCEPTION_STR, e);
            System.exit(1);
        }
    }

    private static class DeviceSession {
        final UUID deviceUid;
        DeviceRecord[] lastState;
        long lastSeen;

        DeviceSession(UUID deviceUid) {
            this.deviceUid = deviceUid;
            this.lastSeen = System.currentTimeMillis();
        }
    }
}