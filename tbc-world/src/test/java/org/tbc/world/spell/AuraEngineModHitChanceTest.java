package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-158 — SPELL_AURA_MOD_HIT_CHANCE (54). Heroic Presence 6562:
 * EffectBasePoints 0 + 1 = +1% weapon hit. CMaNGOS HandleModHitChance →
 * m_modWeaponHitChance[BASE/OFF/RANGED] (creature path; player UpdateWeaponHitChances EquippedItemClass −1).
 */
class AuraEngineModHitChanceTest {
    private static final SpellEngine.SpellInfo HEROIC_PRESENCE = new SpellEngine.SpellInfo(
            SpellEngine.HEROIC_PRESENCE, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_MOD_HIT_CHANCE, 0, 0, 1, 1, 0f);

    @Test
    void applyAuraWhenModHitChanceOnPlayerShouldRaiseWeaponHitChance() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_HIT_CHANCE));
        Player player = new Player();

        eng.apply(player, player, HEROIC_PRESENCE);

        assertTrue(player.hasAura(SpellEngine.HEROIC_PRESENCE));
        assertEquals(1f, player.weaponHitChance(Unit.BASE_ATTACK));
        assertEquals(1f, player.weaponHitChance(Unit.OFF_ATTACK));
        assertEquals(1f, player.weaponHitChance(Unit.RANGED_ATTACK));
    }

    @Test
    void unapplyWhenModHitChanceOnPlayerShouldRestoreWeaponHitChance() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, HEROIC_PRESENCE);
        eng.unapplyAura(player, SpellEngine.HEROIC_PRESENCE);
        assertEquals(0f, player.weaponHitChance(Unit.BASE_ATTACK));
        assertEquals(0f, player.weaponHitChance(Unit.OFF_ATTACK));
        assertEquals(0f, player.weaponHitChance(Unit.RANGED_ATTACK));
    }

    @Test
    void applyWhenAmountZeroShouldLeaveWeaponHitChance() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        eng.apply(player, player, new SpellEngine.SpellInfo(
                999054, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_HIT_CHANCE,
                0, 0, 0, 0, 0f));
        assertEquals(0f, player.weaponHitChance(Unit.BASE_ATTACK));
    }

    @Test
    void applyWhenCreatureShouldRaiseWeaponHitChance() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, HEROIC_PRESENCE);
        assertTrue(mob.hasAura(SpellEngine.HEROIC_PRESENCE));
        assertEquals(1f, mob.weaponHitChance(Unit.BASE_ATTACK));
        assertEquals(1f, mob.weaponHitChance(Unit.OFF_ATTACK));
        assertEquals(1f, mob.weaponHitChance(Unit.RANGED_ATTACK));
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, HEROIC_PRESENCE);
        new AuraEngine().unapply(null, HEROIC_PRESENCE);
    }
}
