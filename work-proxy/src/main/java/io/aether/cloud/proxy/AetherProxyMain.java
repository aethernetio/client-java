
package io.aether.cloud.proxy;

import io.aether.net.fastMeta.FastMetaNet;
import io.aether.net.virtual.VirtualFastMetaNet;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

public final class AetherProxyMain {

    private AetherProxyMain() {
    }

    public static void main(String[] args) throws Exception {
        Path configPath = args.length == 0
                ? Path.of("aether-proxy.properties")
                : Path.of(args[0]);

        AetherProxyConfig config = AetherProxyConfig.load(configPath);

        FastMetaNet.INSTANCE.set(new VirtualFastMetaNet());

        AetherProxy proxy = new AetherProxy(config).start();

        CountDownLatch stopped = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(
                Thread.ofPlatform()
                        .name("aether-proxy-shutdown")
                        .unstarted(() -> {
                            try {
                                proxy.destroy(true).waitSuccessful();
                            } finally {
                                stopped.countDown();
                            }
                        })
        );

        System.out.println(
                "Aether proxy started\n"
                        + "  proxy.sid=" + config.proxySid() + "\n"
                        + "  registration:\n    "
                        + config.registrationListen() + "\n"
                        + "  work:\n    "
                        + config.workListen() + "\n"
                        + "  public:\n    "
                        + config.workPublic()
        );

        stopped.await();
    }
}
