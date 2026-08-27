
package io.aether.smarthub;

import io.aether.api.smarthub.DeviceStream;
import io.aether.api.smarthub.GuiStream;
import io.aether.api.smarthub.SmartHomeClientDeviceApi;
import io.aether.api.smarthub.SmartHomeDeviceApi;
import io.aether.api.smarthub.SmartHomeHubRegistryApi;
import io.aether.net.fastMeta.AutoFlushContext;

import io.aether.utils.Destroyer;
import io.aether.utils.dataio.DataInOutStatic;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartHubAutoFlushTest {

    @Test
    void deviceStreamAutoFlushesWithoutManualFlush() throws Exception {
        Destroyer destroyer = new Destroyer("SmartHubAutoFlushTest");

        java.util.function.Function<Destroyer, AutoFlushContext> contextFactory =
                owner -> {
                    try {
                        try {
                            return AutoFlushContext.class
                                    .getConstructor()
                                    .newInstance();
                        } catch (NoSuchMethodException ignored) {
                            return AutoFlushContext.class
                                    .getConstructor(Destroyer.class)
                                    .newInstance(owner);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };

        java.util.function.BiConsumer<AutoFlushContext, Object> localApiSetter =
                (ctx, api) -> {
                    try {
                        try {
                            AutoFlushContext.class
                                    .getMethod("setLocalApi", Object.class)
                                    .invoke(ctx, api);
                            return;
                        } catch (NoSuchMethodException ignored) {
                        }

                        Class<?> type = ctx.getClass();
                        while (type != null) {
                            try {
                                var field = type.getDeclaredField("localApi");
                                field.setAccessible(true);
                                field.set(ctx, api);
                                return;
                            } catch (NoSuchFieldException ignored) {
                                type = type.getSuperclass();
                            }
                        }

                        throw new IllegalStateException(
                                "localApi field is not found");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };

        try {
            short expected = 157;
            AtomicInteger received = new AtomicInteger();
            CountDownLatch receivedLatch = new CountDownLatch(1);
            UUID peerUid = UUID.randomUUID();

            AutoFlushContext serverContext =
                    contextFactory.apply(destroyer);

            localApiSetter.accept(
                    serverContext,
                    new SmartHomeHubRegistryApi() {
                        @Override
                        public void device(DeviceStream stream) {
                            stream.asIn()
                                    .keys(
                                            ctx -> (SmartHomeDeviceApi) value -> {
                                                received.set(value);
                                                receivedLatch.countDown();
                                            },
                                            peerUid)
                                    .accept();
                        }

                        @Override
                        public void gui(GuiStream stream) {
                        }
                    });

            AutoFlushContext clientContext =
                    contextFactory.apply(destroyer);

            localApiSetter.accept(
                    clientContext,
                    SmartHomeClientDeviceApi.EMPTY);

            clientContext.onFlushData(data ->
                    SmartHomeHubRegistryApi.META.makeLocal(
                            serverContext,
                            new DataInOutStatic(data)));

            var remoteHub =
                    clientContext.makeRemote(
                            SmartHomeHubRegistryApi.META);

            var remoteDevice =
                    remoteHub.openDevice(
                            remote -> SmartHomeClientDeviceApi.EMPTY,
                            data -> data);

            remoteDevice.reportState(expected);

            assertTrue(
                    receivedLatch.await(2, TimeUnit.SECONDS),
                    "AutoFlushContext did not deliver DeviceStream");

            assertEquals(
                    expected,
                    received.get());
        } finally {
            destroyer.destroy(true);
        }
    }
}