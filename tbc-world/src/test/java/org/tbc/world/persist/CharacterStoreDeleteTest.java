package org.tbc.world.persist;

import org.tbc.common.DbPool;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CMSG_CHAR_DELETE — a character loaded from SQL lives in {@code memory} but not in
 * {@code byAccount} (that index is only filled on create in this process). Delete must not
 * call {@code removeIf} on {@code List.of()}.
 */
class CharacterStoreDeleteTest {

    @Test
    void deleteWhenLoadedFromSqlWithoutAccountIndexShouldSucceed() throws Exception {
        String url = "jdbc:h2:mem:char_del_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try (DbPool chars = new DbPool(url, "sa", "", "char-delete-test")) {
            createSchema(chars);
            ObjectMgr mgr = new ObjectMgr();
            mgr.load(null, null);
            CharacterStore creator = new CharacterStore(chars);
            Player p = creator.create(1, "Doomed", 10, 8, 0, 1, 1, 1, 1, 0, mgr);
            creator.save(p);
            long guid = p.guid;

            CharacterStore afterRestart = new CharacterStore(chars);
            assertTrue(afterRestart.load(1, guid, mgr) != null);

            assertDoesNotThrow(() -> assertTrue(afterRestart.delete(1, guid)));
            assertNull(afterRestart.load(1, guid, mgr));
        }
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
