package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.map.Terrain;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinderHandlerTest {
    private ObjectMgr mgr;
    private GameMap map;
    private Player p;
    private final List<Integer> ops = new ArrayList<>();
    private final Map<Integer, byte[]> last = new HashMap<>();

    @BeforeEach
    void setUp() {
        mgr = new ObjectMgr();
        mgr.load(null, null);
        map = new GameMap(0, 0);
        p = new Player();
        p.guid = 1;
        p.setHealth(100);
        p.relocate(0, 0, 0, 0);
        map.add(p);
        ops.clear();
        last.clear();
    }

    @Test
    void activateWhenInnkeeperShouldBindAtPlayerLocation() {
        Creature inn = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        BinderHandler.activate(p, map, new Terrain(null), guid(inn.guid), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_BINDPOINTUPDATE));
        assertTrue(ops.contains(Opcodes.SMSG_PLAYERBOUND));
        assertTrue(ops.contains(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertTrue(ops.contains(Opcodes.SMSG_GOSSIP_COMPLETE));
        WowBuffer bind = new WowBuffer(last.get(Opcodes.SMSG_BINDPOINTUPDATE));
        assertEquals(p.x, bind.getFloat());
        assertEquals(p.y, bind.getFloat());
        assertEquals(p.z, bind.getFloat());
        assertEquals(0, bind.getU32());
        int area = bind.getU32();
        WowBuffer bought = new WowBuffer(last.get(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertEquals(inn.guid, bought.getU64());
        assertEquals(BinderHandler.SPELL_BIND, bought.getU32());
        WowBuffer bound = new WowBuffer(last.get(Opcodes.SMSG_PLAYERBOUND));
        assertEquals(inn.guid, bound.getU64());
        assertEquals(area, bound.getU32());
        assertEquals(p.x, p.bindX);
        assertEquals(0, p.bindMap);
        assertEquals(area, p.bindZone);
        assertTrue(p.dirty);
    }

    @Test
    void activateWhenBadInputShouldIgnore() {
        Creature inn = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        Creature kobold = spawn(6, 0, 0);
        BinderHandler.activate(p, map, new Terrain(null), new WowBuffer(3), this::capture);
        BinderHandler.activate(p, map, new Terrain(null), guid(0), this::capture);
        BinderHandler.activate(p, map, new Terrain(null), guid(kobold.guid), this::capture);
        p.relocate(40, 0, 0, 0);
        BinderHandler.activate(p, map, new Terrain(null), guid(inn.guid), this::capture);
        p.relocate(0, 0, 0, 0);
        p.setHealth(0);
        BinderHandler.activate(p, map, new Terrain(null), guid(inn.guid), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertFalse(ops.contains(Opcodes.SMSG_BINDPOINTUPDATE));
        assertFalse(ops.contains(Opcodes.SMSG_PLAYERBOUND));
    }

    private Creature spawn(int entry, float x, float y) {
        Creature c = mgr.spawnCreature(entry, 0, x, y, 0, 0, null);
        map.add(c);
        return c;
    }

    private void capture(int opcode, byte[] payload) {
        ops.add(opcode);
        last.put(opcode, payload);
    }

    private static WowBuffer guid(long g) {
        WowBuffer b = new WowBuffer(8);
        b.putU64(g);
        return b;
    }
}
