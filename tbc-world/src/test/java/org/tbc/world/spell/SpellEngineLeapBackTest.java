package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-051 — SPELL_EFFECT_LEAP_BACK (138). Negative Jump 40622 misc 100. */
class SpellEngineLeapBackTest {
    @Test
    void applyLeapBackWhenSelfShouldKnockCasterBackward() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_LEAP_BACK));
        Player caster = new Player();
        caster.guid = 5;
        caster.relocate(0f, 0f, 0f, 0f);
        SpellEngine.SpellInfo jump = new SpellEngine.SpellInfo(
                40622, SpellEngine.EFFECT_LEAP_BACK, 0, 0, 0, -350, -350, 0f, 100);
        eng.apply(caster, caster, jump);
        byte[] payload = SpellEngine.encodeMoveKnockBack(caster, 1);
        WowBuffer b = new WowBuffer(payload);
        assertEquals(5, b.getPackedGuid());
        assertEquals(1, b.getU32());
        assertEquals(-1f, b.getFloat(), 0.01f);
        assertEquals(0f, b.getFloat(), 0.01f);
        assertEquals(10f, b.getFloat(), 0.01f);
        assertEquals(35f, b.getFloat(), 0.01f);
        assertEquals(Opcodes.SMSG_MOVE_KNOCK_BACK, 0x0EF);
    }

    @Test
    void applyLeapBackWhenTaxiFlyingShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        caster.guid = 5;
        caster.setInt(UpdateFields.UNIT_FIELD_FLAGS, Unit.UNIT_FLAG_TAXI_FLIGHT);
        SpellEngine.SpellInfo jump = new SpellEngine.SpellInfo(
                40622, SpellEngine.EFFECT_LEAP_BACK, 0, 0, 0, -350, -350, 0f, 100);
        eng.apply(caster, caster, jump);
        assertEquals(0, SpellEngine.encodeMoveKnockBack(caster, 1).length);
    }
}
