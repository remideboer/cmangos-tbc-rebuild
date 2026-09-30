package org.tbc.content.dbc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Loads {@code Name.txt} bindings (ported from WoW-Spell-Editor Bindings_243_tbc).
 * Lines: {@code uint Field}, {@code int Field}, {@code float Field}, or {@code uint Field string}.
 */
public final class DbcBinding {
    private final String dbcName;
    private final List<BindingField> fields;
    private final Map<String, BindingField> byName;

    private DbcBinding(String dbcName, List<BindingField> fields) {
        this.dbcName = dbcName;
        this.fields = List.copyOf(fields);
        Map<String, BindingField> map = new LinkedHashMap<>();
        for (BindingField f : fields) {
            map.put(f.name().toLowerCase(Locale.ROOT), f);
        }
        this.byName = Collections.unmodifiableMap(map);
    }

    public String dbcName() {
        return dbcName;
    }

    public List<BindingField> fields() {
        return fields;
    }

    public int fieldCount() {
        return fields.size();
    }

    public Optional<BindingField> field(String name) {
        if (name == null) {
            return Optional.empty();
        }
        BindingField direct = byName.get(name.toLowerCase(Locale.ROOT));
        if (direct != null) {
            return Optional.of(direct);
        }
        return Optional.ofNullable(byName.get(alias(name).toLowerCase(Locale.ROOT)));
    }

    /** Friendly YAML aliases → Spell.dbc column names (enUS = locale slot 0). */
    static String alias(String name) {
        return switch (name) {
            case "AuraDescription_lang_enUS", "AuraDescription" -> "SpellToolTip0";
            case "Description_lang_enUS", "Description" -> "SpellDescription0";
            case "SpellName_lang_enUS", "Name" -> "SpellName0";
            case "Rank_lang_enUS", "Rank" -> "SpellRank0";
            default -> name;
        };
    }

    public static DbcBinding load(Path bindingFile) throws IOException {
        String fileName = bindingFile.getFileName().toString();
        if (!fileName.endsWith(".txt")) {
            throw new IOException("binding must be Name.txt: " + bindingFile);
        }
        String dbcName = fileName.substring(0, fileName.length() - 4);
        List<String> lines = Files.readAllLines(bindingFile, StandardCharsets.UTF_8);
        List<BindingField> fields = new ArrayList<>();
        int index = 0;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 2) {
                throw new IOException("bad binding line: " + raw);
            }
            String type = parts[0].toLowerCase(Locale.ROOT);
            String name = parts[1];
            boolean stringMarked = parts.length >= 3 && "string".equalsIgnoreCase(parts[2]);
            BindingField.Kind kind;
            if (stringMarked || "string".equals(type)) {
                kind = BindingField.Kind.STRING;
            } else if ("float".equals(type)) {
                kind = BindingField.Kind.FLOAT;
            } else if ("int".equals(type)) {
                kind = BindingField.Kind.INT;
            } else if ("uint".equals(type)) {
                kind = BindingField.Kind.UINT;
            } else {
                throw new IOException("unknown binding type '" + type + "' in " + bindingFile);
            }
            fields.add(new BindingField(name, kind, index++));
        }
        if (fields.isEmpty()) {
            throw new IOException("empty binding: " + bindingFile);
        }
        return new DbcBinding(dbcName, fields);
    }

    public static Map<String, DbcBinding> loadDirectory(Path dir) throws IOException {
        Map<String, DbcBinding> out = new LinkedHashMap<>();
        if (dir == null || !Files.isDirectory(dir)) {
            return out;
        }
        try (var stream = Files.list(dir)) {
            for (Path p : stream.filter(f -> f.getFileName().toString().endsWith(".txt")).sorted().toList()) {
                DbcBinding b = load(p);
                out.put(b.dbcName().toLowerCase(Locale.ROOT), b);
            }
        }
        return out;
    }
}
