package org.tbc.matrix;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

final class MatrixReport {
    private static final List<String> PARTS = List.of("inprocess", "live-auth", "live-world");

    private MatrixReport() {}

    static Path file() {
        return Path.of("target", "client-command-matrix.md");
    }

    static void write(String slug, String heading, List<MatrixRow> rows) {
        try {
            Path dir = Path.of("target");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("matrix-" + slug + ".md"), render(heading, rows), StandardCharsets.UTF_8);
            StringBuilder all = new StringBuilder();
            all.append("# Client command matrix\n\n");
            all.append("Outcomes: `pass` (handler or STATUS_NEVER stub, session survived), ");
            all.append("`unimplemented` (no handler, survived), `fail` (exception or session death), `skip`.\n\n");
            for (String p : PARTS) {
                Path part = dir.resolve("matrix-" + p + ".md");
                if (Files.exists(part)) {
                    all.append(Files.readString(part, StandardCharsets.UTF_8));
                }
            }
            Files.writeString(file(), all.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("matrix report", e);
        }
    }

    private static String render(String heading, List<MatrixRow> rows) {
        Map<String, Long> counts = rows.stream()
                .collect(Collectors.groupingBy(r -> r.result(), Collectors.counting()));
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(heading).append("\n\n");
        sb.append("rows: ").append(rows.size());
        for (String k : List.of("pass", "unimplemented", "fail", "skip")) {
            sb.append(" | ").append(k).append("=").append(counts.getOrDefault(k, 0L));
        }
        sb.append("\n\n| id | name | result | note |\n|----|------|--------|------|\n");
        for (MatrixRow r : rows) {
            sb.append(String.format(Locale.ROOT, "| 0x%03X | %s | %s | %s |%n",
                    r.id(), r.name(), r.result(), r.note() == null ? "" : r.note().replace("|", "/")));
        }
        sb.append("\n");
        return sb.toString();
    }
}
