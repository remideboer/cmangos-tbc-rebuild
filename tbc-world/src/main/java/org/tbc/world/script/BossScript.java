package org.tbc.world.script;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Unit;

import java.util.ArrayList;
import java.util.List;

/** Boss template: timers + spell ids from spec. Do not invent ids. */
public class BossScript {
    public final String scriptName;
    public final int mapId;
    public final List<Action> actions = new ArrayList<>();
    private final int[] nextDue;
    private boolean engaged;

    public record Action(String token, int spellId, int timerMs, boolean self) {}

    public BossScript(String scriptName, int mapId, List<Action> actions) {
        this.scriptName = scriptName;
        this.mapId = mapId;
        this.actions.addAll(actions);
        this.nextDue = new int[actions.size()];
        for (int i = 0; i < actions.size(); i++) {
            nextDue[i] = actions.get(i).timerMs;
        }
    }

    public void aggro() {
        engaged = true;
        for (int i = 0; i < actions.size(); i++) {
            nextDue[i] = actions.get(i).timerMs;
        }
    }

    public void reset() {
        engaged = false;
    }

    public List<Integer> update(Creature c, Unit victim, int diffMs, CastSink sink) {
        List<Integer> cast = new ArrayList<>();
        if (!engaged) {
            return cast;
        }
        for (int i = 0; i < actions.size(); i++) {
            Action a = actions.get(i);
            if (a.timerMs <= 0 || a.spellId == 0) {
                continue;
            }
            nextDue[i] -= diffMs;
            if (nextDue[i] <= 0) {
                nextDue[i] = a.timerMs;
                Unit t = a.self ? c : victim;
                sink.cast(c, t, a.spellId);
                cast.add(a.spellId);
            }
        }
        return cast;
    }

    @FunctionalInterface
    public interface CastSink {
        void cast(Creature c, Unit target, int spellId);
    }
}
