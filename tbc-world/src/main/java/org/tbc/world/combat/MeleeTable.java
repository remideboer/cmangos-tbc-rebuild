package org.tbc.world.combat;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.content.WeaponSkills;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.IntBinaryOperator;

/**
 * One sequential melee roll. spec/05-domain/combat-and-threat.md
 * L1 vs same-level creature: miss 5%, dodge/parry/block 5% each; no glance (victim level ≤ 10).
 */
public final class MeleeTable {
    public enum Outcome { HIT, MISS, DODGE, PARRY, BLOCK, GLANCE, CRIT, CRUSH, EVADE }

    public record Result(Outcome outcome, int damage, int threat, int blocked) {
        public Result(Outcome outcome, int damage, int threat) {
            this(outcome, damage, threat, 0);
        }
    }

    public static final MeleeTable DEFAULT = new MeleeTable();

    private final DoubleSupplier unitRoll;
    private final IntBinaryOperator damageRoll;

    public MeleeTable() {
        this(() -> ThreadLocalRandom.current().nextDouble(),
                (min, max) -> min >= max ? min : ThreadLocalRandom.current().nextInt(min, max + 1));
    }

    public MeleeTable(DoubleSupplier unitRoll, IntBinaryOperator damageRoll) {
        this.unitRoll = unitRoll;
        this.damageRoll = damageRoll;
    }

    public static MeleeTable alwaysHit() {
        return new MeleeTable(() -> 0.99d, (min, max) -> min);
    }

    public static Result roll(Unit attacker, Unit victim, int weaponMin, int weaponMax) {
        return DEFAULT.rollOne(attacker, victim, weaponMin, weaponMax);
    }

    public Result rollOne(Unit attacker, Unit victim, int weaponMin, int weaponMax) {
        return rollOne(attacker, victim, weaponMin, weaponMax, false);
    }

    public Result rollOne(Unit attacker, Unit victim, int weaponMin, int weaponMax, boolean offhand) {
        if (victim instanceof Creature creature && (creature.evading || creature.evadeTimerMs > 0)) {
            return new Result(Outcome.EVADE, 0, 0);
        }
        double r = unitRoll.getAsDouble();
        double acc = missChance(attacker, victim, offhand);
        if (r < acc) {
            return miss();
        }
        if (victim instanceof Player && !victim.isStanding()) {
            return hit(Outcome.CRIT, weaponMin, weaponMax, 2);
        }
        acc += dodgeChance(attacker, victim, offhand);
        if (r < acc) {
            return new Result(Outcome.DODGE, 0, 0);
        }
        acc += parryChance(attacker, victim, offhand);
        if (r < acc) {
            return new Result(Outcome.PARRY, 0, 0);
        }
        acc += blockChance(attacker, victim);
        if (r < acc) {
            int raw = damageRoll.applyAsInt(weaponMin, weaponMax);
            int shield = victim instanceof Player p ? p.getInt(UpdateFields.PLAYER_SHIELD_BLOCK) : 0;
            int dmg = raw - shield;
            if (dmg < 0) {
                dmg = 0;
            }
            return new Result(Outcome.BLOCK, dmg, dmg, raw - dmg);
        }
        acc += glanceChance(attacker, victim);
        if (r < acc) {
            int raw = damageRoll.applyAsInt(weaponMin, weaponMax);
            int dmg = glanceDamage(raw, attacker, victim);
            return new Result(Outcome.GLANCE, dmg, dmg);
        }
        double crit = critChance(attacker, victim, offhand);
        acc += crit;
        if (r < acc) {
            return hit(Outcome.CRIT, weaponMin, weaponMax, 2);
        }
        acc += crushChance(attacker, victim);
        if (r < acc) {
            int crush = damageRoll.applyAsInt(weaponMin, weaponMax) * 3 / 2;
            return new Result(Outcome.CRUSH, crush, crush);
        }
        int dmg = damageRoll.applyAsInt(weaponMin, weaponMax);
        return new Result(Outcome.HIT, dmg, dmg);
    }

    private Result hit(Outcome outcome, int weaponMin, int weaponMax, int mul) {
        int dmg = damageRoll.applyAsInt(weaponMin, weaponMax) * mul;
        return new Result(outcome, dmg, dmg);
    }

    private static Result miss() {
        return new Result(Outcome.MISS, 0, 0);
    }

    /** Base 5%. Dual-wield white +19 unless a next-melee swing is queued. Vs NPC, defense − skill: ≤10 at 0.1 each; above that leftover at 0.2+0.4. */
    static double missChance(Unit attacker, Unit victim) {
        return missChance(attacker, victim, false);
    }

    /** CMaNGOS CalculateEffectiveMissChance — subtract GetHitChance(attType). */
    static double missChance(Unit attacker, Unit victim, boolean offhand) {
        double pct = 5.0;
        if (victim instanceof Creature creature && creature.totem) {
            pct = 0;
        }
        if (attacker instanceof Player p && p.hasOffhandWeapon() && !p.hasNextMeleeSwingQueued()) {
            pct += 19.0;
        }
        int difference = defenseSkill(victim, attacker) - weaponSkill(attacker, victim, offhand);
        if (victim instanceof Creature) {
            if (difference > 10) {
                pct += 1.0;
                int leftover = difference - 10;
                pct += leftover * 0.2;
                pct += leftover * 0.4;
            } else if (difference > 0) {
                pct += difference * 0.1;
            } else {
                pct += difference * 0.04;
            }
        } else {
            pct += difference * 0.04;
        }
        // chance -= GetHitChance(attType); Unit.cpp CalculateEffectiveMissChance.
        pct -= attacker.weaponHitChance(offhand ? Unit.OFF_ATTACK : Unit.BASE_ATTACK);
        if (pct < 0) {
            pct = 0;
        }
        if (pct > 100) {
            pct = 100;
        }
        return pct / 100.0;
    }

    static double dodgeChance(Unit attacker, Unit victim, boolean offhand) {
        return minusExpertise(skillAvoid(attacker, victim, UpdateFields.PLAYER_DODGE_PERCENTAGE, 0.1, 0.1, offhand), attacker, offhand);
    }

    static double parryChance(Unit attacker, Unit victim, boolean offhand) {
        return minusExpertise(skillAvoid(attacker, victim, UpdateFields.PLAYER_PARRY_PERCENTAGE, 0.1, 0.6, offhand), attacker, offhand);
    }

    static double blockChance(Unit attacker, Unit victim) {
        return skillAvoid(attacker, victim, UpdateFields.PLAYER_BLOCK_PERCENTAGE, 0.0, 0.0, false);
    }

    /** PLAYER_EXPERTISE / 4 percent; offhand uses PLAYER_OFFHAND_EXPERTISE. spec/03-protocol/update-fields.yaml */
    static double minusExpertise(double chance, Unit attacker, boolean offhand) {
        if (!(attacker instanceof Player p)) {
            return chance;
        }
        int field = offhand ? UpdateFields.PLAYER_OFFHAND_EXPERTISE : UpdateFields.PLAYER_EXPERTISE;
        chance -= p.getInt(field) * 0.25 / 100.0;
        if (chance < 0) {
            return 0;
        }
        return chance;
    }

    /** Base then (defense − skill) × factor. NPC positive difference: dodge 0.1; parry 0.1 or 0.6 if > 10. */
    static double skillAvoid(Unit attacker, Unit victim, int playerField, double npcPos, double npcHigh, boolean offhand) {
        double pct = victim instanceof Player ? victim.getFloat(playerField) : 5.0;
        if (pct < 0.005) {
            return 0;
        }
        int difference = defenseSkill(victim, attacker) - weaponSkill(attacker, victim, offhand);
        double factor = 0.04;
        if (victim instanceof Creature && difference > 0) {
            factor = difference > 10 ? npcHigh : npcPos;
        }
        pct += difference * factor;
        if (pct < 0) {
            pct = 0;
        }
        if (pct > 100) {
            pct = 100;
        }
        return pct / 100.0;
    }

    static double critChance(Unit attacker, Unit victim, boolean offhand) {
        double pct = attacker instanceof Player
                ? attacker.getFloat(offhand
                        ? UpdateFields.PLAYER_OFFHAND_CRIT_PERCENTAGE
                        : UpdateFields.PLAYER_CRIT_PERCENTAGE)
                : 5.0;
        // Vs NPC: GetSkillMaxForLevel (weapon skill does not raise crit). Vs player: weapon skill (PvP max).
        int skill = victim instanceof Creature
                ? attacker.level * 5
                : weaponSkill(attacker, victim, offhand);
        int defense = defenseSkill(victim, attacker);
        if (victim instanceof Creature) {
            pct += 0.2 * (skill - defense);
        } else {
            pct += 0.04 * (skill - defense);
        }
        if (pct < 0) {
            pct = 0;
        }
        if (pct > 100) {
            pct = 100;
        }
        return pct / 100.0;
    }

    /** Player vs NPC level > 10: 10 + (defense − skill), or wand-user below 30: level + (defense − skill). */
    static double glanceChance(Unit attacker, Unit victim) {
        if (!(attacker instanceof Player p)) {
            return 0;
        }
        if (!(victim instanceof Creature)) {
            return 0;
        }
        if (victim.level <= 10) {
            return 0;
        }
        int skill = weaponSkill(attacker, victim, false);
        int defense = defenseSkill(victim, attacker);
        double pct = p.isWandUser() && p.level < 30
                ? p.level + (defense - skill)
                : 10.0 + (defense - skill);
        if (pct < 0) {
            pct = 0;
        }
        if (pct > 100) {
            pct = 100;
        }
        return pct / 100.0;
    }

    /** Glance: roll multiplier between lowEnd and highEnd. Casters 0.9/0.6; others 1.2/1.3. */
    int glanceDamage(int raw, Unit attacker, Unit victim) {
        int difference = defenseSkill(victim, attacker) - weaponSkill(attacker, victim, false);
        if (difference < 0) {
            return raw;
        }
        boolean caster = ((Player) attacker).isWandUser();
        float baseHighEnd = caster ? 0.9f : 1.2f;
        float baseLowEnd = caster ? 0.6f : 1.3f;
        float maxLowEnd = caster ? 0.6f : 0.91f;
        float highEnd = Math.min(Math.max(baseHighEnd - 0.03f * difference, 0.20f), 0.99f);
        float lowEnd = Math.min(Math.max(baseLowEnd - 0.05f * difference, 0.01f), Math.min(maxLowEnd, highEnd));
        int lo = (int) (lowEnd * 100);
        int hi = (int) (highEnd * 100);
        int hundredths = damageRoll.applyAsInt(lo, hi);
        return raw * hundredths / 100;
    }

    static double crushChance(Unit attacker, Unit victim) {
        if (!(attacker instanceof Creature) || !(victim instanceof Player)) {
            return 0;
        }
        int deficit = weaponSkill(attacker, victim, false) - defenseSkill(victim, attacker);
        if (deficit < 15) {
            return 0;
        }
        return (2.0 * deficit - 15) / 100.0;
    }

    /**
     * CMaNGOS GetWeaponSkillValue — players use skillValue (PvP: max(skill, skillMax));
     * creatures use level×5. Unlearned player skill falls back to level×5 (create default).
     */
    static int weaponSkill(Unit attacker, Unit victim, boolean offhand) {
        if (!(attacker instanceof Player p)) {
            return attacker.level * 5;
        }
        int skillId = p.weaponSkillIdForAttack(offhand);
        if (skillId == 0) {
            return p.level * 5;
        }
        int max = p.skillMax(skillId);
        if (max == 0) {
            return p.level * 5;
        }
        int value = p.skillValue(skillId);
        if (victim instanceof Player) {
            return Math.max(max, value);
        }
        return value;
    }

    /**
     * CMaNGOS GetDefenseSkillValue — players use defense skillValue (PvP max);
     * creatures use level×5. Unlearned falls back to level×5.
     */
    static int defenseSkill(Unit defender, Unit attacker) {
        if (!(defender instanceof Player p)) {
            return defender.level * 5;
        }
        int max = p.skillMax(WeaponSkills.SKILL_DEFENSE);
        if (max == 0) {
            return p.level * 5;
        }
        int value = p.skillValue(WeaponSkills.SKILL_DEFENSE);
        if (attacker instanceof Player) {
            return Math.max(max, value);
        }
        return value;
    }
}
