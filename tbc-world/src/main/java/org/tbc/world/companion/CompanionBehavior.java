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

import java.util.LinkedHashMap;

/**
 * PetAI-shaped companion tick: follow left OOC; chase + auto-cast + melee in combat.
 */
public final class CompanionBehavior {
    private static final float FOLLOW_BROADCAST_DIST_SQ = 0.25f;
    private static final int AUTO_CAST_GCD_MS = 1500;
    private static final int FACING_SPLINE_ID = 1;
    /** Spells with maxRange above this act like Imp/warlock minions (hold range, no white melee). */
    private static final float RANGED_SPELL_RANGE = 10f;
    /** Leave combat and run when at or below 20% max health. */
    private static final int FLEE_HEALTH_DENOM = 5;
    private static final float FLEE_YARDS = 18f;

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
        if (owner.pet.retreating) {
            clearCombatMotion(body);
            followOwner(world, owner, body);
            owner.pet.retreating = false;
            return;
        }
        if (shouldFlee(body)) {
            if (!companion.fleeing) {
                fleeFromCombat(world, owner, body);
                companion.fleeing = true;
            }
            return;
        }
        companion.fleeing = false;

        long target = owner.pet.victim != 0
                ? owner.pet.victim
                : owner.pet.commandState != PetHandlerBar.COMMAND_STAY
                        && owner.pet.reactState != PetHandlerBar.REACT_PASSIVE ? owner.victim : 0;
        if (target == 0) {
            clearCombatMotion(body);
            if (owner.pet.commandState == PetHandlerBar.COMMAND_FOLLOW) {
                followOwner(world, owner, body);
            }
            return;
        }
        Creature prey = world.map(owner.mapId, owner.instanceId).creatures.get(target);
        if (prey == null || !prey.alive()) {
            leaveCombat(world, owner, body, target);
            if (owner.pet.commandState == PetHandlerBar.COMMAND_FOLLOW) {
                followOwner(world, owner, body);
            }
            return;
        }
        if (isFriendlyCompanionTarget(owner, body, prey)) {
            owner.pet.victim = 0;
            companion.clearRequestedSpell();
            clearCombatMotion(body);
            if (owner.pet.commandState == PetHandlerBar.COMMAND_FOLLOW) {
                followOwner(world, owner, body);
            }
            return;
        }
        owner.pet.victim = target;
        markCombatTarget(body, prey);
        if (companion.isCasting()) {
            updatePendingCast(world, owner, body, prey, diff);
            return;
        }
        AutoCastChoice autoCast = autoCastChoice(world, owner, prey);
        if (autoCast != null) {
            float castRange = Math.max(5f, autoCast.spell().maxRange());
            if (body.distance2d(prey) > castRange) {
                ensureChasing(body, prey, castRange * 0.85f);
                advanceChase(world, owner, body, diff);
                return;
            }
            if (body.motion.type() == MotionMaster.CHASE) {
                body.motion.moveIdle();
            }
            tryAutoCast(world, owner, body, prey, autoCast);
            return;
        }

        // Imp-style: while a ranged damage spell is known/ready, do not close to melee.
        if (isRangedCaster(world, owner)) {
            float hold = rangedHoldDistance(world, owner);
            if (body.distance2d(prey) > hold) {
                ensureChasing(body, prey, hold * 0.85f);
                advanceChase(world, owner, body, diff);
            } else if (body.motion.type() == MotionMaster.CHASE) {
                body.motion.moveIdle();
            }
            return;
        }

        // Hunter-pet style melee: close to melee range then white swings.
        ensureChasing(body, prey, -1f);
        advanceChase(world, owner, body, diff);
        if (!Combat.hasMeleeFacing(body, prey) && Combat.canReachWithMeleeAttack(body, prey)) {
            float o = Combat.angleTo(body.x, body.y, prey.x, prey.y);
            body.relocate(body.x, body.y, body.z, o);
            byte[] face = MotionMaster.monsterMoveFacingAngle(body, o, FACING_SPLINE_ID);
            broadcastMove(world, owner, body, face);
        }
        world.companionAssistMelee(owner, body, prey);
    }

    static void ensureChasing(Creature body, Creature prey, float distance) {
        markCombatTarget(body, prey);
        if (body.motion.type() != MotionMaster.CHASE || body.motion.target() != prey
                || Math.abs(body.motion.chaseDistance() - distance) > 0.01f) {
            body.motion.moveChase(prey, distance);
        }
    }

    private static void markCombatTarget(Creature body, Creature prey) {
        body.inCombat = true;
        body.victim = prey.guid;
        body.setGuid(UpdateFields.UNIT_FIELD_TARGET, prey.guid);
    }

    private static void advanceChase(World world, Player owner, Creature body, int diff) {
        byte[] spline = world.advanceCompanionMotion(owner, body, diff);
        if (spline != null) {
            broadcastMove(world, owner, body, spline);
        }
    }

    static void clearCombatMotion(Creature body) {
        body.victim = 0;
        body.inCombat = false;
        body.setGuid(UpdateFields.UNIT_FIELD_TARGET, 0);
        body.setInt(UpdateFields.UNIT_FIELD_FLAGS,
                body.getInt(UpdateFields.UNIT_FIELD_FLAGS) & ~Unit.UNIT_FLAG_IN_COMBAT);
        if (body.motion.type() == MotionMaster.CHASE) {
            body.motion.moveIdle();
        }
    }

    static boolean shouldFlee(Creature body) {
        int max = body.maxHealth();
        return max > 0 && body.health() * FLEE_HEALTH_DENOM <= max;
    }

    /**
     * Drop the current victim, clear IN_COMBAT on the wire, and run away from the prey.
     */
    static void fleeFromCombat(World world, Player owner, Creature body) {
        long preyGuid = owner.pet != null ? owner.pet.victim : body.victim;
        Creature prey = preyGuid != 0
                ? world.map(owner.mapId, owner.instanceId).creatures.get(preyGuid) : null;
        if (owner.pet != null) {
            owner.pet.victim = 0;
        }
        owner.companion.clearRequestedSpell();
        cancelPendingCast(owner.companion);
        clearCombatMotion(body);
        body.setInt(UpdateFields.UNIT_FIELD_FLAGS,
                body.getInt(UpdateFields.UNIT_FIELD_FLAGS) | Unit.UNIT_FLAG_FLEEING);
        float dx = 1f;
        float dy = 0f;
        if (prey != null) {
            dx = body.x - prey.x;
            dy = body.y - prey.y;
            float len = (float) Math.hypot(dx, dy);
            if (len < 0.05f) {
                dx = 1f;
                dy = 0f;
                len = 1f;
            }
            dx /= len;
            dy /= len;
        }
        float nx = body.x + dx * FLEE_YARDS;
        float ny = body.y + dy * FLEE_YARDS;
        float nz = body.z;
        byte[] spline = MotionMaster.monsterMove(body, nx, ny, nz, UpdateBuilder.RUN);
        float ox = body.x;
        float oy = body.y;
        body.relocate(nx, ny, nz, body.o);
        world.map(owner.mapId, owner.instanceId).reindex(body, ox, oy);
        broadcastMove(world, owner, body, spline);
        sendLeaveCombat(world, owner, body, preyGuid);
    }

    /** Prey died or was dropped: stop melee anim on the 8606 client. */
    public static void leaveCombat(World world, Player owner, Creature body, long preyGuid) {
        if (owner.pet != null && (preyGuid == 0 || owner.pet.victim == preyGuid || !alivePrey(world, owner, owner.pet.victim))) {
            owner.pet.victim = 0;
        }
        if (owner.companion != null) {
            owner.companion.fleeing = false;
            cancelPendingCast(owner.companion);
        }
        if (body != null) {
            body.setInt(UpdateFields.UNIT_FIELD_FLAGS,
                    body.getInt(UpdateFields.UNIT_FIELD_FLAGS) & ~Unit.UNIT_FLAG_FLEEING);
            clearCombatMotion(body);
        }
        sendLeaveCombat(world, owner, body, preyGuid);
    }

    private static boolean alivePrey(World world, Player owner, long guid) {
        if (guid == 0) {
            return false;
        }
        Creature c = world.map(owner.mapId, owner.instanceId).creatures.get(guid);
        return c != null && c.alive();
    }

    static void sendLeaveCombat(World world, Player owner, Creature body, long preyGuid) {
        if (owner == null || body == null) {
            return;
        }
        byte[] stop = preyGuid != 0 ? world.combat.encodeAttackStop(body.guid, preyGuid, false) : null;
        var vis = UpdateBuilder.maybeCompress(UpdateBuilder.values(body,
                UpdateFields.UNIT_FIELD_TARGET, UpdateFields.UNIT_FIELD_TARGET + 1,
                UpdateFields.UNIT_FIELD_FLAGS));
        LinkedHashMap<Long, Player> viewers = new LinkedHashMap<>();
        if (owner.session != null) {
            viewers.put(owner.guid, owner);
        }
        GameMap map = world.map(owner.mapId, owner.instanceId);
        for (Player pl : map.nearbyPlayers(body, GameMap.VISIBILITY)) {
            viewers.putIfAbsent(pl.guid, pl);
        }
        for (Player pl : viewers.values()) {
            if (pl.session == null) {
                continue;
            }
            if (stop != null) {
                pl.session.send(Opcodes.SMSG_ATTACKSTOP, stop);
            }
            pl.session.send(vis.opcode(), vis.payload());
        }
    }

    /**
     * Try one enabled bar spell (slots 3–6) when mana and range allow.
     * Non-instant spells send SPELL_START and finish on later ticks (real cast time).
     * @return true if a cast was started or is in progress (skip melee this tick)
     */
    static boolean tryAutoCast(World world, Player owner, Creature body, Creature prey, AutoCastChoice choice) {
        Companion companion = owner.companion;
        if (companion.castCooldownMs > 0 || owner.session == null || companion.isCasting()) {
            return false;
        }
        if (isFriendlyCompanionTarget(owner, body, prey)) {
            owner.pet.victim = 0;
            companion.clearRequestedSpell();
            return false;
        }
        Player snap = companion.snapshot();
        int spellId = choice.spellId();
        SpellEngine.SpellInfo sp = choice.spell();
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
        int castMs = Math.max(0, sp.castTimeMs());
        // CMaNGOS PetAI Cast → SpellStart with normal cast time (TRIGGERED_PET_CAST ≠ instant).
        byte[] start = world.spells.encodeStart(owner.pet.guid, spellId, 0, castMs, targets);
        sendToCombatViewers(world, owner, body, prey, Opcodes.SMSG_SPELL_START, start);
        if (choice.requested()) {
            companion.clearRequestedSpell();
        }
        if (castMs <= 0) {
            finishCast(world, owner, body, prey, spellId, sp);
            companion.castCooldownMs = AUTO_CAST_GCD_MS;
            return true;
        }
        companion.beginCast(spellId, prey.guid, castMs, sp.mana());
        return true;
    }

    private static void updatePendingCast(World world, Player owner, Creature body, Creature prey, int diff) {
        Companion companion = owner.companion;
        if (prey == null || !prey.alive() || prey.guid != companion.pendingTargetGuid()
                || isFriendlyCompanionTarget(owner, body, prey)) {
            cancelPendingCast(companion);
            return;
        }
        companion.advanceCast(diff);
        if (companion.pendingRemainMs() > 0) {
            return;
        }
        int spellId = companion.pendingSpellId();
        SpellEngine.SpellInfo sp = world.spells.info(spellId);
        companion.clearPendingCast();
        if (sp == null) {
            return;
        }
        finishCast(world, owner, body, prey, spellId, sp);
        companion.castCooldownMs = AUTO_CAST_GCD_MS;
    }

    private static void finishCast(World world, Player owner, Creature body, Creature prey,
            int spellId, SpellEngine.SpellInfo sp) {
        SpellCastTargets targets = new SpellCastTargets();
        targets.mask = SpellCastTargets.UNIT;
        targets.unitGuid = prey.guid;
        byte[] go = world.spells.encodeGo(owner.pet.guid, prey.guid, spellId, world.nowMs(), targets);
        int dmg = 0;
        byte[] damageLog = null;
        byte[] hpPayload = null;
        int hpOpcode = 0;
        long now = world.nowMs();
        Player snap = owner.companion.snapshot();
        if (isCombatDamageEffect(sp.effect())) {
            dmg = world.spells.apply(body, prey, sp, now);
            if (dmg > 0) {
                prey.threatManager.add(body, dmg);
                prey.threat += dmg;
                world.pullOrRetarget(body, prey, owner);
                damageLog = world.spells.encodeDamageLog(prey.guid, owner.pet.guid, sp, dmg);
                var hp = UpdateBuilder.maybeCompress(UpdateBuilder.values(prey, UpdateFields.UNIT_FIELD_HEALTH));
                hpOpcode = hp.opcode();
                hpPayload = hp.payload();
            }
        } else if (sp.effect() == SpellEngine.EFFECT_POWER_DRAIN) {
            int amount = Math.max(0, (sp.minDmg() + sp.maxDmg()) / 2);
            world.spells.powerDrain(snap, prey, amount);
        } else {
            world.spells.apply(body, prey, sp, now);
        }
        LinkedHashMap<Long, Player> viewers = combatViewers(world, owner, body, prey);
        for (Player pl : viewers.values()) {
            if (pl.session == null) {
                continue;
            }
            pl.session.send(Opcodes.SMSG_SPELL_GO, go);
            if (damageLog != null) {
                pl.session.send(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG, damageLog);
                if (prey.alive()) {
                    pl.session.send(hpOpcode, hpPayload);
                }
            }
        }
        if (dmg > 0 && !prey.alive()) {
            leaveCombat(world, owner, body, prey.guid);
            world.onCreatureKilled(owner, prey);
        }
    }

    private static void cancelPendingCast(Companion companion) {
        if (companion == null) {
            return;
        }
        if (companion.pendingSpellId() == 0) {
            companion.clearPendingCast();
            return;
        }
        Player snap = companion.snapshot();
        int refund = companion.pendingManaCost();
        if (refund > 0 && snap != null) {
            int cur = snap.getInt(UpdateFields.UNIT_FIELD_POWER1);
            int max = snap.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1);
            snap.setInt(UpdateFields.UNIT_FIELD_POWER1, Math.min(max > 0 ? max : cur + refund, cur + refund));
        }
        companion.clearPendingCast();
    }

    /** True when an enabled combat-damage bar spell has Imp-like ranged maxRange. */
    static boolean isRangedCaster(World world, Player owner) {
        return firstRangedAutoSpell(world, owner) != null;
    }

    private static float rangedHoldDistance(World world, Player owner) {
        SpellEngine.SpellInfo sp = firstRangedAutoSpell(world, owner);
        return sp == null ? 25f : Math.max(5f, sp.maxRange());
    }

    private static SpellEngine.SpellInfo firstRangedAutoSpell(World world, Player owner) {
        if (owner == null || owner.pet == null || world == null) {
            return null;
        }
        Player snap = owner.companion != null ? owner.companion.snapshot() : null;
        for (int i = PetHandlerBar.SPELL_SLOT_START; i < PetHandlerBar.SPELL_SLOT_END; i++) {
            int packed = owner.pet.actionBar[i];
            int act = (packed >>> 24) & 0xFF;
            int spellId = packed & 0xFFFFFF;
            if (spellId == 0 || act != PetHandler.ACT_ENABLED || !owner.pet.spells.contains(spellId)) {
                continue;
            }
            SpellEngine.SpellInfo sp = world.spells.info(spellId);
            if (sp == null || !isCombatDamageEffect(sp.effect()) || sp.maxRange() < RANGED_SPELL_RANGE) {
                continue;
            }
            if (snap != null && !hasPower(snap, sp)) {
                continue;
            }
            return sp;
        }
        return null;
    }

    private static LinkedHashMap<Long, Player> combatViewers(World world, Player owner, Creature body, Creature prey) {
        LinkedHashMap<Long, Player> viewers = new LinkedHashMap<>();
        if (owner != null) {
            viewers.put(owner.guid, owner);
        }
        GameMap map = world.map(owner.mapId, owner.instanceId);
        if (body != null) {
            for (Player pl : map.nearbyPlayers(body, GameMap.VISIBILITY)) {
                viewers.putIfAbsent(pl.guid, pl);
            }
        }
        if (prey != null) {
            for (Player pl : map.nearbyPlayers(prey, GameMap.VISIBILITY)) {
                viewers.putIfAbsent(pl.guid, pl);
            }
        }
        return viewers;
    }

    private static void sendToCombatViewers(World world, Player owner, Creature body, Creature prey,
            int opcode, byte[] payload) {
        for (Player pl : combatViewers(world, owner, body, prey).values()) {
            if (pl.session != null) {
                pl.session.send(opcode, payload);
            }
        }
    }

    private static AutoCastChoice autoCastChoice(World world, Player owner, Creature prey) {
        if (isFriendlyCompanionTarget(owner, owner.companion.worldBody(), prey)) {
            owner.pet.victim = 0;
            owner.companion.clearRequestedSpell();
            return null;
        }
        Player snap = owner.companion.snapshot();
        int requested = owner.companion.requestedSpellId();
        if (requested != 0 && owner.companion.requestedSpellTarget() == prey.guid) {
            SpellEngine.SpellInfo sp = world.spells.info(requested);
            if (owner.pet.spells.contains(requested) && sp != null && hasPower(snap, sp)) {
                return new AutoCastChoice(requested, sp, true);
            }
            owner.companion.clearRequestedSpell();
        }
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
            if (sp == null || !isCombatDamageEffect(sp.effect())) {
                continue;
            }
            if (!hasPower(snap, sp)) {
                continue;
            }
            return new AutoCastChoice(spellId, sp, false);
        }
        return null;
    }

    /** Companion must never cast harmful effects at herself or the owner. */
    private static boolean isFriendlyCompanionTarget(Player owner, Creature body, Creature prey) {
        if (owner == null || prey == null) {
            return true;
        }
        if (body != null && prey.guid == body.guid) {
            return true;
        }
        if (owner.pet != null && prey.guid == owner.pet.guid) {
            return true;
        }
        return prey.guid == owner.guid;
    }

    private static boolean isCombatDamageEffect(int effect) {
        return effect == SpellEngine.EFFECT_SCHOOL_DAMAGE
                || effect == SpellEngine.EFFECT_WEAPON_DAMAGE
                || effect == SpellEngine.EFFECT_WEAPON_DAMAGE_NOSCHOOL;
    }

    private static boolean hasPower(Player snap, SpellEngine.SpellInfo spell) {
        return spell.mana() <= 0 || snap.getInt(UpdateFields.UNIT_FIELD_POWER1) >= spell.mana();
    }

    record AutoCastChoice(int spellId, SpellEngine.SpellInfo spell, boolean requested) {}

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
