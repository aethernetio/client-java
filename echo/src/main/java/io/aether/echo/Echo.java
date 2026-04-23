
package io.aether.echo;

import io.aether.StandardUUIDs;
import io.aether.cloud.client.AetherCloudClient;
import io.aether.cloud.client.ClientState;
import io.aether.cloud.client.ClientStateInFile;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.logger.Log;
import io.aether.logger.LogFilter;
import io.aether.utils.RU;
import io.aether.utils.futures.AFuture;

import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public class Echo {
    private AetherCloudClient client;

    public Echo() {
        this(List.of(URI.create("tcp://registration.aethernet.io:9010")));
    }

    public Echo(List<URI> registrationUris) {
        this(new ClientStateInFile(StandardUUIDs.ANONYMOUS_UID,registrationUris));
    }

    public Echo(ClientState state) {
        client = AetherCloudClient.of(state);
        client.startFuture.to(() -> {
            Log.info("Echo service started", "uid", client.getUid());
        });

        client.onMessage((u,msg) -> {
            Log.info("Echo received message", "from", u, "size", msg.length, "content", new String(msg));
            client.sendMessage(u, msg).to(() -> {
                Log.debug("Echoed message successfully", "to", u);
            }).onError(e -> {
                Log.error("Failed to echo message", e, "to", u);
            });
        });
    }

    public void runForever() {
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public AFuture start() {
        return client.startFuture;
    }

    public UUID getUid() {
        return client.getUid();
    }

    public AFuture destroy(boolean b) {
        return client.destroy(b);
    }

    public static void main(String... aa) {
        Log.printConsoleColored(new LogFilter());
        new Echo().runForever();
    }
}