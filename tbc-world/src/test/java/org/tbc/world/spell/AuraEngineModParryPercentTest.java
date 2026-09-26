package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-156 — SPELL_AURA_MOD_PARRY_PERCENT (47). Nimble Reflexes 3238:
 * EffectBasePoints 49 + 1 = +50% parry. CMaNGOS HandleAuraModParryPercent → UpdateParryPercentage.
 */
class AuraEngineModParryPercentTest {
    private static final SpellEngine.SpellInfo NIMBLE_REFLEXES = new SpellEngine.SpellInfo(
            SpellEngine.NIMBLE_REFLEXES, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_PARRY_PERCENT, 0, 0, 50, 50, 0f);

    @Test
    void applyAuraWhenModParryPercentOnPlayerShouldRaiseParryPercentage() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_PARRY_PERCENT));
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_PARRY_PERCENTAGE, 5f);

        eng.apply(player, player, NIMBLE_REFLEXES);

        assertTrue(player.hasAura(SpellEngine.NIMBLE_REFLEXES));
        assertEquals(55f, player.getFloat(UpdateFields.PLAYER_PARRY_PERCENTAGE));
    }

    @Test
    void unapplyWhenModParryPercentOnPlayerShouldRestoreParryPercentage() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_PARRY_PERCENTAGE, 5f);
        eng.apply(player, player, NIMBLE_REFLEXES);
        eng.unapplyAura(player, SpellEngine.NIMBLE_REFLEXES);
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_PARRY_PERCENTAGE));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveParryPercentage() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_PARRY_PERCENTAGE, 5f);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999047, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_PARRY_PERCENT,
                0, 0, 0, 0, 0f));
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_PARRY_PERCENTAGE));
    }

    @Test
    void applyWhenCreatureShouldNoOpWithoutPlayerParryField() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, NIMBLE_REFLEXES);
        assertTrue(mob.hasAura(SpellEngine.NIMBLE_REFLEXES));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, NIMBLE_REFLEXES);
        new AuraEngine().unapply(null, NIMBLE_REFLEXES);
    }
}
