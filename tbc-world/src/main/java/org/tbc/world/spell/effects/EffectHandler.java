package org.tbc.world.spell.effects;

import org.tbc.world.entity.Unit;
import org.tbc.world.spell.SpellEngine;

/** One SPELL_EFFECT_* implementation. Returns dealt damage (0 for non-damage effects) like SpellEngine.apply. */
@FunctionalInterface
public interface EffectHandler {
    int apply(SpellEngine engine, Unit caster, Unit target, SpellEngine.SpellInfo sp, long nowMs);
}
