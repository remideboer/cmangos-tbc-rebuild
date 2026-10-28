package org.tbc.world.spell;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-262 — experience percent (200) multiplies in apply order. */
class AuraEngineModXpPctTest {
    @Test
    void xpPercentShouldMultiplyEachAuraInOrder() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_XP_PCT));
        Player player = mage();
        SpellEngine.SpellInfo first = xp(999200, 100);
        SpellEngine.SpellInfo second = xp(999201, 100);
        eng.auras().apply(player, first);
        eng.auras().apply(player, second);
        assertEquals(40, player.xpModifiedByAuras(10));
        player.giveXp(10, null);
        assertEquals(40, player.xp);
        eng.auras().unapply(player, second);
        eng.auras().unapply(player, first);
        assertEquals(10, player.xpModifiedByAuras(10));
    }

    @Test
    void xpPercentShouldApplyTheSecondAuraToTheAlreadyModifiedValue() {
        SpellEngine eng = new SpellEngine();
        Player player = mage();
        eng.auras().apply(player, xp(999202, 100));
        eng.auras().apply(player, xp(999203, 50));
        assertEquals(30, player.xpModifiedByAuras(10));
    }

    private static Player mage() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Player player = new Player();
        player.race = 1;
        player.clazz = 8;
        player.level = 1;
        player.initStatsForLevel(mgr.levelStats);
        return player;
    }

    private static SpellEngine.SpellInfo xp(int id, int amount) {
        return new SpellEngine.SpellInfo(id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_XP_PCT, 0, 0, amount, amount, 0f);
    }
}
