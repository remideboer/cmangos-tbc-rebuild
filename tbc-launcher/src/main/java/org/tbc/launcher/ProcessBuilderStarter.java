package org.tbc.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** ProcessBuilder child: pipe stdout for UI pump, or append to a log file. */
public final class ProcessBuilderStarter implements ProcessStarter {
    @Override
    public Process start(List<String> command, Path workDir, Path logFile, boolean pipeOutput) throws IOException {
        Files.createDirectories(logFile.getParent());
        ProcessBuilder pb = new ProcessBuilder(new ArrayList<>(command));
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        if (!pipeOutput) {
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(logFile.toString())));
        }
        return pb.start();
    }
}
