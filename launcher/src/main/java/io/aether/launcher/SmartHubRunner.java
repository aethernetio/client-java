package io.aether.launcher;

import com.sun.net.httpserver.HttpExchange;
import java.nio.file.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;


/**
 * Runner for the Smart Hub demo project.
 * Handles clone, build, run, stop, and emulator operations specific to SmartHub.
 */
public class SmartHubRunner extends AbstractProjectRunner {
    private Process serviceProcess;
    private Process emulatorProcess;
    private String detectedServiceUuid;

    public SmartHubRunner(LauncherContext ctx) {
        super(ctx, "client-java");
    }

    boolean serviceRunning() { return serviceProcess != null && serviceProcess.isAlive(); }
    boolean emulatorRunning() { return emulatorProcess != null && emulatorProcess.isAlive(); }
    String getServiceUuid() { return detectedServiceUuid; }



    @Override
    public String getName() { return "Smart Hub"; }

    @Override
    public String getId() { return "smarthub"; }

    @Override
    public String getRepoUrl() { return "https://github.com/aethernetio/client-java.git"; }

    @Override
    public String getGradleProject() { return ":smarthub"; }

    @Override
    public String getServiceMainClass() { return "SmartHubService"; }

    @Override
    public String getEmulatorMainClass() { return "SmartDeviceEmulator"; }

    @Override
    public String getServiceSourcePath() { return "smarthub/src/main/java/io/aether/smarthub/SmartHubService.java"; }

    @Override
    public String getEmulatorSourcePath() { return "smarthub/src/main/java/io/aether/smarthub/SmartDeviceEmulator.java"; }

    @Override
    public String getGuiSourcePath() { return "smarthub/src/main/resources/smarthub.html"; }

    // TODO: Implement operations using Launcher's infrastructure (SSE, process management)
    @Override public void handleClone(HttpExchange t) throws IOException {
        Path ws = ctx.workspace();
        if (!Files.exists(ws)) Files.createDirectories(ws);
        if (Files.exists(ws.resolve("client-java/.git"))) {
            Launcher.sendOk(t, "{\"status\":\"already_cloned\"}");
            return;
        }
        stopAll();
        detectedServiceUuid = null;
        ctx.broadcast("uuid_clear", "{}");
        String localCmd = RunnerUtils.localCd(ws) + "git clone --progress " + getRepoUrl() + " client-java";
        String dockerCmd = "docker run --rm -v ${HOME}:/host_home -w /host_home ghcr.io/aethernetio/aether-launcher:latest sh -c \"git clone --progress " + getRepoUrl() + " client-java\"";
        ProcessBuilder pb = new ProcessBuilder("git", "clone", "--progress", getRepoUrl(), "client-java");
        pb.directory(ws.toFile());
        RunnerUtils.runCommand(pb, "clone", localCmd, dockerCmd, ctx);
        Launcher.sendOk(t, "{\"status\":\"cloning\"}");
    }

    @Override public void handleReset(HttpExchange t) throws IOException {
        if (!Files.exists(ctx.workspace().resolve("client-java/.git"))) {
            Launcher.sendError(t, 400, "Not cloned");
            return;
        }
        stopAll();
        String safe = "git config --global --add safe.directory " + ctx.workspace().resolve("client-java").toAbsolutePath().toString() + " && git reset --hard HEAD && git clean -fdx";
        String localCmd = RunnerUtils.localCd(ctx.workspace().resolve("client-java")) + safe;
        String dockerCmd = "docker run --rm -v ${HOME}:/host_home -w /host_home/client-java ghcr.io/aethernetio/aether-launcher:latest sh -c \"" + safe + "\"";
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", safe);
        pb.directory(ctx.workspace().resolve("client-java").toFile());
        RunnerUtils.runCommand(pb, "reset", localCmd, dockerCmd, ctx);
        Launcher.sendOk(t, "{\"status\":\"resetting\"}");
    }


    @Override public void handleBuild(HttpExchange t) throws IOException {
        if (Launcher.jdkHome == null || Launcher.gradleHome == null) {
            Launcher.ensureTools();
            Launcher.sendOk(t, "{\"status\":\"waiting_for_tools\"}");
            return;
        }


        ctx.broadcast("log", "{\"stream\":\"build\",\"line\":\"Starting build...\"}");
        Path repo = ctx.workspace().resolve("client-java");
        String gradleBin = Launcher.gradleHome + "/bin/gradle";
        String cmd = gradleBin + " --console=plain " + getGradleProject() + ":build";
        String localCmd = RunnerUtils.localCd(repo) + cmd;
        String dockerCmd = "docker run --rm -v ${HOME}:/host_home -w /host_home/client-java ghcr.io/aethernetio/aether-launcher:latest sh -c \"" + cmd + "\"";
        ProcessBuilder pb = new ProcessBuilder(gradleBin, "--console=plain", getGradleProject() + ":build");
        pb.directory(repo.toFile());
        pb.environment().put("PATH", System.getenv("PATH"));
        pb.environment().put("JAVA_HOME", Launcher.jdkHome);
        RunnerUtils.runCommand(pb, "build", localCmd, dockerCmd, ctx);
        Launcher.sendOk(t, "{\"status\":\"started\"}");
    }



    @Override public void handleRun(HttpExchange t) throws IOException {
        if (Launcher.jdkHome == null || Launcher.gradleHome == null) {
            Launcher.ensureTools();
            Launcher.sendOk(t, "{\"status\":\"waiting_for_tools\"}");
            return;
        }
        ctx.broadcast("log", "{\"stream\":\"run\",\"line\":\"Starting run...\"}");
        stopService();
        Path repo = ctx.workspace().resolve("client-java");
        String gradleBin = Launcher.gradleHome + "/bin/gradle";
        String cmd = gradleBin + " --console=plain " + getGradleProject() + ":run";
        String localCmd = RunnerUtils.localCd(repo) + cmd;
        String dockerCmd = "docker run --rm -v ${HOME}:/host_home -w /host_home/client-java ghcr.io/aethernetio/aether-launcher:latest sh -c \"" + cmd + "\"";
        ProcessBuilder pb = new ProcessBuilder(gradleBin, "--console=plain", getGradleProject() + ":run");
        pb.directory(repo.toFile());
        pb.environment().put("PATH", System.getenv("PATH"));
        pb.environment().put("JAVA_HOME", Launcher.jdkHome);
        serviceProcess = RunnerUtils.runCommand(pb, "run", localCmd, dockerCmd, ctx);
        ctx.broadcast("service_started", "{}");
        Launcher.sendOk(t, "{\"status\":\"started\"}");
    }


    @Override public void handleStop(HttpExchange t) throws IOException {
        stopService();
        Launcher.sendOk(t, "{\"status\":\"stopped\"}");
    }

    @Override public void handleRunEmulator(HttpExchange t) throws IOException {
        if (detectedServiceUuid == null) { Launcher.sendError(t, 400, "No service UUID"); return; }
        stopEmulator();
        Path repo = ctx.workspace().resolve("client-java");
        String gradleBin = Launcher.gradleHome + "/bin/gradle";
        String cmd = gradleBin + " --console=plain -DserviceUid=" + detectedServiceUuid + " " + getGradleProject() + ":runEmulator";
        String localCmd = RunnerUtils.localCd(repo) + cmd;
        String dockerCmd = "docker run --rm -v ${HOME}:/host_home -w /host_home/client-java ghcr.io/aethernetio/aether-launcher:latest sh -c \"" + cmd + "\"";
        ProcessBuilder pb = new ProcessBuilder(gradleBin, "--console=plain", "-DserviceUid=" + detectedServiceUuid, getGradleProject() + ":runEmulator");
        pb.directory(repo.toFile());
        pb.environment().put("PATH", System.getenv("PATH"));
        pb.environment().put("JAVA_HOME", Launcher.jdkHome);
        emulatorProcess = RunnerUtils.runCommand(pb, "emulator", localCmd, dockerCmd, ctx);
        ctx.broadcast("emulator_started", "{}");
        Launcher.sendOk(t, "{\"status\":\"started\"}");
    }

    @Override public void handleStopEmulator(HttpExchange t) throws IOException {
        stopEmulator();
        Launcher.sendOk(t, "{\"status\":\"stopped\"}");
    }


    private void stopService() {
        RunnerUtils.stopProcess(serviceProcess, "service_stopped", ctx);
        serviceProcess = null;
        if (detectedServiceUuid != null) {
            detectedServiceUuid = null;
            ctx.broadcast("uuid_clear", "{}");
        }
    }

    private void stopEmulator() {
        RunnerUtils.stopProcess(emulatorProcess, "emulator_stopped", ctx);
        emulatorProcess = null;
    }

    private void stopAll() {
        stopService();
        stopEmulator();
    }


}