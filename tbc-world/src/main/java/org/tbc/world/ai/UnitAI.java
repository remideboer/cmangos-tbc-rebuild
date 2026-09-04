package org.tbc.world.ai;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;

/** Per-creature brain. spec/05-domain/scripting-plugin-contract.md UnitAI. */
public interface UnitAI {
    String aiName();

    default boolean meleeEnabled() {
        return true;
    }

    default void update(Creature c, Player victim, int diffMs, EventAi.SpellCast cast, Runnable evade) {
    }
}
