package io.aether.cli;

import io.aether.cloud.client.ClientStateInMemory;
import io.aether.common.ServerDescriptor;
import io.aether.crypt.CryptoLib;
import io.aether.logger.Log;
import io.aether.utils.AString;
import io.aether.utils.CType;
import io.aether.utils.RU;
import io.aether.utils.consoleCanonical.ConsoleMgrCanonical;
import io.aether.utils.flow.Flow;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class CLI {
    public final CliApi api = new CliApi();

    public CLI(String... aa) {
        var c = new ConsoleMgrCanonical(aa) {
            String installedAlias;

            @Override
            public String getAppName() {
                return "aether-cli";
            }

        };
        c.footer = "For more information, please visit the website https://aethernet.io";
        c.regConverter(CType.of(CryptoLib.class), CryptoLib::valueOf);
        c.regResultConverter("bin", CType.of(ClientStateInMemory.class), ClientStateInMemory::save);
        c.regResultConverterCtx("bin", CType.of(CliApi.Msg.class), (ctx, v) -> {
            if (ctx.isToFile()) {
                if (ctx.getFileName() == null) {
                    ctx.setFileName(v.address.toString());
                } else {
                    AString.of().replaceVars(ctx.getFileName(), vv -> {
                        switch (vv.toString()) {
                            case "uid":
                                return v.address;
                        }
                        return "???";
                    });
                }
            }
            return v.data;
        });
        c.regResultConverterCtx("json", CType.of(CliApi.Msg.class), (ctx, v) -> {
            if (ctx.isToFile()) {
                if (ctx.getFileName() == null) {
                    ctx.setFileName(v.address.toString());
                } else {
                    AString.of().replaceVars(ctx.getFileName(), vv -> {
                        switch (vv.toString()) {
                            case "uid":
                                return v.address;
                        }
                        return "???";
                    });
                }
            }
            Map<String, Object> m = Map.of(
                    "uid", v.address,
                    "data", v.data
            );
            return RU.toJson(m).toString().getBytes(StandardCharsets.UTF_8);
        });
        c.regResultConverterCtx("hex", CType.of(CliApi.Msg.class), (ctx, v) -> {
            if (ctx.isToFile()) {
                if (ctx.getFileName() == null) {
                    ctx.setFileName(v.address.toString());
                } else {
                    AString.of().replaceVars(ctx.getFileName(), vv -> {
                        switch (vv.toString()) {
                            case "uid":
                                return v.address;
                        }
                        return "???";
                    });
                }
            }
            return RU.toHexString(v.data).getBytes();
        });
        c.regResultConverterCtx("utf8", CType.of(CliApi.Msg.class), (ctx, v) -> {
            if (ctx.isToFile()) {
                var s = AString.of();
                s.add(v.address).add(" -> ").add(new String(v.data));
                return s.getBytes();
            } else {
                var s = AString.of();
                s.add(v.address).add(" -> ").add(new String(v.data));
                return s.getBytes();
            }
        });
        c.regResultConverter("json", CType.of(ClientStateInMemory.class), v -> {
            Map<String, Object> m = Map.of(
                    "uid", v.getUid(),
                    "alias", v.getAlias(),
                    "cloud", v.getCloud(v.getUid()),
                    "serverDescriptors", Flow.flow(v.getCloud(v.getUid())).mapToInt(i -> i).mapToObj(v::getServerDescriptor).toMapExtractKey(ServerDescriptor::idAsInt)
            );
            return RU.toJson(m).toString().getBytes(StandardCharsets.UTF_8);
        });
        c.execute(api).mapRFuture(() -> {
            return api.destroyer.destroy(true).timeout(10, () -> Log.warn("Timeout destroy cli"));
        }).timeout(10, () -> Log.warn("Timeout result cli")).waitDone();
    }

    public static void main(String... aa) {
        new CLI(aa);
    }
}