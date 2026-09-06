package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-058 — SPELL_EFFECT_CHARGE_DEST (149). Eagle Swoop 44732 MoveCharge to dest. */
class SpellEngineChargeDestTest {
    @Test
    void applyChargeDestWhenCasterShouldRelocateAlongFacing() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_CHARGE_DEST));
        Player caster = new Player();
        caster.relocate(0f, 0f, 0f, 0f);
        Creature dummy = new Creature();
        dummy.relocate(100f, 100f, 2f, 0f);
        SpellEngine.SpellInfo swoop = new SpellEngine.SpellInfo(
                44732, SpellEngine.EFFECT_CHARGE_DEST, 0, 0, 0, 0, 0, 10f);
        eng.apply(caster, dummy, swoop);
        assertEquals(10f, caster.x, 0.01f);
        assertEquals(0f, caster.y, 0.01f);
        assertEquals(0f, caster.z, 0.01f);
        assertEquals(0f, caster.o, 0.01f);
        assertEquals(100f, dummy.x, 0.01f);
        assertEquals(100f, dummy.y, 0.01f);
    }

    @Test
    void applyChargeDestWhenNullCasterShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature dummy = new Creature();
        dummy.relocate(100f, 0f, 0f, 0f);
        SpellEngine.SpellInfo swoop = new SpellEngine.SpellInfo(
                44732, SpellEngine.EFFECT_CHARGE_DEST, 0, 0, 0, 0, 0, 10f);
        eng.apply(null, dummy, swoop);
        assertEquals(100f, dummy.x, 0.01f);
    }
}
