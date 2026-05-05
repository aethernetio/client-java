package io.aether.launcher;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

/**
 * Provides access to core launcher services for project runners.
 */
public record LauncherContext(
    Path workspace,
    Path repoPath,
    ExecutorService executor,
    SseBroadcaster broadcaster
) {
    public void broadcast(String event, String data) {
        broadcaster.broadcast(event, data);
    }
}
