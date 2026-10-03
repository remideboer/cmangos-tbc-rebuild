package org.tbc.world.map;

import org.tbc.world.session.DeathHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraveyardManagerTest {
    @Test
    void closestWhenNorthshireShouldReturnElwynnAllianceLoc() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc loc = g.closest(0, -8949.95f, -132.493f, 83.5312f, GraveyardManager.ALLIANCE, 0);
        assertEquals(DeathHandler.GY_ELWYNN_X, loc.x(), 0.01f);
        assertEquals(DeathHandler.GY_ELWYNN_Y, loc.y(), 0.01f);
    }

    @Test
    void closestWhenColdridgeShouldNotReturnElwynn() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc loc = g.closest(0, -6240f, 331f, 383f, GraveyardManager.ALLIANCE, 0);
        assertEquals(-6220f, loc.x(), 0.01f);
        assertTrue(Math.abs(loc.x() - DeathHandler.GY_ELWYNN_X) > 100);
    }

    /**
     * CMaNGOS GetClosestGraveYard: area AREALINK, then zone AREALINK. Goldshire
     * (area 87) has no own row; Elwynn Forest zone 12 links world_safe_locs 106.
     */
    @Test
    void closestWhenGoldshireAreaHasNoLinkShouldUseElwynnZoneSpiritHealer() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc loc = g.closest(0, -9465f, 16f, 57f, GraveyardManager.ALLIANCE, 87, 12);
        assertEquals(106, loc.id());
        assertEquals(-9339.46f, loc.x(), 0.05f);
        assertEquals(171.408f, loc.y(), 0.05f);
    }

    /**
     * TP-SL17-019 — Sunstrider Isle (area 3431) has no AREALINK; Eversong zone 3430 links
     * world_safe_locs 912 (Sunstrider Isle GY), not Horde default / void.
     */
    @Test
    void closestWhenSunstriderAreaShouldUseEversongSunstriderIsleGy() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc loc = g.closest(530, 10349.6f, -6357.29f, 33.4f,
                GraveyardManager.HORDE, AreaTable.SUNSTRIDER_ISLE, AreaTable.EVERSONG_WOODS);
        assertEquals(912, loc.id());
        assertEquals(530, loc.map());
        assertEquals(10458.5f, loc.x(), 0.5f);
        assertEquals(-6364.61f, loc.y(), 0.5f);
    }

    /**
     * tbc-db has no game_graveyard_zone row for Sunstrider Isle 3431 (only parent 3430).
     * Area-only lookup must still hit loc 912, not Horde continent default / void.
     */
    @Test
    void closestWhenSunstriderIsleAreaOnlyShouldNotFallThroughToHordeDefault() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc loc = g.closest(530, 10381.6f, -6399.23f, 38.53f,
                GraveyardManager.HORDE, AreaTable.SUNSTRIDER_ISLE, 0);
        assertEquals(912, loc.id());
        assertEquals(530, loc.map());
        assertEquals(10458.5f, loc.x(), 0.5f);
        assertEquals(39.7907f, loc.z(), 0.5f);
    }

    /**
     * TP-SL17-020 — Shadowglen (area 188) / Teldrassil zone 141 → world_safe_locs 93 Aldrassil,
     * not Alliance continent default (Elwynn) / mid-air void.
     */
    @Test
    void closestWhenShadowglenShouldUseAldrassilSpiritHealer() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc loc = g.closest(1, 10311.3f, 831.463f, 1326.41f,
                GraveyardManager.ALLIANCE, AreaTable.SHADOWGLEN, AreaTable.TELDRASSIL);
        assertEquals(93, loc.id());
        assertEquals(1, loc.map());
        assertEquals(10384.8f, loc.x(), 0.5f);
        assertEquals(811.531f, loc.y(), 0.5f);
        assertEquals(1317.54f, loc.z(), 0.5f);
    }

    /**
     * TP-SL17-022 — Ammen Vale (area 3526) has no tbc-db AREALINK; seed must hit loc 918,
     * not Alliance continent default / void.
     */
    @Test
    void closestWhenAmmenValeAreaOnlyShouldUseAzuremystAmmenValeGy() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc loc = g.closest(530, -3961.64f, -13931.2f, 100.615f,
                GraveyardManager.ALLIANCE, AreaTable.AMMEN_VALE, 0);
        assertEquals(918, loc.id());
        assertEquals(530, loc.map());
        assertEquals(-4123.14f, loc.x(), 0.5f);
        assertEquals(74.6f, loc.z(), 0.5f);
    }

    /** Map 530 with no area/zone must stay on Outland via MAPLINK, not EK/Kalimdor defaults. */
    @Test
    void closestWhenMap530UnknownAreaShouldUseContinentMapLink() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc horde = g.closest(530, 0f, 0f, 0f, GraveyardManager.HORDE, 0, 0);
        assertEquals(912, horde.id());
        assertEquals(530, horde.map());
        GraveyardManager.Loc alliance = g.closest(530, 0f, 0f, 0f, GraveyardManager.ALLIANCE, 0, 0);
        assertEquals(918, alliance.id());
        assertEquals(530, alliance.map());
    }

    /** TP-SL17-023 — Tirisfal / Durotar / Mulgore starters hit nearest GY, not cross-continent default. */
    @Test
    void closestWhenHordeStartersShouldUseNearestSpiritHealer() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc tirisfal = g.closest(0, 1676.35f, 1677.45f, 121.67f,
                GraveyardManager.HORDE, AreaTable.TIRISFAL, AreaTable.TIRISFAL);
        assertEquals(94, tirisfal.id());
        assertEquals(0, tirisfal.map());
        assertEquals(1882.94f, tirisfal.x(), 0.5f);
        GraveyardManager.Loc durotar = g.closest(1, -618.518f, -4251.67f, 38.718f,
                GraveyardManager.HORDE, AreaTable.DUROTAR, AreaTable.DUROTAR);
        assertEquals(709, durotar.id());
        assertEquals(1, durotar.map());
        assertEquals(-634.635f, durotar.x(), 0.5f);
        GraveyardManager.Loc mulgore = g.closest(1, -2917.58f, -257.98f, 52.9968f,
                GraveyardManager.HORDE, AreaTable.MULGORE, AreaTable.MULGORE);
        assertEquals(34, mulgore.id());
        assertEquals(1, mulgore.map());
        assertEquals(-2944.56f, mulgore.x(), 0.5f);
    }

    /** Map 0 Horde with unknown area must stay on EK (Deathknell), not Barrens default. */
    @Test
    void closestWhenMap0HordeUnknownAreaShouldUseTirisfalMapLink() {
        GraveyardManager g = GraveyardManager.seeded();
        GraveyardManager.Loc loc = g.closest(0, 0f, 0f, 0f, GraveyardManager.HORDE, 0, 0);
        assertEquals(94, loc.id());
        assertEquals(0, loc.map());
    }

    /**
     * TP-SL17-024 — tbc-db game_graveyard_zone has no 3431 row. After SQL overlay, Blood Elf
     * createinfo still resolves to world_safe_locs 912 on map 530, not Horde default / void.
     */
    @Test
    void loadWhenSqlOmitsSunstriderAreaShouldKeepIsleGy912() throws Exception {
        String url = "jdbc:h2:mem:gy3431_" + java.util.UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (org.tbc.common.DbPool worldDb = new org.tbc.common.DbPool(url, "sa", "", "gy-3431")) {
            try (java.sql.Connection c = worldDb.get(); java.sql.Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE world_safe_locs (
                          id INT PRIMARY KEY, map INT, x FLOAT, y FLOAT, z FLOAT, o FLOAT)
                        """);
                st.execute("""
                        CREATE TABLE game_graveyard_zone (
                          id INT, ghost_loc INT, link_kind INT, faction INT)
                        """);
                st.execute("""
                        INSERT INTO world_safe_locs (id, map, x, y, z, o) VALUES
                        (10, 1, -618.518, -4251.67, 38.718, 0),
                        (912, 530, 10458.5, -6364.61, 39.7907, 5.49779),
                        (914, 530, 8936.56, -7439.9, 82.0856, 5.49779),
                        (921, 530, 9407, -6847.67, 16, 5.70723),
                        (922, 530, 8709.46, -6671.76, 70.336, 3.14159)
                        """);
                st.execute("""
                        INSERT INTO game_graveyard_zone (id, ghost_loc, link_kind, faction) VALUES
                        (912, 3430, 0, 0),
                        (914, 3430, 0, 0),
                        (921, 3430, 0, 0),
                        (922, 3430, 0, 0)
                        """);
            }
            GraveyardManager g = new GraveyardManager();
            g.addLoc(new GraveyardManager.Loc(GraveyardManager.DEFAULT_HORDE, 1, -618.518f, -4251.67f, 38.718f, 0f));
            g.load(worldDb);
            GraveyardManager.Loc loc = g.closest(530, 10349.6f, -6357.29f, 33.4026f,
                    GraveyardManager.HORDE, AreaTable.SUNSTRIDER_ISLE, 0);
            assertEquals(912, loc.id());
            assertEquals(530, loc.map());
            assertEquals(10458.5f, loc.x(), 0.5f);
            assertEquals(39.7907f, loc.z(), 0.5f);
        }
    }
}
