package org.tbc.world.world;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tbc.common.Conf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** World tunables carved out of the World constructor (refactoring plan cycle 3.4). */
class WorldConfigTest {
    @Test
    void inMemoryShouldUseTheMangosdDefaults() {
        WorldConfig c = WorldConfig.inMemory();
        assertEquals("Welcome to the 8606 rebuild.", c.motd());
        assertEquals(1, c.realmId());
        assertEquals(3, c.instantLogout());
        assertEquals(2, c.maxOverspeedPings());
        assertEquals(25.0, c.sayRange());
        assertEquals(300.0, c.yellRange());
        assertEquals(900_000, c.saveIntervalMs());
        assertFalse(c.inboundOpcodeTrace());
    }

    @Test
    void fromConfWhenKeysMissingShouldEqualInMemory(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("mangosd.conf");
        Files.writeString(f, "# empty\n");
        assertEquals(WorldConfig.inMemory(), WorldConfig.fromConf(Conf.load(f, null)));
    }

    @Test
    void fromConfShouldReadTheMangosdKeys(@TempDir Path dir) throws IOException {
        Path f = dir.resolve("mangosd.conf");
        Files.writeString(f, String.join("\n",
                "Motd = \"Hello@World\"",
                "RealmID = 7",
                "InstantLogout = 1",
                "MaxOverspeedPings = 0",
                "PlayerSave.Interval = 60000",
                "LogInboundOpcodes = 1"));
        WorldConfig c = WorldConfig.fromConf(Conf.load(f, null));
        assertEquals("Hello@World", c.motd());
        assertEquals(7, c.realmId());
        assertEquals(1, c.instantLogout());
        assertEquals(0, c.maxOverspeedPings());
        assertEquals(60_000, c.saveIntervalMs());
        assertTrue(c.inboundOpcodeTrace());
        assertEquals(25.0, c.sayRange(), "say/yell range are not configurable in 2.4.3 mangosd.conf");
        assertEquals(300.0, c.yellRange());
    }
}
