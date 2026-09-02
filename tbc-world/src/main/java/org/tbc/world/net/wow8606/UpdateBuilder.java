package org.tbc.world.net.wow8606;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;

import java.util.zip.Deflater;

/** SMSG_UPDATE_OBJECT / compressed. spec/03-protocol/packets/update-object.md */
public final class UpdateBuilder {
    public static final int UPDATETYPE_VALUES = 0;
    public static final int UPDATETYPE_CREATE_OBJECT = 2;
    public static final int UPDATETYPE_CREATE_OBJECT2 = 3;
    public static final int UPDATETYPE_OUT_OF_RANGE = 4;
    public static final int TYPEID_ITEM = 1;
    public static final int TYPEMASK_OBJECT_ITEM = 0x0003;
    public static final int ITEM_HIGHGUID = 0x4000;
    public static final int ITEM_CREATE_FLAGS = Unit.UPDATEFLAG_LOWGUID | Unit.UPDATEFLAG_HIGHGUID;
    public static final float WALK = 2.5f;
    public static final float RUN = 7f;
    public static final float RUN_BACK = 4.5f;
    public static final float SWIM = 4.722222f;
    public static final float SWIM_BACK = 2.5f;
    public static final float FLIGHT = 7f;
    public static final float FLIGHT_BACK = 4.5f;
    public static final float TURN = 3.141594f;

    private UpdateBuilder() {}

    public static byte[] createUnit(Unit u, boolean self, int serverTime) {
        u.movement.x = u.x;
        u.movement.y = u.y;
        u.movement.z = u.z;
        u.movement.o = u.o;
        WowBuffer block = new WowBuffer(2048);
        block.putU8(UPDATETYPE_CREATE_OBJECT2);
        block.putPackedGuid(u.guid);
        block.putU8(u.typeId);
        int flags = self && u instanceof Player p ? p.createSelfFlags() : u.updateFlags;
        if (self && u instanceof Player) {
            flags = Player.PLAYER_CREATE_FLAGS;
        }
        block.putU8(flags);
        if ((flags & Unit.UPDATEFLAG_LIVING) != 0) {
            u.movement.write(block, false, u.guid, serverTime);
            block.putFloat(WALK);
            block.putFloat(RUN);
            block.putFloat(RUN_BACK);
            block.putFloat(SWIM);
            block.putFloat(SWIM_BACK);
            block.putFloat(FLIGHT);
            block.putFloat(FLIGHT_BACK);
            block.putFloat(TURN);
        }
        if ((flags & Unit.UPDATEFLAG_HIGHGUID) != 0) {
            block.putU32(0);
        }
        writeValues(block, u, self);
        return wrap(block);
    }

    public static long itemGuid(Item it) {
        return Guid.HIGH_ITEM | (Guid.low(it.guid) & 0xFFFFFFFFL);
    }

    public static byte[] createItem(Item it, long ownerGuid) {
        long guid = itemGuid(it);
        int[] v = new int[UpdateFields.ITEM_END];
        v[UpdateFields.OBJECT_FIELD_GUID] = (int) guid;
        v[UpdateFields.OBJECT_FIELD_GUID + 1] = (int) (guid >>> 32);
        v[UpdateFields.OBJECT_FIELD_TYPE] = TYPEMASK_OBJECT_ITEM;
        v[UpdateFields.OBJECT_FIELD_ENTRY] = it.entry;
        v[UpdateFields.OBJECT_FIELD_SCALE_X] = Float.floatToIntBits(1.0f);
        v[UpdateFields.ITEM_FIELD_OWNER] = (int) ownerGuid;
        v[UpdateFields.ITEM_FIELD_OWNER + 1] = (int) (ownerGuid >>> 32);
        v[UpdateFields.ITEM_FIELD_CONTAINED] = (int) ownerGuid;
        v[UpdateFields.ITEM_FIELD_CONTAINED + 1] = (int) (ownerGuid >>> 32);
        v[UpdateFields.ITEM_FIELD_STACK_COUNT] = Math.max(1, it.count);
        if (it.durability > 0) {
            v[UpdateFields.ITEM_FIELD_DURABILITY] = it.durability;
            v[UpdateFields.ITEM_FIELD_MAXDURABILITY] = Math.max(it.durability, 1);
        }
        WowBuffer block = new WowBuffer(256);
        block.putU8(UPDATETYPE_CREATE_OBJECT);
        block.putPackedGuid(guid);
        block.putU8(TYPEID_ITEM);
        block.putU8(ITEM_CREATE_FLAGS);
        block.putU32(Guid.low(guid));
        block.putU32(ITEM_HIGHGUID);
        writeValues(block, v);
        return wrap(block);
    }

    public static byte[] values(Unit u, int... fields) {
        WowBuffer block = new WowBuffer(64 + fields.length * 4);
        block.putU8(UPDATETYPE_VALUES);
        block.putPackedGuid(u.guid);
        int count = u.values.length;
        int nblocks = (count + 31) / 32;
        block.putU8(nblocks);
        int[] mask = new int[nblocks];
        for (int f : fields) {
            if (f >= 0 && f < count) {
                mask[f / 32] |= 1 << (f % 32);
            }
        }
        for (int m : mask) {
            block.putU32(m);
        }
        for (int i = 0; i < count; i++) {
            if ((mask[i / 32] & (1 << (i % 32))) != 0) {
                block.putU32(u.values[i]);
            }
        }
        return wrap(block);
    }

    public static void writeValues(WowBuffer out, Unit u, boolean owner) {
        int count = u.values.length;
        int blocks = (count + 31) / 32;
        out.putU8(blocks);
        int[] mask = new int[blocks];
        for (int i = 0; i < count; i++) {
            if (u.values[i] != 0 && u.visibleToOwner(i, owner)) {
                mask[i / 32] |= 1 << (i % 32);
            }
        }
        for (int m : mask) {
            out.putU32(m);
        }
        for (int i = 0; i < count; i++) {
            if ((mask[i / 32] & (1 << (i % 32))) != 0) {
                out.putU32(u.values[i]);
            }
        }
    }

    static void writeValues(WowBuffer out, int[] values) {
        int count = values.length;
        int blocks = (count + 31) / 32;
        out.putU8(blocks);
        int[] mask = new int[blocks];
        for (int i = 0; i < count; i++) {
            if (values[i] != 0) {
                mask[i / 32] |= 1 << (i % 32);
            }
        }
        for (int m : mask) {
            out.putU32(m);
        }
        for (int i = 0; i < count; i++) {
            if ((mask[i / 32] & (1 << (i % 32))) != 0) {
                out.putU32(values[i]);
            }
        }
    }

    static byte[] wrap(WowBuffer block) {
        WowBuffer pkt = new WowBuffer(block.size() + 8);
        pkt.putU32(1);
        pkt.putU8(0);
        pkt.putBytes(block.array());
        return pkt.array();
    }

    public static Compressed maybeCompress(byte[] uncompressed) {
        if (uncompressed.length <= 100) {
            return new Compressed(Opcodes.SMSG_UPDATE_OBJECT, uncompressed);
        }
        Deflater def = new Deflater(1);
        def.setInput(uncompressed);
        def.finish();
        byte[] buf = new byte[uncompressed.length + 64];
        int n = def.deflate(buf);
        def.end();
        WowBuffer out = new WowBuffer(n + 4);
        out.putU32(uncompressed.length);
        byte[] z = new byte[n];
        System.arraycopy(buf, 0, z, 0, n);
        out.putBytes(z);
        return new Compressed(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT, out.array());
    }

    public record Compressed(int opcode, byte[] payload) {}
}
