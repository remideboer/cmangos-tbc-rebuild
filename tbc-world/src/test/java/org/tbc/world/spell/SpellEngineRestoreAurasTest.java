package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Player::_LoadAuras — reapply persisted holders after logout copy.
 * TP-SL07-017 domain coverage for {@link SpellEngine#restorePersistedAuras}.
 */
class SpellEngineRestoreAurasTest {

    @Test
    void restorePersistedAurasWhenNullShouldNoOp() {
        new SpellEngine().restorePersistedAuras(null, 1_000L);
    }

    @Test
    void restorePersistedAurasWhenFrostArmorShouldApplyResistanceAndSlot() {
        SpellEngine engine = new SpellEngine();
        Player p = new Player();
        p.guid = 1;
        p.level = 1;
        p.applyCreateFields();
        int armorBefore = p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES);
        p.auras.add(new Unit.Aura(SpellEngine.FROST_ARMOR, SpellEngine.FROST_ARMOR_DURATION_MS, 1, 0,
                60_000L, 0, 0, p.guid));
        engine.restorePersistedAuras(p, 1_000L);
        assertTrue(p.hasAura(SpellEngine.FROST_ARMOR));
        assertEquals(SpellEngine.FROST_ARMOR, p.getInt(UpdateFields.UNIT_FIELD_AURA));
        assertEquals(armorBefore + 30, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    @Test
    void restorePersistedAurasWhenExpiredShouldDropHolder() {
        SpellEngine engine = new SpellEngine();
        Player p = new Player();
        p.guid = 2;
        p.level = 1;
        p.auras.add(new Unit.Aura(SpellEngine.FROST_ARMOR, 1_000, 1, 0, 500L, 0, 0, p.guid));
        AuraSlots.applyVisible(p, SpellEngine.FROST_ARMOR, 1, 1);
        engine.restorePersistedAuras(p, 1_000L);
        assertFalse(p.hasAura(SpellEngine.FROST_ARMOR));
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_AURA));
    }

    @Test
    void restorePersistedAurasWhenExpiredWithoutSlotShouldDropHolder() {
        SpellEngine engine = new SpellEngine();
        Player p = new Player();
        p.guid = 4;
        p.auras.add(new Unit.Aura(SpellEngine.FROST_ARMOR, 1_000, 1, 0, 500L, 0, 0, p.guid));
        engine.restorePersistedAuras(p, 1_000L);
        assertFalse(p.hasAura(SpellEngine.FROST_ARMOR));
    }

    @Test
    void restorePersistedAurasWhenUnknownSpellShouldKeepHolderAndSlot() {
        SpellEngine engine = new SpellEngine();
        Player p = new Player();
        p.guid = 3;
        p.level = 1;
        p.auras.add(new Unit.Aura(999_001, 0, 1));
        engine.restorePersistedAuras(p, 0L);
        assertTrue(p.hasAura(999_001));
        assertEquals(999_001, p.getInt(UpdateFields.UNIT_FIELD_AURA));
    }

    @Test
    void restorePersistedAurasWhenSlotAlreadyVisibleShouldNotDuplicate() {
        SpellEngine engine = new SpellEngine();
        Player p = new Player();
        p.guid = 5;
        p.level = 1;
        p.applyCreateFields();
        p.auras.add(new Unit.Aura(SpellEngine.FROST_ARMOR, SpellEngine.FROST_ARMOR_DURATION_MS, 1, 0,
                60_000L, 0, 0, p.guid));
        AuraSlots.applyVisible(p, SpellEngine.FROST_ARMOR, 1, 1);
        engine.restorePersistedAuras(p, 1_000L);
        assertEquals(SpellEngine.FROST_ARMOR, p.getInt(UpdateFields.UNIT_FIELD_AURA));
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_AURA + 1), "no duplicate slot");
    }

    @Test
    void restorePersistedAurasWhenExtraEffectShouldApplyExtra() {
        SpellEngine engine = new SpellEngine();
        // Primary armor + holy resist extra (same pattern as SpellEngineModResistanceTest).
        engine.putTemplate(900_168, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                16, 0, 10, 10, 0f, 0, 0, 0, 30_000,
                SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE, 5, 5,
                0, 0, 0, 0, 0, 0, 1, 1 << 1, 0);
        Player p = new Player();
        p.guid = 6;
        p.level = 1;
        p.applyCreateFields();
        int armorBefore = p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES);
        int holyBefore = p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES + 1);
        p.auras.add(new Unit.Aura(900_168, 30_000, 1, 0, 60_000L, 0, 0, p.guid));
        engine.restorePersistedAuras(p, 1_000L);
        assertEquals(armorBefore + 10, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(holyBefore + 5, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES + 1));
    }

    @Test
    void restorePersistedAurasWhenNowMsZeroShouldKeepTimedHolder() {
        SpellEngine engine = new SpellEngine();
        Player p = new Player();
        p.guid = 7;
        p.level = 1;
        // expireAt in the past, but nowMs 0 means login clock not set — keep.
        p.auras.add(new Unit.Aura(SpellEngine.FROST_ARMOR, 1_000, 1, 0, 500L, 0, 0, p.guid));
        engine.restorePersistedAuras(p, 0L);
        assertTrue(p.hasAura(SpellEngine.FROST_ARMOR));
    }

    @Test
    void restorePersistedAurasWhenTimedMissingExpireShouldHealFromDuration() {
        SpellEngine engine = new SpellEngine();
        Player p = new Player();
        p.guid = 8;
        p.level = 1;
        p.applyCreateFields();
        p.auras.add(new Unit.Aura(SpellEngine.FROST_ARMOR, SpellEngine.FROST_ARMOR_DURATION_MS, 1, 0,
                0L, 0, 0, p.guid));
        engine.restorePersistedAuras(p, 10_000L);
        Unit.Aura a = p.auras.stream().filter(x -> x.spellId() == SpellEngine.FROST_ARMOR).findFirst().orElseThrow();
        assertEquals(10_000L + SpellEngine.FROST_ARMOR_DURATION_MS, a.expireAtMs());
    }

    @Test
    void restorePersistedAurasWhenTimedMissingExpireAndAmplitudeShouldScheduleTick() {
        SpellEngine engine = new SpellEngine();
        Player p = new Player();
        p.guid = 12;
        p.level = 1;
        p.applyCreateFields();
        p.auras.add(new Unit.Aura(SpellEngine.FROST_ARMOR, SpellEngine.FROST_ARMOR_DURATION_MS, 1, 0,
                0L, 3_000, 0, p.guid));
        engine.restorePersistedAuras(p, 10_000L);
        Unit.Aura a = p.auras.stream().filter(x -> x.spellId() == SpellEngine.FROST_ARMOR).findFirst().orElseThrow();
        assertEquals(13_000L, a.nextTickAtMs());
    }

    @Test
    void restorePersistedAurasWhenTimedMissingExpireButNowMsZeroShouldNotHeal() {
        SpellEngine engine = new SpellEngine();
        Player p = new Player();
        p.guid = 13;
        p.auras.add(new Unit.Aura(SpellEngine.FROST_ARMOR, SpellEngine.FROST_ARMOR_DURATION_MS, 1, 0,
                0L, 0, 0, p.guid));
        engine.restorePersistedAuras(p, 0L);
        Unit.Aura a = p.auras.stream().filter(x -> x.spellId() == SpellEngine.FROST_ARMOR).findFirst().orElseThrow();
        assertEquals(0L, a.expireAtMs(), "nowMs 0 leaves expire unset");
    }

    @Test
    void sendPersistedAuraDurationsWhenTimedShouldSendRemainMs() {
        SpellEngine engine = new SpellEngine();
        Player p = new Player();
        p.guid = 9;
        p.level = 1;
        p.applyCreateFields();
        AuraSlots.applyVisible(p, SpellEngine.FROST_ARMOR, 1, 1);
        p.auras.add(new Unit.Aura(SpellEngine.FROST_ARMOR, SpellEngine.FROST_ARMOR_DURATION_MS, 1, 0,
                60_000L, 0, 0, p.guid));
        java.util.ArrayList<Integer> ops = new java.util.ArrayList<>();
        java.util.concurrent.atomic.AtomicReference<byte[]> dur = new java.util.concurrent.atomic.AtomicReference<>();
        engine.sendPersistedAuraDurations(p, 10_000L, (op, payload) -> {
            ops.add(op);
            if (op == Opcodes.SMSG_UPDATE_AURA_DURATION) {
                dur.set(payload);
            }
        });
        assertTrue(ops.contains(Opcodes.SMSG_UPDATE_AURA_DURATION));
        assertEquals(0, dur.get()[0] & 0xFF);
        assertEquals(50_000, u32le(dur.get(), 1));
    }

    @Test
    void sendPersistedAuraDurationsWhenNullOrPermanentShouldNoOp() {
        SpellEngine engine = new SpellEngine();
        engine.sendPersistedAuraDurations(null, 1_000L, (op, payload) -> {
            throw new AssertionError("no send");
        });
        Player p = new Player();
        p.guid = 10;
        p.auras.add(new Unit.Aura(SpellEngine.FROST_ARMOR, 0, 1, 0, 0L, 0, 0, p.guid));
        engine.sendPersistedAuraDurations(p, 1_000L, (op, payload) -> {
            throw new AssertionError("permanent skip");
        });
        engine.sendPersistedAuraDurations(p, 0L, (op, payload) -> {
            throw new AssertionError("nowMs 0 skip");
        });
        p.auras.clear();
        AuraSlots.applyVisible(p, SpellEngine.FROST_ARMOR, 1, 1);
        p.auras.add(new Unit.Aura(SpellEngine.FROST_ARMOR, 1_000, 1, 0, 500L, 0, 0, p.guid));
        engine.sendPersistedAuraDurations(p, 1_000L, (op, payload) -> {
            throw new AssertionError("already expired skip");
        });
        org.tbc.world.entity.Creature c = new org.tbc.world.entity.Creature();
        c.auras.add(new Unit.Aura(SpellEngine.FROST_ARMOR, 1_000, 1, 0, 60_000L, 0, 0, 0));
        engine.sendPersistedAuraDurations(c, 1_000L, (op, payload) -> {
            throw new AssertionError("creature skip");
        });
        engine.sendPersistedAuraDurations(p, 1_000L, null);
    }

    @Test
    void sendPersistedAuraDurationsWhenDurationZeroShouldUseRemainAsMax() {
        SpellEngine engine = new SpellEngine();
        Player p = new Player();
        p.guid = 11;
        p.level = 1;
        AuraSlots.applyVisible(p, SpellEngine.FROST_ARMOR, 1, 1);
        p.auras.add(new Unit.Aura(SpellEngine.FROST_ARMOR, 0, 1, 0, 60_000L, 0, 0, p.guid));
        java.util.concurrent.atomic.AtomicReference<byte[]> dur = new java.util.concurrent.atomic.AtomicReference<>();
        engine.sendPersistedAuraDurations(p, 10_000L, (op, payload) -> {
            if (op == Opcodes.SMSG_UPDATE_AURA_DURATION) {
                dur.set(payload);
            }
        });
        assertEquals(50_000, u32le(dur.get(), 1));
    }

    private static int u32le(byte[] b, int off) {
        return (b[off] & 0xFF) | ((b[off + 1] & 0xFF) << 8)
                | ((b[off + 2] & 0xFF) << 16) | ((b[off + 3] & 0xFF) << 24);
    }
}
