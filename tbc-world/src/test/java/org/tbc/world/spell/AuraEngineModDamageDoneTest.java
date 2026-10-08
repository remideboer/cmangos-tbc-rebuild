package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-176 — SPELL_AURA_MOD_DAMAGE_DONE (13). Flat school bonus EquippedItemClass −1:
 * NORMAL → PLAYER_FIELD_MOD_DAMAGE_DONE_POS; magic mask → POS+i (CMaNGOS HandleModDamageDone).
 * Negative amount uses NEG fields.
 */
class AuraEngineModDamageDoneTest {
    /** Synthetic flat +35 all-magic (mask 126), EquippedItemClass −1 — Greater Arcane Elixir shape. */
    private static final SpellEngine.SpellInfo FLAT_MAGIC = new SpellEngine.SpellInfo(
            999013, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_DAMAGE_DONE, 0, 0, 35, 35, 0f, 126, -1);

    private static final SpellEngine.SpellInfo FLAT_PHYSICAL = new SpellEngine.SpellInfo(
            999014, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_DAMAGE_DONE, 0, 0, 10, 10, 0f, 1, -1);

    @Test
    void applyAuraWhenModDamageDoneMagicOnPlayerShouldRaiseDonePosPerSchool() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_DAMAGE_DONE));
        Player player = new Player();

        eng.apply(player, player, FLAT_MAGIC);

        assertTrue(player.hasAura(999013));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS));
        for (int i = 1; i < AuraEngine.MAX_SPELL_SCHOOL; i++) {
            assertEquals(35, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + i));
        }
    }

    @Test
    void unapplyWhenModDamageDoneShouldRestoreDonePos() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, FLAT_MAGIC);
        eng.auras().unapply(player, FLAT_MAGIC);
        for (int i = 1; i < AuraEngine.MAX_SPELL_SCHOOL; i++) {
            assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + i));
        }
    }

    @Test
    void applyWhenNormalSchoolMaskShouldRaisePhysicalDonePos() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, FLAT_PHYSICAL);
        assertEquals(10, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS));
    }

    @Test
    void applyWhenNegativeAmountShouldRaiseDoneNeg() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999015, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_DAMAGE_DONE,
                0, 0, -20, -20, 0f, 1, -1));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS));
        assertEquals(20, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_NEG));
    }

    @Test
    void applyWhenEquippedItemClassNotMinusOneShouldSkipMagicDonePos() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999016, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_DAMAGE_DONE,
                0, 0, 35, 35, 0f, 126, 2));
        for (int i = 1; i < AuraEngine.MAX_SPELL_SCHOOL; i++) {
            assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + i));
        }
    }

    @Test
    void applyWhenAmountZeroShouldLeaveDonePos() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999017, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_DAMAGE_DONE,
                0, 0, 0, 0, 0f, 126, -1));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + 2));
    }

    @Test
    void applyWhenCreatureShouldNoOpWithoutPlayerDonePosField() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, FLAT_MAGIC);
        assertTrue(mob.hasAura(999013));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, FLAT_MAGIC);
        new AuraEngine().unapply(null, FLAT_MAGIC);
    }
}
