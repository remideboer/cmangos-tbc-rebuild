package org.tbc.world.spell;

import org.tbc.common.WowBuffer;

/** CMSG read vs SMSG write are different (spell.md). Slice 7: SELF and UNIT* guids. */
public final class SpellCastTargets {
    public static final int UNIT = 0x00000002;
    public static final int UNIT_ENEMY = 0x00000080;
    public static final int UNIT_MINIPET = 0x00010000;
    public static final int UNIT_READ = UNIT | UNIT_ENEMY | UNIT_MINIPET;

    public int mask;
    public long unitGuid;

    public static SpellCastTargets read(WowBuffer in) {
        SpellCastTargets t = new SpellCastTargets();
        if (in.remaining() < 4) {
            return t;
        }
        t.mask = in.getU32();
        if (t.mask == 0) {
            return t;
        }
        if ((t.mask & UNIT_READ) != 0 && in.remaining() > 0) {
            t.unitGuid = in.getPackedGuid();
        }
        return t;
    }

    public void write(WowBuffer out) {
        out.putU32(mask);
        if ((mask & UNIT) != 0) {
            out.putPackedGuid(unitGuid);
        } else if ((mask & UNIT_MINIPET) != 0) {
            out.putU8(0);
        }
    }
}
