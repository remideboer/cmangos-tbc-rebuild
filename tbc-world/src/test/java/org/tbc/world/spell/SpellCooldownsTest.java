package org.tbc.world.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL07-005 — WorldObject::AddGCD / HasGCD / ResetGCD per StartRecoveryCategory. */
class SpellCooldownsTest {
    private final SpellCooldowns cd = new SpellCooldowns();

    @Test
    void hasGcdWhenNothingAddedShouldBeFalse() {
        assertFalse(cd.hasGcd(SpellCooldowns.GCD_CATEGORY_NORMAL, 0));
    }

    @Test
    void addGcdWhenDurationElapsesShouldExpireExactlyAtEnd() {
        cd.addGcd(SpellCooldowns.GCD_CATEGORY_NORMAL, 1500, 1000);
        assertTrue(cd.hasGcd(SpellCooldowns.GCD_CATEGORY_NORMAL, 2499));
        assertFalse(cd.hasGcd(SpellCooldowns.GCD_CATEGORY_NORMAL, 2500));
    }

    @Test
    void addGcdWhenZeroDurationShouldAddNothing() {
        cd.addGcd(SpellCooldowns.GCD_CATEGORY_NORMAL, 0, 1000);
        assertFalse(cd.hasGcd(SpellCooldowns.GCD_CATEGORY_NORMAL, 1000));
    }

    @Test
    void hasGcdWhenOtherCategoryShouldNotBlock() {
        cd.addGcd(SpellCooldowns.GCD_CATEGORY_NORMAL, 1500, 1000);
        assertFalse(cd.hasGcd(0, 1000));
    }

    @Test
    void resetGcdWhenCastCancelledShouldGiveGcdBack() {
        cd.addGcd(SpellCooldowns.GCD_CATEGORY_NORMAL, 1500, 1000);
        cd.resetGcd(SpellCooldowns.GCD_CATEGORY_NORMAL);
        assertFalse(cd.hasGcd(SpellCooldowns.GCD_CATEGORY_NORMAL, 1000));
    }

    /** WorldObject::AddCooldown RecoveryTime / IsSpellReady by spell id. Frost Nova 122 is 25 s. */
    @Test
    void isSpellReadyWhenRecoveryElapsedShouldExpireExactlyAtEnd() {
        cd.addSpell(122, 25_000, 1000);
        assertFalse(cd.isSpellReady(122, 25_999));
        assertTrue(cd.isSpellReady(122, 26_000));
    }

    @Test
    void addSpellWhenZeroDurationShouldAddNothing() {
        cd.addSpell(122, 0, 1000);
        assertTrue(cd.isSpellReady(122, 1000));
    }

    @Test
    void isSpellReadyWhenOtherSpellShouldNotBlock() {
        cd.addSpell(122, 25_000, 1000);
        assertTrue(cd.isSpellReady(133, 1000));
    }
}
