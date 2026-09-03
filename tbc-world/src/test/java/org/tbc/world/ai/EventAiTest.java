package org.tbc.world.ai;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventAiTest {
    @Test
    void updateTimerInCombatWhenInitMinZeroShouldCastAfter500msWindow() {
        EventAi ai = new EventAi();
        ai.load(List.of(EventAi.Script.timerInCombat(0, 1000, 7164, EventAi.TARGET_SELF)));
        Creature c = creature();
        Player v = player();
        List<Integer> casts = new ArrayList<>();
        ai.onAggro(c, v, (cr, t, id) -> casts.add(id));
        assertTrue(casts.isEmpty());
        ai.update(c, v, 499, (cr, t, id) -> casts.add(id));
        assertTrue(casts.isEmpty(), "must wait EVENT_UPDATE_TIME 500ms");
        ai.update(c, v, 2, (cr, t, id) -> casts.add(id));
        assertTrue(casts.contains(7164));
    }

    @Test
    void updateTimerInCombatWhenInitMinRemainingShouldNotCastOnFirstWindow() {
        EventAi ai = new EventAi();
        ai.load(List.of(EventAi.Script.timerInCombat(10_000, 10_000, 7164, EventAi.TARGET_SELF)));
        Creature c = creature();
        Player v = player();
        List<Integer> casts = new ArrayList<>();
        ai.onAggro(c, v, EventAi.NOOP);
        ai.update(c, v, 501, (cr, t, id) -> casts.add(id));
        assertTrue(casts.isEmpty());
    }

    @Test
    void processEventWhenChanceZeroShouldNeverCast() {
        EventAi ai = new EventAi(() -> 0);
        ai.load(List.of(new EventAi.Script(EventAi.EVENT_AGGRO, 0, 0, 0, 0, 0, 0, 0,
                EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        List<Integer> casts = new ArrayList<>();
        ai.onAggro(creature(), player(), (cr, t, id) -> casts.add(id));
        assertTrue(casts.isEmpty());
    }

    @Test
    void processEventWhenChance100ShouldAlwaysCast() {
        EventAi ai = new EventAi(() -> 99);
        ai.load(List.of(EventAi.Script.aggroCast(7164)));
        List<Integer> casts = new ArrayList<>();
        ai.onAggro(creature(), player(), (cr, t, id) -> casts.add(id));
        assertEquals(List.of(7164), casts);
    }

    @Test
    void processEventWhenInversePhaseMaskShouldSuppressThenFireAfterSetPhase() {
        EventAi skip = new EventAi();
        skip.load(List.of(new EventAi.Script(EventAi.EVENT_AGGRO, 1, 100, 0, 0, 0, 0, 0,
                EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        List<Integer> casts = new ArrayList<>();
        skip.onAggro(creature(), player(), (cr, t, id) -> casts.add(id));
        assertTrue(casts.isEmpty());

        EventAi phased = new EventAi();
        phased.load(List.of(
                new EventAi.Script(EventAi.EVENT_AGGRO, 0, 100, 0, 0, 0, 0, 0,
                        EventAi.Action.setPhase(1), EventAi.Action.none(), EventAi.Action.none()),
                new EventAi.Script(EventAi.EVENT_AGGRO, 1, 100, 0, 0, 0, 0, 0,
                        EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        phased.onAggro(creature(), player(), (cr, t, id) -> casts.add(id));
        assertTrue(casts.contains(7164));
    }

    @Test
    void processEventWhenRandomActionShouldPickOneCast() {
        AtomicInteger n = new AtomicInteger();
        int[] rolls = {0, 1};
        EventAi ai = new EventAi(() -> rolls[Math.min(n.getAndIncrement(), rolls.length - 1)]);
        ai.load(List.of(new EventAi.Script(EventAi.EVENT_AGGRO, 0, 100, EventAi.EFLAG_RANDOM_ACTION, 0, 0, 0, 0,
                EventAi.Action.cast(7164, EventAi.TARGET_SELF),
                EventAi.Action.cast(133, EventAi.TARGET_SELF),
                EventAi.Action.none())));
        List<Integer> casts = new ArrayList<>();
        ai.onAggro(creature(), player(), (cr, t, id) -> casts.add(id));
        assertEquals(List.of(133), casts);
    }

    @Test
    void processEventWhenThreeActionsShouldRunAll() {
        EventAi ai = new EventAi();
        ai.load(List.of(new EventAi.Script(EventAi.EVENT_AGGRO, 0, 100, 0, 0, 0, 0, 0,
                EventAi.Action.cast(7164, EventAi.TARGET_SELF),
                EventAi.Action.cast(133, EventAi.TARGET_SELF),
                EventAi.Action.setPhase(2))));
        List<Integer> casts = new ArrayList<>();
        ai.onAggro(creature(), player(), (cr, t, id) -> casts.add(id));
        assertEquals(List.of(7164, 133), casts);
    }

    @Test
    void updateHpWhenInsideBandShouldCast() {
        EventAi ai = new EventAi();
        ai.load(List.of(new EventAi.Script(EventAi.EVENT_HP, 0, 100, EventAi.EFLAG_REPEATABLE, 60, 40, 0, 0,
                EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        Creature c = creature();
        c.setHealth(50);
        Player v = player();
        List<Integer> casts = new ArrayList<>();
        ai.onAggro(c, v, EventAi.NOOP);
        ai.update(c, v, 501, (cr, t, id) -> casts.add(id));
        assertTrue(casts.contains(7164));
        casts.clear();
        c.setHealth(c.maxHealth());
        ai.update(c, v, 501, (cr, t, id) -> casts.add(id));
        assertTrue(casts.isEmpty());
    }

    @Test
    void onDeathAndOnEvadeShouldCastOnce() {
        EventAi ai = new EventAi();
        ai.load(List.of(
                new EventAi.Script(EventAi.EVENT_DEATH, 0, 100, 0, 0, 0, 0, 0,
                        EventAi.Action.cast(7164, EventAi.TARGET_HOSTILE), EventAi.Action.none(), EventAi.Action.none()),
                new EventAi.Script(EventAi.EVENT_EVADE, 0, 100, 0, 0, 0, 0, 0,
                        EventAi.Action.cast(133, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        Creature c = creature();
        Player v = player();
        List<Integer> casts = new ArrayList<>();
        List<Long> targets = new ArrayList<>();
        EventAi.SpellCast sink = (cr, t, id) -> {
            casts.add(id);
            targets.add(t.guid);
        };
        ai.onDeath(c, v, sink);
        assertEquals(List.of(7164), casts);
        assertEquals(v.guid, targets.get(0));
        ai.onDeath(c, v, sink);
        assertEquals(1, casts.size(), "DEATH is not repeatable");
        casts.clear();
        ai.onEvade(c, sink);
        assertEquals(List.of(133), casts);
        ai.onEvade(c, sink);
        assertEquals(1, casts.size(), "EVADE is not repeatable");
    }

    @Test
    void loadWhenDebugOnlyShouldSkipRow() {
        EventAi ai = new EventAi();
        ai.load(List.of(new EventAi.Script(EventAi.EVENT_AGGRO, 0, 100, EventAi.EFLAG_DEBUG_ONLY, 0, 0, 0, 0,
                EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        List<Integer> casts = new ArrayList<>();
        ai.onAggro(creature(), player(), (cr, t, id) -> casts.add(id));
        assertTrue(casts.isEmpty());
    }

    @Test
    void onSpawnedAndReachedHomeShouldCast() {
        EventAi ai = new EventAi();
        ai.load(List.of(
                new EventAi.Script(EventAi.EVENT_SPAWNED, 0, 100, 0, 0, 0, 0, 0,
                        EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none()),
                new EventAi.Script(EventAi.EVENT_REACHED_HOME, 0, 100, 0, 0, 0, 0, 0,
                        EventAi.Action.cast(133, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        List<Integer> casts = new ArrayList<>();
        Creature c = creature();
        ai.onSpawned(c, (cr, t, id) -> casts.add(id));
        ai.onReachedHome(c, (cr, t, id) -> casts.add(id));
        assertEquals(List.of(7164, 133), casts);
    }

    @Test
    void updateTimerOocWhenOutOfCombatShouldCast() {
        EventAi ai = new EventAi();
        ai.load(List.of(EventAi.Script.timerOoc(0, 1000, 7164, EventAi.TARGET_SELF)));
        List<Integer> casts = new ArrayList<>();
        ai.update(creature(), null, 501, (cr, t, id) -> casts.add(id));
        assertTrue(casts.contains(7164));
    }

    @Test
    void updateTimerOocWhenInCombatShouldNotCast() {
        EventAi ai = new EventAi();
        ai.load(List.of(EventAi.Script.timerOoc(0, 1000, 7164, EventAi.TARGET_SELF)));
        Creature c = creature();
        Player v = player();
        List<Integer> casts = new ArrayList<>();
        ai.onAggro(c, v, EventAi.NOOP);
        ai.update(c, v, 501, (cr, t, id) -> casts.add(id));
        assertTrue(casts.isEmpty());
    }

    @Test
    void processActionEvadeShouldRunHook() {
        AtomicBoolean evaded = new AtomicBoolean();
        EventAi ai = new EventAi();
        ai.load(List.of(new EventAi.Script(EventAi.EVENT_TIMER_IN_COMBAT, 0, 100, EventAi.EFLAG_REPEATABLE,
                0, 0, 0, 0, EventAi.Action.evade(), EventAi.Action.none(), EventAi.Action.none())));
        Creature c = creature();
        Player v = player();
        ai.onAggro(c, v, EventAi.NOOP);
        ai.update(c, v, 501, EventAi.NOOP, () -> evaded.set(true));
        assertTrue(evaded.get());
    }

    @Test
    void processActionNoneShouldNoOp() {
        EventAi ai = new EventAi();
        ai.load(List.of(new EventAi.Script(EventAi.EVENT_AGGRO, 0, 100, 0, 0, 0, 0, 0,
                EventAi.Action.none(), EventAi.Action.none(), EventAi.Action.none())));
        List<Integer> casts = new ArrayList<>();
        ai.onAggro(creature(), player(), (cr, t, id) -> casts.add(id));
        assertTrue(casts.isEmpty());
    }

    @Test
    void incPhaseWhenOutOfRangeShouldClamp() {
        EventAi up = new EventAi();
        up.load(List.of(
                new EventAi.Script(EventAi.EVENT_AGGRO, 0, 100, 0, 0, 0, 0, 0,
                        EventAi.Action.incPhase(100), EventAi.Action.none(), EventAi.Action.none()),
                new EventAi.Script(EventAi.EVENT_TIMER_IN_COMBAT, 0x7FFFFFFF, 100, EventAi.EFLAG_REPEATABLE,
                        0, 0, 0, 0, EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        Creature c = creature();
        Player v = player();
        List<Integer> casts = new ArrayList<>();
        up.onAggro(c, v, EventAi.NOOP);
        up.update(c, v, 501, (cr, t, id) -> casts.add(id));
        assertEquals(List.of(7164), casts);

        EventAi down = new EventAi();
        down.load(List.of(
                new EventAi.Script(EventAi.EVENT_AGGRO, 0, 100, 0, 0, 0, 0, 0,
                        EventAi.Action.incPhase(-5), EventAi.Action.none(), EventAi.Action.none()),
                new EventAi.Script(EventAi.EVENT_TIMER_IN_COMBAT, 0xFFFFFFFE, 100, EventAi.EFLAG_REPEATABLE,
                        0, 0, 0, 0, EventAi.Action.cast(133, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        casts.clear();
        down.onAggro(c, v, EventAi.NOOP);
        down.update(c, v, 501, (cr, t, id) -> casts.add(id));
        assertEquals(List.of(133), casts);
    }

    @Test
    void processEventWhenCombatActionAndFirstCastFailsShouldSkipLaterActions() {
        EventAi ai = new EventAi();
        ai.load(List.of(new EventAi.Script(EventAi.EVENT_AGGRO, 0, 100, EventAi.EFLAG_COMBAT_ACTION, 0, 0, 0, 0,
                EventAi.Action.cast(0, EventAi.TARGET_SELF),
                EventAi.Action.cast(133, EventAi.TARGET_SELF),
                EventAi.Action.none())));
        List<Integer> casts = new ArrayList<>();
        ai.onAggro(creature(), player(), (cr, t, id) -> casts.add(id));
        assertTrue(casts.isEmpty());
    }

    @Test
    void processEventWhenCombatActionOutOfCombatShouldSkip() {
        EventAi ai = new EventAi();
        ai.load(List.of(new EventAi.Script(EventAi.EVENT_TIMER_OOC, 0, 100,
                EventAi.EFLAG_COMBAT_ACTION | EventAi.EFLAG_REPEATABLE, 0, 0, 0, 0,
                EventAi.Action.cast(7164, EventAi.TARGET_SELF), EventAi.Action.none(), EventAi.Action.none())));
        List<Integer> casts = new ArrayList<>();
        ai.update(creature(), player(), 501, (cr, t, id) -> casts.add(id));
        assertTrue(casts.isEmpty());
    }

    @Test
    void loadWhenNullOrNullRowShouldNoOp() {
        EventAi ai = new EventAi(null);
        ai.load(null);
        List<EventAi.Script> rows = new ArrayList<>();
        rows.add(null);
        ai.load(rows);
        ai.onAggro(creature(), player(), EventAi.NOOP);
        ai.update(creature(), player(), 0, EventAi.NOOP);
        ai.reset();
    }

    private static Creature creature() {
        Creature c = new Creature();
        c.guid = 2;
        c.applyTemplate(103, "Garrick Padfoot", 1, 7, 100, 1);
        return c;
    }

    private static Player player() {
        Player p = new Player();
        p.guid = 1;
        return p;
    }
}
