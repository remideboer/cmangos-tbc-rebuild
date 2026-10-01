package org.tbc.world.spell;

import org.junit.jupiter.api.Test;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-168 — SPELL_AURA_MOD_POWER_COST_SCHOOL_PCT (72). Nether Portal - Serenity 30422
 * effect1: EffectBasePoints −2 → amount −1 (%), frost mask. CMaNGOS HandleModPowerCostPCT →
 * UNIT_FIELD_POWER_COST_MULTIPLIER += amount/100.
 */
class AuraEngineModPowerCostSchoolPctTest {
    /** Frost school bit 4 → mask 16. Amount −1 → float −0.01. */
    private static final SpellEngine.SpellInfo SERENITY_FROST = new SpellEngine.SpellInfo(
            SpellEngine.NETHER_PORTAL_SERENITY, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_POWER_COST_SCHOOL_PCT, 0, 0, -1, -1, 0f, 16);

    @Test
    void applyAuraWhenModPowerCostSchoolPctOnPlayerShouldLowerFrostMultiplier() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_POWER_COST_SCHOOL_PCT));
        Player player = new Player();

        eng.apply(player, player, SERENITY_FROST);

        assertTrue(player.hasAura(SpellEngine.NETHER_PORTAL_SERENITY));
        assertEquals(-0.01f, player.getFloat(UpdateFields.UNIT_FIELD_POWER_COST_MULTIPLIER + 4), 1e-6f);
        assertEquals(0f, player.getFloat(UpdateFields.UNIT_FIELD_POWER_COST_MULTIPLIER), 1e-6f);
    }

    @Test
    void unapplyWhenModPowerCostSchoolPctShouldRestoreMultiplier() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, SERENITY_FROST);
        eng.unapplyAura(player, SpellEngine.NETHER_PORTAL_SERENITY);
        assertEquals(0f, player.getFloat(UpdateFields.UNIT_FIELD_POWER_COST_MULTIPLIER + 4), 1e-6f);
    }

    @Test
    void applyWhenAmountZeroShouldLeaveMultipliers() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999072, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_POWER_COST_SCHOOL_PCT,
                0, 0, 0, 0, 0f, 16));
        assertEquals(0f, player.getFloat(UpdateFields.UNIT_FIELD_POWER_COST_MULTIPLIER + 4), 1e-6f);
    }

    @Test
    void applyWhenMaskZeroShouldLeaveMultipliers() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999075, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_POWER_COST_SCHOOL_PCT,
                0, 0, -1, -1, 0f, 0));
        assertEquals(0f, player.getFloat(UpdateFields.UNIT_FIELD_POWER_COST_MULTIPLIER + 4), 1e-6f);
    }

    @Test
    void applyWhenCreatureShouldRaiseMatchingSchoolMultiplier() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, SERENITY_FROST);
        assertTrue(mob.hasAura(SpellEngine.NETHER_PORTAL_SERENITY));
        assertEquals(-0.01f, mob.getFloat(UpdateFields.UNIT_FIELD_POWER_COST_MULTIPLIER + 4), 1e-6f);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, SERENITY_FROST);
        new AuraEngine().unapply(null, SERENITY_FROST);
    }
}
