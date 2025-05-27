package io.aether.examples.meteostation;

import io.aether.net.meta.Pack;
import io.aether.net.meta.Variant;
import io.aether.utils.AString;
import io.aether.utils.ToString;

public class Metric implements ToString {
    public final @Pack int id;
    public final Variant<?> value;

    public Metric(@Pack int id, Variant<?> value) {
        this.id = id;
        this.value = value;
    }

    @Override
    public AString toString(AString sb) {
        sb.add(id).add(":").add(value);
        return sb;
    }

    @Override
    public String toString() {
        return toString2();
    }
}
