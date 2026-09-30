package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.world.content.WeaponSkills;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Slice 12 combat skill-up wire — CMaNGOS UpdateCombatSkills / UpdateWeaponSkill.
 */
class Slice12P0Test {
    private static final World.Account ACC =
            new World.Account(1, "SKILLUP", new byte[40], 3, 1, "Win", "x86");

    /**
     * TP-SL12-002 — white mainhand swing raises unarmed skill; VALUES carries PLAYER_SKILL_INFO value word.
     */
    @Test
    void tpSl12WeaponSkillUpOnWhiteSwing() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Skiller", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.learnSkill(WeaponSkills.SKILL_UNARMED, 1, 5, 0);
        int skillValueField = skillValueField(p, WeaponSkills.SKILL_UNARMED);
        assertEquals(1, p.skillValue(WeaponSkills.SKILL_UNARMED));

        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x, c.y, c.z, c.o);
        client.clear();
        client.attackSwing(world, c.guid);
        client.session().tick(world, 0);

        assertTrue(client.saw(Opcodes.SMSG_ATTACKERSTATEUPDATE));
        assertEquals(2, p.skillValue(WeaponSkills.SKILL_UNARMED));
        int packed = client.valuesField(p.guid, skillValueField);
        assertEquals(2, packed & 0xFFFF);
        assertEquals(5, (packed >>> 16) & 0xFFFF);
    }

    /** TP-SL12-002 — no skill VALUES gain when already at max. */
    @Test
    void tpSl12WeaponSkillAtCapShouldNotSendSkillGain() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Capped", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.learnSkill(WeaponSkills.SKILL_UNARMED, 5, 5, 0);
        int skillValueField = skillValueField(p, WeaponSkills.SKILL_UNARMED);

        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        p.relocate(c.x, c.y, c.z, c.o);
        client.clear();
        client.attackSwing(world, c.guid);
        client.session().tick(world, 0);

        assertTrue(client.saw(Opcodes.SMSG_ATTACKERSTATEUPDATE));
        assertEquals(5, p.skillValue(WeaponSkills.SKILL_UNARMED));
        assertThrows(AssertionError.class, () -> client.valuesField(p.guid, skillValueField));
    }

    private static int skillValueField(Player p, int skillId) {
        int want = skillId & 0xFFFF;
        for (int slot = 0; slot < 127; slot++) {
            int base = UpdateFields.PLAYER_SKILL_INFO_1_1 + slot * 3;
            if ((p.getInt(base) & 0xFFFF) == want) {
                return base + 1;
            }
        }
        throw new AssertionError("skill " + skillId + " not learned");
    }
}
