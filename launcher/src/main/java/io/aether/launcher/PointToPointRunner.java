package io.aether.launcher;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Runner for the Point-to-Point demo project.
 */
public class PointToPointRunner extends AbstractProjectRunner {
    public PointToPointRunner(LauncherContext ctx) {
        super(ctx, "client-java");
    }

    @Override public String getName() { return "Point-to-Point"; }
    @Override public String getId() { return "pointToPoint"; }
    @Override public String getRepoUrl() { return "https://github.com/aethernetio/client-java.git"; }
    @Override public String getGradleProject() { return ":pointToPoint"; }
    @Override public String getServiceMainClass() { return "PointToPointTest"; }
    @Override public String getEmulatorMainClass() { return null; }
    @Override public String getServiceSourcePath() { return "pointToPoint/src/main/java/io/aether/examples/pointToPoint/PointToPointTest.java"; }
    @Override public String getEmulatorSourcePath() { return null; }
    @Override public String getGuiSourcePath() { return null; }

    // TODO: implement
    @Override public void handleClone(HttpExchange t) throws IOException { }
    @Override public void handleReset(HttpExchange t) throws IOException { }
    @Override public void handleBuild(HttpExchange t) throws IOException { }
    @Override public void handleRun(HttpExchange t) throws IOException { }
    @Override public void handleStop(HttpExchange t) throws IOException { }
    @Override public void handleRunEmulator(HttpExchange t) throws IOException { }
    @Override public void handleStopEmulator(HttpExchange t) throws IOException { }
}