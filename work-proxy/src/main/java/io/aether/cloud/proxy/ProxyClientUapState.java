
package io.aether.cloud.proxy;

/**
 * Immutable per-client User Activity Profile state used by the proxy to
 * decide when a downstream client is currently reachable.
 *
 * <p>This is a local copy of {@code io.aether.cloud.server.ClientUapState}
 * with identical semantics; the proxy needs the same receive-window and
 * ping-deadline bookkeeping as a real work server.
 */
public record ProxyClientUapState(
        long lastActivityMs,
        long lastPingMs,
        long nextPingDeadlineMs,
        long mainRxUntilMs,
        long mainRxWindowMs,
        long temporaryRxUntilMs,
        long scheduledRxFromMs,
        long scheduledRxUntilMs,
        long generation
) {

    public static final ProxyClientUapState EMPTY =
            new ProxyClientUapState(
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L
            );

    public ProxyClientUapState recordActivity(long now) {
        long updatedActivity =
                Math.max(lastActivityMs, now);

        if (updatedActivity == lastActivityMs) {
            return this;
        }

        return new ProxyClientUapState(
                updatedActivity,
                lastPingMs,
                nextPingDeadlineMs,
                mainRxUntilMs,
                mainRxWindowMs,
                temporaryRxUntilMs,
                scheduledRxFromMs,
                scheduledRxUntilMs,
                generation
        );
    }

    public ProxyClientUapState applyPing(
            long now,
            long nextPingDelayMs,
            long rxWindowMs
    ) {
        requireNonNegative(
                nextPingDelayMs,
                "nextPingDelayMs"
        );
        requireNonNegative(
                rxWindowMs,
                "rxWindowMs"
        );

        return new ProxyClientUapState(
                Math.max(lastActivityMs, now),
                now,
                saturatingAdd(now, nextPingDelayMs),
                saturatingAdd(now, rxWindowMs),
                rxWindowMs,
                temporaryRxUntilMs,
                scheduledRxFromMs,
                scheduledRxUntilMs,
                generation + 1L
        );
    }

    public ProxyClientUapState openTemporaryWindow(
            long now,
            long durationMs
    ) {
        requireNonNegative(
                durationMs,
                "durationMs"
        );

        long requestedUntil =
                saturatingAdd(now, durationMs);

        return new ProxyClientUapState(
                Math.max(lastActivityMs, now),
                lastPingMs,
                nextPingDeadlineMs,
                mainRxUntilMs,
                mainRxWindowMs,
                Math.max(
                        temporaryRxUntilMs,
                        requestedUntil
                ),
                scheduledRxFromMs,
                scheduledRxUntilMs,
                generation
        );
    }

    public ProxyClientUapState setReceiveWindow(
            long now,
            long startsInMs,
            long durationMs
    ) {
        requireNonNegative(
                startsInMs,
                "startsInMs"
        );
        requireNonNegative(
                durationMs,
                "durationMs"
        );

        long from = durationMs == 0L
                ? 0L
                : saturatingAdd(now, startsInMs);
        long until = durationMs == 0L
                ? 0L
                : saturatingAdd(from, durationMs);

        return new ProxyClientUapState(
                Math.max(lastActivityMs, now),
                lastPingMs,
                nextPingDeadlineMs,
                mainRxUntilMs,
                mainRxWindowMs,
                temporaryRxUntilMs,
                from,
                until,
                generation + 1L
        );
    }

    public boolean isReceiveWindowOpen(long now) {
        return now < mainRxUntilMs
                || now < temporaryRxUntilMs
                || (now >= scheduledRxFromMs
                && now < scheduledRxUntilMs);
    }

    public long elapsedSinceActivity(long now) {
        if (lastActivityMs == 0L
                || now <= lastActivityMs) {
            return 0L;
        }

        return now - lastActivityMs;
    }

    public long nextPingDeltaMs(long now) {
        if (nextPingDeadlineMs == 0L) {
            return 0L;
        }

        if (nextPingDeadlineMs > now) {
            return nextPingDeadlineMs - now;
        }

        long overdue =
                now - nextPingDeadlineMs;

        return overdue == 0L
                ? -1L
                : -overdue;
    }

    public long receiveWindowEndMs() {
        return Math.max(
                Math.max(
                        mainRxUntilMs,
                        temporaryRxUntilMs
                ),
                scheduledRxUntilMs
        );
    }

    public long queueDeadlineMs(
            long now,
            long hardDeadlineMs,
            boolean canSendNow
    ) {
        if (hardDeadlineMs <= now) {
            return 0L;
        }

        if (scheduledRxUntilMs > now) {
            if (hardDeadlineMs <= scheduledRxFromMs) {
                return 0L;
            }

            return Math.min(
                    hardDeadlineMs,
                    scheduledRxUntilMs
            );
        }

        if (nextPingDeadlineMs > now) {
            return Math.min(
                    hardDeadlineMs,
                    nextPingDeadlineMs
            );
        }

        if (!canSendNow) {
            return 0L;
        }

        long receiveWindowEnd =
                receiveWindowEndMs();

        if (receiveWindowEnd <= now) {
            return 0L;
        }

        return Math.min(
                hardDeadlineMs,
                receiveWindowEnd
        );
    }

    private static long saturatingAdd(
            long time,
            long duration
    ) {
        if (duration > Long.MAX_VALUE - time) {
            return Long.MAX_VALUE;
        }

        return time + duration;
    }

    private static void requireNonNegative(
            long value,
            String name
    ) {
        if (value < 0L) {
            throw new IllegalArgumentException(
                    name + " must be non-negative: " + value
            );
        }
    }
}
