package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpellCastTargetsTest {
    @Test
    void readAndWriteSides() {
        assertEquals(0, SpellCastTargets.read(new WowBuffer(new byte[2])).mask);
        WowBuffer self = new WowBuffer(8);
        self.putU32(0);
        assertEquals(0, SpellCastTargets.read(self).mask);

        WowBuffer unit = new WowBuffer(16);
        unit.putU32(SpellCastTargets.UNIT);
        unit.putPackedGuid(0xABC);
        SpellCastTargets t = SpellCastTargets.read(unit);
        assertEquals(SpellCastTargets.UNIT, t.mask);
        assertEquals(0xABCL, t.unitGuid);

        WowBuffer enemy = new WowBuffer(16);
        enemy.putU32(SpellCastTargets.UNIT_ENEMY);
        enemy.putPackedGuid(7);
        assertEquals(7L, SpellCastTargets.read(enemy).unitGuid);
        WowBuffer miniIn = new WowBuffer(16);
        miniIn.putU32(SpellCastTargets.UNIT_MINIPET);
        miniIn.putPackedGuid(3);
        assertEquals(3L, SpellCastTargets.read(miniIn).unitGuid);

        WowBuffer truncated = new WowBuffer(8);
        truncated.putU32(SpellCastTargets.UNIT);
        assertEquals(0L, SpellCastTargets.read(truncated).unitGuid);

        WowBuffer raid = new WowBuffer(8);
        raid.putU32(0x4);
        assertEquals(0x4, SpellCastTargets.read(raid).mask);
        assertEquals(0L, SpellCastTargets.read(new WowBuffer(raid.array())).unitGuid);

        WowBuffer out = new WowBuffer(16);
        t.write(out);
        SpellCastTargets minipet = new SpellCastTargets();
        minipet.mask = SpellCastTargets.UNIT_MINIPET;
        WowBuffer mp = new WowBuffer(8);
        minipet.write(mp);
        assertEquals(5, mp.size());
        SpellCastTargets none = new SpellCastTargets();
        WowBuffer z = new WowBuffer(8);
        none.write(z);
        assertEquals(4, z.size());
    }
}
