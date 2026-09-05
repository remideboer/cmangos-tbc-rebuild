package org.tbc.world.combat;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;

/**
 * CMaNGOS Relations.cpp CanAttack / CanAttackOnSight (starter-zone; no duel/FFA).
 * World and Combat call this; no TCP.
 */
public final class Relations {
    private static final int UNATTACKABLE = Unit.UNIT_FLAG_SPAWNING | Unit.UNIT_FLAG_NOT_ATTACKABLE_1
            | Unit.UNIT_FLAG_UNTARGETABLE | Unit.UNIT_FLAG_TAXI_FLIGHT | Unit.UNIT_FLAG_UNINTERACTIBLE;

    private Relations() {
    }

    /** CMaNGOS Creature::CanInitiateAttack (stun/feign omitted). */
    public static boolean canInitiateAttack(Creature c) {
        if (c == null || !c.alive()) {
            return false;
        }
        int flags = c.getInt(UpdateFields.UNIT_FIELD_FLAGS);
        return (flags & (Unit.UNIT_FLAG_SPAWNING | Unit.UNIT_FLAG_UNINTERACTIBLE)) == 0;
    }

    /**
     * CMaNGOS Unit::CanAttack. PC↔NPC: not a friend. NPC↔NPC: either is enemy.
     */
    public static boolean canAttack(Unit attacker, Unit target, Factions factions) {
        if (attacker == null || target == null || factions == null) {
            return false;
        }
        if (attacker instanceof Creature && target instanceof Player pl
                && (pl.getInt(UpdateFields.PLAYER_FLAGS) & Player.PLAYER_FLAGS_GHOST) != 0) {
            return false;
        }
        int tflags = target.getInt(UpdateFields.UNIT_FIELD_FLAGS);
        if ((tflags & UNATTACKABLE) != 0) {
            return false;
        }
        boolean thisPc = (attacker.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_PLAYER_CONTROLLED) != 0;
        boolean unitPc = (tflags & Unit.UNIT_FLAG_PLAYER_CONTROLLED) != 0;
        if (thisPc) {
            if ((tflags & Unit.UNIT_FLAG_IMMUNE_TO_PLAYER) != 0) {
                return false;
            }
        } else if ((tflags & Unit.UNIT_FLAG_IMMUNE_TO_NPC) != 0) {
            return false;
        }
        int aflags = attacker.getInt(UpdateFields.UNIT_FIELD_FLAGS);
        if (unitPc) {
            if ((aflags & Unit.UNIT_FLAG_IMMUNE_TO_PLAYER) != 0) {
                return false;
            }
        } else if ((aflags & Unit.UNIT_FLAG_IMMUNE_TO_NPC) != 0) {
            return false;
        }
        if (thisPc || unitPc) {
            return !factions.isFriend(attacker, target);
        }
        return factions.isHostile(attacker, target) || factions.isHostile(target, attacker);
    }

    /** CMaNGOS Unit::CanAttackOnSight (no stealth / pet-disabled). */
    public static boolean canAttackOnSight(Unit attacker, Unit target, Factions factions) {
        if (target instanceof Creature victim && victim.evading) {
            return false;
        }
        return canAttack(attacker, target, factions) && factions.isHostile(attacker, target);
    }
}
