package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-147 — SPELL_AURA_MOD_RANGED_HASTE (140). Spell.dbc 3045 Rapid Fire (+40%).
 * CMaNGOS HandleAuraModRangedHaste → ApplyAttackTimePercentMod(RANGED).
 */
class AuraEngineModRangedHasteTest {
    private static final SpellEngine.SpellInfo RAPID_FIRE = new SpellEngine.SpellInfo(
            SpellEngine.RAPID_FIRE, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_RANGED_HASTE, 0, 0, 40, 40, 0f);

    @Test
    void applyAuraWhenRangedHasteOnPlayerShouldShortenRangedAttackTime() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_RANGED_HASTE));
        Player player = new Player();
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME));
        eng.apply(player, player, RAPID_FIRE);
        assertTrue(player.hasAura(SpellEngine.RAPID_FIRE));
        assertEquals(Math.round(2000 * 100.0f / 140.0f),
                player.getInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME));
    }

    @Test
    void unapplyWhenRangedHasteOnPlayerShouldRestoreRangedAttackTime() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, RAPID_FIRE);
        eng.unapplyAura(player, SpellEngine.RAPID_FIRE);
        // +40% on int ms 2000 is not an exact dyadic; round-trip stays within 1 ms of base.
        assertEquals(2000.0, player.getInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME), 1.0);
    }

    @Test
    void applyWhenAmountZeroShouldLeaveRangedAttackTime() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999140, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_RANGED_HASTE,
                0, 0, 0, 0, 0f));
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, RAPID_FIRE);
        new AuraEngine().unapply(null, RAPID_FIRE);
    }
}
