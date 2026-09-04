package org.tbc.world.ai;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
