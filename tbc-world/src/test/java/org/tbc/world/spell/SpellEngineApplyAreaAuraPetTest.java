package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-103 — SPELL_EFFECT_APPLY_AREA_AURA_PET (119). Spirit Bond 19579.
 * CMaNGOS EffectApplyAreaAura: living unitTarget receives the aura.
 */
class SpellEngineApplyAreaAuraPetTest {
    @Test
    void applyAreaAuraPetWhenLivingShouldAddAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_APPLY_AREA_AURA_PET));
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        SpellEngine.SpellInfo bond = new SpellEngine.SpellInfo(
                19579, SpellEngine.EFFECT_APPLY_AREA_AURA_PET, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, p, bond);
        assertEquals(1, p.auras.size());
        assertEquals(19579, p.auras.get(0).spellId());
    }

    @Test
    void applyAreaAuraPetWhenDeadShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player dead = new Player();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        SpellEngine.SpellInfo bond = new SpellEngine.SpellInfo(
                19579, SpellEngine.EFFECT_APPLY_AREA_AURA_PET, 0, 0, 0, 0, 0, 0f);
        eng.apply(dead, dead, bond);
        assertEquals(0, dead.auras.size());
    }
}
