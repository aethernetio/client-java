package io.aether.launcher;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Runner for the Chat demo project.
 */
public class ChatRunner extends AbstractProjectRunner {
    public ChatRunner(LauncherContext ctx) {
        super(ctx, "client-java");
    }

    @Override public String getName() { return "Chat"; }
    @Override public String getId() { return "chat"; }
    @Override public String getRepoUrl() { return "https://github.com/aethernetio/client-java.git"; }
    @Override public String getGradleProject() { return ":chat"; }
    @Override public String getServiceMainClass() { return "ChatClient"; }
    @Override public String getEmulatorMainClass() { return null; }
    @Override public String getServiceSourcePath() { return "chat/src/main/java/io/aether/examples/plainChat/ChatClient.java"; }
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