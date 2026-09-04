package org.tbc.world.ai;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DbScriptEngineTest {
    @Test
    void startWhenDelayZeroCastShouldInvokeCast7164() {
        DbScriptStore store = new DbScriptStore();
        store.add(DbScriptStore.castSpell(DbScriptStore.CREATURE_DEATH, 6, 0, 7164));
        DbScriptEngine engine = new DbScriptEngine();
        List<Integer> casts = new ArrayList<>();
        engine.start(store, DbScriptStore.CREATURE_DEATH, 6, creature(), player(),
                (src, tgt, spell) -> casts.add(spell));
        assertEquals(List.of(7164), casts);
    }

    @Test
    void processWhenDelay500ShouldCastAfterDue() {
        DbScriptStore store = new DbScriptStore();
        store.add(DbScriptStore.castSpell(DbScriptStore.CREATURE_DEATH, 6, 500, 7164));
        DbScriptEngine engine = new DbScriptEngine();
        List<Integer> casts = new ArrayList<>();
        DbScriptEngine.CastSink sink = (src, tgt, spell) -> casts.add(spell);
        engine.start(store, DbScriptStore.CREATURE_DEATH, 6, creature(), player(), sink);
        assertTrue(casts.isEmpty());
        engine.process(499, sink);
        assertTrue(casts.isEmpty());
        engine.process(1, sink);
        assertEquals(List.of(7164), casts);
    }

    @Test
    void startWhenTerminateShouldDropLaterCast() {
        DbScriptStore store = new DbScriptStore();
        store.add(DbScriptStore.terminate(DbScriptStore.CREATURE_DEATH, 6, 0));
        store.add(DbScriptStore.castSpell(DbScriptStore.CREATURE_DEATH, 6, 0, 7164));
        store.add(DbScriptStore.castSpell(DbScriptStore.CREATURE_DEATH, 6, 500, 7164));
        DbScriptEngine engine = new DbScriptEngine();
        List<Integer> casts = new ArrayList<>();
        DbScriptEngine.CastSink sink = (src, tgt, spell) -> casts.add(spell);
        engine.start(store, DbScriptStore.CREATURE_DEATH, 6, creature(), player(), sink);
        engine.process(500, sink);
        assertTrue(casts.isEmpty());
    }

    @Test
    void startWhenQueuedShouldSkipSecondStart() {
        DbScriptStore store = new DbScriptStore();
        store.add(DbScriptStore.castSpell(DbScriptStore.CREATURE_DEATH, 6, 500, 7164));
        DbScriptEngine engine = new DbScriptEngine();
        List<Integer> casts = new ArrayList<>();
        DbScriptEngine.CastSink sink = (src, tgt, spell) -> casts.add(spell);
        Creature src = creature();
        Player tgt = player();
        engine.start(store, DbScriptStore.CREATURE_DEATH, 6, src, tgt, sink);
        engine.start(store, DbScriptStore.CREATURE_DEATH, 6, src, tgt, sink);
        engine.process(500, sink);
        assertEquals(List.of(7164), casts);
    }

    @Test
    void startWhenUnknownCommandShouldNotThrow() {
        DbScriptStore store = new DbScriptStore();
        store.add(new DbScriptStore.Step(DbScriptStore.CREATURE_DEATH, 6, 0, 99, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0f, 0, 0, 0, 0, 0f, 0));
        assertDoesNotThrow(() -> new DbScriptEngine().start(store, DbScriptStore.CREATURE_DEATH, 6,
                creature(), player(), (src, tgt, spell) -> {
                }));
    }

    private static Creature creature() {
        Creature c = new Creature();
        c.guid = 2;
        c.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        return c;
    }

    private static Player player() {
        Player p = new Player();
        p.guid = 1;
        return p;
    }
}
