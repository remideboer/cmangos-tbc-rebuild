package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-163 — SPELL_AURA_MOD_DAMAGE_PERCENT_DONE (79). Death Wish 12292:
 * EffectBasePoints 19 + 1 = +20%; misc SPELL_SCHOOL_MASK_NORMAL; EquippedItemClass −1.
 * CMaNGOS HandleModDamagePercentDone → PLAYER_FIELD_MOD_DAMAGE_DONE_PCT += amount/100.
 */
class AuraEngineModDamagePercentDoneTest {
    private static final SpellEngine.SpellInfo DEATH_WISH = new SpellEngine.SpellInfo(
            SpellEngine.DEATH_WISH, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_DAMAGE_PERCENT_DONE, 0, 0, 20, 20, 0f, 1);

    @Test
    void applyAuraWhenModDamagePercentDoneOnPlayerShouldRaisePhysicalDonePct() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_DAMAGE_PERCENT_DONE));
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT, 1f);

        eng.apply(player, player, DEATH_WISH);

        assertTrue(player.hasAura(SpellEngine.DEATH_WISH));
        assertEquals(1.20f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT), 1e-5f);
    }

    @Test
    void unapplyWhenModDamagePercentDoneShouldRestoreDonePct() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT, 1f);
        eng.apply(player, player, DEATH_WISH);
        eng.unapplyAura(player, SpellEngine.DEATH_WISH);
        assertEquals(1f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT), 1e-5f);
    }

    @Test
    void applyWhenAmountZeroShouldLeaveDonePct() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT, 1f);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999079, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_DAMAGE_PERCENT_DONE,
                0, 0, 0, 0, 0f, 1));
        assertEquals(1f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT), 1e-5f);
    }

    @Test
    void applyWhenNoNormalSchoolMaskShouldLeavePhysicalDonePct() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT, 1f);
        player.setFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT + 1, 1f);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999080, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_DAMAGE_PERCENT_DONE,
                0, 0, 20, 20, 0f, 126));
        assertEquals(1f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT), 1e-5f);
        assertEquals(1.20f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT + 1), 1e-5f);
    }

    @Test
    void applyWhenCreatureShouldNoOpWithoutPlayerDonePctField() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, DEATH_WISH);
        assertTrue(mob.hasAura(SpellEngine.DEATH_WISH));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, DEATH_WISH);
        new AuraEngine().unapply(null, DEATH_WISH);
    }
}
