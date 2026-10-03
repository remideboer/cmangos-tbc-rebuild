package org.tbc.world.classless;

import org.tbc.world.content.LevelStats;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroStatAllocationTest {
    @AfterEach
    void reset() {
        ClasslessConfig.reset();
    }

    /** TP-SL35-026 — human 1→2 budget is the five-stat race-average delta (seeded L2). */
    @Test
    void pointsForGainWhenHumanOneToTwoShouldSumAverageDeltas() {
        LevelStats ls = LevelStats.defaults();
        int pts = HeroStatAllocation.pointsForGain(ls, 1, 1, 2);
        assertEquals(4, pts);
        assertEquals(0, HeroStatAllocation.pointsForGain(ls, 1, 1, 1));
        assertEquals(0, HeroStatAllocation.pointsForGain(null, 1, 1, 2));
    }

    /** TP-SL35-026 — ding keeps L1 STAT0-4 and banks the average boost as unspent. */
    @Test
    void giveXpWhenClasslessDingsShouldKeepL1StatsAndAwardUnspent() {
        Player p = new Player();
        p.race = 1;
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.level = 1;
        ClasslessCharacterPolicy.applyStartingStats(p);
        int str = p.getInt(UpdateFields.UNIT_FIELD_STAT0);
        int agi = p.getInt(UpdateFields.UNIT_FIELD_STAT1);
        int sta = p.getInt(UpdateFields.UNIT_FIELD_STAT2);
        int inte = p.getInt(UpdateFields.UNIT_FIELD_STAT3);
        int spi = p.getInt(UpdateFields.UNIT_FIELD_STAT4);
        p.giveXp(400, null);
        assertEquals(2, p.level);
        assertEquals(str, p.getInt(UpdateFields.UNIT_FIELD_STAT0));
        assertEquals(agi, p.getInt(UpdateFields.UNIT_FIELD_STAT1));
        assertEquals(sta, p.getInt(UpdateFields.UNIT_FIELD_STAT2));
        assertEquals(inte, p.getInt(UpdateFields.UNIT_FIELD_STAT3));
        assertEquals(spi, p.getInt(UpdateFields.UNIT_FIELD_STAT4));
        assertEquals(HeroStatAllocation.pointsForGain(LevelStats.defaults(), 1, 1, 2), p.heroStats.unspent());
    }

    @Test
    void giveXpWhenWarriorDingsShouldStillRaiseClassStats() {
        Player p = new Player();
        p.race = 1;
        p.clazz = 1;
        p.level = 1;
        p.initStatsForLevel(LevelStats.defaults());
        int str = p.getInt(UpdateFields.UNIT_FIELD_STAT0);
        p.giveXp(400, null);
        assertEquals(2, p.level);
        assertTrue(p.getInt(UpdateFields.UNIT_FIELD_STAT0) > str);
        assertEquals(0, p.heroStats.unspent());
    }

    /** TP-SL35-027 — spend reduces unspent and raises the chosen create stat. */
    @Test
    void spendWhenUnspentShouldRaiseStatAndKeepOtherStats() {
        Player p = heroAtLevel2WithUnspent();
        int str = p.getInt(UpdateFields.UNIT_FIELD_STAT0);
        int agi = p.getInt(UpdateFields.UNIT_FIELD_STAT1);
        int unspent = p.heroStats.unspent();
        assertTrue(p.spendHeroStat(HeroStatAllocation.STR, 1));
        assertEquals(unspent - 1, p.heroStats.unspent());
        assertEquals(str + 1, p.getInt(UpdateFields.UNIT_FIELD_STAT0));
        assertEquals(agi, p.getInt(UpdateFields.UNIT_FIELD_STAT1));
    }

    @Test
    void spendWhenInvalidShouldRefuse() {
        Player p = heroAtLevel2WithUnspent();
        int str = p.getInt(UpdateFields.UNIT_FIELD_STAT0);
        int unspent = p.heroStats.unspent();
        assertFalse(p.spendHeroStat(-1, 1));
        assertFalse(p.spendHeroStat(5, 1));
        assertFalse(p.spendHeroStat(HeroStatAllocation.STR, 0));
        assertFalse(p.spendHeroStat(HeroStatAllocation.STR, unspent + 1));
        Player warrior = new Player();
        warrior.clazz = 1;
        warrior.initStatsForLevel(LevelStats.defaults());
        assertFalse(warrior.spendHeroStat(HeroStatAllocation.STR, 1));
        assertEquals(unspent, p.heroStats.unspent());
        assertEquals(str, p.getInt(UpdateFields.UNIT_FIELD_STAT0));
    }

    @Test
    void spendWhenDamagedShouldPreserveHealthAndManaPercent() {
        Player p = heroAtLevel2WithUnspent();
        int maxHp = p.maxHealth();
        int maxMana = p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1);
        int oldHp = maxHp / 2;
        int oldMana = maxMana / 2;
        p.setHealth(oldHp);
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, oldMana);
        assertTrue(p.spendHeroStat(HeroStatAllocation.STA, 1));
        int newMaxHp = p.maxHealth();
        int newMaxMana = p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1);
        assertEquals(Math.max(1, Math.round(oldHp * (float) newMaxHp / maxHp)), p.health());
        assertEquals(Math.min(newMaxMana, Math.round(oldMana * (float) newMaxMana / maxMana)),
                p.getInt(UpdateFields.UNIT_FIELD_POWER1));
    }

    private static Player heroAtLevel2WithUnspent() {
        Player p = new Player();
        p.race = 1;
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.level = 1;
        ClasslessCharacterPolicy.applyStartingStats(p);
        p.giveXp(400, null);
        assertTrue(p.heroStats.unspent() > 0);
        return p;
    }
}
