package org.tbc.world.spell;

import org.junit.jupiter.api.Test;
import org.tbc.world.classless.ClasslessConfig;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.PacketSink;
import org.tbc.world.session.WorldSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spell.dbc powerType CheckPower / TakePower — mana / rage / energy pools and Hero AddOn push.
 */
class SpellEnginePowerTypeTest {

    @Test
    void hasPowerWhenNullOrZeroCostShouldReturnTrue() {
        Player p = new Player();
        SpellEngine.SpellInfo cost = new SpellEngine.SpellInfo(1, 0, 0, 0, 10, 0, 0, 0f);
        assertTrue(SpellEngine.hasPower(null, cost));
        assertTrue(SpellEngine.hasPower(p, null));
        assertTrue(SpellEngine.hasPower(p, new SpellEngine.SpellInfo(1, 0, 0, 0, 0, 0, 0, 0f)));
    }

    @Test
    void hasPowerAndTakePowerWhenRageShouldUsePower2() {
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER2, Player.POWER_RAGE_MAX);
        p.setRage(100);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(6673, 0, 0, 0, 10, 0, 0, 0f)
                .withPowerType(Player.POWER_RAGE);
        assertTrue(SpellEngine.hasPower(p, sp));
        SpellEngine.takePower(p, sp, null);
        assertEquals(90, p.rage());
        p.setRage(5);
        assertFalse(SpellEngine.hasPower(p, sp));
    }

    @Test
    void hasPowerAndTakePowerWhenEnergyShouldUsePower4() {
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, Player.POWER_ENERGY_MAX);
        p.setInt(UpdateFields.UNIT_FIELD_POWER4, 80);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(2098, 0, 0, 0, 35, 0, 0, 0f)
                .withPowerType(Player.POWER_ENERGY);
        assertTrue(SpellEngine.hasPower(p, sp));
        assertEquals(80, SpellEngine.currentPower(p, Player.POWER_ENERGY));
        List<Integer> ops = new ArrayList<>();
        SpellEngine.takePower(p, sp, (op, pl) -> ops.add(op));
        assertEquals(45, p.getInt(UpdateFields.UNIT_FIELD_POWER4));
        assertFalse(ops.isEmpty());
        p.setInt(UpdateFields.UNIT_FIELD_POWER4, 10);
        assertFalse(SpellEngine.hasPower(p, sp));
    }

    @Test
    void takePowerWhenManaShouldDebitPower1AndSendValues() {
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, 100);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(133, 0, 0, 0, 30, 0, 0, 0f);
        Map<Integer, byte[]> last = new HashMap<>();
        SpellEngine.takePower(p, sp, last::put);
        assertEquals(70, p.getInt(UpdateFields.UNIT_FIELD_POWER1));
        assertTrue(last.containsKey(Opcodes.SMSG_UPDATE_OBJECT)
                || last.containsKey(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
    }

    @Test
    void takePowerWhenClasslessWithSessionShouldPushAddonFields() {
        Sink sink = new Sink();
        WorldSession session = new WorldSession(sink, 1);
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.session = session;
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER2, Player.POWER_RAGE_MAX);
        p.setRage(50);
        session.setHeroPowerAddonEnabled(true);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(6673, 0, 0, 0, 10, 0, 0, 0f)
                .withPowerType(Player.POWER_RAGE);
        SpellEngine.takePower(p, sp, sink::send);
        assertEquals(40, p.rage());
        assertTrue(sink.ops.contains(Opcodes.SMSG_MESSAGECHAT)
                || sink.ops.contains(Opcodes.SMSG_UPDATE_OBJECT)
                || sink.ops.contains(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
    }

    @Test
    void takePowerWhenClasslessWithoutSessionShouldStillDebit() {
        Player p = new Player();
        p.clazz = ClasslessConfig.CLASS_CLASSLESS;
        p.session = null;
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER2, Player.POWER_RAGE_MAX);
        p.setRage(50);
        SpellEngine.SpellInfo sp = new SpellEngine.SpellInfo(6673, 0, 0, 0, 10, 0, 0, 0f)
                .withPowerType(Player.POWER_RAGE);
        SpellEngine.takePower(p, sp, (op, pl) -> {
        });
        assertEquals(40, p.rage());
    }

    @Test
    void takePowerWhenNullOrZeroCostShouldNoOp() {
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, 100);
        SpellEngine.takePower(null, new SpellEngine.SpellInfo(1, 0, 0, 0, 10, 0, 0, 0f), null);
        SpellEngine.takePower(p, null, null);
        SpellEngine.takePower(p, new SpellEngine.SpellInfo(1, 0, 0, 0, 0, 0, 0, 0f), null);
        assertEquals(100, p.getInt(UpdateFields.UNIT_FIELD_POWER1));
    }

    @Test
    void powerTypeForTemplateShouldPreferSqlElseKeepSeed() {
        SpellEngine eng = new SpellEngine();
        assertEquals(7, eng.powerTypeForTemplate(999_001, 7));
        assertEquals(0, eng.powerTypeForTemplate(999_001, -1));
        // Seeded Heroic Strike keeps POWER_RAGE when SQL omits powerType (−1).
        assertEquals(Player.POWER_RAGE, eng.powerTypeForTemplate(SpellEngine.HEROIC_STRIKE, -1));
    }

    private static final class Sink implements PacketSink {
        final List<Integer> ops = new ArrayList<>();

        @Override
        public void send(int opcode, byte[] payload) {
            ops.add(opcode);
        }

        @Override
        public void close() {
        }
    }
}
