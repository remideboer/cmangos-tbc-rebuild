package org.tbc.world.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactionTemplateTest {
    @Test
    void isHostileToWhenEnemyGroupMatchesPlayerShouldBeTrue() {
        FactionTemplate monster = Factions.seeded().get(7);
        FactionTemplate human = Factions.seeded().get(1);
        assertTrue(monster.isHostileTo(human));
        assertTrue(monster.isHostileToPlayers());
    }

    @Test
    void isHostileToWhenAllianceNpcVsPlayerShouldBeFalse() {
        FactionTemplate stormwind = Factions.seeded().get(12);
        FactionTemplate human = Factions.seeded().get(1);
        assertFalse(stormwind.isHostileTo(human));
        assertFalse(stormwind.isHostileToPlayers());
    }

    @Test
    void isHostileToWhenGnomePlayerVsMonsterShouldBeTrue() {
        FactionTemplate monster = Factions.seeded().get(7);
        FactionTemplate gnome = Factions.seeded().get(115);
        assertTrue(monster.isHostileTo(gnome));
    }

    @Test
    void isHostileToWhenDbcRaggedWolf38VsGnomeShouldBeTrue() {
        Factions f = Factions.seeded();
        assertTrue(f.get(38).isHostileTo(f.get(115)));
        assertTrue(f.get(115).isHostileTo(f.get(38)));
        assertTrue(f.get(38).isHostileToPlayers());
    }

    @Test
    void isHostileToWhenDbcTimberWolf32VsGnomeShouldBeFalseBothWays() {
        Factions f = Factions.seeded();
        assertFalse(f.get(32).isHostileTo(f.get(115)));
        assertFalse(f.get(115).isHostileTo(f.get(32)));
        assertFalse(f.get(32).isHostileToPlayers());
    }

    @Test
    void isHostileToWhenDbcKobold25VsGnomeShouldBePlayerHatesMonsterOnly() {
        Factions f = Factions.seeded();
        assertFalse(f.get(25).isHostileTo(f.get(115)));
        assertTrue(f.get(115).isHostileTo(f.get(25)));
        assertFalse(f.get(25).isHostileToPlayers());
    }

    @Test
    void reactionToWhenGnomeViewsRaggedWolf38ShouldBeHostileRedBar() {
        Factions f = Factions.seeded();
        assertEquals(FactionTemplate.REP_HOSTILE, f.get(115).reactionTo(f.get(38)));
        assertEquals(FactionTemplate.REP_HOSTILE, f.get(38).reactionTo(f.get(115)));
    }

    @Test
    void reactionToWhenGnomeViewsKobold25ShouldBeHostileRedBar() {
        Factions f = Factions.seeded();
        assertEquals(FactionTemplate.REP_HOSTILE, f.get(115).reactionTo(f.get(25)));
        assertEquals(FactionTemplate.REP_NEUTRAL, f.get(25).reactionTo(f.get(115)));
    }

    @Test
    void reactionToWhenGnomeViewsTimberWolf32ShouldBeNeutralYellowBar() {
        Factions f = Factions.seeded();
        assertEquals(FactionTemplate.REP_NEUTRAL, f.get(115).reactionTo(f.get(32)));
        assertEquals(FactionTemplate.REP_NEUTRAL, f.get(32).reactionTo(f.get(115)));
    }

    @Test
    void reactionToWhenGnomeViewsStormwindNpcShouldBeFriendlyGreenBar() {
        Factions f = Factions.seeded();
        assertEquals(FactionTemplate.REP_FRIENDLY, f.get(115).reactionTo(f.get(12)));
        assertEquals(FactionTemplate.REP_FRIENDLY, f.get(12).reactionTo(f.get(115)));
    }

    @Test
    void isNeutralToAllWhenKobold25ShouldBeTrue() {
        assertTrue(Factions.seeded().get(25).isNeutralToAll());
        assertFalse(Factions.seeded().get(7).isNeutralToAll());
        assertFalse(Factions.seeded().get(38).isNeutralToAll());
    }

    @Test
    void constructWhenIdentityGroupsAndListsGivenShouldExposeDbcFields() {
        FactionTemplate t = new FactionTemplate(
                new FactionTemplate.Identity(38, 29, 17),
                new FactionTemplate.GroupMasks(FactionTemplate.GROUP_MONSTER, 0, FactionTemplate.GROUP_PLAYER),
                new FactionTemplate.FactionLists(new int[]{28, 0, 0, 0}, new int[]{29, 0, 0, 0}));
        assertEquals(38, t.id);
        assertEquals(29, t.faction);
        assertEquals(17, t.factionFlags);
        assertEquals(FactionTemplate.GROUP_MONSTER, t.factionGroupMask);
        assertEquals(0, t.friendGroupMask);
        assertEquals(FactionTemplate.GROUP_PLAYER, t.enemyGroupMask);
        assertEquals(28, t.enemyFaction[0]);
        assertEquals(29, t.friendFaction[0]);
        assertTrue(t.isHostileToPlayers());
    }

    @Test
    void factionListsWhenNullArraysShouldBecomeEmptySlots() {
        FactionTemplate.FactionLists lists = new FactionTemplate.FactionLists(null, null);
        assertEquals(4, lists.enemies().length);
        assertEquals(4, lists.friends().length);
        assertEquals(0, lists.enemies()[0]);
        assertEquals(0, lists.friends()[0]);
    }
}
