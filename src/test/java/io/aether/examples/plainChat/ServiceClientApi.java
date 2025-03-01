package io.aether.examples.plainChat;

import io.aether.net.meta.ApiManager;
import io.aether.net.meta.Command;
import io.aether.net.meta.MetaApi;

public interface ServiceClientApi {
	MetaApi<ServiceClientApi> META= ApiManager.getApi(ServiceClientApi.class);
	@Command(3)
	void addNewUsers(UserDescriptor[] users);
	@Command(4)
	void newMessages(MessageDescriptor[] messages);
}
