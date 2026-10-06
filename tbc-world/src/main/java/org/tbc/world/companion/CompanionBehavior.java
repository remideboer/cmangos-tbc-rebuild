package org.tbc.world.companion;

import org.tbc.world.ai.MotionMaster;
import org.tbc.world.combat.Combat;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.PetHandler;
import org.tbc.world.spell.SpellCastTargets;
import org.tbc.world.spell.SpellEngine;
import org.tbc.world.world.World;

/**
 * PetAI-shaped companion tick: follow left OOC; chase + auto-cast + melee in combat.
 */
public final class CompanionBehavior {
    private static final float FOLLOW_BROADCAST_DIST_SQ = 0.25f;
    private static final int AUTO_CAST_GCD_MS = 1500;
    private static final int FACING_SPLINE_ID = 1;

    private CompanionBehavior() {}

    public static void tick(World world, Player owner) {
        tick(world, owner, 50);
    }

    public static void tick(World world, Player owner, int diff) {
        if (owner == null || owner.companion == null || owner.pet == null || !owner.pet.alive) {
            return;
        }
        Companion companion = owner.companion;
        Creature body = companion.worldBody();
        if (body == null) {
            Unit combat = owner.pet.asUnit();
            combat.x = owner.x;
            combat.y = owner.y;
            combat.z = owner.z;
            combat.mapId = owner.mapId;
            return;
        }
        if (body.meleeCooldownMs > 0) {
            body.meleeCooldownMs -= Math.max(0, diff);
        }
        if (companion.castCooldownMs > 0) {
            companion.castCooldownMs -= Math.max(0, diff);
        }

        long target = owner.victim != 0 ? owner.victim : owner.pet.victim;
        if (target == 0) {
            clearCombatMotion(body);
            followOwner(world, owner, body);
            return;
        }
        Creature prey = world.map(owner.mapId, owner.instanceId).creatures.get(target);
        if (prey == null || !prey.alive()) {
            owner.pet.victim = 0;
            clearCombatMotion(body);
            followOwner(world, owner, body);
            return;
        }
        owner.pet.victim = target;
        ensureChasing(body, prey);

        if (tryAutoCast(world, owner, body, prey)) {
            return;
        }

        byte[] spline = body.motion.update(body, diff);
        if (spline != null) {
            broadcastMove(world, owner, body, spline);
        }
        if (!Combat.hasMeleeFacing(body, prey) && Combat.canReachWithMeleeAttack(body, prey)) {
            float o = Combat.angleTo(body.x, body.y, prey.x, prey.y);
            body.relocate(body.x, body.y, body.z, o);
            byte[] face = MotionMaster.monsterMoveFacingAngle(body, o, FACING_SPLINE_ID);
            broadcastMove(world, owner, body, face);
        }
        world.companionAssistMelee(owner, body, prey);
    }

    static void ensureChasing(Creature body, Creature prey) {
        body.inCombat = true;
        body.victim = prey.guid;
        body.setGuid(UpdateFields.UNIT_FIELD_TARGET, prey.guid);
        if (body.motion.type() != MotionMaster.CHASE || body.motion.target() != prey) {
            body.motion.moveChase(prey);
        }
    }

    static void clearCombatMotion(Creature body) {
        body.victim = 0;
        if (body.motion.type() == MotionMaster.CHASE) {
            body.motion.moveIdle();
        }
    }

    /**
     * Try one enabled bar spell (slots 3–6) when mana and range allow.
     * @return true if a cast was started (skip melee this tick)
     */
    static boolean tryAutoCast(World world, Player owner, Creature body, Creature prey) {
        Companion companion = owner.companion;
        if (companion.castCooldownMs > 0 || owner.session == null) {
            return false;
        }
        Player snap = companion.snapshot();
        for (int i = PetHandlerBar.SPELL_SLOT_START; i < PetHandlerBar.SPELL_SLOT_END; i++) {
            int packed = owner.pet.actionBar[i];
            int act = (packed >>> 24) & 0xFF;
            int spellId = packed & 0xFFFFFF;
            if (spellId == 0 || act != PetHandler.ACT_ENABLED) {
                continue;
            }
            if (!owner.pet.spells.contains(spellId)) {
                continue;
            }
            SpellEngine.SpellInfo sp = world.spells.info(spellId);
            if (sp == null) {
                continue;
            }
            if (sp.mana() > 0) {
                int power = snap.getInt(UpdateFields.UNIT_FIELD_POWER1);
                if (power < sp.mana()) {
                    continue;
                }
            }
            float range = sp.maxRange() > 0f ? sp.maxRange() : 5f;
            if (body.distance2d(prey) > range) {
                continue;
            }
            if (sp.mana() > 0) {
                int power = snap.getInt(UpdateFields.UNIT_FIELD_POWER1);
                snap.setInt(UpdateFields.UNIT_FIELD_POWER1, power - sp.mana());
            }
            float o = Combat.angleTo(body.x, body.y, prey.x, prey.y);
            body.relocate(body.x, body.y, body.z, o);
            byte[] face = MotionMaster.monsterMoveFacingAngle(body, o, FACING_SPLINE_ID);
            broadcastMove(world, owner, body, face);

            SpellCastTargets targets = new SpellCastTargets();
            targets.mask = SpellCastTargets.UNIT;
            targets.unitGuid = prey.guid;
            owner.session.send(Opcodes.SMSG_SPELL_START,
                    world.spells.encodeStart(owner.pet.guid, spellId, 0, sp.castTimeMs(), targets));
            if (sp.castTimeMs() == 0) {
                owner.session.send(Opcodes.SMSG_SPELL_GO,
                        world.spells.encodeGo(owner.pet.guid, prey.guid, spellId, world.nowMs(), targets));
                if (sp.minDmg() > 0 || sp.maxDmg() > 0) {
                    int dmg = Math.max(1, (sp.minDmg() + sp.maxDmg()) / 2);
                    world.onCreatureAttackedBySpell(owner, prey, dmg);
                    prey.setHealth(Math.max(0, prey.health() - dmg));
                    if (!prey.alive()) {
                        world.onCreatureKilled(owner, prey);
                    }
                }
            }
            companion.castCooldownMs = AUTO_CAST_GCD_MS;
            return true;
        }
        return false;
    }

    static void followOwner(World world, Player owner, Creature body) {
        Companion c = owner.companion;
        float oldX = body.x;
        float oldY = body.y;
        float oldZ = body.z;
        float[] pos = CompanionService.followPosition(owner);
        float dx = pos[0] - (c.hasBroadcastPos ? c.lastBroadcastX : oldX);
        float dy = pos[1] - (c.hasBroadcastPos ? c.lastBroadcastY : oldY);
        float dz = pos[2] - (c.hasBroadcastPos ? c.lastBroadcastZ : oldZ);
        boolean moved = !c.hasBroadcastPos || dx * dx + dy * dy + dz * dz > FOLLOW_BROADCAST_DIST_SQ;
        byte[] spline = null;
        if (moved) {
            spline = MotionMaster.monsterMove(body, pos[0], pos[1], pos[2], UpdateBuilder.RUN);
        }
        body.relocate(pos[0], pos[1], pos[2], owner.o);
        body.mapId = owner.mapId;
        world.map(owner.mapId, owner.instanceId).reindex(body, oldX, oldY);
        if (spline != null) {
            broadcastMove(world, owner, body, spline);
            c.lastBroadcastX = pos[0];
            c.lastBroadcastY = pos[1];
            c.lastBroadcastZ = pos[2];
            c.hasBroadcastPos = true;
        }
    }

    static void broadcastMove(World world, Player owner, Creature body, byte[] spline) {
        if (spline == null) {
            return;
        }
        if (owner.session != null) {
            owner.session.send(Opcodes.SMSG_MONSTER_MOVE, spline);
        }
        if (body.messageToSet != null) {
            body.messageToSet.accept(Opcodes.SMSG_MONSTER_MOVE, spline);
        } else {
            for (Player pl : world.map(owner.mapId, owner.instanceId).nearbyPlayers(body, GameMap.VISIBILITY)) {
                if (pl == owner || pl.session == null) {
                    continue;
                }
                pl.session.send(Opcodes.SMSG_MONSTER_MOVE, spline);
            }
        }
    }

    /** First enabled spell on the companion pet bar (slots 3–6). */
    public static int firstEnabledSpell(org.tbc.world.entity.Pet pet) {
        for (int i = PetHandlerBar.SPELL_SLOT_START; i < PetHandlerBar.SPELL_SLOT_END; i++) {
            int packed = pet.actionBar[i];
            int act = (packed >>> 24) & 0xFF;
            int id = packed & 0xFFFFFF;
            if (id != 0 && act == PetHandler.ACT_ENABLED) {
                return id;
            }
        }
        return 0;
    }
}
