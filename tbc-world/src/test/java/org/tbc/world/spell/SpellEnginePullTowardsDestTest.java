package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-059 — SPELL_EFFECT_PULL_TOWARDS_DEST (145). Black Hole Effect 46230 misc 150. */
class SpellEnginePullTowardsDestTest {
    @Test
    void applyPullTowardsDestWhenApartShouldKnockTargetTowardDest() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_PULL_TOWARDS_DEST));
        Player caster = new Player();
        caster.guid = 2;
        caster.relocate(10f, 0f, 8f, 0f);
        Player target = new Player();
        target.guid = 5;
        target.relocate(0f, 0f, 0f, 0f);
        SpellEngine.SpellInfo hole = new SpellEngine.SpellInfo(
                46230, SpellEngine.EFFECT_PULL_TOWARDS_DEST, 0, 0, 0, 0, 0, 0f, 150);
        eng.apply(caster, target, hole);
        byte[] payload = SpellEngine.encodeMoveKnockBack(target, 1);
        WowBuffer b = new WowBuffer(payload);
        assertEquals(5, b.getPackedGuid());
        assertEquals(1, b.getU32());
        assertEquals(1f, b.getFloat(), 0.01f);
        assertEquals(0f, b.getFloat(), 0.01f);
        assertEquals(15f, b.getFloat(), 0.01f);
        float time = 10f / 15f;
        float speedZ = (8f + 0.5f * time * time * SpellEngine.MOVEMENT_GRAVITY) / time;
        assertEquals(-speedZ, b.getFloat(), 0.01f);
        assertEquals(Opcodes.SMSG_MOVE_KNOCK_BACK, 0x0EF);
    }

    @Test
    void applyPullTowardsDestWhenTooCloseOrNullCasterShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player caster = new Player();
        caster.relocate(0f, 0f, 8f, 0f);
        Player target = new Player();
        target.guid = 5;
        target.relocate(0.05f, 0f, 0f, 0f);
        SpellEngine.SpellInfo hole = new SpellEngine.SpellInfo(
                46230, SpellEngine.EFFECT_PULL_TOWARDS_DEST, 0, 0, 0, 0, 0, 0f, 150);
        eng.apply(caster, target, hole);
        assertEquals(0, SpellEngine.encodeMoveKnockBack(target, 1).length);
        target.relocate(10f, 0f, 0f, 0f);
        eng.apply(null, target, hole);
        assertEquals(0, SpellEngine.encodeMoveKnockBack(target, 1).length);
    }
}
