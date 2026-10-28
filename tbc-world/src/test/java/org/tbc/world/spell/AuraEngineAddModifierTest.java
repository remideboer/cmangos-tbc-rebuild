package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-248 and TP-SL26-249 — SPELL_AURA_ADD_FLAT_MODIFIER (107) and ADD_PCT_MODIFIER (108). */
class AuraEngineAddModifierTest {
    @Test
    void applyAuraWhenFlatAndPercentModsShouldFollowApplySpellMod() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_ADD_FLAT_MODIFIER));
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_ADD_PCT_MODIFIER));
        Player player = new Player();
        SpellEngine.SpellInfo flat = mod(999107, AuraEngine.SPELL_AURA_ADD_FLAT_MODIFIER, 10, AuraEngine.SPELLMOD_COST);
        SpellEngine.SpellInfo pct = mod(999108, AuraEngine.SPELL_AURA_ADD_PCT_MODIFIER, -50, AuraEngine.SPELLMOD_COST);
        eng.apply(player, player, flat);
        eng.apply(player, player, pct);
        assertEquals(55, player.applySpellMod(AuraEngine.SPELLMOD_COST, 100));
        assertEquals(100, player.applySpellMod(AuraEngine.SPELLMOD_DAMAGE, 100));
        eng.auras().unapply(player, pct);
        assertEquals(110, player.applySpellMod(AuraEngine.SPELLMOD_COST, 100));

        Player other = new Player();
        eng.apply(other, other, mod(999109, AuraEngine.SPELL_AURA_ADD_FLAT_MODIFIER, 10, AuraEngine.SPELLMOD_DAMAGE));
        assertEquals(10, other.applySpellMod(AuraEngine.SPELLMOD_DAMAGE, 0));
        eng.apply(other, other, mod(999110, AuraEngine.SPELL_AURA_ADD_FLAT_MODIFIER, 10, AuraEngine.SPELLMOD_COST));
        assertEquals(0, other.applySpellMod(AuraEngine.SPELLMOD_COST, 0));

        Player cast = new Player();
        eng.apply(cast, cast, mod(999111, AuraEngine.SPELL_AURA_ADD_PCT_MODIFIER, -100,
                AuraEngine.SPELLMOD_CASTING_TIME));
        assertEquals(10_000, cast.applySpellMod(AuraEngine.SPELLMOD_CASTING_TIME, 10_000));
        eng.apply(cast, cast, mod(999112, AuraEngine.SPELL_AURA_ADD_PCT_MODIFIER, -60, AuraEngine.SPELLMOD_DAMAGE));
        eng.apply(cast, cast, mod(999113, AuraEngine.SPELL_AURA_ADD_PCT_MODIFIER, -60, AuraEngine.SPELLMOD_DAMAGE));
        assertEquals(0, cast.applySpellMod(AuraEngine.SPELLMOD_DAMAGE, 100));

        Player skipped = new Player();
        eng.apply(skipped, skipped, mod(999114, AuraEngine.SPELL_AURA_ADD_FLAT_MODIFIER, 5, AuraEngine.MAX_SPELLMOD));
        eng.apply(skipped, skipped, mod(999115, AuraEngine.SPELL_AURA_ADD_FLAT_MODIFIER, 5, -1));
        eng.apply(skipped, skipped, mod(999116, AuraEngine.SPELL_AURA_ADD_FLAT_MODIFIER, 0, AuraEngine.SPELLMOD_COST));
        assertEquals(40, skipped.applySpellMod(AuraEngine.SPELLMOD_COST, 40));

        Creature creature = new Creature();
        eng.apply(creature, creature, flat);

        Player copy = new Player();
        copy.copyCreateStatsFrom(player);
        assertEquals(110, copy.applySpellMod(AuraEngine.SPELLMOD_COST, 100));
    }

    @Test
    void applyAuraWhenCostIsAlreadyZeroShouldStopLaterMods() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, mod(999117, AuraEngine.SPELL_AURA_ADD_PCT_MODIFIER, -100, AuraEngine.SPELLMOD_COST));
        eng.apply(player, player, mod(999118, AuraEngine.SPELL_AURA_ADD_FLAT_MODIFIER, 50, AuraEngine.SPELLMOD_COST));
        assertEquals(0, player.applySpellMod(AuraEngine.SPELLMOD_COST, 100));
    }

    private static SpellEngine.SpellInfo mod(int id, int aura, int amount, int op) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA, aura, 0, 0, amount, amount, 0f, op);
    }
}
