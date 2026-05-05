package io.aether.launcher;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility methods for project runners to start and monitor external processes.
 */
public class RunnerUtils {

    /** Starts a process, streams its output (stdout+stderr) to SSE, and broadcasts command start/end. */

    public static Process runCommand(ProcessBuilder pb, String stream, String localCmd,
                                     LauncherContext ctx, AtomicReference<String> uuidRef, String friendlyName) throws IOException {
        ctx.broadcaster().broadcast("command_start",
            buildJsonObj("stream", stream, "localCmd", localCmd, "friendlyName", friendlyName));
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        ctx.executor().submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    ctx.broadcaster().broadcast("log",
                        buildJsonObj("stream", stream, "line", line));
                    if ("run".equals(stream) && line.contains("started with UUID:")) {
                        int idx = line.indexOf("started with UUID:");
                        if (idx >= 0) {
                            String rest = line.substring(idx + "started with UUID:".length()).trim();
                            String uuid = rest.split("\\s+")[0];
                            ctx.broadcaster().broadcast("uuid_found",
                                buildJsonObj("uuid", uuid));
                            if (uuidRef != null) uuidRef.set(uuid);
                        }
                    }
                }
                int exitCode = proc.waitFor();
                ctx.broadcaster().broadcast("command_end",
                    buildJsonObj("stream", stream, "exitCode", String.valueOf(exitCode)));
                if ("clone".equals(stream) && exitCode == 0) {
                    ctx.broadcaster().broadcast("clone_done", "{}");
                }
            } catch (IOException | InterruptedException e) {
                ctx.broadcaster().broadcast("error",
                    buildJsonObj("stream", stream, "error", e.getMessage()));
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


    public static String buildJsonObj(String... keysAndValues) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keysAndValues.length; i += 2) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(keysAndValues[i])).append("\":\"").append(escapeJson(keysAndValues[i+1])).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String exportCmd(String var, String value) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "set " + var + "=" + value;
        } else {
            return "export " + var + "=" + value;
        }
    }



    public static String localCd(Path dir) {
        return "cd " + dir.toAbsolutePath() + " && ";
    }
}