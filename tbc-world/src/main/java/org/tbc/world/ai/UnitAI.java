package org.tbc.world.ai;

import org.tbc.world.combat.Combat;
import org.tbc.world.combat.Factions;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

/** Per-creature brain. spec/05-domain/scripting-plugin-contract.md UnitAI. */
public interface UnitAI {
    String aiName();

    default boolean meleeEnabled() {
        return true;
    }

    /** C++ REACT_AGGRESSIVE MoveInLineOfSight. Null/Totem/Pet do not pull. */
    default boolean aggroOnSight() {
        return meleeEnabled();
    }

    default void update(Creature c, Player victim, int diffMs, EventAi.SpellCast cast, Runnable evade) {
    }

    /**
     * CMaNGOS UnitAI::MoveInLineOfSight. Players first, then hostile creatures
     * (guards / faction NPCs DetectOrAttack — GuardAI.cpp else branch).
     */
    default void updateOoc(Creature c, Iterable<Player> nearbyPlayers, Iterable<Creature> nearbyCreatures,
            Factions factions, BiPredicate<Creature, Unit> los, Consumer<Unit> engage) {
        if (!aggroOnSight() || engage == null) {
            return;
        }
        if (nearbyPlayers != null) {
            for (Player pl : nearbyPlayers) {
                if (Combat.canAggroOnSight(c, pl, factions) && (los == null || los.test(c, pl))) {
                    engage.accept(pl);
                    return;
                }
            }
        }
        if (nearbyCreatures != null) {
            for (Creature other : nearbyCreatures) {
                if (other == null || other == c) {
                    continue;
                }
                if (Combat.canAggroOnSight(c, other, factions) && (los == null || los.test(c, other))) {
                    engage.accept(other);
                    return;
                }
            }
        }
    }
}
