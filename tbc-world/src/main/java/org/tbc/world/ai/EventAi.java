package org.tbc.world.ai;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Unit;

import java.util.ArrayList;
import java.util.List;

/** EventAI interpreter. spec/05-domain/eventai-catalog.md — ACID rows as-is. */
public final class EventAi {
    public static final int EVENT_ON_AGGRO = 4;
    public static final int ACTION_CAST = 11;

    public final List<Row> rows = new ArrayList<>();
    private boolean inCombat;

    public record Row(int eventType, int eventChance, int action1, int param1, int param2, int param3) {}

    public void onAggro(Creature c, Unit victim, SpellCast cast) {
        inCombat = true;
        for (Row r : rows) {
            if (r.eventType == EVENT_ON_AGGRO && r.eventChance >= 100) {
                if (r.action1 == ACTION_CAST && r.param1 != 0) {
                    Unit t = r.param2 == 0 ? c : victim;
                    cast.cast(c, t, r.param1);
                }
            }
        }
    }

    public void update(Creature c, Unit victim, SpellCast cast, int diffMs) {
        if (!inCombat || victim == null) {
            return;
        }
        for (Row r : rows) {
            if (r.eventType == 0 && r.param1 != 0) {
                // EVENT_T_TIMER in combat — simplified: param1 is spell on timer param2
                if (r.action1 == ACTION_CAST) {
                    cast.cast(c, victim, r.param1);
                }
            }
        }
    }

    public void reset() {
        inCombat = false;
    }

    @FunctionalInterface
    public interface SpellCast {
        void cast(Creature c, Unit target, int spellId);
    }
}
