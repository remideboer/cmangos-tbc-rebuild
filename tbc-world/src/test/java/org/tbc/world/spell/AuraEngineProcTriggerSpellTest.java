package org.tbc.world.spell;

import org.tbc.world.combat.MeleeTable;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL26-261 — proc trigger spell (42); only 39057 forces a melee hit. */
class AuraEngineProcTriggerSpellTest {
    @Test
    void parasiteShouldForceAMeleeHitAndEvadeStillWins() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_PROC_TRIGGER_SPELL));
        Player attacker = new Player();
        Creature victim = new Creature();
        MeleeTable table = new MeleeTable(() -> 0.0, (min, max) -> min);
        MeleeTable.Result miss = table.rollOne(attacker, victim, 10, 20);
        assertEquals(MeleeTable.Outcome.MISS, miss.outcome());

        SpellEngine.SpellInfo parasite = proc(AuraEngine.SPELL_SERPENTSHRINE_PARASITE);
        SpellEngine.SpellInfo other = proc(39058);
        eng.auras().apply(attacker, other);
        assertFalse(attacker.meleeAlwaysHits());
        eng.auras().apply(attacker, parasite);
        eng.auras().apply(attacker, parasite);
        assertTrue(attacker.meleeAlwaysHits());
        MeleeTable.Result hit = table.rollOne(attacker, victim, 10, 20);
        assertEquals(MeleeTable.Outcome.HIT, hit.outcome());
        assertEquals(10, hit.damage());

        victim.evading = true;
        MeleeTable.Result evade = table.rollOne(attacker, victim, 10, 20);
        assertEquals(MeleeTable.Outcome.EVADE, evade.outcome());
        assertEquals(0, evade.damage());

        victim.evading = false;
        eng.auras().unapply(attacker, parasite);
        assertTrue(attacker.meleeAlwaysHits());
        eng.auras().unapply(attacker, parasite);
        assertFalse(attacker.meleeAlwaysHits());
        assertEquals(MeleeTable.Outcome.MISS, table.rollOne(attacker, victim, 10, 20).outcome());
    }

    private static SpellEngine.SpellInfo proc(int spellId) {
        return new SpellEngine.SpellInfo(spellId, SpellEngine.EFFECT_APPLY_AURA,
                AuraEngine.SPELL_AURA_PROC_TRIGGER_SPELL, 0, 0, 0, 0, 0f);
    }
}
