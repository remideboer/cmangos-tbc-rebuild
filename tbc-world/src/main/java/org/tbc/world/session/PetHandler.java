package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

/** Pet bar, stable, totem. Layout: spec/03-protocol/packets/pet.md */
public final class PetHandler {
    public static final int ACT_COMMAND = 0x07;
    public static final int COMMAND_ATTACK = 2;
    public static final int COMMAND_DISMISS = 3;
    public static final int STABLE_OK = 0x08;
    public static final int UNSTABLE_OK = 0x09;
    public static final int BUY_SLOT_OK = 0x0A;
    public static final int CLASS_HUNTER = 3;

    private PetHandler() {}

    public static void action(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        long petGuid = in.remaining() >= 8 ? in.getU64() : 0;
        int data = in.remaining() >= 4 ? in.getU32() : 0;
        long target = in.remaining() >= 8 ? in.getU64() : 0;
        int cmd = data & 0xFFFFFF;
        int type = (data >>> 24) & 0xFF;
        if (p.pet == null) {
            p.pet = new Pet();
            p.pet.summoned = true;
            p.pet.name = "Pet";
            p.pet.guid = Guid.HIGH_CREATURE | (p.guid & 0xFFFFFF);
        }
        if (type == ACT_COMMAND && cmd == COMMAND_DISMISS && p.clazz != CLASS_HUNTER) {
            p.pet = null;
            WowBuffer hide = new WowBuffer(8);
            hide.putU64(0);
            s.send(Opcodes.SMSG_PET_SPELLS, hide.array());
            return;
        }
        if (p.pet != null && p.pet.guid == 0) {
            p.pet.guid = petGuid != 0 ? petGuid : (Guid.HIGH_CREATURE | (p.guid & 0xFFFFFF));
        }
        if (type == ACT_COMMAND && cmd == COMMAND_ATTACK && p.pet != null) {
            WowBuffer atk = new WowBuffer(16);
            atk.putU64(p.pet.guid);
            atk.putU64(target);
            s.send(Opcodes.SMSG_ATTACKSTART, atk.array());
        }
        if (p.pet != null) {
            s.send(Opcodes.SMSG_PET_SPELLS, encodeBar(p.pet));
        }
    }

    public static void abandon(WorldSession s, WowBuffer in) {
        if (in.remaining() >= 8) {
            in.getU64();
        }
        Player p = s.player();
        if (p.clazz != CLASS_HUNTER) {
            p.pet = null;
        }
    }

    public static void destroyTotem(WorldSession s, WowBuffer in) {
        Player p = s.player();
        int slot = in.remaining() > 0 ? in.getU8() : 0;
        if (slot < 0 || slot >= p.totems.length) {
            return;
        }
        long guid = p.totems[slot];
        p.totems[slot] = 0;
        if (guid != 0) {
            WowBuffer d = new WowBuffer(8);
            d.putU64(guid);
            s.send(Opcodes.SMSG_DESTROY_OBJECT, d.array());
        }
    }

    public static void buyStableSlot(WorldSession s) {
        s.send(Opcodes.SMSG_STABLE_RESULT, new byte[]{(byte) BUY_SLOT_OK});
    }

    public static void stablePet(WorldSession s) {
        s.send(Opcodes.SMSG_STABLE_RESULT, new byte[]{(byte) STABLE_OK});
    }

    public static void unstablePet(WorldSession s) {
        s.send(Opcodes.SMSG_STABLE_RESULT, new byte[]{(byte) UNSTABLE_OK});
    }

    static byte[] encodeBar(Pet pet) {
        WowBuffer b = new WowBuffer(64);
        b.putU64(pet.guid);
        b.putU32(0);
        b.putU8(1);
        b.putU8(1);
        b.putU16(0);
        for (int i = 0; i < 10; i++) {
            b.putU32(pet.actionBar[i]);
        }
        b.putU8(0);
        b.putU8(0);
        return b.array();
    }
}
