package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellEngineCreateItemTest {
    @Test
    void applyWhenCreateItemShouldAddWornShortswordToBag() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.guid = 1;
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_CREATE_ITEM, 0, 0, 0, 1, 1, 0f, Content.ITEM_WORN_SHORTSWORD);
        assertEquals(0, eng.apply(p, p, sp));
        assertEquals(1, p.items.size());
        Item it = p.items.values().iterator().next();
        assertEquals(Content.ITEM_WORN_SHORTSWORD, it.entry);
        assertEquals(1, it.count);
        assertEquals(0, it.bag);
        assertTrue(it.slot >= Player.INVENTORY_SLOT_ITEM_START);
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_CREATE_ITEM));
    }

    @Test
    void createItemWhenPlayerShouldSendItemPushResultCreated() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.guid = 1;
        List<Integer> ops = new ArrayList<>();
        Map<Integer, byte[]> last = new HashMap<>();
        Item it = eng.createItem(p, Content.ITEM_WORN_SHORTSWORD, 2, 7, (op, pay) -> {
            ops.add(op);
            last.put(op, pay);
        });
        assertEquals(Content.ITEM_WORN_SHORTSWORD, it.entry);
        assertEquals(2, it.count);
        assertTrue(ops.contains(Opcodes.SMSG_ITEM_PUSH_RESULT));
        WowBuffer b = new WowBuffer(last.get(Opcodes.SMSG_ITEM_PUSH_RESULT));
        assertEquals(p.guid, b.getU64());
        assertEquals(0, b.getU32());
        assertEquals(1, b.getU32());
        assertEquals(1, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(it.slot, b.getU32());
        assertEquals(Content.ITEM_WORN_SHORTSWORD, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU32());
        assertEquals(2, b.getU32());
        assertEquals(2, b.getU32());
    }

    @Test
    void createItemWhenInvalidOrBagFullShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        assertNull(eng.createItem(null, Content.ITEM_WORN_SHORTSWORD, 1, 1));
        Creature c = new Creature();
        assertNull(eng.createItem(c, Content.ITEM_WORN_SHORTSWORD, 1, 1));
        Player p = new Player();
        p.guid = 1;
        assertNull(eng.createItem(p, 0, 1, 1));
        assertNull(eng.createItem(p, -1, 1, 1));
        assertNull(eng.createItem(p, Content.ITEM_WORN_SHORTSWORD, 0, 1));
        assertNull(eng.createItem(p, Content.ITEM_WORN_SHORTSWORD, -1, 1));
        assertNull(eng.createItem(p, Content.ITEM_WORN_SHORTSWORD, 1, 0));
        assertEquals(0, p.items.size());
        assertEquals(0, eng.apply(p, c, new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_CREATE_ITEM, 0, 0, 0, 1, 1, 0f, Content.ITEM_WORN_SHORTSWORD)));
        assertEquals(0, eng.apply(p, p, new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_CREATE_ITEM, 0, 0, 0, 0, 0, 0f, Content.ITEM_WORN_SHORTSWORD)));
        for (int s = Player.INVENTORY_SLOT_ITEM_START; s < Player.INVENTORY_SLOT_ITEM_END; s++) {
            Item it = new Item(100 + s, Content.ITEM_WORN_SHORTSWORD);
            it.slot = s;
            p.items.put((int) it.guid, it);
        }
        assertNull(eng.createItem(p, Content.ITEM_WORN_SHORTSWORD, 1, 999));
    }
}
