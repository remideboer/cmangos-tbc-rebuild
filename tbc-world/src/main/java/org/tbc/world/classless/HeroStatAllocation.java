package org.tbc.world.classless;

import org.tbc.world.content.LevelStats;

/**
 * Hero unspent/spent ability points. Budget is the sum of race-average stat
 * increases between levels; level-1 starting stats are not part of the pool.
 */
public final class HeroStatAllocation {
    public static final int STR = 0;
    public static final int AGI = 1;
    public static final int STA = 2;
    public static final int INTELLECT = 3;
    public static final int SPI = 4;

    private int unspent;
    private final int[] spent = new int[5];

    public int unspent() {
        return unspent;
    }

    public int spent(int stat) {
        if (stat < 0 || stat > SPI) {
            return 0;
        }
        return spent[stat];
    }

    public static int pointsForGain(LevelStats ls, int race, int fromLevel, int toLevel) {
        if (ls == null || toLevel <= fromLevel) {
            return 0;
        }
        int pts = 0;
        for (int lvl = fromLevel; lvl < toLevel; lvl++) {
            LevelStats.Stats a = ls.averageStats(race, lvl);
            LevelStats.Stats b = ls.averageStats(race, lvl + 1);
            for (int i = 0; i < 5; i++) {
                pts += Math.max(0, b.stat(i) - a.stat(i));
            }
        }
        return pts;
    }

    public void awardGain(LevelStats ls, int race, int fromLevel, int toLevel) {
        unspent += pointsForGain(ls, race, fromLevel, toLevel);
    }

    /**
     * Missing persist row: give the Hero the unspent budget they would have earned
     * reaching {@code level}, with nothing spent yet.
     */
    public void backfillUnspent(LevelStats ls, int race, int level) {
        unspent = 0;
        for (int i = 0; i < 5; i++) {
            spent[i] = 0;
        }
        if (level > 1) {
            awardGain(ls, race, 1, level);
        }
    }

    public boolean spend(int stat, int amount) {
        if (stat < STR || stat > SPI || amount < 1 || amount > unspent) {
            return false;
        }
        unspent -= amount;
        spent[stat] += amount;
        return true;
    }

    public void copyFrom(HeroStatAllocation src) {
        unspent = src.unspent;
        System.arraycopy(src.spent, 0, spent, 0, 5);
    }

    public void load(int unspentPts, int str, int agi, int sta, int inte, int spi) {
        this.unspent = Math.max(0, unspentPts);
        spent[STR] = Math.max(0, str);
        spent[AGI] = Math.max(0, agi);
        spent[STA] = Math.max(0, sta);
        spent[INTELLECT] = Math.max(0, inte);
        spent[SPI] = Math.max(0, spi);
    }
}
