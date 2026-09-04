package org.tbc.world.ai;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;

/** EventAI holder. Interpreter lives on Creature.eventAi. */
public final class EventCreatureAI implements UnitAI {
    @Override
    public String aiName() {
        return "EventAI";
    }

    @Override
    public void update(Creature c, Player victim, int diffMs, EventAi.SpellCast cast, Runnable evade) {
        if (c.eventAi != null) {
            c.eventAi.update(c, victim, diffMs, cast, evade);
        }
    }
}
