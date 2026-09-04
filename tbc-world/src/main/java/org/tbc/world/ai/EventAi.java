package org.tbc.world.ai;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.map.LineOfSight;
import org.tbc.world.net.wow8606.UpdateFields;

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
    public static final int EVENT_MANA = 3;
    public static final int EVENT_AGGRO = 4;
    public static final int EVENT_KILL = 5;
    public static final int EVENT_DEATH = 6;
    public static final int EVENT_EVADE = 7;
    public static final int EVENT_SPELLHIT = 8;
    public static final int EVENT_RANGE = 9;
    public static final int EVENT_OOC_LOS = 10;
    public static final int EVENT_SPAWNED = 11;
    public static final int EVENT_TARGET_HP = 12;
    public static final int EVENT_TARGET_MANA = 18;
    public static final int EVENT_REACHED_HOME = 21;
    public static final int EVENT_AURA = 23;
    public static final int EVENT_TARGET_AURA = 24;
    public static final int EVENT_MISSING_AURA = 27;
    public static final int EVENT_TARGET_MISSING_AURA = 28;
    public static final int EVENT_TIMER_GENERIC = 29;

    public static final int ACTION_NONE = 0;
    public static final int ACTION_SET_FACTION = 2;
    public static final int ACTION_CAST = 11;
    public static final int ACTION_THREAT_SINGLE = 13;
    public static final int ACTION_THREAT_ALL_PCT = 14;
    public static final int ACTION_SET_UNIT_FLAG = 18;
    public static final int ACTION_REMOVE_UNIT_FLAG = 19;
    public static final int ACTION_COMBAT_MOVEMENT = 21;
    public static final int ACTION_SET_PHASE = 22;
    public static final int ACTION_INC_PHASE = 23;
    public static final int ACTION_EVADE = 24;
    public static final int ACTION_RANDOM_PHASE = 30;
    public static final int ACTION_RANDOM_PHASE_RANGE = 31;
    public static final int ACTION_DIE = 37;

    public static final int TARGET_SELF = 0;
    public static final int TARGET_HOSTILE = 1;
    public static final int TARGET_INVOKER = 6;
    public static final int TARGET_NONE = 15;

    public static final int CAST_FORCE_TARGET_SELF = 16;
    public static final int CAST_AURA_NOT_PRESENT = 32;

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

        public static Action setFaction(int factionId) {
            return new Action(ACTION_SET_FACTION, factionId, 0, 0);
        }

        public static Action threatSingle(int threat, int target, boolean direct) {
            return new Action(ACTION_THREAT_SINGLE, threat, target, direct ? 1 : 0);
        }

        public static Action threatAllPct(int percent) {
            return new Action(ACTION_THREAT_ALL_PCT, percent, 0, 0);
        }

        public static Action setUnitFlag(int flags) {
            return new Action(ACTION_SET_UNIT_FLAG, flags, TARGET_SELF, 0);
        }

        public static Action removeUnitFlag(int flags) {
            return new Action(ACTION_REMOVE_UNIT_FLAG, flags, TARGET_SELF, 0);
        }

        public static Action randomPhase(int a, int b, int c) {
            return new Action(ACTION_RANDOM_PHASE, a, b, c);
        }

        public static Action randomPhaseRange(int min, int max) {
            return new Action(ACTION_RANDOM_PHASE_RANGE, min, max, 0);
        }

        public static Action die() {
            return new Action(ACTION_DIE, 0, 0, 0);
        }

        public static Action cast(int spellId, int target, int castFlags) {
            return new Action(ACTION_CAST, spellId, target, castFlags);
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
            int param5,
            int param6,
            Action a1,
            Action a2,
            Action a3) {
        public Script(int eventType, int inversePhaseMask, int chance, int flags,
                int param1, int param2, int param3, int param4,
                Action a1, Action a2, Action a3) {
            this(eventType, inversePhaseMask, chance, flags, param1, param2, param3, param4, 0, 0, a1, a2, a3);
        }

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

        public static Script timerGeneric(int initMin, int repeatMin, int spellId, int target) {
            return new Script(EVENT_TIMER_GENERIC, 0, 100, EFLAG_REPEATABLE,
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
    private int spawnFaction = -1;
    private int eventUpdateTime = EVENT_UPDATE_TIME;
    private int eventDiff;
    private boolean hasOocLos;

    public EventAi() {
        this(() -> ThreadLocalRandom.current().nextInt());
    }

    public EventAi(IntSupplier urand) {
        this.urand = urand == null ? () -> ThreadLocalRandom.current().nextInt() : urand;
    }

    public void load(List<Script> scripts) {
        holders.clear();
        hasOocLos = false;
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
            if (s.eventType() == EVENT_OOC_LOS) {
                hasOocLos = true;
            }
        }
    }

    public boolean hasOocLos() {
        return hasOocLos;
    }

    public void onSpawned(Creature c, SpellCast cast) {
        rememberFaction(c);
        processImmediate(EVENT_SPAWNED, c, null, null, cast, null);
    }

    public void onAggro(Creature c, Unit victim, SpellCast cast) {
        rememberFaction(c);
        inCombat = true;
        for (Holder h : holders) {
            if (h.script.eventType() == EVENT_TIMER_IN_COMBAT) {
                h.timer = h.script.param1();
            }
        }
        processImmediate(EVENT_AGGRO, c, victim, victim, cast, null);
    }

    public void onDeath(Creature c, Unit killer, SpellCast cast) {
        processImmediate(EVENT_DEATH, c, killer, killer, cast, null);
        inCombat = false;
    }

    public void onKill(Creature c, Unit victim, SpellCast cast) {
        rememberFaction(c);
        for (Holder h : holders) {
            if (!h.enabled || h.script.eventType() != EVENT_KILL || h.timer > 0) {
                continue;
            }
            if (h.script.param3() == 1 && !(victim instanceof Player)) {
                continue;
            }
            processEvent(h, c, victim, victim, cast, null);
        }
    }

    public void onSpellHit(Creature c, Unit caster, int spellId, int school, SpellCast cast) {
        rememberFaction(c);
        for (Holder h : holders) {
            if (!h.enabled || h.script.eventType() != EVENT_SPELLHIT || h.timer > 0) {
                continue;
            }
            if (h.script.param1() != 0 && h.script.param1() != spellId) {
                continue;
            }
            if (h.script.param2() != 0 && (school & h.script.param2()) == 0) {
                continue;
            }
            processEvent(h, c, caster, caster, cast, null);
        }
    }

    public void onOocLos(Creature c, Unit who, SpellCast cast) {
        if (inCombat || who == null || c == null) {
            return;
        }
        rememberFaction(c);
        for (Holder h : holders) {
            if (!h.enabled || h.script.eventType() != EVENT_OOC_LOS || h.timer > 0) {
                continue;
            }
            if (h.script.param6() != 0) {
                continue;
            }
            if (h.script.param5() != 0 && !(who instanceof Player)) {
                continue;
            }
            boolean hostile = c.faction != who.faction;
            if (h.script.param1() == 0) {
                if (!hostile) {
                    continue;
                }
            } else if (hostile) {
                continue;
            }
            if (c.distance2d(who) > h.script.param2()) {
                continue;
            }
            if (!LineOfSight.clear(c, who)) {
                continue;
            }
            processEvent(h, c, who, who, cast, null);
        }
    }

    public void onEvade(Creature c, SpellCast cast) {
        processImmediate(EVENT_EVADE, c, null, null, cast, null);
        inCombat = false;
        for (Holder h : holders) {
            if (h.script.eventType() == EVENT_TIMER_OOC) {
                h.timer = h.script.param1();
                h.enabled = true;
            }
        }
    }

    public void onReachedHome(Creature c, SpellCast cast) {
        processImmediate(EVENT_REACHED_HOME, c, null, null, cast, null);
    }

    public void update(Creature c, Unit victim, int diffMs, SpellCast cast) {
        update(c, victim, diffMs, cast, null);
    }

    public void update(Creature c, Unit victim, int diffMs, SpellCast cast, Runnable evadeAction) {
        rememberFaction(c);
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
                    processEvent(h, c, victim, victim, cast, evadeAction);
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
        spawnFaction = -1;
        eventUpdateTime = EVENT_UPDATE_TIME;
        eventDiff = 0;
        for (Holder h : holders) {
            h.enabled = true;
            h.timer = initialTimer(h.script);
        }
    }

    private void processImmediate(int eventType, Creature c, Unit victim, Unit invoker, SpellCast cast, Runnable evadeAction) {
        rememberFaction(c);
        for (Holder h : holders) {
            if (h.enabled && h.script.eventType() == eventType) {
                processEvent(h, c, victim, invoker, cast, evadeAction);
            }
        }
    }

    private void processEvent(Holder h, Creature c, Unit victim, Unit invoker, SpellCast cast, Runnable evadeAction) {
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
            success = processRandomAction(h, c, victim, invoker, cast, evadeAction);
        } else {
            success = processAction(h.script.a1(), c, victim, invoker, cast, evadeAction);
            if ((h.script.flags() & EFLAG_COMBAT_ACTION) == 0 || success) {
                processAction(h.script.a2(), c, victim, invoker, cast, evadeAction);
                processAction(h.script.a3(), c, victim, invoker, cast, evadeAction);
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
        if (type == EVENT_TIMER_GENERIC) {
            return true;
        }
        if (type == EVENT_HP) {
            return inCombat && inPercentBand(c, h.script.param1(), h.script.param2());
        }
        if (type == EVENT_MANA) {
            return inCombat && inPowerBand(c, h.script.param1(), h.script.param2());
        }
        if (type == EVENT_TARGET_HP) {
            return inCombat && inPercentBand(victim, h.script.param1(), h.script.param2());
        }
        if (type == EVENT_TARGET_MANA) {
            return inCombat && inPowerBand(victim, h.script.param1(), h.script.param2());
        }
        if (type == EVENT_AURA) {
            return auraStacks(c, h.script.param1()) >= Math.max(1, h.script.param2());
        }
        if (type == EVENT_MISSING_AURA) {
            return auraStacks(c, h.script.param1()) < Math.max(1, h.script.param2());
        }
        if (type == EVENT_TARGET_AURA) {
            return inCombat && auraStacks(victim, h.script.param1()) >= Math.max(1, h.script.param2());
        }
        if (type == EVENT_TARGET_MISSING_AURA) {
            return inCombat && auraStacks(victim, h.script.param1()) < Math.max(1, h.script.param2());
        }
        if (type == EVENT_RANGE) {
            if (!inCombat || c == null || victim == null) {
                return false;
            }
            double d = c.distance2d(victim);
            return d >= h.script.param1() && d <= h.script.param2();
        }
        return true;
    }

    private boolean processRandomAction(Holder h, Creature c, Unit victim, Unit invoker, SpellCast cast, Runnable evadeAction) {
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
                return processAction(a, c, victim, invoker, cast, evadeAction);
            }
            seen++;
        }
        return false;
    }

    private boolean processAction(Action a, Creature c, Unit victim, Unit invoker, SpellCast cast, Runnable evadeAction) {
        if (a == null || a.type() == ACTION_NONE) {
            return false;
        }
        if (a.type() == ACTION_CAST) {
            return processCast(a, c, victim, invoker, cast);
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
        if (a.type() == ACTION_SET_FACTION) {
            return processSetFaction(c, a.param1());
        }
        if (a.type() == ACTION_THREAT_SINGLE) {
            if (c == null) {
                return false;
            }
            if (a.param3() != 0) {
                c.threat += a.param1();
            } else {
                c.threat = c.threat * (100 + a.param1()) / 100;
            }
            return true;
        }
        if (a.type() == ACTION_THREAT_ALL_PCT) {
            if (c == null) {
                return false;
            }
            c.threat = c.threat * (100 + a.param1()) / 100;
            return true;
        }
        if (a.type() == ACTION_SET_UNIT_FLAG) {
            Unit t = a.param2() == TARGET_HOSTILE ? victim : c;
            if (t == null) {
                return false;
            }
            t.setInt(UpdateFields.UNIT_FIELD_FLAGS, t.getInt(UpdateFields.UNIT_FIELD_FLAGS) | a.param1());
            return true;
        }
        if (a.type() == ACTION_REMOVE_UNIT_FLAG) {
            Unit t = a.param2() == TARGET_HOSTILE ? victim : c;
            if (t == null) {
                return false;
            }
            t.setInt(UpdateFields.UNIT_FIELD_FLAGS, t.getInt(UpdateFields.UNIT_FIELD_FLAGS) & ~a.param1());
            return true;
        }
        if (a.type() == ACTION_RANDOM_PHASE) {
            int rnd = urand.getAsInt();
            if (rnd < 0) {
                rnd = -rnd;
            }
            int which = rnd % 3;
            int next = which == 0 ? a.param1() : which == 1 ? a.param2() : a.param3();
            phase = clampPhase(next);
            return true;
        }
        if (a.type() == ACTION_RANDOM_PHASE_RANGE) {
            if (a.param2() <= a.param1()) {
                return false;
            }
            int rnd = urand.getAsInt();
            if (rnd < 0) {
                rnd = -rnd;
            }
            int span = a.param2() - a.param1() + 1;
            phase = clampPhase(a.param1() + rnd % span);
            return true;
        }
        if (a.type() == ACTION_DIE) {
            if (c == null || !c.alive()) {
                return false;
            }
            c.setHealth(0);
            return true;
        }
        if (a.type() == ACTION_COMBAT_MOVEMENT) {
            if (c == null) {
                return false;
            }
            c.combatMovement = a.param1() != 0;
            if (!c.combatMovement) {
                c.motion.moveIdle();
            }
            return true;
        }
        return false;
    }

    private boolean processCast(Action a, Creature c, Unit victim, Unit invoker, SpellCast cast) {
        if (a.param1() == 0 || cast == null) {
            return false;
        }
        Unit t = resolveCastTarget(a.param2(), a.param3(), c, victim, invoker);
        if (t == null) {
            return false;
        }
        if ((a.param3() & CAST_AURA_NOT_PRESENT) != 0 && auraStacks(t, a.param1()) > 0) {
            return false;
        }
        cast.cast(c, t, a.param1());
        return true;
    }

    private static Unit resolveCastTarget(int targetType, int castFlags, Creature c, Unit victim, Unit invoker) {
        if ((castFlags & CAST_FORCE_TARGET_SELF) != 0 || targetType == TARGET_SELF) {
            return c;
        }
        if (targetType == TARGET_NONE) {
            return null;
        }
        if (targetType == TARGET_HOSTILE) {
            return victim;
        }
        if (targetType == TARGET_INVOKER) {
            return invoker != null ? invoker : victim;
        }
        return c;
    }

    private boolean processSetFaction(Creature c, int factionId) {
        if (c == null) {
            return false;
        }
        rememberFaction(c);
        int next = factionId == 0 ? spawnFaction : factionId;
        c.faction = next;
        c.setInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE, next);
        return true;
    }

    private void rememberFaction(Creature c) {
        if (c != null && spawnFaction < 0) {
            spawnFaction = c.faction;
        }
    }

    private static boolean inPercentBand(Unit u, int maxPct, int minPct) {
        if (u == null || u.maxHealth() <= 0) {
            return false;
        }
        int pct = (u.health() * 100) / u.maxHealth();
        return pct <= maxPct && pct >= minPct;
    }

    private static boolean inPowerBand(Unit u, int maxPct, int minPct) {
        if (u == null || u.maxPower() <= 0) {
            return false;
        }
        int pct = (u.power() * 100) / u.maxPower();
        return pct <= maxPct && pct >= minPct;
    }

    private static int auraStacks(Unit u, int spellId) {
        if (u == null) {
            return 0;
        }
        int n = 0;
        for (Unit.Aura a : u.auras) {
            if (a.spellId() == spellId) {
                n += Math.max(1, a.stacks());
            }
        }
        return n;
    }

    private void resetEvent(Holder h) {
        if (isTimerBased(h.script.eventType())) {
            h.timer = h.script.eventType() == EVENT_KILL ? h.script.param1() : h.script.param3();
        }
        if (isRepeatableType(h.script.eventType()) && (h.script.flags() & EFLAG_REPEATABLE) == 0) {
            h.enabled = false;
        }
        if (!isRepeatableType(h.script.eventType())) {
            h.enabled = false;
        }
    }

    private static int initialTimer(Script s) {
        if (s.eventType() == EVENT_TIMER_IN_COMBAT || s.eventType() == EVENT_TIMER_OOC
                || s.eventType() == EVENT_TIMER_GENERIC) {
            return s.param1();
        }
        return 0;
    }

    private static boolean isTimerExecuted(int type) {
        return type == EVENT_TIMER_IN_COMBAT || type == EVENT_TIMER_OOC || type == EVENT_TIMER_GENERIC
                || type == EVENT_HP || type == EVENT_MANA || type == EVENT_TARGET_HP || type == EVENT_TARGET_MANA
                || type == EVENT_AURA || type == EVENT_TARGET_AURA || type == EVENT_MISSING_AURA
                || type == EVENT_TARGET_MISSING_AURA || type == EVENT_RANGE;
    }

    private static boolean isTimerBased(int type) {
        return isTimerExecuted(type) || type == EVENT_SPELLHIT || type == EVENT_KILL || type == EVENT_OOC_LOS;
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
