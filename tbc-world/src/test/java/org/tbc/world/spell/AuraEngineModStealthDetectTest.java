package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-210 — SPELL_AURA_MOD_STEALTH_DETECT (17).
 * CMaNGOS HandleModStealthDetect → AddStealthDetectionStrength.
 */
class AuraEngineModStealthDetectTest {
    private static final SpellEngine.SpellInfo DETECT = new SpellEngine.SpellInfo(
            999017, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_STEALTH_DETECT, 0, 0, 30, 30, 0f);

    @Test
    void applyAuraWhenStealthDetectShouldAddDetectionStrength() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_STEALTH_DETECT));
        Player player = new Player();
        Creature mob = new Creature();
        eng.apply(player, player, DETECT);
        eng.apply(player, mob, DETECT);
        assertEquals(30, player.stealthDetectStrength());
        assertEquals(30, mob.stealthDetectStrength());
    }

    @Test
    void unapplyWhenStealthDetectShouldRemoveDetectionStrength() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, DETECT);
        eng.auras().unapply(player, DETECT);
        assertEquals(0, player.stealthDetectStrength());
    }

    @Test
    void applyWhenAmountZeroOrTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, DETECT);
        Player player = new Player();
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999018, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STEALTH_DETECT,
                0, 0, 0, 0, 0f));
        assertEquals(0, player.stealthDetectStrength());
    }
}
