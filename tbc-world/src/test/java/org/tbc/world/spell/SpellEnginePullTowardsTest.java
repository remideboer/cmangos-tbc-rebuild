package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-050 — SPELL_EFFECT_PULL_TOWARDS (124). Magnetic Pull 28337 misc 300. */
class SpellEnginePullTowardsTest {
    @Test
    void applyPullTowardsWhenApartShouldKnockTargetTowardCaster() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_PULL_TOWARDS));
        Player caster = new Player();
        caster.guid = 2;
        caster.relocate(0f, 0f, 0f, 0f);
        Player target = new Player();
        target.guid = 5;
        target.relocate(10f, 0f, 0f, 0f);
        SpellEngine.SpellInfo pull = new SpellEngine.SpellInfo(
                28337, SpellEngine.EFFECT_PULL_TOWARDS, 0, 0, 0, 0, 0, 0f, 300);
        eng.apply(caster, target, pull);
        byte[] payload = SpellEngine.encodeMoveKnockBack(target, 1);
        WowBuffer b = new WowBuffer(payload);
        assertEquals(5, b.getPackedGuid());
        assertEquals(1, b.getU32());
        assertEquals(-1f, b.getFloat(), 0.01f);
        assertEquals(0f, b.getFloat(), 0.01f);
        assertEquals(30f, b.getFloat(), 0.01f);
        float time = 10f / 30f;
        float speedZ = 0.5f * time * SpellEngine.MOVEMENT_GRAVITY;
        assertEquals(-speedZ, b.getFloat(), 0.01f);
        assertEquals(Opcodes.SMSG_MOVE_KNOCK_BACK, 0x0EF);
    }

    @Test
    void applyPullTowardsWhenTooCloseOrNullCasterShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        caster.relocate(0f, 0f, 0f, 0f);
        Player target = new Player();
        target.guid = 5;
        target.relocate(0.05f, 0f, 0f, 0f);
        SpellEngine.SpellInfo pull = new SpellEngine.SpellInfo(
                28337, SpellEngine.EFFECT_PULL_TOWARDS, 0, 0, 0, 0, 0, 0f, 300);
        eng.apply(caster, target, pull);
        assertEquals(0, SpellEngine.encodeMoveKnockBack(target, 1).length);
        target.relocate(10f, 0f, 0f, 0f);
        eng.apply(null, target, pull);
        assertEquals(0, SpellEngine.encodeMoveKnockBack(target, 1).length);
    }
}
