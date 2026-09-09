package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.function.BiConsumer;

/**
 * SpellAuraHolder::_AddAura visible slot (MAX_AURAS 56) and SendAuraDuration.
 * Packed FLAGS/LEVELS/APPLICATIONS: 4 slots per uint32, slot%4 as the byte.
 */
public final class AuraSlots {
    public static final int MAX_AURAS = 56;
    public static final int AFLAG_EFFECT_0 = 0x01;
    public static final int AFLAG_CANCELABLE = 0x10;

    private AuraSlots() {}

    /** SpellAuraHolder::_AddAura — first free slot &lt; MAX_AURAS, or -1. */
    public static int applyVisible(Unit target, int spellId, int casterLevel, int stacks) {
        int slot = 0;
        while (slot < MAX_AURAS && target.getInt(UpdateFields.UNIT_FIELD_AURA + slot) != 0) {
            slot++;
        }
        if (slot >= MAX_AURAS) {
            return -1;
        }
        target.setInt(UpdateFields.UNIT_FIELD_AURA + slot, spellId);
        setPackedByte(target, UpdateFields.UNIT_FIELD_AURAFLAGS, slot, AFLAG_EFFECT_0 | AFLAG_CANCELABLE);
        setPackedByte(target, UpdateFields.UNIT_FIELD_AURALEVELS, slot, casterLevel & 0xFF);
        setPackedByte(target, UpdateFields.UNIT_FIELD_AURAAPPLICATIONS, slot, Math.max(0, stacks - 1) & 0xFF);
        return slot;
    }

    public static int slotOf(Unit target, int spellId) {
        for (int slot = 0; slot < MAX_AURAS; slot++) {
            if (target.getInt(UpdateFields.UNIT_FIELD_AURA + slot) == spellId) {
                return slot;
            }
        }
        return -1;
    }

    /**
     * VALUES for the slot plus SMSG_UPDATE_AURA_DURATION to a player target
     * (SpellAuraHolder::SendAuraDuration).
     */
    public static void sendApply(Unit target, int spellId, int remainMs, BiConsumer<Integer, byte[]> send) {
        int slot = slotOf(target, spellId);
        if (slot < 0 || send == null) {
            return;
        }
        var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(target,
                UpdateFields.UNIT_FIELD_AURA + slot,
                UpdateFields.UNIT_FIELD_AURAFLAGS + slot / 4,
                UpdateFields.UNIT_FIELD_AURALEVELS + slot / 4,
                UpdateFields.UNIT_FIELD_AURAAPPLICATIONS + slot / 4));
        send.accept(upd.opcode(), upd.payload());
        if (target instanceof Player) {
            WowBuffer dur = new WowBuffer(5);
            dur.putU8(slot);
            dur.putU32(remainMs);
            send.accept(Opcodes.SMSG_UPDATE_AURA_DURATION, dur.array());
        }
    }

    static void setPackedByte(Unit target, int fieldBase, int slot, int value) {
        int index = slot / 4;
        int shift = (slot % 4) * 8;
        int val = target.getInt(fieldBase + index);
        val &= ~(0xFF << shift);
        val |= (value & 0xFF) << shift;
        target.setInt(fieldBase + index, val);
    }
}
