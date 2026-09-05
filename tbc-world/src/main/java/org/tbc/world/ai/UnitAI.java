package org.tbc.world.ai;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;

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
     * CMaNGOS UnitAI::MoveInLineOfSight. Tick calls this so World does not grow an inline pull loop.
     */
    default void updateOoc(Creature c, Iterable<Player> nearby, org.tbc.world.combat.Factions factions,
            java.util.function.BiPredicate<Creature, Player> los, java.util.function.Consumer<Player> engage) {
        if (!aggroOnSight() || nearby == null || engage == null) {
            return;
        }
        for (Player pl : nearby) {
            if (org.tbc.world.combat.Combat.canAggroOnSight(c, pl, factions)
                    && (los == null || los.test(c, pl))) {
                engage.accept(pl);
                return;
            }
        }
    }
}
