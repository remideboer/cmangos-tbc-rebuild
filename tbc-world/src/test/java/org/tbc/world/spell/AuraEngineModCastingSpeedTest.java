package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-144 — SPELL_AURA_MOD_CASTING_SPEED_NOT_STACK (65). Spell.dbc 12472 Icy Veins (+20%).
 * CMaNGOS HandleModCastingSpeed → ApplyCastTimePercentMod → UNIT_MOD_CAST_SPEED.
 */
class AuraEngineModCastingSpeedTest {
    private static final float EPS = 1e-5f;
    private static final SpellEngine.SpellInfo ICY_VEINS = new SpellEngine.SpellInfo(
            SpellEngine.ICY_VEINS, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_CASTING_SPEED_NOT_STACK, 0, 0, 20, 20, 0f);

    @Test
    void applyAuraWhenCastingSpeedOnPlayerShouldReduceModCastSpeed() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_CASTING_SPEED_NOT_STACK));
        Player player = new Player();
        assertEquals(1.0f, player.getFloat(UpdateFields.UNIT_MOD_CAST_SPEED), EPS);
        eng.apply(player, player, ICY_VEINS);
        assertTrue(player.hasAura(SpellEngine.ICY_VEINS));
        // ApplyCastTimePercentMod(+20, true) → * 100/120
        assertEquals(100.0f / 120.0f, player.getFloat(UpdateFields.UNIT_MOD_CAST_SPEED), EPS);
    }

    @Test
    void unapplyWhenCastingSpeedOnPlayerShouldRestoreModCastSpeed() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, ICY_VEINS);
        eng.unapplyAura(player, SpellEngine.ICY_VEINS);
        assertEquals(1.0f, player.getFloat(UpdateFields.UNIT_MOD_CAST_SPEED), EPS);
    }

    @Test
    void applyWhenAmountZeroShouldLeaveModCastSpeed() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999065, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_CASTING_SPEED_NOT_STACK,
                0, 0, 0, 0, 0f));
        assertEquals(1.0f, player.getFloat(UpdateFields.UNIT_MOD_CAST_SPEED), EPS);
    }

    @Test
    void applyWhenNegativeAmountShouldIncreaseModCastSpeed() {
        AuraEngine auras = new AuraEngine();
        Player player = new Player();
        SpellEngine.SpellInfo slowCast = new SpellEngine.SpellInfo(
                999066, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_CASTING_SPEED_NOT_STACK,
                0, 0, -20, -20, 0f);
        auras.apply(player, slowCast);
        // ApplyCastTimePercentMod(-20, true) → ApplyPercentModFloatValue(+20, true) → * 1.2
        assertEquals(1.2f, player.getFloat(UpdateFields.UNIT_MOD_CAST_SPEED), EPS);
        auras.unapply(player, slowCast);
        assertEquals(1.0f, player.getFloat(UpdateFields.UNIT_MOD_CAST_SPEED), EPS);
    }

    @Test
    void applyPercentModWhenValIsMinus100ShouldClampToNearZero() {
        Player player = new Player();
        player.applyPercentModFloatValue(UpdateFields.UNIT_MOD_CAST_SPEED, -100.0f, true);
        // CMaNGOS clamps -100 → -99.9 so (100-99.9)/100 = 0.001
        assertEquals(0.001f, player.getFloat(UpdateFields.UNIT_MOD_CAST_SPEED), EPS);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, ICY_VEINS);
        new AuraEngine().unapply(null, ICY_VEINS);
    }
}
