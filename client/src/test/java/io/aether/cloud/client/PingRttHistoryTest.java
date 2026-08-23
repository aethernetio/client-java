
package io.aether.cloud.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PingRttHistoryTest {

    private static final long MS =
            1_000_000L;

    @Test
    void emptyHistoryUsesFallbackOnlyForCalculation() {
        var history =
                new PingRttHistory();

        var stats =
                history.snapshot();

        assertEquals(0, stats.sampleCount());
        assertEquals(
                200L * MS,
                stats.minRttNs()
        );
        assertEquals(
                200L * MS,
                stats.p99RttNs()
        );
        assertEquals(
                10L,
                stats.guardMs()
        );
        assertEquals(
                990L,
                history.nextPingDelayMs(1_000L)
        );
    }

    @Test
    void guardUsesMinAndP99OfSuccessfulSamples() {
        var history =
                new PingRttHistory();

        for (int ms = 10; ms < 110; ms++) {
            history.record(
                    ms * MS
            );
        }

        var stats =
                history.snapshot();

        assertEquals(
                100,
                stats.sampleCount()
        );
        assertEquals(
                10L * MS,
                stats.minRttNs()
        );
        assertEquals(
                108L * MS,
                stats.p99RttNs()
        );
        assertEquals(
                59L,
                stats.guardMs()
        );
        assertEquals(
                941L,
                history.nextPingDelayMs(1_000L)
        );
    }

    @Test
    void historyKeepsLatestHundredSamples() {
        var history =
                new PingRttHistory();

        history.record(1L * MS);

        for (int ms = 200; ms < 300; ms++) {
            history.record(
                    ms * MS
            );
        }

        var stats =
                history.snapshot();

        assertEquals(
                100,
                stats.sampleCount()
        );
        assertEquals(
                200L * MS,
                stats.minRttNs()
        );
        assertEquals(
                298L * MS,
                stats.p99RttNs()
        );
    }

    @Test
    void invalidSamplesAreIgnored() {
        var history =
                new PingRttHistory();

        history.record(0L);
        history.record(-1L);

        assertEquals(
                0,
                history.snapshot().sampleCount()
        );
    }

    @Test
    void localDelayIsClampedToAtLeastOneMillisecond() {
        var history =
                new PingRttHistory();

        assertEquals(
                1L,
                history.nextPingDelayMs(5L)
        );
    }
}
