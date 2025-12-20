package io.aether.examples.meteostation;

import io.aether.net.fastMeta.Pack;
import io.aether.utils.AString;
import io.aether.utils.ToString;

public abstract class SensorDescriptor implements ToString {

    public final @Pack int id;
    public final String name;

    @Override
    public AString toAString(AString sb) {
        return sb.add(id).add(':').add(name);
    }

    public SensorDescriptor(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
