package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-177 — SPELL_AURA_MOD_HEALING_DONE (135). Flat healing bonus for client:
 * CMaNGOS HandleModHealingDone → UpdateSpellHealingBonus → PLAYER_FIELD_MOD_HEALING_DONE_POS.
 */
class AuraEngineModHealingDoneTest {
    /** Synthetic +35 all-schools (mask 127) — Elixir of Healing Power shape. */
    private static final SpellEngine.SpellInfo FLAT_HEAL = new SpellEngine.SpellInfo(
            999135, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_HEALING_DONE, 0, 0, 35, 35, 0f, 127);

    @Test
    void applyAuraWhenModHealingDoneOnPlayerShouldRaiseHealingDonePos() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_HEALING_DONE));
        Player player = new Player();

        eng.apply(player, player, FLAT_HEAL);

        assertTrue(player.hasAura(999135));
        assertEquals(35, player.getInt(UpdateFields.PLAYER_FIELD_MOD_HEALING_DONE_POS));
    }

    @Test
    void unapplyWhenModHealingDoneShouldRestoreHealingDonePos() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, FLAT_HEAL);
        eng.auras().unapply(player, FLAT_HEAL);
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_HEALING_DONE_POS));
    }

    @Test
    void applyWhenMiscZeroShouldLeaveHealingDonePos() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999136, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_HEALING_DONE,
                0, 0, 35, 35, 0f, 0));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_HEALING_DONE_POS));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveHealingDonePos() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999137, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_HEALING_DONE,
                0, 0, 0, 0, 0f, 127));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_HEALING_DONE_POS));
    }

    @Test
    void applyWhenCreatureShouldNoOpWithoutPlayerHealingField() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, FLAT_HEAL);
        assertTrue(mob.hasAura(999135));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, FLAT_HEAL);
        new AuraEngine().unapply(null, FLAT_HEAL);
    }
}
