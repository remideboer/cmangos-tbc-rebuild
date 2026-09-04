package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.script.ClassScripts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellEngineTest {
    private final SpellEngine engine = SpellEngine.alwaysHit();
    private final List<Integer> ops = new ArrayList<>();
    private final Map<Integer, byte[]> last = new HashMap<>();
    private byte[] lastCastResult;
    private Player p;
    private Creature c;
    private GameMap map;

    @BeforeEach
    void setUp() {
        ops.clear();
        last.clear();
        p = new Player();
        p.guid = 1;
        p.spells.add(SpellEngine.FIREBALL);
        p.spells.add(78);
        p.spells.add(2050);
        p.spells.add(ClassScripts.SPELL_EXECUTE);
        p.spells.add(30108);
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        p.setPower(100);
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 50);
        p.setHealth(40);
        p.relocate(0, 0, 0, 0);
        c = new Creature();
        c.guid = 2;
        c.applyTemplate(6, "Kobold Vermin", 1, 7, 42, 1);
        c.relocate(0, 0, 0, 0);
        map = new GameMap(0, 0);
        map.add(p);
        map.add(c);
    }

    @Test
    void castFireballWhenMagicMissShouldSendSpellLogMissWithoutDamage() {
        SpellEngine miss = new SpellEngine(() -> 0.0);
        int hp = c.health();
        miss.cast(p, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        assertEquals(70, p.power());
        assertEquals(hp, c.health());
        assertFalse(ops.contains(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        assertTrue(ops.contains(Opcodes.SMSG_SPELLLOGMISS));
        WowBuffer missLog = new WowBuffer(last.get(Opcodes.SMSG_SPELLLOGMISS));
        assertEquals(SpellEngine.FIREBALL, missLog.getU32());
        assertEquals(p.guid, missLog.getU64());
        assertEquals(0, missLog.getU8());
        assertEquals(1, missLog.getU32());
        assertEquals(c.guid, missLog.getU64());
        assertEquals(1, missLog.getU8());
        WowBuffer go = new WowBuffer(last.get(Opcodes.SMSG_SPELL_GO));
        go.getPackedGuid();
        go.getPackedGuid();
        assertEquals(SpellEngine.FIREBALL, go.getU32());
        go.getU16();
        go.getU32();
        assertEquals(0, go.getU8());
        assertEquals(1, go.getU8());
        assertEquals(c.guid, go.getU64());
        assertEquals(1, go.getU8());
    }

    @Test
    void castFireballWhenMissRollAtFourPercentShouldDealDamage() {
        SpellEngine atFloor = new SpellEngine(() -> 0.04);
        atFloor.cast(p, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        assertEquals(32, c.health());
        assertTrue(ops.contains(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        assertFalse(ops.contains(Opcodes.SMSG_SPELLLOGMISS));
    }

    @Test
    void castFireballSpendsManaAndLogsDamage() {
        engine.cast(p, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        assertEquals(70, p.power());
        assertEquals(32, c.health());
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_START));
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertTrue(ops.contains(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        assertFalse(ops.contains(Opcodes.SMSG_CAST_RESULT));
    }

    @Test
    void castWhenHeroicStrike78ShouldQueueNextMeleeWithoutDamageLog() {
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        p.setPower(200);
        int hp = c.health();
        engine.cast(p, map, 0, 78, 1, unitTarget(c.guid), this::capture);
        assertTrue(p.hasNextMeleeSwingQueued());
        assertEquals(hp, c.health());
        assertEquals(50, p.power());
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertFalse(ops.contains(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        assertEquals(2, p.queuedNextMeleeBonus());
    }

    @Test
    void castFailuresAndIgnores() {
        engine.cast(p, map, 0, 0, 1, empty(), this::capture);
        engine.cast(p, map, 0, 9, 1, empty(), this::capture);
        assertTrue(ops.isEmpty());
        engine.cast(p, map, 0, 36300, 3, empty(), this::capture);
        assertEquals(SpellEngine.SPELL_FAILED_NOT_KNOWN, result());
        ops.clear();
        p.relocate(40, 0, 0, 0);
        engine.cast(p, map, 0, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        assertEquals(SpellEngine.SPELL_FAILED_OUT_OF_RANGE, result());
        p.relocate(6, 0, 0, 0);
        ops.clear();
        engine.cast(p, map, 0, 78, 1, unitTarget(c.guid), this::capture);
        assertEquals(SpellEngine.SPELL_FAILED_OUT_OF_RANGE, result());
        p.relocate(0, 0, 0, 0);
        ops.clear();
        p.setPower(0);
        engine.cast(p, map, 0, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        assertEquals(SpellEngine.SPELL_FAILED_NO_POWER, result());
        assertEquals(0, p.power());
        ops.clear();
        engine.cast(p, map, 0, SpellEngine.FIREBALL, 1, unitTarget(99), this::capture);
        assertEquals(SpellEngine.SPELL_FAILED_BAD_TARGETS, result());
    }

    @Test
    void tpSl13CatalogDummyAndKnownEffects() {
        assertTrue(engine.knownEffect(SpellEngine.EFFECT_SCHOOL_DAMAGE));
        assertTrue(engine.knownEffect(SpellEngine.EFFECT_DUMMY));
        engine.catalogDummy(SpellEngine.EFFECT_DUMMY);
        engine.catalogDummy(SpellEngine.EFFECT_SCRIPT);
    }

    @Test
    void dummyHealAuraWeaponAndExecute() {
        engine.cast(p, map, 0, SpellEngine.LOGINEFFECT, 1, empty(), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertFalse(ops.contains(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        ops.clear();
        engine.cast(p, map, 0, 2050, 1, empty(), this::capture);
        assertEquals(50, p.health());
        assertEquals(80, p.power());
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(40);
        engine.apply(c, p, engine.info(2050));
        assertEquals(52, p.health());
        ops.clear();
        engine.cast(p, map, 0, 30108, 1, unitTarget(c.guid), this::capture);
        assertEquals(1, c.auras.size());
        ops.clear();
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        p.setPower(200);
        engine.cast(p, map, 0, 78, 1, unitTarget(c.guid), this::capture);
        assertTrue(p.hasNextMeleeSwingQueued());
        assertFalse(ops.contains(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        assertEquals(50, p.power());
        engine.apply(p, c, engine.info(78));
        engine.apply(p, c, engine.info(ClassScripts.SPELL_EXECUTE));
        engine.apply(p, p, new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_SCRIPT, 0, 0, 0, 0, 0, 0f));
        engine.apply(p, p, new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_DUMMY, 0, 0, 0, 0, 0, 0f));
        engine.apply(p, p, new SpellEngine.SpellInfo(ClassScripts.SPELL_EXECUTE, SpellEngine.EFFECT_SCRIPT, 0, 0, 0, 0, 0, 0f));
        engine.apply(p, p, new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_OPEN_LOCK, 0, 0, 0, 0, 0, 0f));
        engine.apply(p, p, new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_ENERGIZE, 0, 0, 0, 0, 0, 0f));
        engine.apply(p, p, new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_ADD_HONOR, 0, 0, 0, 0, 0, 0f));
        assertEquals(0, engine.apply(p, p, null));
        assertEquals(0, engine.apply(p, null, engine.info(78)));
        engine.sendFail(this::capture, 78, SpellEngine.SPELL_CAST_OK, 1);
        assertEquals(Opcodes.SMSG_CAST_RESULT, SpellEngine.opcodeCastResult());
        assertTrue(engine.knownEffect(SpellEngine.EFFECT_SCHOOL_DAMAGE));
        assertFalse(engine.knownEffect(0));
        engine.catalogDummy(SpellEngine.EFFECT_DUMMY);
        assertSame(p, SpellEngine.resolve(p, map, 0));
        assertSame(p, SpellEngine.resolve(p, map, p.guid));
        Player other = new Player();
        other.guid = 8;
        map.add(other);
        assertSame(other, SpellEngine.resolve(p, map, 8));
        assertFalse(SpellEngine.outOfRange(p, p, engine.info(SpellEngine.FIREBALL)));
        assertFalse(SpellEngine.outOfRange(p, c, engine.info(2050)));
        assertFalse(SpellEngine.outOfRange(p, c, engine.info(SpellEngine.FIREBALL)));
        c.relocate(31, 0, 0, 0);
        assertTrue(SpellEngine.outOfRange(p, c, engine.info(SpellEngine.FIREBALL)));
    }

    @Test
    void tickPeriodicWhenUnstableAfflictionShouldSendPeriodicAuraLogMatchingHealthDelta() {
        int hp = c.health();
        engine.tickPeriodic(p, c, engine.info(30108), this::capture);
        assertEquals(hp, c.health());
        WowBuffer log = new WowBuffer(last.get(Opcodes.SMSG_PERIODICAURALOG));
        assertEquals(c.guid, log.getPackedGuid());
        assertEquals(p.guid, log.getPackedGuid());
        assertEquals(30108, log.getU32());
        assertEquals(1, log.getU32());
        assertEquals(3, log.getU32());
        assertEquals(0, log.getU32());
        assertEquals(5, log.getU32());
        assertEquals(0, log.getU32());
        assertEquals(0, log.getU32());
    }

    @Test
    void applyWhenLesserHealCritsShouldHealOneAndAHalfTimes() {
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(40);
        p.setFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1, 5f);
        SpellEngine crit = new SpellEngine(() -> 0.0);
        crit.apply(p, p, crit.info(2050));
        assertEquals(58, p.health());
    }

    @Test
    void tickPeriodicWhenInvalidShouldIgnore() {
        engine.tickPeriodic(null, c, engine.info(30108), this::capture);
        engine.tickPeriodic(p, null, engine.info(30108), this::capture);
        engine.tickPeriodic(p, c, null, this::capture);
        engine.tickPeriodic(p, c, engine.info(30108), null);
        engine.tickPeriodic(p, c, engine.info(36300), this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_PERIODICAURALOG));
    }

    private void capture(int opcode, byte[] payload) {
        ops.add(opcode);
        last.put(opcode, payload);
        if (opcode == Opcodes.SMSG_CAST_RESULT) {
            lastCastResult = payload;
        }
    }

    private static WowBuffer empty() {
        return new WowBuffer(new byte[0]);
    }

    private static WowBuffer unitTarget(long guid) {
        WowBuffer b = new WowBuffer(16);
        b.putU32(SpellCastTargets.UNIT);
        b.putPackedGuid(guid);
        return b;
    }

    private int result() {
        assertTrue(lastCastResult != null && lastCastResult.length >= 5);
        return lastCastResult[4] & 0xFF;
    }
}
