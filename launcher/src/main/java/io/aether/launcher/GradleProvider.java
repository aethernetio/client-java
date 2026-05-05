package io.aether.launcher;

import java.io.*;
import java.net.URL;
import java.nio.file.*;

public class GradleProvider extends ToolProvider {
    public GradleProvider() { super("Gradle"); }

    @Override
    public Path find() {
        reportProgress("Searching for Gradle...");
        // 0. Check workspace/tools first (portable installation)
        Path toolsDir = Path.of(System.getProperty("user.home"), "aether_projects", "tools");
        if (Files.exists(toolsDir)) {
            try (var s = Files.list(toolsDir)) {
                for (Path sub : s.toList()) {
                    if (Files.isDirectory(sub) && sub.getFileName().toString().startsWith("gradle-")) {
                        Path bin = sub.resolve("bin/gradle");
                        if (Files.exists(bin)) {
                            reportProgress("Found Gradle in workspace/tools: " + sub);
                            return sub;
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("[GradleProvider] Error listing tools directory: " + e.getMessage());
            }

        }

        String p = which("gradle");
        if (p != null) {
            Path bin = Path.of(p).getParent();
            if (bin != null && Files.exists(bin.resolve("gradle"))) {
                Path home = bin.getParent();
                if (home != null) {
                    reportProgress("Found Gradle via PATH: " + home);
                    return home;
                }
            }
        }
        Path sdk = Path.of(System.getProperty("user.home"), ".sdkman/candidates/gradle");
        if (Files.exists(sdk)) {
            try (var s = Files.list(sdk)) {
                var dirs = s.filter(Files::isDirectory).sorted(java.util.Comparator.reverseOrder()).toList();
                if (!dirs.isEmpty()) {
                    Path latest = dirs.get(0);
                    reportProgress("Found Gradle via SDKMAN: " + latest);
                    return latest;
                }
            } catch (IOException ignored) {}
        }
        reportProgress("Gradle not found locally.");
        return null;
    }

    @Override
    public Path download(Path toolsDir) throws Exception {
        reportProgress("Downloading Gradle 9.1.0...");
        Files.createDirectories(toolsDir);

        String url = "https://services.gradle.org/distributions/gradle-9.1.0-bin.zip";
        Path archive = toolsDir.resolve("gradle.zip");
        downloadWithProgress(url, archive, pct -> reportProgress("Downloading... " + pct + "%"));

        Path destDir = toolsDir.resolve("gradle-temp");
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new FileInputStream(archive.toFile()))) {
            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = destDir.resolve(entry.getName());
                if (entry.isDirectory()) Files.createDirectories(entryPath);
                else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
        try (var s = Files.list(destDir)) {
            for (Path e : s.toList()) {
                if (Files.isDirectory(e) && e.getFileName().toString().startsWith("gradle-")) {
                    Path target = toolsDir.resolve(e.getFileName());
                    Files.move(e, target, StandardCopyOption.REPLACE_EXISTING);
                    deleteRecursive(destDir);
                    reportProgress("Gradle installed to " + target);
                    return target;
                }
            }
        }
        throw new IOException("Failed to locate extracted Gradle directory");
    }

    private void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var s = Files.list(path)) {
                for (Path child : s.toList()) deleteRecursive(child);
            }
        }
        Files.deleteIfExists(path);
    }
}