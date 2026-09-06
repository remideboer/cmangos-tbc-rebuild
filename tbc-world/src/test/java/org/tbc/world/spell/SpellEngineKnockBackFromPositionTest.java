package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-099 — SPELL_EFFECT_KNOCKBACK_FROM_POSITION (144). Spectral Blast 44866 misc 125 damage 75.
 * CMaNGOS KnockBackWithAngle away from dest (caster xyz stand-in).
 */
class SpellEngineKnockBackFromPositionTest {
    @Test
    void applyKnockBackFromPositionWhenTargetShouldEncodeAwayFromCaster() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_KNOCKBACK_FROM_POSITION));
        Player caster = new Player();
        caster.guid = 2;
        caster.relocate(0f, 0f, 0f, 0f);
        Player target = new Player();
        target.guid = 5;
        target.relocate(10f, 0f, 0f, 0f);
        SpellEngine.SpellInfo blast = new SpellEngine.SpellInfo(
                44866, SpellEngine.EFFECT_KNOCKBACK_FROM_POSITION, 0, 0, 0, 75, 75, 0f, 125);
        eng.apply(caster, target, blast);
        byte[] payload = SpellEngine.encodeMoveKnockBack(target, 1);
        WowBuffer b = new WowBuffer(payload);
        assertEquals(5, b.getPackedGuid());
        assertEquals(1, b.getU32());
        assertEquals(1f, b.getFloat(), 0.01f);
        assertEquals(0f, b.getFloat(), 0.01f);
        assertEquals(12.5f, b.getFloat(), 0.01f);
        assertEquals(-7.5f, b.getFloat(), 0.01f);
        assertEquals(Opcodes.SMSG_MOVE_KNOCK_BACK, 0x0EF);
    }

    @Test
    void applyKnockBackFromPositionWhenMissingCasterOrTargetShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player target = new Player();
        target.guid = 5;
        target.relocate(10f, 0f, 0f, 0f);
        SpellEngine.SpellInfo blast = new SpellEngine.SpellInfo(
                44866, SpellEngine.EFFECT_KNOCKBACK_FROM_POSITION, 0, 0, 0, 75, 75, 0f, 125);
        eng.apply(null, target, blast);
        assertEquals(0, SpellEngine.encodeMoveKnockBack(target, 1).length);
        eng.knockBackFromPosition(null, 0f, 0f, 12.5f, 7.5f);
    }
}
