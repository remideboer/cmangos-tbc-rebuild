package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-197 — SPELL_AURA_PREVENTS_FLEEING (92).
 * CMaNGOS HandlePreventFleeing → clear fleeing while MOD_FEAR remains; restore on unapply.
 */
class AuraEnginePreventsFleeingTest {
    private static final SpellEngine.SpellInfo FEAR = new SpellEngine.SpellInfo(
            SpellEngine.SPELL_FEAR, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_FEAR,
            5, 0, 0, 0, 0f);
    private static final SpellEngine.SpellInfo PREVENTS = new SpellEngine.SpellInfo(
            999092, SpellEngine.EFFECT_APPLY_AURA,
            AuraEngine.SPELL_AURA_PREVENTS_FLEEING, 0, 0, 0, 0, 0f);

    @Test
    void applyAuraWhenPreventsFleeingWhileFearedShouldClearFleeingFlag() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.auras().knownAura(AuraEngine.SPELL_AURA_PREVENTS_FLEEING));
        Creature mob = new Creature();
        eng.apply(new Player(), mob, FEAR);
        assertEquals(Unit.UNIT_FLAG_FLEEING,
                mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_FLEEING);

        eng.apply(new Player(), mob, PREVENTS);

        assertTrue(mob.hasAura(999092));
        assertEquals(0, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_FLEEING);
        assertTrue(mob.hasAura(SpellEngine.SPELL_FEAR));
    }

    @Test
    void unapplyWhenPreventsFleeingWhileFearRemainsShouldRestoreFleeing() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, FEAR);
        eng.apply(new Player(), mob, PREVENTS);
        eng.auras().unapply(mob, PREVENTS);
        assertEquals(Unit.UNIT_FLAG_FLEEING,
                mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_FLEEING);
    }

    @Test
    void applyWhenNoFearAuraShouldLeaveFlags() {
        SpellEngine eng = new SpellEngine();
        Creature mob = new Creature();
        eng.apply(new Player(), mob, PREVENTS);
        assertEquals(0, mob.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_FLEEING);
    }

    @Test
    void applyWhenTargetMissingShouldNoOp() {
        new AuraEngine().apply(null, PREVENTS);
        new AuraEngine().unapply(null, PREVENTS);
    }
}
