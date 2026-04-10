package io.aether.cloud.client;

import io.aether.logger.Log;
import io.aether.utils.Destroyer;
import io.aether.utils.RU;
import io.aether.utils.ToString;
import io.aether.utils.futures.AFuture;
import io.aether.utils.interfaces.Destroyable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ClientStateInFile extends ClientStateInMemory implements ToString, Destroyable {
    final File fileState;
    private final Destroyer destroyer = new Destroyer("ClientStateInFile");
    private byte[] lastData;

    public ClientStateInFile(UUID parent, List<URI> registrationUris) {
        this(parent, registrationUris, new File("state.bin"));
    }

    public ClientStateInFile(UUID parent, List<URI> registrationUris, File fileState) {
        super(parent, registrationUris);
        this.fileState = fileState;
        loadState(fileState);
        destroyer.add(RU.scheduleAtFixedRate(10, this::saveState));
    }

    @Override
    public AFuture destroy(boolean force) {
        return destroyer.destroy(false);
    }

    @Override
    public void saveState() {
        if (getUid() == null) return;
        var d = save();
        if (lastData == null || !Arrays.equals(lastData, d)) {
            lastData = d;
            try {
                Files.write(fileState.toPath(), save(), StandardOpenOption.CREATE);
            } catch (IOException e) {
                Log.error("Cannot save a store", e);
                throw new RuntimeException(e);
            }
        }
    }
}