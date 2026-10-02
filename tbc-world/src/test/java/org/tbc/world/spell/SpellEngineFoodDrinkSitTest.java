package org.tbc.world.spell;

import org.junit.jupiter.api.Test;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL07-022 — Food/drink STANDING_CANCELS: sit on apply; leave seated drops aura (CMaNGOS).
 */
class SpellEngineFoodDrinkSitTest {

    @Test
    void applyFoodWhenStandingShouldSit() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        assertTrue(p.isStanding());
        eng.apply(p, p, eng.info(SpellEngine.SPELL_FOOD));
        assertEquals(Unit.UNIT_STAND_STATE_SIT, p.standState());
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == SpellEngine.SPELL_FOOD));
    }

    @Test
    void applyDrinkWhenStandingShouldSit() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_DRINK));
        assertEquals(Unit.UNIT_STAND_STATE_SIT, p.standState());
    }

    @Test
    void standWhenEatingShouldDropFoodAura() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.leaveSeatedAuras = u -> eng.removeAurasWithInterruptFlags(
                u, SpellEngine.AURA_INTERRUPT_FLAG_STANDING_CANCELS, null);
        eng.apply(p, p, eng.info(SpellEngine.SPELL_FOOD));
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == SpellEngine.SPELL_FOOD));
        p.stand();
        assertFalse(p.auras.stream().anyMatch(a -> a.spellId() == SpellEngine.SPELL_FOOD));
        assertTrue(p.isStanding());
    }

    @Test
    void auraInterruptFlagsWhenFoodDrinkShouldStandingCancel() {
        SpellEngine eng = new SpellEngine();
        assertTrue((eng.auraInterruptFlags(SpellEngine.SPELL_FOOD)
                & SpellEngine.AURA_INTERRUPT_FLAG_STANDING_CANCELS) != 0);
        assertTrue((eng.auraInterruptFlags(SpellEngine.SPELL_DRINK)
                & SpellEngine.AURA_INTERRUPT_FLAG_STANDING_CANCELS) != 0);
    }

    @Test
    void applyFoodWhenAlreadySittingShouldKeepSit() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.sit();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_FOOD));
        assertEquals(Unit.UNIT_STAND_STATE_SIT, p.standState());
    }

    @Test
    void applyStealthShouldNotSit() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_STEALTH));
        assertTrue(p.isStanding());
    }
}
