package org.tbc.world.companion;

import org.tbc.world.ai.MotionMaster;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.session.PetHandler;
import org.tbc.world.world.World;

/**
 * Follow owner; assist owner's victim with gated melee (not per-tick setHealth).
 */
public final class CompanionBehavior {
    private static final float FOLLOW_BROADCAST_DIST_SQ = 0.25f;

    private CompanionBehavior() {}

    public static void tick(World world, Player owner) {
        tick(world, owner, 50);
    }

    public static void tick(World world, Player owner, int diff) {
        if (owner == null || owner.companion == null || owner.pet == null || !owner.pet.alive) {
            return;
        }
        Creature body = owner.companion.worldBody();
        if (body != null) {
            if (body.meleeCooldownMs > 0) {
                body.meleeCooldownMs -= Math.max(0, diff);
            }
            followOwner(world, owner, body);
        } else {
            Unit combat = owner.pet.asUnit();
            combat.x = owner.x;
            combat.y = owner.y;
            combat.z = owner.z;
            combat.mapId = owner.mapId;
        }
        long target = owner.victim != 0 ? owner.victim : owner.pet.victim;
        if (target == 0 || body == null) {
            return;
        }
        Creature prey = world.map(owner.mapId, owner.instanceId).creatures.get(target);
        if (prey == null || !prey.alive()) {
            owner.pet.victim = 0;
            return;
        }
        owner.pet.victim = target;
        world.companionAssistMelee(owner, body, prey);
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
            // Encode from current (old) position before relocate so the client sees travel.
            spline = MotionMaster.monsterMove(body, pos[0], pos[1], pos[2], UpdateBuilder.RUN);
        }
        body.relocate(pos[0], pos[1], pos[2], owner.o);
        body.mapId = owner.mapId;
        world.map(owner.mapId, owner.instanceId).reindex(body, oldX, oldY);
        if (spline != null) {
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
            c.lastBroadcastX = pos[0];
            c.lastBroadcastY = pos[1];
            c.lastBroadcastZ = pos[2];
            c.hasBroadcastPos = true;
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
