package org.tbc.world.combat;

import org.junit.jupiter.api.Test;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CMaNGOS Formulas.h MaNGOS::XP — TP-SL06-011 kill XP math. */
class XpFormulasTest {

    @Test
    void gainWhenEqualLevelOnAzerothShouldBeBaseGain() {
        assertEquals(50, XpFormulas.gain(player(1, 0), creature(1, 0)));
        assertEquals(45 + 20 * 5, XpFormulas.gain(player(20, 1), creature(20, 0)));
    }

    @Test
    void gainWhenMobHigherShouldAddFivePercentPerLevelCappedAtFour() {
        assertEquals(Math.round(50 * 1.10f), XpFormulas.gain(player(1, 0), creature(3, 0)));
        assertEquals(Math.round(50 * 1.20f), XpFormulas.gain(player(1, 0), creature(9, 0)));
    }

    @Test
    void gainWhenMobLowerShouldScaleByZeroDifferenceAndGreyGivesNothing() {
        // level 10: ZD 7; mob 8 → (1 - 2/7) × 95.
        assertEquals(Math.round(95 * (1f - 2f / 7f)), XpFormulas.gain(player(10, 0), creature(8, 0)));
        assertEquals(0, XpFormulas.gain(player(10, 0), creature(4, 0)));
        assertEquals(0, XpFormulas.gain(player(30, 0), creature(22, 0)));
    }

    @Test
    void gainWhenOutlandShouldUseContent61To70Bonus() {
        assertEquals(60 * 5 + 235, XpFormulas.gain(player(60, 530), creature(60, 0)));
    }

    @Test
    void gainWhenEliteShouldBeTwoAndAHalfTimesButRareIsNormal() {
        assertEquals(Math.round(50 * 2.5f), XpFormulas.gain(player(1, 0), creature(1, 1)));
        assertEquals(50, XpFormulas.gain(player(1, 0), creature(1, 4)));
    }

    @Test
    void gainWhenPetOrTotemShouldBeZero() {
        Creature pet = creature(1, 0);
        pet.pet = true;
        assertEquals(0, XpFormulas.gain(player(1, 0), pet));
        Creature totem = creature(1, 0);
        totem.totem = true;
        assertEquals(0, XpFormulas.gain(player(1, 0), totem));
    }

    @Test
    void trivialLevelDifferenceShouldFollowFiveLevelBrackets() {
        assertFalse(XpFormulas.isTrivialLevelDifference(5, 1));
        assertTrue(XpFormulas.isTrivialLevelDifference(6, 1));
        assertTrue(XpFormulas.isTrivialLevelDifference(15, 9));
        assertFalse(XpFormulas.isTrivialLevelDifference(15, 10));
        assertTrue(XpFormulas.isTrivialLevelDifference(25, 18));
        assertTrue(XpFormulas.isTrivialLevelDifference(35, 27));
        assertFalse(XpFormulas.isTrivialLevelDifference(45, 37));
        assertTrue(XpFormulas.isTrivialLevelDifference(45, 36));
    }

    @Test
    void zeroDifferenceShouldMatchLevelTable() {
        int[][] rows = {{1, 5}, {8, 6}, {10, 7}, {12, 8}, {16, 9}, {20, 11}, {30, 12}, {40, 13}, {45, 14},
            {50, 15}, {55, 16}, {60, 17}};
        for (int[] r : rows) {
            assertEquals(r[1], XpFormulas.zeroDifference(r[0]), "level " + r[0]);
        }
    }

    private static Player player(int level, int mapId) {
        Player p = new Player();
        p.level = level;
        p.mapId = mapId;
        return p;
    }

    private static Creature creature(int level, int rank) {
        Creature c = new Creature();
        c.level = level;
        c.rank = rank;
        return c;
    }
}
