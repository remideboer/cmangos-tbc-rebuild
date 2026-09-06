package org.tbc.world.spell;

import org.tbc.world.entity.DynamicObject;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-100 — SPELL_EFFECT_PERSISTENT_AREA_AURA (27). Blizzard 10.
 * CMaNGOS EffectPersistentAA: dynobject at dest (caster xyz stand-in).
 * Spell.dbc EffectRadiusIndex1 is 0 → GetSpellRadius 0.
 */
class SpellEnginePersistentAreaAuraTest {
    @Test
    void applyPersistentAreaAuraWhenCasterShouldCreateDynObjectAtDest() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_PERSISTENT_AREA_AURA));
        Player p = new Player();
        p.guid = 9;
        p.relocate(1f, 2f, 3f, 0.5f);
        SpellEngine.SpellInfo blizzard = new SpellEngine.SpellInfo(
                10, SpellEngine.EFFECT_PERSISTENT_AREA_AURA, 0, 0, 0, 25, 25, 0f);
        eng.apply(p, p, blizzard);
        DynamicObject dyn = p.lastDynObject();
        assertNotNull(dyn);
        assertEquals(DynamicObject.TYPEID_DYNAMICOBJECT, dyn.typeId);
        assertEquals(10, dyn.spellId);
        assertEquals(10, dyn.getInt(UpdateFields.OBJECT_FIELD_ENTRY));
        assertEquals(10, dyn.getInt(UpdateFields.DYNAMICOBJECT_SPELLID));
        assertEquals(DynamicObject.DYNAMIC_OBJECT_AREA_SPELL, dyn.getInt(UpdateFields.DYNAMICOBJECT_BYTES) & 0xFF);
        assertEquals(9, dyn.getGuid(UpdateFields.DYNAMICOBJECT_CASTER));
        assertEquals(1f, dyn.x, 0.01f);
        assertEquals(2f, dyn.y, 0.01f);
        assertEquals(3f, dyn.z, 0.01f);
        assertEquals(0f, dyn.getFloat(UpdateFields.DYNAMICOBJECT_RADIUS), 0.01f);
    }

    @Test
    void applyPersistentAreaAuraWhenMissingCasterOrSpellShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.guid = 9;
        SpellEngine.SpellInfo none = new SpellEngine.SpellInfo(
                0, SpellEngine.EFFECT_PERSISTENT_AREA_AURA, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, p, none);
        assertNull(p.lastDynObject());
        SpellEngine.SpellInfo blizzard = new SpellEngine.SpellInfo(
                10, SpellEngine.EFFECT_PERSISTENT_AREA_AURA, 0, 0, 0, 25, 25, 0f);
        eng.apply(null, p, blizzard);
        assertNull(p.lastDynObject());
        eng.persistentAreaAura(null, 10, 0f, 0f, 0f, 0f);
        eng.persistentAreaAura(p, 0, 0f, 0f, 0f, 0f);
        assertNull(p.lastDynObject());
    }
}
