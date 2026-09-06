package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-049 — SPELL_EFFECT_PLAY_MUSIC (132). Ribbon Pole Music 46852 sound 12319. */
class SpellEnginePlayMusicTest {
    @Test
    void applyPlayMusicWhenPlayerTargetShouldEncodeSoundId() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_PLAY_MUSIC));
        Player target = new Player();
        SpellEngine.SpellInfo music = new SpellEngine.SpellInfo(
                46852, SpellEngine.EFFECT_PLAY_MUSIC, 0, 0, 0, 0, 0, 0f, 12319);
        eng.apply(new Player(), target, music);
        assertEquals(12319, target.lastPlayMusic());
        WowBuffer b = new WowBuffer(SpellEngine.encodePlayMusic(target.lastPlayMusic()));
        assertEquals(12319, b.getU32());
        assertEquals(Opcodes.SMSG_PLAY_MUSIC, 0x277);
    }

    @Test
    void applyPlayMusicWhenNonPlayerOrZeroSoundShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Creature npc = new Creature();
        SpellEngine.SpellInfo music = new SpellEngine.SpellInfo(
                46852, SpellEngine.EFFECT_PLAY_MUSIC, 0, 0, 0, 0, 0, 0f, 12319);
        eng.apply(new Player(), npc, music);
        Player p = new Player();
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                46852, SpellEngine.EFFECT_PLAY_MUSIC, 0, 0, 0, 0, 0, 0f, 0);
        eng.apply(new Player(), p, zero);
        assertEquals(0, p.lastPlayMusic());
    }
}
