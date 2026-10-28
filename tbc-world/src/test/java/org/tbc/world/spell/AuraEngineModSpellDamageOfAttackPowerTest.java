package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-219 — SPELL_AURA_MOD_SPELL_DAMAGE_OF_ATTACK_POWER (237).
 * CMaNGOS SpellBaseDamageBonusDone adds attack power × amount / 100 per school bit.
 */
class AuraEngineModSpellDamageOfAttackPowerTest {
    private static final int HOLY = 1;
    private static final int FIRE = 2;

    private static SpellEngine.SpellInfo bonus(int id, int amount) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_SPELL_DAMAGE_OF_ATTACK_POWER, 0, 0, amount, amount, 0f,
                (1 << HOLY) | (1 << FIRE));
    }

    @Test
    void applyAuraWhenAttackPowerShouldAddSchoolSpellDamage() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_SPELL_DAMAGE_OF_ATTACK_POWER));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_ATTACK_POWER, 200);
        eng.apply(player, player, bonus(999237, 10));
        assertEquals(20, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + HOLY));
        assertEquals(20, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + FIRE));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + 3));
    }

    @Test
    void unapplyWhenAttackPowerBonusShouldRemoveTheSameAmount() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_ATTACK_POWER, 200);
        SpellEngine.SpellInfo spell = bonus(999237, 10);
        eng.apply(player, player, spell);
        eng.auras().unapply(player, spell);
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + HOLY));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + FIRE));
    }

    @Test
    void applyWhenModsAndMultiplierShouldTruncateTheBonus() {
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_ATTACK_POWER_MODS, 50);
        player.setFloat(UpdateFields.UNIT_FIELD_ATTACK_POWER_MULTIPLIER, 0.5f);
        new SpellEngine().apply(player, player, bonus(999238, 10));
        assertEquals(7, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + HOLY));
    }

    @Test
    void applyWhenCreatureMaskZeroOrTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, bonus(999237, 10));
        Creature mob = new Creature();
        mob.setInt(UpdateFields.UNIT_FIELD_ATTACK_POWER, 200);
        new SpellEngine().apply(new Player(), mob, bonus(999237, 10));
        Player player = new Player();
        player.setInt(UpdateFields.UNIT_FIELD_ATTACK_POWER, 200);
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999239, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_SPELL_DAMAGE_OF_ATTACK_POWER, 0, 0, 10, 10, 0f, 0));
        assertEquals(0, player.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + HOLY));
    }
}
