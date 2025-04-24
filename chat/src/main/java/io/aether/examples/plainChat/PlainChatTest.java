package io.aether.examples.plainChat;

import io.aether.logger.Log;
import io.aether.utils.futures.AFuture;
import io.aether.utils.futures.ARFuture;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class PlainChatTest {
    public final List<URI> registrationUri = new ArrayList<>();

    {
        registrationUri.add(URI.create("tcp://registration.aethernet.io:9010"));
    }

    public void start() {
        ChatService chatService = new ChatService(registrationUri);
        ChatClient chatClient1 = new ChatClient(chatService.aether.getUid(), registrationUri, "client1");
        ChatClient chatClient2 = new ChatClient(chatService.aether.getUid(), registrationUri, "client2");
        AFuture.all(chatClient1.aether.startFuture, chatClient2.aether.startFuture).waitDoneSeconds(10);
        var future = new ARFuture<MessageDescriptor>();
        chatClient2.onMessage.add(m -> {
            Log.info("receive message: $msg", "msg", m);
            future.tryDone(m);
        });
        chatClient1.sendMessage("test");
        future.to((m) -> Log.info("The message has been received: $msg", "msg", m));
        if (!future.waitDoneSeconds(10)) {
            throw new IllegalStateException("timeout receive message exception");
        }
    }
}
