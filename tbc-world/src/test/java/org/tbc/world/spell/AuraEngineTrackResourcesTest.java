package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-142 — SPELL_AURA_TRACK_RESOURCES (45). Spell.dbc 2580 Find Minerals (misc 3).
 * CMaNGOS HandleAuraTrackResources → PLAYER_TRACK_RESOURCES bit 1&lt;&lt;(misc-1).
 */
class AuraEngineTrackResourcesTest {
    private static final int MINERAL_BIT = 1 << (3 - 1);
    private static final SpellEngine.SpellInfo FIND_MINERALS = new SpellEngine.SpellInfo(
            SpellEngine.FIND_MINERALS, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_TRACK_RESOURCES,
            0, 0, 0, 0, 0f, 3);

    @Test
    void applyAuraWhenTrackResourcesOnPlayerShouldSetTrackBit() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_TRACK_RESOURCES));
        Player player = new Player();
        eng.apply(player, player, FIND_MINERALS);
        assertTrue(player.hasAura(SpellEngine.FIND_MINERALS));
        assertEquals(MINERAL_BIT, player.getInt(UpdateFields.PLAYER_TRACK_RESOURCES) & MINERAL_BIT);
    }

    @Test
    void unapplyWhenTrackResourcesOnPlayerShouldClearTrackBit() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, FIND_MINERALS);
        eng.unapplyAura(player, SpellEngine.FIND_MINERALS);
        assertEquals(0, player.getInt(UpdateFields.PLAYER_TRACK_RESOURCES) & MINERAL_BIT);
    }

    @Test
    void applyWhenMiscOutOfRangeShouldLeaveTrackClear() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        SpellEngine.SpellInfo badLow = new SpellEngine.SpellInfo(
                999045, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_TRACK_RESOURCES,
                0, 0, 0, 0, 0f, 0);
        eng.apply(player, player, badLow);
        assertEquals(0, player.getInt(UpdateFields.PLAYER_TRACK_RESOURCES));
        SpellEngine.SpellInfo badHigh = new SpellEngine.SpellInfo(
                999046, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_TRACK_RESOURCES,
                0, 0, 0, 0, 0f, 33);
        eng.apply(player, player, badHigh);
        assertEquals(0, player.getInt(UpdateFields.PLAYER_TRACK_RESOURCES));
    }

    @Test
    void applyWhenCreatureShouldNoOpTrackField() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, FIND_MINERALS);
        assertTrue(mob.hasAura(SpellEngine.FIND_MINERALS));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, FIND_MINERALS);
        new AuraEngine().unapply(null, FIND_MINERALS);
    }
}
