package org.tbc.bdd;

import org.junit.jupiter.api.Test;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The double's create-self decoder must ignore non-update, VALUES, and other-player blocks. */
class WowClientDoubleDecodeTest {
    private static Player player() {
        Player p = new Player();
        p.guid = 0x9032;
        p.race = 1;
        p.clazz = 1;
        p.money = 7;
        p.applyCreateFields();
        return p;
    }

    @Test
    void decodeSelfCreateWhenSelfBlockShouldReturnMaskedFields() {
        Map<Integer, Integer> v = WowClientDouble.decodeSelfCreate(UpdateBuilder.createUnit(player(), true, 0));
        assertEquals(7, v.get(UpdateFields.PLAYER_FIELD_COINAGE));
    }

    @Test
    void decodeSelfCreateWhenOtherPlayerOrValuesOrNullShouldReturnNull() {
        assertNull(WowClientDouble.decodeSelfCreate(UpdateBuilder.createUnit(player(), false, 0)));
        assertNull(WowClientDouble.decodeSelfCreate(UpdateBuilder.values(player(), UpdateFields.UNIT_FIELD_HEALTH)));
        assertNull(WowClientDouble.decodeSelfCreate(null));
    }

    @Test
    void valuesFieldWhenLatestValuesBlockCarriesFieldShouldReturnIt() {
        Player p = player();
        p.setHealth(1);
        WowClientDouble client = new WowClientDouble();
        client.send(Opcodes.SMSG_UPDATE_OBJECT, UpdateBuilder.values(p, UpdateFields.UNIT_FIELD_HEALTH));
        client.send(Opcodes.SMSG_UPDATE_OBJECT, UpdateBuilder.values(p, UpdateFields.PLAYER_FIELD_COINAGE));
        assertEquals(1, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_HEALTH));
        assertEquals(7, client.valuesField(p.guid, UpdateFields.PLAYER_FIELD_COINAGE));
    }

    @Test
    void valuesFieldWhenOtherGuidCreateBlockOrMissingFieldShouldThrow() {
        Player p = player();
        WowClientDouble client = new WowClientDouble();
        client.send(Opcodes.SMSG_UPDATE_OBJECT, UpdateBuilder.createUnit(p, true, 0));
        client.send(Opcodes.SMSG_MESSAGECHAT, new byte[] {1});
        client.send(Opcodes.SMSG_UPDATE_OBJECT, UpdateBuilder.values(p, UpdateFields.PLAYER_FIELD_COINAGE));
        assertThrows(AssertionError.class, () -> client.valuesField(p.guid + 1, UpdateFields.PLAYER_FIELD_COINAGE));
        assertThrows(AssertionError.class, () -> client.valuesField(p.guid, UpdateFields.UNIT_FIELD_HEALTH));
    }

    @Test
    void selfCreateValuesWhenNoSelfBlockReceivedShouldBeEmpty() {
        WowClientDouble client = new WowClientDouble();
        client.send(Opcodes.SMSG_MESSAGECHAT, new byte[] {1});
        assertEquals(Map.of(), client.selfCreateValues());
    }

    @Test
    void sawCreateObjectWhenCreateBlockMatchesGuidShouldReturnTrue() {
        Player p = player();
        WowClientDouble client = new WowClientDouble();
        client.send(Opcodes.SMSG_MESSAGECHAT, new byte[] {1});
        client.send(Opcodes.SMSG_UPDATE_OBJECT, UpdateBuilder.createUnit(p, true, 0));
        assertTrue(client.sawCreateObject(p.guid));
        assertFalse(client.sawCreateObject(p.guid + 1));
    }

    @Test
    void sawCreateObjectWhenItemCreateShouldMatchCreateObjectType() {
        Item it = new Item(100, 25);
        WowClientDouble client = new WowClientDouble();
        client.send(Opcodes.SMSG_UPDATE_OBJECT, UpdateBuilder.createItem(it, 1));
        assertTrue(client.sawCreateObject(UpdateBuilder.itemGuid(it)));
    }

    @Test
    void sawCreateObjectWhenEmptyValuesOrTruncatedShouldReturnFalse() {
        Player p = player();
        WowClientDouble client = new WowClientDouble();
        assertFalse(client.sawCreateObject(p.guid));
        client.send(Opcodes.SMSG_UPDATE_OBJECT, UpdateBuilder.values(p, UpdateFields.UNIT_FIELD_HEALTH));
        assertFalse(client.sawCreateObject(p.guid));
        WowBuffer truncated = new WowBuffer(5);
        truncated.putU32(1);
        truncated.putU8(0);
        client.send(Opcodes.SMSG_UPDATE_OBJECT, truncated.array());
        assertFalse(client.sawCreateObject(p.guid));
        assertNull(WowClientDouble.decodeCreateObjectGuid(null));
    }

    @Test
    void inflateUpdateWhenNotAnUpdateOpcodeShouldReturnNullOrPassThrough() {
        byte[] raw = new byte[] {1, 2, 3};
        assertSame(raw, WowClientDouble.inflateUpdate(Opcodes.SMSG_UPDATE_OBJECT, raw));
        assertNull(WowClientDouble.inflateUpdate(Opcodes.SMSG_MESSAGECHAT, raw));
    }

    @Test
    void inflateUpdateWhenCompressedShouldRoundTripAndRejectGarbage() {
        byte[] raw = UpdateBuilder.createUnit(player(), true, 0);
        UpdateBuilder.Compressed c = UpdateBuilder.maybeCompress(raw);
        assertEquals(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT, c.opcode());
        assertEquals(raw.length, WowClientDouble.inflateUpdate(c.opcode(), c.payload()).length);
        WowBuffer garbage = new WowBuffer(8);
        garbage.putU32(4);
        garbage.putU32(0xFFFFFFFF);
        assertThrows(IllegalStateException.class,
                () -> WowClientDouble.inflateUpdate(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT, garbage.array()));
    }
}
