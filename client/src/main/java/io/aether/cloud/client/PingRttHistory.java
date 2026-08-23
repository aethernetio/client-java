
package io.aether.cloud.client;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

final class PingRttHistory {

    static final int CAPACITY = 100;

    static final long FALLBACK_RTT_NS =
            200_000_000L;

    private static final long BASE_GUARD_NS =
            10_000_000L;

    private static final long NS_PER_MS =
            1_000_000L;

    private final AtomicLongArray samples =
            new AtomicLongArray(CAPACITY);

    private final AtomicLong sequence =
            new AtomicLong();

    void record(long rttNs) {
        if (rttNs <= 0L) {
            return;
        }

        long ordinal =
                sequence.getAndIncrement();

        int index =
                (int) Math.floorMod(
                        ordinal,
                        (long) CAPACITY
                );

        samples.set(index, rttNs);
    }

    Stats snapshot() {
        long[] current =
                new long[CAPACITY];

        int count = 0;

        for (int i = 0; i < CAPACITY; i++) {
            long sample =
                    samples.get(i);

            if (sample > 0L) {
                current[count++] = sample;
            }
        }

        if (count == 0) {
            return new Stats(
                    0,
                    FALLBACK_RTT_NS,
                    FALLBACK_RTT_NS,
                    guardMs(
                            FALLBACK_RTT_NS,
                            FALLBACK_RTT_NS
                    )
            );
        }

        Arrays.sort(
                current,
                0,
                count
        );

        long minRttNs =
                current[0];

        int p99Index =
                Math.max(
                        0,
                        (int) Math.ceil(
                                count * 0.99d
                        ) - 1
                );

        long p99RttNs =
                current[p99Index];

        return new Stats(
                count,
                minRttNs,
                p99RttNs,
                guardMs(
                        minRttNs,
                        p99RttNs
                )
        );
    }

    long nextPingDelayMs(long fullPingIntervalMs) {
        long guardMs =
                snapshot().guardMs();

        return Math.max(
                1L,
                fullPingIntervalMs - guardMs
        );
    }

    private static long guardMs(
            long minRttNs,
            long p99RttNs
    ) {
        long spreadNs =
                Math.max(
                        0L,
                        p99RttNs - minRttNs
                );

        long guardNs =
                spreadNs / 2L
                        + BASE_GUARD_NS;

        return guardNs / NS_PER_MS
                + (guardNs % NS_PER_MS == 0L
                ? 0L
                : 1L);
    }

    record Stats(
            int sampleCount,
            long minRttNs,
            long p99RttNs,
            long guardMs
    ) {
    }
}
