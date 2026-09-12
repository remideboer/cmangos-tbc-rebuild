package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

    /**
     * VALUES fields after paper-doll sync: occupied slots plus the first free slot so an unequip
     * that cleared the last extra aura still reaches the client (Battle Stance stays in slot 0).
     */
    public static int[] paperDollAuraFields(Unit u) {
        int lastOcc = -1;
        for (int i = 0; i < MAX_AURAS; i++) {
            if (u.getInt(UpdateFields.UNIT_FIELD_AURA + i) != 0) {
                lastOcc = i;
            }
        }
        int firstFree = lastOcc + 1;
        if (firstFree >= MAX_AURAS) {
            firstFree = MAX_AURAS - 1;
        }
        int until = Math.max(lastOcc, firstFree);
        LinkedHashSet<Integer> fields = new LinkedHashSet<>();
        for (int slot = 0; slot <= until; slot++) {
            fields.add(UpdateFields.UNIT_FIELD_AURA + slot);
            fields.add(UpdateFields.UNIT_FIELD_AURAFLAGS + slot / 4);
            fields.add(UpdateFields.UNIT_FIELD_AURALEVELS + slot / 4);
            fields.add(UpdateFields.UNIT_FIELD_AURAAPPLICATIONS + slot / 4);
        }
        int[] out = new int[fields.size()];
        int n = 0;
        for (int f : fields) {
            out[n++] = f;
        }
        return out;
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

    /** SpellAuraHolder remove visible slot (SetAura 0, flags/levels/applications cleared). */
    public static void clearVisible(Unit target, int slot) {
        if (slot < 0 || slot >= MAX_AURAS) {
            return;
        }
        target.setInt(UpdateFields.UNIT_FIELD_AURA + slot, 0);
        setPackedByte(target, UpdateFields.UNIT_FIELD_AURAFLAGS, slot, 0);
        setPackedByte(target, UpdateFields.UNIT_FIELD_AURALEVELS, slot, 0);
        setPackedByte(target, UpdateFields.UNIT_FIELD_AURAAPPLICATIONS, slot, 0);
    }

    /**
     * Unit::_UpdateSpells: holders whose remaining duration has elapsed
     * (not permanent: duration 0 / expireAt 0) are removed AURA_REMOVE_BY_EXPIRE.
     */
    public static void expireTimed(Unit target, long nowMs, BiConsumer<Integer, byte[]> send) {
        if (target == null) {
            return;
        }
        ArrayList<Integer> expired = new ArrayList<>();
        for (Unit.Aura a : target.auras) {
            if (a.durationMs() > 0 && a.expireAtMs() > 0 && nowMs >= a.expireAtMs()) {
                expired.add(a.spellId());
            }
        }
        for (int spellId : expired) {
            int slot = slotOf(target, spellId);
            target.auras.removeIf(a -> a.spellId() == spellId);
            if (slot >= 0) {
                clearVisible(target, slot);
                if (send != null) {
                    var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(target,
                            UpdateFields.UNIT_FIELD_AURA + slot,
                            UpdateFields.UNIT_FIELD_AURAFLAGS + slot / 4,
                            UpdateFields.UNIT_FIELD_AURALEVELS + slot / 4,
                            UpdateFields.UNIT_FIELD_AURAAPPLICATIONS + slot / 4));
                    send.accept(upd.opcode(), upd.payload());
                }
            }
        }
    }

    /**
     * Aura::Update m_isPeriodic: holders whose Amplitude has elapsed fire PeriodicTick
     * and schedule the next tick (one tick per pulse, matching CMaNGOS Unit::Update).
     */
    public static void pulsePeriodic(Unit target, long nowMs, Consumer<Unit.Aura> onTick) {
        if (target == null || onTick == null) {
            return;
        }
        for (int i = 0; i < target.auras.size(); i++) {
            Unit.Aura a = target.auras.get(i);
            if (a.amplitudeMs() > 0 && a.nextTickAtMs() > 0 && nowMs >= a.nextTickAtMs()) {
                onTick.accept(a);
                target.auras.set(i, a.withNextTick(a.nextTickAtMs() + a.amplitudeMs()));
            }
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
