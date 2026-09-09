package org.tbc.world.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestXpTest {
    @Test
    void xpValueWhenRewMoneyMaxLevelMissingShouldBeZero() {
        assertEquals(0, QuestXp.xpValue(1, 1, 0));
        assertEquals(0, QuestXp.xpValue(1, 0, 24));
    }

    @Test
    void xpValueWhenLevelOneNorthshireShouldUsePointSixDivisor() {
        assertEquals(40, QuestXp.xpValue(1, 1, 24));
        assertEquals(40, QuestXp.xpValue(6, 1, 24));
    }

    @Test
    void xpValueWhenPlayerOutlevelsQuestShouldGrey() {
        assertEquals(32, QuestXp.xpValue(7, 1, 24));
        assertEquals(24, QuestXp.xpValue(8, 1, 24));
        assertEquals(16, QuestXp.xpValue(9, 1, 24));
        assertEquals(8, QuestXp.xpValue(10, 1, 24));
        assertEquals(4, QuestXp.xpValue(11, 1, 24));
    }

    @Test
    void xpValueWhenOutlandQuestLevelShouldUseHighDivisors() {
        assertEquals(50, QuestXp.xpValue(61, 61, 60));
        assertEquals(25, QuestXp.xpValue(62, 62, 60));
        assertEquals(17, QuestXp.xpValue(63, 63, 60));
        assertEquals(13, QuestXp.xpValue(64, 64, 60));
        assertEquals(10, QuestXp.xpValue(65, 65, 60));
        assertEquals(10, QuestXp.xpValue(70, 70, 60));
    }
}
