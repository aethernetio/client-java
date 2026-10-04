
package io.aether.cloud.client;

import io.aether.api.common.ServerDescriptor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkProxyClientConfigTest {

    private static Path writeProps(String content) throws IOException {
        Path path = Files.createTempFile("work-proxy-client-", ".properties");
        Files.writeString(path, content);
        path.toFile().deleteOnExit();
        return path;
    }


    private static ServerDescriptor descriptor(short sid) {
        return new ServerDescriptor(
                sid,
                io.aether.api.common.IPAddressAndPortsListUtil.of(
                        java.util.List.of()
                ),
                new io.aether.api.common.SignedKey[0]
        );
    }


    @Test
    void loadsRoutesAndResolvesBySid() throws IOException {
        Path path = writeProps("""
                work.21.proxy=tcp://proxy.example.com:12021
                work.27.proxy=tcp://proxy.example.com:12027
                """);

        WorkConnectionRouter router =
                WorkProxyClientConfig.load(path);

        assertEquals(
                URI.create("tcp://proxy.example.com:12021"),
                router.resolve(descriptor((short) 21))
        );
        assertEquals(
                URI.create("tcp://proxy.example.com:12027"),
                router.resolve(descriptor((short) 27))
        );
    }

    @Test
    void proxyOnlyRejectsUnknownSid() throws IOException {
        Path path = writeProps("""
                work.21.proxy=tcp://proxy.example.com:12021
                """);

        WorkConnectionRouter router =
                WorkProxyClientConfig.load(path);

        assertThrows(
                AetherCloudClient.ClientApiException.class,
                () -> router.resolve(descriptor((short) 99))
        );
    }

    @Test
    void directFallbackReturnsNullForUnknownSid() throws IOException {
        Path path = writeProps("""
                work.21.proxy=tcp://proxy.example.com:12021
                """);

        WorkConnectionRouter router =
                WorkProxyClientConfig.load(path, false);

        assertNull(router.resolve(descriptor((short) 99)));
    }

    @Test
    void rejectsEmptyConfig() throws IOException {
        Path path = writeProps("# no routes\n");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> WorkProxyClientConfig.load(path)
        );
        assertTrue(error.getMessage().contains("No work proxy routes"));
    }
}