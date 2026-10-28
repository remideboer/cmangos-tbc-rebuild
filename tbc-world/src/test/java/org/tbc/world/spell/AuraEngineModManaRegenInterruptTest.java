package org.tbc.world.spell;

import org.tbc.world.content.LevelStats;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-206 — SPELL_AURA_MOD_MANA_REGEN_INTERRUPT (134).
 * CMaNGOS HandleAuraModRegenInterrupt → UpdateManaRegen interrupt = spirit rate × percent.
 */
class AuraEngineModManaRegenInterruptTest {
    private static final SpellEngine.SpellInfo MEDITATION = new SpellEngine.SpellInfo(
            999134, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_MANA_REGEN_INTERRUPT, 0, 0, 50, 50, 0f);

    @Test
    void applyAuraWhenManaRegenInterruptShouldFillInterruptField() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_MANA_REGEN_INTERRUPT));
        Player player = mage();
        float spirit = player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN);
        assertTrue(spirit > 0f);
        eng.apply(player, player, MEDITATION);
        assertEquals(spirit * 0.5f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN_INTERRUPT), 1e-4f);
    }

    @Test
    void unapplyWhenManaRegenInterruptShouldClearInterruptField() {
        SpellEngine eng = new SpellEngine();
        Player player = mage();
        eng.apply(player, player, MEDITATION);
        eng.auras().unapply(player, MEDITATION);
        assertEquals(0f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN_INTERRUPT), 1e-4f);
    }

    @Test
    void applyWhenCreatureOrZeroAmountShouldNoOp() {
        new AuraEngine().apply(null, MEDITATION);
        new SpellEngine().apply(new Player(), new Creature(), MEDITATION);
        Player player = mage();
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999135, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_MANA_REGEN_INTERRUPT,
                0, 0, 0, 0, 0f));
        assertEquals(0f, player.getFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN_INTERRUPT), 1e-4f);
    }

    private static Player mage() {
        Player player = new Player();
        player.race = 1;
        player.clazz = 8;
        player.level = 1;
        player.initStatsForLevel(LevelStats.defaults());
        return player;
    }
}
