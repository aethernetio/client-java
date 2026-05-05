package io.aether.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JdkProvider extends ToolProvider {
    private static final int REQUIRED_MAJOR = 25;

    public JdkProvider() {
        super("JDK");
    }

    @Override
    public Path find() {
        reportProgress("Searching for JDK " + REQUIRED_MAJOR + "...");

        // 1. Check JAVA_HOME
        // 0. Check workspace/tools first (portable installation)
        Path toolsDir = Launcher.workspace.resolve("tools");
        if (Files.exists(toolsDir)) {
            try (var s = Files.list(toolsDir)) {
                for (Path sub : s.toList()) {
                    if (Files.isDirectory(sub) && sub.getFileName().toString().startsWith("jdk-") && isValidJdk(sub)) {
                        reportProgress("Found JDK in workspace/tools: " + sub);
                        return sub;
                    }
                }
            } catch (IOException e) {
                System.err.println("[JdkProvider] Error listing tools directory: " + e.getMessage());
            }

        }

        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            Path home = Path.of(javaHome);
            if (isValidJdk(home)) {
                reportProgress("Found JDK via JAVA_HOME: " + home);
                return home;
            }
        }

        // 2. Check PATH for javac
        String javacPath = which("javac");
        if (javacPath != null) {
            Path binDir = Path.of(javacPath).getParent();
            if (binDir != null) {
                Path home = binDir.getParent();
                if (home != null && isValidJdk(home)) {
                    reportProgress("Found JDK via PATH: " + home);
                    return home;
                }
            }
        }

        // 3. Search standard directories
        List<Path> searchDirs = new ArrayList<>();
        searchDirs.add(Path.of("/usr/lib/jvm"));
        searchDirs.add(Path.of("/usr/local/lib/jvm"));
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            searchDirs.add(Path.of(userHome, ".sdkman/candidates/java"));
            searchDirs.add(Path.of(userHome, "Downloads"));
        }

        for (Path dir : searchDirs) {
            if (!Files.exists(dir)) continue;
            try (var stream = Files.list(dir)) {
                for (Path sub : stream.toList()) {
                    if (Files.isDirectory(sub) && isValidJdk(sub)) {
                        reportProgress("Found JDK 25 at " + sub);
                        return sub;
                    }
                }
            } catch (IOException e) {
                System.err.println("[JdkProvider] Error listing standard directory: " + e.getMessage());
            }

        }

        reportProgress("JDK " + REQUIRED_MAJOR + " not found locally.");
        return null;
    }

    @Override
    public Path download(Path toolsDir) throws Exception {
        reportProgress("Downloading JDK " + REQUIRED_MAJOR + "...");
        Files.createDirectories(toolsDir);
        String url = "https://api.adoptium.net/v3/binary/latest/" + REQUIRED_MAJOR +
                     "/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk";
        Path archive = toolsDir.resolve("jdk.tar.gz");
        downloadWithProgress(url, archive, pct -> reportProgress("Downloading... " + pct + "%"));
        reportProgress("Extracting JDK...");
        ProcessBuilder pb = new ProcessBuilder("tar", "xzf", archive.toString(), "-C", toolsDir.toString());
        pb.inheritIO();
        Process p = pb.start();
        p.waitFor();
        // Find extracted directory
        try (var s = Files.list(toolsDir)) {
            for (Path e : s.toList()) {
                if (Files.isDirectory(e) && e.getFileName().toString().startsWith("jdk-" + REQUIRED_MAJOR)) {
                    reportProgress("JDK installed to " + e);
                    return e;
                }
            }
        }
        throw new IOException("Failed to locate extracted JDK directory");
    }

    private boolean isValidJdk(Path home) {
        Path javac = home.resolve("bin/javac");
        if (!Files.exists(javac)) return false;
        try {
            ProcessBuilder pb = new ProcessBuilder(javac.toString(), "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("javac " + REQUIRED_MAJOR)) return true;
                }
            }
            p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("[JdkProvider] Error checking javac version: " + e.getMessage());
        }

        return false;
    }

    protected String which(String cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line = r.readLine();
                p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
                if (p.exitValue() == 0 && line != null) return line.trim();
            }
        } catch (Exception e) {
            System.err.println("[JdkProvider] Error running which: " + e.getMessage());
        }

        return null;
    }
}