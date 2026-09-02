package org.tbc.world.combat;

import org.tbc.world.entity.Unit;

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
            return new Result(Outcome.MISS, 0, 0);
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
                int dmg = damageRoll.applyAsInt(weaponMin, weaponMax);
                return new Result(Outcome.GLANCE, dmg, dmg);
            }
        }
        int dmg = damageRoll.applyAsInt(weaponMin, weaponMax);
        return new Result(Outcome.HIT, dmg, dmg);
    }
}
