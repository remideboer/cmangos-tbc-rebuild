package org.tbc.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** CMaNGOS-style key = value, with env prefix Realmd_ / Mangosd_. */
public final class Conf {
    private final Map<String, String> values = new LinkedHashMap<>();

    public static Conf load(Path file, String envPrefix) throws IOException {
        Conf c = new Conf();
        if (file != null && Files.exists(file)) {
            for (String line : Files.readAllLines(file)) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#") || t.startsWith("//")) {
                    continue;
                }
                int eq = t.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String k = t.substring(0, eq).trim();
                String v = t.substring(eq + 1).trim();
                if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
                    v = v.substring(1, v.length() - 1);
                }
                c.values.put(k, v);
            }
        }
        if (envPrefix != null) {
            System.getenv().forEach((k, v) -> {
                if (k.startsWith(envPrefix)) {
                    c.values.put(k.substring(envPrefix.length()), v);
                }
            });
        }
        return c;
    }

    public String get(String key, String def) {
        return values.getOrDefault(key, def);
    }

    public int getInt(String key, int def) {
        String v = values.get(key);
        return v == null ? def : Integer.parseInt(v.trim());
    }

    public boolean getBool(String key, boolean def) {
        String v = values.get(key);
        if (v == null) {
            return def;
        }
        return "1".equals(v) || "true".equalsIgnoreCase(v);
    }

    public DbInfo db(String key) {
        return DbInfo.parse(get(key, "127.0.0.1;3306;mangos;mangos;tbcrealmd"));
    }

    public record DbInfo(String host, int port, String user, String password, String database) {
        public static DbInfo parse(String s) {
            String[] p = s.split(";");
            return new DbInfo(p[0], Integer.parseInt(p[1]), p[2], p.length > 3 ? p[3] : "", p.length > 4 ? p[4] : "");
        }

        public String jdbcUrl() {
            return "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8";
        }
    }
}
