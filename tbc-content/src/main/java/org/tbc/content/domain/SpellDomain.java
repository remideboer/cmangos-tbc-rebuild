package org.tbc.content.domain;

import org.tbc.content.compile.CompileContext;
import org.tbc.content.compile.ContentDelta;
import org.tbc.content.dbc.DbcBinding;
import org.tbc.content.dbc.WdbcFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Patches Spell.dbc client fields and optional spell_template SQL. */
public final class SpellDomain implements ContentDomain {
    @Override
    public String kind() {
        return "spell";
    }

    @Override
    public void apply(ContentDelta delta, CompileContext ctx) {
        Map<String, Map<String, Object>> client = delta.client();
        if (!client.isEmpty()) {
            for (var dbcEntry : client.entrySet()) {
                String dbcName = dbcEntry.getKey();
                if (!"Spell".equalsIgnoreCase(dbcName)) {
                    throw new IllegalArgumentException(delta.source()
                            + ": spell kind only supports client.Spell (got client." + dbcName + ")");
                }
                DbcBinding binding = ctx.requireBinding("Spell");
                WdbcFile dbc = ctx.requireDbc("Spell");
                for (var fieldEntry : dbcEntry.getValue().entrySet()) {
                    dbc.setField(binding, delta.id(), fieldEntry.getKey(), fieldEntry.getValue());
                }
                ctx.markTouched("Spell");
            }
        }
        Map<String, Object> server = delta.server();
        if (!server.isEmpty()) {
            ctx.addSql(buildUpdate(delta.id(), server, delta.source()));
        }
    }

    public static String buildUpdate(int id, Map<String, Object> server, String source) {
        List<String> sets = new ArrayList<>();
        for (var e : server.entrySet()) {
            String col = e.getKey();
            if (!col.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException(source + ": bad SQL column '" + col + "'");
            }
            sets.add("`" + col + "`=" + sqlLiteral(e.getValue()));
        }
        return "-- " + source + "\nUPDATE `spell_template` SET "
                + String.join(", ", sets) + " WHERE `Id`=" + id + ";";
    }

    private static String sqlLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number n) {
            return n.toString();
        }
        if (value instanceof Boolean b) {
            return b ? "1" : "0";
        }
        String s = String.valueOf(value).replace("\\", "\\\\").replace("'", "''");
        return "'" + s + "'";
    }

    /** After all deltas, compact string blocks for touched Spell.dbc. */
    public static void finish(CompileContext ctx) {
        if (!ctx.touchedDbcs().stream().anyMatch(n -> n.equalsIgnoreCase("Spell"))) {
            return;
        }
        DbcBinding binding = ctx.requireBinding("Spell");
        WdbcFile dbc = ctx.requireDbc("Spell");
        dbc.rebuildStringBlock(binding);
    }
}
