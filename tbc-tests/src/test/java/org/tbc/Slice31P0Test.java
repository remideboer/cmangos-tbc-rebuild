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

    @Test
    void tpSl31KarathressCataclysmicBolt() {
        World w = World.inMemory();
        assertTrue(w.scripts.knows("boss_fathomlord_karathress"));
        BossScript boss = w.scripts.create("boss_fathomlord_karathress");
        assertEquals(548, boss.mapId);
        assertTrue(boss.actions.stream().anyMatch(a -> a.spellId() == 38441),
                "SPELL_CATACLYSMIC_BOLT 38441");
    }

    @Test
    void tpSl31AlarFlameBuffet() {
        World w = World.inMemory();
        assertTrue(w.scripts.knows("boss_alar"));
        BossScript boss = w.scripts.create("boss_alar");
        assertEquals(550, boss.mapId);
        assertTrue(boss.actions.stream().anyMatch(a -> a.spellId() == 34121),
                "SPELL_FLAME_BUFFET 34121");
    }

    @Test
    void tpSl31AnetheronCarrionSwarm() {
        World w = World.inMemory();
        assertTrue(w.scripts.knows("boss_anetheron"));
        BossScript boss = w.scripts.create("boss_anetheron");
        assertEquals(534, boss.mapId);
        boss.aggro();
        List<Integer> casts = new ArrayList<>();
        boss.update(new Creature(), new Player(), 20_000, (c, t, id) -> casts.add(id));
        assertTrue(casts.contains(31306), "SPELL_CARRION_SWARM 31306 at 20000 ms");
    }

    @Test
    void tpSl31NajentusImpalingSpine() {
        World w = World.inMemory();
        assertTrue(w.scripts.knows("boss_najentus"));
        BossScript boss = w.scripts.create("boss_najentus");
        assertEquals(564, boss.mapId);
        assertTrue(boss.actions.stream().anyMatch(a -> a.spellId() == 39837),
                "SPELL_IMPALING_SPINE 39837 from black_temple.md");
    }
}
