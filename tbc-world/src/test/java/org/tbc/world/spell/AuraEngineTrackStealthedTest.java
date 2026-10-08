package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * TP-SL26-196 — SPELL_AURA_TRACK_STEALTHED (151).
 * CMaNGOS HandleAuraTrackStealthed → PLAYER_FIELD_BYTE_TRACK_STEALTHED on PLAYER_FIELD_BYTES.
 */
class AuraEngineTrackStealthedTest {
    private static final SpellEngine.SpellInfo TRACK = new SpellEngine.SpellInfo(
            999151, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_TRACK_STEALTHED, 0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenTrackStealthedShouldSetPlayerBytesFlag() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_TRACK_STEALTHED));
        Player player = new Player();
        eng.apply(player, player, TRACK);
        assertTrue(player.hasAura(999151));
        assertTrue(player.isTrackingStealthed());
        int bytes = player.getInt(UpdateFields.PLAYER_FIELD_BYTES);
        assertEquals(Player.PLAYER_FIELD_BYTE_TRACK_STEALTHED,
                bytes & Player.PLAYER_FIELD_BYTE_TRACK_STEALTHED);
    }

    @Test
    void unapplyWhenTrackStealthedShouldClearPlayerBytesFlag() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, TRACK);
        eng.auras().unapply(player, TRACK);
        assertFalse(player.isTrackingStealthed());
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_BYTES)
                & Player.PLAYER_FIELD_BYTE_TRACK_STEALTHED);
    }

    @Test
    void applyWhenCreatureShouldNoOpPlayerBytes() {
        new AuraEngine().apply(new Creature(), TRACK);
        new AuraEngine().unapply(new Creature(), TRACK);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, TRACK);
        new AuraEngine().unapply(null, TRACK);
    }
}
