package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-047 — SPELL_EFFECT_CHARGE (96). Charge 100 Rank 1 relocates caster to unitTarget. */
class SpellEngineChargeTest {
    @Test
    void applyChargeWhenTargetShouldRelocateCasterToTarget() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_CHARGE));
        Player caster = new Player();
        caster.relocate(0f, 0f, 0f, 0f);
        Creature target = new Creature();
        target.relocate(10f, 4f, 1f, 0f);
        SpellEngine.SpellInfo charge = new SpellEngine.SpellInfo(
                100, SpellEngine.EFFECT_CHARGE, 0, 0, 0, 0, 0, 0f);
        eng.apply(caster, target, charge);
        assertEquals(10f, caster.x, 0.01f);
        assertEquals(4f, caster.y, 0.01f);
        assertEquals(1f, caster.z, 0.01f);
        assertEquals(10f, target.x, 0.01f);
        assertEquals(4f, target.y, 0.01f);
        assertEquals(1f, target.z, 0.01f);
        assertEquals((float) Math.atan2(4f, 10f), caster.o, 0.01f);
    }

    @Test
    void applyChargeWhenNullCasterShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature target = new Creature();
        target.relocate(10f, 0f, 0f, 0f);
        SpellEngine.SpellInfo charge = new SpellEngine.SpellInfo(
                100, SpellEngine.EFFECT_CHARGE, 0, 0, 0, 0, 0, 0f);
        eng.apply(null, target, charge);
        assertEquals(10f, target.x, 0.01f);
    }
}
