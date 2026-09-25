package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-143 — SPELL_AURA_TRACK_CREATURES (44). Spell.dbc 1494 Track Beasts (misc 1).
 * CMaNGOS HandleAuraTrackCreatures → PLAYER_TRACK_CREATURES bit 1&lt;&lt;(misc-1).
 */
class AuraEngineTrackCreaturesTest {
    private static final int BEAST_BIT = 1 << (1 - 1);
    private static final SpellEngine.SpellInfo TRACK_BEASTS = new SpellEngine.SpellInfo(
            SpellEngine.TRACK_BEASTS, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_TRACK_CREATURES,
            0, 0, 0, 0, 0f, 1);

    @Test
    void applyAuraWhenTrackCreaturesOnPlayerShouldSetTrackBit() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_TRACK_CREATURES));
        Player player = new Player();
        eng.apply(player, player, TRACK_BEASTS);
        assertTrue(player.hasAura(SpellEngine.TRACK_BEASTS));
        assertEquals(BEAST_BIT, player.getInt(UpdateFields.PLAYER_TRACK_CREATURES) & BEAST_BIT);
    }

    @Test
    void unapplyWhenTrackCreaturesOnPlayerShouldClearTrackBit() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, TRACK_BEASTS);
        eng.unapplyAura(player, SpellEngine.TRACK_BEASTS);
        assertEquals(0, player.getInt(UpdateFields.PLAYER_TRACK_CREATURES) & BEAST_BIT);
    }

    @Test
    void applyWhenMiscOutOfRangeShouldLeaveTrackClear() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999044, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_TRACK_CREATURES,
                0, 0, 0, 0, 0f, 0));
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999047, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_TRACK_CREATURES,
                0, 0, 0, 0, 0f, 33));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_TRACK_CREATURES));
    }

    @Test
    void applyWhenCreatureShouldNoOpTrackField() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, TRACK_BEASTS);
        assertTrue(mob.hasAura(SpellEngine.TRACK_BEASTS));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, TRACK_BEASTS);
        new AuraEngine().unapply(null, TRACK_BEASTS);
    }
}
