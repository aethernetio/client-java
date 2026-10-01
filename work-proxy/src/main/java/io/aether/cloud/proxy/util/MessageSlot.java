
package io.aether.cloud.proxy.util;

import io.aether.logger.Log;
import io.aether.utils.AString;
import io.aether.utils.CTypeI;
import io.aether.utils.ToString;

import java.lang.invoke.VarHandle;
import java.util.UUID;

/**
 * Local copy of {@code io.aether.utils.MessageSlot} (originally in
 * server-core) used by the proxy message grid. Semantics are unchanged.
 */
public class MessageSlot implements ToString {
    private static final VarHandle STATE_FIELD =
            CTypeI.of(MessageSlot.class).getFieldVarHandle("state");
    private final Grid grid;
    private final int index;
    public volatile UUID from;
    public volatile UUID to;
    public volatile byte[] data;
    public volatile int timeDead;

    public volatile long uapDeadlineMs;
    public volatile long uapGeneration;

    volatile State state = State.FREE;

    public MessageSlot(Grid grid, int index) {
        this.grid = grid;
        this.index = index;
    }

    @Override
    public AString toAString(AString sb) {
        UUID currentFrom = from;
        UUID currentTo = to;
        byte[] currentData = data;

        sb.add("(i:")
                .add(index)
                .add(", f:")
                .add(currentFrom)
                .add(", t:")
                .add(currentTo)
                .add(", msg:");

        if (currentData == null) {
            sb.add("null");
        } else {
            sb.add(currentData);
        }

        return sb.add(")");
    }

    @Override
    public String toString() {
        return toString2();
    }

    public UUID from() {
        return from;
    }

    public UUID to() {
        return to;
    }

    public int size() {
        byte[] currentData = data;
        return currentData == null ? 0 : currentData.length;
    }

    public boolean trySetState(State oldState, State newState) {
        return STATE_FIELD.compareAndSet(this, oldState, newState);
    }

    public boolean trySend(int currentTime) {
        if (currentTime > timeDead) {
            return false;
        }
        return trySetState(State.TO_SEND, State.LOCK_FOR_SEND);
    }

    public boolean free() {
        while (true) {
            State current = state;

            if (current == State.FREE
                    || current == State.COMPLETING) {
                return false;
            }

            if (trySetState(
                    current,
                    State.COMPLETING
            )) {
                break;
            }
        }

        grid.clearBit(index);

        data = null;
        from = null;
        to = null;
        timeDead = 0;
        uapDeadlineMs = 0L;
        uapGeneration = 0L;
        state = State.FREE;

        return true;
    }

    public boolean tryLoad(
            UUID from,
            UUID to,
            byte[] data,
            int timeDead
    ) {
        if (trySetState(
                State.FREE,
                State.LOCK_FOR_LOAD
        )) {
            this.from = from;
            this.to = to;
            this.data = data;
            this.timeDead = timeDead;
            this.uapDeadlineMs = Long.MAX_VALUE;
            this.uapGeneration = -1L;

            state = State.TO_SEND;
            grid.setBit(index);

            return true;
        }

        return false;
    }

    public void recoveryToSend() {
        trySetState(State.LOCK_FOR_SEND, State.TO_SEND);
    }

    public boolean tryLock() {
        return trySetState(State.FREE, State.LOCK_FOR_LOAD);
    }

    public boolean checkTimeout(int time) {
        if (time <= timeDead) {
            return false;
        }

        if (trySetState(State.TO_SEND, State.TIMED_OUT)) {
            return true;
        }

        if (time > timeDead + 10
                && trySetState(
                        State.LOCK_FOR_SEND,
                        State.TIMED_OUT
                )) {
            Log.warn(
                    "send message timeout $fromUid -> $toUid",
                    "fromUid",
                    from,
                    "toUid",
                    to
            );
            return true;
        }

        return false;
    }

    public enum State {
        FREE,
        LOCK_FOR_LOAD,
        LOCK_FOR_SEND,
        TO_SEND,
        TIMED_OUT,
        COMPLETING
    }
}
