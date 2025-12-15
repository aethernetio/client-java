package io.aether.example.smarthome.plain;

import io.aether.StandardUUIDs;
import io.aether.api.common.CryptoLib;
import io.aether.api.smarthome.Record;
import io.aether.api.smarthome.SimpleClientApi;
import io.aether.api.smarthome.SimpleClientApiRemote;
import io.aether.api.smarthome.SimpleDeviceApi;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.logger.Log;
import io.aether.net.fastMeta.FastApiContextLocal;
import io.aether.utils.futures.AFuture;

import java.io.File;
import java.net.URI;
import java.util.List;

public class MockCommutatorClientPlain {

    public final AetherCloudClient client;
    public final String name;
    final File stateFile = new File("./state.bin");
    ClientStateInMemory state;
    private double timePhase = 0; // Для генерации синусоиды

    public MockCommutatorClientPlain(String name, String registrationUri) {
        this.name = name;
        if (stateFile.exists()) {
            state = ClientStateInMemory.load(stateFile);
        } else {
            state = new ClientStateInMemory(
                    StandardUUIDs.ANONYMOUS_UID,
                    List.of(URI.create(registrationUri)),
                    null,
                    CryptoLib.SODIUM
            );
        }
        this.client = new AetherCloudClient(state, name);
    }

    public AFuture start() {
        Log.info("[$name] Plain Commutator starting...", "name", name);

        client.onClientStream(node -> {
            // Заглушка
            SimpleDeviceApi localApiStub = new SimpleDeviceApi() {
                @Override
                public void requestRecords(short count) {
                }
            };

            FastApiContextLocal<SimpleDeviceApi> ctx = node.toApi(SimpleDeviceApi.META, localApiStub);

            // Реализация логики
            node.toApi(SimpleDeviceApi.META, new SimpleDeviceApi() {
                @Override
                public void requestRecords(short count) {
                    int size = count & 0xFFFF;
                    if (size > 1000) size = 1000;
                    if (size <= 0) size = 1;

                    Log.info("Generating " + size + " records for GUI");

                    // Генерируем историю "назад во времени"
                    Record[] records = new Record[size];

                    // Текущая фаза синусоиды (сдвигаем каждый запрос, чтобы график двигался)
                    timePhase += 0.2;

                    for (int i = 0; i < size; i++) {
                        // 1. Генерируем "реальную" температуру (например, от 15 до 25 градусов)
                        // i * 0.1 - это шаг назад в прошлое для синусоиды
                        double realTemp = 20.0 + Math.sin(timePhase - (i * 0.1)) * 5.0;

                        // 2. Упаковываем по формуле: (Temp + 30) * 3
                        // Ограничиваем байтом (0..255)
                        int packedVal = (int) Math.round((realTemp + 30) * 3);
                        if (packedVal < 0) packedVal = 0;
                        if (packedVal > 255) packedVal = 255;

                        // 3. Создаем запись
                        // time = разница с предыдущим замером. Пусть будет 1 секунда.
                        byte timeDiff = 1;

                        records[i] = new Record((byte) packedVal, timeDiff);
                    }

                    SimpleClientApiRemote gui = SimpleClientApi.META.makeRemote(ctx);
                    gui.receiveStatus(records);
                    ctx.flush(AFuture.make());
                }
            });
        });

        return client.connect().to(() -> {
            client.forceUpdateStateFromCache().to(()->{
                state.save(stateFile);
            });
        });
    }

    public AFuture stop() {
        return client.destroy(true);
    }
}