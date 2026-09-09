package org.tbc.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/** Remembers where the 8606 client exe is (conf/local-launcher.conf) and starts it. The client stays external. */
public final class ClientLauncher {
    public static final String SETTINGS_FILE = "local-launcher.conf";
    static final String CLIENT_PATH_KEY = "client.path";

    private final Path settings;
    private final ProcessStarter starter;
    private Path clientPath;

    public ClientLauncher(Path settings, ProcessStarter starter) {
        this.settings = settings.toAbsolutePath().normalize();
        this.starter = starter;
        this.clientPath = load();
    }

    public boolean hasClientPath() {
        return clientPath != null;
    }

    public Optional<Path> clientPath() {
        return Optional.ofNullable(clientPath);
    }

    /** Remembers the client exe; must exist. */
    public void setClientPath(Path exe) {
        Path p = exe.toAbsolutePath().normalize();
        if (!Files.isRegularFile(p)) {
            throw new LauncherException("Client not found: " + p);
        }
        clientPath = p;
        save();
    }

    public void clearClientPath() {
        clientPath = null;
        save();
    }

    /** Starts the client in its own folder (it reads Data/ and WTF/ relative to the exe). */
    public void startClient() {
        if (clientPath == null) {
            throw new LauncherException("Client path is not set.");
        }
        if (!Files.isRegularFile(clientPath)) {
            throw new LauncherException("Client not found: " + clientPath);
        }
        Path log = settings.getParent().getParent().resolve("logs").resolve("client.log");
        try {
            starter.start(List.of(clientPath.toString()), clientPath.getParent(), log, false);
        } catch (IOException e) {
            throw new LauncherException("Could not start client: " + e.getMessage(), e);
        }
    }

    private Path load() {
        if (!Files.isRegularFile(settings)) {
            return null;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(settings)) {
            props.load(in);
        } catch (IOException e) {
            return null;
        }
        String v = props.getProperty(CLIENT_PATH_KEY);
        return v == null || v.isBlank() ? null : Path.of(v);
    }

    private void save() {
        Properties props = new Properties();
        if (clientPath != null) {
            props.setProperty(CLIENT_PATH_KEY, clientPath.toString());
        }
        try {
            Files.createDirectories(settings.getParent());
            try (OutputStream out = Files.newOutputStream(settings)) {
                props.store(out, "TBC Launcher settings");
            }
        } catch (IOException e) {
            throw new LauncherException("Could not save " + settings + ": " + e.getMessage(), e);
        }
    }
}
