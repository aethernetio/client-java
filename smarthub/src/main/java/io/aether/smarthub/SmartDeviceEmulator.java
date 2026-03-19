package io.aether.smarthub;

import io.aether.api.smarthub.*;
import io.aether.utils.futures.ARFuture;
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
    private final UUID deviceUid;
    private final String statePath;
    private AetherCloudClient client;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SmartDeviceEmulator(UUID deviceUid) {
        this.deviceUid = deviceUid;
        this.statePath = "smarthub-data/device-" + deviceUid + ".bin";
    }

    public void start(String regUri, UUID serviceUid) throws Exception {
        File stateFile = new File(statePath);
        ClientStateInMemory state = stateFile.exists()
                ? ClientStateInMemory.load(stateFile)
                : new ClientStateInMemory(UUID.randomUUID(), List.of(URI.create(regUri)));

        client = new AetherCloudClient(state, "Emulator-" + deviceUid);
        client.connect().to(() -> {
            try { state.save(stateFile); } catch (Exception e) { Log.error(e); }
            Log.info("Device Emulator connected", "uid", state.getUid());
        });

        // getMessageNode синхронизируется внутри, вызываем смело
        io.aether.cloud.client.MessageNode node = client.getMessageNode(serviceUid);
        
        // 1. Создаем контекст регистратора
        var regCtx = node.toApi(SmartHomeHubRegistryApi.META, new SmartHomeHubRegistryApi() {
            @Override public void device(DeviceStream s) {}
            @Override public void gui(GuiStream s) {}
        });

        // 2. Создаем свой стрим (роль устройства) и получаем Remote через Consumer
        final SmartHomeDeviceApiRemote[] hubRemote = {null};
        DeviceStream deviceStream = new DeviceStream(regCtx, r -> hubRemote[0] = r);

        // 3. Регистрируемся на уровне Registry
        SmartHomeHubRegistryApi.META.makeRemote(regCtx).device(deviceStream);
        regCtx.flush();

        scheduler.scheduleAtFixedRate(() -> {
            if (hubRemote[0] == null) return;
            short temp = (short) (20 + Math.random() * 10);
            DeviceRecord record = new DeviceRecord(temp, (short) (System.currentTimeMillis() / 1000));
            hubRemote[0].reportState(deviceUid, new DeviceRecord[]{record}).to(success -> {
                if (success) Log.info("Temperature sent", "val", temp);
            });
            regCtx.flush();
        }, 0, 5, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
        if (client != null) client.destroy(true);
    }
}