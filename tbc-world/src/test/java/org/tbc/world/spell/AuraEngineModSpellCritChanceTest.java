package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-160 — SPELL_AURA_MOD_SPELL_CRIT_CHANCE (57). Moonkin Aura 24907:
 * EffectBasePoints 4 + 1 = +5% spell crit all schools. CMaNGOS HandleModSpellCritChance →
 * UpdateAllSpellCritChances → PLAYER_SPELL_CRIT_PERCENTAGE1..+6.
 */
class AuraEngineModSpellCritChanceTest {
    private static final SpellEngine.SpellInfo MOONKIN_AURA = new SpellEngine.SpellInfo(
            SpellEngine.MOONKIN_AURA, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_SPELL_CRIT_CHANCE, 0, 0, 5, 5, 0f);

    @Test
    void applyAuraWhenModSpellCritChanceOnPlayerShouldRaiseAllSchoolCritFields() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_SPELL_CRIT_CHANCE));
        Player player = new Player();
        for (int i = 0; i < AuraEngine.MAX_SPELL_SCHOOL; i++) {
            player.setFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + i, 5f);
        }

        eng.apply(player, player, MOONKIN_AURA);

        assertTrue(player.hasAura(SpellEngine.MOONKIN_AURA));
        for (int i = 0; i < AuraEngine.MAX_SPELL_SCHOOL; i++) {
            assertEquals(10f, player.getFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + i));
        }
    }

    @Test
    void unapplyWhenModSpellCritChanceOnPlayerShouldRestoreCritFields() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        for (int i = 0; i < AuraEngine.MAX_SPELL_SCHOOL; i++) {
            player.setFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + i, 5f);
        }
        eng.apply(player, player, MOONKIN_AURA);
        eng.unapplyAura(player, SpellEngine.MOONKIN_AURA);
        for (int i = 0; i < AuraEngine.MAX_SPELL_SCHOOL; i++) {
            assertEquals(5f, player.getFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + i));
        }
    }

    @Test
    void applyWhenAmountZeroShouldLeaveSpellCritFields() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1, 5f);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999057, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_SPELL_CRIT_CHANCE,
                0, 0, 0, 0, 0f));
        assertEquals(5f, player.getFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1));
    }

    @Test
    void applyWhenCreatureShouldRaiseSpellCritChance() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, MOONKIN_AURA);
        assertTrue(mob.hasAura(SpellEngine.MOONKIN_AURA));
        for (int i = 0; i < AuraEngine.MAX_SPELL_SCHOOL; i++) {
            assertEquals(5f, mob.spellCritChance(i));
        }
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, MOONKIN_AURA);
        new AuraEngine().unapply(null, MOONKIN_AURA);
    }
}
