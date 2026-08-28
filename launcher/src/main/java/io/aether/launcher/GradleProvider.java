
package io.aether.launcher;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class GradleProvider extends ToolProvider {
    private static final String DOWNLOAD_VERSION = "9.1.0";
    private static final int REQUIRED_MAJOR = 9;
    private static final int REQUIRED_MINOR = 1;

    public GradleProvider() {
        super("Gradle");
    }

    @Override
    public Path find() {
        reportProgress(
                "Searching for compatible Gradle >= "
                        + REQUIRED_MAJOR
                        + "."
                        + REQUIRED_MINOR
                        + "...");

        Path toolsDir =
                Launcher.workspace.resolve("tools");

        Path preferred =
                toolsDir.resolve(
                        "gradle-" + DOWNLOAD_VERSION);

        if (isCompatibleGradle(preferred)) {
            reportProgress(
                    "Found Gradle in workspace/tools: "
                            + preferred);
            return preferred;
        }

        if (Files.exists(toolsDir)) {
            try (var stream = Files.list(toolsDir)) {
                var dirs =
                        stream
                                .filter(Files::isDirectory)
                                .filter(path ->
                                        path.getFileName()
                                                .toString()
                                                .startsWith("gradle-"))
                                .sorted(
                                        Comparator.comparing(
                                                        (Path path) ->
                                                                path.getFileName()
                                                                        .toString())
                                                .reversed())
                                .toList();

                for (Path candidate : dirs) {
                    if (candidate.equals(preferred)) {
                        continue;
                    }

                    if (isCompatibleGradle(candidate)) {
                        reportProgress(
                                "Found compatible Gradle in workspace/tools: "
                                        + candidate);
                        return candidate;
                    }
                }
            } catch (IOException e) {
                System.err.println(
                        "[GradleProvider] Error listing tools directory: "
                                + e.getMessage());
            }
        }

        String gradlePath =
                which("gradle");

        if (gradlePath != null) {
            Path bin =
                    Path.of(gradlePath)
                            .getParent();

            if (bin != null) {
                Path home =
                        bin.getParent();

                if (isCompatibleGradle(home)) {
                    reportProgress(
                            "Found compatible Gradle via PATH: "
                                    + home);
                    return home;
                }
            }
        }

        Path sdk =
                Path.of(
                        System.getProperty("user.home"),
                        ".sdkman/candidates/gradle");

        if (Files.exists(sdk)) {
            try (var stream = Files.list(sdk)) {
                var dirs =
                        stream
                                .filter(Files::isDirectory)
                                .sorted(
                                        Comparator.comparing(
                                                        (Path path) ->
                                                                path.getFileName()
                                                                        .toString())
                                                .reversed())
                                .toList();

                for (Path candidate : dirs) {
                    if (isCompatibleGradle(candidate)) {
                        reportProgress(
                                "Found compatible Gradle via SDKMAN: "
                                        + candidate);
                        return candidate;
                    }
                }
            } catch (IOException e) {
                System.err.println(
                        "[GradleProvider] Error listing SDKMAN Gradle directory: "
                                + e.getMessage());
            }
        }

        reportProgress(
                "Compatible Gradle not found locally.");

        return null;
    }

    private boolean isCompatibleGradle(
            Path home) {

        if (home == null) {
            return false;
        }

        Path gradle =
                home.resolve("bin/gradle");

        if (!Files.exists(gradle)) {
            return false;
        }

        String version =
                detectGradleVersion(
                        home,
                        gradle);

        if (version == null) {
            reportProgress(
                    "Ignoring Gradle with unknown version: "
                            + home);
            return false;
        }

        if (isSupportedVersion(version)) {
            return true;
        }

        reportProgress(
                "Ignoring incompatible Gradle "
                        + version
                        + " at "
                        + home
                        + "; need >= "
                        + REQUIRED_MAJOR
                        + "."
                        + REQUIRED_MINOR);

        return false;
    }

    private String detectGradleVersion(
            Path home,
            Path gradle) {

        String version =
                versionFromPath(home);

        if (version != null) {
            return version;
        }

        try {
            Path realHome =
                    home.toRealPath();

            version =
                    versionFromPath(realHome);

            if (version != null) {
                return version;
            }
        } catch (IOException ignored) {
        }

        try {
            ProcessBuilder pb =
                    new ProcessBuilder(
                            gradle.toString(),
                            "--version");

            String jdkHome =
                    Launcher.jdkHome;

            if (jdkHome != null
                    && !jdkHome.isBlank()) {

                pb.environment()
                        .put(
                                "JAVA_HOME",
                                jdkHome);
            }

            pb.redirectErrorStream(true);

            Process process =
                    pb.start();

            String detected = null;

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         process.getInputStream()))) {

                String line;

                while ((line =
                                reader.readLine())
                        != null) {

                    line = line.trim();

                    if (line.startsWith("Gradle ")) {
                        detected =
                                line.substring(
                                                "Gradle ".length())
                                        .trim();
                        break;
                    }
                }
            }

            if (!process.waitFor(
                    10,
                    TimeUnit.SECONDS)) {

                process.destroyForcibly();
                return null;
            }

            return detected;
        } catch (Exception e) {
            return null;
        }
    }

    private String versionFromPath(
            Path home) {

        Path fileName =
                home.getFileName();

        if (fileName == null) {
            return null;
        }

        String name =
                fileName.toString();

        if (name.startsWith("gradle-")) {
            name =
                    name.substring(
                            "gradle-".length());
        }

        int end = 0;

        while (end < name.length()) {
            char c =
                    name.charAt(end);

            if (!Character.isDigit(c)
                    && c != '.') {
                break;
            }

            end++;
        }

        if (end == 0) {
            return null;
        }

        String version =
                name.substring(
                        0,
                        end);

        while (version.endsWith(".")) {
            version =
                    version.substring(
                            0,
                            version.length() - 1);
        }

        return version.contains(".")
                ? version
                : null;
    }

    private boolean isSupportedVersion(
            String version) {

        try {
            String[] parts =
                    version.split("\\.");

            int major =
                    Integer.parseInt(
                            parts[0]);

            int minor =
                    parts.length > 1
                            ? Integer.parseInt(
                                    parts[1])
                            : 0;

            return major > REQUIRED_MAJOR
                    || (major == REQUIRED_MAJOR
                    && minor >= REQUIRED_MINOR);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public Path download(
            Path toolsDir)
            throws Exception {


        reportProgress(
                "Downloading Gradle "
                        + DOWNLOAD_VERSION
                        + "...");

        Files.createDirectories(
                toolsDir);


        String primaryUrl =
                "https://repo.huaweicloud.com/gradle/gradle-"
                        + DOWNLOAD_VERSION
                        + "-bin.zip";

        String fallbackUrl =
                "https://services.gradle.org/distributions/gradle-"
                        + DOWNLOAD_VERSION
                        + "-bin.zip";


        Path archive =
                toolsDir.resolve(
                        "gradle.zip");

        try {
            downloadWithProgress(
                    primaryUrl,
                    archive,
                    pct ->
                            reportProgress(
                                    "Downloading... "
                                            + pct
                                            + "%"));
        } catch (IOException primaryError) {
            Files.deleteIfExists(
                    archive);

            reportProgress(
                    "Primary Gradle download failed, trying fallback...");

            try {
                downloadWithProgress(
                        fallbackUrl,
                        archive,
                        pct ->
                                reportProgress(
                                        "Downloading fallback... "
                                                + pct
                                                + "%"));
            } catch (IOException fallbackError) {
                Files.deleteIfExists(
                        archive);

                fallbackError.addSuppressed(
                        primaryError);

                throw fallbackError;
            }
        }

        String expectedSha256 =
                "a17ddd85a26b6a7f5ddb71ff8b05fc5104c0202c6e64782429790c933686c806";

        java.security.MessageDigest digest =
                java.security.MessageDigest.getInstance(
                        "SHA-256");

        try (java.io.InputStream in =
                     Files.newInputStream(
                             archive)) {

            byte[] buffer =
                    new byte[8192];

            int read;

            while ((read = in.read(buffer)) != -1) {
                digest.update(
                        buffer,
                        0,
                        read);
            }
        }

        String actualSha256 =
                java.util.HexFormat.of()
                        .formatHex(
                                digest.digest());

        if (!expectedSha256.equalsIgnoreCase(
                actualSha256)) {

            Files.deleteIfExists(
                    archive);

            throw new IOException(
                    "Gradle "
                            + DOWNLOAD_VERSION
                            + " archive checksum mismatch: "
                            + actualSha256);
        }

        reportProgress(
                "Gradle archive verified.");


        Path destDir =
                toolsDir.resolve(
                        "gradle-temp");

        deleteRecursive(
                destDir);

        Files.createDirectories(
                destDir);

        try (java.util.zip.ZipInputStream zis =
                     new java.util.zip.ZipInputStream(
                             new FileInputStream(
                                     archive.toFile()))) {

            java.util.zip.ZipEntry entry;

            while ((entry =
                            zis.getNextEntry())
                    != null) {

                Path entryPath =
                        destDir.resolve(
                                        entry.getName())
                                .normalize();

                if (!entryPath.startsWith(destDir)) {
                    throw new IOException(
                            "Invalid Gradle archive entry: "
                                    + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(
                            entryPath);
                } else {
                    Files.createDirectories(
                            entryPath.getParent());

                    Files.copy(
                            zis,
                            entryPath,
                            StandardCopyOption.REPLACE_EXISTING);
                }

                zis.closeEntry();
            }
        }

        try (var stream =
                     Files.list(destDir)) {

            for (Path extracted :
                    stream.toList()) {

                if (!Files.isDirectory(extracted)
                        || !extracted
                                .getFileName()
                                .toString()
                                .startsWith("gradle-")) {
                    continue;
                }

                Path target =
                        toolsDir.resolve(
                                extracted.getFileName());

                deleteRecursive(
                        target);

                Files.move(
                        extracted,
                        target,
                        StandardCopyOption.REPLACE_EXISTING);

                Path gradle =
                        target.resolve(
                                "bin/gradle");

                if (!gradle
                        .toFile()
                        .setExecutable(true)) {

                    throw new IOException(
                            "Failed to make Gradle executable: "
                                    + gradle);
                }

                deleteRecursive(
                        destDir);

                Files.deleteIfExists(
                        archive);

                reportProgress(
                        "Gradle installed to "
                                + target);

                return target;
            }
        }

        throw new IOException(
                "Failed to locate extracted Gradle directory");
    }

    private void deleteRecursive(
            Path path)
            throws IOException {

        if (!Files.exists(path)) {
            return;
        }

        if (Files.isDirectory(path)) {
            try (var stream =
                         Files.list(path)) {

                for (Path child :
                        stream.toList()) {

                    deleteRecursive(
                            child);
                }
            }
        }

        Files.deleteIfExists(path);
    }
}