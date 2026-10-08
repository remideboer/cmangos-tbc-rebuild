package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-186 — SPELL_AURA_MOD_OFFHAND_DAMAGE_PCT (122).
 * CMaNGOS HandleModOffhandDamagePercent → TOTAL_PCT on UNIT_MOD_DAMAGE_OFFHAND
 * (sheet: UNIT_FIELD_MIN/MAXOFFHANDDAMAGE).
 */
class AuraEngineModOffhandDamagePctTest {
    /** Synthetic +25% offhand damage (Dual Wield Specialization shape). */
    private static final SpellEngine.SpellInfo OFFHAND_PCT = new SpellEngine.SpellInfo(
            999122, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_OFFHAND_DAMAGE_PCT, 0, 0, 25, 25, 0f);

    @Test
    void applyAuraWhenModOffhandDamagePctShouldRaiseOffhandDamage() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_OFFHAND_DAMAGE_PCT));
        Player player = new Player();
        player.setFloat(UpdateFields.UNIT_FIELD_MINOFFHANDDAMAGE, 40f);
        player.setFloat(UpdateFields.UNIT_FIELD_MAXOFFHANDDAMAGE, 80f);

        eng.apply(player, player, OFFHAND_PCT);

        assertTrue(player.hasAura(999122));
        assertEquals(50f, player.getFloat(UpdateFields.UNIT_FIELD_MINOFFHANDDAMAGE), 1e-4f);
        assertEquals(100f, player.getFloat(UpdateFields.UNIT_FIELD_MAXOFFHANDDAMAGE), 1e-4f);
    }

    @Test
    void unapplyWhenModOffhandDamagePctShouldRestoreOffhandDamage() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.UNIT_FIELD_MINOFFHANDDAMAGE, 40f);
        player.setFloat(UpdateFields.UNIT_FIELD_MAXOFFHANDDAMAGE, 80f);
        eng.apply(player, player, OFFHAND_PCT);
        eng.auras().unapply(player, OFFHAND_PCT);
        assertEquals(40f, player.getFloat(UpdateFields.UNIT_FIELD_MINOFFHANDDAMAGE), 1e-4f);
        assertEquals(80f, player.getFloat(UpdateFields.UNIT_FIELD_MAXOFFHANDDAMAGE), 1e-4f);
    }

    @Test
    void applyWhenAmountZeroShouldLeaveOffhandDamage() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setFloat(UpdateFields.UNIT_FIELD_MINOFFHANDDAMAGE, 40f);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999121, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_OFFHAND_DAMAGE_PCT,
                0, 0, 0, 0, 0f));
        assertEquals(40f, player.getFloat(UpdateFields.UNIT_FIELD_MINOFFHANDDAMAGE), 1e-4f);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, OFFHAND_PCT);
        new AuraEngine().unapply(null, OFFHAND_PCT);
    }
}
