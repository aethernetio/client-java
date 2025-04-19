package io.aether.examples.plainChat;

import io.aether.net.meta.ApiManager;
import io.aether.net.meta.Command;
import io.aether.net.meta.MetaApi;

public interface ServiceServerApi {
    MetaApi<ServiceServerApi> META = ApiManager.getApi(ServiceServerApi.class);

    @Command(3)
    void registration(String name);

    @Command(4)
    void sendMessage(String msg);
}
