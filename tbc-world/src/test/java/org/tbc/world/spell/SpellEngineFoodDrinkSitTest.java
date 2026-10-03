package org.tbc.world.spell;

import org.junit.jupiter.api.Test;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL07-022 — Food/drink STANDING_CANCELS: sit on apply; leave seated drops aura (CMaNGOS).
 */
class SpellEngineFoodDrinkSitTest {

    @Test
    void applyFoodWhenStandingShouldSit() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        assertTrue(p.isStanding());
        eng.apply(p, p, eng.info(SpellEngine.SPELL_FOOD));
        assertEquals(Unit.UNIT_STAND_STATE_SIT, p.standState());
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == SpellEngine.SPELL_FOOD));
    }

    @Test
    void applyDrinkWhenStandingShouldSit() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_DRINK));
        assertEquals(Unit.UNIT_STAND_STATE_SIT, p.standState());
    }

    @Test
    void standWhenEatingShouldDropFoodAura() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.leaveSeatedAuras = u -> eng.removeAurasWithInterruptFlags(
                u, SpellEngine.AURA_INTERRUPT_FLAG_STANDING_CANCELS, null);
        eng.apply(p, p, eng.info(SpellEngine.SPELL_FOOD));
        assertTrue(p.auras.stream().anyMatch(a -> a.spellId() == SpellEngine.SPELL_FOOD));
        p.stand();
        assertFalse(p.auras.stream().anyMatch(a -> a.spellId() == SpellEngine.SPELL_FOOD));
        assertTrue(p.isStanding());
    }

    @Test
    void auraInterruptFlagsWhenFoodDrinkShouldStandingCancel() {
        SpellEngine eng = new SpellEngine();
        assertTrue((eng.auraInterruptFlags(SpellEngine.SPELL_FOOD)
                & SpellEngine.AURA_INTERRUPT_FLAG_STANDING_CANCELS) != 0);
        assertTrue((eng.auraInterruptFlags(SpellEngine.SPELL_DRINK)
                & SpellEngine.AURA_INTERRUPT_FLAG_STANDING_CANCELS) != 0);
    }

    @Test
    void applyFoodWhenAlreadySittingShouldKeepSit() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.sit();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_FOOD));
        assertEquals(Unit.UNIT_STAND_STATE_SIT, p.standState());
    }

    @Test
    void applyStealthShouldNotSit() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_STEALTH));
        assertTrue(p.isStanding());
    }

    @Test
    void regenGainWhenPartialPeriodShouldScaleAndRejectNonPositive() {
        assertEquals(0, SpellEngine.regenGain(0, 5_000, 2_000));
        assertEquals(0, SpellEngine.regenGain(17, 0, 2_000));
        assertEquals(0, SpellEngine.regenGain(17, 5_000, 0));
        assertEquals(6, SpellEngine.regenGain(17, 5_000, 2_000));
        assertEquals(17, SpellEngine.regenGain(17, 5_000, 5_000));
    }

    @Test
    void applyFoodWhenTimedShouldCreditFromNowNotTheFullPeriod() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        eng.apply(p, p, eng.info(SpellEngine.SPELL_FOOD), 1_000);
        assertEquals(1_000L, p.auras.get(0).nextTickAtMs());
        eng.apply(p, p, eng.info(30108), 1_000);
        assertEquals(1_000L + SpellEngine.UA_AMPLITUDE_MS, p.auras.get(p.auras.size() - 1).nextTickAtMs());
        assertTrue(eng.eatDrinkRegen(eng.info(SpellEngine.SPELL_FOOD)));
        assertTrue(eng.eatDrinkRegen(eng.info(SpellEngine.SPELL_DRINK)));
        assertFalse(eng.eatDrinkRegen(eng.info(SpellEngine.SPELL_STEALTH)));
        assertFalse(eng.eatDrinkRegen(null));
        assertFalse(eng.eatDrinkRegen(new SpellEngine.SpellInfo(
                99999, 6, AuraEngine.SPELL_AURA_MOD_REGEN, 0, 0, 17, 17, 0f, 0)));
    }

    @Test
    void accrueEatDrinkWhenPartialShouldHealAndIgnoreEdges() {
        SpellEngine eng = new SpellEngine();
        Player p = standingPlayer();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 200);
        p.setHealth(40);
        eng.apply(p, p, eng.info(SpellEngine.SPELL_FOOD), 1_000);
        p.auras.add(0, new Unit.Aura(1, 1, 1, 0, 0, 0, 0, 0));
        Unit.Aura food = p.auras.get(1);
        assertTrue(eng.accrueEatDrink(null, food, eng.info(SpellEngine.SPELL_FOOD), 3_000, (op, payload) -> { }));
        assertTrue(eng.accrueEatDrink(p, null, eng.info(SpellEngine.SPELL_FOOD), 3_000, (op, payload) -> { }));
        assertTrue(eng.accrueEatDrink(p, food, null, 3_000, (op, payload) -> { }));
        p.setHealth(0);
        assertTrue(eng.accrueEatDrink(p, food, eng.info(SpellEngine.SPELL_FOOD), 3_000, (op, payload) -> { }));
        p.setHealth(40);
        assertTrue(eng.accrueEatDrink(p, food, eng.info(SpellEngine.SPELL_FOOD), 1_000, null));
        assertEquals(40, p.health());
        Unit.Aura missing = new Unit.Aura(SpellEngine.SPELL_FOOD, 18_000, 1, 0, 0, 5_000, 50, 1);
        assertTrue(eng.accrueEatDrink(p, missing, eng.info(SpellEngine.SPELL_FOOD), 3_000, null));
        assertTrue(eng.accrueEatDrink(p, food, eng.info(SpellEngine.SPELL_FOOD), 3_000, null));
        assertEquals(46, p.health());
        food = p.auras.get(1);
        assertTrue(eng.accrueEatDrink(p, food, eng.info(SpellEngine.SPELL_FOOD), 6_000, (op, payload) -> { }));
        assertEquals(57, p.health());

        Player quiet = standingPlayer();
        quiet.guid = 3;
        quiet.setHealth(40);
        quiet.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        quiet.setPower(10);
        quiet.powerType = 0;
        eng.apply(quiet, quiet, eng.info(SpellEngine.SPELL_DRINK), 1_000);
        assertTrue(eng.accrueEatDrink(quiet, quiet.auras.get(0), eng.info(SpellEngine.SPELL_DRINK), 6_000, null));
        assertEquals(52, quiet.power());

        Player drinker = standingPlayer();
        drinker.guid = 2;
        drinker.setHealth(40);
        drinker.setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, 200);
        drinker.setPower(10);
        drinker.powerType = 0;
        eng.apply(drinker, drinker, eng.info(SpellEngine.SPELL_DRINK), 1_000);
        Unit.Aura drink = drinker.auras.get(0);
        drinker.powerType = 1;
        assertTrue(eng.accrueEatDrink(drinker, drink, eng.info(SpellEngine.SPELL_DRINK), 6_000, null));
        assertEquals(10, drinker.getInt(UpdateFields.UNIT_FIELD_POWER1));
        org.tbc.world.entity.Creature mob = new org.tbc.world.entity.Creature();
        mob.setHealth(40);
        mob.auras.add(drink);
        assertTrue(eng.accrueEatDrink(mob, drink, eng.info(SpellEngine.SPELL_DRINK), 6_000, null));
        drinker.powerType = 0;
        assertTrue(eng.accrueEatDrink(drinker, drink, eng.info(SpellEngine.SPELL_DRINK), 6_000,
                (op, payload) -> { }));
        assertEquals(52, drinker.power());
    }

    /** TP-SL07-022 — food from standing publishes UNIT_FIELD_BYTES_1 sit (CMaNGOS SetStandState). */
    @Test
    void castFoodWhenStandingShouldSendSitBytes() {
        SpellEngine eng = new SpellEngine();
        Player p = standingPlayer();
        List<Captured> sent = new ArrayList<>();
        assertTrue(eng.castFromItem(p, new GameMap(0, 0), 10, SpellEngine.SPELL_FOOD, 1,
                new WowBuffer(new byte[0]), (op, payload) -> sent.add(new Captured(op, payload))));
        assertEquals(Unit.UNIT_STAND_STATE_SIT, sitByte(sent, p.guid));
    }

    /** TP-SL07-022 — drink uses the same STANDING_CANCELS sit update. */
    @Test
    void castDrinkWhenStandingShouldSendSitBytes() {
        SpellEngine eng = new SpellEngine();
        Player p = standingPlayer();
        List<Captured> sent = new ArrayList<>();
        assertTrue(eng.castFromItem(p, new GameMap(0, 0), 10, SpellEngine.SPELL_DRINK, 1,
                new WowBuffer(new byte[0]), (op, payload) -> sent.add(new Captured(op, payload))));
        assertEquals(Unit.UNIT_STAND_STATE_SIT, sitByte(sent, p.guid));
    }

    /** Already seated: SetStandState is a no-op, so no fresh BYTES_1 update. */
    @Test
    void castFoodWhenAlreadySittingShouldNotSendSitBytes() {
        SpellEngine eng = new SpellEngine();
        Player p = standingPlayer();
        p.sit();
        List<Captured> sent = new ArrayList<>();
        assertTrue(eng.castFromItem(p, new GameMap(0, 0), 10, SpellEngine.SPELL_FOOD, 1,
                new WowBuffer(new byte[0]), (op, payload) -> sent.add(new Captured(op, payload))));
        assertNull(sitByte(sent, p.guid));
    }

    /** An apply-aura without STANDING_CANCELS must not publish a sit byte. */
    @Test
    void castStealthWhenStandingShouldNotSendSitBytes() {
        SpellEngine eng = new SpellEngine();
        Player p = standingPlayer();
        p.spells.add(SpellEngine.SPELL_STEALTH);
        List<Captured> sent = new ArrayList<>();
        assertTrue(eng.cast(p, new GameMap(0, 0), 10, SpellEngine.SPELL_STEALTH, 1,
                new WowBuffer(new byte[0]), (op, payload) -> sent.add(new Captured(op, payload)), () -> { }, false));
        assertTrue(p.isStanding());
        assertNull(sitByte(sent, p.guid));
    }

    private static Player standingPlayer() {
        Player p = new Player();
        p.guid = 1;
        p.relocate(0, 0, 0, 0);
        return p;
    }

    private record Captured(int opcode, byte[] payload) {}

    /** Low byte of UNIT_FIELD_BYTES_1 from the latest VALUES block that carries it, or null. */
    private static Integer sitByte(List<Captured> sent, long guid) {
        Integer found = null;
        for (Captured c : sent) {
            Integer v = decodeValuesField(inflate(c.opcode, c.payload), guid, UpdateFields.UNIT_FIELD_BYTES_1);
            if (v != null) {
                found = v & 0xFF;
            }
        }
        return found;
    }

    private static byte[] inflate(int opcode, byte[] payload) {
        if (opcode == Opcodes.SMSG_UPDATE_OBJECT) {
            return payload;
        }
        if (opcode != Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT || payload == null || payload.length < 4) {
            return null;
        }
        Inflater inf = new Inflater();
        inf.setInput(payload, 4, payload.length - 4);
        byte[] out = new byte[u32le(payload, 0)];
        try {
            inf.inflate(out);
        } catch (DataFormatException e) {
            throw new IllegalStateException(e);
        } finally {
            inf.end();
        }
        return out;
    }

    private static int u32le(byte[] raw, int at) {
        return (raw[at] & 0xFF) | ((raw[at + 1] & 0xFF) << 8)
                | ((raw[at + 2] & 0xFF) << 16) | ((raw[at + 3] & 0xFF) << 24);
    }

    private static Integer decodeValuesField(byte[] raw, long guid, int field) {
        if (raw == null) {
            return null;
        }
        WowBuffer b = new WowBuffer(raw);
        b.getU32();
        b.getU8();
        if (b.remaining() == 0 || b.getU8() != UpdateBuilder.UPDATETYPE_VALUES) {
            return null;
        }
        if (b.getPackedGuid() != guid) {
            return null;
        }
        int nblocks = b.getU8();
        int[] mask = new int[nblocks];
        for (int k = 0; k < nblocks; k++) {
            mask[k] = b.getU32();
        }
        Integer value = null;
        for (int f = 0; f < nblocks * 32; f++) {
            if ((mask[f / 32] & (1 << (f % 32))) != 0) {
                int v = b.getU32();
                if (f == field) {
                    value = v;
                }
            }
        }
        return value;
    }
}
