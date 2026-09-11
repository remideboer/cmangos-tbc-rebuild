package org.tbc;

import org.junit.jupiter.api.Test;
import org.tbc.bdd.WowClientDouble;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Gap inventory TP-SL04-011..013 — create-self.md "Stats (level 1)".
 * Oracle: the self CREATE_OBJECT2 VALUES the 8606 client renders as its bars and character sheet.
 * Numbers are tbc-db rows: player_classlevelstats (1,1,20,0), player_levelstats (1,1,1,23,20,22,20,20),
 * CMaNGOS Unit::GetHealthBonusFromStamina (first 20 stamina 1 hp each, then 10 per point).
 */
class Slice04StatsTest {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 0, 1, "Win", "x86");

    private static final int RACE_HUMAN = 1;
    private static final int CLASS_WARRIOR = 1;
    private static final int CLASS_MAGE = 8;
    private static final int CLASS_ROGUE = 4;

    @Test
    void tpSl04CreateSelfStatsFromLevelStats() {
        World world = World.inMemory();
        Map<Integer, Integer> self = enterAndDecodeSelf(world, RACE_HUMAN, CLASS_WARRIOR, "Statwar");

        assertEquals(20, self.get(UpdateFields.UNIT_FIELD_BASE_HEALTH), "basehp from player_classlevelstats");
        assertEquals(23, self.get(UpdateFields.UNIT_FIELD_STAT0), "str");
        assertEquals(20, self.get(UpdateFields.UNIT_FIELD_STAT1), "agi");
        assertEquals(22, self.get(UpdateFields.UNIT_FIELD_STAT2), "sta");
        assertEquals(20, self.get(UpdateFields.UNIT_FIELD_STAT3), "int");
        assertEquals(20, self.get(UpdateFields.UNIT_FIELD_STAT4), "spi");
        // UpdateMaxHealth: create health + stamina bonus (20 + 2 * 10) = 60; full at create.
        assertEquals(60, self.get(UpdateFields.UNIT_FIELD_MAXHEALTH));
        assertEquals(60, self.get(UpdateFields.UNIT_FIELD_HEALTH));
        // SetArmor(createStats[agi] * 2)
        assertEquals(40, self.get(UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    @Test
    void tpSl04ManaClassHasManaBar() {
        World world = World.inMemory();
        Map<Integer, Integer> self = enterAndDecodeSelf(world, RACE_HUMAN, CLASS_MAGE, "Statmage");

        // player_classlevelstats (8,1,32,100); human mage int 23 → UpdateMaxPower: 100 + 20 + 3 * 15 = 165.
        assertEquals(100, self.get(UpdateFields.UNIT_FIELD_BASE_MANA));
        assertEquals(165, self.get(UpdateFields.UNIT_FIELD_MAXPOWER1));
        assertEquals(165, self.get(UpdateFields.UNIT_FIELD_POWER1), "full mana at create");
    }

    @Test
    void tpSl04RageClassHasNoManaBar() {
        World world = World.inMemory();
        Map<Integer, Integer> self = enterAndDecodeSelf(world, RACE_HUMAN, CLASS_WARRIOR, "Rager");

        assertNull(self.get(UpdateFields.UNIT_FIELD_MAXPOWER1), "basemana 0 → no mana bar for a warrior");
        assertEquals(1000, self.get(UpdateFields.UNIT_FIELD_MAXPOWER2));
    }

    /** TP-SL04-018 — InitStatsForLevel: SetMaxPower(POWER_ENERGY, POWER_ENERGY_DEFAULT) then SetPower full. */
    @Test
    void tpSl04RogueHasFullEnergyBar() {
        World world = World.inMemory();
        Map<Integer, Integer> self = enterAndDecodeSelf(world, RACE_HUMAN, CLASS_ROGUE, "Stabber");

        assertEquals(100, self.get(UpdateFields.UNIT_FIELD_MAXPOWER4));
        assertEquals(100, self.get(UpdateFields.UNIT_FIELD_POWER4), "full energy at create");
        assertNull(self.get(UpdateFields.UNIT_FIELD_MAXPOWER1), "basemana 0 → no mana bar for a rogue");
    }

    /** TP-SL04-018 — LoadFromDB restores characters.power4 (clamped to max), like power1. */
    @Test
    void tpSl04RogueEnergySurvivesRelog() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Relogrogue", RACE_HUMAN, CLASS_ROGUE, 0, 1, 1, 1, 1, 0,
                world.objectMgr);
        created.setPower(37);
        world.characters.save(created);
        client.login(world, created.guid);
        Map<Integer, Integer> self = client.selfCreateValues();

        assertEquals(37, self.get(UpdateFields.UNIT_FIELD_POWER4));
    }

    @Test
    void tpSl04XpBarFromXpForLevel() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Xpbar", RACE_HUMAN, CLASS_WARRIOR, 0, 1, 1, 1, 1, 0,
                world.objectMgr);
        created.xp = 150;
        world.characters.save(created);
        client.login(world, created.guid);
        Map<Integer, Integer> self = client.selfCreateValues();

        // player_xp_for_level (1,400): ObjectMgr::GetXPForLevel(1) in InitStatsForLevel.
        assertEquals(400, self.get(UpdateFields.PLAYER_NEXT_LEVEL_XP));
        assertEquals(150, self.get(UpdateFields.PLAYER_XP), "characters.xp shows on the bar");
    }

    /**
     * PaperDollFrame Damage uses UnitDamage() percent (PLAYER_FIELD_MOD_DAMAGE_DONE_PCT).
     * CMaNGOS InitStatsForLevel SetFloatValue(..., 1.00f) for MAX_SPELL_SCHOOL. Zero bits
     * make the client divide by 0 and render 1.#INF - 1.#INFx0.0%.
     */
    @Test
    void tpSl04DamageDonePctShouldBeOne() {
        World world = World.inMemory();
        Map<Integer, Integer> self = enterAndDecodeSelf(world, RACE_HUMAN, CLASS_WARRIOR, "Dmgpct");
        int one = Float.floatToIntBits(1.0f);
        for (int i = 0; i < 7; i++) {
            assertEquals(one, self.get(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT + i), "school " + i);
        }
    }

    private static Map<Integer, Integer> enterAndDecodeSelf(World world, int race, int clazz, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), name, race, clazz, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client.selfCreateValues();
    }
}
