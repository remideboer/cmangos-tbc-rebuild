package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-104 — SPELL_EFFECT_APPLY_AREA_AURA_OWNER (143). Soul Link 25228.
 * CMaNGOS EffectApplyAreaAura: living unitTarget receives the aura.
 */
class SpellEngineApplyAreaAuraOwnerTest {
    @Test
    void applyAreaAuraOwnerWhenLivingShouldAddAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_APPLY_AREA_AURA_OWNER));
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        SpellEngine.SpellInfo link = new SpellEngine.SpellInfo(
                25228, SpellEngine.EFFECT_APPLY_AREA_AURA_OWNER, 0, 0, 0, 5, 5, 0f, 127);
        eng.apply(p, p, link);
        assertEquals(1, p.auras.size());
        assertEquals(25228, p.auras.get(0).spellId());
    }

    @Test
    void applyAreaAuraOwnerWhenDeadShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player dead = new Player();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo link = new SpellEngine.SpellInfo(
                25228, SpellEngine.EFFECT_APPLY_AREA_AURA_OWNER, 0, 0, 0, 5, 5, 0f, 127);
        eng.apply(dead, dead, link);
        assertEquals(0, dead.auras.size());
    }
}
