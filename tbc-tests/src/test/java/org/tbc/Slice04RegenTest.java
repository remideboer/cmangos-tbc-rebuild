package org.tbc;

import org.junit.jupiter.api.Test;
import org.tbc.bdd.WowClientDouble;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Gap inventory TP-SL04-014 / TP-SL04-015 — CMaNGOS Player::RegenerateAll every REGEN_TIME_FULL (2000 ms).
 * Ratios are gtOCTRegenHP / gtRegenHPPerSpt / gtRegenMPPerSpt level-1 rows; the client sees VALUES.
 */
class Slice04RegenTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86");
    private static final int HUMAN = 1;
    private static final int WARRIOR = 1;
    private static final int MAGE = 8;
    private static final int ROGUE = 4;

    @Test
    void tpSl04OocRegenRaisesHealthAndMana() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, HUMAN, MAGE, "Regenmage");
        Player p = client.session().player();
        p.setHealth(20);
        p.setPower(100);
        client.clear();

        world.tick(2000);

        // RegenerateHealth: spirit 22 * gtOCTRegenHP(mage L1 0.079365) * 2 s = 3.49 → +3.
        // Regenerate(MANA): sqrt(int 23) * spirit 22 * gtRegenMPPerSpt(0.034965) = 3.689/s * 2 s → +7.
        assertEquals(23, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_HEALTH));
        assertEquals(107, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER1));
    }

    @Test
    void tpSl04RegenWhenFullShouldSendNothing() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, HUMAN, MAGE, "Fullmage");
        client.clear();

        world.tick(2000);

        assertFalse(client.saw(org.tbc.world.net.wow8606.Opcodes.SMSG_UPDATE_OBJECT));
        assertFalse(client.saw(org.tbc.world.net.wow8606.Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
    }

    @Test
    void tpSl04RegenWhenTimerNotElapsedShouldWait() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, HUMAN, MAGE, "Waitmage");
        Player p = client.session().player();
        p.setHealth(20);
        client.clear();

        world.tick(1000);
        assertFalse(client.saw(org.tbc.world.net.wow8606.Opcodes.SMSG_UPDATE_OBJECT));
        world.tick(1000);
        assertEquals(23, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_HEALTH));
    }

    @Test
    void tpSl04RegenWhenInCombatShouldNotHealButStillRegenMana() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, HUMAN, MAGE, "Fightmage");
        Player p = client.session().player();
        p.setHealth(20);
        p.setPower(100);
        p.inCombat = true;
        client.clear();

        world.tick(2000);

        assertEquals(20, p.health());
        assertEquals(107, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER1));
    }

    @Test
    void tpSl04ManaRegenWhenUnderFiveSecondRuleShouldPauseThenResume() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, HUMAN, MAGE, "Fsrmage");
        Player p = client.session().player();
        p.setPower(100);
        p.noteManaUse();
        client.clear();

        world.tick(2000);
        assertEquals(100, p.power(), "spirit regen paused for 5 s after a mana use (interrupt rate 0)");
        world.tick(2000);
        world.tick(2000);
        assertEquals(107, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER1));
    }

    @Test
    void tpSl04HealthRegenWhenSittingShouldBeHalfAgain() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, HUMAN, MAGE, "Sitmage");
        Player p = client.session().player();
        p.setHealth(20);
        p.sit();
        client.clear();

        world.tick(2000);

        // 1.746 * 1.5 * 2 s = 5.24 → +5.
        assertEquals(25, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_HEALTH));
    }

    @Test
    void tpSl04RageDecaysOutOfCombat() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, HUMAN, WARRIOR, "Ragewar");
        Player p = client.session().player();
        p.setRage(500);
        client.clear();

        world.tick(2000);

        // Regenerate(RAGE): uint32(2000 / 200) * 2.5 = 25 (2.5 rage), floor 0.
        assertEquals(475, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER2));
        p.setRage(10);
        client.clear();
        world.tick(2000);
        assertEquals(0, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER2));
    }

    @Test
    void tpSl04RageWhenInCombatShouldNotDecay() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, HUMAN, WARRIOR, "Fightwar");
        Player p = client.session().player();
        p.setRage(500);
        p.inCombat = true;
        client.clear();

        world.tick(2000);

        assertEquals(500, p.rage());
    }

    /** TP-SL04-018 — Regenerate(POWER_ENERGY): uint32(diff / 100) * EnergyRate; not gated on combat. */
    @Test
    void tpSl04EnergyRegensTwentyPerTickEvenInCombat() {
        World world = World.inMemory();
        WowClientDouble client = enter(world, HUMAN, ROGUE, "Stabber");
        Player p = client.session().player();
        p.setPower(40);
        p.inCombat = true;
        client.clear();

        world.tick(2000);

        assertEquals(60, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER4));
        p.setPower(95);
        client.clear();
        world.tick(2000);
        assertEquals(100, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER4), "clamped at max");
    }

    private static WowClientDouble enter(World world, int race, int clazz, String name) {
        WowClientDouble client = new WowClientDouble();
        world.addSession(client.connect(ACC));
        Player created = world.characters.create(ACC.id(), name, race, clazz, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
    }
}
