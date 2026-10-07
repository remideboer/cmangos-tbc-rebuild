package org.tbc.world.spell.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.tbc.world.spell.SpellEngine;

/** Effect-id → handler table that replaces the if-chain in SpellEngine.apply (plan cycle 5.1). */
class EffectTableTest {

    @Test
    void registerWhenSameEffectTwiceShouldThrow() {
        EffectTable t = new EffectTable();
        EffectHandler h = (e, c, target, sp, now) -> 0;
        t.register(SpellEngine.EFFECT_CHARGE, h);

        assertThrows(IllegalStateException.class, () -> t.register(SpellEngine.EFFECT_CHARGE, h));
    }

    @Test
    void getWhenUnregisteredEffectShouldReturnNull() {
        assertNull(new EffectTable().get(SpellEngine.EFFECT_SCHOOL_DAMAGE));
    }

    @Test
    void standardWhenBuiltShouldRegisterMovementFamily() {
        Set<Integer> movement = Set.of(
                SpellEngine.EFFECT_TELEPORT_UNITS_FACE_CASTER, SpellEngine.EFFECT_TELEPORT_GRAVEYARD,
                SpellEngine.EFFECT_CHARGE, SpellEngine.EFFECT_CHARGE_DEST,
                SpellEngine.EFFECT_LEAP, SpellEngine.EFFECT_LEAP_BACK,
                SpellEngine.EFFECT_KNOCK_BACK, SpellEngine.EFFECT_KNOCKBACK_FROM_POSITION,
                SpellEngine.EFFECT_PULL_TOWARDS, SpellEngine.EFFECT_PULL_TOWARDS_DEST,
                SpellEngine.EFFECT_STUCK, SpellEngine.EFFECT_SUMMON_PLAYER,
                SpellEngine.EFFECT_BIND, SpellEngine.EFFECT_SEND_TAXI);

        assertTrue(EffectTable.standard().effects().containsAll(movement));
    }

    @Test
    void standardWhenBuiltShouldRegisterItemFamily() {
        Set<Integer> item = Set.of(
                SpellEngine.EFFECT_OPEN_LOCK, SpellEngine.EFFECT_OPEN_LOCK_ITEM,
                SpellEngine.EFFECT_SUMMON_CHANGE_ITEM, SpellEngine.EFFECT_ENCHANT_HELD_ITEM,
                SpellEngine.EFFECT_ENCHANT_ITEM, SpellEngine.EFFECT_ENCHANT_ITEM_TEMPORARY,
                SpellEngine.EFFECT_PROSPECTING, SpellEngine.EFFECT_DISENCHANT,
                SpellEngine.EFFECT_DURABILITY_DAMAGE, SpellEngine.EFFECT_DURABILITY_DAMAGE_PCT,
                SpellEngine.EFFECT_PICKPOCKET, SpellEngine.EFFECT_SKINNING, SpellEngine.EFFECT_SKIN_PLAYER_CORPSE);

        assertTrue(EffectTable.standard().effects().containsAll(item));
    }

    @Test
    void standardWhenBuiltShouldRegisterPetFamily() {
        Set<Integer> pet = Set.of(
                SpellEngine.EFFECT_SUMMON_DEAD_PET, SpellEngine.EFFECT_CREATE_PET, SpellEngine.EFFECT_TAME_CREATURE,
                SpellEngine.EFFECT_SUMMON_PET, SpellEngine.EFFECT_FEED_PET, SpellEngine.EFFECT_DISMISS_PET,
                SpellEngine.EFFECT_LEARN_PET_SPELL);

        assertTrue(EffectTable.standard().effects().containsAll(pet));
    }

    @Test
    void standardWhenBuiltShouldRegisterSummonFamily() {
        Set<Integer> summon = Set.of(
                SpellEngine.EFFECT_ACTIVATE_OBJECT, SpellEngine.EFFECT_DESTROY_ALL_TOTEMS, SpellEngine.EFFECT_SPAWN,
                SpellEngine.EFFECT_SUMMON_OBJECT_SLOT1, SpellEngine.EFFECT_SUMMON_OBJECT_SLOT2,
                SpellEngine.EFFECT_SUMMON_OBJECT_WILD, SpellEngine.EFFECT_TRANS_DOOR, SpellEngine.EFFECT_SUMMON,
                SpellEngine.EFFECT_PERSISTENT_AREA_AURA, SpellEngine.EFFECT_SEND_EVENT);

        assertTrue(EffectTable.standard().effects().containsAll(summon));
    }

    @Test
    void standardWhenBuiltShouldRegisterPlayerFamily() {
        Set<Integer> player = Set.of(
                SpellEngine.EFFECT_QUEST_COMPLETE, SpellEngine.EFFECT_QUEST_FAIL, SpellEngine.EFFECT_KILL_CREDIT_GROUP,
                SpellEngine.EFFECT_DUAL_WIELD, SpellEngine.EFFECT_SKILL_STEP, SpellEngine.EFFECT_PARRY,
                SpellEngine.EFFECT_BLOCK, SpellEngine.EFFECT_PROFICIENCY, SpellEngine.EFFECT_PLAY_MUSIC,
                SpellEngine.EFFECT_PLAY_SOUND, SpellEngine.EFFECT_UNLEARN_SPECIALIZATION,
                SpellEngine.EFFECT_REPUTATION, SpellEngine.EFFECT_DUEL, SpellEngine.EFFECT_INEBRIATE,
                SpellEngine.EFFECT_ADD_HONOR, SpellEngine.EFFECT_LEARN_SPELL, SpellEngine.EFFECT_ADD_FARSIGHT);

        assertTrue(EffectTable.standard().effects().containsAll(player));
    }

    @Test
    void standardWhenBuiltShouldRegisterUnitFamily() {
        Set<Integer> unit = Set.of(
                SpellEngine.EFFECT_INSTAKILL, SpellEngine.EFFECT_HEALTH_LEECH, SpellEngine.EFFECT_POWER_DRAIN,
                SpellEngine.EFFECT_ADD_COMBO_POINTS, SpellEngine.EFFECT_INTERRUPT_CAST, SpellEngine.EFFECT_SANCTUARY,
                SpellEngine.EFFECT_ADD_EXTRA_ATTACKS, SpellEngine.EFFECT_ATTACK_ME, SpellEngine.EFFECT_RESURRECT,
                SpellEngine.EFFECT_RESURRECT_NEW, SpellEngine.EFFECT_SPIRIT_HEAL,
                SpellEngine.EFFECT_ENVIRONMENTAL_DAMAGE, SpellEngine.EFFECT_POWER_BURN, SpellEngine.EFFECT_THREAT,
                SpellEngine.EFFECT_HEAL_PCT, SpellEngine.EFFECT_ENERGIZE_PCT, SpellEngine.EFFECT_SELF_RESURRECT,
                SpellEngine.EFFECT_HEAL_MECHANICAL, SpellEngine.EFFECT_WEAPON_PERCENT_DAMAGE,
                SpellEngine.EFFECT_NORMALIZED_WEAPON_DMG, SpellEngine.EFFECT_DISTRACT,
                SpellEngine.EFFECT_DISPEL_MECHANIC, SpellEngine.EFFECT_STEAL_BENEFICIAL_BUFF,
                SpellEngine.EFFECT_MODIFY_THREAT_PERCENT, SpellEngine.EFFECT_REDIRECT_THREAT,
                SpellEngine.EFFECT_HEAL_MAX_HEALTH, SpellEngine.EFFECT_ENERGIZE);

        assertTrue(EffectTable.standard().effects().containsAll(unit));
    }

    @Test
    void standardWhenBuiltShouldRegisterTriggerFamily() {
        Set<Integer> trigger = Set.of(
                SpellEngine.EFFECT_TRIGGER_SPELL, SpellEngine.EFFECT_TRIGGER_MISSILE, SpellEngine.EFFECT_TRIGGER_SPELL_2,
                SpellEngine.EFFECT_SUMMON_RAF_FRIEND, SpellEngine.EFFECT_FORCE_CAST,
                SpellEngine.EFFECT_FORCE_CAST_WITH_VALUE, SpellEngine.EFFECT_TRIGGER_SPELL_WITH_VALUE);

        assertTrue(EffectTable.standard().effects().containsAll(trigger));
    }

    @Test
    void standardWhenBuiltShouldLeaveDamageAndAuraInSpellEngine() {
        Set<Integer> effects = EffectTable.standard().effects();

        assertTrue(!effects.contains(SpellEngine.EFFECT_SCHOOL_DAMAGE));
        assertTrue(!effects.contains(SpellEngine.EFFECT_APPLY_AURA));
        assertEquals(effects, EffectTable.standard().effects(), "deterministic registration");
    }
}
