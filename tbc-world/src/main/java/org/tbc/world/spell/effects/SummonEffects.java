package org.tbc.world.spell.effects;

import static org.tbc.world.spell.SpellEngine.EFFECT_ACTIVATE_OBJECT;
import static org.tbc.world.spell.SpellEngine.EFFECT_DESTROY_ALL_TOTEMS;
import static org.tbc.world.spell.SpellEngine.EFFECT_PERSISTENT_AREA_AURA;
import static org.tbc.world.spell.SpellEngine.EFFECT_SEND_EVENT;
import static org.tbc.world.spell.SpellEngine.EFFECT_SPAWN;
import static org.tbc.world.spell.SpellEngine.EFFECT_SUMMON;
import static org.tbc.world.spell.SpellEngine.EFFECT_SUMMON_OBJECT_SLOT1;
import static org.tbc.world.spell.SpellEngine.EFFECT_SUMMON_OBJECT_SLOT2;
import static org.tbc.world.spell.SpellEngine.EFFECT_SUMMON_OBJECT_WILD;
import static org.tbc.world.spell.SpellEngine.EFFECT_TRANS_DOOR;

import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Player;

/** Effects that create, activate or remove world objects (summons, totems, gameobjects, events). */
final class SummonEffects {

    private SummonEffects() {
    }

    static void register(EffectTable t) {
        t.register(EFFECT_ACTIVATE_OBJECT, (e, caster, target, sp, now) -> {
            GameObject go = caster instanceof Player p ? p.spellGameObjectTarget() : null;
            e.activateObject(go, sp.misc());
            return 0;
        });
        t.register(EFFECT_DESTROY_ALL_TOTEMS, (e, caster, target, sp, now) -> {
            e.destroyAllTotems(caster);
            return 0;
        });
        t.register(EFFECT_SPAWN, (e, caster, target, sp, now) -> {
            e.spawn(caster);
            return 0;
        });
        t.register(EFFECT_SUMMON_OBJECT_SLOT1, (e, caster, target, sp, now) -> {
            e.summonObjectSlot(caster, 0, sp.misc());
            return 0;
        });
        t.register(EFFECT_SUMMON_OBJECT_SLOT2, (e, caster, target, sp, now) -> {
            e.summonObjectSlot(caster, 1, sp.misc());
            return 0;
        });
        t.register(EFFECT_SUMMON_OBJECT_WILD, (e, caster, target, sp, now) -> {
            e.summonObjectWild(caster, sp.misc());
            return 0;
        });
        t.register(EFFECT_TRANS_DOOR, (e, caster, target, sp, now) -> {
            e.transmitted(caster, sp.misc());
            return 0;
        });
        t.register(EFFECT_SUMMON, (e, caster, target, sp, now) -> {
            e.summon(caster, sp.misc());
            return 0;
        });
        t.register(EFFECT_PERSISTENT_AREA_AURA, (e, caster, target, sp, now) -> {
            if (caster != null) {
                e.persistentAreaAura(caster, sp.id(), caster.x, caster.y, caster.z, 0f);
            }
            return 0;
        });
        t.register(EFFECT_SEND_EVENT, (e, caster, target, sp, now) -> {
            e.sendEvent(caster, sp.misc());
            return 0;
        });
    }
}
