package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-135 — SPELL_AURA_MOD_STEALTH (16). Spell.dbc 1784 Stealth effect1.
 * CMaNGOS HandleModStealth → PLAYER_FIELD_BYTES2 byte 1 PLAYER_FIELD_BYTE2_STEALTH.
 */
class AuraEngineModStealthTest {
    private static final SpellEngine.SpellInfo STEALTH = new SpellEngine.SpellInfo(
            SpellEngine.SPELL_STEALTH, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STEALTH,
            0, 0, 0, 0, 0f, 30);

    @Test
    void applyAuraWhenModStealthOnPlayerShouldSetStealthByte() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_STEALTH));
        Player player = new Player();
        eng.apply(player, player, STEALTH);
        assertTrue(player.hasAura(SpellEngine.SPELL_STEALTH));
        int stealthMask = Player.PLAYER_FIELD_BYTE2_STEALTH << 8;
        assertEquals(stealthMask,
                player.getInt(UpdateFields.PLAYER_FIELD_BYTES2) & stealthMask);
    }

    @Test
    void unapplyWhenModStealthOnPlayerShouldClearStealthByte() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, STEALTH);
        eng.unapplyAura(player, SpellEngine.SPELL_STEALTH);
        int stealthMask = Player.PLAYER_FIELD_BYTE2_STEALTH << 8;
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_BYTES2) & stealthMask);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, STEALTH);
        new AuraEngine().unapply(null, STEALTH);
    }

    @Test
    void applyWhenCreatureShouldNoOpStealthByte() {
        SpellEngine eng = new SpellEngine();
        org.tbc.world.entity.Creature mob = new org.tbc.world.entity.Creature();
        eng.apply(new Player(), mob, STEALTH);
        assertTrue(mob.hasAura(SpellEngine.SPELL_STEALTH));
    }
}
