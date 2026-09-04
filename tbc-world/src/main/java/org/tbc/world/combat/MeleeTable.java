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
        acc += 0.05;
        if (r < acc) {
            return new Result(Outcome.DODGE, 0, 0);
        }
        acc += 0.05;
        if (r < acc) {
            return new Result(Outcome.PARRY, 0, 0);
        }
        acc += 0.05;
        if (r < acc) {
            return new Result(Outcome.BLOCK, 0, 0);
        }
        if (victim.level > 10) {
            acc += 0.10;
            if (r < acc) {
                return hit(Outcome.GLANCE, weaponMin, weaponMax, 1);
            }
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
