package org.tbc.world.ai;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;

/** EventAI interpreter. spec/05-domain/eventai-catalog.md + scripting-plugin-contract.md */
public final class EventAi {
    public static final int EVENT_UPDATE_TIME = 500;
    public static final int MAX_ACTIONS = 3;
    public static final int MAX_PHASE = 32;

    public static final int EVENT_TIMER_IN_COMBAT = 0;
    public static final int EVENT_TIMER_OOC = 1;
    public static final int EVENT_HP = 2;
    public static final int EVENT_AGGRO = 4;
    public static final int EVENT_DEATH = 6;
    public static final int EVENT_EVADE = 7;
    public static final int EVENT_SPAWNED = 11;
    public static final int EVENT_REACHED_HOME = 21;

    public static final int ACTION_NONE = 0;
    public static final int ACTION_CAST = 11;
    public static final int ACTION_SET_PHASE = 22;
    public static final int ACTION_INC_PHASE = 23;
    public static final int ACTION_EVADE = 24;

    public static final int TARGET_SELF = 0;
    public static final int TARGET_HOSTILE = 1;

    public static final int EFLAG_REPEATABLE = 1;
    public static final int EFLAG_RANDOM_ACTION = 32;
    public static final int EFLAG_DEBUG_ONLY = 128;
    public static final int EFLAG_COMBAT_ACTION = 1024;

    public static final SpellCast NOOP = (c, t, id) -> {};

    public record Action(int type, int param1, int param2, int param3) {
        public static Action none() {
            return new Action(ACTION_NONE, 0, 0, 0);
        }

        public static Action cast(int spellId, int target) {
            return new Action(ACTION_CAST, spellId, target, 0);
        }

        public static Action setPhase(int phase) {
            return new Action(ACTION_SET_PHASE, phase, 0, 0);
        }

        public static Action incPhase(int step) {
            return new Action(ACTION_INC_PHASE, step, 0, 0);
        }

        public static Action evade() {
            return new Action(ACTION_EVADE, 0, 0, 0);
        }
    }

    /** One creature_ai_scripts row. */
    public record Script(
            int eventType,
            int inversePhaseMask,
            int chance,
            int flags,
            int param1,
            int param2,
            int param3,
            int param4,
            Action a1,
            Action a2,
            Action a3) {
        public Action action(int i) {
            if (i <= 0) {
                return a1;
            }
            if (i == 1) {
                return a2;
            }
            return a3;
        }

        public static Script aggroCast(int spellId) {
            return new Script(EVENT_AGGRO, 0, 100, 0, 0, 0, 0, 0,
                    Action.cast(spellId, TARGET_SELF), Action.none(), Action.none());
        }

        public static Script timerInCombat(int initMin, int repeatMin, int spellId, int target) {
            return new Script(EVENT_TIMER_IN_COMBAT, 0, 100, EFLAG_REPEATABLE,
                    initMin, initMin, repeatMin, repeatMin,
                    Action.cast(spellId, target), Action.none(), Action.none());
        }

        public static Script timerOoc(int initMin, int repeatMin, int spellId, int target) {
            return new Script(EVENT_TIMER_OOC, 0, 100, EFLAG_REPEATABLE,
                    initMin, initMin, repeatMin, repeatMin,
                    Action.cast(spellId, target), Action.none(), Action.none());
        }
    }

    private static final class Holder {
        final Script script;
        boolean enabled = true;
        int timer;

        Holder(Script script) {
            this.script = script;
        }
    }

    private final IntSupplier urand;
    private final List<Holder> holders = new ArrayList<>();
    private boolean inCombat;
    private int phase;
    private int eventUpdateTime = EVENT_UPDATE_TIME;
    private int eventDiff;

    public EventAi() {
        this(() -> ThreadLocalRandom.current().nextInt());
    }

    public EventAi(IntSupplier urand) {
        this.urand = urand == null ? () -> ThreadLocalRandom.current().nextInt() : urand;
    }

    public void load(List<Script> scripts) {
        holders.clear();
        if (scripts == null) {
            return;
        }
        for (Script s : scripts) {
            if (s == null || (s.flags() & EFLAG_DEBUG_ONLY) != 0) {
                continue;
            }
            Holder h = new Holder(s);
            h.timer = initialTimer(s);
            holders.add(h);
        }
    }

    public void onSpawned(Creature c, SpellCast cast) {
        processImmediate(EVENT_SPAWNED, c, null, cast, null);
    }

    public void onAggro(Creature c, Unit victim, SpellCast cast) {
        inCombat = true;
        for (Holder h : holders) {
            if (h.script.eventType() == EVENT_TIMER_IN_COMBAT) {
                h.timer = h.script.param1();
            }
        }
        processImmediate(EVENT_AGGRO, c, victim, cast, null);
    }

    public void onDeath(Creature c, Unit killer, SpellCast cast) {
        processImmediate(EVENT_DEATH, c, killer, cast, null);
        inCombat = false;
    }

    public void onEvade(Creature c, SpellCast cast) {
        processImmediate(EVENT_EVADE, c, null, cast, null);
        inCombat = false;
        for (Holder h : holders) {
            if (h.script.eventType() == EVENT_TIMER_OOC) {
                h.timer = h.script.param1();
                h.enabled = true;
            }
        }
    }

    public void onReachedHome(Creature c, SpellCast cast) {
        processImmediate(EVENT_REACHED_HOME, c, null, cast, null);
    }

    public void update(Creature c, Unit victim, int diffMs, SpellCast cast) {
        update(c, victim, diffMs, cast, null);
    }

    public void update(Creature c, Unit victim, int diffMs, SpellCast cast, Runnable evadeAction) {
        if (diffMs <= 0) {
            return;
        }
        if (eventUpdateTime < diffMs) {
            eventDiff += diffMs;
            for (Holder h : holders) {
                if (!h.enabled) {
                    continue;
                }
                if ((h.script.inversePhaseMask() & (1 << phase)) != 0) {
                    continue;
                }
                if (h.timer > 0) {
                    if (h.timer > eventDiff) {
                        h.timer -= eventDiff;
                    } else {
                        h.timer = 0;
                    }
                }
                if (h.timer == 0 && isTimerExecuted(h.script.eventType())) {
                    processEvent(h, c, victim, cast, evadeAction);
                }
            }
            eventDiff = 0;
            eventUpdateTime = EVENT_UPDATE_TIME;
        } else {
            eventDiff += diffMs;
            eventUpdateTime -= diffMs;
        }
    }

    public void reset() {
        inCombat = false;
        phase = 0;
        eventUpdateTime = EVENT_UPDATE_TIME;
        eventDiff = 0;
        for (Holder h : holders) {
            h.enabled = true;
            h.timer = initialTimer(h.script);
        }
    }

    private void processImmediate(int eventType, Creature c, Unit victim, SpellCast cast, Runnable evadeAction) {
        for (Holder h : holders) {
            if (h.enabled && h.script.eventType() == eventType) {
                processEvent(h, c, victim, cast, evadeAction);
            }
        }
    }

    private void processEvent(Holder h, Creature c, Unit victim, SpellCast cast, Runnable evadeAction) {
        if (!h.enabled) {
            return;
        }
        if ((h.script.inversePhaseMask() & (1 << phase)) != 0) {
            return;
        }
        if ((h.script.flags() & EFLAG_COMBAT_ACTION) != 0 && !inCombat) {
            return;
        }
        if (!checkEvent(h, c, victim)) {
            return;
        }
        int chanceRoll = urand.getAsInt();
        if (chanceRoll < 0) {
            chanceRoll = -chanceRoll;
        }
        if (h.script.chance() <= chanceRoll % 100) {
            resetEvent(h);
            return;
        }
        boolean success;
        if ((h.script.flags() & EFLAG_RANDOM_ACTION) != 0) {
            success = processRandomAction(h, c, victim, cast, evadeAction);
        } else {
            success = processAction(h.script.a1(), c, victim, cast, evadeAction);
            if ((h.script.flags() & EFLAG_COMBAT_ACTION) == 0 || success) {
                processAction(h.script.a2(), c, victim, cast, evadeAction);
                processAction(h.script.a3(), c, victim, cast, evadeAction);
            }
        }
        if (success || (h.script.flags() & EFLAG_COMBAT_ACTION) == 0) {
            resetEvent(h);
        }
    }

    private boolean checkEvent(Holder h, Creature c, Unit victim) {
        int type = h.script.eventType();
        if (type == EVENT_TIMER_IN_COMBAT) {
            return inCombat;
        }
        if (type == EVENT_TIMER_OOC) {
            return !inCombat;
        }
        if (type == EVENT_HP) {
            if (!inCombat || c == null || c.maxHealth() <= 0) {
                return false;
            }
            int pct = (c.health() * 100) / c.maxHealth();
            return pct <= h.script.param1() && pct >= h.script.param2();
        }
        return true;
    }

    private boolean processRandomAction(Holder h, Creature c, Unit victim, SpellCast cast, Runnable evadeAction) {
        int count = 0;
        for (int i = 0; i < MAX_ACTIONS; i++) {
            if (h.script.action(i).type() != ACTION_NONE) {
                count++;
            }
        }
        if (count == 0) {
            return false;
        }
        int pick = urand.getAsInt();
        if (pick < 0) {
            pick = -pick;
        }
        pick %= count;
        int seen = 0;
        for (int i = 0; i < MAX_ACTIONS; i++) {
            Action a = h.script.action(i);
            if (a.type() == ACTION_NONE) {
                continue;
            }
            if (seen == pick) {
                return processAction(a, c, victim, cast, evadeAction);
            }
            seen++;
        }
        return false;
    }

    private boolean processAction(Action a, Creature c, Unit victim, SpellCast cast, Runnable evadeAction) {
        if (a == null || a.type() == ACTION_NONE) {
            return false;
        }
        if (a.type() == ACTION_CAST) {
            if (a.param1() == 0 || cast == null) {
                return false;
            }
            Unit t = a.param2() == TARGET_HOSTILE ? victim : c;
            if (t == null) {
                return false;
            }
            cast.cast(c, t, a.param1());
            return true;
        }
        if (a.type() == ACTION_SET_PHASE) {
            phase = clampPhase(a.param1());
            return true;
        }
        if (a.type() == ACTION_INC_PHASE) {
            phase = clampPhase(phase + a.param1());
            return true;
        }
        if (a.type() == ACTION_EVADE) {
            inCombat = false;
            if (evadeAction != null) {
                evadeAction.run();
            }
            return true;
        }
        return false;
    }

    private void resetEvent(Holder h) {
        if (isTimerBased(h.script.eventType())) {
            h.timer = h.script.param3();
        }
        if (isRepeatableType(h.script.eventType()) && (h.script.flags() & EFLAG_REPEATABLE) == 0) {
            h.enabled = false;
        }
        if (!isRepeatableType(h.script.eventType())) {
            h.enabled = false;
        }
    }

    private static int initialTimer(Script s) {
        if (s.eventType() == EVENT_TIMER_IN_COMBAT || s.eventType() == EVENT_TIMER_OOC) {
            return s.param1();
        }
        return 0;
    }

    private static boolean isTimerExecuted(int type) {
        return type == EVENT_TIMER_IN_COMBAT || type == EVENT_TIMER_OOC || type == EVENT_HP;
    }

    private static boolean isTimerBased(int type) {
        return isTimerExecuted(type);
    }

    private static boolean isRepeatableType(int type) {
        return type != EVENT_SPAWNED && type != EVENT_DEATH && type != EVENT_AGGRO
                && type != EVENT_EVADE && type != EVENT_REACHED_HOME;
    }

    private static int clampPhase(int p) {
        if (p < 0) {
            return 0;
        }
        if (p >= MAX_PHASE) {
            return MAX_PHASE - 1;
        }
        return p;
    }

    @FunctionalInterface
    public interface SpellCast {
        void cast(Creature c, Unit target, int spellId);
    }
}
