package org.tbc.world.classless;

import org.junit.jupiter.api.Test;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroStarterTrainersTest {
    @Test
    void forClassWhenPaladinShouldUseNativeJesthenisOnSunstrider() {
        boolean jesthenis = false;
        for (HeroStarterTrainers.Placement p : HeroStarterTrainers.forClass(Player.CLASS_PALADIN)) {
            if (p.entry() == 15280 && p.map() == 530) {
                jesthenis = true;
                assertFalse(p.custom());
                assertEquals("Jesthenis Sunstriker", p.name());
            }
            assertTrue(p.entry() != 91002);
        }
        assertTrue(jesthenis);
    }

    @Test
    void forClassWhenSunstriderShamanShouldUseTaurenMeelaDisplay() {
        HeroStarterTrainers.Placement sun = null;
        for (HeroStarterTrainers.Placement p : HeroStarterTrainers.forClass(Player.CLASS_SHAMAN)) {
            if (p.entry() == HeroClassUnlock.NPC_HERO_SHAMAN_TRAINER) {
                sun = p;
            }
        }
        assertTrue(sun != null);
        assertTrue(sun.custom());
        assertEquals(HeroClassUnlock.NAME_HUURUN_STONESONG, sun.name());
        assertEquals(HeroStarterTrainers.DISPLAY_MEELA, sun.display());
    }

    @Test
    void forClassWhenSunstriderDruidShouldUseTaurenGenniaDisplay() {
        HeroStarterTrainers.Placement sun = null;
        for (HeroStarterTrainers.Placement p : HeroStarterTrainers.forClass(Player.CLASS_DRUID)) {
            if (p.entry() == HeroClassUnlock.NPC_HERO_DRUID_TRAINER) {
                sun = p;
            }
        }
        assertTrue(sun != null);
        assertEquals(HeroClassUnlock.NAME_MESA_WILDHOOF, sun.name());
        assertEquals(HeroStarterTrainers.DISPLAY_GENNIA, sun.display());
    }

    @Test
    void allWhenStarterHubsShouldCoverNineClassesWithoutDuplicateCustomEntries() {
        int[] classes = {
                Player.CLASS_WARRIOR, Player.CLASS_PALADIN, Player.CLASS_HUNTER, Player.CLASS_ROGUE,
                Player.CLASS_PRIEST, Player.CLASS_SHAMAN, Player.CLASS_MAGE, Player.CLASS_WARLOCK,
                Player.CLASS_DRUID
        };
        for (int clazz : classes) {
            assertTrue(HeroStarterTrainers.forClass(clazz).size() >= 8, "hubs for " + clazz);
        }
        Set<Integer> custom = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (HeroStarterTrainers.Placement p : HeroStarterTrainers.all()) {
            if (p.custom()) {
                assertTrue(custom.add(p.entry()), "dup custom " + p.entry());
                assertTrue(names.add(p.name()), "dup name " + p.name());
            }
        }
    }

    @Test
    void seedQueryDefaultsWhenNorthshireShouldOfferPaladinUnlockOnBrotherSammuel() {
        ObjectMgr mgr = new ObjectMgr();
        mgr.load(null, null);
        assertTrue(mgr.questGivers.get(925).contains(HeroClassUnlock.QUEST_A_VOW_TESTED));
        assertTrue(mgr.questGivers.get(91010).contains(HeroClassUnlock.QUEST_THE_MARKED_TRAIL));
    }
}
