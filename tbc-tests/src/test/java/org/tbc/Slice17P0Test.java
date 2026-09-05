package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.pvp.PvpObjectives;
import org.tbc.world.session.DeathHandler;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL17-* from spec/03-protocol/packets/death.md */
class Slice17P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");

    @Test
    void tpSl17RepopGhostAtGraveyard() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Ghost");
        Player p = client.session().player();
        float deathX = p.x;
        float deathY = p.y;
        p.setHealth(0);
        client.clear();
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        assertTrue(p.ghost);
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.GHOST_AURA));
        assertNotNull(p.corpse);
        assertEquals(deathX, p.corpse.x, 0.01);
        assertEquals(deathY, p.corpse.y, 0.01);
        byte[] loc = lastPayload(client, Opcodes.SMSG_DEATH_RELEASE_LOC);
        assertEquals(DeathHandler.GY_ELWYNN_MAP, WowClientDouble.u32le(loc, 0));
        assertEquals(DeathHandler.GY_ELWYNN_X, WowClientDouble.floatle(loc, 4), 0.01);
        assertEquals(DeathHandler.GY_ELWYNN_Y, WowClientDouble.floatle(loc, 8), 0.01);
        assertEquals(DeathHandler.GY_ELWYNN_Z, WowClientDouble.floatle(loc, 12), 0.01);
        assertEquals(DeathHandler.CORPSE_RECLAIM_DELAY_FIRST_MS,
                WowClientDouble.u32le(lastPayload(client, Opcodes.SMSG_CORPSE_RECLAIM_DELAY), 0));
        assertTrue(sawSpellGo(client, PvpObjectives.GHOST_AURA));
        assertEquals(Player.PLAYER_FLAGS_GHOST, p.getInt(UpdateFields.PLAYER_FLAGS) & Player.PLAYER_FLAGS_GHOST);
        assertTrue(client.saw(Opcodes.SMSG_MOVE_WATER_WALK));
        assertTrue(client.saw(Opcodes.MSG_MOVE_TELEPORT_ACK));
        assertFalse(client.saw(Opcodes.SMSG_NEW_WORLD));
    }

    @Test
    void tpSl17RepopWhenDunMoroghShouldUseClosestGraveyard() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Piep");
        Player p = client.session().player();
        p.relocate(-6240f, 331f, 383f, 0);
        p.setHealth(0);
        client.clear();
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        byte[] loc = lastPayload(client, Opcodes.SMSG_DEATH_RELEASE_LOC);
        assertEquals(0, WowClientDouble.u32le(loc, 0));
        float gx = WowClientDouble.floatle(loc, 4);
        float gy = WowClientDouble.floatle(loc, 8);
        assertEquals(-6220f, gx, 0.01);
        assertEquals(330f, gy, 0.01);
        assertTrue(Math.abs(gx - DeathHandler.GY_ELWYNN_X) > 100);
        assertTrue(p.ghost);
        assertTrue(client.saw(Opcodes.MSG_MOVE_TELEPORT_ACK));
        assertFalse(client.saw(Opcodes.SMSG_NEW_WORLD));
    }

    @Test
    void tpSl17ReclaimHalfHpNoSickness() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Ghost");
        Player p = client.session().player();
        p.setHealth(0);
        WowBuffer repop = new WowBuffer(1);
        repop.putU8(0);
        client.handle(world, Opcodes.CMSG_REPOP_REQUEST, repop.array());
        p.ghostTimeMs = world.nowMs() - DeathHandler.CORPSE_RECLAIM_DELAY_FIRST_MS;
        p.relocate(p.corpse.x, p.corpse.y, p.corpse.z, 0);
        WowBuffer reclaim = new WowBuffer(8);
        reclaim.putU64(p.guid);
        client.handle(world, Opcodes.CMSG_RECLAIM_CORPSE, reclaim.array());
        assertFalse(p.ghost);
        assertEquals(p.maxHealth() / 2, p.health());
        assertTrue(p.auras.stream().noneMatch(a -> a.spellId() == PvpObjectives.SICKNESS));
    }

    @Test
    void tpSl17SpiritHealerSickness() {
        World world = World.inMemory();
        WowClientDouble client = login(world, "Ghost");
        Player p = client.session().player();
        p.setGhost(true);
        p.level = 11;
        Item gear = new Item(world.nextItemGuid(), 25);
        gear.durability = 100;
        p.items.put((int) gear.guid, gear);
        client.clear();
        client.handle(world, Opcodes.CMSG_SPIRIT_HEALER_ACTIVATE, new byte[8]);
        assertEquals(p.maxHealth() / 2, p.health());
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == PvpObjectives.SICKNESS));
        assertEquals(75, gear.durability);
        assertTrue(sawSpellGo(client, PvpObjectives.SICKNESS));
    }

    private static WowClientDouble login(World world, String name) {
        WowClientDouble client = new WowClientDouble();
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), name, 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        return client;
    }

    private static boolean sawSpellGo(WowClientDouble client, int spellId) {
        for (int i = 0; i < client.opcodes.size(); i++) {
            if (client.opcodes.get(i) != Opcodes.SMSG_SPELL_GO) {
                continue;
            }
            byte[] p = client.payloads.get(i);
            for (int off = 0; off + 4 <= p.length; off++) {
                if (WowClientDouble.u32le(p, off) == spellId) {
                    return true;
                }
            }
        }
        return false;
    }

    private static byte[] lastPayload(WowClientDouble client, int opcode) {
        for (int i = client.opcodes.size() - 1; i >= 0; i--) {
            if (client.opcodes.get(i) == opcode) {
                return client.payloads.get(i);
            }
        }
        throw new AssertionError("missing opcode " + opcode);
    }
}
