package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-220 — SPELL_AURA_MOD_SPELL_HEALING_OF_ATTACK_POWER (238).
 * CMaNGOS SpellBaseHealingBonusDone adds attack power × amount / 100 when the school mask overlaps.
 */
class AuraEngineModSpellHealingOfAttackPowerTest {
    private static final SpellEngine.SpellInfo HEAL = new SpellEngine.SpellInfo(
            999238, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_SPELL_HEALING_OF_ATTACK_POWER, 0, 0, 25, 25, 0f, 1);

    @Test
    void applyAuraWhenAttackPowerShouldAddHealingDone() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_SPELL_HEALING_OF_ATTACK_POWER));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_ATTACK_POWER, 200);
        eng.apply(player, player, HEAL);
        assertEquals(50, player.getInt(UpdateFields.PLAYER_FIELD_MOD_HEALING_DONE_POS));
    }

    @Test
    void unapplyWhenAttackPowerHealingShouldRemoveTheBonus() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_ATTACK_POWER, 200);
        eng.apply(player, player, HEAL);
        eng.auras().unapply(player, HEAL);
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_HEALING_DONE_POS));
    }

    @Test
    void applyWhenCreatureMaskZeroOrTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, HEAL);
        Creature mob = new Creature();
        mob.setInt(UpdateFields.UNIT_FIELD_ATTACK_POWER, 200);
        new SpellEngine().apply(new Player(), mob, HEAL);
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_ATTACK_POWER, 200);
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999239, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_SPELL_HEALING_OF_ATTACK_POWER, 0, 0, 25, 25, 0f, 0));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_HEALING_DONE_POS));
    }
}
