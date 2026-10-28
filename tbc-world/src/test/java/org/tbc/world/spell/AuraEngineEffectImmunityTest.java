package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-239 — SPELL_AURA_EFFECT_IMMUNITY (37). */
class AuraEngineEffectImmunityTest {
    @Test
    void applyAuraWhenEffectImmunityShouldRecordTheEffectId() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_EFFECT_IMMUNITY));
        Player player = new Player();
        SpellEngine.SpellInfo aura = new SpellEngine.SpellInfo(
                999037, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_EFFECT_IMMUNITY, 0, 0, 1, 1, 0f, 2);
        eng.apply(player, player, aura);
        assertTrue(player.isImmuneToEffect(2));
        assertFalse(player.isImmuneToEffect(6));
        eng.auras().unapply(player, aura);
        assertFalse(player.isImmuneToEffect(2));
    }
}
