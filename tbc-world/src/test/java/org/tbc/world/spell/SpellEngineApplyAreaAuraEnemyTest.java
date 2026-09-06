package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-102 — SPELL_EFFECT_APPLY_AREA_AURA_ENEMY (129). Alluring Aura 29485.
 * CMaNGOS EffectApplyAreaAura: living unitTarget receives the aura.
 */
class SpellEngineApplyAreaAuraEnemyTest {
    @Test
    void applyAreaAuraEnemyWhenLivingShouldAddAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_APPLY_AREA_AURA_ENEMY));
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        SpellEngine.SpellInfo alluring = new SpellEngine.SpellInfo(
                29485, SpellEngine.EFFECT_APPLY_AREA_AURA_ENEMY, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, p, alluring);
        assertEquals(1, p.auras.size());
        assertEquals(29485, p.auras.get(0).spellId());
    }

    @Test
    void applyAreaAuraEnemyWhenDeadShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player dead = new Player();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo alluring = new SpellEngine.SpellInfo(
                29485, SpellEngine.EFFECT_APPLY_AREA_AURA_ENEMY, 0, 0, 0, 0, 0, 0f);
        eng.apply(dead, dead, alluring);
        assertEquals(0, dead.auras.size());
    }
}
