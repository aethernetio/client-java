package io.aether.cloud.client;

import io.aether.api.common.AppliedConfig;
import io.aether.api.common.Cloud;
import io.aether.api.common.CloudConfig;
import io.aether.utils.rcollections.BMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientCloud {

    private final UUID uid;
    private final Map<Short, Long> weights = new ConcurrentHashMap<>();
    private volatile long configVersion;
    private volatile long confirmedConfigVersion;

    private short[] sids;

    public ClientCloud(UUID uid, Cloud cloud) {
        this.uid = uid;
        this.sids = cloud.getData();
        for (short sid : sids) weights.putIfAbsent(sid, 0L);
    }

    public UUID getUid() {
        return uid;
    }

    public synchronized short[] getOrderedSids() {
        List<Short> list = new ArrayList<>();
        for (short sid : sids) list.add(sid);
        list.sort((a, b) -> Long.compare(weights.getOrDefault(b, 0L), weights.getOrDefault(a, 0L)));
        short[] result = new short[sids.length];
        for (int i = 0; i < list.size(); i++) result[i] = list.get(i);
        return result;
    }

    public void promote(short sid) {
        weights.merge(sid, 60L, Long::sum);
    }

    public void demote(short sid) {
        weights.merge(sid, -90L, Long::sum);
    }

    public Cloud toCloud() {
        return new Cloud(sids);
    }

    public short[] getData() {
        return sids;
    }


    public synchronized void smartMerge(Cloud newCloud) {
        short[] newData = newCloud.getData();
        // 1. Применяем механизм "забывания" (снижаем веса на 10% при каждом обновлении)
        weights.replaceAll((sid, weight) -> (long) (weight * 0.92));
        // 2. Расчет среднего балла для новичков
        long avg = weights.values().stream().mapToLong(Long::longValue).sum() / Math.max(1, weights.size());
        // 3. Интеграция новых и удаление старых
        for (short sid : newData) weights.putIfAbsent(sid, avg);
        weights.keySet().removeIf(sid -> {
            for (short n : newData) if (n == sid) return false;
            return true;
        });
        // 4. Нормализация (вычитаем минимум, чтобы держать значения в узком диапазоне)
        long min = weights.values().stream().mapToLong(Long::longValue).min().orElse(0L);
        if (min != 0) weights.replaceAll((sid, weight) -> weight - min);
        this.sids = newData;
    }

    public java.util.Map<Short, Long> getWeights() {
        return java.util.Collections.unmodifiableMap(weights);
    }

    public void setWeight(short sid, long weight) {
        weights.put(sid, weight);
    }

    public long getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(long configVersion) {
        this.configVersion = configVersion;
    }

    public long getConfirmedConfigVersion() {
        return confirmedConfigVersion;
    }


    public void applyCloudConfig(CloudConfig config, BMap<AppliedConfig, Boolean> requestsBMap) {
        if (config.getConfigVersion() > this.configVersion) {
            smartMerge(config.getCloud());
            this.configVersion = config.getConfigVersion();
            requestsBMap.getFuture(new AppliedConfig(uid, configVersion));
        }
    }


    public void updateConfirmedConfigVersion(long version) {
        if (version > this.confirmedConfigVersion) {
            this.confirmedConfigVersion = version;
        }
    }
}