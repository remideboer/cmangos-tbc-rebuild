package org.tbc.world.spell.effects;

import static org.tbc.world.spell.SpellEngine.EFFECT_BIND;
import static org.tbc.world.spell.SpellEngine.EFFECT_CHARGE;
import static org.tbc.world.spell.SpellEngine.EFFECT_CHARGE_DEST;
import static org.tbc.world.spell.SpellEngine.EFFECT_KNOCKBACK_FROM_POSITION;
import static org.tbc.world.spell.SpellEngine.EFFECT_KNOCK_BACK;
import static org.tbc.world.spell.SpellEngine.EFFECT_LEAP;
import static org.tbc.world.spell.SpellEngine.EFFECT_LEAP_BACK;
import static org.tbc.world.spell.SpellEngine.EFFECT_PULL_TOWARDS;
import static org.tbc.world.spell.SpellEngine.EFFECT_PULL_TOWARDS_DEST;
import static org.tbc.world.spell.SpellEngine.EFFECT_SEND_TAXI;
import static org.tbc.world.spell.SpellEngine.EFFECT_STUCK;
import static org.tbc.world.spell.SpellEngine.EFFECT_SUMMON_PLAYER;
import static org.tbc.world.spell.SpellEngine.EFFECT_TELEPORT_GRAVEYARD;
import static org.tbc.world.spell.SpellEngine.EFFECT_TELEPORT_UNITS_FACE_CASTER;

import org.tbc.world.map.GraveyardManager;

/** Effects that move or relocate a unit (teleport, charge, leap, knockback, pull, taxi, bind). */
final class MovementEffects {

    private MovementEffects() {
    }

    static void register(EffectTable t) {
        t.register(EFFECT_TELEPORT_UNITS_FACE_CASTER, (e, caster, target, sp, now) -> {
            e.teleportUnitsFaceCaster(caster, target);
            return 0;
        });
        t.register(EFFECT_STUCK, (e, caster, target, sp, now) -> {
            e.stuck(caster);
            return 0;
        });
        t.register(EFFECT_SUMMON_PLAYER, (e, caster, target, sp, now) -> {
            e.summonPlayer(caster, target);
            return 0;
        });
        t.register(EFFECT_BIND, (e, caster, target, sp, now) -> {
            e.bindHearth(target);
            return 0;
        });
        t.register(EFFECT_SEND_TAXI, (e, caster, target, sp, now) -> {
            e.sendTaxi(target, sp.misc());
            return 0;
        });
        t.register(EFFECT_CHARGE, (e, caster, target, sp, now) -> {
            e.charge(caster, target);
            return 0;
        });
        t.register(EFFECT_CHARGE_DEST, (e, caster, target, sp, now) -> {
            e.chargeDest(caster, sp.maxRange());
            return 0;
        });
        t.register(EFFECT_TELEPORT_GRAVEYARD, (e, caster, target, sp, now) -> {
            e.teleportGraveyard(target, GraveyardManager.seeded());
            return 0;
        });
        t.register(EFFECT_PULL_TOWARDS, (e, caster, target, sp, now) -> {
            e.pullTowards(caster, target, sp.misc());
            return 0;
        });
        t.register(EFFECT_PULL_TOWARDS_DEST, (e, caster, target, sp, now) -> {
            if (caster != null) {
                float destX = caster.x + sp.maxRange() * (float) Math.cos(caster.o);
                float destY = caster.y + sp.maxRange() * (float) Math.sin(caster.o);
                e.pullTowardsDest(target, destX, destY, caster.z, sp.misc());
            }
            return 0;
        });
        t.register(EFFECT_LEAP, (e, caster, target, sp, now) -> {
            e.leapForward(target, sp.maxRange());
            return 0;
        });
        t.register(EFFECT_LEAP_BACK, (e, caster, target, sp, now) -> {
            e.leapBack(caster, target, sp.misc() / 10f, (sp.minDmg() + sp.maxDmg()) / 2 / 10f);
            return 0;
        });
        t.register(EFFECT_KNOCK_BACK, (e, caster, target, sp, now) -> {
            e.knockBack(caster, target, sp.misc() / 10f, Math.max(0, (sp.minDmg() + sp.maxDmg()) / 2) / 10f);
            return 0;
        });
        t.register(EFFECT_KNOCKBACK_FROM_POSITION, (e, caster, target, sp, now) -> {
            if (caster != null) {
                e.knockBackFromPosition(target, caster.x, caster.y,
                        sp.misc() / 10f, Math.max(0, (sp.minDmg() + sp.maxDmg()) / 2) / 10f);
            }
            return 0;
        });
    }
}
