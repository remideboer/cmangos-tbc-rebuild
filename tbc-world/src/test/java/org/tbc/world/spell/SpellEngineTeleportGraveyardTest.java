package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GraveyardManager;
import org.tbc.world.session.DeathHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-076 — SPELL_EFFECT_TELEPORT_GRAVEYARD (120). Graveyard Teleport Test 24253.
 * CMaNGOS player + IsBattleGround → RepopAtGraveyard closest WorldSafeLocs.
 */
class SpellEngineTeleportGraveyardTest {
    @Test
    void applyTeleportGraveyardWhenBattlegroundShouldNearTeleportToClosestGrave() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_TELEPORT_GRAVEYARD));
        Player p = new Player();
        p.team = GraveyardManager.ALLIANCE;
        p.mapId = 489;
        p.relocate(1000f, 1000f, 10f, 1.2f);
        SpellEngine.SpellInfo gy = new SpellEngine.SpellInfo(
                24253, SpellEngine.EFFECT_TELEPORT_GRAVEYARD, 0, 0, 0, 0, 0, 0f);
        eng.apply(p, p, gy);
        assertEquals(DeathHandler.GY_ELWYNN_MAP, p.mapId);
        assertEquals(DeathHandler.GY_ELWYNN_X, p.x, 0.01f);
        assertEquals(DeathHandler.GY_ELWYNN_Y, p.y, 0.01f);
        assertEquals(DeathHandler.GY_ELWYNN_Z, p.z, 0.01f);
    }

    @Test
    void applyTeleportGraveyardWhenNotBattlegroundOrMissingYardShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player continent = new Player();
        continent.mapId = 0;
        continent.relocate(5f, 6f, 7f, 0f);
        SpellEngine.SpellInfo gy = new SpellEngine.SpellInfo(
                24253, SpellEngine.EFFECT_TELEPORT_GRAVEYARD, 0, 0, 0, 0, 0, 0f);
        eng.apply(continent, continent, gy);
        assertEquals(5f, continent.x, 0.01f);
        Player arena = new Player();
        arena.mapId = 559;
        arena.relocate(8f, 9f, 10f, 0f);
        eng.apply(arena, arena, gy);
        assertEquals(8f, arena.x, 0.01f);
        Creature npc = new Creature();
        npc.mapId = 489;
        npc.relocate(1f, 2f, 3f, 0f);
        eng.apply(npc, npc, gy);
        assertEquals(1f, npc.x, 0.01f);
        Player bg = new Player();
        bg.mapId = 489;
        bg.relocate(11f, 12f, 13f, 0f);
        eng.teleportGraveyard(null, GraveyardManager.seeded());
        eng.teleportGraveyard(bg, null);
        eng.teleportGraveyard(bg, new GraveyardManager());
        assertEquals(11f, bg.x, 0.01f);
    }
}
