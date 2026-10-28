package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-247 — SPELL_AURA_FORCE_REACTION (139). */
class AuraEngineForceReactionTest {
    @Test
    void applyAuraWhenForceReactionShouldStoreTheRankForThatFaction() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_FORCE_REACTION));
        Player player = new Player();
        SpellEngine.SpellInfo stormwind = aura(999139, 4, 72);
        SpellEngine.SpellInfo honored = aura(999140, 5, 72);
        SpellEngine.SpellInfo hated = aura(999141, 0, 21);
        eng.apply(player, player, stormwind);
        assertEquals(4, player.reputations.forcedReaction(72));
        eng.apply(player, player, honored);
        assertEquals(5, player.reputations.forcedReaction(72));
        eng.apply(player, player, hated);
        assertEquals(0, player.reputations.forcedReaction(21));
        eng.auras().unapply(player, stormwind);
        assertEquals(-1, player.reputations.forcedReaction(72));
        assertEquals(0, player.reputations.forcedReaction(21));

        Creature creature = new Creature();
        eng.apply(creature, creature, stormwind);
        Player copy = new Player();
        copy.reputations.copyFrom(player.reputations);
        assertEquals(0, copy.reputations.forcedReaction(21));
    }

    private static SpellEngine.SpellInfo aura(int id, int rank, int faction) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_FORCE_REACTION, 0, 0, rank, rank, 0f, faction);
    }
}
