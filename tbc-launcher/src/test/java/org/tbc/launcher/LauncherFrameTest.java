package org.tbc.launcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DisabledIfSystemProperty(named = "java.awt.headless", matches = "true")
class LauncherFrameTest {
    @TempDir
    Path home;
    private RecordingStarter starter;
    private ServerProcessService svc;
    private ClientLauncher client;
    private FakeDialogs dialogs;
    private Path exe;

    @BeforeEach
    void setUp() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        exe = home.resolve("client").resolve("Wow.exe");
        Files.createDirectories(exe.getParent());
        Files.writeString(exe, "exe");
        dialogs = new FakeDialogs();
        Path jdk = home.resolve("jdk");
        Files.createDirectories(jdk.resolve("bin"));
        Files.writeString(jdk.resolve("bin").resolve("java.exe"), "java");
        touch(ServerProcessService.EDITOR_JAR);
        Files.createDirectories(home.resolve("conf"));
        Files.writeString(home.resolve(ServerProcessService.LOCAL_MANGOSD), "mangosd");
        starter = new RecordingStarter();
        svc = new ServerProcessService(home, starter, jdk.toString(), home.resolve("user").toString(), 1L);
        client = new ClientLauncher(home.resolve("conf").resolve(ClientLauncher.SETTINGS_FILE), starter);
    }

    @Test
    void givenFrameWhenShownThenOpenEditorButtonIsFullyVisible() throws Exception {
        LauncherFrame frame = constructFrame();
        try {
            onEdt(() -> {
                frame.setVisible(true);
                frame.validate();
            });
            for (String text : new String[]{"Open editor", "Start client", "Client path…"}) {
                JButton btn = findButton(frame, text);
                assertNotNull(btn, text);
                onEdt(() -> {
                    assertTrue(btn.getWidth() > 0 && btn.getHeight() > 0);
                    Container parent = btn.getParent();
                    Rectangle b = btn.getBounds();
                    assertTrue(b.y + b.height <= parent.getHeight(),
                            text + " clipped at y=" + b.y + " h=" + b.height + " parentH=" + parent.getHeight());
                    assertTrue(b.x + b.width <= parent.getWidth(),
                            text + " clipped at x=" + b.x + " w=" + b.width + " parentW=" + parent.getWidth());
                });
            }
        } finally {
            onEdt(frame::dispose);
        }
    }

    @Test
    void givenFrameWhenShownThenAuthAndWorldLogPanesAreScrollableTenRows() throws Exception {
        LauncherFrame frame = constructFrame();
        try {
            onEdt(() -> {
                frame.setVisible(true);
                frame.validate();
                JTextArea auth = frame.authLogArea();
                JTextArea world = frame.worldLogArea();
                assertEquals(LauncherFrame.LOG_ROWS, auth.getRows());
                assertEquals(LauncherFrame.LOG_ROWS, world.getRows());
                assertFalse(auth.isEditable());
                assertFalse(world.isEditable());
                assertTrue(auth.getParent().getParent() instanceof JScrollPane);
                assertTrue(world.getParent().getParent() instanceof JScrollPane);
            });
        } finally {
            onEdt(frame::dispose);
        }
    }

    @Test
    void givenLogListenerWhenLineArrivesThenAuthPaneShowsText() throws Exception {
        touch(ServerProcessService.AUTH_JAR);
        touch(ServerProcessService.WORLD_JAR);
        Files.writeString(home.resolve(ServerProcessService.LOCAL_REALMD), "realmd");
        LauncherFrame frame = constructFrame();
        try {
            starter.authStdout = "auth-line\n";
            starter.worldStdout = "world-line\n";
            svc.startServers();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (System.nanoTime() < deadline) {
                AtomicReference<String> authText = new AtomicReference<>();
                AtomicReference<String> worldText = new AtomicReference<>();
                onEdt(() -> {
                    authText.set(frame.authLogArea().getText());
                    worldText.set(frame.worldLogArea().getText());
                });
                if (authText.get().contains("auth-line") && worldText.get().contains("world-line")) {
                    break;
                }
                Thread.sleep(20);
            }
            onEdt(() -> {
                assertTrue(frame.authLogArea().getText().contains("auth-line"));
                assertTrue(frame.worldLogArea().getText().contains("world-line"));
            });
            svc.stopServers();
        } finally {
            onEdt(frame::dispose);
        }
    }

    @Test
    void givenOpenEditorWhenClickedThenSpawnsEditorJar() throws Exception {
        LauncherFrame frame = constructFrame();
        try {
            JButton editor = findButton(frame, "Open editor");
            onEdt(() -> editor.doClick());
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (starter.calls.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(20);
            }
            assertTrue(starter.calls.size() >= 1);
            String joined = String.join(" ", starter.calls.get(0)).replace('\\', '/');
            assertTrue(joined.contains("tbc-editor/"), joined);
            assertTrue(joined.contains("local-mangosd.conf"), joined);
        } finally {
            onEdt(frame::dispose);
        }
    }

    @Test
    void givenNoClientPathWhenStartClientClickedThenAsksForExeAndStartsIt() throws Exception {
        dialogs.browseResult = exe;
        LauncherFrame frame = constructFrame();
        try {
            onEdt(() -> findButton(frame, "Start client").doClick());
            awaitCalls(1);
            assertEquals(1, dialogs.browseCalls);
            assertEquals(List.of(exe.toString()), starter.calls.get(0));
            assertEquals(exe, client.clientPath().orElseThrow());
        } finally {
            onEdt(frame::dispose);
        }
    }

    @Test
    void givenNoClientPathWhenBrowseCancelledThenNothingStartsAndPathStaysUnset() throws Exception {
        dialogs.browseResult = null;
        LauncherFrame frame = constructFrame();
        try {
            onEdt(() -> findButton(frame, "Start client").doClick());
            Thread.sleep(100);
            assertEquals(1, dialogs.browseCalls);
            assertTrue(starter.calls.isEmpty());
            assertFalse(client.hasClientPath());
        } finally {
            onEdt(frame::dispose);
        }
    }

    @Test
    void givenClientPathSetWhenStartClientClickedThenStartsWithoutAsking() throws Exception {
        client.setClientPath(exe);
        LauncherFrame frame = constructFrame();
        try {
            onEdt(() -> findButton(frame, "Start client").doClick());
            awaitCalls(1);
            assertEquals(0, dialogs.browseCalls);
            assertEquals(List.of(exe.toString()), starter.calls.get(0));
        } finally {
            onEdt(frame::dispose);
        }
    }

    @Test
    void givenClientPathSetWhenClientPathClickedAndClearChosenThenPathIsReset() throws Exception {
        client.setClientPath(exe);
        dialogs.manageResult = ClientPathDialogs.Choice.CLEAR;
        LauncherFrame frame = constructFrame();
        try {
            onEdt(() -> findButton(frame, "Client path…").doClick());
            assertEquals(exe, dialogs.managedCurrent);
            assertFalse(client.hasClientPath());
            assertTrue(starter.calls.isEmpty());
        } finally {
            onEdt(frame::dispose);
        }
    }

    @Test
    void givenClientPathSetWhenClientPathClickedAndBrowseChosenThenPathChanges() throws Exception {
        client.setClientPath(exe);
        Path other = home.resolve("client2").resolve("Wow.exe");
        Files.createDirectories(other.getParent());
        Files.writeString(other, "exe");
        dialogs.manageResult = ClientPathDialogs.Choice.BROWSE;
        dialogs.browseResult = other;
        LauncherFrame frame = constructFrame();
        try {
            onEdt(() -> findButton(frame, "Client path…").doClick());
            assertEquals(other, client.clientPath().orElseThrow());
            assertTrue(starter.calls.isEmpty());
        } finally {
            onEdt(frame::dispose);
        }
    }

    @Test
    void givenNoClientPathWhenClientPathClickedThenBrowsesDirectly() throws Exception {
        dialogs.browseResult = exe;
        LauncherFrame frame = constructFrame();
        try {
            onEdt(() -> findButton(frame, "Client path…").doClick());
            assertNull(dialogs.managedCurrent);
            assertEquals(exe, client.clientPath().orElseThrow());
        } finally {
            onEdt(frame::dispose);
        }
    }

    @Test
    void givenClientPathSetWhenClientPathClickedAndCancelledThenPathKept() throws Exception {
        client.setClientPath(exe);
        dialogs.manageResult = ClientPathDialogs.Choice.CANCEL;
        LauncherFrame frame = constructFrame();
        try {
            onEdt(() -> findButton(frame, "Client path…").doClick());
            assertEquals(exe, client.clientPath().orElseThrow());
        } finally {
            onEdt(frame::dispose);
        }
    }

    private void awaitCalls(int n) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (starter.calls.size() < n && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertTrue(starter.calls.size() >= n);
    }

    private LauncherFrame constructFrame() throws Exception {
        AtomicReference<LauncherFrame> out = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> out.set(new LauncherFrame(svc, client, dialogs)));
        return out.get();
    }

    private static final class FakeDialogs implements ClientPathDialogs {
        Path browseResult;
        int browseCalls;
        Choice manageResult = Choice.CANCEL;
        Path managedCurrent;

        @Override
        public java.util.Optional<Path> browse() {
            browseCalls++;
            return java.util.Optional.ofNullable(browseResult);
        }

        @Override
        public Choice manage(Path current) {
            managedCurrent = current;
            return manageResult;
        }
    }

    private static void onEdt(Runnable work) throws Exception {
        SwingUtilities.invokeAndWait(work);
    }

    private void touch(String relative) throws IOException {
        Path p = home.resolve(relative);
        Files.createDirectories(p.getParent());
        Files.writeString(p, "x");
    }

    private static JButton findButton(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b && text.equals(b.getText())) {
                return b;
            }
            if (c instanceof Container nested) {
                JButton found = findButton(nested, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static final class RecordingStarter implements ProcessStarter {
        final List<List<String>> calls = new ArrayList<>();
        String authStdout = "";
        String worldStdout = "";

        @Override
        public Process start(List<String> command, Path workDir, Path logFile, boolean pipeOutput) {
            calls.add(List.copyOf(command));
            String joined = String.join(" ", command).replace('\\', '/');
            String stdout = joined.contains("tbc-auth/") ? authStdout
                    : joined.contains("tbc-world/") ? worldStdout : "";
            return new FakeProcess(stdout);
        }
    }

    private static final class FakeProcess extends Process {
        private final byte[] stdout;

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
            return 0;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {
        }

        @Override
        public boolean isAlive() {
            return true;
        }
    }
}
