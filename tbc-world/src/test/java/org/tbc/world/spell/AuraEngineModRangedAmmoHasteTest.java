package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-214 — SPELL_AURA_MOD_RANGED_AMMO_HASTE (141).
 * CMaNGOS HandleRangedAmmoHaste keeps only the highest positive amount on ranged attack time.
 */
class AuraEngineModRangedAmmoHasteTest {
    private static SpellEngine.SpellInfo haste(int id, int amount) {
        return new SpellEngine.SpellInfo(
                id, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_MOD_RANGED_AMMO_HASTE, 0, 0, amount, amount, 0f, 0, -1);
    }

    @Test
    void applyAuraWhenAmmoHasteShouldShortenRangedAttackTime() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_MOD_RANGED_AMMO_HASTE));
        Player player = new Player();
        eng.apply(player, player, haste(999141, 20));
        assertEquals(Math.round(2000 * 100f / 120f), player.getInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME));
    }

    @Test
    void applyWhenLowerAmountShouldKeepTheHighestUntilItDrops() {
        SpellEngine eng = new SpellEngine();
        Player player = new Player();
        SpellEngine.SpellInfo high = haste(999141, 20);
        SpellEngine.SpellInfo low = haste(999142, 10);
        eng.apply(player, player, high);
        eng.apply(player, player, low);
        assertEquals(Math.round(2000 * 100f / 120f), player.getInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME));
        eng.auras().unapply(player, high);
        assertEquals(Math.round(2000 * 100f / 110f), player.getInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME));
        eng.auras().unapply(player, low);
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME));
    }

    @Test
    void applyWhenCreatureItemClassOrTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, haste(999141, 20));
        Creature mob = new Creature();
        mob.setInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME, 2000);
        new SpellEngine().apply(new Player(), mob, haste(999141, 20));
        assertEquals(2000, mob.getInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME));
        Player player = new Player();
        new AuraEngine().apply(player, new SpellEngine.SpellInfo(
                999143, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_RANGED_AMMO_HASTE,
                0, 0, 20, 20, 0f));
        assertEquals(2000, player.getInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME));
    }
}
