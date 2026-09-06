package org.tbc.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;

/** Reads process stdout line-by-line, tees to a log file and an optional listener. */
public final class ProcessLogPump {
    private ProcessLogPump() {
    }

    public static Thread start(String name, Process process, Path logFile, Consumer<String> onLine) {
        Thread t = new Thread(() -> pump(process, logFile, onLine), "log-pump-" + name);
        t.setDaemon(true);
        t.start();
        return t;
    }

    static void pump(Process process, Path logFile, Consumer<String> onLine) {
        try {
            Files.createDirectories(logFile.getParent());
        } catch (IOException ignored) {
        }
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    Files.writeString(logFile, line + "\n", StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException ignored) {
                }
                if (onLine != null) {
                    onLine.accept(line);
                }
            }
        } catch (IOException ignored) {
        }
    }
}
