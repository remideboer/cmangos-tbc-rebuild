package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-092 — SPELL_EFFECT_PLAY_SOUND (131). BOTM Jungle Madness Music 49963 sound 7294.
 * CMaNGOS PlayDirectSound to player target. SMSG_PLAY_SOUND 0x2D2 uint32.
 */
class SpellEnginePlaySoundTest {
    @Test
    void applyPlaySoundWhenPlayerTargetShouldEncodeSoundId() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_PLAY_SOUND));
        Player target = new Player();
        SpellEngine.SpellInfo jungle = new SpellEngine.SpellInfo(
                49963, SpellEngine.EFFECT_PLAY_SOUND, 0, 0, 0, 0, 0, 0f, 7294);
        eng.apply(new Player(), target, jungle);
        assertEquals(7294, target.lastPlaySound());
        WowBuffer b = new WowBuffer(SpellEngine.encodePlaySound(target.lastPlaySound()));
        assertEquals(7294, b.getU32());
        assertEquals(Opcodes.SMSG_PLAY_SOUND, 0x2D2);
    }

    @Test
    void applyPlaySoundWhenNonPlayerOrZeroSoundShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature npc = new Creature();
        SpellEngine.SpellInfo jungle = new SpellEngine.SpellInfo(
                49963, SpellEngine.EFFECT_PLAY_SOUND, 0, 0, 0, 0, 0, 0f, 7294);
        eng.apply(new Player(), npc, jungle);
        Player p = new Player();
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                49963, SpellEngine.EFFECT_PLAY_SOUND, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(new Player(), p, zero);
        assertEquals(0, p.lastPlaySound());
        eng.playSound(null, 7294);
        eng.playSound(p, 0);
        assertEquals(0, p.lastPlaySound());
    }
}
