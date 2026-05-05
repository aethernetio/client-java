package io.aether.launcher;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class Launcher {
    static final int PORT = Integer.getInteger("port", 29383);
    static final ExecutorService exec = Executors.newCachedThreadPool();
    static final List<SseConnection> sseConnections = Collections.synchronizedList(new ArrayList<>());
    static Path workspace = Path.of(System.getProperty("user.home"), "aether_projects");

    static String displayPath() {
        return workspace.toAbsolutePath().toString();
    }



    static class SseConnection {
        final HttpExchange exchange;
        volatile boolean closed;
        SseConnection(HttpExchange t) { this.exchange = t; this.closed = false; }
        synchronized void send(String event, String data) {
            try {
                OutputStream os = exchange.getResponseBody();
                if (event != null && !event.isEmpty()) os.write(("event:" + event + "\n").getBytes());
                os.write(("data:" + data + "\n\n").getBytes());
                os.flush();
            } catch (IOException e) { closed = true; }
        }
    }

    static void broadcast(String event, String data) {
        for (SseConnection c : sseConnections) c.send(event, data);
    }

    /** Try to open a URL in the system browser using OS-specific commands. */
    static void openBrowser(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String[] cmd;
            if (os.contains("linux")) {
                cmd = new String[]{"xdg-open", url};
            } else if (os.contains("mac")) {
                cmd = new String[]{"open", url};
            } else if (os.contains("windows")) {
                cmd = new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
            } else {
                // fallback
                System.out.println("Please open " + url + " manually.");
                return;
            }
            new ProcessBuilder(cmd).start();
        } catch (Exception e) {
            System.out.println("Could not open browser automatically: " + e.getMessage());
            System.out.println("Please open " + url + " manually.");
        }
    }

    /** Open a file/folder in the system file manager (future use). */
    static void openPath(java.nio.file.Path path) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String[] cmd;
            if (os.contains("linux")) {
                cmd = new String[]{"xdg-open", path.toString()};
            } else if (os.contains("mac")) {
                cmd = new String[]{"open", path.toString()};
            } else if (os.contains("windows")) {
                cmd = new String[]{"explorer", path.toString()};
            } else {
                System.out.println("Cannot open path: " + path);
                return;
            }
            new ProcessBuilder(cmd).start();
        } catch (Exception e) {
            System.out.println("Could not open path: " + e.getMessage());
        }
    }





    // -- Project Runners --
    static final Map<String, ProjectRunner> runners = new java.util.LinkedHashMap<>();
    static {
        LauncherContext ctx = new LauncherContext(workspace, null, exec, new SseBroadcaster() {
            @Override public void broadcast(String event, String data) {
                Launcher.broadcast(event, data);
            }
        });
        runners.put("smarthub", new SmartHubRunner(ctx));
        // placeholder for chat, pointToPoint
    }

    static ProjectRunner activeRunner = runners.get("smarthub");



    static ProjectRunner getActiveRunner() {
        String project = System.getProperty("activeProject", "smarthub");
        return runners.getOrDefault(project, runners.get("smarthub"));
    }

    // -- JDK Provider --
    static final JdkProvider jdkProvider = new JdkProvider();
    static {

        jdkProvider.addProgressListener(msg -> {
            if (msg.startsWith("Downloading...")) {
                String pct = msg.substring(msg.indexOf("...")+3).trim().replace("%","");
                broadcast("download_progress", RunnerUtils.buildJsonObj("tool","jdk","percent", pct));
            } else {
                broadcast("progress", RunnerUtils.buildJsonObj("tool","jdk","message", msg));
            }
        });

    }
    static volatile String jdkHome;

    static void ensureJdk() {
        if (jdkHome != null) {
            broadcast("jdk_ready", "{\"path\":\"" + escapeJson(jdkHome) + "\"}");
            return;
        }
        exec.submit(() -> {
            Path found = jdkProvider.find();
            if (found != null) {
                jdkHome = found.toString();
                broadcast("jdk_ready", "{\"path\":\"" + escapeJson(jdkHome) + "\"}");
                return;
            }
            try {
                Path downloaded = jdkProvider.download(workspace.resolve("tools"));
                jdkHome = downloaded.toString();
                broadcast("jdk_ready", "{\"path\":\"" + escapeJson(jdkHome) + "\"}");
            } catch (Exception e) {
                broadcast("error", "{\"error\":\"Failed to download JDK: " + escapeJson(e.getMessage()) + "\"}");
                jdkHome = "";
                broadcast("jdk_ready", "{\"path\":\"\"}");
            }
        });
    }

    static void handleEnsureJdk(HttpExchange t) throws IOException {
        sendOk(t, "{\"status\":\"started\"}");
        ensureJdk();
    }

    static void handleSetJdkPath(HttpExchange t) throws IOException {
        String path = new String(t.getRequestBody().readAllBytes()).trim();
        if (path.isEmpty()) { jdkHome = null; sendOk(t, "{\"status\":\"ok\",\"jdkHome\":null}"); return; }
        Path p = Path.of(path);
        if (Files.exists(p.resolve("bin/javac"))) {
            jdkHome = p.toString();
            sendOk(t, "{\"status\":\"ok\",\"jdkHome\":\"" + escapeJson(jdkHome) + "\"}");
        } else {
            sendError(t, 400, "Invalid JDK directory (no bin/javac found)");
        }
    }

    static void handleGetJdkStatus(HttpExchange t) throws IOException {
        String json = "{\"jdkHome\":\"" + (jdkHome != null ? escapeJson(jdkHome) : "") + "\",\"status\":\"" + jdkProvider.getStatus() + "\"}";
        sendOk(t, json);
    }

    static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }


    // -- Gradle Provider (similar) --
    static final GradleProvider gradleProvider = new GradleProvider();

    static void handleDownloadJdk(HttpExchange t) throws IOException {
        sendOk(t, "{\"status\":\"started\"}");
        exec.submit(() -> {
            try {
                Path downloaded = jdkProvider.download(workspace.resolve("tools"));
                jdkHome = downloaded.toString();
                broadcast("jdk_ready", "{\"path\":\"" + escapeJson(jdkHome) + "\"}");
            } catch (Exception e) {
                broadcast("error", "{\"error\":\"Failed to download JDK: " + escapeJson(e.getMessage()) + "\"}");
            }
        });
    }

    static void handleDownloadGradle(HttpExchange t) throws IOException {
        sendOk(t, "{\"status\":\"started\"}");
        exec.submit(() -> {
            try {
                Path downloaded = gradleProvider.download(workspace.resolve("tools"));
                gradleHome = downloaded.toString();
                broadcast("gradle_ready", "{\"path\":\"" + escapeJson(gradleHome) + "\"}");
            } catch (Exception e) {
                broadcast("error", "{\"error\":\"Failed to download Gradle: " + escapeJson(e.getMessage()) + "\"}");
            }
        });
    }

    static void handleOpenPath(HttpExchange t) throws IOException {
        String pathStr = new String(t.getRequestBody().readAllBytes()).trim();
        Path p = Path.of(pathStr);
        openPath(p);
        sendOk(t, "{\"status\":\"ok\"}");
    }


    static void ensureTools() {
        if (jdkHome == null || gradleHome == null) {
            if (jdkHome == null) ensureJdk();
            if (gradleHome == null) ensureGradle();
        }
    }

    static {
        gradleProvider.addProgressListener(msg -> broadcast("progress", "{\"tool\":\"gradle\",\"message\":\"" + escapeJson(msg) + "\"}"));
    }
    static volatile String gradleHome;

    static void ensureGradle() {
        if (gradleHome != null) {
            broadcast("gradle_ready", "{\"path\":\"" + escapeJson(gradleHome) + "\"}");
            return;
        }
        exec.submit(() -> {
            Path found = gradleProvider.find();
            if (found != null) {
                gradleHome = found.toString();
                broadcast("gradle_ready", "{\"path\":\"" + escapeJson(gradleHome) + "\"}");
                return;
            }
            try {
                Path downloaded = gradleProvider.download(workspace.resolve("tools"));
                gradleHome = downloaded.toString();
                broadcast("gradle_ready", "{\"path\":\"" + escapeJson(gradleHome) + "\"}");
            } catch (Exception e) {
                broadcast("error", "{\"error\":\"Failed to download Gradle: " + escapeJson(e.getMessage()) + "\"}");
                gradleHome = "";
                broadcast("gradle_ready", "{\"path\":\"\"}");
            }
        });
    }

    static void handleEnsureGradle(HttpExchange t) throws IOException {
        sendOk(t, "{\"status\":\"started\"}");
        ensureGradle();
    }
    static void handleSetGradlePath(HttpExchange t) throws IOException {
        String path = new String(t.getRequestBody().readAllBytes()).trim();
        if (path.isEmpty()) { gradleHome = null; sendOk(t, "{\"status\":\"ok\",\"gradleHome\":null}"); return; }
        Path p = Path.of(path);
        if (Files.exists(p.resolve("bin/gradle"))) {
            gradleHome = p.toString();
            sendOk(t, "{\"status\":\"ok\",\"gradleHome\":\"" + escapeJson(gradleHome) + "\"}");
        } else sendError(t, 400, "Invalid Gradle directory (no bin/gradle found)");
    }
    static void handleGetGradleStatus(HttpExchange t) throws IOException {
        String json = "{\"gradleHome\":\"" + (gradleHome != null ? escapeJson(gradleHome) : "") + "\",\"status\":\"" + gradleProvider.getStatus() + "\"}";
        sendOk(t, json);
    }

    // -- Template Engine for UI --
    static void handleIndex(HttpExchange t) throws IOException {
        java.util.Map<String, String> vars = new java.util.HashMap<>();
        vars.put("title", "Aether Launcher");
        vars.put("jdkStatus", jdkProvider.getStatus());
        vars.put("gradleStatus", gradleProvider.getStatus());
        vars.put("workspace", displayPath());
        StringBuilder options = new StringBuilder();
        for (ProjectRunner r : runners.values()) {
            options.append("<option value=\"").append(r.getId()).append("\">").append(r.getName()).append("</option>");
        }
        vars.put("projectOptions", options.toString());
        // Emulator buttons only for SmartHub
        boolean hasEmu = getActiveRunner() instanceof SmartHubRunner;
        vars.put("emulatorButtons", hasEmu ? "<button id=\"emulatorBtn\" onclick=\"api('run-emulator')\">Start Emulator</button>" : "");

        String sourceLinks = "";
        ProjectRunner active = getActiveRunner();
        if (active.getServiceSourcePath() != null) {
            sourceLinks += "<li><span class=\"src-label\">Service:</span> <span class=\"src-desc\">" + active.getName() + " backend</span> <code id=\"srcServicePath\" onclick=\"navigator.clipboard.writeText(this.textContent)\" style=\"cursor:pointer; color:#2a5db0;\" title=\"Click to copy\"></code></li>";
        }
        if (active.getEmulatorSourcePath() != null) {
            sourceLinks += "<li><span class=\"src-label\">Emulator:</span> <span class=\"src-desc\">device emulator</span> <code id=\"srcEmulatorPath\" onclick=\"navigator.clipboard.writeText(this.textContent)\" style=\"cursor:pointer; color:#2a5db0;\" title=\"Click to copy\"></code></li>";
        }
        if (active.getGuiSourcePath() != null) {
            sourceLinks += "<li><span class=\"src-label\">GUI:</span> <span class=\"src-desc\">web interface</span> <code id=\"srcGuiPath\" onclick=\"navigator.clipboard.writeText(this.textContent)\" style=\"cursor:pointer; color:#2a5db0;\" title=\"Click to copy\"></code></li>";
        }
        vars.put("sourceLinks", sourceLinks);
        vars.put("sourcePathInit", "");
        String head = TemplateEngine.render("header.html", vars);
        String env = TemplateEngine.render("environment.html", vars);
        String proj = TemplateEngine.render("project.html", vars);
        String scripts = TemplateEngine.render("scripts.html", vars);
        String foot = TemplateEngine.render("footer.html", java.util.Map.of("scripts", scripts));
        String html = head + env + proj + foot;
        t.getResponseHeaders().set("Content-Type", "text/html");
        byte[] bytes = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        t.sendResponseHeaders(200, bytes.length);
        t.getResponseBody().write(bytes);
        t.getResponseBody().close();
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new StaticHandler());
        server.createContext("/api/", new ApiHandler());
        server.setExecutor(exec);

        server.start();
        System.out.println("Launcher started on port " + PORT);
        openBrowser("http://localhost:" + PORT);



    }

    static class ApiHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String path = t.getRequestURI().getPath().substring(5);
            try {
                if ("POST".equals(t.getRequestMethod())) {
                    switch (path) {
                        case "select": handleSelect(t); break;
                        case "set-workspace": handleSetWorkspace(t); break;
                        case "clone": activeRunner.handleClone(t); break;
                        case "reset": activeRunner.handleReset(t); break;
                        case "download-jdk": handleDownloadJdk(t); break;
                        case "download-gradle": handleDownloadGradle(t); break;
                        case "open-path": handleOpenPath(t); break;

                        case "build": activeRunner.handleBuild(t); break;
                        case "run": activeRunner.handleRun(t); break;
                        case "stop": activeRunner.handleStop(t); break;
                        case "run-emulator": activeRunner.handleRunEmulator(t); break;
                        case "stop-emulator": activeRunner.handleStopEmulator(t); break;
                        case "status": handleStatus(t); break;
                        case "set-jdk-path": handleSetJdkPath(t); break;
                        case "set-gradle-path": handleSetGradlePath(t); break;
                        default: send404(t);
                    }
                } else if ("GET".equals(t.getRequestMethod())) {
                    switch (path) {
                        case "logs": handleSse(t); break;
                        case "status": handleStatus(t); break;
                        case "ensure-jdk": handleEnsureJdk(t); break;
                        case "get-jdk-status": handleGetJdkStatus(t); break;
                        case "ensure-gradle": handleEnsureGradle(t); break;
                        case "get-gradle-status": handleGetGradleStatus(t); break;
                        default: send404(t);
                    }
                } else send404(t);
            } catch (Exception e) {
                sendError(t, 500, e.getMessage());
            }
        }
    }

    static void handleSelect(HttpExchange t) throws IOException {
        String project = new String(t.getRequestBody().readAllBytes()).trim();
        if (!runners.containsKey(project)) { sendError(t, 400, "Unknown project"); return; }
        activeRunner = runners.get(project);
        sendOk(t, "{\"status\":\"ok\",\"project\":\"" + project + "\"}");
    }

    static void handleSetWorkspace(HttpExchange t) throws IOException {
        String path = new String(t.getRequestBody().readAllBytes()).trim();
        workspace = Path.of(path);
        LauncherContext newCtx = new LauncherContext(workspace, null, exec, Launcher::broadcast);
        for (ProjectRunner r : runners.values()) {
            if (r instanceof AbstractProjectRunner ab) {
                ab.ctx = newCtx;
            }
        }
        // re-evaluate cloned state and broadcast it to all SSE clients
        boolean cloned = Files.exists(workspace.resolve("client-java/.git"));
        if (cloned) {
            broadcast("clone_done", "{}");
        } else {
            broadcast("uuid_clear", "{}");
            broadcast("service_stopped", "{}");
            broadcast("emulator_stopped", "{}");
        }
        sendOk(t, "{\"status\":\"ok\",\"workspace\":\"" + displayPath() + "\"}");
    }


    static void handleSse(HttpExchange t) throws IOException {
        t.getResponseHeaders().set("Content-Type", "text/event-stream");
        t.getResponseHeaders().set("Cache-Control", "no-cache");
        t.getResponseHeaders().set("Connection", "keep-alive");
        t.sendResponseHeaders(200, 0);
        SseConnection conn = new SseConnection(t);
        sseConnections.add(conn);
        sendCurrentToolStatus(conn);
        // send current service/emulator state
        if (activeRunner instanceof SmartHubRunner r) {
            if (r.serviceRunning()) {
                conn.send("service_started", "{}");
                String uuid = r.getServiceUuid();
                if (uuid != null) {
                    conn.send("uuid_found", "{\"uuid\":\"" + uuid + "\"}");
                }
            }
            if (r.emulatorRunning()) {
                conn.send("emulator_started", "{}");
            }
        }

        while (!conn.closed) {
            try {
                conn.send("", "heartbeat");
                Thread.sleep(15000);
            } catch (InterruptedException e) { break; }
        }
        sseConnections.remove(conn);
    }

    static void sendCurrentToolStatus(SseConnection conn) {
        if (jdkHome != null) {
            conn.send("jdk_ready", "{\"path\":\"" + escapeJson(jdkHome) + "\"}");
        } else {
            conn.send("jdk_ready", "{\"path\":\"\"}");
        }
        if (gradleHome != null) {
            conn.send("gradle_ready", "{\"path\":\"" + escapeJson(gradleHome) + "\"}");
        } else {
            conn.send("gradle_ready", "{\"path\":\"\"}");
        }
    }


    static void handleStatus(HttpExchange t) throws IOException {
        boolean cloned = Files.exists(workspace.resolve("client-java/.git"));
        boolean srvRunning = false;
        boolean emRunning = false;
        String svcUuid = "";
        if (activeRunner instanceof SmartHubRunner r) {
            srvRunning = r.serviceRunning();
            emRunning = r.emulatorRunning();
            svcUuid = r.getServiceUuid();
        }
        String json = "{\"cloned\":" + cloned + ",\"serviceRunning\":" + srvRunning + ",\"emulatorRunning\":" + emRunning + ",\"serviceUuid\":\"" + (svcUuid != null ? svcUuid : "") + "\",\"workspace\":\"" + displayPath() + "\"}";
        sendOk(t, json);
    }



    // -- Modified StaticHandler --
    static class StaticHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            String path = t.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) {
                handleIndex(t);
                return;
            }
            // for static resources, fallback to classpath resources (css/js)
            try (InputStream is = Launcher.class.getResourceAsStream(path.substring(1))) {
                if (is == null) { send404(t); return; }
                byte[] bytes = is.readAllBytes();
                t.getResponseHeaders().set("Content-Type", getContentType(path));
                t.sendResponseHeaders(200, bytes.length);
                t.getResponseBody().write(bytes);
            }
            t.getResponseBody().close();
        }
    }


    static void sendOk(HttpExchange t, String json) throws IOException {
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.sendResponseHeaders(200, json.length());
        t.getResponseBody().write(json.getBytes());
        t.getResponseBody().close();
    }

    static void send404(HttpExchange t) throws IOException {
        t.sendResponseHeaders(404, -1);
    }

    static void sendError(HttpExchange t, int code, String msg) throws IOException {
        String json = "{\"error\":\"" + msg + "\"}";
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.sendResponseHeaders(code, json.length());
        t.getResponseBody().write(json.getBytes());
        t.getResponseBody().close();
    }

    static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".css")) return "text/css";
        return "application/octet-stream";
    }
}