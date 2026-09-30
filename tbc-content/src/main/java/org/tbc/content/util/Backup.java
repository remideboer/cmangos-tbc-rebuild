package org.tbc.content.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Backup existing outputs before overwrite. */
public final class Backup {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private Backup() {}

    public static Path bakIfExists(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return null;
        }
        Path bak = path.resolveSibling(path.getFileName() + ".bak." + LocalDateTime.now().format(FMT));
        Files.copy(path, bak, StandardCopyOption.REPLACE_EXISTING);
        return bak;
    }
}
