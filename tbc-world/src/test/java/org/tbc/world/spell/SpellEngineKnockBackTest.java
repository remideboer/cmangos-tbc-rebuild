package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-033 — SPELL_EFFECT_KNOCK_BACK (98). Knockback 10689. movement.md SMSG_MOVE_KNOCK_BACK. */
class SpellEngineKnockBackTest {
    @Test
    void applyKnockBackWhenLivingShouldEncodeMoveKnockBackSpeeds() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_KNOCK_BACK));
        Player caster = new Player();
        caster.guid = 2;
        caster.relocate(0f, 0f, 0f, 0f);
        Player target = new Player();
        target.guid = 5;
        target.relocate(10f, 0f, 0f, 0f);
        SpellEngine.SpellInfo kb = new SpellEngine.SpellInfo(
                10689, SpellEngine.EFFECT_KNOCK_BACK, 0, 0, 0, 100, 100, 0f, 100);
        eng.apply(caster, target, kb);
        byte[] payload = SpellEngine.encodeMoveKnockBack(target, 1);
        WowBuffer b = new WowBuffer(payload);
        assertEquals(5, b.getPackedGuid());
        assertEquals(1, b.getU32());
        assertEquals(1f, b.getFloat(), 0.01f);
        assertEquals(0f, b.getFloat(), 0.01f);
        assertEquals(10f, b.getFloat(), 0.01f);
        assertEquals(-10f, b.getFloat(), 0.01f);
        assertEquals(Opcodes.SMSG_MOVE_KNOCK_BACK, 0x0EF);
    }

    @Test
    void applyKnockBackWhenRootedShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        caster.guid = 2;
        caster.relocate(0f, 0f, 0f, 0f);
        Player target = new Player();
        target.guid = 5;
        target.relocate(10f, 0f, 0f, 0f);
        target.setRooted(true);
        SpellEngine.SpellInfo kb = new SpellEngine.SpellInfo(
                10689, SpellEngine.EFFECT_KNOCK_BACK, 0, 0, 0, 100, 100, 0f, 100);
        eng.apply(caster, target, kb);
        assertEquals(0, SpellEngine.encodeMoveKnockBack(target, 1).length);
    }
}
