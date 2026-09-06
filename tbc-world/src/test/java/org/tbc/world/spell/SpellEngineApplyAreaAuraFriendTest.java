package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-101 — SPELL_EFFECT_APPLY_AREA_AURA_FRIEND (128). Strength of Earth 31634.
 * CMaNGOS EffectApplyAreaAura: living unitTarget receives the aura.
 */
class SpellEngineApplyAreaAuraFriendTest {
    @Test
    void applyAreaAuraFriendWhenLivingShouldAddAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_APPLY_AREA_AURA_FRIEND));
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        SpellEngine.SpellInfo earth = new SpellEngine.SpellInfo(
                31634, SpellEngine.EFFECT_APPLY_AREA_AURA_FRIEND, 0, 0, 0, 20, 20, 0f);
        eng.apply(p, p, earth);
        assertEquals(1, p.auras.size());
        assertEquals(31634, p.auras.get(0).spellId());
    }

    @Test
    void applyAreaAuraFriendWhenDeadShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player dead = new Player();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo earth = new SpellEngine.SpellInfo(
                31634, SpellEngine.EFFECT_APPLY_AREA_AURA_FRIEND, 0, 0, 0, 20, 20, 0f);
        eng.apply(dead, dead, earth);
        assertEquals(0, dead.auras.size());
    }
}
