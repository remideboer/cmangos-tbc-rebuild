package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-055 — SPELL_EFFECT_LEAP (29). Blink 1953 NearTeleportTo dest, keep facing. */
class SpellEngineLeapTest {
    @Test
    void applyLeapWhenTargetShouldNearTeleportAlongFacing() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_LEAP));
        Player jumper = new Player();
        jumper.relocate(10f, 20f, 5f, 0f);
        SpellEngine.SpellInfo blink = new SpellEngine.SpellInfo(
                1953, SpellEngine.EFFECT_LEAP, 0, 0, 0, 0, 0, 8f);
        eng.apply(jumper, jumper, blink);
        assertEquals(18f, jumper.x, 0.01f);
        assertEquals(20f, jumper.y, 0.01f);
        assertEquals(5f, jumper.z, 0.01f);
        assertEquals(0f, jumper.o, 0.01f);
    }

    @Test
    void applyLeapWhenNullTargetShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player jumper = new Player();
        jumper.relocate(10f, 20f, 5f, 0f);
        SpellEngine.SpellInfo blink = new SpellEngine.SpellInfo(
                1953, SpellEngine.EFFECT_LEAP, 0, 0, 0, 0, 0, 8f);
        eng.apply(jumper, null, blink);
        assertEquals(10f, jumper.x, 0.01f);
        assertEquals(20f, jumper.y, 0.01f);
    }
}
