package org.tbc.launcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Starts a child process. Tests inject a fake; production uses ProcessBuilder. */
public interface ProcessStarter {
    Process start(List<String> command, Path workDir, Path logFile) throws IOException;
}
