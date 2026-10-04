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
    void trainerClassUnlockedWhenWarriorShouldRequireRewardedQuest() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(null, Player.CLASS_WARRIOR));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_HEROS_FIRST_LESSON);
        assertTrue(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
    }

    @Test
    void trainerClassUnlockedWhenPaladinShouldRequireVowTested() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_PALADIN));
        assertSame(HeroClassUnlock.PALADIN, HeroClassUnlock.forQuest(HeroClassUnlock.QUEST_A_VOW_TESTED));
        assertTrue(HeroClassUnlock.isHeroOnly(HeroClassUnlock.QUEST_STAND_FAST));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_A_VOW_TESTED);
        assertTrue(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_PALADIN));
        assertFalse(HeroClassUnlock.spellUnlocked(p, SpellEngine.DEVOTION_AURA));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_STAND_FAST);
        assertTrue(HeroClassUnlock.spellUnlocked(p, SpellEngine.DEVOTION_AURA));
    }

    @Test
    void trainerClassUnlockedWhenHunterShouldRequireMarkedTrail() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_HUNTER));
        assertSame(HeroClassUnlock.HUNTER, HeroClassUnlock.forQuest(HeroClassUnlock.QUEST_THE_MARKED_TRAIL));
        assertTrue(HeroClassUnlock.isHeroOnly(HeroClassUnlock.QUEST_STEADY_AIM));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_THE_MARKED_TRAIL);
        assertTrue(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_HUNTER));
        assertFalse(HeroClassUnlock.spellUnlocked(p, HeroClassUnlock.SPELL_AUTO_SHOT));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_STEADY_AIM);
        assertTrue(HeroClassUnlock.spellUnlocked(p, HeroClassUnlock.SPELL_AUTO_SHOT));
        // Prior class paths stay gated independently.
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_PALADIN));
    }

    @Test
    void trainerClassUnlockedWhenRogueShouldRequireQuietHand() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_ROGUE));
        assertSame(HeroClassUnlock.ROGUE, HeroClassUnlock.forQuest(HeroClassUnlock.QUEST_A_QUIET_HAND));
        assertTrue(HeroClassUnlock.isHeroOnly(HeroClassUnlock.QUEST_DISAPPEAR_FROM_SIGHT));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_A_QUIET_HAND);
        assertTrue(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_ROGUE));
        assertFalse(HeroClassUnlock.spellUnlocked(p, SpellEngine.SPELL_STEALTH));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_DISAPPEAR_FROM_SIGHT);
        assertTrue(HeroClassUnlock.spellUnlocked(p, SpellEngine.SPELL_STEALTH));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_PALADIN));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_HUNTER));
    }

    @Test
    void trainerClassUnlockedWhenPriestShouldRequireMercyAndJudgment() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_PRIEST));
        assertSame(HeroClassUnlock.PRIEST, HeroClassUnlock.forQuest(HeroClassUnlock.QUEST_MERCY_AND_JUDGMENT));
        assertTrue(HeroClassUnlock.isHeroOnly(HeroClassUnlock.QUEST_JUDGMENT_FROM_AFAR));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_MERCY_AND_JUDGMENT);
        assertTrue(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_PRIEST));
        assertFalse(HeroClassUnlock.spellUnlocked(p, HeroClassUnlock.SPELL_SMITE));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_JUDGMENT_FROM_AFAR);
        assertTrue(HeroClassUnlock.spellUnlocked(p, HeroClassUnlock.SPELL_SMITE));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_ROGUE));
    }

    @Test
    void trainerClassUnlockedWhenMageShouldRequireControlledSpark() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_MAGE));
        assertSame(HeroClassUnlock.MAGE, HeroClassUnlock.forQuest(HeroClassUnlock.QUEST_A_CONTROLLED_SPARK));
        assertTrue(HeroClassUnlock.isHeroOnly(HeroClassUnlock.QUEST_A_COOLER_HEAD));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_A_CONTROLLED_SPARK);
        assertTrue(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_MAGE));
        assertFalse(HeroClassUnlock.spellUnlocked(p, SpellEngine.FROST_ARMOR));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_A_COOLER_HEAD);
        assertTrue(HeroClassUnlock.spellUnlocked(p, SpellEngine.FROST_ARMOR));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_PRIEST));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
    }

    @Test
    void trainerClassUnlockedWhenWarlockShouldRequireBoundFlame() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARLOCK));
        assertSame(HeroClassUnlock.WARLOCK, HeroClassUnlock.forQuest(HeroClassUnlock.QUEST_THE_BOUND_FLAME));
        assertTrue(HeroClassUnlock.isHeroOnly(HeroClassUnlock.QUEST_SHADOW_IN_RESERVE));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_THE_BOUND_FLAME);
        assertTrue(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARLOCK));
        assertFalse(HeroClassUnlock.spellUnlocked(p, HeroClassUnlock.SPELL_SHADOW_BOLT));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_SHADOW_IN_RESERVE);
        assertTrue(HeroClassUnlock.spellUnlocked(p, HeroClassUnlock.SPELL_SHADOW_BOLT));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_MAGE));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
    }

    @Test
    void trainerClassUnlockedWhenShamanShouldRequireListenToTheElements() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_SHAMAN));
        assertSame(HeroClassUnlock.SHAMAN, HeroClassUnlock.forQuest(HeroClassUnlock.QUEST_LISTEN_TO_THE_ELEMENTS));
        assertTrue(HeroClassUnlock.isHeroOnly(HeroClassUnlock.QUEST_MEND_THE_WOUNDED));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_LISTEN_TO_THE_ELEMENTS);
        assertTrue(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_SHAMAN));
        assertFalse(HeroClassUnlock.spellUnlocked(p, HeroClassUnlock.SPELL_HEALING_WAVE));
        p.rewardedQuests.add(HeroClassUnlock.QUEST_MEND_THE_WOUNDED);
        assertTrue(HeroClassUnlock.spellUnlocked(p, HeroClassUnlock.SPELL_HEALING_WAVE));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARLOCK));
        assertFalse(HeroClassUnlock.trainerClassUnlocked(p, Player.CLASS_WARRIOR));
    }
}
