package io.aether.examples.plainChat;

import io.aether.net.ApiResultConsumer;

public interface ServiceServerApi extends ApiResultConsumer {
	void registration(String name);
	void sendMessage(String msg);
}
