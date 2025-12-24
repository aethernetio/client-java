package io.aether.cloud.client;

import io.aether.api.common.Cloud;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientCloud {

    private final UUID uid;

    private short[] sids;

    private final Map<Short, Long> weights = new ConcurrentHashMap<>();

    public ClientCloud(UUID uid, Cloud cloud) {
        this.uid = uid;
        this.sids = cloud.getData();
        for (short sid : sids) weights.putIfAbsent(sid, 0L);
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
        weights.merge(sid, 50L, Long::sum);
    }

    public void demote(short sid) {
        weights.merge(sid, -100L, Long::sum);
    }

    public Cloud toCloud() {
        return new Cloud(sids);
    }

    public synchronized void smartMerge(Cloud newCloud) {
        short[] newData = newCloud.getData();
        
        // 1. Применяем механизм "забывания" (снижаем веса на 10% при каждом обновлении)
        weights.replaceAll((sid, weight) -> (long)(weight * 0.9));

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
}