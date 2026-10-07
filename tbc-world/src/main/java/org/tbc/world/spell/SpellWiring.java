package org.tbc.world.spell;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.content.SkillLineAbility;
import org.tbc.world.entity.Unit;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Collaborators the world wires into {@link SpellEngine}. {@link #defaults()} are the domain-unit-test values:
 * no-op visibility updater, seeded SkillLineAbility, no ObjectMgr, random irand(1,1000) skill rolls.
 */
public record SpellWiring(Consumer<Unit> visibilityUpdater, SkillLineAbility skillLineAbilities, ObjectMgr objectMgr,
                          IntSupplier craftSkillRoll, IntSupplier gatherSkillRoll) {
    public static SpellWiring defaults() {
        return new SpellWiring(u -> { }, SkillLineAbility.seeded(), null,
                () -> ThreadLocalRandom.current().nextInt(1, 1001),
                () -> ThreadLocalRandom.current().nextInt(1, 1001));
    }

    public SpellWiring withVisibilityUpdater(Consumer<Unit> v) {
        return new SpellWiring(v, skillLineAbilities, objectMgr, craftSkillRoll, gatherSkillRoll);
    }

    public SpellWiring withSkillLineAbilities(SkillLineAbility s) {
        return new SpellWiring(visibilityUpdater, s, objectMgr, craftSkillRoll, gatherSkillRoll);
    }

    public SpellWiring withObjectMgr(ObjectMgr m) {
        return new SpellWiring(visibilityUpdater, skillLineAbilities, m, craftSkillRoll, gatherSkillRoll);
    }

    /** In-memory world / tests: every UpdateSkillPro roll succeeds. */
    public SpellWiring alwaysSucceedSkillRolls() {
        return new SpellWiring(visibilityUpdater, skillLineAbilities, objectMgr, () -> 1, () -> 1);
    }
}
