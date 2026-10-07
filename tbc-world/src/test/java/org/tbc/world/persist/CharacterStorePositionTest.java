package org.tbc.world.persist;

import org.tbc.common.DbPool;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CMaNGOS Player::LoadFromDB — invalid saved coordinates → RelocateToHomebind, so a character
 * saved in the void (x=-8.2e22) logs in at its hearth instead of falling forever.
 */
class CharacterStorePositionTest {
    private static final float VOID_X = -8.23932e22f;
    private static final float VOID_Y = -50713.7f;
    private static final float VOID_Z = -3.28652e37f;

    @Test
    void loadWhenSnapshotPositionInvalidShouldRelocateToHomebind() {
        World world = World.inMemory();
        Player p = world.characters.create(1, "Bevoid", 10, 8, 0, 1, 1, 1, 1, 0, world.objectMgr);
        p.mapId = 1;
        p.relocate(VOID_X, VOID_Y, VOID_Z, 3.4f);
        world.characters.save(p);
        Player loaded = world.characters.load(1, p.guid, world.objectMgr);
        assertAtHomebind(p, loaded);
    }

    @Test
    void loadWhenSnapshotPositionValidShouldKeepIt() {
        World world = World.inMemory();
        Player p = world.characters.create(1, "Bevalid", 10, 8, 0, 1, 1, 1, 1, 0, world.objectMgr);
        p.relocate(10381.6f, -6399.23f, 38.5306f, 3.74f);
        world.characters.save(p);
        Player loaded = world.characters.load(1, p.guid, world.objectMgr);
        assertEquals(10381.6f, loaded.x, 0.01f);
        assertEquals(-6399.23f, loaded.y, 0.01f);
    }

    @Test
    void loadWhenSqlPositionInvalidShouldRelocateToHomebind() throws Exception {
        String url = "jdbc:h2:mem:void_pos_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool chars = new DbPool(url, "sa", "", "void-pos-test")) {
            createSchema(chars);
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(null, null);
            CharacterStore store = new CharacterStore(chars);
            Player p = store.create(1, "Dude", 10, 8, 0, 1, 1, 1, 1, 0, mgr);
            store.save(p);
            try (Connection c = chars.get(); Statement st = c.createStatement()) {
                st.execute("UPDATE characters SET map = 1, position_x = " + VOID_X + ", position_y = " + VOID_Y
                        + ", position_z = " + VOID_Z);
            }
            Player loaded = new CharacterStore(chars).load(1, p.guid, mgr);
            assertAtHomebind(p, loaded);
        }
    }

    private static void assertAtHomebind(Player saved, Player loaded) {
        assertEquals(saved.bindMap, loaded.mapId);
        assertEquals(saved.bindX, loaded.x, 0.01f);
        assertEquals(saved.bindY, loaded.y, 0.01f);
        assertEquals(saved.bindZ, loaded.z, 0.01f);
    }

    private static void createSchema(DbPool chars) throws Exception {
        try (Connection c = chars.get(); Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE characters (
                      guid INT PRIMARY KEY, account INT, name VARCHAR(12), race INT, class INT, gender INT,
                      level INT, xp INT, money INT, playerBytes INT, playerBytes2 INT, playerFlags INT,
                      position_x FLOAT, position_y FLOAT, position_z FLOAT, map INT, dungeon_difficulty INT,
                      orientation FLOAT, online INT, cinematic INT, totaltime INT, leveltime INT,
                      logout_time BIGINT, is_logout_resting INT, rest_bonus FLOAT, zone INT, at_login INT,
                      health INT, power1 INT, power2 INT, power3 INT, power4 INT, power5 INT,
                      watchedFaction BIGINT, actionBars TINYINT, deleteDate BIGINT)
                    """);
            st.execute("""
                    CREATE TABLE character_spell (
                      guid INT, spell INT, active TINYINT, disabled TINYINT, PRIMARY KEY (guid, spell))
                    """);
            st.execute("""
                    CREATE TABLE character_skills (
                      guid INT, skill INT, `value` INT, `max` INT, PRIMARY KEY (guid, skill))
                    """);
            st.execute("CREATE TABLE item_instance (guid INT PRIMARY KEY)");
            st.execute("""
                    CREATE TABLE character_action (
                      guid INT, button INT, action INT, type INT, PRIMARY KEY (guid, button))
                    """);
        }
    }
}
