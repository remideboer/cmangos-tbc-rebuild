package org.tbc.world.spell.effects;

import static org.tbc.world.spell.SpellEngine.EFFECT_DISENCHANT;
import static org.tbc.world.spell.SpellEngine.EFFECT_DURABILITY_DAMAGE;
import static org.tbc.world.spell.SpellEngine.EFFECT_DURABILITY_DAMAGE_PCT;
import static org.tbc.world.spell.SpellEngine.EFFECT_ENCHANT_HELD_ITEM;
import static org.tbc.world.spell.SpellEngine.EFFECT_ENCHANT_ITEM;
import static org.tbc.world.spell.SpellEngine.EFFECT_ENCHANT_ITEM_TEMPORARY;
import static org.tbc.world.spell.SpellEngine.EFFECT_OPEN_LOCK;
import static org.tbc.world.spell.SpellEngine.EFFECT_OPEN_LOCK_ITEM;
import static org.tbc.world.spell.SpellEngine.EFFECT_PICKPOCKET;
import static org.tbc.world.spell.SpellEngine.EFFECT_PROSPECTING;
import static org.tbc.world.spell.SpellEngine.EFFECT_SKINNING;
import static org.tbc.world.spell.SpellEngine.EFFECT_SKIN_PLAYER_CORPSE;
import static org.tbc.world.spell.SpellEngine.EFFECT_SUMMON_CHANGE_ITEM;

import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;

/** Effects that act on an item target or on a corpse/pocket (lock, enchant, prospect, skin, durability). */
final class ItemEffects {

    private ItemEffects() {
    }

    /** Spell::m_CastItem / item target of a player caster; null for creature casters. */
    private static Item itemTarget(Unit caster) {
        return caster instanceof Player p ? p.spellItemTarget() : null;
    }

    static void register(EffectTable t) {
        t.register(EFFECT_OPEN_LOCK, (e, caster, target, sp, now) -> {
            e.openLock(caster, itemTarget(caster), sp.misc());
            return 0;
        });
        t.register(EFFECT_OPEN_LOCK_ITEM, (e, caster, target, sp, now) -> {
            e.openLock(caster, itemTarget(caster), sp.misc());
            return 0;
        });
        t.register(EFFECT_SUMMON_CHANGE_ITEM, (e, caster, target, sp, now) -> {
            e.summonChangeItem(caster, itemTarget(caster), sp.misc());
            return 0;
        });
        t.register(EFFECT_ENCHANT_HELD_ITEM, (e, caster, target, sp, now) -> {
            e.enchantHeldItem(target, sp.misc());
            return 0;
        });
        t.register(EFFECT_ENCHANT_ITEM, (e, caster, target, sp, now) -> {
            e.enchantItem(caster, itemTarget(caster), sp.misc());
            return 0;
        });
        t.register(EFFECT_ENCHANT_ITEM_TEMPORARY, (e, caster, target, sp, now) -> {
            e.enchantItemTemporary(caster, itemTarget(caster), sp.misc());
            return 0;
        });
        t.register(EFFECT_PROSPECTING, (e, caster, target, sp, now) -> {
            e.prospecting(caster, itemTarget(caster));
            return 0;
        });
        t.register(EFFECT_DISENCHANT, (e, caster, target, sp, now) -> {
            e.disenchant(caster, itemTarget(caster));
            return 0;
        });
        t.register(EFFECT_DURABILITY_DAMAGE, (e, caster, target, sp, now) -> {
            e.durabilityDamage(target, sp.misc(), Math.max(0, (sp.minDmg() + sp.maxDmg()) / 2));
            return 0;
        });
        t.register(EFFECT_DURABILITY_DAMAGE_PCT, (e, caster, target, sp, now) -> {
            e.durabilityDamagePct(target, sp.misc(), Math.max(0, (sp.minDmg() + sp.maxDmg()) / 2));
            return 0;
        });
        t.register(EFFECT_PICKPOCKET, (e, caster, target, sp, now) -> {
            e.pickPocket(caster, target);
            return 0;
        });
        t.register(EFFECT_SKINNING, (e, caster, target, sp, now) -> {
            e.skinning(caster, target);
            return 0;
        });
        t.register(EFFECT_SKIN_PLAYER_CORPSE, (e, caster, target, sp, now) -> {
            e.skinPlayerCorpse(caster, target);
            return 0;
        });
    }
}
