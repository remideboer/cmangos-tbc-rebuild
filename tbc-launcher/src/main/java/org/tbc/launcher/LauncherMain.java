package org.tbc.launcher;

import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;

/** Swing operator tool. Spawns auth/world/admin/editor JVMs. No SOAP/RA. */
public final class LauncherMain {
    private LauncherMain() {}

    public static void main(String[] args) {
        Path home = detectHome(Path.of("").toAbsolutePath());
        ServerProcessService service = new ServerProcessService(home);
        SwingUtilities.invokeLater(() -> new LauncherFrame(service).setVisible(true));
    }

    static Path detectHome(Path start) {
        Path p = start.toAbsolutePath().normalize();
        for (int i = 0; i < 4; i++) {
            if (Files.isDirectory(p.resolve("conf")) && Files.isRegularFile(p.resolve("tbc-auth").resolve("pom.xml"))) {
                return p;
            }
            Path parent = p.getParent();
            if (parent == null) {
                break;
            }
            p = parent;
        }
        return start.toAbsolutePath().normalize();
    }
}
