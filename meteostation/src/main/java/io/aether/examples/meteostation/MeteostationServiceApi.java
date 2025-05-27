package io.aether.examples.meteostation;

import io.aether.net.meta.ApiManager;
import io.aether.net.meta.Command;
import io.aether.net.meta.MetaApi;
import io.aether.net.meta.Pack;
import io.aether.utils.futures.ARFuture;

public interface MeteostationServiceApi {
    MetaApi<MeteostationServiceApi> META = ApiManager.getApi(MeteostationServiceApi.class);

    @Command(3)
    ARFuture<@Pack Integer> registrationSensor(SensorDescriptor sensorDescriptor);
    @Command(4)
    ARFuture<@Pack Integer> registrationControl(String name);

    @Command(30)
    void sendMetric(Metric metric);
}
