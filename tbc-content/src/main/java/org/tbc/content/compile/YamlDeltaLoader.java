package org.tbc.content.compile;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Loads {@code kind: spell} (etc.) YAML documents from a content tree. */
public final class YamlDeltaLoader {
    private YamlDeltaLoader() {}

    public static List<ContentDelta> loadTree(Path contentRoot) throws IOException {
        List<ContentDelta> out = new ArrayList<>();
        if (contentRoot == null || !Files.isDirectory(contentRoot)) {
            throw new IOException("content root missing: " + contentRoot);
        }
        try (Stream<Path> walk = Files.walk(contentRoot)) {
            List<Path> files = walk
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return Files.isRegularFile(p) && (n.endsWith(".yaml") || n.endsWith(".yml"));
                    })
                    .filter(p -> !p.toString().replace('\\', '/').contains("/out/"))
                    .filter(p -> !p.toString().replace('\\', '/').contains("/bindings/"))
                    .filter(p -> !underQuests(contentRoot, p))
                    .sorted()
                    .toList();
            for (Path file : files) {
                out.addAll(loadFile(file));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public static List<ContentDelta> loadFile(Path file) throws IOException {
        Yaml yaml = new Yaml();
        List<ContentDelta> out = new ArrayList<>();
        try (InputStream in = Files.newInputStream(file)) {
            for (Object doc : yaml.loadAll(in)) {
                if (doc == null) {
                    continue;
                }
                if (!(doc instanceof Map<?, ?> map)) {
                    throw new IOException(file + ": document must be a mapping");
                }
                out.add(parse(map, file.toString()));
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static ContentDelta parse(Map<?, ?> map, String source) {
        Object kindObj = map.get("kind");
        if (kindObj == null) {
            throw new IllegalArgumentException(source + ": missing kind");
        }
        String kind = String.valueOf(kindObj).trim().toLowerCase();
        Object idObj = map.get("id");
        if (idObj == null) {
            throw new IllegalArgumentException(source + ": missing id");
        }
        int id = idObj instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(idObj));

        Map<String, Map<String, Object>> client = new LinkedHashMap<>();
        Object clientObj = map.get("client");
        if (clientObj instanceof Map<?, ?> clientMap) {
            for (var e : clientMap.entrySet()) {
                String dbc = String.valueOf(e.getKey());
                if (!(e.getValue() instanceof Map<?, ?> fields)) {
                    throw new IllegalArgumentException(source + ": client." + dbc + " must be a mapping");
                }
                Map<String, Object> fieldMap = new LinkedHashMap<>();
                for (var fe : fields.entrySet()) {
                    fieldMap.put(String.valueOf(fe.getKey()), fe.getValue());
                }
                client.put(dbc, fieldMap);
            }
        }

        Map<String, Object> server = new LinkedHashMap<>();
        Object serverObj = map.get("server");
        if (serverObj instanceof Map<?, ?> serverMap) {
            for (var e : serverMap.entrySet()) {
                server.put(String.valueOf(e.getKey()), e.getValue());
            }
        }

        if (client.isEmpty() && server.isEmpty()) {
            throw new IllegalArgumentException(source + ": id=" + id + " has empty client and server");
        }
        return new ContentDelta(kind, id, source, client, server);
    }

    /** Quest drafts/published are not spell-shaped ContentDelta documents. */
    static boolean underQuests(Path contentRoot, Path file) {
        if (contentRoot == null || file == null) {
            return false;
        }
        Path rel = contentRoot.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize());
        if (rel.getNameCount() == 0) {
            return false;
        }
        return "quests".equalsIgnoreCase(rel.getName(0).toString());
    }
}
