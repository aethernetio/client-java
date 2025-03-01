package io.aether.examples.plainChat;

import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;

public class Main {
	public static void main(String... aa) {
		ChatService chatService = new ChatService();
		chatService.aether.startFuture.waitDoneSeconds(10);
		ChatClient chatClient1 = new ChatClient(chatService.aether.getUid(), "client1");
		ChatClient chatClient2 = new ChatClient(chatService.aether.getUid(), "client2");
		AFuture.all(chatClient1.aether.startFuture, chatClient2.aether.startFuture).waitDoneSeconds(10);
		var future = new ARFuture<MessageDescriptor>();
		chatClient2.onMessage.add(future::done);
		chatClient1.sendMessage("test");
		future.waitDoneSeconds(10);
	}
}
