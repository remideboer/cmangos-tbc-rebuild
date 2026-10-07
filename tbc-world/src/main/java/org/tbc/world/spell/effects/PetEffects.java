package org.tbc.world.spell.effects;

import static org.tbc.world.spell.SpellEngine.EFFECT_CREATE_PET;
import static org.tbc.world.spell.SpellEngine.EFFECT_DISMISS_PET;
import static org.tbc.world.spell.SpellEngine.EFFECT_FEED_PET;
import static org.tbc.world.spell.SpellEngine.EFFECT_LEARN_PET_SPELL;
import static org.tbc.world.spell.SpellEngine.EFFECT_SUMMON_DEAD_PET;
import static org.tbc.world.spell.SpellEngine.EFFECT_SUMMON_PET;
import static org.tbc.world.spell.SpellEngine.EFFECT_TAME_CREATURE;

import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;

/** Hunter / warlock pet effects (tame, summon, dismiss, feed, revive, pet spells). */
final class PetEffects {

    private PetEffects() {
    }

    static void register(EffectTable t) {
        t.register(EFFECT_SUMMON_DEAD_PET, (e, caster, target, sp, now) -> {
            e.summonDeadPet(caster);
            return 0;
        });
        t.register(EFFECT_CREATE_PET, (e, caster, target, sp, now) -> {
            e.createTamedPet(target, sp.misc());
            return 0;
        });
        t.register(EFFECT_TAME_CREATURE, (e, caster, target, sp, now) -> {
            e.tameCreature(caster, target);
            return 0;
        });
        t.register(EFFECT_SUMMON_PET, (e, caster, target, sp, now) -> {
            e.summonPet(caster, sp.misc());
            return 0;
        });
        t.register(EFFECT_FEED_PET, (e, caster, target, sp, now) -> {
            Item food = caster instanceof Player p ? p.spellItemTarget() : null;
            e.feedPet(caster, food, sp.misc());
            return 0;
        });
        t.register(EFFECT_DISMISS_PET, (e, caster, target, sp, now) -> {
            e.dismissPet(caster);
            return 0;
        });
        t.register(EFFECT_LEARN_PET_SPELL, (e, caster, target, sp, now) -> {
            e.learnPetSpell(caster, sp.misc());
            return 0;
        });
    }
}
