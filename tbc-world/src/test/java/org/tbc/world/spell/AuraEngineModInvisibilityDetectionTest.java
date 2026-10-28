package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-212 — SPELL_AURA_MOD_INVISIBILITY_DETECTION (19).
 * CMaNGOS HandleInvisibilityDetect sets the detect mask bit and adds the value.
 */
class AuraEngineModInvisibilityDetectionTest {
    private static final SpellEngine.SpellInfo DETECT = new SpellEngine.SpellInfo(
            999019, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_INVISIBILITY_DETECTION, 0, 0, 20, 20, 0f, 1);

    @Test
    void applyAuraWhenInvisibilityDetectShouldSetMaskAndValue() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_INVISIBILITY_DETECTION));
        Player player = new Player();
        Creature mob = new Creature();
        eng.apply(player, player, DETECT);
        eng.apply(player, mob, DETECT);
        assertEquals(1 << 1, player.invisibilityDetectMask());
        assertEquals(20, player.invisibilityDetectValue(1));
        assertEquals(20, mob.invisibilityDetectValue(1));
    }

    @Test
    void unapplyWhenAnotherAuraOfThatTypeRemainsShouldKeepTheMask() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        SpellEngine.SpellInfo lesser = new SpellEngine.SpellInfo(
                999020, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_INVISIBILITY_DETECTION, 0, 0, 5, 5, 0f, 1);
        eng.apply(player, player, DETECT);
        eng.apply(player, player, lesser);
        assertEquals(25, player.invisibilityDetectValue(1));
        eng.auras().unapply(player, DETECT);
        assertEquals(5, player.invisibilityDetectValue(1));
        assertEquals(1 << 1, player.invisibilityDetectMask());
        eng.auras().unapply(player, lesser);
        assertEquals(0, player.invisibilityDetectValue(1));
        assertEquals(0, player.invisibilityDetectMask());
    }

    @Test
    void applyWhenTypeOutOfRangeOrTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, DETECT);
        Player player = new Player();
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999021, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_INVISIBILITY_DETECTION,
                0, 0, 9, 9, 0f, 32));
        assertEquals(0, player.invisibilityDetectMask());
        assertEquals(0, player.invisibilityDetectValue(0));
    }
}
