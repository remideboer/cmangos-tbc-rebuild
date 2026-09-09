package org.tbc.world.spell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void copyFromWhenSpellCooldownSetShouldCloneExpireAndSkipGcd() {
        SpellCooldowns src = new SpellCooldowns();
        src.addGcd(SpellCooldowns.GCD_CATEGORY_NORMAL, 1500, 1000);
        src.addSpell(122, 25_000, 1000);
        SpellCooldowns dest = new SpellCooldowns();
        dest.copyFrom(src);
        assertFalse(dest.isSpellReady(122, 25_999));
        assertFalse(dest.hasGcd(SpellCooldowns.GCD_CATEGORY_NORMAL, 1000), "GCD is not persisted");
        src.addSpell(122, 1, 1000);
        assertFalse(dest.isSpellReady(122, 25_999), "clone is independent");
    }

    @Test
    void remainingSpellsWhenElapsedShouldOmitSpell() {
        cd.addSpell(122, 100, 1000);
        assertTrue(cd.remainingSpells(1100).isEmpty());
        var still = cd.remainingSpells(1050);
        assertEquals(1, still.size());
        assertEquals(122, still.get(0).spellId());
        assertEquals(50, still.get(0).remainMs());
        assertEquals(0, still.get(0).itemId());
        assertEquals(0, still.get(0).category());
    }

    @Test
    void restoreSpellWhenExpireInPastShouldSkip() {
        cd.restoreSpell(122, 500, 1000);
        assertTrue(cd.isSpellReady(122, 1000));
        cd.restoreSpell(122, 2000, 1000);
        assertFalse(cd.isSpellReady(122, 1000));
        assertEquals(2000L, cd.unexpiredSpellExpireMs(1000).get(122));
    }
}
