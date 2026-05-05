package io.aether.smarthub;

import io.aether.api.smarthub.*;
import io.aether.net.fastMeta.*;
import io.aether.utils.futures.ARFuture;
import io.aether.utils.futures.AFuture;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.logger.Log;

import java.io.File;
import java.net.URI;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class SmartDeviceEmulator {
    private final UUID serviceUid;
    private UUID deviceUid;
    private final String statePath;
    private AetherCloudClient client;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SmartDeviceEmulator(UUID serviceUid) {
        this.serviceUid = serviceUid;
        this.statePath = "smarthub-data/device-" + serviceUid + ".bin";
    }

    public UUID getDeviceUid() {
        return deviceUid;
    }

    private final AFuture ready = AFuture.make();

    public AFuture getReady() {
        return ready;
    }




    public void start(String regUri) throws Exception {
        Log.info("SmartDeviceEmulator.start() called", "regUri", regUri, "serviceUid", serviceUid);
        File stateFile = new File(statePath);
        ClientStateInMemory state = stateFile.exists()
                ? ClientStateInMemory.load(stateFile)
                : new ClientStateInMemory(serviceUid, List.of(URI.create(regUri)));
        Log.info("ClientStateInMemory created/loaded", "exists", stateFile.exists());
        client = new AetherCloudClient(state, "Emulator-for-" + serviceUid);
        Log.info("AetherCloudClient created");
        client.connect()
            .timeoutError(60, "Connect timeout")
            .to(() -> {
                Log.info("Connect callback started");
                try { state.save(stateFile); Log.info("State saved", "path", statePath); } catch (Exception e) { Log.error(e); }
                deviceUid = state.getUid();
                Log.info("Device Emulator connected", "uid", deviceUid);
                ready.done();
            })
            .onError(ready::error);
        


        ready.to(() -> {
            Log.info("DeviceUid obtained, starting device reporting", "deviceUid", deviceUid);
            try {
                io.aether.cloud.client.MessageNode node = client.getMessageNode(serviceUid);

                var ctx = node.toApi(SmartHomeClientDeviceApi.META, SmartHomeClientDeviceApi.EMPTY);
                final SmartHomeHubRegistryApiRemote remoteHubApi = ctx.makeRemote(SmartHomeHubRegistryApi.META);
                FastFutureContext ctx2=new FastApiContext(){
                    @Override
                    public int regFuture(FutureRec worker) {
                        return ctx.regFuture(worker);
                    }

                    @Override
                    public void flush(FlushReport report) {
                        var dataApi2=remoteDataToArray();
                        remoteHubApi.device(new DeviceStream(dataApi2));
                        remoteHubApi.flush(report);
                    }
                };
                var remoteDeviceApi=ctx2.makeRemote(SmartHomeDeviceApi.META);
                Log.info("Starting scheduled temperature reporting", "intervalSec", 5);
                scheduler.scheduleAtFixedRate(() -> {
                    int tempCelsius = 22 + (int)(Math.random() * 5);
                    // Формат: 0 = -30C, шаг 1/3 градуса. 25C -> (25+30)*3 = 165
                    byte rawTemp = (byte)((tempCelsius + 30) * 3);
                    Log.info("Sending temperature", "celsius", tempCelsius, "raw", (rawTemp & 0xFF), "deviceUid", deviceUid);
                    remoteDeviceApi.reportState(rawTemp);
                    remoteDeviceApi.flush();
                }, 0, 1, TimeUnit.SECONDS);
            } catch (Exception e) {
                Log.error("Failed to start device reporting", e);
            }
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