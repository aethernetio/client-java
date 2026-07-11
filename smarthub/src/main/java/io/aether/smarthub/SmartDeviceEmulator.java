package io.aether.smarthub;

import io.aether.api.smarthub.SmartHomeClientDeviceApi;
import io.aether.api.smarthub.SmartHomeHubRegistryApi;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.logger.Log;
import io.aether.utils.futures.AFuture;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class SmartDeviceEmulator {
    private final UUID serviceUid;
    private final String statePath;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AFuture ready = AFuture.make();
    private UUID deviceUid;
    private AetherCloudClient client;

    public SmartDeviceEmulator(UUID serviceUid) {
        this.serviceUid = serviceUid;
        this.statePath = "smarthub-data/device-" + serviceUid + ".bin";
    }

    public UUID getDeviceUid() {
        return deviceUid;
    }

    public AFuture getReady() {
        return ready;
    }


    public void start(String regUri) throws Exception {
        Log.info("SmartDeviceEmulator.start() called", "regUri", regUri, "serviceUid", serviceUid);
        URI uri = URI.create(regUri);
        client = AetherCloudClient.asClient(serviceUid, uri, "Emulator-" + serviceUid,
                SmartHomeClientDeviceApi.META,
                SmartHomeHubRegistryApi.META,
                remoteHubApi -> {
                    deviceUid = client.getUid();
                    Log.info("Device Emulator connected", "uid", deviceUid);
                    remoteHubApi.openDevice(remoteDeviceApi -> {
                        Log.info("Starting scheduled temperature reporting", "intervalSec", 1);
                        scheduler.scheduleAtFixedRate(() -> {
                            int tempCelsius = 22 + (int) (Math.random() * 5);
                            byte rawTemp = (byte) ((tempCelsius + 30) * 3);
                            Log.info("Sending temperature", "celsius", tempCelsius, "raw", (rawTemp & 0xFF), "deviceUid", deviceUid);
                            remoteDeviceApi.reportState(rawTemp);
                        }, 0, 1, TimeUnit.SECONDS);
                        return SmartHomeClientDeviceApi.EMPTY;
                    }, d -> d);
                    ready.done();
                    return SmartHomeClientDeviceApi.EMPTY;
                });
    }


    public void stop() {
        scheduler.shutdown();
        if (client != null) client.destroy(true);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: SmartDeviceEmulator <serviceUid> [regUri]");
            System.exit(1);
        }
        UUID serviceUid = UUID.fromString(args[0]);
        System.out.println("Emulator args: " + java.util.Arrays.toString(args));
        String regUri = args.length > 1 ? args[1] : "tcp://registration.aethernet.io:9010";
        SmartDeviceEmulator emulator = new SmartDeviceEmulator(serviceUid);
        emulator.start(regUri);
        Runtime.getRuntime().addShutdownHook(new Thread(emulator::stop));
        // Wait until ready
        while (!emulator.getReady().isDone() && !emulator.getReady().isError()) {
            Thread.sleep(100);
        }
        if (emulator.getReady().isError()) {
            throw new RuntimeException(emulator.getReady().getError());
        }
        // Block main thread indefinitely
        Thread.currentThread().join();
    }
}