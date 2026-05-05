package io.aether.launcher;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for project runners to start and monitor external processes.
 */
public class RunnerUtils {

    /** Starts a process, streams its output (stdout+stderr) to SSE, and broadcasts command start/end. */
    public static Process runCommand(ProcessBuilder pb, String stream, String localCmd, String dockerCmd,
                                     LauncherContext ctx) throws IOException {
        ctx.broadcaster().broadcast("command_start",
            "{\"stream\":\"" + stream + "\",\"localCmd\":\"" + escapeJson(localCmd) + "\",\"dockerCmd\":\"" + escapeJson(dockerCmd) + "\"}");
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        ctx.executor().submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ctx.broadcaster().broadcast("log",
                        "{\"stream\":\"" + stream + "\",\"line\":\"" + escapeJson(line) + "\"}");
                    // UUID detection for SmartHub
                    if ("run".equals(stream) && line.contains("started with UUID:")) {
                        int idx = line.indexOf("started with UUID:");
                        if (idx >= 0) {
                            String rest = line.substring(idx + "started with UUID:".length()).trim();
                            String uuid = rest.split("\\s+")[0];
                            ctx.broadcaster().broadcast("uuid_found", "{\"uuid\":\"" + uuid + "\"}");
                        }
                    }
                }
                int exitCode = proc.waitFor();
                ctx.broadcaster().broadcast("command_end",
                    "{\"stream\":\"" + stream + "\",\"exitCode\":" + exitCode + "}");
                if ("clone".equals(stream) && exitCode == 0) {
                    ctx.broadcaster().broadcast("clone_done", "{}");
                }
            } catch (IOException | InterruptedException e) {
                ctx.broadcaster().broadcast("error", "{\"stream\":\"" + stream + "\",\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }

        });
        return proc;
    }

    public static void stopProcess(Process proc, String event, LauncherContext ctx) {
        if (proc != null && proc.isAlive()) {
            proc.destroyForcibly();
            ctx.broadcaster().broadcast(event, "{}");
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static String localCd(Path dir) {
        return "cd " + dir.toAbsolutePath() + " && ";
    }
}