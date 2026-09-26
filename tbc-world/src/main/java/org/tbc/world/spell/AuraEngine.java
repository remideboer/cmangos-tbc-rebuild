package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.Set;

/**
 * SPELL_AURA_* modifier catalog (CMaNGOS Aura::ApplyModifier → AuraHandler[auraName]).
 * SpellEngine records the aura on the unit; this type applies the named modifier.
 * spell-algorithms.md "Auras 0–261".
 */
public final class AuraEngine {
    public static final int SPELL_AURA_MOD_STUN = 12;
    public static final int SPELL_AURA_MOD_CONFUSE = 5;
    public static final int SPELL_AURA_MOD_FEAR = 7;
    public static final int SPELL_AURA_MOD_RESISTANCE = 22;
    public static final int SPELL_AURA_MOD_PACIFY = 25;
    public static final int SPELL_AURA_MOD_ROOT = 26;
    public static final int SPELL_AURA_MOD_SILENCE = 27;
    public static final int SPELL_AURA_MOD_STAT = 29;
    public static final int SPELL_AURA_MOD_STEALTH = 16;
    public static final int SPELL_AURA_MOD_INVISIBILITY = 18;
    public static final int SPELL_AURA_TRACK_CREATURES = 44;
    public static final int SPELL_AURA_TRACK_RESOURCES = 45;
    public static final int SPELL_AURA_MOD_INCREASE_SPEED = 31;
    public static final int SPELL_AURA_MOD_DECREASE_SPEED = 33;
    public static final int SPELL_AURA_MOD_INCREASE_SWIM_SPEED = 58;
    public static final int SPELL_AURA_MOD_INCREASE_HEALTH = 34;
    public static final int SPELL_AURA_MOD_INCREASE_ENERGY = 35;
    public static final int SPELL_AURA_MOD_SHAPESHIFT = 36;
    public static final int SPELL_AURA_MOD_PACIFY_SILENCE = 60;
    public static final int SPELL_AURA_MOD_CRIT_PERCENT = 52;
    public static final int SPELL_AURA_MOD_SCALE = 61;
    public static final int SPELL_AURA_MOD_CASTING_SPEED_NOT_STACK = 65;
    public static final int SPELL_AURA_FEIGN_DEATH = 66;
    public static final int SPELL_AURA_MOD_DISARM = 67;
    public static final int SPELL_AURA_MOD_PERCENT_STAT = 80;
    public static final int SPELL_AURA_WATER_BREATHING = 82;
    public static final int SPELL_AURA_GHOST = 95;
    public static final int SPELL_AURA_MOD_ATTACK_POWER = 99;
    public static final int SPELL_AURA_WATER_WALK = 104;
    public static final int SPELL_AURA_FEATHER_FALL = 105;
    public static final int SPELL_AURA_HOVER = 106;
    public static final int SPELL_AURA_MOD_RANGED_ATTACK_POWER = 124;
    public static final int SPELL_AURA_MOD_MELEE_HASTE = 138;
    public static final int SPELL_AURA_MOD_RANGED_HASTE = 140;
    public static final int SPELL_AURA_SAFE_FALL = 144;
    /** SpellSchools.h MAX_SPELL_SCHOOL — normal through arcane. */
    public static final int MAX_SPELL_SCHOOL = 7;
    /** SharedDefines.h MAX_STATS — strength through spirit. */
    public static final int MAX_STATS = 5;
    /** SharedDefines.h MAX_POWERS — mana through runic (TBC: 5). */
    public static final int MAX_POWERS = 5;

    private static final Set<Integer> KNOWN_AURAS = Set.of(
            SPELL_AURA_MOD_CONFUSE, SPELL_AURA_MOD_FEAR, SPELL_AURA_MOD_STUN, SPELL_AURA_MOD_RESISTANCE,
            SPELL_AURA_MOD_PACIFY, SPELL_AURA_MOD_ROOT, SPELL_AURA_MOD_SILENCE, SPELL_AURA_MOD_STAT,
            SPELL_AURA_MOD_STEALTH, SPELL_AURA_MOD_INVISIBILITY,
            SPELL_AURA_TRACK_CREATURES, SPELL_AURA_TRACK_RESOURCES,
            SPELL_AURA_MOD_CRIT_PERCENT,
            SPELL_AURA_MOD_INCREASE_SPEED,
            SPELL_AURA_MOD_DECREASE_SPEED, SPELL_AURA_MOD_INCREASE_SWIM_SPEED,
            SPELL_AURA_MOD_INCREASE_HEALTH, SPELL_AURA_MOD_INCREASE_ENERGY,
            SPELL_AURA_MOD_SHAPESHIFT, SPELL_AURA_MOD_PACIFY_SILENCE, SPELL_AURA_MOD_SCALE,
            SPELL_AURA_MOD_CASTING_SPEED_NOT_STACK, SPELL_AURA_FEIGN_DEATH, SPELL_AURA_MOD_DISARM,
            SPELL_AURA_MOD_PERCENT_STAT, SPELL_AURA_WATER_BREATHING,
            SPELL_AURA_GHOST, SPELL_AURA_MOD_ATTACK_POWER, SPELL_AURA_MOD_RANGED_ATTACK_POWER,
            SPELL_AURA_WATER_WALK, SPELL_AURA_FEATHER_FALL, SPELL_AURA_HOVER,
            SPELL_AURA_MOD_MELEE_HASTE, SPELL_AURA_MOD_RANGED_HASTE, SPELL_AURA_SAFE_FALL);

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
        if (sp.aura() == SPELL_AURA_MOD_FEAR) {
            // HandleModFear(true) → SetFleeing(true).
            target.setFleeing(true);
        }
        if (sp.aura() == SPELL_AURA_MOD_CONFUSE) {
            // HandleModConfuse(true) → SetConfused(true).
            target.setConfused(true);
        }
        if (sp.aura() == SPELL_AURA_MOD_DISARM) {
            target.setDisarmed(true);
        }
        if (sp.aura() == SPELL_AURA_MOD_STEALTH) {
            modStealth(target, true);
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
        if (sp.aura() == SPELL_AURA_MOD_SCALE) {
            modScale(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_CASTING_SPEED_NOT_STACK) {
            modCastingSpeed(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_FEIGN_DEATH) {
            modFeignDeath(target, true);
        }
        if (sp.aura() == SPELL_AURA_WATER_BREATHING) {
            modWaterBreathing(target, true);
        }
        if (sp.aura() == SPELL_AURA_GHOST) {
            modGhost(target, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_ATTACK_POWER) {
            modAttackPower(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_RANGED_ATTACK_POWER) {
            modRangedAttackPower(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_MELEE_HASTE) {
            modMeleeHaste(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_RANGED_HASTE) {
            modRangedHaste(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_SAFE_FALL) {
            modSafeFall(target, sp, true);
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
            immobilize(target);
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
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_SPEED) {
            modIncreaseSpeed(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_SWIM_SPEED) {
            modIncreaseSwimSpeed(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_SHAPESHIFT) {
            target.setShapeshiftForm(sp.misc());
        }
        if (sp.aura() == SPELL_AURA_MOD_RESISTANCE) {
            modResistance(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_STAT) {
            modStat(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_PERCENT_STAT) {
            modPercentStat(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_CRIT_PERCENT) {
            modCritPercent(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_HEALTH) {
            modIncreaseHealth(target, sp, true);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_ENERGY) {
            modIncreaseEnergy(target, sp, true);
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
        if (sp.aura() == SPELL_AURA_MOD_STAT) {
            modStat(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_PERCENT_STAT) {
            modPercentStat(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_CRIT_PERCENT) {
            modCritPercent(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_HEALTH) {
            modIncreaseHealth(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_ENERGY) {
            modIncreaseEnergy(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_STUN) {
            // HandleAuraModStun(false) → SetStunned(false) → clear flag + SetImmobilizedState(false).
            // Stacking other MOD_STUN auras later (HasAuraType check).
            target.setStunned(false);
            target.setRooted(false);
            target.sendMoveRoot(false);
        }
        if (sp.aura() == SPELL_AURA_MOD_FEAR) {
            // HandleModFear(false) → SetFleeing(false); stacking other MOD_FEAR later.
            target.setFleeing(false);
        }
        if (sp.aura() == SPELL_AURA_MOD_CONFUSE) {
            // HandleModConfuse(false) → SetConfused(false); stacking other MOD_CONFUSE later.
            target.setConfused(false);
        }
        if (sp.aura() == SPELL_AURA_MOD_DISARM) {
            // HandleAuraModDisarm(false); stacking other MOD_DISARM later.
            target.setDisarmed(false);
        }
        if (sp.aura() == SPELL_AURA_MOD_STEALTH) {
            modStealth(target, false);
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
        if (sp.aura() == SPELL_AURA_MOD_SCALE) {
            modScale(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_CASTING_SPEED_NOT_STACK) {
            modCastingSpeed(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_FEIGN_DEATH) {
            modFeignDeath(target, false);
        }
        if (sp.aura() == SPELL_AURA_WATER_BREATHING) {
            modWaterBreathing(target, false);
        }
        if (sp.aura() == SPELL_AURA_GHOST) {
            modGhost(target, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_ATTACK_POWER) {
            modAttackPower(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_RANGED_ATTACK_POWER) {
            modRangedAttackPower(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_MELEE_HASTE) {
            modMeleeHaste(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_RANGED_HASTE) {
            modRangedHaste(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_SAFE_FALL) {
            modSafeFall(target, sp, false);
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
            // HandleAuraModRoot(false) → SetImmobilizedState(false); stacking other roots later.
            target.setRooted(false);
            target.sendMoveRoot(false);
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
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_SPEED) {
            modIncreaseSpeed(target, sp, false);
        }
        if (sp.aura() == SPELL_AURA_MOD_INCREASE_SWIM_SPEED) {
            modIncreaseSwimSpeed(target, sp, false);
        }
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
     * Aura 29 — CMaNGOS HandleAuraModStat: misc is stat index (0–4) or &lt; 0 for all stats;
     * TOTAL_VALUE amount then ApplyStatBuffMod for POSSTAT/NEGSTAT columns.
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
        }
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
     * Aura 34 — CMaNGOS HandleAuraModIncreaseHealth default → HandleStatModifier(UNIT_MOD_HEALTH).
     * Clamp current health when max drops below it on unapply.
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
     * Aura 31 — CMaNGOS HandleAuraModIncreaseSpeed → UpdateSpeed; amount EffectBasePoints+1 %.
     * Stacking max-positive later (GetMaxPositiveAuraModifier).
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
     * Aura 16 — CMaNGOS HandleModStealth → player PLAYER_FIELD_BYTES2 stealth byte.
     * Visibility group / UNIT_VIS_FLAG_CREEP later.
     */
    private static void modStealth(Unit target, boolean apply) {
        if (target instanceof Player player) {
            player.setStealthByte(apply);
        }
    }

    /**
     * Aura 18 — CMaNGOS HandleInvisibility → player PLAYER_FIELD_BYTES2 invisibility glow.
     * Visibility group later.
     */
    private static void modInvisibility(Unit target, boolean apply) {
        if (target instanceof Player player) {
            player.setInvisibilityGlow(apply);
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
     * Aura 66 — CMaNGOS HandleFeignDeath → SetFeignDeath (success path; resist roll later).
     * Sets UNIT_FLAG2_FEIGN_DEATH + UNIT_DYNFLAG_DEAD.
     */
    private static void modFeignDeath(Unit target, boolean apply) {
        target.setFeignDeath(apply);
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
     * Aura 140 — CMaNGOS HandleAuraModRangedHaste → ApplyAttackTimePercentMod(RANGED_ATTACK).
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
     * Aura 82 — CMaNGOS HandleWaterBreathing → SetWaterBreathingIntervalMultiplier(0) on apply;
     * restore 1.0 on unapply when no other WATER_BREATHING (HasAuraType / MOD_WATER_BREATHING later).
     */
    private static void modWaterBreathing(Unit target, boolean apply) {
        if (!(target instanceof Player player)) {
            return;
        }
        player.setWaterBreathingIntervalMultiplier(apply ? 0f : 1.0f);
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
     * Aura 61 — CMaNGOS HandleAuraModScale: scale = max(0.1, (100+MOD_SCALE)/100 * (100+MOD_SCALE_2)/100).
     * MOD_SCALE_2 stacking later; unapply restores 1.0 when no other scale auras.
     */
    private static void modScale(Unit target, SpellEngine.SpellInfo sp, boolean apply) {
        int amount = (sp.minDmg() + sp.maxDmg()) / 2;
        if (!apply) {
            target.setFloat(UpdateFields.OBJECT_FIELD_SCALE_X, 1.0f);
            return;
        }
        if (amount == 0) {
            return;
        }
        float scale = Math.max(0.1f, (100f + amount) / 100f);
        target.setFloat(UpdateFields.OBJECT_FIELD_SCALE_X, scale);
    }

    /** Aura 12 — CMaNGOS SetStunned: SetImmobilizedState(stun=true) then UNIT_FLAG_STUNNED. */
    private static void modStun(Unit target) {
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
