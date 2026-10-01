package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-135 — SPELL_AURA_MOD_STEALTH (16). Spell.dbc 1784 Stealth effect1.
 * CMaNGOS HandleModStealth → PLAYER_FIELD_BYTES2 byte 1 PLAYER_FIELD_BYTE2_STEALTH.
 * TP-SL26-173 — VISIBILITY_GROUP_STEALTH + UNIT_VIS_FLAG_CREEP.
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

    /**
     * TP-SL26-173 — HandleModStealth sets GROUP_STEALTH and UNIT_VIS_FLAG_CREEP on BYTES_1.
     */
    @Test
    void applyAuraWhenModStealthShouldSetVisibilityGroupAndCreep() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.guid = 1;
        eng.apply(player, player, STEALTH);
        assertEquals(Unit.Visibility.GROUP_STEALTH, player.visibility());
        assertTrue(player.hasVisFlagCreep());
        Player observer = new Player();
        observer.guid = 2;
        assertFalse(player.isVisibleTo(observer));
    }

    @Test
    void unapplyWhenModStealthOnPlayerShouldClearStealthByte() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, STEALTH);
        eng.unapplyAura(player, SpellEngine.SPELL_STEALTH);
        int stealthMask = Player.PLAYER_FIELD_BYTE2_STEALTH << 8;
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_BYTES2) & stealthMask);
        assertEquals(Unit.Visibility.ON, player.visibility());
        assertFalse(player.hasVisFlagCreep());
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, STEALTH);
        new AuraEngine().unapply(null, STEALTH);
    }

    @Test
    void applyWhenCreatureShouldSetVisibilityWithoutStealthByte() {
        SpellEngine eng = new SpellEngine();
        org.tbc.world.entity.Creature mob = new org.tbc.world.entity.Creature();
        eng.apply(new Player(), mob, STEALTH);
        assertTrue(mob.hasAura(SpellEngine.SPELL_STEALTH));
        assertEquals(Unit.Visibility.GROUP_STEALTH, mob.visibility());
        assertTrue(mob.hasVisFlagCreep());
    }
}
