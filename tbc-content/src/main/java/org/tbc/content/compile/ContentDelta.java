package org.tbc.content.compile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** One YAML document: kind + id + optional client/server maps. */
public final class ContentDelta {
    private final String kind;
    private final int id;
    private final String source;
    private final Map<String, Map<String, Object>> client;
    private final Map<String, Object> server;

    public ContentDelta(String kind, int id, String source,
                        Map<String, Map<String, Object>> client,
                        Map<String, Object> server) {
        this.kind = kind;
        this.id = id;
        this.source = source;
        this.client = client == null ? Map.of() : deepClient(client);
        this.server = server == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(server));
    }

    private static Map<String, Map<String, Object>> deepClient(Map<String, Map<String, Object>> in) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (var e : in.entrySet()) {
            out.put(e.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(e.getValue())));
        }
        return Collections.unmodifiableMap(out);
    }

    public String kind() {
        return kind;
    }

    public int id() {
        return id;
    }

    public String source() {
        return source;
    }

    /** dbcName → field → value */
    public Map<String, Map<String, Object>> client() {
        return client;
    }

    public Map<String, Object> server() {
        return server;
    }
}
