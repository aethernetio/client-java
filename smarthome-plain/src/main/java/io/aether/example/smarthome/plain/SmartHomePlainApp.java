package io.aether.example.smarthome.plain;

import io.aether.logger.Log;

import java.util.UUID;

public class SmartHomePlainApp {
    public void start() {
        Log.printConsoleColored();
        MockCommutatorClientPlain mockCommutator = null;
        try {

            mockCommutator = new MockCommutatorClientPlain("ManualCommutator", "tcp://dbservice.aethernet.io:9010");
            mockCommutator.start().toCompletableFuture().join();
            UUID commutatorUuid = mockCommutator.client.getUid();

            Log.info("==================================================================");
            Log.info("MANUAL TEST READY.");
            Log.info("1. Commutator UUID: " + commutatorUuid);
            Log.info("==================================================================");

            Thread.currentThread().join();

        } catch (Exception e) {
            Log.error("Manual Probe Failed", e);
        } finally {
            if (mockCommutator != null) mockCommutator.stop().join();
        }
    }

    public static void main(String... aa) {
        new SmartHomePlainApp().start();
    }
}