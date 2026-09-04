package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.script.ClassScripts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellEngineCancelAuraTest {
    @Test
    void cancelAuraWhenUnstableAfflictionShouldRemoveAura() {
        SpellEngine eng = new SpellEngine();
        Creature c = new Creature();
        c.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        Player p = new Player();
        eng.apply(p, c, eng.info(ClassScripts.SPELL_UNSTABLE_AFFLICTION));
        assertEquals(1, c.auras.size());
        eng.cancelAura(c, ClassScripts.SPELL_UNSTABLE_AFFLICTION);
        assertTrue(c.auras.isEmpty());
    }

    @Test
    void cancelAuraWhenNullOrUnknownShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        eng.cancelAura(null, ClassScripts.SPELL_UNSTABLE_AFFLICTION);
        Creature c = new Creature();
        c.auras.add(new org.tbc.world.entity.Unit.Aura(ClassScripts.SPELL_UNSTABLE_AFFLICTION, 30_000, 1));
        eng.cancelAura(c, 0);
        eng.cancelAura(c, -1);
        assertEquals(1, c.auras.size());
        eng.cancelAura(c, SpellEngine.FIREBALL);
        assertEquals(1, c.auras.size());
    }
}
