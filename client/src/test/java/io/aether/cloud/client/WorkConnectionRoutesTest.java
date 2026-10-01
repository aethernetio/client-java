
package io.aether.cloud.client;

import io.aether.api.common.ServerDescriptor;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkConnectionRoutesTest {


    private static ServerDescriptor descriptor(short sid) {
        return new ServerDescriptor(
                sid,
                io.aether.api.common.IPAddressAndPortsListUtil.of(
                        java.util.List.of()
                )
        );
    }


    @Test
    void resolvesExactSidRoute() {
        URI p21 = URI.create("tcp://proxy:12021");
        URI p27 = URI.create("tcp://proxy:12027");

        WorkConnectionRoutes routes = WorkConnectionRoutes.proxyOnly(
                Map.of((short) 21, p21, (short) 27, p27)
        );

        assertEquals(p21, routes.resolve(descriptor((short) 21)));
        assertEquals(p27, routes.resolve(descriptor((short) 27)));
    }

    @Test
    void proxyOnlyRejectsMissingSid() {
        WorkConnectionRoutes routes = WorkConnectionRoutes.proxyOnly(
                Map.of((short) 21, URI.create("tcp://proxy:12021"))
        );

        AetherCloudClient.ClientApiException error = assertThrows(
                AetherCloudClient.ClientApiException.class,
                () -> routes.resolve(descriptor((short) 30))
        );
        assertTrue(
                error.getMessage().contains("SID 30"),
                "Message must name missing SID: " + error.getMessage()
        );
    }

    @Test
    void directFallbackReturnsNull() {
        WorkConnectionRoutes routes = WorkConnectionRoutes.withDirectFallback(
                Map.of((short) 21, URI.create("tcp://proxy:12021"))
        );

        assertNull(routes.resolve(descriptor((short) 30)));
    }

    @Test
    void copiesInputMapImmutably() {
        Map<Short, URI> mutable = new HashMap<>();
        URI initial = URI.create("tcp://proxy:12021");
        mutable.put((short) 21, initial);

        WorkConnectionRoutes routes =
                WorkConnectionRoutes.proxyOnly(mutable);

        mutable.put((short) 21, URI.create("tcp://other:1"));

        assertEquals(initial, routes.resolve(descriptor((short) 21)));
    }
}