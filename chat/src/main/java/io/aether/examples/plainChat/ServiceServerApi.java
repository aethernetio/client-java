package io.aether.examples.plainChat;

import io.aether.net.meta.ApiManager;
import io.aether.net.meta.Command;
import io.aether.net.meta.MetaApi;
import io.aether.utils.futures.AFuture;
import io.aether.utils.streams.Value;

public interface ServiceServerApi {
    MetaApi<ServiceServerApi> META = ApiManager.getApi(ServiceServerApi.class);

    @Command(3)
    AFuture registration(String name);

    @Command(4)
    void sendMessage(Value<String> msg);
}
