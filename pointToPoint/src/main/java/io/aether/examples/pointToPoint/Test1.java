package io.aether.examples.pointToPoint;

import io.aether.cloud.client.AetherCloudClient;
import io.aether.utils.futures.ARFuture;

public class Test1 {
    public void main(String[] args) {
        var aether1 = new AetherCloudClient();
        ARFuture<String> messageFuture = new ARFuture<>();
        aether1.onMessage((uid, msg) -> messageFuture.tryDone(new String(msg)));
        aether1.waitStart(10);

        var aether2 = new AetherCloudClient();
        aether2.sendMessage(aether1.getUid(), "Hello World!".getBytes());

        if (messageFuture.waitDoneSeconds(10)) {
            System.out.println("receive the message: " + messageFuture.get());
        } else {
            throw new IllegalStateException();
        }
    }
}
