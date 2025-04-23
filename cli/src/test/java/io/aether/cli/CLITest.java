package io.aether.cli;

import io.aether.StandardUUIDs;
import io.aether.logger.Log;
import io.aether.logger.LogFilter;
import org.junit.jupiter.api.Test;

class CLITest {
    @Test
    void createClientTest() {
        Log.printConsoleColored(new LogFilter());
//
        CLI.main("send", "A4E386BA-B07B-3A86-8FA0-C67749CD4605","text", "test123",
                "--state", "state1.bin", "--console=human");
        CLI.main("show", "messages",
                "--state", "state2.bin", "--console=human");
    }
}