package io.aether.cli;

import io.aether.api.common.CryptoLib;
import io.aether.api.common.ServerDescriptor;
import io.aether.cloud.client.ClientStateInMemory;
import io.aether.logger.Log;
import io.aether.utils.AString;
import io.aether.utils.CTypeI;
import io.aether.utils.HexUtils;
import io.aether.utils.RU;
import io.aether.utils.consoleCanonical.ConsoleMgrCanonical;
import io.aether.utils.flow.Flow;
import io.aether.utils.futures.ARFuture;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

public class CLI {
    public final CliApi api;
    private final ARFuture<Object> resultFuture;
    private final CliState cliState;

    public CLI(String... aa) {
        // 1. Initialize and load persistent state
        this.cliState = new CliState();
        this.cliState.load();

        // 2. Pass state to CliApi
        this.api = new CliApi(this.cliState);

        var consoleMgr = new ConsoleMgrCanonical(aa) {
            @Override
            public String getAppName() {
                return "aether-cli";
            }
        };

        consoleMgr.footer = "For more information, please visit the website https://aethernet.io";

        // Setup converters
        consoleMgr.regConverter(CTypeI.of(CryptoLib.class), CryptoLib::valueOf);

        // Register custom converter for UUID to support aliases (user-defined and static)
        consoleMgr.regConverter(CTypeI.of(UUID.class), s -> {
            if (s == null) return null;
            // api.resolveUuid resolves user aliases first, then static aliases
            return api.resolveUuid(s);
        });

        consoleMgr.regResultConverter("bin", CTypeI.of(ClientStateInMemory.class), ClientStateInMemory::save);

        // Converters for Msg
        setupMsgConverters(consoleMgr);
        setupClientStateJsonConverter(consoleMgr);

        // Start execution and get the result
        this.resultFuture = consoleMgr.execute(api);

        // Cleanup chain
        resultFuture.mapRFuture(v ->
                api.destroyer.destroy(true).timeout(10, () ->
                        Log.warn("Timeout destroying CLI resources")
                ).mapRFuture(() -> null)
        ).timeout(10, () -> Log.warn("Timeout result cli")).waitDone();
    }

    private void setupMsgConverters(ConsoleMgrCanonical consoleMgr) {
        consoleMgr.regResultConverterCtx("bin", CTypeI.of(CliApi.Msg.class), (ctx, v) -> {
            if (ctx.isToFile() && ctx.getFileName() == null) {
                ctx.setFileName(v.address.toString());
            }
            return v.data;
        });

        consoleMgr.regResultConverterCtx("json", CTypeI.of(CliApi.Msg.class), (ctx, v) -> {
            if (ctx.isToFile() && ctx.getFileName() == null) {
                ctx.setFileName(v.address.toString());
            }
            Map<String, Object> m = Map.of("uid", v.address, "data", v.data);
            return RU.toJson(m).toString().getBytes(StandardCharsets.UTF_8);
        });

        consoleMgr.regResultConverterCtx("hex", CTypeI.of(CliApi.Msg.class), (ctx, v) -> {
            if (ctx.isToFile() && ctx.getFileName() == null) {
                ctx.setFileName(v.address.toString());
            }
            return HexUtils.toHexString(v.data).getBytes();
        });

        consoleMgr.regResultConverterCtx("utf8", CTypeI.of(CliApi.Msg.class), (ctx, v) -> {
            var s = AString.of();
            s.add(v.address).add(" -> ").add(new String(v.data));
            return s.getBytes();
        });
    }

    private void setupClientStateJsonConverter(ConsoleMgrCanonical consoleMgr) {
        consoleMgr.regResultConverter("json", CTypeI.of(ClientStateInMemory.class), v -> {
            Map<String, Object> m = Map.of(
                    "uid", v.getUid(),
                    "alias", v.getAlias(),
                    "cloud", v.getCloud(v.getUid()),
                    "serverDescriptors", Flow.flow(v.getCloud(v.getUid()).getData())
                            .mapToInt()
                            .mapToObj(v::getServerDescriptor)
                            .toMapExtractKey(ServerDescriptor::getId)
            );
            return RU.toJson(m).toString().getBytes(StandardCharsets.UTF_8);
        });
    }

    public ARFuture<Object> getResultFuture() {
        return resultFuture;
    }

    public static void main(String... aa) {
        new CLI(aa);
    }
}