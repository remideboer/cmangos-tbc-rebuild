package org.tbc.launcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientLauncherTest {
    @TempDir
    Path home;
    private Path settings;
    private Path exe;
    private RecordingStarter starter;

    @BeforeEach
    void setUp() throws Exception {
        settings = home.resolve("conf").resolve(ClientLauncher.SETTINGS_FILE);
        Path clientDir = home.resolve("client");
        Files.createDirectories(clientDir);
        exe = clientDir.resolve("Wow.exe");
        Files.writeString(exe, "exe");
        starter = new RecordingStarter();
    }

    @Test
    void clientPathWhenNoSettingsFileShouldBeUnset() {
        ClientLauncher cl = new ClientLauncher(settings, starter);
        assertFalse(cl.hasClientPath());
        assertTrue(cl.clientPath().isEmpty());
    }

    @Test
    void startClientWhenPathUnsetShouldThrowAndNotSpawn() {
        ClientLauncher cl = new ClientLauncher(settings, starter);
        LauncherException e = assertThrows(LauncherException.class, cl::startClient);
        assertTrue(e.getMessage().contains("Client path"), e.getMessage());
        assertTrue(starter.calls.isEmpty());
    }

    @Test
    void startClientWhenPathSetShouldSpawnExeInItsFolder() {
        ClientLauncher cl = new ClientLauncher(settings, starter);
        cl.setClientPath(exe);
        cl.startClient();
        assertEquals(1, starter.calls.size());
        assertEquals(List.of(exe.toAbsolutePath().normalize().toString()), starter.calls.get(0));
        assertEquals(exe.getParent().toAbsolutePath().normalize(), starter.workDirs.get(0));
    }

    @Test
    void setClientPathWhenFileMissingShouldThrowAndStayUnset() {
        ClientLauncher cl = new ClientLauncher(settings, starter);
        assertThrows(LauncherException.class, () -> cl.setClientPath(home.resolve("nope").resolve("Wow.exe")));
        assertFalse(cl.hasClientPath());
    }

    @Test
    void setClientPathShouldPersistAcrossInstances() {
        new ClientLauncher(settings, starter).setClientPath(exe);
        ClientLauncher reloaded = new ClientLauncher(settings, starter);
        assertEquals(exe.toAbsolutePath().normalize(), reloaded.clientPath().orElseThrow());
    }

    @Test
    void clearClientPathShouldForgetAndPersistUnset() {
        ClientLauncher cl = new ClientLauncher(settings, starter);
        cl.setClientPath(exe);
        cl.clearClientPath();
        assertFalse(cl.hasClientPath());
        assertFalse(new ClientLauncher(settings, starter).hasClientPath());
    }

    @Test
    void startClientWhenStoredExeVanishedShouldThrow() throws Exception {
        ClientLauncher cl = new ClientLauncher(settings, starter);
        cl.setClientPath(exe);
        Files.delete(exe);
        assertThrows(LauncherException.class, cl::startClient);
        assertTrue(starter.calls.isEmpty());
    }

    @Test
    void startClientWhenStarterFailsShouldWrap() {
        starter.fail = true;
        ClientLauncher cl = new ClientLauncher(settings, starter);
        cl.setClientPath(exe);
        LauncherException e = assertThrows(LauncherException.class, cl::startClient);
        assertTrue(e.getMessage().contains("Could not start client"), e.getMessage());
    }

    @Test
    void clientPathWhenSettingsFileUnreadableShouldBeUnset() throws Exception {
        Files.createDirectories(settings);
        assertFalse(new ClientLauncher(settings, starter).hasClientPath());
    }

    @Test
    void setClientPathWhenSettingsDirIsAFileShouldThrow() throws Exception {
        Files.createDirectories(settings.getParent().getParent());
        Files.writeString(settings.getParent(), "not a dir");
        ClientLauncher cl = new ClientLauncher(settings, starter);
        assertThrows(LauncherException.class, () -> cl.setClientPath(exe));
    }

    private static final class RecordingStarter implements ProcessStarter {
        final List<List<String>> calls = new ArrayList<>();
        final List<Path> workDirs = new ArrayList<>();
        boolean fail;

        @Override
        public Process start(List<String> command, Path workDir, Path logFile, boolean pipeOutput)
                throws java.io.IOException {
            if (fail) {
                throw new java.io.IOException("boom");
            }
            calls.add(List.copyOf(command));
            workDirs.add(workDir);
            return null;
        }
    }
}
