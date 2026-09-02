package org.tbc.world.net.wow8606;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateBuilderTest {
    @Test
    void creatureCreateUsesRelocatedPosition() {
        Creature c = new Creature();
        c.guid = Guid.HIGH_CREATURE | 42;
        c.applyTemplate(6, "Kobold Vermin", 10913, 25, 42, 1);
        c.relocate(-6240f, 331f, 383f, 1.5f);
        byte[] raw = UpdateBuilder.createUnit(c, false, 0);
        WowBuffer b = new WowBuffer(raw);
        assertEquals(1, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(UpdateBuilder.UPDATETYPE_CREATE_OBJECT2, b.getU8());
        b.getPackedGuid();
        assertEquals(3, b.getU8());
        int flags = b.getU8();
        assertEquals(Unit.UPDATEFLAG_LIVING, flags & Unit.UPDATEFLAG_LIVING);
        assertEquals(0, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(0, b.getU32());
        assertEquals(-6240f, b.getFloat(), 0.01f);
        assertEquals(331f, b.getFloat(), 0.01f);
        assertEquals(383f, b.getFloat(), 0.01f);
        assertEquals(1.5f, b.getFloat(), 0.01f);
    }

    @Test
    void itemCreateIsTypeIdItemWithLowHighGuid() {
        Item it = new Item(7, 25);
        it.count = 1;
        it.durability = 20;
        byte[] raw = UpdateBuilder.createItem(it, 9032);
        WowBuffer b = new WowBuffer(raw);
        assertEquals(1, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(UpdateBuilder.UPDATETYPE_CREATE_OBJECT, b.getU8());
        long guid = b.getPackedGuid();
        assertEquals(UpdateBuilder.itemGuid(it), guid);
        assertEquals(UpdateBuilder.TYPEID_ITEM, b.getU8());
        assertEquals(UpdateBuilder.ITEM_CREATE_FLAGS, b.getU8());
        assertEquals(7, b.getU32());
        assertEquals(UpdateBuilder.ITEM_HIGHGUID, b.getU32());
        assertTrue(raw.length <= 100);
    }

    @Test
    void valuesUpdateWritesZeroHealth() {
        Creature c = new Creature();
        c.guid = Guid.HIGH_CREATURE | 42;
        c.applyTemplate(6, "Kobold Vermin", 10913, 25, 42, 1);
        c.setHealth(0);
        byte[] raw = UpdateBuilder.values(c, UpdateFields.UNIT_FIELD_HEALTH);
        WowBuffer b = new WowBuffer(raw);
        assertEquals(1, b.getU32());
        assertEquals(0, b.getU8());
        assertEquals(UpdateBuilder.UPDATETYPE_VALUES, b.getU8());
        assertEquals(c.guid, b.getPackedGuid());
        int nblocks = b.getU8();
        assertEquals((UpdateFields.UNIT_END + 31) / 32, nblocks);
        int[] mask = new int[nblocks];
        for (int i = 0; i < nblocks; i++) {
            mask[i] = b.getU32();
        }
        int field = UpdateFields.UNIT_FIELD_HEALTH;
        assertTrue((mask[field / 32] & (1 << (field % 32))) != 0);
        assertEquals(0, b.getU32());
    }
}
