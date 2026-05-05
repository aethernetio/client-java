package io.aether.launcher;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;






/** Abstraction for a tool (JDK, Gradle, Git) that can be found or downloaded. */
public abstract class ToolProvider {
    protected final String name;
    private final List<Consumer<String>> progressListeners = new ArrayList<>();

    protected ToolProvider(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    protected String which(String cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                p.waitFor(3, TimeUnit.SECONDS);
                if (p.exitValue() == 0 && line != null) return line.trim();
            }
        } catch (Exception ignored) {}
        return null;
    }


    public void addProgressListener(Consumer<String> listener) {
        progressListeners.add(listener);
    }

    protected void reportProgress(String message) {
        for (Consumer<String> l : progressListeners) {
            l.accept(message);
        }
    }

    /** Try to find the tool on the system. Returns the path or null. */
    public abstract Path find();

    /** Download and install the tool into the given workspace tools directory. Returns the path or null. */
    public abstract Path download(Path toolsDir) throws Exception;

    /** Return a human-readable status string for UI. */
    public String getStatus() {
        Path found = find();
        if (found != null) return "Found: " + found.toString();
        return "Not found";
    }

    protected static void downloadWithProgress(String url, Path dest, Consumer<Integer> onProgress) throws IOException {
        java.net.URLConnection conn = new java.net.URL(url).openConnection();
        long total = conn.getContentLengthLong();
        long downloaded = 0;
        try (InputStream in = conn.getInputStream();
             OutputStream out = java.nio.file.Files.newOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                downloaded += n;
                if (total > 0) {
                    int pct = (int)(downloaded * 100 / total);
                    onProgress.accept(pct);
                }
            }
        }
    }


}