package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-129 — SPELL_AURA_MOD_INCREASE_HEALTH (34). Pet Hardiness 6280 (EffectBasePoints+1 = +20).
 * CMaNGOS HandleAuraModIncreaseHealth default → HandleStatModifier(UNIT_MOD_HEALTH, TOTAL_VALUE).
 */
class AuraEngineModIncreaseHealthTest {
    private static final SpellEngine.SpellInfo PET_HARDINESS = new SpellEngine.SpellInfo(
            SpellEngine.PET_HARDINESS, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_INCREASE_HEALTH,
            0, 0, 20, 20, 0f);

    @Test
    void applyWhenPetHardinessShouldRaiseMaxHealth() {
        AuraEngine auras = new AuraEngine();
        assertTrue(auras.knownAura(AuraEngine.SPELL_AURA_MOD_INCREASE_HEALTH));
        Creature pet = new Creature();
        pet.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        pet.setInt(UpdateFields.UNIT_FIELD_HEALTH, 80);
        auras.apply(pet, PET_HARDINESS);
        assertEquals(120, pet.maxHealth());
        assertEquals(80, pet.health());
    }

    @Test
    void unapplyWhenPetHardinessShouldRestoreMaxHealthAndClampCurrent() {
        AuraEngine auras = new AuraEngine();
        Creature pet = new Creature();
        pet.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        pet.setInt(UpdateFields.UNIT_FIELD_HEALTH, 100);
        auras.apply(pet, PET_HARDINESS);
        pet.setInt(UpdateFields.UNIT_FIELD_HEALTH, 120);
        auras.unapply(pet, PET_HARDINESS);
        assertEquals(100, pet.maxHealth());
        assertEquals(100, pet.health());
    }

    @Test
    void applyWhenTargetMissingOrZeroAmountShouldNoOp() {
        AuraEngine auras = new AuraEngine();
        auras.apply(null, PET_HARDINESS);
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                999012, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_INCREASE_HEALTH,
                0, 0, 0, 0, 0f);
        auras.apply(p, zero);
        assertEquals(50, p.maxHealth());
    }

    @Test
    void unapplyAuraWhenCatalogedShouldRestoreMaxHealth() {
        SpellEngine eng = new SpellEngine();
        Creature pet = new Creature();
        pet.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        eng.apply(new Player(), pet, PET_HARDINESS);
        eng.unapplyAura(pet, SpellEngine.PET_HARDINESS);
        assertEquals(100, pet.maxHealth());
    }
}
