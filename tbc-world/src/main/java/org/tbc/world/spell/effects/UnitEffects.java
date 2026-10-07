package org.tbc.world.spell.effects;

import static org.tbc.world.spell.SpellEngine.EFFECT_ADD_COMBO_POINTS;
import static org.tbc.world.spell.SpellEngine.EFFECT_ADD_EXTRA_ATTACKS;
import static org.tbc.world.spell.SpellEngine.EFFECT_ATTACK_ME;
import static org.tbc.world.spell.SpellEngine.EFFECT_DISPEL_MECHANIC;
import static org.tbc.world.spell.SpellEngine.EFFECT_DISTRACT;
import static org.tbc.world.spell.SpellEngine.EFFECT_ENERGIZE;
import static org.tbc.world.spell.SpellEngine.EFFECT_ENERGIZE_PCT;
import static org.tbc.world.spell.SpellEngine.EFFECT_ENVIRONMENTAL_DAMAGE;
import static org.tbc.world.spell.SpellEngine.EFFECT_HEALTH_LEECH;
import static org.tbc.world.spell.SpellEngine.EFFECT_HEAL_MAX_HEALTH;
import static org.tbc.world.spell.SpellEngine.EFFECT_HEAL_MECHANICAL;
import static org.tbc.world.spell.SpellEngine.EFFECT_HEAL_PCT;
import static org.tbc.world.spell.SpellEngine.EFFECT_INSTAKILL;
import static org.tbc.world.spell.SpellEngine.EFFECT_INTERRUPT_CAST;
import static org.tbc.world.spell.SpellEngine.EFFECT_MODIFY_THREAT_PERCENT;
import static org.tbc.world.spell.SpellEngine.EFFECT_NORMALIZED_WEAPON_DMG;
import static org.tbc.world.spell.SpellEngine.EFFECT_POWER_BURN;
import static org.tbc.world.spell.SpellEngine.EFFECT_POWER_DRAIN;
import static org.tbc.world.spell.SpellEngine.EFFECT_REDIRECT_THREAT;
import static org.tbc.world.spell.SpellEngine.EFFECT_RESURRECT;
import static org.tbc.world.spell.SpellEngine.EFFECT_RESURRECT_NEW;
import static org.tbc.world.spell.SpellEngine.EFFECT_SANCTUARY;
import static org.tbc.world.spell.SpellEngine.EFFECT_SELF_RESURRECT;
import static org.tbc.world.spell.SpellEngine.EFFECT_SPIRIT_HEAL;
import static org.tbc.world.spell.SpellEngine.EFFECT_STEAL_BENEFICIAL_BUFF;
import static org.tbc.world.spell.SpellEngine.EFFECT_THREAT;
import static org.tbc.world.spell.SpellEngine.EFFECT_WEAPON_PERCENT_DAMAGE;

import org.tbc.world.spell.SpellEngine.SpellInfo;

/** Effects on a unit's life, power, threat and combat state (no school/weapon damage rolls, no auras). */
final class UnitEffects {

    private UnitEffects() {
    }

    /** CMaNGOS CalculateDamage midpoint, clamped at 0 like the former inline chain. */
    private static int amount(SpellInfo sp) {
        return Math.max(0, (sp.minDmg() + sp.maxDmg()) / 2);
    }

    static void register(EffectTable t) {
        t.register(EFFECT_INSTAKILL, (e, caster, target, sp, now) -> {
            e.instakill(target);
            return 0;
        });
        t.register(EFFECT_HEALTH_LEECH, (e, caster, target, sp, now) -> e.healthLeech(caster, target, amount(sp)));
        t.register(EFFECT_POWER_DRAIN, (e, caster, target, sp, now) -> e.powerDrain(caster, target, amount(sp)));
        t.register(EFFECT_ADD_COMBO_POINTS, (e, caster, target, sp, now) -> {
            e.addComboPoints(caster, target, amount(sp));
            return 0;
        });
        t.register(EFFECT_INTERRUPT_CAST, (e, caster, target, sp, now) -> {
            e.interruptCast(target);
            return 0;
        });
        t.register(EFFECT_SANCTUARY, (e, caster, target, sp, now) -> {
            e.sanctuary(target);
            return 0;
        });
        t.register(EFFECT_ADD_EXTRA_ATTACKS, (e, caster, target, sp, now) -> {
            e.addExtraAttacks(target, amount(sp));
            return 0;
        });
        t.register(EFFECT_ATTACK_ME, (e, caster, target, sp, now) -> {
            e.attackMe(caster, target);
            return 0;
        });
        t.register(EFFECT_RESURRECT, (e, caster, target, sp, now) -> {
            e.resurrect(caster, target, amount(sp));
            return 0;
        });
        t.register(EFFECT_RESURRECT_NEW, (e, caster, target, sp, now) -> {
            e.resurrectNew(caster, target, amount(sp), sp.misc());
            return 0;
        });
        t.register(EFFECT_SPIRIT_HEAL, (e, caster, target, sp, now) -> {
            e.spiritHeal(target, sp.id());
            return 0;
        });
        t.register(EFFECT_ENVIRONMENTAL_DAMAGE,
                (e, caster, target, sp, now) -> e.environmentalDamage(caster, amount(sp)));
        t.register(EFFECT_POWER_BURN, (e, caster, target, sp, now) -> e.powerBurn(target, amount(sp), sp.misc()));
        t.register(EFFECT_THREAT, (e, caster, target, sp, now) -> {
            e.addThreat(caster, target, amount(sp));
            return 0;
        });
        t.register(EFFECT_HEAL_PCT, (e, caster, target, sp, now) -> {
            e.healPct(target, amount(sp));
            return 0;
        });
        t.register(EFFECT_ENERGIZE_PCT, (e, caster, target, sp, now) -> {
            e.energizePct(target, amount(sp), sp.misc());
            return 0;
        });
        t.register(EFFECT_SELF_RESURRECT, (e, caster, target, sp, now) -> {
            e.selfResurrect(caster, amount(sp));
            return 0;
        });
        t.register(EFFECT_HEAL_MECHANICAL, (e, caster, target, sp, now) -> {
            e.healMechanical(target, amount(sp));
            return 0;
        });
        t.register(EFFECT_WEAPON_PERCENT_DAMAGE,
                (e, caster, target, sp, now) -> e.weaponPercentDamage(caster, target, amount(sp)));
        t.register(EFFECT_NORMALIZED_WEAPON_DMG,
                (e, caster, target, sp, now) -> e.normalizedWeaponDamage(caster, target));
        t.register(EFFECT_DISTRACT, (e, caster, target, sp, now) -> {
            float destX = caster != null ? caster.x : target.x;
            float destY = caster != null ? caster.y : target.y;
            e.distract(target, destX, destY);
            return 0;
        });
        t.register(EFFECT_DISPEL_MECHANIC, (e, caster, target, sp, now) -> {
            e.dispelMechanic(target, sp.misc(), amount(sp));
            return 0;
        });
        t.register(EFFECT_STEAL_BENEFICIAL_BUFF, (e, caster, target, sp, now) -> {
            e.stealBeneficialBuff(caster, target, amount(sp));
            return 0;
        });
        t.register(EFFECT_MODIFY_THREAT_PERCENT, (e, caster, target, sp, now) -> {
            e.modifyThreatPercent(caster, target, (sp.minDmg() + sp.maxDmg()) / 2);
            return 0;
        });
        t.register(EFFECT_REDIRECT_THREAT, (e, caster, target, sp, now) -> {
            e.redirectThreat(caster, target);
            return 0;
        });
        t.register(EFFECT_HEAL_MAX_HEALTH, (e, caster, target, sp, now) -> {
            target.setHealth(target.maxHealth());
            return 0;
        });
        t.register(EFFECT_ENERGIZE, (e, caster, target, sp, now) -> {
            e.energize(target, Math.max(1, (sp.minDmg() + sp.maxDmg()) / 2));
            return 0;
        });
    }
}
