package io.aether.examples.plainChat;

import io.aether.net.meta.Command;

public interface ServiceClientApi {
	@Command(3)
	void addNewUsers(UserDescriptor[] users);
	@Command(4)
	void newMessages(MessageDescriptor[] messages);
}
