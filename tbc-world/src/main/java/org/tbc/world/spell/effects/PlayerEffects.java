package org.tbc.world.spell.effects;

import static org.tbc.world.spell.SpellEngine.EFFECT_ADD_FARSIGHT;
import static org.tbc.world.spell.SpellEngine.EFFECT_ADD_HONOR;
import static org.tbc.world.spell.SpellEngine.EFFECT_BLOCK;
import static org.tbc.world.spell.SpellEngine.EFFECT_DUAL_WIELD;
import static org.tbc.world.spell.SpellEngine.EFFECT_DUEL;
import static org.tbc.world.spell.SpellEngine.EFFECT_INEBRIATE;
import static org.tbc.world.spell.SpellEngine.EFFECT_KILL_CREDIT_GROUP;
import static org.tbc.world.spell.SpellEngine.EFFECT_LEARN_SPELL;
import static org.tbc.world.spell.SpellEngine.EFFECT_PARRY;
import static org.tbc.world.spell.SpellEngine.EFFECT_PLAY_MUSIC;
import static org.tbc.world.spell.SpellEngine.EFFECT_PLAY_SOUND;
import static org.tbc.world.spell.SpellEngine.EFFECT_PROFICIENCY;
import static org.tbc.world.spell.SpellEngine.EFFECT_QUEST_COMPLETE;
import static org.tbc.world.spell.SpellEngine.EFFECT_QUEST_FAIL;
import static org.tbc.world.spell.SpellEngine.EFFECT_REPUTATION;
import static org.tbc.world.spell.SpellEngine.EFFECT_SKILL_STEP;
import static org.tbc.world.spell.SpellEngine.EFFECT_UNLEARN_SPECIALIZATION;

import org.tbc.world.entity.Player;

/** Effects on a player's progression and social state (quests, skills, honor, reputation, duel, sounds). */
final class PlayerEffects {

    private PlayerEffects() {
    }

    static void register(EffectTable t) {
        t.register(EFFECT_QUEST_COMPLETE, (e, caster, target, sp, now) -> {
            e.questComplete(target, sp.misc());
            return 0;
        });
        t.register(EFFECT_QUEST_FAIL, (e, caster, target, sp, now) -> {
            e.questFail(target, sp.misc());
            return 0;
        });
        t.register(EFFECT_KILL_CREDIT_GROUP, (e, caster, target, sp, now) -> {
            e.killCreditGroup(target, sp.misc());
            return 0;
        });
        t.register(EFFECT_DUAL_WIELD, (e, caster, target, sp, now) -> {
            e.dualWield(target);
            return 0;
        });
        t.register(EFFECT_SKILL_STEP, (e, caster, target, sp, now) -> {
            e.skillStep(target, sp.misc(), (sp.minDmg() + sp.maxDmg()) / 2);
            return 0;
        });
        t.register(EFFECT_PARRY, (e, caster, target, sp, now) -> {
            e.enableParry(caster);
            return 0;
        });
        t.register(EFFECT_BLOCK, (e, caster, target, sp, now) -> {
            e.enableBlock(caster);
            return 0;
        });
        t.register(EFFECT_PROFICIENCY, (e, caster, target, sp, now) -> {
            e.proficiency(caster, sp.equippedItemClass(), sp.misc());
            return 0;
        });
        t.register(EFFECT_PLAY_MUSIC, (e, caster, target, sp, now) -> {
            e.playMusic(target, sp.misc());
            return 0;
        });
        t.register(EFFECT_PLAY_SOUND, (e, caster, target, sp, now) -> {
            e.playSound(target, sp.misc());
            return 0;
        });
        t.register(EFFECT_UNLEARN_SPECIALIZATION, (e, caster, target, sp, now) -> {
            e.unlearnSpecialization(target, sp.misc());
            return 0;
        });
        t.register(EFFECT_REPUTATION, (e, caster, target, sp, now) -> {
            e.modifyReputation(target, sp.misc(), (sp.minDmg() + sp.maxDmg()) / 2);
            return 0;
        });
        t.register(EFFECT_DUEL, (e, caster, target, sp, now) -> {
            e.duel(caster, target, sp.misc());
            return 0;
        });
        t.register(EFFECT_INEBRIATE, (e, caster, target, sp, now) -> {
            e.inebriate(target, Math.max(0, (sp.minDmg() + sp.maxDmg()) / 2));
            return 0;
        });
        t.register(EFFECT_ADD_HONOR, (e, caster, target, sp, now) -> {
            e.addHonor(target, Math.max(0, (sp.minDmg() + sp.maxDmg()) / 2));
            return 0;
        });
        t.register(EFFECT_LEARN_SPELL, (e, caster, target, sp, now) -> {
            e.learnSpell(target, sp.misc());
            return 0;
        });
        t.register(EFFECT_ADD_FARSIGHT, (e, caster, target, sp, now) -> {
            if (caster instanceof Player p) {
                e.addFarsight(p, target.guid);
            }
            return 0;
        });
    }
}
