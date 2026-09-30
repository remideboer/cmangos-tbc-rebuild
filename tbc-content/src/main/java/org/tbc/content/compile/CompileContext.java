package org.tbc.content.compile;

import org.tbc.content.dbc.DbcBinding;
import org.tbc.content.dbc.WdbcFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Mutable compile state: loaded DBCs, SQL statements, paths. */
public final class CompileContext {
    private final Path baseDbcDir;
    private final Path outDir;
    private final Map<String, DbcBinding> bindings;
    private final Map<String, WdbcFile> dbcs = new LinkedHashMap<>();
    private final List<String> sqlStatements = new ArrayList<>();
    private final List<String> touchedDbcs = new ArrayList<>();

    public CompileContext(Path baseDbcDir, Path outDir, Map<String, DbcBinding> bindings) {
        this.baseDbcDir = baseDbcDir;
        this.outDir = outDir;
        this.bindings = bindings;
    }

    public Path baseDbcDir() {
        return baseDbcDir;
    }

    public Path outDir() {
        return outDir;
    }

    public DbcBinding requireBinding(String dbcName) {
        DbcBinding b = bindings.get(dbcName.toLowerCase(Locale.ROOT));
        if (b == null) {
            throw new IllegalStateException("no binding for DBC '" + dbcName + "'");
        }
        return b;
    }

    public WdbcFile requireDbc(String dbcName) {
        String key = dbcName.toLowerCase(Locale.ROOT);
        WdbcFile existing = dbcs.get(key);
        if (existing != null) {
            return existing;
        }
        DbcBinding binding = requireBinding(dbcName);
        Path canonical = baseDbcDir.resolve(binding.dbcName() + ".dbc");
        if (!canonical.toFile().isFile()) {
            throw new IllegalStateException("base DBC missing: " + canonical
                    + " (set --base-dbc to DataDir/dbc)");
        }
        try {
            WdbcFile loaded = WdbcFile.load(canonical);
            if (loaded.fieldCount() != binding.fieldCount()) {
                throw new IllegalStateException(canonical + " fieldCount=" + loaded.fieldCount()
                        + " but binding has " + binding.fieldCount());
            }
            dbcs.put(key, loaded);
            markTouched(binding.dbcName());
            return loaded;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("failed to load " + canonical + ": " + e.getMessage(), e);
        }
    }

    public void markTouched(String dbcName) {
        DbcBinding b = requireBinding(dbcName);
        if (!touchedDbcs.contains(b.dbcName())) {
            touchedDbcs.add(b.dbcName());
        }
    }

    public void addSql(String statement) {
        sqlStatements.add(statement);
    }

    public List<String> sqlStatements() {
        return List.copyOf(sqlStatements);
    }

    public List<String> touchedDbcs() {
        return List.copyOf(touchedDbcs);
    }

    public Map<String, WdbcFile> dbcs() {
        return dbcs;
    }
}
