package io.aether.launcher;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Defines the lifecycle operations for a demo project.
 * Each demo project (SmartHub, Chat, Point-to-Point) provides its own implementation.
 */
public interface ProjectRunner {

    /** Display name shown in UI, e.g. "Smart Hub". */
    String getName();

    /** Project identifier used in API paths, e.g. "smarthub". */
    String getId();

    /** URL of the Git repository to clone. */
    String getRepoUrl();

    Path getRepoPath();

    /** Gradle subproject name, e.g. ":smarthub". */
    String getGradleProject();

    /** Main class for the service (if any), used to detect UUID, etc. */
    String getServiceMainClass();

    /** Main class for the emulator (if any), or null if not applicable. */
    String getEmulatorMainClass();

    /** Called when the user clicks Clone/Reset. */
    void handleClone(HttpExchange t) throws IOException;

    /** Called when the user clicks Reset. */
    void handleReset(HttpExchange t) throws IOException;

    /** Called when the user clicks Build. */
    void handleBuild(HttpExchange t) throws IOException;

    /** Called when the user clicks Run. */
    void handleRun(HttpExchange t) throws IOException;

    /** Called when the user clicks Stop. */
    void handleStop(HttpExchange t) throws IOException;

    /** Called when the user clicks Emulator. */
    void handleRunEmulator(HttpExchange t) throws IOException;

    /** Called when the user clicks Stop Emulator. */
    void handleStopEmulator(HttpExchange t) throws IOException;

    /** Returns a path to the service source file (relative to repository root) for display in UI. */
    String getServiceSourcePath();

    /** Returns a path to the emulator source file (relative to repository root) for display in UI. */
    String getEmulatorSourcePath();

    /** Returns a path to the GUI source file (relative to repository root) for display in UI. */
    String getGuiSourcePath();
}