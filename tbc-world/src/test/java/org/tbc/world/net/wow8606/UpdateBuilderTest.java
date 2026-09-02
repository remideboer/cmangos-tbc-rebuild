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

    @Test
    void createSelfIncludesLanguageSkillFields() {
        org.tbc.world.entity.Player p = new org.tbc.world.entity.Player();
        p.guid = Guid.player(7);
        p.race = 7;
        p.clazz = 1;
        p.level = 1;
        p.applyCreateFields();
        byte[] raw = UpdateBuilder.createUnit(p, true, 0);
        WowBuffer b = new WowBuffer(raw);
        b.getU32();
        b.getU8();
        b.getU8();
        b.getPackedGuid();
        b.getU8();
        int flags = b.getU8();
        assertEquals(0x71, flags);
        b.getU32();
        b.getU8();
        b.getU32();
        b.getFloat();
        b.getFloat();
        b.getFloat();
        b.getFloat();
        b.getU32();
        for (int i = 0; i < 8; i++) {
            b.getFloat();
        }
        b.getU32();
        int nblocks = b.getU8();
        int[] mask = new int[nblocks];
        for (int i = 0; i < nblocks; i++) {
            mask[i] = b.getU32();
        }
        int[] values = new int[nblocks * 32];
        for (int i = 0; i < nblocks * 32; i++) {
            if ((mask[i / 32] & (1 << (i % 32))) != 0) {
                values[i] = b.getU32();
            }
        }
        int id = values[UpdateFields.PLAYER_SKILL_INFO_1_1] & 0xFFFF;
        int val = values[UpdateFields.PLAYER_SKILL_INFO_1_1 + 1] & 0xFFFF;
        int max = (values[UpdateFields.PLAYER_SKILL_INFO_1_1 + 1] >>> 16) & 0xFFFF;
        assertEquals(org.tbc.world.content.ChrStatic.SKILL_LANG_COMMON, id);
        assertEquals(300, val);
        assertEquals(300, max);
        assertEquals(org.tbc.world.content.ChrStatic.SKILL_LANG_GNOMISH,
                values[UpdateFields.PLAYER_SKILL_INFO_1_1 + 3] & 0xFFFF);
    }

    @Test
    void compressedCreateSelfRoundtripKeepsLanguageSkills() throws Exception {
        org.tbc.world.entity.Player p = new org.tbc.world.entity.Player();
        p.guid = Guid.player(7);
        p.race = 7;
        p.clazz = 1;
        p.level = 1;
        p.applyCreateFields();
        byte[] raw = UpdateBuilder.createUnit(p, true, 0);
        UpdateBuilder.Compressed z = UpdateBuilder.maybeCompress(raw);
        assertEquals(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT, z.opcode());
        WowBuffer hdr = new WowBuffer(z.payload());
        int unsized = hdr.getU32();
        assertEquals(raw.length, unsized);
        java.util.zip.Inflater inf = new java.util.zip.Inflater();
        inf.setInput(hdr.remainingBytes());
        byte[] round = new byte[unsized];
        int got = 0;
        while (got < unsized && !inf.finished()) {
            int n = inf.inflate(round, got, unsized - got);
            if (n == 0) {
                break;
            }
            got += n;
        }
        inf.end();
        assertEquals(unsized, got);
        assertEquals(java.util.Arrays.hashCode(raw), java.util.Arrays.hashCode(round));
    }

    @Test
    void languageValuesUpdateIncludesCommonSkill() {
        org.tbc.world.entity.Player p = new org.tbc.world.entity.Player();
        p.guid = Guid.player(1);
        p.race = 1;
        p.clazz = 1;
        p.applyCreateFields();
        int[] fields = org.tbc.world.session.LoginBurst.languageSkillFields(p);
        assertTrue(fields.length >= 2);
        byte[] raw = UpdateBuilder.values(p, fields);
        WowBuffer b = new WowBuffer(raw);
        b.getU32();
        b.getU8();
        b.getU8();
        b.getPackedGuid();
        int nblocks = b.getU8();
        int[] mask = new int[nblocks];
        for (int i = 0; i < nblocks; i++) {
            mask[i] = b.getU32();
        }
        int[] values = new int[nblocks * 32];
        for (int i = 0; i < nblocks * 32; i++) {
            if ((mask[i / 32] & (1 << (i % 32))) != 0) {
                values[i] = b.getU32();
            }
        }
        assertEquals(org.tbc.world.content.ChrStatic.SKILL_LANG_COMMON,
                values[UpdateFields.PLAYER_SKILL_INFO_1_1] & 0xFFFF);
    }
}
