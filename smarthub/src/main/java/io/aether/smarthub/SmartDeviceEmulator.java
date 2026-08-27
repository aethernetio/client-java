package io.aether.smarthub;

import io.aether.api.smarthub.SmartHomeClientDeviceApi;
import io.aether.api.smarthub.SmartHomeHubRegistryApi;

import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInFile;


import io.aether.logger.Log;
import io.aether.logger.LogFilter;

import io.aether.utils.futures.AFuture;


import java.io.File;
import java.net.URI;
import java.util.List;
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
    private ClientStateInFile clientState;



    public SmartDeviceEmulator(UUID serviceUid) {
        this(
                serviceUid,
                "smarthub-data/device-" + serviceUid + ".bin");
    }

    SmartDeviceEmulator(
            UUID serviceUid,
            String statePath) {

        this.serviceUid = serviceUid;
        this.statePath = statePath;
    }


    public UUID getDeviceUid() {
        return deviceUid;
    }

    public AFuture getReady() {
        return ready;
    }


    public void start(String regUri) throws Exception {
        Log.info(
                "SmartDeviceEmulator.start() called",
                "regUri", regUri,
                "serviceUid", serviceUid);

        URI uri = URI.create(regUri);

        clientState =
                new ClientStateInFile(
                        serviceUid,
                        List.of(uri),
                        new File(statePath));

        client =
                AetherCloudClient.asClient(
                        clientState,
                        "Emulator-" + serviceUid,
                        SmartHomeClientDeviceApi.META,
                        SmartHomeHubRegistryApi.META,
                        remoteHubApi -> {
                            deviceUid = clientState.getUid();

                            Log.info(
                                    "Device Emulator connected",
                                    "uid", deviceUid);

                            remoteHubApi.openDevice(
                                    remoteDeviceApi -> {
                                        Log.info(
                                                "Starting scheduled temperature reporting",
                                                "intervalSec", 1);

                                        scheduler.scheduleAtFixedRate(() -> {
                                            int tempCelsius =
                                                    22 + (int) (Math.random() * 5);

                                            byte rawTemp =
                                                    (byte) ((tempCelsius + 30) * 3);

                                            Log.info(
                                                    "Sending temperature",
                                                    "celsius", tempCelsius,
                                                    "raw", rawTemp & 0xFF,
                                                    "deviceUid", deviceUid);

                                            remoteDeviceApi.reportState(rawTemp);
                                        }, 0, 1, TimeUnit.SECONDS);

                                        return SmartHomeClientDeviceApi.EMPTY;
                                    },
                                    d -> d);

                            ready.done();

                            return SmartHomeClientDeviceApi.EMPTY;
                        });

        client.startFuture.onError(error -> {
            Log.error(
                    "Device Emulator failed to start",
                    error);

            ready.error(error);
        });
    }


    public void stop() {
        scheduler.shutdown();

        if (clientState != null) {
            clientState.saveState();
        }

        if (client != null) {
            client.destroy(true);
        }

        if (clientState != null) {
            clientState.destroy(true);
        }
    }

    public static void main(String[] args) throws Exception {
        Log.printPlainConsole(new LogFilter());

        if (args.length < 1) {
            System.err.println("Usage: SmartDeviceEmulator <serviceUid> [regUri]");
            System.exit(1);
        }

        UUID serviceUid = UUID.fromString(args[0]);
        System.out.println("Emulator args: " + java.util.Arrays.toString(args));

        String regUri = args.length > 1
                ? args[1]
                : "tcp://registration.aethernet.io:9010";

        SmartDeviceEmulator emulator = new SmartDeviceEmulator(serviceUid);
        emulator.start(regUri);

        Runtime.getRuntime().addShutdownHook(new Thread(emulator::stop));

        while (!emulator.getReady().isDone() && !emulator.getReady().isError()) {
            Thread.sleep(100);
        }

        if (emulator.getReady().isError()) {
            throw new RuntimeException(emulator.getReady().getError());
        }

        Thread.currentThread().join();
    }
}