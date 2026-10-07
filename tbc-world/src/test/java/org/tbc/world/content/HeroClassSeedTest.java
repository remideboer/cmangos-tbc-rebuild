package org.tbc.world.content;

import org.junit.jupiter.api.Test;
import org.tbc.world.classless.HeroClassUnlock;
import org.tbc.world.classless.HeroStarterTrainers;
import org.tbc.world.entity.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hero starter paths seeded outside ObjectMgr (refactoring plan cycle 4.1). */
class HeroClassSeedTest {
    @Test
    void seedShouldInstallEveryClassUnlockQuestAndTrainer() {
        ObjectMgr m = new ObjectMgr();
        HeroClassSeed.seed(m);
        assertTrue(m.quests.containsKey(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON));
        assertTrue(m.quests.containsKey(HeroClassUnlock.QUEST_A_VOW_TESTED));
        for (int cls : new int[]{Player.CLASS_WARRIOR, Player.CLASS_PALADIN, Player.CLASS_HUNTER, Player.CLASS_ROGUE,
                Player.CLASS_PRIEST, Player.CLASS_SHAMAN, Player.CLASS_MAGE, Player.CLASS_WARLOCK, Player.CLASS_DRUID}) {
            for (HeroStarterTrainers.Placement p : HeroStarterTrainers.forClass(cls)) {
                assertTrue(m.creatures.containsKey(p.entry()), "trainer template " + p.entry());
                assertEquals(cls, m.trainerClass.get(p.entry()), "trainer class " + p.entry());
                assertTrue(m.questGivers.containsKey(p.entry()), "quest giver " + p.entry());
            }
        }
    }

    @Test
    void seedTwiceShouldBeIdempotent() {
        ObjectMgr m = new ObjectMgr();
        HeroClassSeed.seed(m);
        int quests = m.quests.size();
        int creatures = m.creatures.size();
        int spawns = m.spawns.size();
        HeroClassSeed.seed(m);
        assertEquals(quests, m.quests.size());
        assertEquals(creatures, m.creatures.size());
        assertEquals(spawns, m.spawns.size());
    }

    @Test
    void loadInMemoryShouldStillSeedTheHeroPaths() {
        ObjectMgr m = new ObjectMgr();
        m.load(null, null);
        assertTrue(m.quests.containsKey(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON));
        assertTrue(m.creatures.containsKey(HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER));
    }
}
