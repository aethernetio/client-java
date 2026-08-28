
package io.aether.smarthub;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.ClientStateInFile;
import io.aether.utils.futures.AFuture;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartHubPublicSmokeTest {

    private static final URI REG_URI =
            URI.create("tcp://registration.aethernet.io:9010");

    private static final long TIMEOUT_MS =
            60_000L;

    @Test
    void publicRegistrationRoutesStateAndPreservesDeviceUidAcrossRestart()
            throws Exception {

        File smokeDir =
                new File("build/public-smoke");

        assertTrue(
                smokeDir.exists() || smokeDir.mkdirs(),
                "Cannot create public smoke directory");

        String smokeDbPath =
                new File(
                        smokeDir,
                        "smarthub")
                        .getPath();

        SmartHubService.clearDatabaseFiles(
                smokeDbPath);


        File serviceStateFile =
                new File(
                        smokeDir,
                        "service.bin");

        File deviceStateFile =
                new File(
                        smokeDir,
                        "device.bin");

        serviceStateFile.delete();
        deviceStateFile.delete();

        ClientStateInFile serviceState =
                new ClientStateInFile(
                        StandardUUIDs.TEST_UID,
                        List.of(REG_URI),
                        serviceStateFile);


        SmartHubService service =
                new SmartHubService(
                        serviceState,
                        smokeDbPath);


        SmartDeviceEmulator first = null;
        SmartDeviceEmulator second = null;
        UUID deviceUid = null;

        try {
            await(
                    service.start(),
                    "SmartHub public registration");

            UUID serviceUid =
                    service.getClient().getUid();

            assertNotNull(
                    serviceUid,
                    "SmartHub service UID was not assigned");

            first =
                    new SmartDeviceEmulator(
                            serviceUid,
                            deviceStateFile.getPath());

            first.start(
                    REG_URI.toString());

            await(
                    first.getReady(),
                    "First emulator public registration");

            await(
                    service.getDeviceRegisteredFuture(),
                    "First device state delivery");

            deviceUid =
                    first.getDeviceUid();

            assertNotNull(
                    deviceUid,
                    "First emulator UID was not assigned");

            long statesBeforeRestart =
                    countStates(
                            service,
                            deviceUid);

            assertTrue(
                    statesBeforeRestart >= 1,
                    "First emulator state is absent from DB");

            assertEquals(
                    1L,
                    countDevices(
                            service,
                            deviceUid),
                    "Device must have exactly one DB identity");

            first.stop();
            first = null;

            assertTrue(
                    deviceStateFile.isFile(),
                    "Emulator state file was not persisted");

            second =
                    new SmartDeviceEmulator(
                            serviceUid,
                            deviceStateFile.getPath());

            second.start(
                    REG_URI.toString());

            await(
                    second.getReady(),
                    "Restarted emulator public registration");

            assertEquals(
                    deviceUid,
                    second.getDeviceUid(),
                    "Emulator UID changed after restart");

            awaitStateCount(
                    service,
                    deviceUid,
                    statesBeforeRestart + 1);

            assertEquals(
                    1L,
                    countDevices(
                            service,
                            deviceUid),
                    "Restart created a duplicate device identity");
        } finally {
            if (second != null) {
                second.stop();
            }

            if (first != null) {
                first.stop();
            }

            if (deviceUid != null) {
                cleanupDevice(
                        service,
                        deviceUid);
            }

            service.stop();
            serviceState.destroy(true);

            serviceStateFile.delete();
            deviceStateFile.delete();
        }
    }


    @Test
    void publicGuiRoundTripReturnsDevicesAndHistory()
            throws Exception {

        File smokeDir =
                new File("build/public-smoke");

        assertTrue(
                smokeDir.exists() || smokeDir.mkdirs(),
                "Cannot create public smoke directory");

        String smokeDbPath =
                new File(
                        smokeDir,
                        "smarthub-gui")
                        .getPath();

        SmartHubService.clearDatabaseFiles(
                smokeDbPath);

        File serviceStateFile =
                new File(
                        smokeDir,
                        "gui-service.bin");

        File deviceStateFile =
                new File(
                        smokeDir,
                        "gui-device.bin");

        File guiStateFile =
                new File(
                        smokeDir,
                        "gui-client.bin");

        serviceStateFile.delete();
        deviceStateFile.delete();
        guiStateFile.delete();

        ClientStateInFile serviceState =
                new ClientStateInFile(
                        StandardUUIDs.TEST_UID,
                        List.of(REG_URI),
                        serviceStateFile);

        SmartHubService service =
                new SmartHubService(
                        serviceState,
                        smokeDbPath);

        SmartDeviceEmulator emulator = null;
        ClientStateInFile guiState = null;
        io.aether.cloud.client.AetherCloudClient guiClient = null;
        UUID deviceUid = null;


        AFuture devicesReceived = AFuture.make();
        AFuture historyReceived = AFuture.make();
        AFuture liveStateReceived = AFuture.make();


        java.util.concurrent.atomic.AtomicReference<UUID[]> devices =
                new java.util.concurrent.atomic.AtomicReference<>();


        java.util.concurrent.atomic.AtomicReference<io.aether.api.smarthub.SensorRecord[]> history =
                new java.util.concurrent.atomic.AtomicReference<>();

        java.util.concurrent.atomic.AtomicReference<io.aether.api.smarthub.SensorRecord[]> liveState =
                new java.util.concurrent.atomic.AtomicReference<>();


        try {
            await(
                    service.start(),
                    "SmartHub GUI smoke service registration");

            UUID serviceUid =
                    service.getClient().getUid();

            assertNotNull(
                    serviceUid,
                    "SmartHub service UID was not assigned");

            emulator =
                    new SmartDeviceEmulator(
                            serviceUid,
                            deviceStateFile.getPath());

            emulator.start(
                    REG_URI.toString());

            await(
                    emulator.getReady(),
                    "SmartHub GUI smoke emulator registration");

            await(
                    service.getDeviceRegisteredFuture(),
                    "SmartHub GUI smoke device state delivery");

            deviceUid =
                    emulator.getDeviceUid();

            assertNotNull(
                    deviceUid,
                    "SmartHub GUI smoke device UID was not assigned");

            UUID expectedDeviceUid =
                    deviceUid;

            guiState =
                    new ClientStateInFile(
                            serviceUid,
                            List.of(REG_URI),
                            guiStateFile);

            io.aether.api.smarthub.SmartHomeClientGuiApi localGuiApi =
                    new io.aether.api.smarthub.SmartHomeClientGuiApi() {
                        @Override

                        public void deviceStateUpdated(
                                UUID uid,
                                io.aether.api.smarthub.SensorRecord[] records) {

                            if (expectedDeviceUid.equals(uid)
                                    && records != null
                                    && records.length > 0) {

                                liveState.set(
                                        records);

                                liveStateReceived.done();
                            }
                        }


                        @Override
                        public void onGetDevicesResult(
                                UUID[] receivedDevices) {

                            devices.set(
                                    receivedDevices);

                            devicesReceived.done();
                        }

                        @Override
                        public void onRequestHistoryResult(
                                UUID uid,
                                io.aether.api.smarthub.SensorRecord[] records) {

                            if (expectedDeviceUid.equals(uid)) {
                                history.set(
                                        records);

                                historyReceived.done();
                            }
                        }
                    };

            guiClient =
                    io.aether.cloud.client.AetherCloudClient.asClient(
                            guiState,
                            "SmartHub-Gui-Public-Smoke",
                            io.aether.api.smarthub.SmartHomeClientGuiApi.META,
                            io.aether.api.smarthub.SmartHomeHubRegistryApi.META,
                            remoteHubApi -> {
                                io.aether.api.smarthub.SmartHomeGuiApiRemote guiApi =
                                        remoteHubApi.openGui(
                                                remoteGuiApi -> localGuiApi,
                                                data -> data);

                                guiApi.getDevices();

                                guiApi.requestDeviceHistory(
                                        expectedDeviceUid,
                                        10);

                                return localGuiApi;
                            });


            await(
                    guiClient.startFuture,
                    "SmartHub GUI client registration");

            await(
                    liveStateReceived,
                    "SmartHub live deviceStateUpdated callback");

            await(
                    devicesReceived,
                    "SmartHub getDevices callback");


            await(
                    historyReceived,
                    "SmartHub requestDeviceHistory callback");

            UUID[] receivedDevices =
                    devices.get();

            assertNotNull(
                    receivedDevices,
                    "SmartHub getDevices returned null");

            assertTrue(
                    java.util.Arrays.asList(receivedDevices)
                            .contains(expectedDeviceUid),
                    "SmartHub getDevices did not return emulator UID");


            io.aether.api.smarthub.SensorRecord[] receivedLiveState =
                    liveState.get();

            assertNotNull(
                    receivedLiveState,
                    "SmartHub live state callback returned null");

            assertTrue(
                    receivedLiveState.length > 0,
                    "SmartHub live state callback returned no records");

            io.aether.api.smarthub.SensorRecord[] receivedHistory =
                    history.get();


            assertNotNull(
                    receivedHistory,
                    "SmartHub history callback returned null");

            assertTrue(
                    receivedHistory.length > 0,
                    "SmartHub history callback returned no records");
        } finally {
            if (guiClient != null) {
                guiClient.destroy(true);
            }

            if (guiState != null) {
                guiState.destroy(true);
            }

            if (emulator != null) {
                emulator.stop();
            }

            if (deviceUid != null) {
                cleanupDevice(
                        service,
                        deviceUid);
            }

            service.stop();
            serviceState.destroy(true);

            serviceStateFile.delete();
            deviceStateFile.delete();
            guiStateFile.delete();

            SmartHubService.clearDatabaseFiles(
                    smokeDbPath);
        }
    }



    @Test
    void publicGuiConnectedBeforeEmulatorReceivesDeviceAndLiveState() throws Exception {
        File dir = new File("build/public-smoke");
        assertTrue(dir.exists() || dir.mkdirs(), "Cannot create public smoke directory");
        String db = new File(dir, "smarthub-gui-before-device").getPath();
        SmartHubService.clearDatabaseFiles(db);
        File serviceFile = new File(dir, "gui-before-device-service.bin");
        File deviceFile = new File(dir, "gui-before-device.bin");
        File guiFile = new File(dir, "gui-before-device-client.bin");
        serviceFile.delete(); deviceFile.delete(); guiFile.delete();

        ClientStateInFile serviceState =
                new ClientStateInFile(StandardUUIDs.TEST_UID, List.of(REG_URI), serviceFile);
        SmartHubService service = new SmartHubService(serviceState, db);
        SmartDeviceEmulator emulator = null;
        ClientStateInFile guiState = null;
        io.aether.cloud.client.AetherCloudClient guiClient = null;
        UUID deviceUid = null;

        AFuture emptyDevices = AFuture.make();
        AFuture discoveredDevice = AFuture.make();
        AFuture liveState = AFuture.make();
        var expectedUid = new java.util.concurrent.atomic.AtomicReference<UUID>();
        var devices = new java.util.concurrent.atomic.AtomicReference<UUID[]>();
        var lastLiveUid = new java.util.concurrent.atomic.AtomicReference<UUID>();
        var lastLiveState =
                new java.util.concurrent.atomic.AtomicReference<io.aether.api.smarthub.SensorRecord[]>();

        try {
            await(service.start(), "GUI-before-emulator service registration");
            UUID serviceUid = service.getClient().getUid();
            assertNotNull(serviceUid, "SmartHub service UID was not assigned");
            guiState = new ClientStateInFile(serviceUid, List.of(REG_URI), guiFile);

            io.aether.api.smarthub.SmartHomeClientGuiApi localGui =
                    new io.aether.api.smarthub.SmartHomeClientGuiApi() {
                        @Override
                        public void deviceStateUpdated(
                                UUID uid, io.aether.api.smarthub.SensorRecord[] records) {
                            lastLiveUid.set(uid); lastLiveState.set(records);
                            UUID expected = expectedUid.get();
                            if (expected != null && expected.equals(uid)
                                    && records != null && records.length > 0) liveState.tryDone();
                        }

                        @Override
                        public void onGetDevicesResult(UUID[] received) {
                            devices.set(received);
                            if (received != null && received.length == 0) emptyDevices.tryDone();
                            UUID expected = expectedUid.get();
                            if (expected != null && received != null
                                    && java.util.Arrays.asList(received).contains(expected))
                                discoveredDevice.tryDone();
                        }

                        @Override
                        public void onRequestHistoryResult(
                                UUID uid, io.aether.api.smarthub.SensorRecord[] records) {
                        }
                    };

            guiClient = io.aether.cloud.client.AetherCloudClient.asClient(
                    guiState, "SmartHub-Gui-Before-Emulator-Smoke",
                    io.aether.api.smarthub.SmartHomeClientGuiApi.META,
                    io.aether.api.smarthub.SmartHomeHubRegistryApi.META,
                    remoteHubApi -> {
                        remoteHubApi.openGui(remoteGuiApi -> localGui, data -> data).getDevices();
                        return localGui;
                    });

            await(guiClient.startFuture, "GUI-before-emulator client registration");
            await(emptyDevices, "Initial empty SmartHub device list");
            assertNotNull(devices.get(), "Initial getDevices returned null");
            assertEquals(0, devices.get().length, "Device list is not empty before emulator");

            emulator = new SmartDeviceEmulator(serviceUid, deviceFile.getPath());
            emulator.start(REG_URI.toString());
            await(emulator.getReady(), "GUI-before-emulator emulator registration");
            deviceUid = emulator.getDeviceUid();
            assertNotNull(deviceUid, "Emulator UID was not assigned");
            expectedUid.set(deviceUid);

            UUID[] current = devices.get();
            if (current != null && java.util.Arrays.asList(current).contains(deviceUid))
                discoveredDevice.tryDone();
            if (deviceUid.equals(lastLiveUid.get())
                    && lastLiveState.get() != null && lastLiveState.get().length > 0)
                liveState.tryDone();

            await(service.getDeviceRegisteredFuture(), "Device state after GUI connected first");
            await(discoveredDevice, "Automatic device list broadcast");
            await(liveState, "Live state broadcast");
        } finally {
            if (guiClient != null) guiClient.destroy(true);
            if (guiState != null) guiState.destroy(true);
            if (emulator != null) emulator.stop();
            if (deviceUid != null) cleanupDevice(service, deviceUid);
            service.stop(); serviceState.destroy(true);
            serviceFile.delete(); deviceFile.delete(); guiFile.delete();
            SmartHubService.clearDatabaseFiles(db);
        }
    }

    private static void await(
            AFuture future,
            String operation)
            throws Exception {

        long deadline =
                System.currentTimeMillis()
                        + TIMEOUT_MS;

        while (!future.isDone()
                && !future.isError()
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }

        if (future.isError()) {
            throw new AssertionError(
                    operation + " failed",
                    future.getError());
        }

        assertTrue(
                future.isDone(),
                operation
                        + " timed out after "
                        + TIMEOUT_MS
                        + " ms");
    }


    private static void awaitStateCount(
            SmartHubService service,
            UUID deviceUid,
            long expected)
            throws Exception {

        long deadline =
                System.currentTimeMillis()
                        + TIMEOUT_MS;

        long actual = 0;

        while (System.currentTimeMillis() < deadline) {
            actual =
                    countStates(
                            service,
                            deviceUid);

            if (actual >= expected) {
                return;
            }

            Thread.sleep(200);
        }

        assertTrue(
                actual >= expected,
                "Restarted emulator did not produce another state; expected at least "
                        + expected
                        + ", actual "
                        + actual);
    }

    private static long countStates(
            SmartHubService service,
            UUID deviceUid)
            throws Exception {

        try (Connection connection =
                     service
                             .getConnectionPool()
                             .getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(
                             "SELECT COUNT(*) FROM device_states WHERE DEVICE_UID = ?")) {

            statement.setObject(
                    1,
                    deviceUid);

            try (ResultSet result =
                         statement.executeQuery()) {

                result.next();
                return result.getLong(1);
            }
        }
    }

    private static long countDevices(
            SmartHubService service,
            UUID deviceUid)
            throws Exception {

        try (Connection connection =
                     service
                             .getConnectionPool()
                             .getConnection();

             PreparedStatement statement =
                     connection.prepareStatement(
                             "SELECT COUNT(*) FROM devices WHERE UID = ?")) {

            statement.setObject(
                    1,
                    deviceUid);

            try (ResultSet result =
                         statement.executeQuery()) {

                result.next();
                return result.getLong(1);
            }
        }
    }

    private static void cleanupDevice(
            SmartHubService service,
            UUID deviceUid) {

        try (Connection connection =
                     service
                             .getConnectionPool()
                             .getConnection()) {

            try (PreparedStatement states =
                         connection.prepareStatement(
                                 "DELETE FROM device_states WHERE DEVICE_UID = ?")) {

                states.setObject(
                        1,
                        deviceUid);

                states.executeUpdate();
            }

            try (PreparedStatement device =
                         connection.prepareStatement(
                                 "DELETE FROM devices WHERE UID = ?")) {

                device.setObject(
                        1,
                        deviceUid);

                device.executeUpdate();
            }
        } catch (Exception ignored) {
        }
    }
}