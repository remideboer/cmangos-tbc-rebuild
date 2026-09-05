package org.tbc.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** ProcessBuilder child with stdout/stderr appended to a log file. */
public final class ProcessBuilderStarter implements ProcessStarter {
    @Override
    public Process start(List<String> command, Path workDir, Path logFile) throws IOException {
        Files.createDirectories(logFile.getParent());
        ProcessBuilder pb = new ProcessBuilder(new ArrayList<>(command));
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(logFile.toString())));
        return pb.start();
    }
}
