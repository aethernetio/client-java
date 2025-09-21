package io.aether.examples.meteostation;

import io.aether.net.fastMeta.Pack;
import io.aether.net.meta.Variant;
import io.aether.utils.AString;

import java.util.Date;
import java.util.UUID;

public class MetricFull extends Metric {
    final Date time;
    final UUID producer;

    public MetricFull(@Pack int id, Variant<?> value, Date time, UUID producer) {
        super(id, value);
        this.time = time;
        this.producer = producer;
    }

    @Override
    public AString toString(AString sb) {
        sb.add(producer).add(":").add(time).add(":").add(id).add(":").add(value);
        return sb;
    }

}
