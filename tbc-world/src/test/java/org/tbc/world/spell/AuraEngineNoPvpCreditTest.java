package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.pvp.Honor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-270 — no PvP credit (159) blocks RewardHonor for that victim. */
class AuraEngineNoPvpCreditTest {
    @Test
    void noPvpCreditShouldBlockRewardFromVictimUntilEveryAuraDrops() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_NO_PVP_CREDIT));
        Player killer = new Player();
        Player victim = new Player();
        victim.guid = 42;
        Honor.rewardFromVictim(killer, victim, 10);
        assertEquals(10, killer.honorPoints);

        SpellEngine.SpellInfo first = credit(999159);
        SpellEngine.SpellInfo second = credit(999160);
        eng.auras().apply(victim, first);
        eng.auras().apply(victim, second);
        Honor.rewardFromVictim(killer, victim, 10);
        assertEquals(10, killer.honorPoints);
        Honor.reward(killer, null, victim.guid, 10);
        assertEquals(20, killer.honorPoints);

        eng.auras().unapply(victim, first);
        Honor.rewardFromVictim(killer, victim, 10);
        assertEquals(20, killer.honorPoints);
        eng.auras().unapply(victim, second);
        Honor.rewardFromVictim(killer, victim, 10);
        assertEquals(30, killer.honorPoints);
    }

    private static SpellEngine.SpellInfo credit(int id) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_NO_PVP_CREDIT, 0, 0, 0, 0, 0f);
    }
}
