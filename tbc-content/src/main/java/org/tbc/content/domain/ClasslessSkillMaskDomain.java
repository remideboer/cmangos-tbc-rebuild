package org.tbc.content.domain;

import org.tbc.content.compile.CompileContext;
import org.tbc.content.compile.ContentDelta;
import org.tbc.content.dbc.BindingField;
import org.tbc.content.dbc.DbcBinding;
import org.tbc.content.dbc.WdbcFile;

import java.util.Map;

/**
 * Bulk-OR {@code ClassMask} on SkillLineAbility / SkillRaceClassInfo so ChrClasses id 6
 * (Classless / DK bit 0x20) passes the 8606 client trainer-list skill filter.
 * Rows with ClassMask 0 already mean “any class” and are left alone.
 */
public final class ClasslessSkillMaskDomain implements ContentDomain {
    @Override
    public String kind() {
        return "classless-skill-mask";
    }

    @Override
    public void apply(ContentDelta delta, CompileContext ctx) {
        Map<String, Map<String, Object>> client = delta.client();
        if (client.isEmpty()) {
            throw new IllegalArgumentException(delta.source()
                    + ": classless-skill-mask requires client.SkillLineAbility and/or SkillRaceClassInfo");
        }
        for (var e : client.entrySet()) {
            String dbcName = e.getKey();
            Map<String, Object> fields = e.getValue();
            Object orObj = fields.get("orClassMask");
            if (orObj == null) {
                throw new IllegalArgumentException(delta.source()
                        + ": client." + dbcName + " requires orClassMask");
            }
            int orMask = orObj instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(orObj));
            if (orMask == 0) {
                continue;
            }
            DbcBinding binding = ctx.requireBinding(dbcName);
            BindingField classMask = binding.field("ClassMask")
                    .orElseThrow(() -> new IllegalStateException(dbcName + " binding missing ClassMask"));
            WdbcFile dbc = ctx.requireDbc(dbcName);
            int idx = classMask.index();
            int patched = 0;
            for (int[] row : dbc.records()) {
                int mask = row[idx];
                if (mask == 0) {
                    continue;
                }
                int next = mask | orMask;
                if (next != mask) {
                    row[idx] = next;
                    patched++;
                }
            }
            ctx.markTouched(dbcName);
            System.out.println("content: " + dbcName + " orClassMask=0x"
                    + Integer.toHexString(orMask) + " patched " + patched + " rows");
        }
    }
}
