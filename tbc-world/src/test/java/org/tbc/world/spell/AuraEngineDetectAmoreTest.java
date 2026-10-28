package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-209 — SPELL_AURA_DETECT_AMORE (170).
 * CMaNGOS HandleDetectAmore sets PLAYER_FIELD_BYTES2 byte 1 from the misc flag bit.
 */
class AuraEngineDetectAmoreTest {
    private static final SpellEngine.SpellInfo AMORE = new SpellEngine.SpellInfo(
            999170, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_DETECT_AMORE, 0, 0, 0, 0, 0f, 0);

    @Test
    void applyAuraWhenDetectAmoreShouldSetBytes2Flag() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_DETECT_AMORE));
        Player player = new Player();
        player.setStealthByte(true);
        eng.apply(player, player, AMORE);
        int bytes = player.getInt(UpdateFields.PLAYER_FIELD_BYTES2);
        assertEquals((1 << 8) | (Player.PLAYER_FIELD_BYTE2_STEALTH << 8), bytes);
    }

    @Test
    void unapplyWhenDetectAmoreShouldClearOnlyThatFlag() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setStealthByte(true);
        eng.apply(player, player, AMORE);
        eng.auras().unapply(player, AMORE);
        assertEquals(Player.PLAYER_FIELD_BYTE2_STEALTH << 8, player.getInt(UpdateFields.PLAYER_FIELD_BYTES2));
    }

    @Test
    void applyWhenMiscOutOfRangeOrCreatureShouldNoOp() {
        new AuraEngine().apply(null, AMORE);
        new SpellEngine().apply(new Player(), new Creature(), AMORE);
        Player player = new Player();
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999171, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_DETECT_AMORE,
                0, 0, 0, 0, 0f, 8));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_BYTES2));
    }
}
