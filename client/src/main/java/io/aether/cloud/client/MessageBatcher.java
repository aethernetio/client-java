package io.aether.cloud.client;

import io.aether.api.clientserverapi.AuthorizedApi;
import io.aether.api.clientserverapi.Message;

import java.util.*;

/**
 * Группирует сообщения по одинаковому телу перед отправкой.
 * Если несколько получателей имеют одинаковое тело — используется sendMulticast.
 */
class MessageBatcher {
    private final Map<ByteArrayKey, Set<UUID>> groups = new LinkedHashMap<>();

    void add(UUID uid, byte[] data) {
        groups.computeIfAbsent(new ByteArrayKey(data), k -> new LinkedHashSet<>()).add(uid);
    }

    void flush(AuthorizedApi api) {
        for (var entry : groups.entrySet()) {
            byte[] data = entry.getKey().data;
            UUID[] uids = entry.getValue().toArray(new UUID[0]);
            if (uids.length > 1) {
                api.sendMulticast(uids, data);
            } else {
                api.sendMessage(new Message(uids[0], data));
            }
        }
        groups.clear();
    }

    boolean isEmpty() {
        return groups.isEmpty();
    }

    private static class ByteArrayKey {
        final byte[] data;

        ByteArrayKey(byte[] data) {
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ByteArrayKey)) return false;
            return Arrays.equals(data, ((ByteArrayKey) o).data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }
    }
}