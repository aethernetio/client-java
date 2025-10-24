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
    public final CliApi api; // <-- Изменено
    private final ARFuture<Object> resultFuture;
    private final CliState cliState; // <-- Добавлено

    public CLI(String... aa) {
        this.cliState = new CliState();
        this.cliState.load();
        this.api = new CliApi(this.cliState);
        // ------------------

        var consoleMgr = new ConsoleMgrCanonical(aa) {
            @Override
            public String getAppName() {
                return "aether-cli";
            }
        };

        consoleMgr.footer = "For more information, please visit the website https://aethernet.io";

        // Настройка конвертеров
        consoleMgr.regConverter(CTypeI.of(CryptoLib.class), CryptoLib::valueOf);

        // --- ОБНОВЛЕНО ---
        // Регистрируем кастомный конвертер для UUID
        // Теперь он использует не-статический метод api.resolveUuid
        consoleMgr.regConverter(CTypeI.of(UUID.class), s -> {
            if (s == null) return null;
            // api.resolveUuid теперь знает о пользовательских алиасах
            return api.resolveUuid(s);
        });
        // --------------------

        consoleMgr.regResultConverter("bin", CTypeI.of(ClientStateInMemory.class), ClientStateInMemory::save);

        // Конвертеры для Msg
        setupMsgConverters(consoleMgr);
        setupClientStateJsonConverter(consoleMgr);

        // Запуск выполнения и получение результата
        this.resultFuture = consoleMgr.execute(api);

        // Cleanup цепочка
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