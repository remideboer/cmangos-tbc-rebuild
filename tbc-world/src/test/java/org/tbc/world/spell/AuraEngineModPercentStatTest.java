package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-153 — SPELL_AURA_MOD_PERCENT_STAT (80). Spell.dbc 23735 Sayge's +10% strength.
 * CMaNGOS HandleModPercentStat → BASE_PCT on matching UNIT_FIELD_STAT.
 */
class AuraEngineModPercentStatTest {
    private static final SpellEngine.SpellInfo SAYGE = new SpellEngine.SpellInfo(
            SpellEngine.SAYGES_STRENGTH, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_PERCENT_STAT, 0, 0, 10, 10, 0f, 0);

    @Test
    void applyAuraWhenPercentStatOnPlayerShouldRaiseStrength() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_PERCENT_STAT));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_STAT0, 100);
        eng.apply(player, player, SAYGE);
        assertTrue(player.hasAura(SpellEngine.SAYGES_STRENGTH));
        assertEquals(110, player.getInt(UpdateFields.UNIT_FIELD_STAT0));
    }

    @Test
    void unapplyWhenPercentStatOnPlayerShouldRestoreStrength() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_STAT0, 100);
        eng.apply(player, player, SAYGE);
        eng.unapplyAura(player, SpellEngine.SAYGES_STRENGTH);
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_STAT0));
    }

    @Test
    void applyWhenMiscOutOfRangeShouldLeaveStats() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_STAT0, 100);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999080, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_PERCENT_STAT,
                0, 0, 10, 10, 0f, 5));
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_STAT0));
    }

    @Test
    void applyWhenCreatureShouldNoOpStats() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        mob.setInt(UpdateFields.UNIT_FIELD_STAT0, 100);
        eng.apply(new Player(), mob, SAYGE);
        assertEquals(100, mob.getInt(UpdateFields.UNIT_FIELD_STAT0));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveStats() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_STAT0, 100);
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999081, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_PERCENT_STAT,
                0, 0, 0, 0, 0f, 0));
        assertEquals(100, player.getInt(UpdateFields.UNIT_FIELD_STAT0));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, SAYGE);
        new AuraEngine().unapply(null, SAYGE);
    }
}
