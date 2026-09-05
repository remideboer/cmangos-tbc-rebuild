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
}
