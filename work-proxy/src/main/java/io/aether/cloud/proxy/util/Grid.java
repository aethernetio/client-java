
package io.aether.cloud.proxy.util;

import io.aether.logger.Log;
import io.aether.utils.RU;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Local copy of {@code io.aether.utils.GlobalMessageGrid} (originally in
 * server-core), renamed to {@code Grid} for use inside the proxy package.
 * Semantics are unchanged.
 */
public abstract class Grid {
    protected final int capacity;
    protected final int mask;
    protected final MessageSlot[] slots;
    protected final AtomicLongArray bitmask;

    public Grid(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "capacity must be positive: " + capacity
            );
        }
        this.capacity = capacity;
        this.mask = capacity - 1;
        this.slots = new MessageSlot[capacity];
        this.bitmask = new AtomicLongArray((capacity + 63) >>> 6);
        for (int i = 0; i < capacity; i++) {
            slots[i] = new MessageSlot(this, i);
        }
    }

    public MessageSlot store(
            UUID from,
            UUID to,
            byte[] data,
            int timeDead
    ) {
        return store(
                from,
                to,
                data,
                timeDead,
                Long.MAX_VALUE,
                -1L
        );
    }

    public void processGrid() {
        int currentTime = (int) (RU.time() / 1000);
        for (int m = 0; m < bitmask.length(); m++) {
            long word = bitmask.get(m);
            if (word == 0) continue;
            while (word != 0) {
                int bitIndex = Long.numberOfTrailingZeros(word);
                int slotIndex = (m << 6) + bitIndex;
                handleSlot(slotIndex, currentTime);
                word &= ~(1L << bitIndex);
            }
        }
    }

    private void handleSlot(int idx, int currentTime) {
        MessageSlot slot = slots[idx];
        if (slot.checkTimeout(currentTime)) {
            onMessageTimeout(slot);
        } else if (slot.trySend(currentTime)) {
            onProcessMessage(slot);
        }
    }

    protected void onMessageTimeout(MessageSlot slot) {
    }

    protected abstract void onProcessMessage(MessageSlot slot);

    void setBit(int idx) {
        int wordIdx = idx >>> 6;
        long bit = 1L << (idx & 63);
        while (true) {
            long current = bitmask.get(wordIdx);
            if (bitmask.compareAndSet(wordIdx, current, current | bit)) {
                break;
            }
        }
    }

    void clearBit(int idx) {
        int wordIdx = idx >>> 6;
        long bit = 1L << (idx & 63);
        while (true) {
            long current = bitmask.get(wordIdx);
            if (bitmask.compareAndSet(wordIdx, current, current & ~bit)) {
                break;
            }
        }
    }

    public boolean isRequests() {
        for (int i = 0; i < bitmask.length(); i++) {
            if (bitmask.get(i) != 0) {
                return true;
            }
        }
        return false;
    }

    public MessageSlot store(
            UUID from,
            UUID to,
            byte[] data,
            int timeDead,
            long uapDeadlineMs,
            long uapGeneration
    ) {
        for (int i = 0; i < capacity; i++) {
            MessageSlot slot = slots[i];

            if (slot.tryLock()) {
                slot.from = from;
                slot.to = to;
                slot.data = data;
                slot.timeDead = timeDead;
                slot.uapDeadlineMs = uapDeadlineMs;
                slot.uapGeneration = uapGeneration;

                setBit(i);
                slot.state = MessageSlot.State.TO_SEND;

                Log.trace(
                        "GRID: message offered to slot",
                        "slot",
                        slot
                );

                return slot;
            }
        }

        return null;
    }
}
