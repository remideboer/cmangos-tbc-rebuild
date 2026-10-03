package org.tbc.world.classless;

import org.junit.jupiter.api.Test;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Player;
import org.tbc.world.spell.SpellEngine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroClassUnlockTest {
    @Test
    void forQuestWhenWarriorUnlockShouldReturnWarriorPilot() {
        assertSame(HeroClassUnlock.WARRIOR, HeroClassUnlock.forQuest(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON));
        assertNull(HeroClassUnlock.forQuest(783));
        assertTrue(HeroClassUnlock.isHeroOnly(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON));
        assertTrue(HeroClassUnlock.isHeroOnly(HeroClassUnlock.QUEST_RALLY_THE_LINE));
        assertFalse(HeroClassUnlock.isHeroOnly(783));
        assertFalse(HeroClassUnlock.spellUnlocked(new Player(), Content.SPELL_BATTLE_SHOUT));
        Player unlocked = new Player();
        unlocked.rewardedQuests.add(HeroClassUnlock.QUEST_RALLY_THE_LINE);
        assertTrue(HeroClassUnlock.spellUnlocked(unlocked, Content.SPELL_BATTLE_SHOUT));
        assertTrue(HeroClassUnlock.spellUnlocked(unlocked, 99999));
        assertEquals(Player.CLASS_WARRIOR, HeroClassUnlock.WARRIOR.classId());
        assertEquals(SpellEngine.HEROIC_STRIKE, HeroClassUnlock.WARRIOR.starterSpell());
        assertEquals(15274, HeroClassUnlock.WARRIOR.targetCreature());
        assertEquals(5, HeroClassUnlock.WARRIOR.requiredHits());
        assertEquals(1, HeroClassUnlock.WARRIOR.requiredKills());
        assertEquals(HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER, HeroClassUnlock.WARRIOR.trainerEntry());
    }

    @Test
    void trainerClassUnlockedWhenMageShouldStayOpenWithoutQuest() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        assertTrue(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_MAGE));
        assertTrue(HeroClassUnlock.trainerClassUnlocked(null, Player.CLASS_MAGE));
    }

    @Test
    void trainerClassUnlockedWhenWarriorShouldRequireRewardedQuest() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(null, Player.CLASS_WARRIOR));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        assertTrue(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
    }
}
