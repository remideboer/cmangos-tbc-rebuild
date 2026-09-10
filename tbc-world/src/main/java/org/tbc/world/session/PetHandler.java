package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.combat.MeleeTable;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.spell.SpellCastTargets;
import org.tbc.world.world.World;

/** Pet bar, stable, totem. Layout: spec/03-protocol/packets/pet.md */
public final class PetHandler {
    public static final int ACT_COMMAND = 0x07;
    public static final int ACT_REACTION = 0x06;
    public static final int ACT_ENABLED = 0xC1;
    public static final int ACT_DISABLED = 0x81;
    public static final int ACT_PASSIVE = 0x01;
    public static final int COMMAND_ATTACK = 2;
    public static final int COMMAND_DISMISS = 3;
    public static final int MAX_ACTION_BAR = 10;
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
            if (p.clazz == CLASS_HUNTER) {
                p.pet.petType = Pet.HUNTER_PET;
                p.pet.canRename = true;
            }
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
            // PetHandler.cpp COMMAND_ATTACK → AttackStart; first swing hits when in range.
            Creature prey = world.map(p.mapId, p.instanceId).creatures.get(target);
            if (prey != null && prey.alive()) {
                Unit petUnit = new Unit(UpdateFields.UNIT_END, Unit.TYPEID_UNIT);
                petUnit.guid = p.pet.guid;
                int dmg = Math.max(1, (int) petUnit.getFloat(UpdateFields.UNIT_FIELD_MINDAMAGE));
                prey.setHealth(Math.max(0, prey.health() - dmg));
                MeleeTable.Result hit = new MeleeTable.Result(MeleeTable.Outcome.HIT, dmg, dmg);
                s.send(Opcodes.SMSG_ATTACKERSTATEUPDATE, world.combat.encodeAttack(petUnit, prey, hit));
            }
        }
        if (p.pet != null) {
            s.send(Opcodes.SMSG_PET_SPELLS, encodeBar(p.pet));
        }
    }

    /** HandlePetRename — hunter pet with UNIT_CAN_BE_RENAMED only. */
    public static void rename(WorldSession s, WowBuffer in) {
        Player p = s.player();
        long guid = in.remaining() >= 8 ? in.getU64() : 0;
        String name = in.remaining() > 0 ? in.getCString() : "";
        if (in.remaining() > 0) {
            in.getU8();
        }
        Pet pet = p.pet;
        if (pet == null || pet.guid != guid || pet.petType != Pet.HUNTER_PET || !pet.canRename) {
            return;
        }
        int res = Pet.checkName(name);
        if (res != Pet.PET_NAME_SUCCESS) {
            WowBuffer out = new WowBuffer(8 + name.length());
            out.putU32(res);
            out.putCString(name);
            out.putU8(0);
            s.send(Opcodes.SMSG_PET_NAME_INVALID, out.array());
            return;
        }
        pet.name = name;
        pet.canRename = false;
        pet.nameTimestamp = (int) (System.currentTimeMillis() / 1000L);
        Unit u = new Unit(UpdateFields.UNIT_END, Unit.TYPEID_UNIT);
        u.guid = pet.guid;
        u.setInt(UpdateFields.UNIT_FIELD_PET_NAME_TIMESTAMP, pet.nameTimestamp);
        u.setInt(UpdateFields.UNIT_FIELD_BYTES_2, pet.unitBytes2());
        var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.values(
                u, UpdateFields.UNIT_FIELD_PET_NAME_TIMESTAMP, UpdateFields.UNIT_FIELD_BYTES_2));
        s.send(pkt.opcode(), pkt.payload());
    }

    /** HandlePetSetAction — bar index &lt; 10; command/reaction only via 24-byte swap. */
    public static void setAction(WorldSession s, WowBuffer in) {
        Player p = s.player();
        int size = in.size();
        long guid = in.remaining() >= 8 ? in.getU64() : 0;
        Pet pet = p.pet;
        if (pet == null || pet.guid != guid) {
            return;
        }
        int count = size == 24 ? 2 : 1;
        int[] position = new int[2];
        int[] data = new int[2];
        boolean moveCommand = false;
        for (int i = 0; i < count; i++) {
            if (in.remaining() < 8) {
                return;
            }
            position[i] = in.getU32();
            data[i] = in.getU32();
            int act = (data[i] >>> 24) & 0xFF;
            if (position[i] < 0 || position[i] >= MAX_ACTION_BAR) {
                return;
            }
            if (act == ACT_COMMAND || act == ACT_REACTION) {
                if (count == 1) {
                    return;
                }
                moveCommand = true;
            }
        }
        if (moveCommand) {
            int act0 = (data[0] >>> 24) & 0xFF;
            if (act0 == ACT_COMMAND || act0 == ACT_REACTION) {
                int other = pet.actionBar[position[1]];
                if ((other & 0xFFFFFF) != (data[0] & 0xFFFFFF)
                        || ((other >>> 24) & 0xFF) != act0) {
                    return;
                }
            }
            int act1 = (data[1] >>> 24) & 0xFF;
            if (act1 == ACT_COMMAND || act1 == ACT_REACTION) {
                int other = pet.actionBar[position[0]];
                if ((other & 0xFFFFFF) != (data[1] & 0xFFFFFF)
                        || ((other >>> 24) & 0xFF) != act1) {
                    return;
                }
            }
        }
        for (int i = 0; i < count; i++) {
            int spellId = data[i] & 0xFFFFFF;
            int act = (data[i] >>> 24) & 0xFF;
            boolean spellAct = act == ACT_ENABLED || act == ACT_DISABLED || act == ACT_PASSIVE;
            if (spellAct && spellId != 0 && !pet.spells.contains(spellId)) {
                continue;
            }
            pet.actionBar[position[i]] = data[i];
        }
        s.send(Opcodes.SMSG_PET_SPELLS, encodeBar(pet));
    }

    /** HandlePetSpellAutocastOpcode — known spell on a spell-bar slot. */
    public static void spellAutocast(WorldSession s, WowBuffer in) {
        Player p = s.player();
        long guid = in.remaining() >= 8 ? in.getU64() : 0;
        int spellId = in.remaining() >= 4 ? in.getU32() : 0;
        int state = in.remaining() > 0 ? in.getU8() : 0;
        Pet pet = p.pet;
        if (pet == null || pet.guid != guid || spellId == 0 || !pet.spells.contains(spellId)) {
            return;
        }
        int type = state != 0 ? ACT_ENABLED : ACT_DISABLED;
        boolean found = false;
        for (int i = 0; i < pet.actionBar.length; i++) {
            int packed = pet.actionBar[i];
            int act = (packed >>> 24) & 0xFF;
            if ((packed & 0xFFFFFF) != spellId) {
                continue;
            }
            if (act != ACT_ENABLED && act != ACT_DISABLED && act != ACT_PASSIVE) {
                continue;
            }
            pet.actionBar[i] = spellId | (type << 24);
            found = true;
            break;
        }
        if (!found) {
            return;
        }
        s.send(Opcodes.SMSG_PET_SPELLS, encodeBar(pet));
    }

    /** HandlePetCastSpellOpcode — learned catalog spell; START caster is the pet. */
    public static void castSpell(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        long guid = in.remaining() >= 8 ? in.getU64() : 0;
        int spellId = in.remaining() >= 4 ? in.getU32() : 0;
        SpellCastTargets targets = SpellCastTargets.read(in);
        Pet pet = p.pet;
        if (pet == null || pet.guid != guid || spellId == 0 || !pet.spells.contains(spellId)) {
            return;
        }
        var sp = world.spells.info(spellId);
        if (sp == null) {
            return;
        }
        s.send(Opcodes.SMSG_SPELL_START, world.spells.encodeStart(pet.guid, spellId, 0, sp.castTimeMs(), targets));
        if (sp.castTimeMs() == 0) {
            long hit = targets.unitGuid != 0 ? targets.unitGuid : pet.guid;
            s.send(Opcodes.SMSG_SPELL_GO, world.spells.encodeGo(pet.guid, hit, spellId, world.nowMs(), targets));
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
