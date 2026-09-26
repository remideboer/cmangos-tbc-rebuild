package org.tbc.world.spell;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
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
    /** Fireball 133 / Lesser Heal 2050 rank 1 cast bar (SpellCastTimes.dbc index 16). */
    private static final int FIREBALL_CAST_MS = 1500;
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
        p.spells.add(SpellEngine.FROST_NOVA);
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
        miss.update(FIREBALL_CAST_MS, 10);
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
        atFloor.update(FIREBALL_CAST_MS, 10);
        assertEquals(32, c.health());
        assertTrue(ops.contains(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        assertFalse(ops.contains(Opcodes.SMSG_SPELLLOGMISS));
    }

    @Test
    void castFireballSpendsManaAndLogsDamage() {
        engine.cast(p, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        engine.update(FIREBALL_CAST_MS, 10);
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
        assertTrue(p.hasNextMeleeSpellQueued());
        assertEquals(SpellEngine.HEROIC_STRIKE, p.peekNextMeleeSpellId());
        assertEquals(hp, c.health());
        assertEquals(200, p.power(), "TakePower waits for the swing (Spell::cast)");
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_START));
        assertFalse(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertFalse(ops.contains(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        assertEquals(2, p.queuedNextMeleeBonus());
    }

    @Test
    void finishNextMeleeSwingWhenHeroicStrikeShouldSendGoAndDamageLog() {
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        p.setPower(200);
        p.setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, 5f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, 5f);
        c.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        c.setHealth(50);
        engine.cast(p, map, 0, SpellEngine.HEROIC_STRIKE, 1, unitTarget(c.guid), this::capture);
        ops.clear();
        int bonus = p.queuedNextMeleeBonus();
        int damage = 5 + bonus;
        c.setHealth(c.health() - damage);
        p.consumeNextMeleeSwing();
        engine.finishNextMeleeSwing(p, c, SpellEngine.HEROIC_STRIKE, 1, damage, 100, this::capture);
        assertEquals(50, p.power());
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_GO));
        WowBuffer log = new WowBuffer(last.get(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        assertEquals(c.guid, log.getPackedGuid());
        assertEquals(p.guid, log.getPackedGuid());
        assertEquals(SpellEngine.HEROIC_STRIKE, log.getU32());
        assertEquals(damage, log.getU32());
    }

    @Test
    void finishNextMeleeSwingWhenInvalidOrZeroDamageShouldIgnoreOrSkipLog() {
        engine.finishNextMeleeSwing(null, c, SpellEngine.HEROIC_STRIKE, 1, 5, 0, this::capture);
        engine.finishNextMeleeSwing(p, null, SpellEngine.HEROIC_STRIKE, 1, 5, 0, this::capture);
        engine.finishNextMeleeSwing(p, c, SpellEngine.HEROIC_STRIKE, 1, 5, 0, null);
        engine.finishNextMeleeSwing(p, c, 0, 1, 5, 0, this::capture);
        engine.finishNextMeleeSwing(p, c, 999_999, 1, 5, 0, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_SPELL_GO));
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        p.powerType = 1;
        p.setPower(200);
        engine.finishNextMeleeSwing(p, c, SpellEngine.HEROIC_STRIKE, 1, 0, 50, this::capture);
        assertEquals(50, p.power());
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertFalse(ops.contains(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        ops.clear();
        engine.finishNextMeleeSwing(p, c, SpellEngine.LOGINEFFECT, 1, 0, 60, this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertEquals(50, p.power(), "mana 0 spell does not TakePower");
    }

    /** TP-SL07-003 — Spell::update: timer counts down across ticks; no GO until it reaches 0. */
    @Test
    void castFireballWhenTimerNotElapsedShouldHoldGoAndPower() {
        engine.cast(p, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_START));
        assertFalse(ops.contains(Opcodes.SMSG_SPELL_GO));
        engine.update(1000, 10);
        assertFalse(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertEquals(100, p.power());
        engine.update(500, 10);
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertEquals(70, p.power());
    }

    /** TP-SL07-004 — Spell::update cancel(): any axis of the cast position changing interrupts; the cast is gone. */
    @Test
    void castFireballWhenCasterMovesOnYShouldInterruptAndDropCast() {
        engine.cast(p, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        p.relocate(0, 1, 0, 0);
        engine.update(100, 10);
        assertEquals(SpellEngine.SPELL_FAILED_INTERRUPTED, lastCastResult[4] & 0xFF);
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_FAILURE));
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_FAILED_OTHER));
        engine.update(2000, 10);
        assertFalse(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertEquals(100, p.power());
    }

    /** Spell::cancel → ResetGCD: after a movement cancel the caster may recast at once. */
    @Test
    void castFireballWhenCancelledByMovementShouldResetGcd() {
        engine.cast(p, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        p.relocate(0, 1, 0, 0);
        engine.update(100, 10);
        ops.clear();
        assertTrue(engine.cast(p, map, 20, SpellEngine.FIREBALL, 2, unitTarget(c.guid), this::capture));
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_START));
    }

    /** TP-SL07-005 — CheckCast HasGCD → SPELL_FAILED_NOT_READY; the second cast is not started. */
    @Test
    void castLesserHealDuringFireballGcdShouldFailNotReady() {
        engine.cast(p, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        ops.clear();
        assertFalse(engine.cast(p, map, 1000, 2050, 2, empty(), this::capture));
        assertEquals(SpellEngine.SPELL_FAILED_NOT_READY, lastCastResult[4] & 0xFF);
        assertFalse(ops.contains(Opcodes.SMSG_SPELL_START));
        assertTrue(engine.cast(p, map, 1510, 2050, 3, empty(), this::capture));
    }

    /** TP-SL07-005 — Spell::cast AddCooldown(RecoveryTime); after the GCD the spell is still NOT_READY. */
    @Test
    void castFrostNovaWhenRecoveryActiveShouldFailNotReady() {
        assertTrue(engine.cast(p, map, 10, SpellEngine.FROST_NOVA, 1, empty(), this::capture));
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertFalse(ops.contains(Opcodes.SMSG_SPELL_COOLDOWN));
        ops.clear();
        assertFalse(engine.cast(p, map, 10 + SpellCooldowns.GCD_NORMAL_MS,
                SpellEngine.FROST_NOVA, 2, empty(), this::capture));
        assertEquals(SpellEngine.SPELL_FAILED_NOT_READY, lastCastResult[4] & 0xFF);
        p.setPower(100);
        assertTrue(engine.cast(p, map, 10 + SpellEngine.FROST_NOVA_RECOVERY_MS,
                SpellEngine.FROST_NOVA, 3, empty(), this::capture));
    }

    /** Heroic Strike is on-next-swing (StartRecoveryTime 0): it neither checks nor starts the GCD. */
    @Test
    void castHeroicStrikeDuringGcdShouldBeAccepted() {
        engine.cast(p, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        p.setPower(200);
        ops.clear();
        assertTrue(engine.cast(p, map, 20, 78, 2, unitTarget(c.guid), this::capture));
        assertFalse(ops.contains(Opcodes.SMSG_CAST_RESULT));
    }

    @Test
    void castFireballWhenCasterMovesOnZShouldInterrupt() {
        engine.cast(p, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        p.relocate(0, 0, 1, 0);
        engine.update(100, 10);
        assertEquals(SpellEngine.SPELL_FAILED_INTERRUPTED, lastCastResult[4] & 0xFF);
    }

    @Test
    void castFireballWhenOnlyOrientationChangesShouldKeepCasting() {
        engine.cast(p, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        p.relocate(0, 0, 0, 1.5f);
        engine.update(1500, 10);
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertFalse(ops.contains(Opcodes.SMSG_SPELL_FAILURE));
    }

    /** Two casters: only the one who moved is cancelled, the other still lands. */
    @Test
    void updateWhenOneOfTwoCastersMovesShouldCancelOnlyThatCast() {
        Player other = new Player();
        other.guid = 7;
        other.spells.add(SpellEngine.FIREBALL);
        other.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 100);
        other.setPower(100);
        other.relocate(0, 0, 0, 0);
        map.add(other);
        List<Integer> otherOps = new ArrayList<>();
        engine.cast(p, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        engine.cast(other, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), (op, b) -> otherOps.add(op));
        p.relocate(0, 1, 0, 0);
        engine.update(1500, 10);
        assertEquals(SpellEngine.SPELL_FAILED_INTERRUPTED, lastCastResult[4] & 0xFF);
        assertFalse(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertTrue(otherOps.contains(Opcodes.SMSG_SPELL_GO));
        assertEquals(70, other.power());
    }

    /** SendInterrupted goes to the set; a nearby player without a session is skipped. */
    @Test
    void castFireballWhenInterruptedShouldSkipSessionlessNeighbour() {
        Player ghost = new Player();
        ghost.guid = 9;
        ghost.relocate(1, 0, 0, 0);
        map.add(ghost);
        engine.cast(p, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        p.relocate(1, 0, 0, 0);
        engine.update(100, 10);
        assertEquals(SpellEngine.SPELL_FAILED_INTERRUPTED, lastCastResult[4] & 0xFF);
    }

    /** Spell::cast re-checks power: mana lost during the cast bar fails with NO_POWER and no GO. */
    @Test
    void castFireballWhenManaGoneAtTimerShouldFailNoPower() {
        engine.cast(p, map, 10, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        p.setPower(0);
        engine.update(FIREBALL_CAST_MS, 10);
        assertEquals(SpellEngine.SPELL_FAILED_NO_POWER, result());
        assertFalse(ops.contains(Opcodes.SMSG_SPELL_GO));
    }

    @Test
    void castFailuresAndIgnores() {
        engine.cast(p, map, 0, 0, 1, empty(), this::capture);
        assertTrue(ops.isEmpty());
        engine.cast(p, map, 0, 9, 1, empty(), this::capture);
        assertEquals(SpellEngine.SPELL_FAILED_ERROR, result());
        ops.clear();
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
    void castFromItemWhenHearthstoneUnknownShouldSendStartWithTenSecondTimer() {
        assertFalse(p.spells.contains(SpellEngine.HEARTHSTONE));
        assertTrue(engine.castFromItem(p, map, 10, SpellEngine.HEARTHSTONE, 1, empty(), this::capture));
        WowBuffer start = new WowBuffer(last.get(Opcodes.SMSG_SPELL_START));
        start.getPackedGuid();
        start.getPackedGuid();
        assertEquals(SpellEngine.HEARTHSTONE, start.getU32());
        assertEquals(1, start.getU8());
        start.getU16();
        assertEquals(SpellEngine.HEARTHSTONE_CAST_MS, start.getU32());
        assertFalse(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertFalse(ops.contains(Opcodes.SMSG_CAST_RESULT));
    }

    @Test
    void castFromItemWhenOnFinishedNullShouldUseNoOpCallback() {
        assertTrue(engine.castFromItem(p, map, 10, SpellEngine.SPELL_FOOD, 1, empty(), this::capture, null));
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_GO));
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
        engine.update(FIREBALL_CAST_MS, 0);
        assertEquals(50, p.health());
        assertEquals(80, p.power());
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(40);
        engine.apply(c, p, engine.info(2050));
        assertEquals(52, p.health());
        ops.clear();
        // Lesser Heal started the 1500 ms GCD at t=0; the next spell waits for it.
        engine.cast(p, map, 2000, 30108, 1, unitTarget(c.guid), this::capture);
        assertEquals(1, c.auras.size());
        ops.clear();
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        p.setPower(200);
        engine.cast(p, map, 0, 78, 1, unitTarget(c.guid), this::capture);
        assertTrue(p.hasNextMeleeSpellQueued());
        assertFalse(ops.contains(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        assertEquals(200, p.power(), "rage taken on the swing, not on queue");
        engine.apply(p, c, engine.info(78));
        engine.apply(p, c, engine.info(ClassScripts.SPELL_EXECUTE));
        engine.apply(p, p, new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_SCRIPT, 0, 0, 0, 0, 0, 0f));
        engine.apply(p, p, new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_DUMMY, 0, 0, 0, 0, 0, 0f));
        engine.apply(p, p, new SpellEngine.SpellInfo(ClassScripts.SPELL_EXECUTE, SpellEngine.EFFECT_SCRIPT, 0, 0, 0, 0, 0, 0f));
        engine.apply(p, p, new SpellEngine.SpellInfo(1, 0, 0, 0, 0, 0, 0, 0f));
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

    @Test
    void tickPeriodicWhenFoodShouldHealLivingTarget() {
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(40);
        engine.tickPeriodic(p, p, engine.info(SpellEngine.SPELL_FOOD), this::capture);
        assertEquals(57, p.health());
        assertTrue(ops.contains(Opcodes.SMSG_UPDATE_OBJECT) || ops.contains(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
    }

    @Test
    void tickPeriodicWhenFoodDeadOrZeroAmountShouldIgnore() {
        p.setHealth(0);
        engine.tickPeriodic(p, p, engine.info(SpellEngine.SPELL_FOOD), this::capture);
        assertEquals(0, p.health());
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                SpellEngine.SPELL_FOOD, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_REGEN,
                0, 0, 0, 0, 0f);
        p.setHealth(40);
        engine.tickPeriodic(p, p, zero, this::capture);
        assertEquals(40, p.health());
    }

    @Test
    void tickPeriodicWhenDrinkShouldRestoreMana() {
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        p.setPower(10);
        p.powerType = 0;
        engine.tickPeriodic(p, p, engine.info(SpellEngine.SPELL_DRINK), this::capture);
        assertEquals(52, p.power());
    }

    @Test
    void tickPeriodicWhenDrinkNonManaOrDeadShouldIgnore() {
        p.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        p.setPower(10);
        p.powerType = 1;
        engine.tickPeriodic(p, p, engine.info(SpellEngine.SPELL_DRINK), this::capture);
        assertEquals(10, p.getInt(UpdateFields.UNIT_FIELD_POWER1));
        p.powerType = 0;
        p.setHealth(0);
        engine.tickPeriodic(p, p, engine.info(SpellEngine.SPELL_DRINK), this::capture);
        assertEquals(10, p.power());
        SpellEngine.SpellInfo zero = new SpellEngine.SpellInfo(
                SpellEngine.SPELL_DRINK, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_POWER_REGEN,
                0, 0, 0, 0, 0f);
        p.setHealth(50);
        engine.tickPeriodic(p, p, zero, this::capture);
        assertEquals(10, p.power());
        engine.tickPeriodic(p, c, engine.info(SpellEngine.SPELL_DRINK), this::capture);
        assertEquals(10, p.power());
    }

    @Test
    void applyWhenFrostArmorShouldWriteVisibleSlotUsingTargetLevelIfCasterNull() {
        p.level = 3;
        engine.apply(null, p, engine.info(SpellEngine.FROST_ARMOR));
        assertEquals(SpellEngine.FROST_ARMOR, p.getInt(UpdateFields.UNIT_FIELD_AURA));
        assertEquals(3, p.getInt(UpdateFields.UNIT_FIELD_AURALEVELS) & 0xFF);
        assertEquals(SpellEngine.FROST_ARMOR_DURATION_MS, p.auras.get(p.auras.size() - 1).durationMs());
        assertEquals(0, p.auras.get(p.auras.size() - 1).expireAtMs());
        assertEquals(30, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(30, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void applyWhenNowMsSetShouldStampExpireAt() {
        engine.apply(p, p, engine.info(SpellEngine.FROST_ARMOR), 1000);
        assertEquals(1000 + SpellEngine.FROST_ARMOR_DURATION_MS, p.auras.get(p.auras.size() - 1).expireAtMs());
    }

    @Test
    void applyWhenPeriodicAndNowMsSetShouldStampNextTick() {
        engine.apply(p, c, engine.info(30108), 1000);
        org.tbc.world.entity.Unit.Aura a = c.auras.get(c.auras.size() - 1);
        assertEquals(SpellEngine.UA_AMPLITUDE_MS, a.amplitudeMs());
        assertEquals(1000 + SpellEngine.UA_AMPLITUDE_MS, a.nextTickAtMs());
        assertEquals(p.guid, a.casterGuid());
        engine.apply(p, c, engine.info(30108));
        assertEquals(0, c.auras.get(c.auras.size() - 1).nextTickAtMs());
        engine.apply(null, c, engine.info(30108), 2000);
        assertEquals(0, c.auras.get(c.auras.size() - 1).casterGuid());
    }

    @Test
    void castFrostArmorWhenInstantShouldSendAuraDuration() {
        p.spells.add(SpellEngine.FROST_ARMOR);
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, 200);
        engine.cast(p, map, 0, SpellEngine.FROST_ARMOR, 1, unitTarget(p.guid), this::capture);
        assertTrue(ops.contains(Opcodes.SMSG_UPDATE_AURA_DURATION));
        byte[] dur = last.get(Opcodes.SMSG_UPDATE_AURA_DURATION);
        assertEquals(0, dur[0] & 0xFF);
        int remain = (dur[1] & 0xFF) | ((dur[2] & 0xFF) << 8) | ((dur[3] & 0xFF) << 16) | ((dur[4] & 0xFF) << 24);
        assertEquals(SpellEngine.FROST_ARMOR_DURATION_MS, remain);
    }

    /**
     * spell_template load without SpellDuration.dbc yields DurationIndex→0 ms and must not wipe the
     * hand-seeded 30-minute Frost Armor (client was seeing the 30 s auraDurationMs fallback).
     */
    @Test
    void putTemplateWhenDurationZeroShouldKeepSeededFrostArmorThirtyMinutes() {
        engine.putTemplate(SpellEngine.FROST_ARMOR, SpellEngine.EFFECT_APPLY_AURA,
                SpellEngine.SPELL_AURA_MOD_RESISTANCE, 16, 60, 30, 30, 0f,
                0, SpellCooldowns.GCD_NORMAL_MS, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(SpellEngine.FROST_ARMOR_DURATION_MS, engine.info(SpellEngine.FROST_ARMOR).durationMs());
        assertEquals(1, engine.info(SpellEngine.FROST_ARMOR).misc());
        engine.apply(p, p, engine.info(SpellEngine.FROST_ARMOR), 1_000);
        assertEquals(SpellEngine.FROST_ARMOR_DURATION_MS, p.auras.get(p.auras.size() - 1).durationMs());
        assertEquals(1_000 + SpellEngine.FROST_ARMOR_DURATION_MS, p.auras.get(p.auras.size() - 1).expireAtMs());
    }

    @Test
    void putTemplateWhenDurationPositiveShouldReplaceSeed() {
        engine.putTemplate(SpellEngine.FROST_ARMOR, SpellEngine.EFFECT_APPLY_AURA,
                SpellEngine.SPELL_AURA_MOD_RESISTANCE, 16, 60, 30, 30, 0f,
                0, SpellCooldowns.GCD_NORMAL_MS, 0, 60_000,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(60_000, engine.info(SpellEngine.FROST_ARMOR).durationMs());
    }

    @Test
    void putTemplateWhenDurationZeroAndUnknownIdShouldStayZero() {
        engine.putTemplate(999_001, SpellEngine.EFFECT_SCHOOL_DAMAGE, 0, 0, 0, 1, 1, 0f,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(0, engine.info(999_001).durationMs());
        engine.putTemplate(999_001, SpellEngine.EFFECT_SCHOOL_DAMAGE, 0, 0, 0, 1, 1, 0f,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(0, engine.info(999_001).durationMs());
    }

    @Test
    void castDrainLifeWhenChanneledShouldSendChannelStart() {
        p.spells.add(SpellEngine.DRAIN_LIFE);
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, 200);
        p.setPower(200);
        engine.cast(p, map, 10, SpellEngine.DRAIN_LIFE, 1, unitTarget(c.guid), this::capture);
        assertTrue(p.channeling);
        assertTrue(ops.contains(Opcodes.MSG_CHANNEL_START));
        WowBuffer start = new WowBuffer(last.get(Opcodes.MSG_CHANNEL_START));
        assertEquals(p.guid, start.getPackedGuid());
        assertEquals(SpellEngine.DRAIN_LIFE, start.getU32());
        assertEquals(SpellEngine.DRAIN_LIFE_DURATION_MS, start.getU32());
        assertFalse(SpellEngine.isChanneled(null));
        assertFalse(SpellEngine.isChanneled(engine.info(SpellEngine.FIREBALL)));
    }

    @Test
    void cancelChannelWhenChannelingShouldSendUpdateZero() {
        engine.cancelChannel(null, this::capture);
        engine.cancelChannel(p, this::capture);
        assertFalse(ops.contains(Opcodes.MSG_CHANNEL_UPDATE));
        p.channeling = true;
        engine.cancelChannel(p, null);
        assertFalse(p.channeling);
        p.channeling = true;
        engine.cancelChannel(p, this::capture);
        WowBuffer upd = new WowBuffer(last.get(Opcodes.MSG_CHANNEL_UPDATE));
        assertEquals(p.guid, upd.getPackedGuid());
        assertEquals(0, upd.getU32());
        assertFalse(p.channeling);
    }

    @Test
    void cancelCastWhenPreparingShouldInterruptLikeMovement() {
        engine.cancelCast(p, SpellEngine.FIREBALL);
        p.spells.add(SpellEngine.FIREBALL);
        engine.cast(p, map, 0, SpellEngine.FIREBALL, 1, unitTarget(c.guid), this::capture);
        ops.clear();
        last.clear();
        engine.cancelCast(null, SpellEngine.FIREBALL);
        engine.cancelCast(p, 999);
        assertFalse(ops.contains(Opcodes.SMSG_CAST_RESULT));
        engine.cancelCast(p, SpellEngine.FIREBALL);
        assertTrue(ops.contains(Opcodes.SMSG_CAST_RESULT));
        engine.cast(p, map, 2000, SpellEngine.FIREBALL, 2, unitTarget(c.guid), this::capture);
        ops.clear();
        engine.cancelCast(p, 0);
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_FAILURE));
    }

    @Test
    void castHolyLightWhenKnownShouldStartThenHealAfterCastTime() {
        p.spells.add(SpellEngine.HOLY_LIGHT);
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 200);
        p.setHealth(40);
        assertTrue(engine.cast(p, map, 0, SpellEngine.HOLY_LIGHT, 4, empty(), this::capture));
        assertFalse(ops.contains(Opcodes.SMSG_SPELL_GO));
        WowBuffer start = new WowBuffer(last.get(Opcodes.SMSG_SPELL_START));
        start.getPackedGuid();
        start.getPackedGuid();
        assertEquals(SpellEngine.HOLY_LIGHT, start.getU32());
        assertEquals(4, start.getU8());
        start.getU16();
        assertEquals(2500, start.getU32());
        assertEquals(100, p.power());

        engine.update(2500, 2500);
        assertEquals(65, p.power());
        WowBuffer log = new WowBuffer(last.get(Opcodes.SMSG_SPELLHEALLOG));
        assertEquals(p.guid, log.getPackedGuid());
        assertEquals(p.guid, log.getPackedGuid());
        assertEquals(SpellEngine.HOLY_LIGHT, log.getU32());
        assertEquals(46, log.getU32());
        assertEquals(0, log.getU8());
        assertEquals(0, log.getU8());
        assertEquals(86, p.health());
        assertTrue(ops.contains(Opcodes.SMSG_UPDATE_OBJECT) || ops.contains(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_GO));
    }

    @Test
    void castHolyLightWhenSpellCritShouldMarkHealLogCritical() {
        p.spells.add(SpellEngine.HOLY_LIGHT);
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 200);
        p.setHealth(40);
        p.setFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1, 100f);
        SpellEngine crit = new SpellEngine(() -> 0.0);
        crit.cast(p, map, 0, SpellEngine.HOLY_LIGHT, 1, empty(), this::capture);
        crit.update(2500, 2500);
        WowBuffer log = new WowBuffer(last.get(Opcodes.SMSG_SPELLHEALLOG));
        log.getPackedGuid();
        log.getPackedGuid();
        log.getU32();
        assertEquals(69, log.getU32());
        assertEquals(1, log.getU8());
    }

    @Test
    void castSealOfRighteousnessWhenKnownShouldApplyOneAura() {
        p.spells.add(SpellEngine.SEAL_OF_RIGHTEOUSNESS);
        assertTrue(engine.cast(p, map, 1000, SpellEngine.SEAL_OF_RIGHTEOUSNESS, 2, empty(), this::capture));
        WowBuffer start = new WowBuffer(last.get(Opcodes.SMSG_SPELL_START));
        start.getPackedGuid();
        start.getPackedGuid();
        assertEquals(SpellEngine.SEAL_OF_RIGHTEOUSNESS, start.getU32());
        assertEquals(2, start.getU8());
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertEquals(80, p.power());
        assertEquals(1, sealCount());
        assertEquals(30_000, p.auras.get(p.auras.size() - 1).durationMs());
        assertTrue(ops.contains(Opcodes.SMSG_UPDATE_AURA_DURATION));

        engine.apply(p, p, engine.info(SpellEngine.FROST_ARMOR), 1000);
        engine.cast(p, map, 3000, SpellEngine.SEAL_OF_RIGHTEOUSNESS, 3, empty(), this::capture);
        assertEquals(1, sealCount());
        assertTrue(p.hasAura(SpellEngine.FROST_ARMOR));
    }

    @Test
    void castWhenSpellNotCataloguedShouldSendCastResultError() {
        engine.cast(p, map, 0, 9, 7, empty(), this::capture);
        assertEquals(SpellEngine.SPELL_FAILED_ERROR, result());
        assertEquals(9, lastCastResult[0] & 0xFF | (lastCastResult[1] & 0xFF) << 8
                | (lastCastResult[2] & 0xFF) << 16 | (lastCastResult[3] & 0xFF) << 24);
        assertEquals(7, lastCastResult[5] & 0xFF);
        assertFalse(ops.contains(Opcodes.SMSG_SPELL_START));
    }

    @Test
    void putTemplateWhenSpellMissingFromHandCatalogShouldCastThatRow() {
        assertTrue(engine.info(19750) == null);
        engine.putTemplate(19750, SpellEngine.EFFECT_HEAL, 0, 2, 35, 62, 72, 40f, 1500, 1500, 0, 0,
                SpellEngine.EFFECT_ENERGIZE, 0, 1, 1, 0, 0, 0, 0, 0, 0);
        p.spells.add(19750);
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 200);
        p.setHealth(40);
        assertTrue(engine.cast(p, map, 0, 19750, 5, empty(), this::capture));
        assertFalse(ops.contains(Opcodes.SMSG_SPELL_GO));
        engine.update(1500, 1500);
        assertEquals(66, p.power());
        assertEquals(107, p.health());
        WowBuffer log = new WowBuffer(last.get(Opcodes.SMSG_SPELLHEALLOG));
        log.getPackedGuid();
        log.getPackedGuid();
        assertEquals(19750, log.getU32());
        assertEquals(67, log.getU32());
    }

    @Test
    void procMeleeWhenSealOfRighteousnessShouldSendHolyDamageLog() {
        p.spells.add(SpellEngine.SEAL_OF_RIGHTEOUSNESS);
        p.setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME, 2000);
        p.setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, 10f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, 10f);
        c.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 200);
        c.setHealth(100);
        engine.apply(p, p, engine.info(SpellEngine.FROST_ARMOR), 0);
        engine.cast(p, map, 0, SpellEngine.SEAL_OF_RIGHTEOUSNESS, 1, empty(), this::capture);
        ops.clear();
        engine.procMelee(p, c, false, this::capture);
        WowBuffer log = new WowBuffer(last.get(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        assertEquals(c.guid, log.getPackedGuid());
        assertEquals(p.guid, log.getPackedGuid());
        assertEquals(25742, log.getU32());
        int dmg = log.getU32();
        // Tooltip 1H: 0.85*(108*1.2*1.03*2/100)+0.03*10-1 → trunc 1
        assertEquals(1, dmg);
        assertEquals(2, log.getU8());
        assertEquals(100 - dmg, c.health());
    }

    @Test
    void sealOfRighteousnessDamageWhenTwoHandShouldUseTwoHandFormula() {
        Item mh = new Item(1, 25);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.applyWeaponLine(SpellEngine.INVTYPE_2HWEAPON, 3300, 15f, 15f);
        p.items.put(1, mh);
        // Stale UNIT_FIELD fist line must not win over item proto (client tooltip uses proto).
        p.setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME, 2000);
        p.setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, 1f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, 3f);
        int dmg = SpellEngine.sealOfRighteousnessDamage(p, engine.info(SpellEngine.SEAL_OF_RIGHTEOUSNESS));
        // Tooltip 2H: 1.2*(108*1.2*1.03*3.3/100)+0.03*15+1 → trunc 6
        assertEquals(6, dmg);
    }

    @Test
    void sealOfRighteousnessDamageWhenOneHandOrZeroAttackTimeShouldUseOneHandFormula() {
        Item mh = new Item(2, 25);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.applyWeaponLine(13, 2000, 10f, 10f);
        p.items.put(2, mh);
        p.setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME, 0);
        p.setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, 10f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, 10f);
        int dmg = SpellEngine.sealOfRighteousnessDamage(p, engine.info(SpellEngine.SEAL_OF_RIGHTEOUSNESS));
        assertEquals(1, dmg);
    }

    @Test
    void sealOfRighteousnessDamageWhenItemProtoMatchesBuffTooltipShouldUseWeaponNotUnitField() {
        Item mh = new Item(3, 25);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.applyWeaponLine(SpellEngine.INVTYPE_2HWEAPON, 3500, 80f, 100f);
        p.items.put(3, mh);
        p.setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME, 2000);
        p.setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, 1f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, 3f);
        int dmg = SpellEngine.sealOfRighteousnessDamage(p, engine.info(SpellEngine.SEAL_OF_RIGHTEOUSNESS));
        // Tooltip 2H @ 3.5s / avg 90: 1.2*(108*1.2*1.03*3.5/100)+0.03*90+1 → trunc 9
        assertEquals(9, dmg);
    }

    @Test
    void sealOfRighteousnessDamageWhenApplyEquippedMeleeHealsItemShouldMatchTooltip() {
        org.tbc.world.content.ObjectMgr mgr = new org.tbc.world.content.ObjectMgr();
        mgr.load(null, null);
        int entry = 900_352;
        org.tbc.world.content.ObjectMgr.ItemTemplate t = new org.tbc.world.content.ObjectMgr.ItemTemplate();
        t.entry = entry;
        t.inventoryType = SpellEngine.INVTYPE_2HWEAPON;
        t.delay = 3500;
        t.dmgMin[0] = 80f;
        t.dmgMax[0] = 100f;
        mgr.items.put(entry, t);
        Item mh = new Item(4, entry);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.delay = 0;
        p.items.put(4, mh);
        p.setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME, 2000);
        p.setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, 1f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, 3f);
        mgr.applyEquippedMelee(p);
        assertEquals(9, SpellEngine.sealOfRighteousnessDamage(p, engine.info(SpellEngine.SEAL_OF_RIGHTEOUSNESS)));
    }

    @Test
    void sealOfRighteousnessDamageWhenMisclassifiedOneHandShouldBecomeTwoHandAfterHeal() {
        // Combat log 5 = 1H formula on 2H stats; buff 9 = client Item.dbc 2H. Heal inventoryType.
        org.tbc.world.content.ObjectMgr mgr = new org.tbc.world.content.ObjectMgr();
        mgr.load(null, null);
        int entry = 900_353;
        org.tbc.world.content.ObjectMgr.ItemTemplate t = new org.tbc.world.content.ObjectMgr.ItemTemplate();
        t.entry = entry;
        t.inventoryType = SpellEngine.INVTYPE_2HWEAPON;
        t.subClass = org.tbc.world.combat.MainhandWeaponStats.SUBCLASS_SWORD2;
        t.delay = 3500;
        t.dmgMin[0] = 80f;
        t.dmgMax[0] = 100f;
        mgr.items.put(entry, t);
        Item mh = new Item(7, entry);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.inventoryType = 13;
        mh.delay = 3500;
        mh.dmgMin = 80f;
        mh.dmgMax = 100f;
        p.items.put(7, mh);
        assertEquals(5, SpellEngine.sealOfRighteousnessDamage(p, engine.info(SpellEngine.SEAL_OF_RIGHTEOUSNESS)));
        mgr.applyEquippedMelee(p);
        assertEquals(9, SpellEngine.sealOfRighteousnessDamage(p, engine.info(SpellEngine.SEAL_OF_RIGHTEOUSNESS)));
    }

    @Test
    void sealOfRighteousnessDamageWhenTwoHandSubclassAndWrongInventoryTypeShouldUseTwoHandFormula() {
        Item mh = new Item(9, 2361);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        // Template/SQL left InventoryType as 21 but subclass is 2H mace — client buff still shows 9.
        mh.applyWeaponLine(21, org.tbc.world.combat.MainhandWeaponStats.SUBCLASS_MACE2, 3500, 80f, 100f);
        p.items.put(9, mh);
        assertEquals(9, SpellEngine.sealOfRighteousnessDamage(p, engine.info(SpellEngine.SEAL_OF_RIGHTEOUSNESS)));
    }

    @Test
    void procMeleeWhenSealOfRighteousnessTwoHandShouldLogTooltipDamageAndZeroResist() {
        org.tbc.world.content.ObjectMgr mgr = new org.tbc.world.content.ObjectMgr();
        mgr.load(null, null);
        int entry = 900_354;
        org.tbc.world.content.ObjectMgr.ItemTemplate t = new org.tbc.world.content.ObjectMgr.ItemTemplate();
        t.entry = entry;
        t.inventoryType = SpellEngine.INVTYPE_2HWEAPON;
        t.delay = 3500;
        t.dmgMin[0] = 80f;
        t.dmgMax[0] = 100f;
        mgr.items.put(entry, t);
        Item mh = new Item(8, entry);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.inventoryType = 13;
        mh.delay = 3500;
        mh.dmgMin = 80f;
        mh.dmgMax = 100f;
        p.items.put(8, mh);
        mgr.applyEquippedMelee(p);
        p.spells.add(SpellEngine.SEAL_OF_RIGHTEOUSNESS);
        c.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 200);
        c.setHealth(100);
        // Holy resist must not shrink SoR (CMaNGOS ignores SPELL_SCHOOL_MASK_HOLY).
        c.setInt(UpdateFields.UNIT_FIELD_RESISTANCES + 1, 100);
        engine.cast(p, map, 0, SpellEngine.SEAL_OF_RIGHTEOUSNESS, 1, empty(), this::capture);
        ops.clear();
        engine.procMelee(p, c, false, this::capture);
        WowBuffer log = new WowBuffer(last.get(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        log.getPackedGuid();
        log.getPackedGuid();
        assertEquals(25742, log.getU32());
        assertEquals(9, log.getU32());
        assertEquals(2, log.getU8());
        assertEquals(0, log.getU32());
        assertEquals(0, log.getU32());
        assertEquals(91, c.health());
    }

    @Test
    void sealOfRighteousnessDamageWhenMainhandHasNoDelayShouldFallBackToUnitField() {
        Item mh = new Item(5, 25);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.inventoryType = 13;
        mh.delay = 0;
        p.items.put(5, mh);
        p.setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME, 0);
        p.setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, 10f);
        p.setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, 10f);
        int dmg = SpellEngine.sealOfRighteousnessDamage(p, engine.info(SpellEngine.SEAL_OF_RIGHTEOUSNESS));
        // attackTime 0 → speed 2.0 default; same as 1H tooltip trunc 1
        assertEquals(1, dmg);
    }

    @Test
    void sealOfRighteousnessDamageWhenOneHandFloorIsNegativeShouldClampAtZero() {
        engine.putTemplate(SpellEngine.SEAL_OF_RIGHTEOUSNESS, SpellEngine.EFFECT_APPLY_AURA,
                SpellEngine.SPELL_AURA_DUMMY, 2, 20, 0, 0, 0f,
                0, 1500, 0, 30_000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        Item mh = new Item(6, 25);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.applyWeaponLine(13, 1000, 0f, 0f);
        p.items.put(6, mh);
        assertEquals(0, SpellEngine.sealOfRighteousnessDamage(p, engine.info(SpellEngine.SEAL_OF_RIGHTEOUSNESS)));
    }

    @Test
    void sealOfRighteousnessDamageWhenHolySpellPowerShouldAddCoeff() {
        Item mh = new Item(4, 25);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.applyWeaponLine(13, 2000, 10f, 10f);
        p.items.put(4, mh);
        p.setInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + 1, 50);
        int dmg = SpellEngine.sealOfRighteousnessDamage(p, engine.info(SpellEngine.SEAL_OF_RIGHTEOUSNESS));
        // Base trunc 1 + 50 * 0.092 * 2.0 = 1 + 9.2 → trunc 10
        assertEquals(10, dmg);
    }

    @Test
    void sealOfRighteousnessDamageWhenTwoHandHolySpellPowerShouldUseHigherCoeff() {
        Item mh = new Item(7, 25);
        mh.slot = Player.EQUIPMENT_SLOT_MAINHAND;
        mh.applyWeaponLine(SpellEngine.INVTYPE_2HWEAPON, 2000, 10f, 10f);
        p.items.put(7, mh);
        p.setInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + 1, 50);
        int dmg = SpellEngine.sealOfRighteousnessDamage(p, engine.info(SpellEngine.SEAL_OF_RIGHTEOUSNESS));
        // 2H base + 50 * 0.108 * 2.0 → trunc 15
        assertEquals(15, dmg);
    }

    @Test
    void procMeleeWhenMissShouldNotProcSeal() {
        p.spells.add(SpellEngine.SEAL_OF_RIGHTEOUSNESS);
        engine.cast(p, map, 0, SpellEngine.SEAL_OF_RIGHTEOUSNESS, 1, empty(), this::capture);
        ops.clear();
        engine.procMelee(p, c, true, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
    }

    @Test
    void procMeleeWhenProcTriggerSpellShouldCastTrigger() {
        engine.putTemplate(324, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_PROC_TRIGGER_SPELL, 0, 0, 0, 0, 0f,
                0, 0, 0, 30_000, 0, 0, 0, 0, SpellEngine.EFFECT_ENERGIZE, 0, 1, 1,
                SpellEngine.PROC_FLAG_DEAL_MELEE_SWING, 13897);
        engine.putTemplate(13897, SpellEngine.EFFECT_SCHOOL_DAMAGE, 0, 4, 0, 40, 40, 0f,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        p.spells.add(324);
        c.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 200);
        c.setHealth(80);
        engine.cast(p, map, 0, 324, 1, empty(), this::capture);
        ops.clear();
        engine.procMelee(p, c, false, this::capture);
        WowBuffer log = new WowBuffer(last.get(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        log.getPackedGuid();
        log.getPackedGuid();
        assertEquals(13897, log.getU32());
        assertEquals(40, log.getU32());
        assertEquals(40, c.health());
    }

    @Test
    void procMeleeWhenAuraProcFlagMissesMeleeShouldNotFire() {
        engine.putTemplate(20549, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_PROC_TRIGGER_SPELL, 0, 0, 0, 0, 0f,
                0, 0, 0, 30_000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13897);
        p.spells.add(20549);
        engine.cast(p, map, 0, 20549, 1, empty(), this::capture);
        engine.putTemplate(20580, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_PROC_TRIGGER_SPELL, 0, 0, 0, 0, 0f,
                0, 0, 0, 30_000, 0, 0, 0, 0, 0, 0, 0, 0, SpellEngine.PROC_FLAG_DEAL_MELEE_SWING, 1);
        p.spells.add(20580);
        engine.cast(p, map, 0, 20580, 1, empty(), this::capture);
        p.auras.add(new org.tbc.world.entity.Unit.Aura(9, 1000, 1));
        ops.clear();
        engine.procMelee(p, c, false, this::capture);
        assertFalse(ops.contains(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
    }

    @Test
    void createBarSpellsShouldBeInTheCatalog() {
        int[] bar = {20154, 635, 1752, 2098, 2764, 585, 686, 687, 2973, 75, 403, 331,
                20580, 5176, 5185, 20549, 28734, 28730, 25046, 28880};
        for (int id : bar) {
            assertTrue(engine.info(id) != null, "missing spell " + id);
        }
        assertTrue(engine.info(6603) == null);
    }

    private int sealCount() {
        int n = 0;
        for (var aura : p.auras) {
            if (aura.spellId() == SpellEngine.SEAL_OF_RIGHTEOUSNESS) {
                n++;
            }
        }
        return n;
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
