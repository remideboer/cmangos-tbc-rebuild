package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
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
}
