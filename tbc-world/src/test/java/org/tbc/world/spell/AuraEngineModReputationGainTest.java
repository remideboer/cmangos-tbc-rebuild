package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.ReputationMgr;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-268..269 — reputation gain (156) and faction reputation gain (190). */
class AuraEngineModReputationGainTest {
    @Test
    void reputationGainShouldScaleEveryGainAndInvertNegatives() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_REPUTATION_GAIN));
        Player player = new Player();
        SpellEngine.SpellInfo gain = rep(999156, AuraEngine.SPELL_AURA_MOD_REPUTATION_GAIN, 0, 25);
        eng.auras().apply(player, gain);
        player.modifyReputation(ReputationMgr.FACTION_STORMWIND, 100);
        assertEquals(125, player.reputationStanding(ReputationMgr.FACTION_STORMWIND));

        Player negative = new Player();
        eng.auras().apply(negative, gain);
        negative.modifyReputation(ReputationMgr.FACTION_STORMWIND, -100);
        assertEquals(-75, negative.reputationStanding(ReputationMgr.FACTION_STORMWIND));
    }

    @Test
    void factionReputationGainShouldApplyOnlyOnKillsForThatFaction() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_FACTION_REPUTATION_GAIN));
        Player player = new Player();
        eng.auras().apply(player, rep(999156, AuraEngine.SPELL_AURA_MOD_REPUTATION_GAIN, 0, 25));
        SpellEngine.SpellInfo faction = rep(999190, AuraEngine.SPELL_AURA_MOD_FACTION_REPUTATION_GAIN,
                ReputationMgr.FACTION_STORMWIND, 25);
        eng.auras().apply(player, faction);
        player.modifyReputation(ReputationMgr.FACTION_STORMWIND, 100);
        assertEquals(125, player.reputationStanding(ReputationMgr.FACTION_STORMWIND));
        player.modifyReputation(ReputationMgr.FACTION_STORMWIND, 100, true);
        assertEquals(275, player.reputationStanding(ReputationMgr.FACTION_STORMWIND));
        eng.auras().unapply(player, faction);
        player.modifyReputation(ReputationMgr.FACTION_STORMWIND, 100, true);
        assertEquals(400, player.reputationStanding(ReputationMgr.FACTION_STORMWIND));
    }

    private static SpellEngine.SpellInfo rep(int id, int aura, int faction, int amount) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA, aura, 0, 0, amount, amount, 0f, faction);
    }
}
