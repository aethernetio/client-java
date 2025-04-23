package io.aether.cli;

import io.aether.cloud.client.ClientStateInMemory;
import io.aether.common.ServerDescriptor;
import io.aether.crypt.CryptoLib;
import io.aether.utils.CType;
import io.aether.utils.RU;
import io.aether.utils.consoleCanonical.ConsoleMgrCanonical;
import io.aether.utils.flow.Flow;

import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class CLI {
    public final AetherApi api = new AetherApi();

    public CLI(String... aa) {
        var c = new ConsoleMgrCanonical(aa);
        c.regConverter(CType.of(CryptoLib.class), CryptoLib::valueOf);
        c.regResultConverter("bin", CType.of(ClientStateInMemory.class), ClientStateInMemory::save);
        c.regResultConverterCtx("bin", CType.of(AetherApi.ShowApi.Msg.class), (ctx,v)->{
            ctx.setFileName(v.address.toString());
            ctx.setFileMode(StandardOpenOption.TRUNCATE_EXISTING);
            return v.data;
        });
        c.regResultConverter("json", CType.of(AetherApi.ShowApi.Msg.class), v->{
            Map<String, Object> m = Map.of(
                    "uid", v.address,
                    "data", v.data
            );
            return RU.toJson(m).toString().getBytes(StandardCharsets.UTF_8);
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
        c.execute(api).to(() -> {
            System.out.println("DONE");
        }).waitDone();
    }

    public static void main(String... aa) {
        new CLI(aa);
    }
}