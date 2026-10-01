package org.tbc.content.domain;

import org.tbc.content.compile.CompileContext;
import org.tbc.content.compile.ContentDelta;
import org.tbc.content.dbc.DbcBinding;
import org.tbc.content.dbc.WdbcFile;

import java.util.Map;

/**
 * Patches or inserts {@code ChrClasses.dbc} rows (e.g. classless id 6).
 * New ids clone a template row from {@code server.templateId} (default 1 / Warrior).
 */
public final class ChrClassesDomain implements ContentDomain {
    @Override
    public String kind() {
        return "chrclasses";
    }

    @Override
    public void apply(ContentDelta delta, CompileContext ctx) {
        Map<String, Map<String, Object>> client = delta.client();
        if (client.isEmpty()) {
            return;
        }
        Map<String, Object> fields = client.get("ChrClasses");
        if (fields == null) {
            throw new IllegalArgumentException(delta.source()
                    + ": chrclasses kind requires client.ChrClasses");
        }
        DbcBinding binding = ctx.requireBinding("ChrClasses");
        WdbcFile dbc = ctx.requireDbc("ChrClasses");
        int templateId = 1;
        Object tmpl = delta.server().get("templateId");
        if (tmpl instanceof Number n) {
            templateId = n.intValue();
        }
        if (dbc.findRecordIndexById(delta.id()) < 0) {
            dbc.cloneRecord(templateId, delta.id());
        }
        for (var fieldEntry : fields.entrySet()) {
            dbc.setField(binding, delta.id(), fieldEntry.getKey(), fieldEntry.getValue());
        }
        ctx.markTouched("ChrClasses");
    }

    public static void finish(CompileContext ctx) {
        if (ctx.touchedDbcs().stream().noneMatch(n -> n.equalsIgnoreCase("ChrClasses"))) {
            return;
        }
        DbcBinding binding = ctx.requireBinding("ChrClasses");
        WdbcFile dbc = ctx.requireDbc("ChrClasses");
        dbc.rebuildStringBlock(binding);
    }
}
