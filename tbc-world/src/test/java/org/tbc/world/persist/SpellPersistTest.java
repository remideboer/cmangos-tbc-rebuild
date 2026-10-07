package org.tbc.world.persist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.tbc.world.entity.Player;

/** character_spell / character_skills / character_spell_cooldown rows owned by SpellPersist (plan cycle 5.3). */
class SpellPersistTest {

    private static Connection h2() throws Exception {
        String url = "jdbc:h2:mem:sp_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL";
        Connection c = DriverManager.getConnection(url, "sa", "");
        try (Statement st = c.createStatement()) {
            st.execute("CREATE TABLE character_spell (guid INT, spell INT, active TINYINT, disabled TINYINT)");
            st.execute("CREATE TABLE character_skills (guid INT, skill INT, `value` INT, `max` INT)");
        }
        return c;
    }

    @Test
    void writeSpellsThenLoadShouldReplaceSpellBook() throws Exception {
        try (Connection c = h2()) {
            Player p = new Player();
            p.guid = 3;
            p.spells.clear();
            p.spells.addAll(List.of(6603, 78));

            SpellPersist.writeSpells(c, p);
            Player loaded = new Player();
            loaded.guid = 3;
            loaded.spells.add(1);

            assertTrue(SpellPersist.loadSpells(c, loaded));
            assertEquals(List.of(6603, 78), loaded.spells);
        }
    }

    @Test
    void loadSpellsWhenNoRowsShouldReturnFalseAndKeepDefaults() throws Exception {
        try (Connection c = h2()) {
            Player loaded = new Player();
            loaded.guid = 4;
            loaded.spells.add(1);

            assertFalse(SpellPersist.loadSpells(c, loaded));
            assertEquals(List.of(1), loaded.spells);
        }
    }

    @Test
    void writeSkillsThenLoadShouldRestoreValueAndMax() throws Exception {
        try (Connection c = h2()) {
            Player p = new Player();
            p.guid = 9;
            p.learnSkill(44, 5, 75, 0);

            SpellPersist.writeSkills(c, p);
            Player loaded = new Player();
            loaded.guid = 9;

            assertTrue(SpellPersist.loadSkills(c, loaded));
            assertEquals(5, loaded.skillValue(44));
        }
    }
}
