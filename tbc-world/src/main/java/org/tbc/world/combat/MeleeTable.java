package org.tbc.world.combat;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;
import java.util.function.IntBinaryOperator;

/**
 * One sequential melee roll. spec/05-domain/combat-and-threat.md
 * L1 vs same-level creature: miss 5%, dodge/parry/block 5% each; no glance (victim level ≤ 10).
 */
public final class MeleeTable {
    public enum Outcome { HIT, MISS, DODGE, PARRY, BLOCK, GLANCE, CRIT, CRUSH }

    public record Result(Outcome outcome, int damage, int threat) {}

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
        double r = unitRoll.getAsDouble();
        double acc = 0.05;
        if (r < acc) {
            return miss();
        }
        if (victim instanceof Player && !victim.isStanding()) {
            return hit(Outcome.CRIT, weaponMin, weaponMax, 2);
        }
        acc += dodgeChance(victim);
        if (r < acc) {
            return new Result(Outcome.DODGE, 0, 0);
        }
        acc += parryChance(victim);
        if (r < acc) {
            return new Result(Outcome.PARRY, 0, 0);
        }
        acc += blockChance(victim);
        if (r < acc) {
            return new Result(Outcome.BLOCK, 0, 0);
        }
        acc += glanceChance(attacker, victim);
        if (r < acc) {
            int raw = damageRoll.applyAsInt(weaponMin, weaponMax);
            int dmg = glanceDamage(raw, attacker, victim);
            return new Result(Outcome.GLANCE, dmg, dmg);
        }
        double crit = critChance(attacker, victim);
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

    static double dodgeChance(Unit victim) {
        return avoidChance(victim, UpdateFields.PLAYER_DODGE_PERCENTAGE);
    }

    static double parryChance(Unit victim) {
        return avoidChance(victim, UpdateFields.PLAYER_PARRY_PERCENTAGE);
    }

    static double blockChance(Unit victim) {
        return avoidChance(victim, UpdateFields.PLAYER_BLOCK_PERCENTAGE);
    }

    /** Creatures +5; players use the rating field with no extra +5. Base < 0.005 → incapable. */
    static double avoidChance(Unit victim, int playerField) {
        double pct = victim instanceof Player ? victim.getFloat(playerField) : 5.0;
        if (pct < 0.005) {
            return 0;
        }
        if (pct > 100) {
            pct = 100;
        }
        return pct / 100.0;
    }

    static double critChance(Unit attacker, Unit victim) {
        double pct = attacker instanceof Player
                ? attacker.getFloat(UpdateFields.PLAYER_CRIT_PERCENTAGE)
                : 5.0;
        int skill = attacker.level * 5;
        int defense = victim.level * 5;
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

    /** Player vs NPC level > 10: 10 + (defense − capped weapon skill) percent. */
    static double glanceChance(Unit attacker, Unit victim) {
        if (!(attacker instanceof Player)) {
            return 0;
        }
        if (!(victim instanceof Creature)) {
            return 0;
        }
        if (victim.level <= 10) {
            return 0;
        }
        int skill = attacker.level * 5;
        int defense = victim.level * 5;
        double pct = 10.0 + (defense - skill);
        if (pct < 0) {
            pct = 0;
        }
        if (pct > 100) {
            pct = 100;
        }
        return pct / 100.0;
    }

    /** Non-caster glance: roll multiplier between lowEnd and highEnd (combat-and-threat.md). */
    int glanceDamage(int raw, Unit attacker, Unit victim) {
        int difference = victim.level * 5 - attacker.level * 5;
        if (difference < 0) {
            return raw;
        }
        float highEnd = Math.min(Math.max(1.2f - 0.03f * difference, 0.20f), 0.99f);
        float lowEnd = Math.min(Math.max(1.3f - 0.05f * difference, 0.01f), Math.min(0.91f, highEnd));
        int lo = (int) (lowEnd * 100);
        int hi = (int) (highEnd * 100);
        int hundredths = damageRoll.applyAsInt(lo, hi);
        return raw * hundredths / 100;
    }

    static double crushChance(Unit attacker, Unit victim) {
        if (!(attacker instanceof Creature) || !(victim instanceof Player)) {
            return 0;
        }
        int deficit = attacker.level * 5 - victim.level * 5;
        if (deficit < 15) {
            return 0;
        }
        return (2.0 * deficit - 15) / 100.0;
    }
}
