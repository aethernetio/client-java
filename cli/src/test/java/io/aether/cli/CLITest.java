package io.aether.cli;

import io.aether.utils.RU;
import org.junit.jupiter.api.Test;

class CLITest {
    @Test
    void createClientTest() {
//        Log.printConsoleColored(new LogFilter());
        var cli = new CLI();
        try {
            var st1 = cli.createClient(new String[]{
                    "create",
                    "--reg-uri", "dev",
                    "--format", "bin",
                    "--output", "state1.bin",
            });
            System.out.println("st1: " + st1.getUid());
            var st2 = cli.createClient(new String[]{
                    "create",
                    "--reg-uri", "dev",
                    "--format", "bin",
                    "--output", "state2.bin",
            });
            System.out.println("st2: " + st2.getUid());
            cli.getMessages(new String[]{
                    "get",
                    "--format", "text_utf8",
                    "--state-bin", "state2.bin",
            });
            cli.sendMessage(new String[]{
                    "send",
                    "--send-text", "test",
                    "--state-bin", "state1.bin",
                    "--receiver", st2.getUid().toString()
            });

            RU.sleep(5000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}