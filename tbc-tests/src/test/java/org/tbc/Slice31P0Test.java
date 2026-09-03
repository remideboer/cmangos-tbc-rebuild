package org.tbc;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.script.BossScript;
import org.tbc.world.script.ScriptRegistry;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TP-SL31-002 P1 — boss_curator Hateful Bolt after Gruul template. */
class Slice31P0Test {
    @Test
    void tpSl31CuratorHatefulBolt() {
        World w = World.inMemory();
        ScriptRegistry reg = w.scripts;
        assertTrue(reg.knows("boss_curator"));
        BossScript curator = reg.create("boss_curator");
        assertEquals(532, curator.mapId);
        curator.aggro();
        List<Integer> casts = new ArrayList<>();
        curator.update(new Creature(), new Player(), 15_000, (c, t, id) -> casts.add(id));
        assertTrue(casts.contains(30383), "SPELL_HATEFUL_BOLT 30383 every 15s");
    }

    @Test
    void tpSl31MagtheridonBlastNova() {
        World w = World.inMemory();
        assertTrue(w.scripts.knows("boss_magtheridon"));
        BossScript mag = w.scripts.create("boss_magtheridon");
        assertEquals(544, mag.mapId);
        assertTrue(mag.actions.stream().anyMatch(a -> a.spellId() == 30616),
                "SPELL_BLASTNOVA 30616 from magtheridons_lair.md");
    }
}
