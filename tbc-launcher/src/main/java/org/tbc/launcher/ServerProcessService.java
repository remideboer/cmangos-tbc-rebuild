package org.tbc.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Auth/world process rules. Admin/editor are separate JVMs, not stopped with servers. */
public final class ServerProcessService {
    public static final String AUTH_JAR = "tbc-auth/target/tbc-auth-0.1.0-SNAPSHOT.jar";
    public static final String WORLD_JAR = "tbc-world/target/tbc-world-0.1.0-SNAPSHOT.jar";
    public static final String ADMIN_JAR = "tbc-admin/target/tbc-admin-0.1.0-SNAPSHOT.jar";
    public static final String EDITOR_JAR = "tbc-editor/target/tbc-editor-0.1.0-SNAPSHOT.jar";
    public static final String LOCAL_REALMD = "conf/local-realmd.conf";
    public static final String REALMD = "conf/realmd.conf";
    public static final String LOCAL_MANGOSD = "conf/local-mangosd.conf";
    public static final String MANGOSD = "conf/mangosd.conf";
    static final long DEFAULT_STOP_WAIT_MS = 3000L;

    private final Path home;
    private final ProcessStarter starter;
    private final String javaHome;
    private final String userHome;
    private final long stopWaitMs;
    private Process auth;
    private Process world;

    public ServerProcessService(Path home) {
        this(home, new ProcessBuilderStarter(), System.getenv("JAVA_HOME"),
                System.getProperty("user.home"), DEFAULT_STOP_WAIT_MS);
    }

    ServerProcessService(Path home, ProcessStarter starter, String javaHome, String userHome, long stopWaitMs) {
        this.home = home.toAbsolutePath().normalize();
        this.starter = starter;
        this.javaHome = javaHome;
        this.userHome = userHome;
        this.stopWaitMs = stopWaitMs;
    }

    public Path home() {
        return home;
    }

    public boolean isAuthRunning() {
        return alive(auth);
    }

    public boolean isWorldRunning() {
        return alive(world);
    }

    public void startServers() {
        forgetDead();
        if (alive(auth)) {
            throw new LauncherException("Auth is already running.");
        }
        if (alive(world)) {
            throw new LauncherException("World is already running.");
        }
        Path java = resolveJava();
        Path authJar = requireJar(AUTH_JAR, "tbc-auth");
        Path worldJar = requireJar(WORLD_JAR, "tbc-world");
        Path realmd = resolveConf(LOCAL_REALMD, REALMD);
        Path mangosd = resolveConf(LOCAL_MANGOSD, MANGOSD);
        auth = spawn("auth", java, authJar, realmd, "auth.log");
        world = spawn("world", java, worldJar, mangosd, "world.log");
    }

    public void stopServers() {
        stop(world);
        world = null;
        stop(auth);
        auth = null;
    }

    public void restartServers() {
        stopServers();
        startServers();
    }

    public void openAdmin() {
        Path java = resolveJava();
        Path jar = requireJar(ADMIN_JAR, "tbc-admin");
        Path conf = resolveConf(LOCAL_REALMD, REALMD);
        spawn("admin", java, jar, conf, "admin.log");
    }

    public void openEditor() {
        Path java = resolveJava();
        Path jar = requireJar(EDITOR_JAR, "tbc-editor");
        Path conf = resolveConf(LOCAL_MANGOSD, MANGOSD);
        spawn("editor", java, jar, conf, "editor.log");
    }

    private Process spawn(String name, Path java, Path jar, Path conf, String logName) {
        Path log = home.resolve("logs").resolve(logName);
        List<String> command = List.of(
                java.toString(),
                "-jar",
                relativize(jar),
                relativize(conf));
        try {
            return starter.start(command, home, log);
        } catch (IOException e) {
            throw new LauncherException("Could not start " + name + ": " + e.getMessage(), e);
        }
    }

    private void forgetDead() {
        if (!alive(auth)) {
            auth = null;
        }
        if (!alive(world)) {
            world = null;
        }
    }

    private void stop(Process p) {
        if (p == null || !p.isAlive()) {
            return;
        }
        p.destroy();
        try {
            if (p.waitFor(stopWaitMs, TimeUnit.MILLISECONDS)) {
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (p.isAlive()) {
            p.destroyForcibly();
        }
    }

    Path resolveJava() {
        if (javaHome != null && !javaHome.isBlank()) {
            Path fromEnv = Path.of(javaHome.trim()).resolve("bin").resolve("java.exe");
            if (Files.isRegularFile(fromEnv)) {
                return fromEnv.toAbsolutePath().normalize();
            }
        }
        Path fallback = Path.of(userHome).resolve(".jdks").resolve("jdk-21").resolve("bin").resolve("java.exe");
        if (Files.isRegularFile(fallback)) {
            return fallback.toAbsolutePath().normalize();
        }
        throw new LauncherException("Java 21 not found. Set JAVA_HOME.");
    }

    private Path requireJar(String relative, String label) {
        Path p = home.resolve(relative);
        if (!Files.isRegularFile(p)) {
            throw new LauncherException("Missing " + label + " jar. Run build.bat first.");
        }
        return p.toAbsolutePath().normalize();
    }

    private Path resolveConf(String preferred, String fallback) {
        Path a = home.resolve(preferred);
        if (Files.isRegularFile(a)) {
            return a.toAbsolutePath().normalize();
        }
        Path b = home.resolve(fallback);
        if (Files.isRegularFile(b)) {
            return b.toAbsolutePath().normalize();
        }
        throw new LauncherException("Missing " + preferred + ".");
    }

    private String relativize(Path p) {
        return home.relativize(p).toString();
    }

    private static boolean alive(Process p) {
        return p != null && p.isAlive();
    }
}
