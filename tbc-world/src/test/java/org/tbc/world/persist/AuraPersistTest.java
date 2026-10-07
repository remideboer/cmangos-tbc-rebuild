package org.tbc.world.persist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;

/** character_aura rows owned by AuraPersist, split out of CharacterStore (plan cycle 5.3). */
class AuraPersistTest {

    @Test
    void writeThenLoadShouldRestorePermanentAuraWithStacksAndCaster() throws Exception {
        String url = "jdbc:h2:mem:ap_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL";
        try (Connection c = DriverManager.getConnection(url, "sa", "")) {
            try (Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE character_aura (
                          guid INT, caster_guid BIGINT, item_guid INT, spell INT,
                          stackcount INT, remaincharges INT,
                          basepoints0 INT, basepoints1 INT, basepoints2 INT,
                          periodictime0 INT, periodictime1 INT, periodictime2 INT,
                          maxduration INT, remaintime INT, effIndexMask INT)
                        """);
            }
            Player p = new Player();
            p.guid = 5;
            p.auras.add(new Unit.Aura(1243, -1, 2, 0, 0, 0, 0, 99));

            AuraPersist.write(c, p);
            Player loaded = new Player();
            loaded.guid = 5;
            AuraPersist.load(c, loaded);

            assertEquals(1, loaded.auras.size());
            Unit.Aura a = loaded.auras.get(0);
            assertEquals(1243, a.spellId());
            assertEquals(2, a.stacks());
            assertEquals(99, a.casterGuid());
            assertTrue(a.expireAtMs() == 0, "permanent aura has no expiry");
        }
    }
}
