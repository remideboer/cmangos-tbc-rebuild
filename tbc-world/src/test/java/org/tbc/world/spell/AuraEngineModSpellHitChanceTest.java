package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-159 — SPELL_AURA_MOD_SPELL_HIT_CHANCE (55). Inspiring Presence 28878:
 * EffectBasePoints 0 + 1 = +1% spell hit. CMaNGOS HandleModSpellHitChance →
 * m_modSpellHitChance (creature path; player UpdateSpellHitChances).
 */
class AuraEngineModSpellHitChanceTest {
    private static final SpellEngine.SpellInfo INSPIRING_PRESENCE = new SpellEngine.SpellInfo(
            SpellEngine.INSPIRING_PRESENCE, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_SPELL_HIT_CHANCE, 0, 0, 1, 1, 0f);

    @Test
    void applyAuraWhenModSpellHitChanceOnPlayerShouldRaiseSpellHitChance() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_SPELL_HIT_CHANCE));
        Player player = new Player();

        eng.apply(player, player, INSPIRING_PRESENCE);

        assertTrue(player.hasAura(SpellEngine.INSPIRING_PRESENCE));
        assertEquals(1f, player.spellHitChance());
    }

    @Test
    void unapplyWhenModSpellHitChanceOnPlayerShouldRestoreSpellHitChance() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, INSPIRING_PRESENCE);
        eng.unapplyAura(player, SpellEngine.INSPIRING_PRESENCE);
        assertEquals(0f, player.spellHitChance());
    }

    @Test
    void applyWhenAmountZeroShouldLeaveSpellHitChance() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999055, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_SPELL_HIT_CHANCE,
                0, 0, 0, 0, 0f));
        assertEquals(0f, player.spellHitChance());
    }

    @Test
    void applyWhenCreatureShouldRaiseSpellHitChance() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, INSPIRING_PRESENCE);
        assertTrue(mob.hasAura(SpellEngine.INSPIRING_PRESENCE));
        assertEquals(1f, mob.spellHitChance());
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, INSPIRING_PRESENCE);
        new AuraEngine().unapply(null, INSPIRING_PRESENCE);
    }
}
