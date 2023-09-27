package io.aether.examples.plainChat;

import io.aether.net.ApiResultConsumer;
import io.aether.net.Command;

public interface ServiceClientApi extends ApiResultConsumer {
	@Command(3)
	void addNewUsers(UserDescriptor[] users);
	@Command(4)
	void newMessages(MessageDescriptor[] messages);
}
