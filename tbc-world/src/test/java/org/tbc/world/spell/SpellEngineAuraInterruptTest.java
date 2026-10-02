package org.tbc.world.spell;

import org.junit.jupiter.api.Test;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-174 — CMaNGOS RemoveAurasWithInterruptFlags(ATTACKING / DAMAGE) drops Stealth 1784.
 */
class SpellEngineAuraInterruptTest {

    @Test
    void removeAurasWhenAttackingShouldDropStealth() {
        SpellEngine eng = new SpellEngine();
        AtomicInteger vis = new AtomicInteger();
        eng.visibilityUpdater = u -> vis.incrementAndGet();
        Player p = new Player();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_STEALTH));
        assertEquals(Unit.Visibility.GROUP_STEALTH, p.visibility());
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == SpellEngine.SPELL_STEALTH));
        eng.removeAurasWithInterruptFlags(p, SpellEngine.AURA_INTERRUPT_FLAG_ATTACKING, null);
        assertFalse(p.auras.stream().anyMatch(a -> a.spellId() == SpellEngine.SPELL_STEALTH));
        assertEquals(Unit.Visibility.ON, p.visibility());
        assertTrue(vis.get() >= 1);
    }

    @Test
    void removeAurasWhenDamageShouldDropStealth() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_STEALTH));
        eng.removeAurasWithInterruptFlags(p, SpellEngine.AURA_INTERRUPT_FLAG_DAMAGE, null);
        assertTrue(p.auras.isEmpty());
        assertEquals(Unit.Visibility.ON, p.visibility());
    }

    @Test
    void removeAurasWhenWrongFlagOrNullShouldKeepStealth() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_STEALTH));
        eng.removeAurasWithInterruptFlags(null, SpellEngine.AURA_INTERRUPT_FLAG_ATTACKING, null);
        eng.removeAurasWithInterruptFlags(p, 0, null);
        eng.removeAurasWithInterruptFlags(p, SpellEngine.AURA_INTERRUPT_FLAG_STANDING_CANCELS, null);
        assertEquals(1, p.auras.size());
        assertEquals(Unit.Visibility.GROUP_STEALTH, p.visibility());
    }

    @Test
    void removeAurasWhenSendWithoutVisibleSlotShouldStillUnapply() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_STEALTH));
        int slot = AuraSlots.slotOf(p, SpellEngine.SPELL_STEALTH);
        assertTrue(slot >= 0);
        AuraSlots.clearVisible(p, slot);
        assertTrue(AuraSlots.slotOf(p, SpellEngine.SPELL_STEALTH) < 0);
        java.util.ArrayList<Integer> ops = new java.util.ArrayList<>();
        eng.removeAurasWithInterruptFlags(p, SpellEngine.AURA_INTERRUPT_FLAG_ATTACKING, (op, pl) -> ops.add(op));
        assertFalse(p.auras.stream().anyMatch(a -> a.spellId() == SpellEngine.SPELL_STEALTH));
        assertEquals(Unit.Visibility.ON, p.visibility());
    }

    @Test
    void removeAurasWhenSendAndVisibleSlotShouldClearAuraFields() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.level = 1;
        eng.apply(p, p, eng.info(SpellEngine.SPELL_STEALTH));
        assertTrue(AuraSlots.slotOf(p, SpellEngine.SPELL_STEALTH) >= 0);
        java.util.ArrayList<Integer> ops = new java.util.ArrayList<>();
        eng.removeAurasWithInterruptFlags(p, SpellEngine.AURA_INTERRUPT_FLAG_ATTACKING, (op, pl) -> ops.add(op));
        assertFalse(p.auras.stream().anyMatch(a -> a.spellId() == SpellEngine.SPELL_STEALTH));
        assertTrue(AuraSlots.slotOf(p, SpellEngine.SPELL_STEALTH) < 0);
        assertFalse(ops.isEmpty());
    }

    @Test
    void auraInterruptFlagsWhenStealthShouldIncludeAttackingAndDamage() {
        SpellEngine eng = new SpellEngine();
        int f = eng.auraInterruptFlags(SpellEngine.SPELL_STEALTH);
        assertEquals(SpellEngine.STEALTH_AURA_INTERRUPT_FLAGS, f);
        assertTrue((f & SpellEngine.AURA_INTERRUPT_FLAG_ATTACKING) != 0);
        assertTrue((f & SpellEngine.AURA_INTERRUPT_FLAG_DAMAGE) != 0);
        assertEquals(0, eng.auraInterruptFlags(999_999));
    }
}
