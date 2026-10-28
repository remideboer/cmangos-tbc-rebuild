package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

/**
 * SPELL_AURA_* modifier catalog (CMaNGOS Aura::ApplyModifier → AuraHandler[auraName]).
 * SpellEngine records the aura on the unit; this type applies the named modifier.
 * spell-algorithms.md "Auras 0–261".
 */
public final class AuraEngine {
    public static final int SPELL_AURA_MOD_ATTACKSPEED = 9;
    public static final int SPELL_AURA_MOD_THREAT = 10;
    public static final int SPELL_AURA_MOD_STUN = 12;
    public static final int SPELL_AURA_MOD_DECREASE_ARMOR = 15;
    public static final int SPELL_AURA_MOD_CONFUSE = 5;
    public static final int SPELL_AURA_MOD_FEAR = 7;
    public static final int SPELL_AURA_PERIODIC_HEAL = 8;
    public static final int SPELL_AURA_OBS_MOD_HEALTH = 20;
    public static final int SPELL_AURA_OBS_MOD_MANA = 21;
    public static final int SPELL_AURA_PERIODIC_ENERGIZE = 24;
    public static final int SPELL_AURA_STATE_IMMUNITY = 38;
    public static final int SPELL_AURA_DISPEL_IMMUNITY = 41;
    public static final int SPELL_AURA_PERIODIC_DAMAGE_PERCENT = 89;
    public static final int SPELL_AURA_ADD_FLAT_MODIFIER = 107;
    public static final int SPELL_AURA_ADD_PCT_MODIFIER = 108;
    public static final int SPELL_AURA_OVERRIDE_CLASS_SCRIPTS = 112;
    public static final int SPELL_AURA_FORCE_REACTION = 139;
    public static final int SPELL_AURA_MECHANIC_IMMUNITY_MASK = 147;
    public static final int SPELL_AURA_SPIRIT_OF_REDEMPTION = 176;
    public static final int SPELL_AURA_MOD_RANGED_ATTACK_POWER_OF_STAT_PERCENT = 212;
    public static final int SPELL_AURA_PHASE = 261;
    /** SpellDefines.h MAX_SPELLMOD. An op at or above this is ignored. */
    public static final int MAX_SPELLMOD = 32;
    /** SpellDefines.h SPELLMOD_DAMAGE. */
    public static final int SPELLMOD_DAMAGE = 0;
    /** SpellDefines.h SPELLMOD_CASTING_TIME. */
    public static final int SPELLMOD_CASTING_TIME = 10;
    /** SpellDefines.h SPELLMOD_COST. */
    public static final int SPELLMOD_COST = 14;
    /** HandleAuraModTotalHealthPercentRegen spell 40409 sets UNIT_FLAG_UNINTERACTIBLE. */
    private static final int SPELL_UNINTERACTIBLE_HEALTH_REGEN = 40409;
    public static final int SPELL_AURA_MOD_RESISTANCE = 22;
    public static final int SPELL_AURA_MOD_BASE_RESISTANCE = 83;
    public static final int SPELL_AURA_MOD_RESISTANCE_PCT = 101;
    public static final int SPELL_AURA_MOD_BASE_RESISTANCE_PCT = 142;
    public static final int SPELL_AURA_MOD_PACIFY = 25;
    public static final int SPELL_AURA_MOD_ROOT = 26;
    public static final int SPELL_AURA_MOD_SILENCE = 27;
    public static final int SPELL_AURA_MOD_STAT = 29;
    public static final int SPELL_AURA_MOD_SKILL = 30;
    public static final int SPELL_AURA_MOD_SKILL_TALENT = 98;
    public static final int SPELL_AURA_MOD_STEALTH = 16;
    public static final int SPELL_AURA_MOD_STEALTH_DETECT = 17;
    public static final int SPELL_AURA_MOD_INVISIBILITY = 18;
    public static final int SPELL_AURA_MOD_INVISIBILITY_DETECTION = 19;
    public static final int SPELL_AURA_TRACK_CREATURES = 44;
    public static final int SPELL_AURA_TRACK_RESOURCES = 45;
    public static final int SPELL_AURA_MOD_INCREASE_SPEED = 31;
    public static final int SPELL_AURA_MOD_INCREASE_MOUNTED_SPEED = 32;
    public static final int SPELL_AURA_MOD_MOUNTED_SPEED_ALWAYS = 130;
    public static final int SPELL_AURA_MOD_MANA_REGEN_INTERRUPT = 134;
    public static final int SPELL_AURA_MOD_RESISTANCE_EXCLUSIVE = 143;
    public static final int SPELL_AURA_MOD_WATER_BREATHING = 155;
    public static final int SPELL_AURA_DETECT_AMORE = 170;
    public static final int SPELL_AURA_MOD_MOUNTED_SPEED_NOT_STACK = 172;
    public static final int SPELL_AURA_USE_NORMAL_MOVEMENT_SPEED = 191;
    public static final int SPELL_AURA_MOD_MELEE_RANGED_HASTE = 192;
    public static final int SPELL_AURA_HASTE_ALL = 193;
    public static final int SPELL_AURA_FLY = 201;
    public static final int SPELL_AURA_MOD_FLIGHT_SPEED = 206;
    public static final int SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED = 207;
    public static final int SPELL_AURA_MOD_FLIGHT_SPEED_STACKING = 208;
    public static final int SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED_STACKING = 209;
    public static final int SPELL_AURA_MOD_FLIGHT_SPEED_NOT_STACKING = 210;
    public static final int SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED_NOT_STACKING = 211;
    public static final int SPELL_AURA_ARENA_PREPARATION = 215;
    public static final int SPELL_AURA_HASTE_RANGED = 218;
    public static final int SPELL_AURA_FORCE_MOVE_FORWARD = 241;
    public static final int SPELL_AURA_COMPREHEND_LANGUAGE = 244;
    public static final int SPELL_AURA_MOD_DECREASE_SPEED = 33;
    public static final int SPELL_AURA_MOD_SPEED_ALWAYS = 129;
    public static final int SPELL_AURA_MOD_SPEED_NOT_STACK = 171;
    public static final int SPELL_AURA_MOD_INCREASE_SWIM_SPEED = 58;
    public static final int SPELL_AURA_MOD_INCREASE_HEALTH = 34;
    public static final int SPELL_AURA_MOD_INCREASE_HEALTH_2 = 250;
    public static final int SPELL_AURA_MOD_INCREASE_MAX_HEALTH = 230;
    public static final int SPELL_AURA_MOD_INCREASE_ENERGY = 35;
    public static final int SPELL_AURA_MOD_INCREASE_ENERGY_PERCENT = 132;
    public static final int SPELL_AURA_MOD_INCREASE_HEALTH_PERCENT = 133;
    public static final int SPELL_AURA_EFFECT_IMMUNITY = 37;
    public static final int SPELL_AURA_SCHOOL_IMMUNITY = 39;
    public static final int SPELL_AURA_DAMAGE_IMMUNITY = 40;
    public static final int SPELL_AURA_MOD_SHAPESHIFT = 36;
    public static final int SPELL_AURA_MOD_PACIFY_SILENCE = 60;
    public static final int SPELL_AURA_MOD_CRIT_PERCENT = 52;
    public static final int SPELL_AURA_MOD_DODGE_PERCENT = 49;
    public static final int SPELL_AURA_MOD_PARRY_PERCENT = 47;
    public static final int SPELL_AURA_MOD_BLOCK_PERCENT = 51;
    public static final int SPELL_AURA_MOD_HIT_CHANCE = 54;
    public static final int SPELL_AURA_MOD_SPELL_HIT_CHANCE = 55;
    public static final int SPELL_AURA_MOD_SPELL_CRIT_CHANCE = 57;
    public static final int SPELL_AURA_MOD_STALKED = 68;
    public static final int SPELL_AURA_MOD_SPELL_CRIT_CHANCE_SCHOOL = 71;
    public static final int SPELL_AURA_MOD_DAMAGE_DONE = 13;
    public static final int SPELL_AURA_MOD_DAMAGE_PERCENT_DONE = 79;
    public static final int SPELL_AURA_MOD_HEALING_DONE = 135;
    public static final int SPELL_AURA_MOD_POWER_COST_SCHOOL = 73;
    public static final int SPELL_AURA_MOD_POWER_COST_SCHOOL_PCT = 72;
    public static final int SPELL_AURA_MOD_SCALE = 61;
    public static final int SPELL_AURA_MOD_SCALE_2 = 239;
    public static final int SPELL_AURA_MECHANIC_IMMUNITY = 77;
    public static final int SPELL_AURA_MOD_CASTING_SPEED_NOT_STACK = 65;
    public static final int SPELL_AURA_HASTE_SPELLS = 216;
    public static final int SPELL_AURA_FEIGN_DEATH = 66;
    public static final int SPELL_AURA_MOD_DISARM = 67;
    public static final int SPELL_AURA_MOD_PERCENT_STAT = 80;
    public static final int SPELL_AURA_MOD_TOTAL_STAT_PERCENTAGE = 137;
    public static final int SPELL_AURA_MOD_REGEN = 84;
    /** Drink effect1 (Spell.dbc 430) — amount copied onto MOD_POWER_REGEN (spell_scripts Drink). */
    public static final int SPELL_AURA_PERIODIC_DUMMY = 226;
    public static final int SPELL_AURA_MOD_POWER_REGEN = 85;
    public static final int SPELL_AURA_MOD_POWER_REGEN_PERCENT = 110;
    public static final int SPELL_AURA_MOD_MANA_REGEN_FROM_STAT = 219;
    public static final int SPELL_AURA_WATER_BREATHING = 82;
    public static final int SPELL_AURA_PREVENTS_FLEEING = 92;
    public static final int SPELL_AURA_MOD_UNATTACKABLE = 93;
    public static final int SPELL_AURA_GHOST = 95;
    public static final int SPELL_AURA_MOD_ATTACK_POWER = 99;
    public static final int SPELL_AURA_MOD_ATTACK_POWER_PCT = 166;
    public static final int SPELL_AURA_WATER_WALK = 104;
    public static final int SPELL_AURA_FEATHER_FALL = 105;
    public static final int SPELL_AURA_HOVER = 106;
    public static final int SPELL_AURA_UNTRACKABLE = 120;
    public static final int SPELL_AURA_EMPATHY = 121;
    public static final int SPELL_AURA_TRACK_STEALTHED = 151;
    public static final int SPELL_AURA_MOD_STEALTH_LEVEL = 154;
    public static final int SPELL_AURA_MOD_OFFHAND_DAMAGE_PCT = 122;
    public static final int SPELL_AURA_MOD_TARGET_RESISTANCE = 123;
    public static final int SPELL_AURA_MOD_RANGED_ATTACK_POWER = 124;
    public static final int SPELL_AURA_MOD_RANGED_ATTACK_POWER_PCT = 167;
    public static final int SPELL_AURA_MOD_RATING = 189;
    public static final int SPELL_AURA_MOD_SPELL_DAMAGE_OF_ATTACK_POWER = 237;
    public static final int SPELL_AURA_MOD_SPELL_HEALING_OF_ATTACK_POWER = 238;
    public static final int SPELL_AURA_MOD_EXPERTISE = 240;
    public static final int SPELL_AURA_FACTION_OVERRIDE = 243;
    /** SharedDefines.h SPELL_SCHOOL_MASK_SPELL — fire|nature|frost|shadow|arcane (no normal/holy). */
    public static final int SPELL_SCHOOL_MASK_SPELL = 124;
    public static final int SPELL_AURA_MOD_MELEE_HASTE = 138;
    public static final int SPELL_AURA_MOD_RANGED_HASTE = 140;
    public static final int SPELL_AURA_MOD_RANGED_AMMO_HASTE = 141;
    public static final int SPELL_AURA_SAFE_FALL = 144;
    public static final int SPELL_AURA_MOD_SHIELD_BLOCKVALUE_PCT = 150;
    public static final int SPELL_AURA_MOD_SHIELD_BLOCKVALUE = 158;
    /** SpellSchools.h MAX_SPELL_SCHOOL — normal through arcane. */
    public static final int MAX_SPELL_SCHOOL = 7;
    /** SharedDefines.h MAX_STATS — strength through spirit. */
    public static final int MAX_STATS = 5;
    /** SharedDefines.h MAX_POWERS — mana through runic (TBC: 5). */
    public static final int MAX_POWERS = 5;

    private static final Set<Integer> KNOWN_AURAS = Set.of(
            SPELL_AURA_MOD_CONFUSE, SPELL_AURA_MOD_FEAR, SPELL_AURA_MOD_ATTACKSPEED,
            SPELL_AURA_MOD_THREAT,
            SPELL_AURA_MOD_STUN, SPELL_AURA_MOD_DECREASE_ARMOR,
            SPELL_AURA_MOD_RESISTANCE, SPELL_AURA_MOD_BASE_RESISTANCE, SPELL_AURA_MOD_RESISTANCE_PCT,
            SPELL_AURA_MOD_BASE_RESISTANCE_PCT,
            SPELL_AURA_MOD_PACIFY, SPELL_AURA_MOD_ROOT, SPELL_AURA_MOD_SILENCE, SPELL_AURA_MOD_STAT,
            SPELL_AURA_MOD_SKILL, SPELL_AURA_MOD_SKILL_TALENT,
            SPELL_AURA_MOD_STEALTH, SPELL_AURA_MOD_STEALTH_DETECT, SPELL_AURA_MOD_STEALTH_LEVEL,
            SPELL_AURA_MOD_INVISIBILITY, SPELL_AURA_MOD_INVISIBILITY_DETECTION,
            SPELL_AURA_TRACK_CREATURES, SPELL_AURA_TRACK_RESOURCES,
            SPELL_AURA_MOD_CRIT_PERCENT, SPELL_AURA_MOD_DODGE_PERCENT, SPELL_AURA_MOD_PARRY_PERCENT,
            SPELL_AURA_MOD_BLOCK_PERCENT, SPELL_AURA_MOD_HIT_CHANCE, SPELL_AURA_MOD_SPELL_HIT_CHANCE,
            SPELL_AURA_MOD_SPELL_CRIT_CHANCE, SPELL_AURA_MOD_STALKED,
            SPELL_AURA_MOD_SPELL_CRIT_CHANCE_SCHOOL, SPELL_AURA_MOD_DAMAGE_DONE,
            SPELL_AURA_MOD_DAMAGE_PERCENT_DONE, SPELL_AURA_MOD_HEALING_DONE,
            SPELL_AURA_MOD_POWER_COST_SCHOOL, SPELL_AURA_MOD_POWER_COST_SCHOOL_PCT,
            SPELL_AURA_MOD_INCREASE_SPEED, SPELL_AURA_MOD_INCREASE_MOUNTED_SPEED,
            SPELL_AURA_MOD_MOUNTED_SPEED_ALWAYS, SPELL_AURA_MOD_MOUNTED_SPEED_NOT_STACK,
            SPELL_AURA_MOD_SPEED_ALWAYS, SPELL_AURA_MOD_SPEED_NOT_STACK,
            SPELL_AURA_MOD_DECREASE_SPEED, SPELL_AURA_MOD_INCREASE_SWIM_SPEED,
            SPELL_AURA_MOD_INCREASE_HEALTH, SPELL_AURA_MOD_INCREASE_HEALTH_2,
            SPELL_AURA_MOD_INCREASE_MAX_HEALTH, SPELL_AURA_MOD_INCREASE_HEALTH_PERCENT,
            SPELL_AURA_EFFECT_IMMUNITY, SPELL_AURA_SCHOOL_IMMUNITY, SPELL_AURA_DAMAGE_IMMUNITY,
            SPELL_AURA_STATE_IMMUNITY, SPELL_AURA_DISPEL_IMMUNITY, SPELL_AURA_MECHANIC_IMMUNITY_MASK,
            SPELL_AURA_MECHANIC_IMMUNITY,
            SPELL_AURA_PERIODIC_HEAL, SPELL_AURA_OBS_MOD_HEALTH, SPELL_AURA_OBS_MOD_MANA,
            SPELL_AURA_PERIODIC_ENERGIZE, SPELL_AURA_PERIODIC_DAMAGE_PERCENT,
            SPELL_AURA_ADD_FLAT_MODIFIER, SPELL_AURA_ADD_PCT_MODIFIER,
            SPELL_AURA_OVERRIDE_CLASS_SCRIPTS, SPELL_AURA_FORCE_REACTION,
            SPELL_AURA_SPIRIT_OF_REDEMPTION, SPELL_AURA_MOD_RANGED_ATTACK_POWER_OF_STAT_PERCENT,
            SPELL_AURA_PHASE,
            SPELL_AURA_MOD_INCREASE_ENERGY, SPELL_AURA_MOD_INCREASE_ENERGY_PERCENT,
            SPELL_AURA_MOD_SHAPESHIFT, SPELL_AURA_MOD_PACIFY_SILENCE,
            SPELL_AURA_MOD_SCALE, SPELL_AURA_MOD_SCALE_2,
            SPELL_AURA_MOD_CASTING_SPEED_NOT_STACK, SPELL_AURA_HASTE_SPELLS,
            SPELL_AURA_FEIGN_DEATH, SPELL_AURA_MOD_DISARM,
            SPELL_AURA_MOD_PERCENT_STAT, SPELL_AURA_MOD_TOTAL_STAT_PERCENTAGE,
            SPELL_AURA_MOD_REGEN, SPELL_AURA_MOD_POWER_REGEN, SPELL_AURA_MOD_POWER_REGEN_PERCENT,
            SPELL_AURA_MOD_MANA_REGEN_FROM_STAT,
            SPELL_AURA_WATER_BREATHING, SPELL_AURA_MOD_WATER_BREATHING,
            SPELL_AURA_MOD_MANA_REGEN_INTERRUPT, SPELL_AURA_MOD_RESISTANCE_EXCLUSIVE,
            SPELL_AURA_PREVENTS_FLEEING, SPELL_AURA_MOD_UNATTACKABLE,
            SPELL_AURA_GHOST, SPELL_AURA_MOD_ATTACK_POWER, SPELL_AURA_MOD_ATTACK_POWER_PCT,
            SPELL_AURA_UNTRACKABLE, SPELL_AURA_EMPATHY, SPELL_AURA_TRACK_STEALTHED,
            SPELL_AURA_MOD_OFFHAND_DAMAGE_PCT, SPELL_AURA_MOD_TARGET_RESISTANCE,
            SPELL_AURA_MOD_RANGED_ATTACK_POWER, SPELL_AURA_MOD_RANGED_ATTACK_POWER_PCT,
            SPELL_AURA_MOD_RATING, SPELL_AURA_MOD_EXPERTISE, SPELL_AURA_FACTION_OVERRIDE,
            SPELL_AURA_MOD_SPELL_DAMAGE_OF_ATTACK_POWER, SPELL_AURA_MOD_SPELL_HEALING_OF_ATTACK_POWER,
            SPELL_AURA_WATER_WALK, SPELL_AURA_FEATHER_FALL, SPELL_AURA_HOVER,
            SPELL_AURA_MOD_MELEE_HASTE, SPELL_AURA_MOD_RANGED_HASTE, SPELL_AURA_MOD_RANGED_AMMO_HASTE,
            SPELL_AURA_USE_NORMAL_MOVEMENT_SPEED,
            SPELL_AURA_MOD_MELEE_RANGED_HASTE, SPELL_AURA_HASTE_ALL,
            SPELL_AURA_DETECT_AMORE, SPELL_AURA_FLY,
            SPELL_AURA_MOD_FLIGHT_SPEED, SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED,
            SPELL_AURA_MOD_FLIGHT_SPEED_STACKING, SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED_STACKING,
            SPELL_AURA_MOD_FLIGHT_SPEED_NOT_STACKING, SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED_NOT_STACKING,
            SPELL_AURA_ARENA_PREPARATION, SPELL_AURA_HASTE_RANGED,
            SPELL_AURA_FORCE_MOVE_FORWARD, SPELL_AURA_COMPREHEND_LANGUAGE,
            SPELL_AURA_SAFE_FALL,
            SPELL_AURA_MOD_SHIELD_BLOCKVALUE, SPELL_AURA_MOD_SHIELD_BLOCKVALUE_PCT);

    public boolean knownAura(int aura) {
        return KNOWN_AURAS.contains(aura);
    }

    /** Apply the modifier named by {@code sp.aura} to {@code target}. Unknown auras mutate nothing. */
    public void apply(Unit target, SpellEngine.SpellInfo sp) {
        if (target == null || sp == null) {
            return;
        }
        if (sp.aura() == SPELL_AURA_MOD_STUN) {
            modStun(target);
        }
        if (sp.aura() == SPELL_AURA_MOD_DECREASE_ARMOR) {
            modDecreaseArmor(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_FEAR) {
            // HandleModFear(true) → SetFleeing(true).
            if (target.addFearAura()) {
                target.setFleeing(true);
            }
        }
        if (sp.aura() == SPELL_AURA_PREVENTS_FLEEING) {
            modPreventFleeing(target, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_CONFUSE) {
            // HandleModConfuse(true) → SetConfused(true).
            target.setConfused(true);
        }
        if (sp.aura() == SPELL_AURA_MOD_DISARM) {
            target.setDisarmed(true);
        }
        if (sp.aura() == SPELL_AURA_MOD_UNATTACKABLE) {
            modUnattackable(target, true);
        }
        if (sp.aura() == SPELL_AURA_UNTRACKABLE) {
            modUntrackable(target, true);
        }
        if (sp.aura() == SPELL_AURA_EMPATHY) {
            modEmpathy(target, true);
        }
        if (sp.aura() == SPELL_AURA_TRACK_STEALTHED) {
            modTrackStealthed(target, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_STALKED) {
            modStalked(target, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_STEALTH) {
            modStealth(target, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_STEALTH_DETECT) {
            modStealthDetect(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_STEALTH_LEVEL) {
            modStealthLevel(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_INVISIBILITY_DETECTION) {
            modInvisibilityDetect(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_INVISIBILITY) {
            modInvisibility(target, true);
        }
        if (sp.aura() == SPELL_AURA_TRACK_RESOURCES) {
            modTrackResources(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_TRACK_CREATURES) {
            modTrackCreatures(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_SCALE || sp.aura() == SPELL_AURA_MOD_SCALE_2) {
            modScale(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_CASTING_SPEED_NOT_STACK || sp.aura() == SPELL_AURA_HASTE_SPELLS) {
            modCastingSpeed(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_FEIGN_DEATH) {
            modFeignDeath(target, true);
        }
        if (sp.aura() == SPELL_AURA_WATER_BREATHING) {
            modWaterBreathing(target, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_WATER_BREATHING) {
            modWaterBreathingPct(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_MANA_REGEN_INTERRUPT) {
            modManaRegenInterrupt(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_MANA_REGEN_FROM_STAT) {
            modManaRegenFromStat(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_RESISTANCE_EXCLUSIVE) {
            modResistanceExclusive(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_DETECT_AMORE) {
            modDetectAmore(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_FLY) {
            target.sendCanFly(true);
        }
        if (sp.aura() == SPELL_AURA_MOD_FLIGHT_SPEED
                || sp.aura() == SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED
                || sp.aura() == SPELL_AURA_MOD_FLIGHT_SPEED_STACKING
                || sp.aura() == SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED_STACKING
                || sp.aura() == SPELL_AURA_MOD_FLIGHT_SPEED_NOT_STACKING
                || sp.aura() == SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED_NOT_STACKING) {
            modFlightSpeed(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_ARENA_PREPARATION) {
            modArenaPreparation(target, true);
        }
        if (sp.aura() == SPELL_AURA_FORCE_MOVE_FORWARD) {
            modForceMoveForward(target, true);
        }
        if (sp.aura() == SPELL_AURA_COMPREHEND_LANGUAGE) {
            modComprehendLanguage(target, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_MELEE_RANGED_HASTE) {
            modMeleeRangedHaste(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_HASTE_ALL) {
            modHasteAll(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_GHOST) {
            modGhost(target, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_ATTACK_POWER) {
            modAttackPower(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_ATTACK_POWER_PCT) {
            modAttackPowerPct(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_OFFHAND_DAMAGE_PCT) {
            modOffhandDamagePct(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_TARGET_RESISTANCE) {
            modTargetResistance(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_RANGED_ATTACK_POWER) {
            modRangedAttackPower(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_RANGED_ATTACK_POWER_PCT) {
            modRangedAttackPowerPct(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_ATTACKSPEED) {
            modAttackSpeed(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_MELEE_HASTE) {
            modMeleeHaste(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_RANGED_HASTE || sp.aura() == SPELL_AURA_HASTE_RANGED) {
            modRangedHaste(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_RANGED_AMMO_HASTE) {
            modRangedAmmoHaste(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_POWER_REGEN_PERCENT) {
            modPowerRegenPercent(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_USE_NORMAL_MOVEMENT_SPEED) {
            modUseNormalMovementSpeed(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_RATING) {
            modRating(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_EXPERTISE) {
            modExpertise(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_SPELL_DAMAGE_OF_ATTACK_POWER) {
            modSpellDamageOfAttackPower(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_SPELL_HEALING_OF_ATTACK_POWER) {
            modSpellHealingOfAttackPower(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_SAFE_FALL) {
            modSafeFall(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_SHIELD_BLOCKVALUE) {
            modShieldBlockValue(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_SHIELD_BLOCKVALUE_PCT) {
            modShieldBlockValuePct(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_WATER_WALK) {
            target.sendWaterWalk(true);
        }
        if (sp.aura() == SPELL_AURA_FEATHER_FALL) {
            target.sendFeatherFall(true);
        }
        if (sp.aura() == SPELL_AURA_HOVER) {
            target.sendHover(true);
        }
        if (sp.aura() == SPELL_AURA_MOD_ROOT) {
            modRoot(target, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_SILENCE) {
            target.setSilenced(true);
        }
        if (sp.aura() == SPELL_AURA_MOD_PACIFY) {
            target.setPacified(true);
        }
        if (sp.aura() == SPELL_AURA_MOD_PACIFY_SILENCE) {
            // HandleAuraModPacifyAndSilence → pacify + silence.
            target.setPacified(true);
            target.setSilenced(true);
        }
        if (sp.aura() == SPELL_AURA_MOD_DECREASE_SPEED) {
            modDecreaseSpeed(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_SPEED
                || sp.aura() == SPELL_AURA_MOD_SPEED_ALWAYS
                || sp.aura() == SPELL_AURA_MOD_SPEED_NOT_STACK) {
            modIncreaseSpeed(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_MOUNTED_SPEED
                || sp.aura() == SPELL_AURA_MOD_MOUNTED_SPEED_ALWAYS
                || sp.aura() == SPELL_AURA_MOD_MOUNTED_SPEED_NOT_STACK) {
            modIncreaseMountedSpeed(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_SWIM_SPEED) {
            modIncreaseSwimSpeed(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_SHAPESHIFT) {
            modShapeshift(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_RESISTANCE) {
            modResistance(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_BASE_RESISTANCE) {
            modBaseResistance(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_BASE_RESISTANCE_PCT) {
            modBaseResistancePct(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_RESISTANCE_PCT) {
            modResistancePct(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_STAT) {
            modStat(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_SKILL) {
            modSkill(target, sp, true, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_SKILL_TALENT) {
            modSkill(target, sp, true, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_PERCENT_STAT) {
            modPercentStat(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_TOTAL_STAT_PERCENTAGE) {
            modTotalStatPercentage(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_CRIT_PERCENT) {
            modCritPercent(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_DODGE_PERCENT) {
            modDodgePercent(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_PARRY_PERCENT) {
            modParryPercent(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_BLOCK_PERCENT) {
            modBlockPercent(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_HIT_CHANCE) {
            modHitChance(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_SPELL_HIT_CHANCE) {
            modSpellHitChance(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_SPELL_CRIT_CHANCE) {
            modSpellCritChanceAura(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_SPELL_CRIT_CHANCE_SCHOOL) {
            modSpellCritChanceSchool(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_DAMAGE_DONE) {
            modDamageDone(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_DAMAGE_PERCENT_DONE) {
            modDamagePercentDone(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_HEALING_DONE) {
            modHealingDone(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_POWER_COST_SCHOOL) {
            modPowerCostSchool(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_POWER_COST_SCHOOL_PCT) {
            modPowerCostSchoolPct(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_HEALTH || sp.aura() == SPELL_AURA_MOD_INCREASE_HEALTH_2) {
            modIncreaseHealth(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_MAX_HEALTH) {
            modIncreaseMaxHealth(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_HEALTH_PERCENT) {
            modIncreaseHealthPercent(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_THREAT) {
            modThreat(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_SCHOOL_IMMUNITY) {
            modSchoolImmunity(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MECHANIC_IMMUNITY) {
            modMechanicImmunity(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_EFFECT_IMMUNITY) {
            modEffectImmunity(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_DAMAGE_IMMUNITY) {
            modDamageImmunity(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_STATE_IMMUNITY) {
            modStateImmunity(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_DISPEL_IMMUNITY) {
            modDispelImmunity(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MECHANIC_IMMUNITY_MASK) {
            modMechanicImmunityMask(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_PHASE) {
            modPhase(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_RANGED_ATTACK_POWER_OF_STAT_PERCENT) {
            modRangedAttackPowerOfStatPercent(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_SPIRIT_OF_REDEMPTION) {
            modSpiritOfRedemption(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_FORCE_REACTION) {
            modForceReaction(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_ADD_FLAT_MODIFIER || sp.aura() == SPELL_AURA_ADD_PCT_MODIFIER) {
            modSpellModifier(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_OVERRIDE_CLASS_SCRIPTS) {
            modOverrideClassScript(target, sp, true);
        }
        if (sp.id() == SPELL_UNINTERACTIBLE_HEALTH_REGEN && sp.aura() == SPELL_AURA_OBS_MOD_HEALTH) {
            modUninteractible(target, true);
        }
        if (sp.aura() == SPELL_AURA_FACTION_OVERRIDE) {
            modFactionOverride(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_ENERGY) {
            modIncreaseEnergy(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_ENERGY_PERCENT) {
            modIncreaseEnergyPercent(target, sp, true);
        }
    }

    /** Reverse {@link #apply} for auras that mutate stats (CMaNGOS Aura::ApplyModifier(false)). */
    public void unapply(Unit target, SpellEngine.SpellInfo sp) {
        if (target == null || sp == null) {
            return;
        }
        if (sp.aura() == SPELL_AURA_MOD_RESISTANCE) {
            modResistance(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_BASE_RESISTANCE) {
            modBaseResistance(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_BASE_RESISTANCE_PCT) {
            modBaseResistancePct(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_RESISTANCE_PCT) {
            modResistancePct(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_DECREASE_ARMOR) {
            modDecreaseArmor(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_STAT) {
            modStat(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_SKILL) {
            modSkill(target, sp, false, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_SKILL_TALENT) {
            modSkill(target, sp, false, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_PERCENT_STAT) {
            modPercentStat(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_TOTAL_STAT_PERCENTAGE) {
            modTotalStatPercentage(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_CRIT_PERCENT) {
            modCritPercent(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_DODGE_PERCENT) {
            modDodgePercent(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_PARRY_PERCENT) {
            modParryPercent(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_BLOCK_PERCENT) {
            modBlockPercent(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_HIT_CHANCE) {
            modHitChance(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_SPELL_HIT_CHANCE) {
            modSpellHitChance(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_SPELL_CRIT_CHANCE) {
            modSpellCritChanceAura(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_SPELL_CRIT_CHANCE_SCHOOL) {
            modSpellCritChanceSchool(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_DAMAGE_DONE) {
            modDamageDone(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_DAMAGE_PERCENT_DONE) {
            modDamagePercentDone(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_HEALING_DONE) {
            modHealingDone(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_POWER_COST_SCHOOL) {
            modPowerCostSchool(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_POWER_COST_SCHOOL_PCT) {
            modPowerCostSchoolPct(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_HEALTH || sp.aura() == SPELL_AURA_MOD_INCREASE_HEALTH_2) {
            modIncreaseHealth(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_MAX_HEALTH) {
            modIncreaseMaxHealth(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_HEALTH_PERCENT) {
            modIncreaseHealthPercent(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_THREAT) {
            modThreat(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_SCHOOL_IMMUNITY) {
            modSchoolImmunity(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MECHANIC_IMMUNITY) {
            modMechanicImmunity(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_EFFECT_IMMUNITY) {
            modEffectImmunity(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_DAMAGE_IMMUNITY) {
            modDamageImmunity(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_STATE_IMMUNITY) {
            modStateImmunity(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_DISPEL_IMMUNITY) {
            modDispelImmunity(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MECHANIC_IMMUNITY_MASK) {
            modMechanicImmunityMask(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_PHASE) {
            modPhase(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_RANGED_ATTACK_POWER_OF_STAT_PERCENT) {
            modRangedAttackPowerOfStatPercent(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_SPIRIT_OF_REDEMPTION) {
            modSpiritOfRedemption(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_FORCE_REACTION) {
            modForceReaction(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_ADD_FLAT_MODIFIER || sp.aura() == SPELL_AURA_ADD_PCT_MODIFIER) {
            modSpellModifier(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_OVERRIDE_CLASS_SCRIPTS) {
            modOverrideClassScript(target, sp, false);
        }
        if (sp.id() == SPELL_UNINTERACTIBLE_HEALTH_REGEN && sp.aura() == SPELL_AURA_OBS_MOD_HEALTH) {
            modUninteractible(target, false);
        }
        if (sp.aura() == SPELL_AURA_FACTION_OVERRIDE) {
            modFactionOverride(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_ENERGY) {
            modIncreaseEnergy(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_ENERGY_PERCENT) {
            modIncreaseEnergyPercent(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_STUN) {
            // HandleAuraModStun(false) → SetStunned(false) only when no MOD_STUN remain.
            if (target.removeStunAura()) {
                target.setStunned(false);
                // SetImmobilizedState(false): keep rooted while MOD_ROOT still active.
                if (target.rootAuraCount() == 0) {
                    target.setRooted(false);
                    target.sendMoveRoot(false);
                }
            }
        }
        if (sp.aura() == SPELL_AURA_MOD_FEAR) {
            // HandleModFear(false) → SetFleeing(false) only when no MOD_FEAR remain.
            if (target.removeFearAura()) {
                target.setFleeing(false);
            }
        }
        if (sp.aura() == SPELL_AURA_PREVENTS_FLEEING) {
            modPreventFleeing(target, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_CONFUSE) {
            // HandleModConfuse(false) → SetConfused(false); stacking other MOD_CONFUSE later.
            target.setConfused(false);
        }
        if (sp.aura() == SPELL_AURA_MOD_DISARM) {
            // HandleAuraModDisarm(false); stacking other MOD_DISARM later.
            target.setDisarmed(false);
        }
        if (sp.aura() == SPELL_AURA_MOD_UNATTACKABLE) {
            modUnattackable(target, false);
        }
        if (sp.aura() == SPELL_AURA_UNTRACKABLE) {
            modUntrackable(target, false);
        }
        if (sp.aura() == SPELL_AURA_EMPATHY) {
            modEmpathy(target, false);
        }
        if (sp.aura() == SPELL_AURA_TRACK_STEALTHED) {
            modTrackStealthed(target, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_STALKED) {
            modStalked(target, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_STEALTH) {
            modStealth(target, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_STEALTH_DETECT) {
            modStealthDetect(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_STEALTH_LEVEL) {
            modStealthLevel(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_INVISIBILITY_DETECTION) {
            modInvisibilityDetect(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_INVISIBILITY) {
            modInvisibility(target, false);
        }
        if (sp.aura() == SPELL_AURA_TRACK_RESOURCES) {
            modTrackResources(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_TRACK_CREATURES) {
            modTrackCreatures(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_SCALE || sp.aura() == SPELL_AURA_MOD_SCALE_2) {
            modScale(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_CASTING_SPEED_NOT_STACK || sp.aura() == SPELL_AURA_HASTE_SPELLS) {
            modCastingSpeed(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_FEIGN_DEATH) {
            modFeignDeath(target, false);
        }
        if (sp.aura() == SPELL_AURA_WATER_BREATHING) {
            modWaterBreathing(target, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_WATER_BREATHING) {
            modWaterBreathingPct(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_MANA_REGEN_INTERRUPT) {
            modManaRegenInterrupt(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_MANA_REGEN_FROM_STAT) {
            modManaRegenFromStat(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_RESISTANCE_EXCLUSIVE) {
            modResistanceExclusive(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_DETECT_AMORE) {
            modDetectAmore(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_FLY) {
            target.sendCanFly(false);
        }
        if (sp.aura() == SPELL_AURA_MOD_FLIGHT_SPEED
                || sp.aura() == SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED
                || sp.aura() == SPELL_AURA_MOD_FLIGHT_SPEED_STACKING
                || sp.aura() == SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED_STACKING
                || sp.aura() == SPELL_AURA_MOD_FLIGHT_SPEED_NOT_STACKING
                || sp.aura() == SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED_NOT_STACKING) {
            modFlightSpeed(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_ARENA_PREPARATION) {
            modArenaPreparation(target, false);
        }
        if (sp.aura() == SPELL_AURA_FORCE_MOVE_FORWARD) {
            modForceMoveForward(target, false);
        }
        if (sp.aura() == SPELL_AURA_COMPREHEND_LANGUAGE) {
            modComprehendLanguage(target, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_MELEE_RANGED_HASTE) {
            modMeleeRangedHaste(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_HASTE_ALL) {
            modHasteAll(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_GHOST) {
            modGhost(target, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_ATTACK_POWER) {
            modAttackPower(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_ATTACK_POWER_PCT) {
            modAttackPowerPct(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_OFFHAND_DAMAGE_PCT) {
            modOffhandDamagePct(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_TARGET_RESISTANCE) {
            modTargetResistance(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_RANGED_ATTACK_POWER) {
            modRangedAttackPower(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_RANGED_ATTACK_POWER_PCT) {
            modRangedAttackPowerPct(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_ATTACKSPEED) {
            modAttackSpeed(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_MELEE_HASTE) {
            modMeleeHaste(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_RANGED_HASTE || sp.aura() == SPELL_AURA_HASTE_RANGED) {
            modRangedHaste(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_RANGED_AMMO_HASTE) {
            modRangedAmmoHaste(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_POWER_REGEN_PERCENT) {
            modPowerRegenPercent(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_USE_NORMAL_MOVEMENT_SPEED) {
            modUseNormalMovementSpeed(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_RATING) {
            modRating(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_EXPERTISE) {
            modExpertise(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_SPELL_DAMAGE_OF_ATTACK_POWER) {
            modSpellDamageOfAttackPower(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_SPELL_HEALING_OF_ATTACK_POWER) {
            modSpellHealingOfAttackPower(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_SAFE_FALL) {
            modSafeFall(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_SHIELD_BLOCKVALUE) {
            modShieldBlockValue(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_SHIELD_BLOCKVALUE_PCT) {
            modShieldBlockValuePct(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_WATER_WALK) {
            target.sendWaterWalk(false);
        }
        if (sp.aura() == SPELL_AURA_FEATHER_FALL) {
            target.sendFeatherFall(false);
        }
        if (sp.aura() == SPELL_AURA_HOVER) {
            target.sendHover(false);
        }
        if (sp.aura() == SPELL_AURA_MOD_ROOT) {
            // HandleAuraModRoot(false) → SetImmobilizedState(false) only when no other MOD_ROOT.
            modRoot(target, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_SILENCE) {
            // HandleAuraModSilence(false); stacking other MOD_SILENCE later.
            target.setSilenced(false);
        }
        if (sp.aura() == SPELL_AURA_MOD_PACIFY) {
            // HandleAuraModPacify(false); stacking other MOD_PACIFY later.
            target.setPacified(false);
        }
        if (sp.aura() == SPELL_AURA_MOD_PACIFY_SILENCE) {
            target.setPacified(false);
            target.setSilenced(false);
        }
        if (sp.aura() == SPELL_AURA_MOD_DECREASE_SPEED) {
            modDecreaseSpeed(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_SPEED
                || sp.aura() == SPELL_AURA_MOD_SPEED_ALWAYS
                || sp.aura() == SPELL_AURA_MOD_SPEED_NOT_STACK) {
            modIncreaseSpeed(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_MOUNTED_SPEED
                || sp.aura() == SPELL_AURA_MOD_MOUNTED_SPEED_ALWAYS
                || sp.aura() == SPELL_AURA_MOD_MOUNTED_SPEED_NOT_STACK) {
            modIncreaseMountedSpeed(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_SWIM_SPEED) {
            modIncreaseSwimSpeed(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_SHAPESHIFT) {
            // HandleAuraModShapeshift(false) → SetShapeshiftForm(FORM_NONE). Apply removes other
            // shapeshift auras first, so only one form is active at a time.
            modShapeshift(target, sp, false);
        }
    }

    /**
     * Aura 36 — CMaNGOS HandleAuraModShapeshift: misc = ShapeshiftForm → UNIT_FIELD_BYTES_2 byte 3.
     * Display / power-type / stance-rage stay later; form byte is the player-visible contract.
     */
    private static void modShapeshift(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (apply) {
            target.setShapeshiftForm(sp.misc());
        } else {
            target.setShapeshiftForm(Unit.FORM_NONE);
        }
    }

    /**
     * Aura 15 — CMaNGOS HandleModDecreaseArmor: TOTAL_VALUE on physical armor
     * ({@code UNIT_FIELD_RESISTANCES} school 0).
     */
    private static void modDecreaseArmor(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int delta = apply ? amount : -amount;
        target.setInt(UpdateFields.UNIT_FIELD_RESISTANCES,
                target.getInt(UpdateFields.UNIT_FIELD_RESISTANCES) + delta);
        int buffField = amount > 0
                ? UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE
                : UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSNEGATIVE;
        target.setInt(buffField, target.getInt(buffField) + delta);
    }

    /**
     * Aura 22 — CMaNGOS HandleAuraModResistance: school bits in EffectMiscValue, TOTAL_VALUE amount,
     * then ApplyResistanceBuffModsMod for the character sheet bonus/malus columns.
     */
    private static void modResistance(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int mask = sp.misc();
        if (mask == 0) {
            return;
        }
        for (int i = 0; i < MAX_SPELL_SCHOOL; i++) {
            if ((mask & (1 << i)) == 0) {
                continue;
            }
            int delta = apply ? amount : -amount;
            int resistField = UpdateFields.UNIT_FIELD_RESISTANCES + i;
            target.setInt(resistField, target.getInt(resistField) + delta);
            int buffBase = amount > 0
                    ? UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE
                    : UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSNEGATIVE;
            int buffField = buffBase + i;
            target.setInt(buffField, target.getInt(buffField) + delta);
        }
    }

    /**
     * Aura 83 — CMaNGOS HandleModBaseResistance → BASE_VALUE per school bit.
     * Updates UNIT_FIELD_RESISTANCES only (no ApplyResistanceBuffModsMod).
     */
    private static void modBaseResistance(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int mask = sp.misc();
        if (mask == 0) {
            return;
        }
        int delta = apply ? amount : -amount;
        for (int i = 0; i < MAX_SPELL_SCHOOL; i++) {
            if ((mask & (1 << i)) == 0) {
                continue;
            }
            int resistField = UpdateFields.UNIT_FIELD_RESISTANCES + i;
            target.setInt(resistField, target.getInt(resistField) + delta);
        }
    }

    /**
     * Aura 142 — CMaNGOS HandleAuraModBaseResistancePercent → BASE_PCT per school bit.
     * Scales UNIT_FIELD_RESISTANCES only (no buff columns).
     */
    private static void modBaseResistancePct(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int mask = sp.misc();
        if (mask == 0) {
            return;
        }
        float factor = (100.0f + amount) / 100.0f;
        for (int i = 0; i < MAX_SPELL_SCHOOL; i++) {
            if ((mask & (1 << i)) == 0) {
                continue;
            }
            int resistField = UpdateFields.UNIT_FIELD_RESISTANCES + i;
            int old = target.getInt(resistField);
            int neu = apply ? Math.round(old * factor) : Math.round(old / factor);
            target.setInt(resistField, neu);
        }
    }

    /**
     * Aura 101 — CMaNGOS HandleModResistancePercent → TOTAL_PCT per school bit;
     * sheet buff columns get the resistance delta (ApplyResistanceBuffModsMod).
     */
    private static void modResistancePct(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int mask = sp.misc();
        if (mask == 0) {
            return;
        }
        float factor = (100.0f + amount) / 100.0f;
        for (int i = 0; i < MAX_SPELL_SCHOOL; i++) {
            if ((mask & (1 << i)) == 0) {
                continue;
            }
            int resistField = UpdateFields.UNIT_FIELD_RESISTANCES + i;
            int old = target.getInt(resistField);
            int neu = apply ? Math.round(old * factor) : Math.round(old / factor);
            int change = neu - old;
            target.setInt(resistField, neu);
            int buffBase = amount > 0
                    ? UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE
                    : UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSNEGATIVE;
            int buffField = buffBase + i;
            target.setInt(buffField, target.getInt(buffField) + change);
        }
    }

    /**
     * Aura 29 — CMaNGOS HandleAuraModStat: misc is stat index (0–4) or &lt; 0 for all stats;
     * TOTAL_VALUE amount then ApplyStatBuffMod for POSSTAT/NEGSTAT columns.
     * Stamina → UpdateMaxHealth; intellect → UpdateMaxPower(MANA) for players.
     */
    private static void modStat(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int misc = sp.misc();
        if (misc < -2 || misc > 4) {
            return;
        }
        boolean touchSta = false;
        boolean touchInt = false;
        for (int i = 0; i < MAX_STATS; i++) {
            if (misc >= 0 && misc != i) {
                continue;
            }
            int delta = apply ? amount : -amount;
            int statField = UpdateFields.UNIT_FIELD_STAT0 + i;
            target.setInt(statField, target.getInt(statField) + delta);
            int buffBase = amount > 0
                    ? UpdateFields.UNIT_FIELD_POSSTAT0
                    : UpdateFields.UNIT_FIELD_NEGSTAT0;
            int buffField = buffBase + i;
            target.setInt(buffField, target.getInt(buffField) + delta);
            if (i == 2) {
                touchSta = true;
            }
            if (i == 3) {
                touchInt = true;
            }
        }
        if (target instanceof Player p) {
            if (touchSta) {
                p.recalculateMaxHealthFromStamina();
            }
            if (touchInt) {
                p.recalculateMaxManaFromIntellect();
            }
        }
    }

    /**
     * Aura 73 — CMaNGOS HandleModPowerCost: misc school mask →
     * UNIT_FIELD_POWER_COST_MODIFIER + school += amount.
     */
    private static void modPowerCostSchool(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int mask = sp.misc();
        if (mask == 0) {
            return;
        }
        int delta = apply ? amount : -amount;
        for (int i = 0; i < MAX_SPELL_SCHOOL; i++) {
            if ((mask & (1 << i)) == 0) {
                continue;
            }
            int field = UpdateFields.UNIT_FIELD_POWER_COST_MODIFIER + i;
            target.setInt(field, target.getInt(field) + delta);
        }
    }

    /**
     * Aura 72 — CMaNGOS HandleModPowerCostPCT: school bits in EffectMiscValue,
     * UNIT_FIELD_POWER_COST_MULTIPLIER += amount/100 (signed float).
     */
    private static void modPowerCostSchoolPct(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int mask = sp.misc();
        if (mask == 0) {
            return;
        }
        float delta = (apply ? amount : -amount) / 100.0f;
        for (int i = 0; i < MAX_SPELL_SCHOOL; i++) {
            if ((mask & (1 << i)) == 0) {
                continue;
            }
            int field = UpdateFields.UNIT_FIELD_POWER_COST_MULTIPLIER + i;
            target.setFloat(field, target.getFloat(field) + delta);
        }
    }

    /**
     * Aura 135 — CMaNGOS HandleModHealingDone → UpdateSpellHealingBonus:
     * PLAYER_FIELD_MOD_HEALING_DONE_POS += amount when misc school mask non-zero (client-only).
     */
    private static void modHealingDone(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0 || sp.misc() == 0) {
            return;
        }
        int delta = apply ? amount : -amount;
        target.setInt(UpdateFields.PLAYER_FIELD_MOD_HEALING_DONE_POS,
                target.getInt(UpdateFields.PLAYER_FIELD_MOD_HEALING_DONE_POS) + delta);
    }

    /**
     * Aura 13 — CMaNGOS HandleModDamageDone client fields only:
     * NORMAL → PLAYER_FIELD_MOD_DAMAGE_DONE_POS (or NEG if amount negative);
     * magic schools when EquippedItemClass −1 → POS/NEG+i for bits in misc mask.
     * Skips UNIT_MOD_DAMAGE_* / weapon-dependent / pet scaling (min green).
     */
    private static void modDamageDone(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int mask = sp.misc();
        boolean positive = amount >= 0;
        int abs = Math.abs(amount);
        int signed = apply ? abs : -abs;
        if ((mask & 1) != 0) {
            int field = positive
                    ? UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS
                    : UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_NEG;
            target.setInt(field, target.getInt(field) + signed);
        }
        if ((mask & 0x7E) == 0) {
            return;
        }
        if (sp.equippedItemClass() != -1) {
            return;
        }
        int base = positive
                ? UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS
                : UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_NEG;
        for (int i = 1; i < MAX_SPELL_SCHOOL; i++) {
            if ((mask & (1 << i)) == 0) {
                continue;
            }
            target.setInt(base + i, target.getInt(base + i) + signed);
        }
    }

    /**
     * Aura 79 — CMaNGOS HandleModDamagePercentDone EquippedItemClass −1:
     * NORMAL → PLAYER_FIELD_MOD_DAMAGE_DONE_PCT += amount/100;
     * magic schools → DONE_PCT+i for bits in misc mask.
     */
    private static void modDamagePercentDone(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int mask = sp.misc();
        float delta = (apply ? amount : -amount) / 100.0f;
        if ((mask & 1) != 0) {
            target.setFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT,
                    target.getFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT) + delta);
        }
        if ((mask & 0x7E) == 0) {
            return;
        }
        for (int i = 1; i < MAX_SPELL_SCHOOL; i++) {
            if ((mask & (1 << i)) == 0) {
                continue;
            }
            target.setFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT + i,
                    target.getFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT + i) + delta);
        }
    }

    /**
     * Aura 71 — CMaNGOS HandleModSpellCritChanceShool: misc mask selects schools;
     * player UpdateSpellCritChance(school); creature m_modSpellCritChance[school].
     */
    private static void modSpellCritChanceSchool(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int mask = sp.misc();
        if (mask == 0) {
            return;
        }
        float delta = apply ? amount : -amount;
        for (int i = 0; i < MAX_SPELL_SCHOOL; i++) {
            if ((mask & (1 << i)) == 0) {
                continue;
            }
            if (target instanceof Player) {
                target.setFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + i,
                        target.getFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + i) + delta);
            } else {
                target.adjustSpellCritChance(i, delta);
            }
        }
    }

    /**
     * Aura 68 — CMaNGOS HandleAuraModStalked → UNIT_DYNFLAG_TRACK_UNIT.
     */
    private static void modStalked(Unit target, boolean apply) {
        int dyn = target.getInt(UpdateFields.UNIT_DYNAMIC_FLAGS);
        if (apply) {
            target.setInt(UpdateFields.UNIT_DYNAMIC_FLAGS, dyn | Unit.UNIT_DYNFLAG_TRACK_UNIT);
        } else {
            target.setInt(UpdateFields.UNIT_DYNAMIC_FLAGS, dyn & ~Unit.UNIT_DYNFLAG_TRACK_UNIT);
        }
    }

    /**
     * Aura 92 — CMaNGOS HandlePreventFleeing: while any MOD_FEAR remains,
     * clear fleeing on apply and restore on unapply.
     */
    private static void modPreventFleeing(Unit target, boolean apply) {
        if (target.fearAuraCount() == 0) {
            return;
        }
        target.setFleeing(!apply);
    }

    /**
     * Aura 93 — CMaNGOS HandleModUnattackable → UNIT_FLAG_UNTARGETABLE;
     * CombatStop on apply (Real).
     */
    private static void modUnattackable(Unit target, boolean apply) {
        if (apply) {
            target.combatStop();
        }
        target.setUntargetable(apply);
    }

    /** Aura 120 — CMaNGOS HandleAuraUntrackable → UNIT_VIS_FLAG_UNTRACKABLE. */
    private static void modUntrackable(Unit target, boolean apply) {
        target.setVisFlagUntrackable(apply);
    }

    /**
     * Aura 121 — CMaNGOS HandleAuraEmpathy → UNIT_DYNFLAG_SPECIALINFO
     * (player or CREATURE_TYPE_BEAST; type filter deferred until CreatureTemplate).
     */
    private static void modEmpathy(Unit target, boolean apply) {
        int dyn = target.getInt(UpdateFields.UNIT_DYNAMIC_FLAGS);
        if (apply) {
            target.setInt(UpdateFields.UNIT_DYNAMIC_FLAGS, dyn | Unit.UNIT_DYNFLAG_SPECIALINFO);
        } else {
            target.setInt(UpdateFields.UNIT_DYNAMIC_FLAGS, dyn & ~Unit.UNIT_DYNFLAG_SPECIALINFO);
        }
    }

    /** Aura 151 — CMaNGOS HandleAuraTrackStealthed → PLAYER_FIELD_BYTE_TRACK_STEALTHED. */
    private static void modTrackStealthed(Unit target, boolean apply) {
        if (target instanceof Player player) {
            player.setTrackingStealthed(apply);
        }
    }

    /**
     * Aura 57 — CMaNGOS HandleModSpellCritChance → UpdateAllSpellCritChances (player):
     * flat add to PLAYER_SPELL_CRIT_PERCENTAGE1..+6; creature m_modSpellCritChance[school].
     */
    private static void modSpellCritChanceAura(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        float delta = apply ? amount : -amount;
        if (target instanceof Player) {
            for (int i = 0; i < MAX_SPELL_SCHOOL; i++) {
                target.setFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + i,
                        target.getFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1 + i) + delta);
            }
            return;
        }
        for (int i = 0; i < MAX_SPELL_SCHOOL; i++) {
            target.adjustSpellCritChance(i, delta);
        }
    }

    /**
     * Aura 55 — CMaNGOS HandleModSpellHitChance: m_modSpellHitChance += amount
     * (creature path; player UpdateSpellHitChances same aura sum).
     */
    private static void modSpellHitChance(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        float delta = apply ? amount : -amount;
        target.adjustSpellHitChance(delta);
    }

    /**
     * Aura 54 — CMaNGOS HandleModHitChance EquippedItemClass −1 path:
     * m_modWeaponHitChance[BASE/OFF/RANGED] += amount (creature path; player UpdateWeaponHitChances same sum).
     */
    private static void modHitChance(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        float delta = apply ? amount : -amount;
        target.adjustWeaponHitChance(Unit.BASE_ATTACK, delta);
        target.adjustWeaponHitChance(Unit.OFF_ATTACK, delta);
        target.adjustWeaponHitChance(Unit.RANGED_ATTACK, delta);
    }

    /**
     * Aura 51 — CMaNGOS HandleAuraModBlockPercent → UpdateBlockPercentage (player).
     * Flat add to PLAYER_BLOCK_PERCENTAGE; creature m_modBlockChance later.
     */
    private static void modBlockPercent(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        float delta = apply ? amount : -amount;
        target.setFloat(UpdateFields.PLAYER_BLOCK_PERCENTAGE,
                target.getFloat(UpdateFields.PLAYER_BLOCK_PERCENTAGE) + delta);
    }

    /**
     * Aura 47 — CMaNGOS HandleAuraModParryPercent → UpdateParryPercentage (player).
     * Flat add to PLAYER_PARRY_PERCENTAGE; creature m_modParryChance later.
     */
    private static void modParryPercent(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        float delta = apply ? amount : -amount;
        target.setFloat(UpdateFields.PLAYER_PARRY_PERCENTAGE,
                target.getFloat(UpdateFields.PLAYER_PARRY_PERCENTAGE) + delta);
    }

    /**
     * Aura 49 — CMaNGOS HandleAuraModDodgePercent → UpdateDodgePercentage (player).
     * Flat add to PLAYER_DODGE_PERCENTAGE; creature m_modDodgeChance later.
     */
    private static void modDodgePercent(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        float delta = apply ? amount : -amount;
        target.setFloat(UpdateFields.PLAYER_DODGE_PERCENTAGE,
                target.getFloat(UpdateFields.PLAYER_DODGE_PERCENTAGE) + delta);
    }

    /**
     * Aura 52 — CMaNGOS HandleAuraModCritPercent EquippedItemClass −1 path:
     * HandleBaseModValue(CRIT / OFFHAND / RANGED_CRIT_PERCENTAGE, FLAT_MOD).
     * Weapon-class masks and creature m_modCritChance later.
     */
    private static void modCritPercent(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        float delta = apply ? amount : -amount;
        target.setFloat(UpdateFields.PLAYER_CRIT_PERCENTAGE,
                target.getFloat(UpdateFields.PLAYER_CRIT_PERCENTAGE) + delta);
        target.setFloat(UpdateFields.PLAYER_OFFHAND_CRIT_PERCENTAGE,
                target.getFloat(UpdateFields.PLAYER_OFFHAND_CRIT_PERCENTAGE) + delta);
        target.setFloat(UpdateFields.PLAYER_RANGED_CRIT_PERCENTAGE,
                target.getFloat(UpdateFields.PLAYER_RANGED_CRIT_PERCENTAGE) + delta);
    }

    /**
     * Aura 30 / 98 — CMaNGOS HandleAuraModSkill → ModifySkillBonus(misc=skillId, amount, permanent).
     * 30 = temporary; 98 MOD_SKILL_TALENT = permanent.
     */
    private static void modSkill(Unit target, SpellEngine.SpellInfo sp, boolean apply, boolean permanent) {
        if (!(target instanceof Player player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0 || sp.misc() == 0) {
            return;
        }
        player.modifySkillBonus(sp.misc(), apply ? amount : -amount, permanent);
    }

    /**
     * Aura 80 — CMaNGOS HandleModPercentStat → HandleStatModifier(BASE_PCT) for matching stats.
     * Players only; misc −1 = all, 0–4 = one stat. Sheet uses UNIT_FIELD_STAT0+i percent math.
     */
    private static void modPercentStat(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int misc = sp.misc();
        if (misc < -1 || misc > 4) {
            return;
        }
        for (int i = 0; i < MAX_STATS; i++) {
            if (misc != -1 && misc != i) {
                continue;
            }
            int field = UpdateFields.UNIT_FIELD_STAT0 + i;
            int cur = target.getInt(field);
            float factor = apply ? (100.0f + amount) / 100.0f : 100.0f / (100.0f + amount);
            target.setInt(field, Math.max(0, Math.round(cur * factor)));
        }
    }

    /**
     * Aura 137 — CMaNGOS HandleModTotalPercentStat → TOTAL_PCT on matching stats;
     * player/pet also ApplyStatPercentBuffMod (POSSTAT/NEGSTAT).
     */
    private static void modTotalStatPercentage(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int misc = sp.misc();
        if (misc < -1 || misc > 4) {
            return;
        }
        float factor = apply ? (100.0f + amount) / 100.0f : 100.0f / (100.0f + amount);
        boolean playerOrPet = target instanceof Player;
        for (int i = 0; i < MAX_STATS; i++) {
            if (misc != -1 && misc != i) {
                continue;
            }
            int field = UpdateFields.UNIT_FIELD_STAT0 + i;
            int cur = target.getInt(field);
            target.setInt(field, Math.max(0, Math.round(cur * factor)));
            if (playerOrPet) {
                int pos = UpdateFields.UNIT_FIELD_POSSTAT0 + i;
                int neg = UpdateFields.UNIT_FIELD_NEGSTAT0 + i;
                target.setInt(pos, Math.round(target.getInt(pos) * factor));
                target.setInt(neg, Math.round(target.getInt(neg) * factor));
            }
        }
    }

    /**
     * Auras 34 and 250 — CMaNGOS HandleAuraModIncreaseHealth default → TOTAL_VALUE on UNIT_MOD_HEALTH.
     * Current health stays put; unapply clamps it when max drops below it.
     */
    private static void modIncreaseHealth(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int delta = apply ? amount : -amount;
        int max = target.maxHealth() + delta;
        if (max < 1) {
            max = 1;
        }
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, max);
        if (target.health() > max) {
            target.setHealth(max);
        }
    }

    /**
     * Aura 230 — CMaNGOS HandleAuraModIncreaseMaxHealth. Flat max health, current health keeps its
     * share of the old max ({@code ceil}). Health 0 stays 0. A zero old max is left unscaled.
     */
    private static void modIncreaseMaxHealth(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int oldHealth = target.health();
        int oldMax = target.maxHealth();
        int max = oldMax + (apply ? amount : -amount);
        if (max < 1) {
            max = 1;
        }
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, max);
        if (oldHealth <= 0 || oldMax <= 0) {
            return;
        }
        int next = (int) Math.ceil(max * ((double) oldHealth / oldMax));
        if (next < 1) {
            next = 1;
        }
        target.setHealth(next);
    }

    /**
     * Aura 133 — CMaNGOS HandleAuraModIncreaseHealthPercent → TOTAL_PCT on UNIT_MOD_HEALTH.
     * Multiplies max HP by (100+amount)/100; clamps current when max shrinks.
     */
    private static void modIncreaseHealthPercent(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        float factor = (100.0f + amount) / 100.0f;
        int oldMax = target.maxHealth();
        int max = apply
                ? Math.round(oldMax * factor)
                : Math.round(oldMax / factor);
        if (max < 1) {
            max = 1;
        }
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, max);
        if (target.health() > max) {
            target.setHealth(max);
        }
    }

    /**
     * Aura 35 — CMaNGOS HandleAuraModIncreaseEnergy: misc = Powers, TOTAL_VALUE on that max power.
     * Clamp current power when max drops on unapply.
     */
    private static void modIncreaseEnergy(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int power = sp.misc();
        if (power < 0 || power >= MAX_POWERS) {
            return;
        }
        int delta = apply ? amount : -amount;
        int maxField = UpdateFields.UNIT_FIELD_MAXPOWER1 + power;
        int curField = UpdateFields.UNIT_FIELD_POWER1 + power;
        int max = target.getInt(maxField) + delta;
        if (max < 0) {
            max = 0;
        }
        target.setInt(maxField, max);
        if (target.getInt(curField) > max) {
            target.setInt(curField, max);
        }
    }

    /**
     * Aura 132 — CMaNGOS HandleAuraModIncreaseEnergyPercent → TOTAL_PCT on power max (misc = Powers).
     * Multiplies max power by (100+amount)/100; clamps current when max shrinks.
     */
    private static void modIncreaseEnergyPercent(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int power = sp.misc();
        if (power < 0 || power >= MAX_POWERS) {
            return;
        }
        float factor = (100.0f + amount) / 100.0f;
        int maxField = UpdateFields.UNIT_FIELD_MAXPOWER1 + power;
        int curField = UpdateFields.UNIT_FIELD_POWER1 + power;
        int oldMax = target.getInt(maxField);
        int max = apply
                ? Math.round(oldMax * factor)
                : Math.round(oldMax / factor);
        if (max < 0) {
            max = 0;
        }
        target.setInt(maxField, max);
        if (target.getInt(curField) > max) {
            target.setInt(curField, max);
        }
    }

    /**
     * Aura 33 — CMaNGOS HandleAuraModDecreaseSpeed → UpdateSpeed; amount is EffectBasePoints+1 %.
     * Stacking strongest-negative later (GetMaxNegativeAuraModifier).
     */
    private static void modDecreaseSpeed(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        target.setDecreaseSpeedPct(apply ? amount : 0);
    }

    /**
     * Auras 32 / 130 / 172 — CMaNGOS HandleAuraModIncreaseMountedSpeed → UpdateSpeed(MOVE_RUN).
     * 32 is the mounted main mod; 130 and 172 are the mounted stack and non-stack terms.
     * All three share the mounted percent until the full bonus formula lands. On foot, aura 31 stays the mod.
     */
    private static void modIncreaseMountedSpeed(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        target.setMountedSpeedPct(apply ? amount : 0);
    }

    /**
     * Aura 31 / 129 / 171 — CMaNGOS HandleAuraModIncreaseSpeed → UpdateSpeed;
     * amount EffectBasePoints+1 %. Stacking max-positive later (GetMaxPositiveAuraModifier).
     */
    private static void modIncreaseSpeed(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        target.setIncreaseSpeedPct(apply ? amount : 0);
    }

    /**
     * Aura 58 — CMaNGOS HandleAuraModIncreaseSwimSpeed → UpdateSpeed(MOVE_SWIM);
     * amount EffectBasePoints+1 %.
     */
    private static void modIncreaseSwimSpeed(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        target.setIncreaseSwimSpeedPct(apply ? amount : 0);
    }

    /**
     * Aura 16 — CMaNGOS HandleModStealth → PLAYER_FIELD_BYTES2 stealth + UNIT_VIS_FLAG_CREEP
     * + VISIBILITY_GROUP_STEALTH (UpdateVisibilityAndView via SpellEngine.visibilityUpdater).
     */
    private static void modStealth(Unit target, boolean apply) {
        if (apply) {
            target.addStealthAura();
            if (target instanceof Player player) {
                player.setStealthByte(true);
            }
            target.setVisFlagCreep(true);
            // CMaNGOS: SetVisibility(NO_DETECT) then STEALTH so the first grid pass hides.
            target.setVisibility(Unit.Visibility.GROUP_NO_DETECT);
            target.setVisibility(Unit.Visibility.GROUP_STEALTH);
            return;
        }
        if (!target.removeStealthAura()) {
            return;
        }
        target.setVisFlagCreep(false);
        if (target instanceof Player player) {
            player.setStealthByte(false);
        }
        if (target.invisAuraCount() > 0) {
            target.setVisibility(Unit.Visibility.GROUP_INVISIBILITY);
        } else {
            target.setVisibility(Unit.Visibility.ON);
        }
    }

    /**
     * Aura 18 — CMaNGOS HandleInvisibility → glow byte + VISIBILITY_GROUP_INVISIBILITY when not stealthed.
     */
    private static void modInvisibility(Unit target, boolean apply) {
        if (apply) {
            target.addInvisAura();
            if (target instanceof Player player) {
                player.setInvisibilityGlow(true);
            }
            if (target.visibility() == Unit.Visibility.ON) {
                target.setVisibility(Unit.Visibility.GROUP_INVISIBILITY);
            }
            return;
        }
        if (!target.removeInvisAura()) {
            return;
        }
        if (target instanceof Player player) {
            player.setInvisibilityGlow(false);
        }
        if (target.stealthAuraCount() > 0) {
            target.setVisibility(Unit.Visibility.GROUP_STEALTH);
        } else {
            target.setVisibility(Unit.Visibility.ON);
        }
    }

    /**
     * Aura 45 — CMaNGOS HandleAuraTrackResources → SetFlag/RemoveFlag(PLAYER_TRACK_RESOURCES, 1 &lt;&lt; (misc-1)).
     */
    private static void modTrackResources(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player player)) {
            return;
        }
        int misc = sp.misc();
        if (misc < 1 || misc > 32) {
            return;
        }
        int bit = 1 << (misc - 1);
        int flags = player.getInt(UpdateFields.PLAYER_TRACK_RESOURCES);
        player.setInt(UpdateFields.PLAYER_TRACK_RESOURCES, apply ? flags | bit : flags & ~bit);
    }

    /**
     * Aura 44 — CMaNGOS HandleAuraTrackCreatures → SetFlag/RemoveFlag(PLAYER_TRACK_CREATURES, 1 &lt;&lt; (misc-1)).
     */
    private static void modTrackCreatures(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player player)) {
            return;
        }
        int misc = sp.misc();
        if (misc < 1 || misc > 32) {
            return;
        }
        int bit = 1 << (misc - 1);
        int flags = player.getInt(UpdateFields.PLAYER_TRACK_CREATURES);
        player.setInt(UpdateFields.PLAYER_TRACK_CREATURES, apply ? flags | bit : flags & ~bit);
    }

    /**
     * Aura 95 — CMaNGOS HandleAuraGhost → PLAYER_FLAGS_GHOST + water walk (vis flags later).
     * Keep water walk on unapply when WATER_WALK aura still present — later.
     */
    private static void modGhost(Unit target, boolean apply) {
        if (!(target instanceof Player player)) {
            return;
        }
        player.setGhost(apply);
        player.sendWaterWalk(apply);
    }

    /**
     * Aura 99 — CMaNGOS HandleAuraModAttackPower → HandleStatModifier(UNIT_MOD_ATTACK_POWER, TOTAL_VALUE).
     * Sheet bonus lives in UNIT_FIELD_ATTACK_POWER_MODS (same field ObjectMgr writes for ON_EQUIP 14052).
     * Packed pos/neg TWO_SHORT and full UpdateAttackPowerAndDamage later.
     */
    private static void modAttackPower(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int delta = apply ? amount : -amount;
        target.setInt(UpdateFields.UNIT_FIELD_ATTACK_POWER_MODS,
                target.getInt(UpdateFields.UNIT_FIELD_ATTACK_POWER_MODS) + delta);
    }

    /**
     * Aura 122 — CMaNGOS HandleModOffhandDamagePercent → TOTAL_PCT on UNIT_MOD_DAMAGE_OFFHAND;
     * scales UNIT_FIELD_MIN/MAXOFFHANDDAMAGE by (100+amount)/100.
     */
    private static void modOffhandDamagePct(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        float factor = (100.0f + amount) / 100.0f;
        scaleFloatField(target, UpdateFields.UNIT_FIELD_MINOFFHANDDAMAGE, factor, apply);
        scaleFloatField(target, UpdateFields.UNIT_FIELD_MAXOFFHANDDAMAGE, factor, apply);
    }

    private static void scaleFloatField(Unit target, int field, float factor, boolean apply) {
        float old = target.getFloat(field);
        float neu = apply ? old * factor : old / factor;
        target.setFloat(field, neu);
    }

    /**
     * Aura 123 — CMaNGOS HandleModTargetResistance (client sheet only):
     * NORMAL → PLAYER_FIELD_MOD_TARGET_PHYSICAL_RESISTANCE;
     * full SPELL mask → PLAYER_FIELD_MOD_TARGET_RESISTANCE.
     */
    private static void modTargetResistance(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int delta = apply ? amount : -amount;
        int mask = sp.misc();
        if ((mask & 1) != 0) {
            target.setInt(UpdateFields.PLAYER_FIELD_MOD_TARGET_PHYSICAL_RESISTANCE,
                    target.getInt(UpdateFields.PLAYER_FIELD_MOD_TARGET_PHYSICAL_RESISTANCE) + delta);
        }
        if ((mask & SPELL_SCHOOL_MASK_SPELL) == SPELL_SCHOOL_MASK_SPELL) {
            target.setInt(UpdateFields.PLAYER_FIELD_MOD_TARGET_RESISTANCE,
                    target.getInt(UpdateFields.PLAYER_FIELD_MOD_TARGET_RESISTANCE) + delta);
        }
    }

    /**
     * Aura 166 — CMaNGOS HandleAuraModAttackPowerPercent → TOTAL_PCT;
     * UNIT_FIELD_ATTACK_POWER_MULTIPLIER = TOTAL_PCT − 1 (amount/100 additive for min green).
     */
    private static void modAttackPowerPct(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        float delta = (apply ? amount : -amount) / 100.0f;
        target.setFloat(UpdateFields.UNIT_FIELD_ATTACK_POWER_MULTIPLIER,
                target.getFloat(UpdateFields.UNIT_FIELD_ATTACK_POWER_MULTIPLIER) + delta);
    }

    /**
     * Aura 124 — CMaNGOS HandleAuraModRangedAttackPower; wand-users (priest/mage/warlock) skip.
     */
    private static void modRangedAttackPower(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (target instanceof Player player && player.isWandUser()) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int delta = apply ? amount : -amount;
        target.setInt(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MODS,
                target.getInt(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MODS) + delta);
    }

    /**
     * Aura 167 — CMaNGOS HandleAuraModRangedAttackPowerPercent (wand-users skip);
     * UNIT_FIELD_RANGED_ATTACK_POWER_MULTIPLIER += amount/100.
     */
    private static void modRangedAttackPowerPct(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (target instanceof Player player && player.isWandUser()) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        float delta = (apply ? amount : -amount) / 100.0f;
        target.setFloat(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MULTIPLIER,
                target.getFloat(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MULTIPLIER) + delta);
    }

    /**
     * Aura 66 — CMaNGOS HandleFeignDeath → SetFeignDeath.
     * PLAYER_CONTROLLED: resist chance = max miss vs NPC attackers; failed roll skips CombatStop.
     */
    private static void modFeignDeath(Unit target, boolean apply) {
        if (!apply) {
            target.setFeignDeath(false);
            return;
        }
        target.setFeignDeath(true, feignDeathSucceeds(target));
    }

    /** CMaNGOS HandleFeignDeath roll_chance_f(max CalculateSpellMissChance vs NPC attackers). */
    private static boolean feignDeathSucceeds(Unit target) {
        if ((target.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_PLAYER_CONTROLLED) == 0) {
            return true;
        }
        float resist = 0f;
        for (Unit attacker : target.attackers()) {
            if ((attacker.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_PLAYER_CONTROLLED) != 0) {
                continue;
            }
            float chance = spellMissPercent(target);
            if (chance > resist) {
                resist = chance;
            }
        }
        return ThreadLocalRandom.current().nextFloat() * 100f >= resist;
    }

    /** CMaNGOS CalculateSpellMissChance base 4%, hit auras, Pre-WotLK floor 1%. */
    private static float spellMissPercent(Unit caster) {
        float pct = 4f - caster.spellHitChance();
        if (pct < 1f) {
            pct = 1f;
        }
        if (pct > 100f) {
            pct = 100f;
        }
        return pct;
    }

    /**
     * Aura 144 — CMaNGOS HandleAuraSafeFall (fall damage in HandleMovementOpcodes uses
     * GetTotalAuraModifier). Accumulate yards reduced from fall height.
     */
    private static void modSafeFall(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        target.addSafeFall(apply ? amount : -amount);
    }

    /**
     * Aura 158 — CMaNGOS HandleShieldBlockValue FLAT_MOD → PLAYER_SHIELD_BLOCK.
     */
    private static void modShieldBlockValue(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        int delta = apply ? amount : -amount;
        target.setInt(UpdateFields.PLAYER_SHIELD_BLOCK,
                target.getInt(UpdateFields.PLAYER_SHIELD_BLOCK) + delta);
    }

    /**
     * Aura 150 — CMaNGOS HandleShieldBlockValue PCT_MOD → PLAYER_SHIELD_BLOCK × (100+amount)/100.
     */
    private static void modShieldBlockValuePct(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        float factor = (100.0f + amount) / 100.0f;
        int old = target.getInt(UpdateFields.PLAYER_SHIELD_BLOCK);
        int neu = apply ? Math.round(old * factor) : Math.round(old / factor);
        target.setInt(UpdateFields.PLAYER_SHIELD_BLOCK, neu);
    }

    /**
     * Aura 9 — CMaNGOS HandleModAttackSpeed → ApplyAttackTimePercentMod(BASE_ATTACK) only.
     */
    private static void modAttackSpeed(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        applyAttackTimePercentMod(target, UpdateFields.UNIT_FIELD_BASEATTACKTIME, amount, apply);
    }

    /**
     * Aura 138 — CMaNGOS HandleModMeleeSpeedPct → ApplyAttackTimePercentMod(BASE+OFF).
     * Attack times stay int ms (Combat); percent math mirrors ApplyPercentModFloatValue.
     */
    private static void modMeleeHaste(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        applyAttackTimePercentMod(target, UpdateFields.UNIT_FIELD_BASEATTACKTIME, amount, apply);
        applyAttackTimePercentMod(target, UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1, amount, apply);
    }

    /**
     * Aura 141 — CMaNGOS HandleRangedAmmoHaste. Players only; the highest positive amount
     * is the one applied to ranged attack time. EquippedItemClass other than -1 needs a
     * weapon fit check, which is not wired, so those spells do nothing.
     */
    private static void modRangedAmmoHaste(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player player) || sp.equippedItemClass() != -1) {
            return;
        }
        int oldHighest = player.highestAmmoMod();
        player.noteAmmoMod(sp.id(), (sp.minDmg() + sp.maxDmg()) / 2, apply);
        int newHighest = player.highestAmmoMod();
        if (oldHighest == newHighest) {
            return;
        }
        if (oldHighest > 0) {
            applyAttackTimePercentMod(target, UpdateFields.UNIT_FIELD_RANGEDATTACKTIME, oldHighest, false);
        }
        if (newHighest > 0) {
            applyAttackTimePercentMod(target, UpdateFields.UNIT_FIELD_RANGEDATTACKTIME, newHighest, true);
        }
    }

    /**
     * Aura 110 — CMaNGOS HandleModPowerRegenPCT. Mana scales the spirit regen fields;
     * energy scales Regenerate(POWER_ENERGY).
     */
    private static void modPowerRegenPercent(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player player)) {
            return;
        }
        player.setPowerRegenPercent(sp.id(), sp.misc(), (sp.minDmg() + sp.maxDmg()) / 2, apply);
    }

    /** Aura 191 — CMaNGOS HandleAuraModUseNormalSpeed → UpdateSpeed run, swim, and flight. */
    private static void modUseNormalMovementSpeed(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        target.setUseNormalMovementSpeed(sp.id(), (sp.minDmg() + sp.maxDmg()) / 2, apply);
    }

    /** Aura 189 — CMaNGOS HandleModRating → ApplyRatingMod for each misc bit. */
    private static void modRating(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player player)) {
            return;
        }
        player.applyRatingMod(sp.misc(), (sp.minDmg() + sp.maxDmg()) / 2, apply);
    }

    /** Aura 240 — CMaNGOS HandleAuraModExpertise → UpdateExpertise(BASE) and OFF. */
    private static void modExpertise(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player player) || sp.equippedItemClass() != -1) {
            return;
        }
        player.applyExpertise((sp.minDmg() + sp.maxDmg()) / 2, apply);
    }

    /**
     * Aura 237 — SpellBaseDamageBonusDone attack-power term, written per school bit
     * onto PLAYER_FIELD_MOD_DAMAGE_DONE_POS. Players only.
     */
    private static void modSpellDamageOfAttackPower(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player) || sp.misc() == 0) {
            return;
        }
        int bonus = attackPowerSpellBonus(target, sp);
        int delta = apply ? bonus : -bonus;
        for (int school = 0; school < MAX_SPELL_SCHOOL; school++) {
            if ((sp.misc() & (1 << school)) == 0) {
                continue;
            }
            target.setInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + school,
                    target.getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + school) + delta);
        }
    }

    /**
     * Aura 238 — SpellBaseHealingBonusDone attack-power term. Any school bit against
     * SPELL_SCHOOL_MASK_ALL adds the bonus once to PLAYER_FIELD_MOD_HEALING_DONE_POS.
     */
    private static void modSpellHealingOfAttackPower(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player) || (sp.misc() & ((1 << MAX_SPELL_SCHOOL) - 1)) == 0) {
            return;
        }
        int bonus = attackPowerSpellBonus(target, sp);
        int delta = apply ? bonus : -bonus;
        target.setInt(UpdateFields.PLAYER_FIELD_MOD_HEALING_DONE_POS,
                target.getInt(UpdateFields.PLAYER_FIELD_MOD_HEALING_DONE_POS) + delta);
    }

    /** CMaNGOS GetTotalAttackPowerValue(BASE_ATTACK) * amount / 100, truncated. */
    private static int attackPowerSpellBonus(Unit target, SpellEngine.SpellInfo sp) {
        int mods = target.getInt(UpdateFields.UNIT_FIELD_ATTACK_POWER_MODS);
        int pos = (short) (mods & 0xFFFF);
        int neg = (short) (mods >> 16);
        int ap = target.getInt(UpdateFields.UNIT_FIELD_ATTACK_POWER) + pos + neg;
        if (ap < 0) {
            ap = 0;
        }
        float total = ap * (1f + target.getFloat(UpdateFields.UNIT_FIELD_ATTACK_POWER_MULTIPLIER));
        return (int) (total * ((sp.minDmg() + sp.maxDmg()) / 2) / 100f);
    }

    /**
     * Auras 206–211 — CMaNGOS HandleAuraModIncreaseFlightSpeed → UpdateSpeed(MOVE_FLIGHT).
     * Aura 207 also SetCanFly. Polymorph immunity on the flying mount stays later.
     */
    private static void modFlightSpeed(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (sp.aura() == SPELL_AURA_MOD_FLIGHT_SPEED_MOUNTED) {
            target.sendCanFly(apply);
        }
        target.setFlightSpeedAura(sp.aura(), sp.id(), (sp.minDmg() + sp.maxDmg()) / 2, apply);
    }

    /** Aura 215 — CMaNGOS HandleArenaPreparation. Fills health, mana, and energy; clears rage on remove. */
    private static void modArenaPreparation(Unit target, boolean apply) {
        int flags = target.getInt(UpdateFields.UNIT_FIELD_FLAGS);
        target.setInt(UpdateFields.UNIT_FIELD_FLAGS,
                apply ? flags | Unit.UNIT_FLAG_PREPARATION : flags & ~Unit.UNIT_FLAG_PREPARATION);
        if (apply) {
            target.setHealth(target.maxHealth());
            target.setPower(target.maxPower());
            target.setInt(UpdateFields.UNIT_FIELD_POWER4, target.getInt(UpdateFields.UNIT_FIELD_MAXPOWER4));
        } else {
            target.setInt(UpdateFields.UNIT_FIELD_POWER2, 0);
        }
    }

    /** Aura 241 — CMaNGOS HandleForceMoveForward → UNIT_FLAG2_FORCE_MOVE. */
    private static void modForceMoveForward(Unit target, boolean apply) {
        int flags = target.getInt(UpdateFields.UNIT_FIELD_FLAGS_2);
        target.setInt(UpdateFields.UNIT_FIELD_FLAGS_2,
                apply ? flags | Unit.UNIT_FLAG2_FORCE_MOVE : flags & ~Unit.UNIT_FLAG2_FORCE_MOVE);
    }

    /** Aura 244 — CMaNGOS HandleComprehendLanguage → UNIT_FLAG2_COMPREHEND_LANG. */
    private static void modComprehendLanguage(Unit target, boolean apply) {
        int flags = target.getInt(UpdateFields.UNIT_FIELD_FLAGS_2);
        target.setInt(UpdateFields.UNIT_FIELD_FLAGS_2,
                apply ? flags | Unit.UNIT_FLAG2_COMPREHEND_LANG : flags & ~Unit.UNIT_FLAG2_COMPREHEND_LANG);
    }

    /**
     * Auras 140 and 218 — CMaNGOS HandleAuraModRangedHaste → ApplyAttackTimePercentMod(RANGED_ATTACK).
     */
    private static void modRangedHaste(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        applyAttackTimePercentMod(target, UpdateFields.UNIT_FIELD_RANGEDATTACKTIME, amount, apply);
    }

    /** CMaNGOS Unit::ApplyAttackTimePercentMod without attack-timer / m_modAttackSpeedPct side effects. */
    private static void applyAttackTimePercentMod(Unit target, int field, int amount, boolean apply) {
        int cur = target.getInt(field);
        if (cur <= 0) {
            return;
        }
        float val = amount;
        float factor;
        if (val > 0) {
            float v = val;
            boolean pctApply = !apply;
            factor = pctApply ? (100.0f + v) / 100.0f : 100.0f / (100.0f + v);
        } else {
            float v = -val;
            factor = apply ? (100.0f + v) / 100.0f : 100.0f / (100.0f + v);
        }
        int next = Math.round(cur * factor);
        target.setInt(field, Math.max(1, next));
    }

    /**
     * Aura 82 — CMaNGOS HandleWaterBreathing → infinite breath (multiplier 0).
     * Unapply restores the MOD_WATER_BREATHING multiplier, or 1.0 when that aura is absent.
     */
    private static void modWaterBreathing(Unit target, boolean apply) {
        if (!(target instanceof Player player)) {
            return;
        }
        player.setWaterBreathingInfinite(apply);
    }

    /**
     * Aura 155 — CMaNGOS HandleModWaterBreathing → GetTotalAuraMultiplier.
     * One aura: multiplier (100+amount)/100. Infinite breath from aura 82 wins.
     */
    private static void modWaterBreathingPct(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        player.setWaterBreathingModPct(apply ? amount : 0);
    }

    /**
     * Aura 134 — CMaNGOS HandleAuraModRegenInterrupt → Player::UpdateManaRegen.
     * Interrupt regen is the spirit rate times the aura percent (Meditation).
     */
    private static void modManaRegenInterrupt(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        player.setManaRegenInterruptPct(apply ? amount : 0);
    }

    /**
     * Aura 143 — CMaNGOS HandleAuraModResistanceExclusive: per school, the highest amount wins.
     */
    private static void modResistanceExclusive(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount <= 0 || sp.misc() == 0) {
            return;
        }
        target.setExclusiveResistance(sp.id(), sp.misc(), amount, apply);
    }

    /** Aura 170 — CMaNGOS HandleDetectAmore → PLAYER_FIELD_BYTES2 byte 1, flag {@code 1 << misc}. */
    private static void modDetectAmore(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player player)) {
            return;
        }
        player.setDetectAmore(sp.misc(), apply);
    }

    /** Aura 154 — CMaNGOS HandleModStealthLevel → AddStealthStrength(StealthType). */
    private static void modStealthLevel(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        target.addStealthStrength(sp.misc(), apply ? amount : -amount);
    }

    /**
     * Aura 19 — CMaNGOS HandleInvisibilityDetect. Spell 44855's phase-mask hack has no phase
     * field here; the detect value and mask still apply.
     */
    private static void modInvisibilityDetect(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        target.setInvisibilityDetect(sp.id(), sp.misc(), amount, apply);
    }

    /** Aura 17 — CMaNGOS HandleModStealthDetect → AddStealthDetectionStrength. */
    private static void modStealthDetect(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        target.addStealthDetect(apply ? amount : -amount);
    }

    /**
     * Aura 192 — CMaNGOS HandleModMeleeRangedSpeedPct → BASE, OFF, and RANGED attack time.
     * Bloodlust exclusive-max later.
     */
    private static void modMeleeRangedHaste(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        applyAttackTimePercentMod(target, UpdateFields.UNIT_FIELD_BASEATTACKTIME, amount, apply);
        applyAttackTimePercentMod(target, UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1, amount, apply);
        applyAttackTimePercentMod(target, UpdateFields.UNIT_FIELD_RANGEDATTACKTIME, amount, apply);
    }

    /**
     * Aura 193 — CMaNGOS HandleModCombatSpeedPct → cast time plus BASE, OFF, and RANGED.
     */
    private static void modHasteAll(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        modCastingSpeed(target, sp, apply);
        modMeleeRangedHaste(target, sp, apply);
    }

    /**
     * Aura 65 — CMaNGOS HandleModCastingSpeed → ApplyCastTimePercentMod(amount, apply).
     * Positive amount: ApplyPercentModFloatValue(UNIT_MOD_CAST_SPEED, amount, !apply).
     * Bloodlust/Heroism exclusive max later.
     */
    private static void modCastingSpeed(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        float val = amount;
        if (val > 0) {
            target.applyPercentModFloatValue(UpdateFields.UNIT_MOD_CAST_SPEED, val, !apply);
        } else {
            target.applyPercentModFloatValue(UpdateFields.UNIT_MOD_CAST_SPEED, -val, apply);
        }
    }

    /**
     * Auras 61 and 239 — CMaNGOS HandleAuraModScale:
     * {@code max(0.1, (100+sum61)/100 * (100+sum239)/100)}. A zero amount leaves the stored sum alone.
     */
    private static void modScale(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        target.setScaleAura(sp.aura() == SPELL_AURA_MOD_SCALE_2, sp.id(), amount, apply);
    }

    /** Aura 219 — players only. {@code stat * amount / 500} added to both mana regen fields, not × aura 110. */
    private static void modManaRegenFromStat(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player player)) {
            return;
        }
        int stat = sp.misc();
        if (stat < 0 || stat >= MAX_STATS) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        player.setManaRegenFromStat(sp.id(), stat, amount, apply);
    }

    /**
     * Aura 10 — CMaNGOS HandleModThreat. Percent-mod {@code m_threatModifier[school]} for each misc bit.
     * Dead targets are skipped. Spell-id level scaling (26400 and the Defiance talents) stays later.
     */
    private static void modThreat(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!target.alive()) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0 || sp.misc() == 0) {
            return;
        }
        target.applyThreatPercent(sp.misc(), amount, apply);
    }

    /** Aura 39 — ApplySpellImmune(IMMUNITY_SCHOOL) and UNIT_FLAG_IMMUNE while any such aura remains. */
    private static void modSchoolImmunity(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        target.setSchoolImmunity(sp.id(), sp.misc(), apply);
    }

    /** Aura 77 — ApplySpellImmune(IMMUNITY_MECHANIC, misc). Purge and trinket specials stay later. */
    private static void modMechanicImmunity(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        target.setMechanicImmunity(sp.id(), sp.misc(), apply);
    }

    /** Aura 37 — ApplySpellImmune(IMMUNITY_EFFECT, misc). */
    private static void modEffectImmunity(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        target.setEffectImmunity(sp.id(), sp.misc(), apply);
    }

    /** Aura 40 — ApplySpellImmune(IMMUNITY_DAMAGE, misc). Threat suppression stays later. */
    private static void modDamageImmunity(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        target.setDamageImmunity(sp.id(), sp.misc(), apply);
    }

    /** Aura 38 — ApplySpellImmune(IMMUNITY_STATE, misc). The purge attribute stays later. */
    private static void modStateImmunity(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        target.setStateImmunity(sp.id(), sp.misc(), apply);
    }

    /** Aura 41 — ApplySpellDispelImmunity. Removing auras of that dispel type stays later. */
    private static void modDispelImmunity(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        target.setDispelImmunity(sp.id(), sp.misc(), apply);
    }

    /**
     * Aura 147 — the mask checked by IsImmuneToSpell ({@code mask & (1 << mechanic)}).
     * The purge attribute stays later. This is separate from aura 77's single mechanic id.
     */
    private static void modMechanicImmunityMask(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        target.setMechanicImmunityMask(sp.id(), sp.misc(), apply);
    }

    /**
     * Aura 261 — SetPhaseMask(apply ? misc : 1) on the target and, when the target is a player
     * with a pet, on that pet. Unapply writes 1 even if another phase aura is still up.
     */
    private static void modPhase(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int mask = apply ? sp.misc() : 1;
        target.setPhaseMask(mask);
        if (target instanceof Player player && player.pet != null) {
            player.pet.asUnit().setPhaseMask(mask);
        }
    }

    /**
     * Aura 212 — players who are not wand users. {@code (int)(stat * amount / 100)} is added to
     * UNIT_FIELD_RANGED_ATTACK_POWER_MODS without replacing flat aura 124.
     */
    private static void modRangedAttackPowerOfStatPercent(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player player) || player.isWandUser()) {
            return;
        }
        int stat = sp.misc();
        if (stat < 0 || stat >= MAX_STATS) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        player.setRangedAttackPowerOfStatPercent(sp.id(), stat, amount, apply);
    }

    /**
     * Aura 176 — damage immunity 255, stand if seated, fill mana. Unapply sets health to 0
     * (spell 27965 is not in the catalog). Cast interrupts stay later.
     */
    private static void modSpiritOfRedemption(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        target.setDamageImmunity(sp.id(), 255, apply);
        if (!apply) {
            target.setHealth(0);
            return;
        }
        if (target instanceof Player player && !player.isStanding()) {
            player.stand();
        }
        target.setInt(UpdateFields.UNIT_FIELD_POWER1, target.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1));
    }

    /** Aura 139 — players only. Faction id is misc, rank is the amount. Packets and stop-attack stay later. */
    private static void modForceReaction(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player player)) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        player.reputations.applyForceReaction(sp.misc(), amount, apply);
    }

    /**
     * Auras 107 and 108 — players only, misc is the spell mod op. Charge spells and spell-family
     * masks stay later; {@link Player#applySpellMod} sums every stored mod of that op.
     */
    private static void modSpellModifier(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (!(target instanceof Player player)) {
            return;
        }
        if (sp.misc() < 0 || sp.misc() >= MAX_SPELLMOD) {
            return;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount == 0) {
            return;
        }
        player.setSpellMod(sp.id(), sp.misc(), sp.aura(), amount, apply);
    }

    /**
     * Aura 112 — RegisterOverrideScriptAura keyed by the script id. A later aura with the same
     * id replaces the first, and any unapply of that id clears it.
     */
    private static void modOverrideClassScript(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        target.setOverrideClassScript(sp.misc(), apply);
    }

    private static void modUninteractible(Unit target, boolean apply) {
        int flags = target.getInt(UpdateFields.UNIT_FIELD_FLAGS);
        if (apply) {
            target.setInt(UpdateFields.UNIT_FIELD_FLAGS, flags | Unit.UNIT_FLAG_UNINTERACTIBLE);
        } else {
            target.setInt(UpdateFields.UNIT_FIELD_FLAGS, flags & ~Unit.UNIT_FLAG_UNINTERACTIBLE);
        }
    }

    /**
     * PeriodicTick for auras 8, 20, 21, 24, and 89. Returns true when {@code sp} is one of those
     * auras. Heals (8 and 20) do nothing without a caster. Armor, school immunity, and bonus
     * healing stay later. Spell 40409's flag is applied in {@link #apply}, not here.
     */
    public boolean tickCatalogPeriodic(Unit caster, Unit target, SpellEngine.SpellInfo sp,
                                       BiConsumer<Integer, byte[]> send) {
        if (target == null || sp == null) {
            return false;
        }
        int aura = sp.aura();
        if (aura != SPELL_AURA_PERIODIC_HEAL && aura != SPELL_AURA_OBS_MOD_HEALTH
                && aura != SPELL_AURA_OBS_MOD_MANA && aura != SPELL_AURA_PERIODIC_ENERGIZE
                && aura != SPELL_AURA_PERIODIC_DAMAGE_PERCENT) {
            return false;
        }
        if (!target.alive()) {
            return true;
        }
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (amount <= 0) {
            return true;
        }
        if (aura == SPELL_AURA_PERIODIC_HEAL || aura == SPELL_AURA_OBS_MOD_HEALTH) {
            if (caster == null || target.health() == target.maxHealth()) {
                return true;
            }
            int gain = aura == SPELL_AURA_OBS_MOD_HEALTH ? target.maxHealth() * amount / 100 : amount;
            if (gain <= 0) {
                return true;
            }
            target.setHealth(target.health() + gain);
            sendField(target, UpdateFields.UNIT_FIELD_HEALTH, send);
            return true;
        }
        if (aura == SPELL_AURA_OBS_MOD_MANA) {
            int max = target.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1);
            if (max == 0) {
                return true;
            }
            int gain = max * amount / 100;
            addPower(target, UpdateFields.UNIT_FIELD_POWER1, max, gain, send);
            return true;
        }
        if (aura == SPELL_AURA_PERIODIC_ENERGIZE) {
            int power = sp.misc();
            if (power < 0 || power >= MAX_POWERS) {
                return true;
            }
            int max = target.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1 + power);
            if (max == 0) {
                return true;
            }
            addPower(target, UpdateFields.UNIT_FIELD_POWER1 + power, max, amount, send);
            return true;
        }
        int damage = target.maxHealth() * amount / 100;
        if (damage <= 0) {
            return true;
        }
        target.setHealth(target.health() - damage);
        sendField(target, UpdateFields.UNIT_FIELD_HEALTH, send);
        return true;
    }

    private static void addPower(Unit target, int field, int max, int gain, BiConsumer<Integer, byte[]> send) {
        if (gain <= 0) {
            return;
        }
        int cur = target.getInt(field);
        int next = Math.min(max, cur + gain);
        if (next == cur) {
            return;
        }
        target.setInt(field, next);
        sendField(target, field, send);
    }

    private static void sendField(Unit target, int field, BiConsumer<Integer, byte[]> send) {
        if (send == null) {
            return;
        }
        var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(target, field));
        send.accept(upd.opcode(), upd.payload());
    }

    /**
     * Aura 243 — CMaNGOS HandleFactionOverride. misc ≤ 0 is a missing template and does nothing.
     * Unapply restores the faction saved on the first apply, even if another override is still up.
     */
    private static void modFactionOverride(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        if (sp.misc() <= 0) {
            return;
        }
        target.overrideFaction(sp.misc(), apply);
    }

    /**
     * Aura 26 — CMaNGOS HandleAuraModRoot / SetImmobilizedState with HasAuraType stacking.
     * Apply always SendMoveRoot (order counter); unapply clears only when no MOD_ROOT remain.
     */
    private static void modRoot(Unit target, boolean apply) {
        if (apply) {
            target.addRootAura();
            target.sendMoveRoot(true);
        } else if (target.removeRootAura()) {
            target.sendMoveRoot(false);
        }
    }

    /** Aura 12 — CMaNGOS HandleAuraModStun / SetStunned with HasAuraType stacking. */
    private static void modStun(Unit target) {
        target.addStunAura();
        immobilize(target);
        target.setStunned(true);
    }

    /**
     * CMaNGOS Unit::SetImmobilizedState → add UNIT_STAT_ROOT then SendMoveRoot(true).
     * Root itself has no UNIT_FIELD_FLAGS bit; players get SMSG_FORCE_MOVE_ROOT, other units
     * SMSG_SPLINE_MOVE_ROOT (Unit.cpp). Players' SendMoveRoot does not set unit state.
     */
    private static void immobilize(Unit target) {
        target.setRooted(true);
        target.sendMoveRoot(true);
    }
}
