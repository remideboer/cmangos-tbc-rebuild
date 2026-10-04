package org.tbc.world.content;

import org.junit.jupiter.api.Test;
import org.tbc.world.classless.HeroClassUnlock;
import org.tbc.world.content.Content;
import org.tbc.world.combat.FactionTemplate;
import org.tbc.world.combat.Factions;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.script.ScriptRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hero Warrior Trainer 91001: Jesthenis model, Silvermoon faction, Llane virtual gear, Sarrandor spawn. */
class ObjectMgrHeroWarriorTrainerTest {
    @Test
    void seedWhenHeroWarriorTrainerShouldUseJesthenisModelAndSilvermoonFaction() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        ObjectMgr.CreatureTemplate t = mgr.creatures.get(HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER);
        assertNotNull(t);
        assertEquals(HeroClassUnlock.NAME_LORVAEN_BLOODFEATHER, t.name());
        assertEquals(HeroClassUnlock.DISPLAY_JESTHENIS, t.display());
        assertEquals(HeroClassUnlock.FACTION_SILVERMOON, t.faction());
        assertEquals("Warrior Trainer", t.subName());
        assertEquals(HeroClassUnlock.DISPLAY_JESTHENIS, t.display(), "overwrite must beat a prior empty display");
    }

    @Test
    void spawnWhenHeroWarriorTrainerShouldWearLlaneVirtualSwordAndShield() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        Creature c = mgr.spawnCreature(HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER, 530, 0, 0, 0, 0,
                new ScriptRegistry());
        assertEquals(HeroClassUnlock.DISPLAY_JESTHENIS, c.getInt(UpdateFields.UNIT_FIELD_DISPLAYID));
        assertEquals(HeroClassUnlock.FACTION_SILVERMOON, c.getInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE));
        assertEquals(HeroClassUnlock.VIRTUAL_ITEM_SWORD_DISPLAY,
                c.getInt(UpdateFields.UNIT_VIRTUAL_ITEM_SLOT_DISPLAY));
        assertEquals(HeroClassUnlock.VIRTUAL_ITEM_SHIELD_DISPLAY,
                c.getInt(UpdateFields.UNIT_VIRTUAL_ITEM_SLOT_DISPLAY + 1));
    }

    @Test
    void seedWhenSunstriderSpawnShouldUseSarrandorEditorCoords() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        ObjectMgr.Spawn sun = null;
        for (ObjectMgr.Spawn s : mgr.spawns) {
            if (s.entry() == HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER && s.map() == 530) {
                sun = s;
                break;
            }
        }
        assertNotNull(sun);
        assertEquals(HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER, sun.entry());
        assertEquals(530, sun.map());
        assertEquals(HeroClassUnlock.SUNSTRIDER_SPAWN_X, sun.x(), 0.01f);
        assertEquals(HeroClassUnlock.SUNSTRIDER_SPAWN_Y, sun.y(), 0.01f);
        assertEquals(HeroClassUnlock.SUNSTRIDER_SPAWN_Z, sun.z(), 0.01f);
        assertEquals(HeroClassUnlock.SUNSTRIDER_SPAWN_O, sun.o(), 0.01f);
    }

    @Test
    void seedQueryDefaultsWhenSqlWorldShouldPlaceEveryHeroTrainerOnSunstriderWithUnlockQuest() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.seedQueryDefaults();
        int[] trainers = {
                HeroClassUnlock.NPC_HERO_WARRIOR_TRAINER,
                HeroClassUnlock.NPC_HERO_PALADIN_TRAINER,
                HeroClassUnlock.NPC_HERO_HUNTER_TRAINER,
                HeroClassUnlock.NPC_HERO_ROGUE_TRAINER,
                HeroClassUnlock.NPC_HERO_PRIEST_TRAINER,
                HeroClassUnlock.NPC_HERO_MAGE_TRAINER,
                HeroClassUnlock.NPC_HERO_WARLOCK_TRAINER,
                HeroClassUnlock.NPC_HERO_SHAMAN_TRAINER,
                HeroClassUnlock.NPC_HERO_DRUID_TRAINER
        };
        int[] unlocks = {
                HeroClassUnlock.QUEST_HEROS_FIRST_LESSON,
                HeroClassUnlock.QUEST_A_VOW_TESTED,
                HeroClassUnlock.QUEST_THE_MARKED_TRAIL,
                HeroClassUnlock.QUEST_A_QUIET_HAND,
                HeroClassUnlock.QUEST_MERCY_AND_JUDGMENT,
                HeroClassUnlock.QUEST_A_CONTROLLED_SPARK,
                HeroClassUnlock.QUEST_THE_BOUND_FLAME,
                HeroClassUnlock.QUEST_LISTEN_TO_THE_ELEMENTS,
                HeroClassUnlock.QUEST_A_LIVING_BALANCE
        };
        for (int i = 0; i < trainers.length; i++) {
            int entry = trainers[i];
            ObjectMgr.CreatureTemplate t = mgr.creatures.get(entry);
            assertNotNull(t, "missing template " + entry);
            assertTrue((t.npcFlags() & Content.UNIT_NPC_FLAG_QUESTGIVER) != 0, "questgiver " + entry);
            ObjectMgr.Spawn sun = null;
            for (ObjectMgr.Spawn s : mgr.spawns) {
                if (s.entry() == entry && s.map() == 530) {
                    sun = s;
                    break;
                }
            }
            assertNotNull(sun, "Sunstrider spawn " + entry);
            java.util.List<Integer> offered = mgr.questGivers.get(entry);
            assertNotNull(offered, "quest relation " + entry);
            assertTrue(offered.contains(unlocks[i]), "unlock quest on " + entry);
        }
    }

    @Test
    void factionsWhenSilvermoonNpcShouldBeFriendlyToBloodElf() {
        Factions factions = Factions.seeded();
        assertNotNull(factions.get(HeroClassUnlock.FACTION_SILVERMOON));
        Player be = new Player();
        be.faction = 1610;
        be.setInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE, 1610);
        Creature trainer = new Creature();
        trainer.faction = HeroClassUnlock.FACTION_SILVERMOON;
        trainer.setInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE, HeroClassUnlock.FACTION_SILVERMOON);
        assertTrue(factions.reaction(be, trainer) >= FactionTemplate.REP_FRIENDLY);
        assertTrue(factions.isFriend(be, trainer));
    }
}
