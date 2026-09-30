package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
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
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BinderHandlerTest {
    private ObjectMgr mgr;
    private GameMap map;
    private Player p;
    private final List<Integer> ops = new ArrayList<>();
    private final Map<Integer, byte[]> last = new HashMap<>();
    private final AtomicLong nextItem = new AtomicLong(500);

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
        nextItem.set(500);
    }

    @Test
    void activateWhenInnkeeperShouldBindAtPlayerLocation() {
        Creature inn = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        BinderHandler.activate(p, map, new Terrain(null), guid(inn.guid), this::capture, nextItem::getAndIncrement, mgr);
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

    /** TP-SL08-029 — spell 3286 EffectCreateItem 6948 when the player has no hearthstone. */
    @Test
    void activateWhenMissingHearthstoneShouldCreateItem6948() {
        Creature inn = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        BinderHandler.activate(p, map, new Terrain(null), guid(inn.guid), this::capture, nextItem::getAndIncrement, mgr);
        assertEquals(1, countEntry(Content.ITEM_HEARTHSTONE));
        assertTrue(ops.contains(Opcodes.SMSG_ITEM_PUSH_RESULT));
        assertTrue(ops.contains(Opcodes.SMSG_ITEM_QUERY_SINGLE_RESPONSE));
        WowBuffer query = new WowBuffer(last.get(Opcodes.SMSG_ITEM_QUERY_SINGLE_RESPONSE));
        assertEquals(Content.ITEM_HEARTHSTONE, query.getU32());
        WowBuffer push = new WowBuffer(last.get(Opcodes.SMSG_ITEM_PUSH_RESULT));
        assertEquals(p.guid, push.getU64());
        push.getU32();
        push.getU32();
        push.getU32();
        push.getU8();
        push.getU32();
        assertEquals(Content.ITEM_HEARTHSTONE, push.getU32());
    }

    /** TP-SL08-029 — MaxCount 1: already owning 6948 must not get a second. */
    @Test
    void activateWhenAlreadyHasHearthstoneShouldNotDuplicate() {
        Item hs = new Item(42, Content.ITEM_HEARTHSTONE);
        hs.ownerGuid = 1;
        hs.bag = 0;
        hs.slot = Player.INVENTORY_SLOT_ITEM_START;
        hs.count = 1;
        p.items.put(42, hs);
        Creature inn = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        BinderHandler.activate(p, map, new Terrain(null), guid(inn.guid), this::capture, nextItem::getAndIncrement, mgr);
        assertTrue(ops.contains(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertEquals(1, countEntry(Content.ITEM_HEARTHSTONE));
        assertFalse(ops.contains(Opcodes.SMSG_ITEM_PUSH_RESULT));
    }

    @Test
    void activateWhenBadInputShouldIgnore() {
        Creature inn = spawn(Content.NPC_INNKEEPER_FARLEY, 0, 0);
        Creature kobold = spawn(6, 0, 0);
        BinderHandler.activate(p, map, new Terrain(null), new WowBuffer(3), this::capture, nextItem::getAndIncrement, mgr);
        BinderHandler.activate(p, map, new Terrain(null), guid(0), this::capture, nextItem::getAndIncrement, mgr);
        BinderHandler.activate(p, map, new Terrain(null), guid(kobold.guid), this::capture, nextItem::getAndIncrement, mgr);
        p.relocate(40, 0, 0, 0);
        BinderHandler.activate(p, map, new Terrain(null), guid(inn.guid), this::capture, nextItem::getAndIncrement, mgr);
        p.relocate(0, 0, 0, 0);
        p.setHealth(0);
        BinderHandler.activate(p, map, new Terrain(null), guid(inn.guid), this::capture, nextItem::getAndIncrement, mgr);
        assertFalse(ops.contains(Opcodes.SMSG_TRAINER_BUY_SUCCEEDED));
        assertFalse(ops.contains(Opcodes.SMSG_BINDPOINTUPDATE));
        assertFalse(ops.contains(Opcodes.SMSG_PLAYERBOUND));
    }

    private int countEntry(int entry) {
        int n = 0;
        for (Item it : p.items.values()) {
            if (it.entry == entry) {
                n++;
            }
        }
        return n;
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
