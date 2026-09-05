package org.tbc.world.combat;

import org.tbc.world.ai.FactorySelector;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CMaNGOS Relations.cpp CanAttack / CanAttackOnSight (no PvP duel). */
class RelationsTest {
    private Player p;
    private Creature c;
    private final Factions factions = Factions.seeded();

    @BeforeEach
    void setUp() {
        p = new Player();
        p.guid = 1;
        p.level = 1;
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        p.setHealth(50);
        c = new Creature();
        c.guid = 2;
        c.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        FactorySelector.selectAI(c, null);
        setFaction(p, 1);
        setFaction(c, 7);
        p.relocate(2, 0, 0, 0);
        c.relocate(0, 0, 0, 0);
    }

    private static void setFaction(Unit u, int templateId) {
        u.faction = templateId;
        u.setInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE, templateId);
    }

    @Test
    void canAttackWhenTargetSpawningShouldBeFalse() {
        p.setInt(UpdateFields.UNIT_FIELD_FLAGS,
                p.getInt(UpdateFields.UNIT_FIELD_FLAGS) | Unit.UNIT_FLAG_SPAWNING);
        assertFalse(Relations.canAttack(c, p, factions));
    }

    @Test
    void canAttackWhenTargetUntargetableShouldBeFalse() {
        p.setInt(UpdateFields.UNIT_FIELD_FLAGS,
                p.getInt(UpdateFields.UNIT_FIELD_FLAGS) | Unit.UNIT_FLAG_UNTARGETABLE);
        assertFalse(Relations.canAttack(c, p, factions));
    }

    @Test
    void canAttackWhenTargetImmuneToNpcShouldBeFalse() {
        p.setInt(UpdateFields.UNIT_FIELD_FLAGS,
                p.getInt(UpdateFields.UNIT_FIELD_FLAGS) | Unit.UNIT_FLAG_IMMUNE_TO_NPC);
        assertFalse(Relations.canAttack(c, p, factions));
    }

    @Test
    void canAttackWhenCreatureImmuneToPlayerShouldBeFalse() {
        c.setInt(UpdateFields.UNIT_FIELD_FLAGS,
                c.getInt(UpdateFields.UNIT_FIELD_FLAGS) | Unit.UNIT_FLAG_IMMUNE_TO_PLAYER);
        assertFalse(Relations.canAttack(c, p, factions));
    }

    @Test
    void canAttackWhenFriendlyNpcShouldBeFalse() {
        setFaction(c, 12);
        assertFalse(Relations.canAttack(c, p, factions));
    }

    @Test
    void canAttackWhenNeutralTimberWolfShouldBeTrue() {
        setFaction(p, 115);
        setFaction(c, 32);
        assertTrue(Relations.canAttack(c, p, factions));
        assertTrue(Relations.canAttack(p, c, factions));
    }

    @Test
    void canAttackOnSightWhenMonsterShouldBeTrue() {
        assertTrue(Relations.canAttackOnSight(c, p, factions));
    }

    @Test
    void canAttackOnSightWhenKobold25ShouldBeFalse() {
        setFaction(p, 115);
        setFaction(c, 25);
        assertFalse(Relations.canAttackOnSight(c, p, factions));
    }

    @Test
    void canAttackOnSightWhenNeutralWolfShouldBeFalse() {
        setFaction(p, 115);
        setFaction(c, 32);
        assertFalse(Relations.canAttackOnSight(c, p, factions));
    }

    @Test
    void canAttackOnSightWhenTargetEvadingHomeShouldBeFalse() {
        Creature home = new Creature();
        home.guid = 3;
        home.applyTemplate(6, "Other", 1, 7, 42, 1);
        home.evading = true;
        assertFalse(Relations.canAttackOnSight(c, home, factions));
    }

    @Test
    void canInitiateAttackWhenSpawningShouldBeFalse() {
        c.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_SPAWNING);
        assertFalse(Relations.canInitiateAttack(c));
    }

    @Test
    void canAggroOnSightWhenImmuneFlagsShouldBeFalse() {
        p.setInt(UpdateFields.UNIT_FIELD_FLAGS,
                p.getInt(UpdateFields.UNIT_FIELD_FLAGS) | Unit.UNIT_FLAG_IMMUNE_TO_NPC);
        assertFalse(Combat.canAggroOnSight(c, p, factions));
    }

    @Test
    void canAggroOnSightWhenNeutralToAllCritterShouldBeFalse() {
        factions.add(new FactionTemplate(189, 7, 0, 0, 0, 0, new int[4], new int[4]));
        setFaction(c, 189);
        assertTrue(factions.template(c).isNeutralToAll());
        assertFalse(Combat.canAggroOnSight(c, p, factions));
    }

    @Test
    void canAggroOnSightWhenPlayerHostileToNeutralToAllKoboldShouldBeTrue() {
        setFaction(p, 115);
        setFaction(c, 25);
        assertTrue(factions.template(c).isNeutralToAll());
        assertTrue(Combat.canAggroOnSight(c, p, factions));
    }

    @Test
    void canAggroOnSightWhenCreatureSpawningShouldBeFalse() {
        c.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_SPAWNING);
        assertFalse(Combat.canAggroOnSight(c, p, factions));
    }

    @Test
    void canAggroOnSightWhenUnknownFactionTemplateShouldBeFalse() {
        setFaction(c, 9999);
        assertFalse(Combat.canAggroOnSight(c, p, factions));
    }

    @Test
    void canAggroOnSightWhenCreatureHatesPlayerWithoutRedBarShouldBeTrue() {
        factions.add(new FactionTemplate(400, 400, 0, 0, 0, 0, new int[]{1, 0, 0, 0}, new int[4]));
        setFaction(c, 400);
        assertFalse(factions.isHostile(p, c));
        assertTrue(factions.isHostile(c, p));
        assertTrue(Combat.canAggroOnSight(c, p, factions));
    }
}
