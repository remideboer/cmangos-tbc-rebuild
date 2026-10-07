package org.tbc.world.spell.effects;

import static org.tbc.world.spell.SpellEngine.EFFECT_FORCE_CAST;
import static org.tbc.world.spell.SpellEngine.EFFECT_FORCE_CAST_WITH_VALUE;
import static org.tbc.world.spell.SpellEngine.EFFECT_SUMMON_RAF_FRIEND;
import static org.tbc.world.spell.SpellEngine.EFFECT_TRIGGER_MISSILE;
import static org.tbc.world.spell.SpellEngine.EFFECT_TRIGGER_SPELL;
import static org.tbc.world.spell.SpellEngine.EFFECT_TRIGGER_SPELL_2;
import static org.tbc.world.spell.SpellEngine.EFFECT_TRIGGER_SPELL_WITH_VALUE;

import org.tbc.world.spell.SpellEngine.SpellInfo;

/** Effects that cast another spell (trigger, missile, ritual, force cast). Return the nested damage. */
final class TriggerEffects {

    private TriggerEffects() {
    }

    private static int amount(SpellInfo sp) {
        return Math.max(0, (sp.minDmg() + sp.maxDmg()) / 2);
    }

    static void register(EffectTable t) {
        t.register(EFFECT_TRIGGER_SPELL, (e, caster, target, sp, now) -> {
            SpellInfo nested = e.info(sp.misc());
            if (nested == null) {
                return 0;
            }
            return e.apply(caster, target, nested);
        });
        t.register(EFFECT_TRIGGER_MISSILE, (e, caster, target, sp, now) -> e.triggerMissile(caster, target, sp.misc()));
        t.register(EFFECT_TRIGGER_SPELL_2,
                (e, caster, target, sp, now) -> e.triggerRitualOfSummoning(caster, target, sp.misc()));
        t.register(EFFECT_SUMMON_RAF_FRIEND, (e, caster, target, sp, now) -> e.summonRafFriend(caster, sp.misc()));
        t.register(EFFECT_FORCE_CAST, (e, caster, target, sp, now) -> e.forceCast(target, sp.misc()));
        t.register(EFFECT_FORCE_CAST_WITH_VALUE,
                (e, caster, target, sp, now) -> e.forceCastWithValue(target, sp.misc(), amount(sp)));
        t.register(EFFECT_TRIGGER_SPELL_WITH_VALUE,
                (e, caster, target, sp, now) -> e.triggerSpellWithValue(caster, target, sp.misc(), amount(sp)));
    }
}
