package org.tbc.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** Auth/world process rules. Admin/editor are separate JVMs, not stopped with servers. */
public final class ServerProcessService {
    public static final String AUTH_JAR = "tbc-auth/target/tbc-auth-0.1.0-SNAPSHOT.jar";
    public static final String WORLD_JAR = "tbc-world/target/tbc-world-0.1.0-SNAPSHOT.jar";
    public static final String ADMIN_JAR = "tbc-admin/target/tbc-admin-0.1.0-SNAPSHOT.jar";
    public static final String EDITOR_JAR = "tbc-editor/target/tbc-editor-0.1.0-SNAPSHOT.jar";
    /** Sentinels under target/classes — if newer than the shaded jar, package was skipped. */
    public static final String AUTH_CLASS = "tbc-auth/target/classes/org/tbc/auth/AuthMain.class";
    public static final String WORLD_CLASS = "tbc-world/target/classes/org/tbc/world/WorldMain.class";
    public static final String ADMIN_CLASS = "tbc-admin/target/classes/org/tbc/admin/AdminMain.class";
    public static final String EDITOR_CLASS = "tbc-editor/target/classes/org/tbc/editor/EditorMain.class";
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
    private Thread authPump;
    private Thread worldPump;
    private Thread lastPump;
    private volatile Consumer<String> authLogListener;
    private volatile Consumer<String> worldLogListener;

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

    public void setAuthLogListener(Consumer<String> listener) {
        this.authLogListener = listener;
    }

    public void setWorldLogListener(Consumer<String> listener) {
        this.worldLogListener = listener;
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
        Path authJar = requireJar(AUTH_JAR, "tbc-auth", AUTH_CLASS);
        Path worldJar = requireJar(WORLD_JAR, "tbc-world", WORLD_CLASS);
        Path realmd = resolveConf(LOCAL_REALMD, REALMD);
        Path mangosd = resolveConf(LOCAL_MANGOSD, MANGOSD);
        notifyLog(authLogListener, jarStartLine("auth", authJar));
        notifyLog(worldLogListener, jarStartLine("world", worldJar));
        auth = spawn("auth", java, authJar, realmd, "auth.log", true, authLogListener);
        authPump = lastPump;
        world = spawn("world", java, worldJar, mangosd, "world.log", true, worldLogListener);
        worldPump = lastPump;
    }

    public void stopServers() {
        Process w = world;
        stop(w);
        joinPump(w, worldPump);
        world = null;
        worldPump = null;
        Process a = auth;
        stop(a);
        joinPump(a, authPump);
        auth = null;
        authPump = null;
    }

    public void restartServers() {
        stopServers();
        startServers();
    }

    public void openAdmin() {
        Path java = resolveJava();
        Path jar = requireJar(ADMIN_JAR, "tbc-admin", ADMIN_CLASS);
        Path conf = resolveConf(LOCAL_REALMD, REALMD);
        spawn("admin", java, jar, conf, "admin.log", false, null);
    }

    public void openEditor() {
        spawnEditor(null);
    }

    public void openQuestEditor() {
        spawnEditor("--quest");
    }

    private void spawnEditor(String extraArg) {
        Path java = resolveJava();
        Path jar = requireJar(EDITOR_JAR, "tbc-editor", EDITOR_CLASS);
        Path conf = resolveConf(LOCAL_MANGOSD, MANGOSD);
        spawn("editor", java, jar, conf, "editor.log", false, null, extraArg);
    }

    private Process spawn(String name, Path java, Path jar, Path conf, String logName,
            boolean pipeOutput, Consumer<String> onLine) {
        return spawn(name, java, jar, conf, logName, pipeOutput, onLine, null);
    }

    private Process spawn(String name, Path java, Path jar, Path conf, String logName,
            boolean pipeOutput, Consumer<String> onLine, String extraArg) {
        Path log = home.resolve("logs").resolve(logName);
        List<String> command;
        if (extraArg == null) {
            command = List.of(java.toString(), "-jar", relativize(jar), relativize(conf));
        } else {
            command = List.of(java.toString(), "-jar", relativize(jar), relativize(conf), extraArg);
        }
        try {
            Process p = starter.start(command, home, log, pipeOutput);
            lastPump = pipeOutput ? ProcessLogPump.start(name, p, log, onLine) : null;
            return p;
        } catch (IOException e) {
            lastPump = null;
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

    void joinPump(Process p, Thread t) {
        if (p != null) {
            try {
                p.getInputStream().close();
            } catch (IOException ignored) {
            }
        }
        if (t == null) {
            return;
        }
        t.interrupt();
        try {
            t.join(stopWaitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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

    private Path requireJar(String relative, String label, String sentinelClass) {
        Path p = home.resolve(relative);
        if (!Files.isRegularFile(p)) {
            throw new LauncherException("Missing " + label + " jar. Run build.bat first.");
        }
        Path abs = p.toAbsolutePath().normalize();
        rejectIfStale(abs, label, sentinelClass);
        return abs;
    }

    /**
     * {@code mvn test} refreshes target/classes without shading the runnable jar. Refuse start
     * when a sentinel class is newer than the jar the launcher would exec.
     */
    void rejectIfStale(Path jar, String label, String sentinelClass) {
        if (sentinelClass == null || sentinelClass.isEmpty()) {
            return;
        }
        Path cls = home.resolve(sentinelClass);
        if (!Files.isRegularFile(cls)) {
            return;
        }
        long classMs = cls.toFile().lastModified();
        long jarMs = jar.toFile().lastModified();
        if (classMs > jarMs) {
            throw new LauncherException(
                    label + " jar is older than target/classes. Run build.bat (package), not only mvn test.");
        }
    }

    static String jarStartLine(String name, Path jar) {
        long ms = jar.toFile().lastModified();
        return "starting " + name + " jar=" + jar.toAbsolutePath().normalize() + " mtimeMs=" + ms;
    }

    private static void notifyLog(Consumer<String> listener, String line) {
        if (listener != null) {
            listener.accept(line);
        }
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
