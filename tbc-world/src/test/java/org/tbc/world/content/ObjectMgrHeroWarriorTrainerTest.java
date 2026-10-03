package org.tbc.world.content;

import org.junit.jupiter.api.Test;
import org.tbc.world.classless.HeroClassUnlock;
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
            if (s.guid() == 17) {
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
