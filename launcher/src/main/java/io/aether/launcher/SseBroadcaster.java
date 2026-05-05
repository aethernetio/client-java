package io.aether.launcher;

/**
 * Interface for broadcasting SSE events to connected clients.
 */
@FunctionalInterface
public interface SseBroadcaster {
    void broadcast(String event, String data);
}
