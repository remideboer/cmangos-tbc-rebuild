package org.tbc.launcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Starts a child process. Tests inject a fake; production uses ProcessBuilder. */
public interface ProcessStarter {
    /**
     * @param pipeOutput if true, leave stdout as a pipe for the caller to pump;
     *                   if false, append merged stdout/stderr to {@code logFile}
     */
    Process start(List<String> command, Path workDir, Path logFile, boolean pipeOutput) throws IOException;
}
