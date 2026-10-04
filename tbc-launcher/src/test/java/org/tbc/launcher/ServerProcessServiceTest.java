package org.tbc.launcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerProcessServiceTest {
    @TempDir
    Path home;
    private Path jdk;
    private Path userHome;
    private RecordingStarter starter;
    private ServerProcessService svc;

    @BeforeEach
    void setUp() throws Exception {
        jdk = home.resolve("jdk");
        userHome = home.resolve("user");
        Files.createDirectories(jdk.resolve("bin"));
        Files.createDirectories(userHome);
        Files.writeString(jdk.resolve("bin").resolve("java.exe"), "java");
        touch(AUTH_JAR);
        touch(WORLD_JAR);
        touch(ADMIN_JAR);
        touch(EDITOR_JAR);
        Files.createDirectories(home.resolve("conf"));
        Files.writeString(home.resolve(ServerProcessService.LOCAL_REALMD), "realmd");
        Files.writeString(home.resolve(ServerProcessService.LOCAL_MANGOSD), "mangosd");
        starter = new RecordingStarter();
        svc = new ServerProcessService(home, starter, jdk.toString(), userHome.toString(), 1L);
    }

    @Test
    void startStopRestartAndOpenShouldRecordCommands() {
        svc.startServers();
        assertTrue(svc.isAuthRunning());
        assertTrue(svc.isWorldRunning());
        assertEquals(2, starter.calls.size());
        assertCmd(starter.calls.get(0), AUTH_JAR, "local-realmd.conf", "auth.log", true);
        assertCmd(starter.calls.get(1), WORLD_JAR, "local-mangosd.conf", "world.log", true);

        svc.openAdmin();
        svc.openEditor();
        svc.openQuestEditor();
        assertCmd(starter.calls.get(2), ADMIN_JAR, "local-realmd.conf", "admin.log", false);
        assertCmd(starter.calls.get(3), EDITOR_JAR, "local-mangosd.conf", "editor.log", false);
        assertEquals(4, starter.calls.get(3).command().size());
        assertCmd(starter.calls.get(4), EDITOR_JAR, "local-mangosd.conf", "editor.log", false);
        assertEquals("--quest", starter.calls.get(4).command().get(4));
        assertTrue(svc.isAuthRunning());

        svc.stopServers();
        assertFalse(svc.isAuthRunning());
        assertFalse(svc.isWorldRunning());
        svc.stopServers();

        svc.restartServers();
        assertTrue(svc.isAuthRunning());
        assertEquals(7, starter.calls.size());
        svc.stopServers();
    }

    @Test
    void startWhenWorldClassesNewerThanJarShouldRefuse() throws Exception {
        Path worldClass = home.resolve(ServerProcessService.WORLD_CLASS);
        Files.createDirectories(worldClass.getParent());
        Files.writeString(worldClass, "class");
        long jarMs = Files.getLastModifiedTime(home.resolve(WORLD_JAR)).toMillis();
        Files.setLastModifiedTime(worldClass, java.nio.file.attribute.FileTime.fromMillis(jarMs + 60_000));
        assertMsg("tbc-world jar is older than target/classes. Run build.bat (package), not only mvn test.",
                svc::startServers);
    }

    @Test
    void startWhenAuthClassesNewerThanJarShouldRefuse() throws Exception {
        Path authClass = home.resolve(ServerProcessService.AUTH_CLASS);
        Files.createDirectories(authClass.getParent());
        Files.writeString(authClass, "class");
        long jarMs = Files.getLastModifiedTime(home.resolve(AUTH_JAR)).toMillis();
        Files.setLastModifiedTime(authClass, java.nio.file.attribute.FileTime.fromMillis(jarMs + 60_000));
        assertMsg("tbc-auth jar is older than target/classes. Run build.bat (package), not only mvn test.",
                svc::startServers);
    }

    @Test
    void openAdminWhenClassesNewerThanJarShouldRefuse() throws Exception {
        Path adminClass = home.resolve(ServerProcessService.ADMIN_CLASS);
        Files.createDirectories(adminClass.getParent());
        Files.writeString(adminClass, "class");
        long jarMs = Files.getLastModifiedTime(home.resolve(ADMIN_JAR)).toMillis();
        Files.setLastModifiedTime(adminClass, java.nio.file.attribute.FileTime.fromMillis(jarMs + 60_000));
        assertMsg("tbc-admin jar is older than target/classes. Run build.bat (package), not only mvn test.",
                svc::openAdmin);
    }

    @Test
    void openEditorWhenClassesNewerThanJarShouldRefuse() throws Exception {
        Path editorClass = home.resolve(ServerProcessService.EDITOR_CLASS);
        Files.createDirectories(editorClass.getParent());
        Files.writeString(editorClass, "class");
        long jarMs = Files.getLastModifiedTime(home.resolve(EDITOR_JAR)).toMillis();
        Files.setLastModifiedTime(editorClass, java.nio.file.attribute.FileTime.fromMillis(jarMs + 60_000));
        assertMsg("tbc-editor jar is older than target/classes. Run build.bat (package), not only mvn test.",
                svc::openEditor);
    }

    @Test
    void startWhenClassesOlderOrAbsentShouldStart() throws Exception {
        Path worldClass = home.resolve(ServerProcessService.WORLD_CLASS);
        Files.createDirectories(worldClass.getParent());
        Files.writeString(worldClass, "class");
        long jarMs = Files.getLastModifiedTime(home.resolve(WORLD_JAR)).toMillis();
        Files.setLastModifiedTime(worldClass, java.nio.file.attribute.FileTime.fromMillis(jarMs - 60_000));
        svc.startServers();
        assertTrue(svc.isWorldRunning());
        svc.stopServers();
    }

    @Test
    void startWhenAlreadyRunningShouldRefuse() {
        svc.startServers();
        assertMsg("Auth is already running.", svc::startServers);
        starter.calls.get(1).process().alive = false;
        assertMsg("Auth is already running.", svc::startServers);
        starter.calls.get(0).process().alive = false;
        svc.startServers();
        assertEquals(4, starter.calls.size());
        starter.calls.get(2).process().alive = false;
        assertMsg("World is already running.", svc::startServers);
    }

    @Test
    void startWhenJavaOrJarOrConfMissingShouldThrow() throws Exception {
        ServerProcessService noJava = new ServerProcessService(
                home, starter, home.resolve("missing-jdk").toString(), userHome.toString(), 1L);
        assertMsg("Java 21 not found. Set JAVA_HOME.", noJava::startServers);

        Files.delete(home.resolve(AUTH_JAR));
        assertMsg("Missing tbc-auth jar. Run build.bat first.", svc::startServers);
        touch(AUTH_JAR);
        Files.delete(home.resolve(WORLD_JAR));
        assertMsg("Missing tbc-world jar. Run build.bat first.", svc::startServers);
        touch(WORLD_JAR);
        Files.delete(home.resolve(ADMIN_JAR));
        assertMsg("Missing tbc-admin jar. Run build.bat first.", svc::openAdmin);
        touch(ADMIN_JAR);
        Files.delete(home.resolve(EDITOR_JAR));
        assertMsg("Missing tbc-editor jar. Run build.bat first.", svc::openEditor);
        touch(EDITOR_JAR);

        Files.delete(home.resolve(ServerProcessService.LOCAL_REALMD));
        Files.delete(home.resolve(ServerProcessService.LOCAL_MANGOSD));
        assertMsg("Missing " + ServerProcessService.LOCAL_REALMD + ".", svc::startServers);
        Files.writeString(home.resolve(ServerProcessService.REALMD), "r");
        Files.writeString(home.resolve(ServerProcessService.MANGOSD), "m");
        svc.startServers();
        assertCmd(starter.calls.get(0), AUTH_JAR, "realmd.conf", "auth.log", true);
        assertCmd(starter.calls.get(1), WORLD_JAR, "mangosd.conf", "world.log", true);
    }

    @Test
    void startWhenLogListenersSetShouldDeliverPumpedLines() throws Exception {
        List<String> authLines = new ArrayList<>();
        List<String> worldLines = new ArrayList<>();
        svc.setAuthLogListener(authLines::add);
        svc.setWorldLogListener(worldLines::add);
        starter.authStdout = "auth-hi\n";
        starter.worldStdout = "world-hi\n";
        svc.startServers();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while ((authLines.size() < 2 || worldLines.size() < 2) && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(authLines.get(0).startsWith("starting auth jar="), authLines.toString());
        assertTrue(authLines.contains("auth-hi"), authLines.toString());
        assertTrue(worldLines.get(0).startsWith("starting world jar="), worldLines.toString());
        assertTrue(worldLines.contains("world-hi"), worldLines.toString());
        assertEquals("auth-hi\n", Files.readString(home.resolve("logs").resolve("auth.log")));
        assertEquals("world-hi\n", Files.readString(home.resolve("logs").resolve("world.log")));
        svc.stopServers();
    }

    @Test
    void rejectIfStaleWhenSentinelNullOrEmptyShouldNoOp() {
        Path jar = home.resolve(WORLD_JAR);
        svc.rejectIfStale(jar, "tbc-world", null);
        svc.rejectIfStale(jar, "tbc-world", "");
    }

    @Test
    void jarStartLineShouldIncludeAbsolutePathAndMtime() {
        Path jar = home.resolve(WORLD_JAR).toAbsolutePath().normalize();
        String line = ServerProcessService.jarStartLine("world", jar);
        assertTrue(line.startsWith("starting world jar=" + jar));
        assertTrue(line.contains("mtimeMs="));
    }

    @Test
    void resolveJavaWhenEnvMissingShouldUseFallbackJdk() throws Exception {
        Path fb = userHome.resolve(".jdks").resolve("jdk-21").resolve("bin");
        Files.createDirectories(fb);
        Files.writeString(fb.resolve("java.exe"), "fb");
        ServerProcessService blank = new ServerProcessService(home, starter, "  ", userHome.toString(), 1L);
        assertTrue(blank.resolveJava().endsWith(Path.of("java.exe")));
        ServerProcessService none = new ServerProcessService(home, starter, null, userHome.toString(), 1L);
        assertEquals(fb.resolve("java.exe").toAbsolutePath().normalize(), none.resolveJava());
        ServerProcessService badEnv = new ServerProcessService(
                home, starter, home.resolve("nope").toString(), userHome.toString(), 1L);
        assertEquals(fb.resolve("java.exe").toAbsolutePath().normalize(), badEnv.resolveJava());
    }

    @Test
    void startWhenProcessStarterFailsShouldWrap() {
        starter.failName = "auth";
        assertMsg("Could not start auth: boom", svc::startServers);
        assertFalse(svc.isAuthRunning());
        starter.failName = "world";
        try {
            svc.startServers();
        } catch (LauncherException e) {
            assertEquals("Could not start world: boom", e.getMessage());
        }
        assertTrue(svc.isAuthRunning());
        assertFalse(svc.isWorldRunning());
        svc.stopServers();
    }

    @Test
    void stopWhenAliveOrInterruptedShouldDestroy() {
        svc.startServers();
        FakeProcess world = starter.calls.get(1).process();
        world.destroyKills = false;
        world.waitReturns = false;
        svc.stopServers();
        assertTrue(world.forcibly);
        assertFalse(world.alive);

        svc.startServers();
        FakeProcess auth = starter.calls.get(2).process();
        auth.destroyKills = false;
        auth.waitReturns = false;
        auth.waitInterrupt = true;
        svc.stopServers();
        assertTrue(auth.forcibly);
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();

        svc.startServers();
        FakeProcess w2 = starter.calls.get(5).process();
        w2.destroyKills = true;
        w2.waitReturns = false;
        w2.waitInterrupt = true;
        svc.stopServers();
        assertFalse(w2.forcibly);
        Thread.interrupted();

        svc.startServers();
        FakeProcess w3 = starter.calls.get(7).process();
        w3.destroyKills = true;
        w3.waitReturns = false;
        w3.waitInterrupt = false;
        svc.stopServers();
        assertFalse(w3.forcibly);

        svc.startServers();
        starter.calls.get(8).process().alive = false;
        starter.calls.get(9).process().alive = false;
        svc.stopServers();
        svc.startServers();
        FakeProcess w4 = starter.calls.get(11).process();
        w4.waitReturns = true;
        svc.stopServers();
        assertFalse(w4.forcibly);
    }

    private void assertCmd(StartCall call, String jar, String conf, String log, boolean pipeOutput) {
        assertEquals("-jar", call.command().get(1));
        assertTrue(call.command().get(2).replace('\\', '/').endsWith(jar), call.command().get(2));
        assertTrue(call.command().get(3).replace('\\', '/').endsWith(conf), call.command().get(3));
        assertEquals(home.toAbsolutePath().normalize(), call.workDir());
        assertTrue(call.logFile().endsWith(Path.of("logs", log)));
        assertEquals(pipeOutput, call.pipeOutput());
    }

    private void touch(String relative) throws IOException {
        Path p = home.resolve(relative);
        Files.createDirectories(p.getParent());
        Files.writeString(p, "x");
    }

    private static void assertMsg(String msg, Runnable action) {
        LauncherException e = assertThrows(LauncherException.class, action::run);
        assertEquals(msg, e.getMessage());
    }

    private static final String AUTH_JAR = ServerProcessService.AUTH_JAR;
    private static final String WORLD_JAR = ServerProcessService.WORLD_JAR;
    private static final String ADMIN_JAR = ServerProcessService.ADMIN_JAR;
    private static final String EDITOR_JAR = ServerProcessService.EDITOR_JAR;

    private static final class RecordingStarter implements ProcessStarter {
        final List<StartCall> calls = new ArrayList<>();
        String failName;
        String authStdout = "";
        String worldStdout = "";

        @Override
        public Process start(List<String> command, Path workDir, Path logFile, boolean pipeOutput)
                throws IOException {
            String joined = String.join(" ", command).replace('\\', '/');
            String name;
            if (joined.contains("tbc-auth/")) {
                name = "auth";
            } else if (joined.contains("tbc-world/")) {
                name = "world";
            } else if (joined.contains("tbc-admin/")) {
                name = "admin";
            } else {
                name = "editor";
            }
            if (name.equals(failName)) {
                throw new IOException("boom");
            }
            String stdout = name.equals("auth") ? authStdout : name.equals("world") ? worldStdout : "";
            FakeProcess p = new FakeProcess(stdout);
            calls.add(new StartCall(command, workDir, logFile, pipeOutput, p));
            return p;
        }
    }

    private record StartCall(List<String> command, Path workDir, Path logFile, boolean pipeOutput,
            FakeProcess process) {}

    private static final class FakeProcess extends Process {
        boolean alive = true;
        boolean destroyKills = true;
        boolean waitReturns = true;
        boolean waitInterrupt;
        boolean forcibly;
        private final byte[] stdout;

        FakeProcess() {
            this("");
        }

        FakeProcess(String stdout) {
            this.stdout = stdout.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        @Override
        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(stdout);
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int waitFor() {
            alive = false;
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            if (waitInterrupt) {
                throw new InterruptedException();
            }
            if (waitReturns) {
                alive = false;
                return true;
            }
            return !alive;
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException();
            }
            return 0;
        }

        @Override
        public void destroy() {
            if (destroyKills) {
                alive = false;
            }
        }

        @Override
        public Process destroyForcibly() {
            forcibly = true;
            alive = false;
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }
    }
}
