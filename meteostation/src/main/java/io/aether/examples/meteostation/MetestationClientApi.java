package io.aether.examples.meteostation;

import io.aether.net.meta.ApiManager;
import io.aether.net.meta.Command;
import io.aether.net.meta.MetaApi;

public interface MetestationClientApi {
    MetaApi<MetestationClientApi> META = ApiManager.getApi(MetestationClientApi.class);

    @Command(3)
    void addMetricDescriptor(SensorDescriptor sensorDescriptors);
    @Command(4)
    void addMetrics(MetricFull metrics);

}
