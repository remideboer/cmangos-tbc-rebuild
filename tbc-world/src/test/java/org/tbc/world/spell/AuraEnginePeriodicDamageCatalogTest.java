package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-260 — periodic damage (3) stays on the existing SpellEngine tick. */
class AuraEnginePeriodicDamageCatalogTest {
    @Test
    void knownPeriodicDamageShouldStillTickThroughSpellEngine() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_PERIODIC_DAMAGE));
        Creature caster = new Creature();
        Player target = new Player();
        target.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        target.setHealth(80);
        SpellEngine.SpellInfo dot = new SpellEngine.SpellInfo(999003, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_PERIODIC_DAMAGE, 0, 0, 15, 15, 0f);
        assertFalse(eng.auras().tickCatalogPeriodic(caster, target, dot, null));
        assertEquals(80, target.health());
        AtomicInteger packets = new AtomicInteger();
        eng.tickPeriodic(caster, target, dot, (op, payload) -> packets.incrementAndGet());
        assertEquals(65, target.health());
        assertTrue(packets.get() > 0);
    }
}
