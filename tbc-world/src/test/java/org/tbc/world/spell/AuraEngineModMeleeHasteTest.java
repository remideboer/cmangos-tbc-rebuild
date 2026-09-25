package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-146 — SPELL_AURA_MOD_MELEE_HASTE (138). Spell.dbc 13877 Blade Flurry (+20%).
 * CMaNGOS HandleModMeleeSpeedPct → ApplyAttackTimePercentMod(BASE+OFF).
 */
class AuraEngineModMeleeHasteTest {
    private static final SpellEngine.SpellInfo BLADE_FLURRY = new SpellEngine.SpellInfo(
            SpellEngine.BLADE_FLURRY, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_MELEE_HASTE, 0, 0, 20, 20, 0f);

    @Test
    void applyAuraWhenMeleeHasteOnPlayerShouldShortenMainAndOffhand() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_MELEE_HASTE));
        Player player = new Player();
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        eng.apply(player, player, BLADE_FLURRY);
        assertTrue(player.hasAura(SpellEngine.BLADE_FLURRY));
        // +20% haste → * 100/120
        assertEquals(Math.round(2000 * 100.0f / 120.0f),
                player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        assertEquals(Math.round(2000 * 100.0f / 120.0f),
                player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1));
    }

    @Test
    void unapplyWhenMeleeHasteOnPlayerShouldRestoreAttackTimes() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, BLADE_FLURRY);
        eng.unapplyAura(player, SpellEngine.BLADE_FLURRY);
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveAttackTimes() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999138, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_MELEE_HASTE,
                0, 0, 0, 0, 0f));
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
    }

    @Test
    void applyWhenNegativeAmountShouldLengthenAttackTimes() {
        AuraEngine auras = new AuraEngine();
        Player player = new Player();
        SpellEngine.SpellInfo slow = new SpellEngine.SpellInfo(
                999139, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_MELEE_HASTE,
                0, 0, -25, -25, 0f);
        auras.apply(player, slow);
        assertEquals(Math.round(2000 * 1.25f), player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        auras.unapply(player, slow);
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
    }

    @Test
    void applyWhenAttackTimeZeroShouldNoOpField() {
        AuraEngine auras = new AuraEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1, 0);
        auras.apply(player, BLADE_FLURRY);
        assertEquals(Math.round(2000 * 100.0f / 120.0f),
                player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME));
        assertEquals(0, player.getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, BLADE_FLURRY);
        new AuraEngine().unapply(null, BLADE_FLURRY);
    }
}
