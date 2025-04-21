package io.aether.cli;

import io.aether.StandardUUIDs;
import io.aether.logger.Log;
import io.aether.logger.LogFilter;
import org.junit.jupiter.api.Test;

class CLITest {
    @Test
    void createClientTest() {
        Log.printConsoleColored(new LogFilter());
        CLI.main("create", "client", StandardUUIDs.TEST_UID.toString(),
                "--dev",
                "--format-out", "bin",
                "--file-out", "state1.bin");
    }
}