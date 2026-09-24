package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPELL_AURA_MOD_RESISTANCE wire + expire — Frost Armor raises armor and SMSG_UPDATEOBJECT values.
 * JaCoCo: every branch of {@link SpellEngine#sendResistanceStatValues} / {@link SpellEngine#unapplyAura}.
 */
class SpellEngineModResistanceTest {
    private SpellEngine engine;
    private Player p;
    private GameMap map;
    private final Set<Integer> ops = new HashSet<>();
    private final Map<Integer, byte[]> last = new HashMap<>();
    private final List<byte[]> valuesPayloads = new ArrayList<>();

    @BeforeEach
    void setUp() {
        engine = new SpellEngine();
        p = new Player();
        p.guid = 1;
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 40);
        map = new GameMap(0, 0);
        map.add(p);
        ops.clear();
        last.clear();
        valuesPayloads.clear();
    }

    @Test
    void applyWhenFrostArmorShouldRaiseArmorOnUnit() {
        engine.apply(p, p, engine.info(SpellEngine.FROST_ARMOR));
        assertEquals(70, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(30, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void castWhenFrostArmorShouldSendResistanceValues() {
        p.spells.add(SpellEngine.FROST_ARMOR);
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, 200);
        p.setPower(200);
        engine.cast(p, map, 0, SpellEngine.FROST_ARMOR, 1, unitTarget(p.guid), this::capture);
        assertEquals(70, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertTrue(ops.contains(Opcodes.SMSG_UPDATE_OBJECT) || ops.contains(Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT));
        assertTrue(valuesPayloads.size() >= 2);
    }

    @Test
    void castWhenAuraHasResistanceExtraShouldSendExtraValues() {
        engine.putTemplate(900_168, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                16, 0, 10, 10, 0f, 0, 0, 0, 30_000,
                SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE, 5, 5,
                0, 0, 0, 0, 0, 0, 1, 1 << 1, 0);
        p.spells.add(900_168);
        engine.cast(p, map, 0, 900_168, 1, unitTarget(p.guid), this::capture);
        assertEquals(50, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(5, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES + 1));
    }

    @Test
    void unapplyAuraWhenFrostArmorShouldRestoreArmor() {
        engine.apply(p, p, engine.info(SpellEngine.FROST_ARMOR));
        engine.unapplyAura(p, SpellEngine.FROST_ARMOR);
        assertEquals(40, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void unapplyAuraWhenNullTargetOrUnknownSpellShouldNoOp() {
        engine.unapplyAura(null, SpellEngine.FROST_ARMOR);
        engine.unapplyAura(p, 999_999);
        assertEquals(40, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    @Test
    void unapplyAuraWhenExtrasPresentShouldReverseExtras() {
        engine.putTemplate(900_169, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                16, 0, 10, 10, 0f, 0, 0, 0, 30_000,
                SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE, 5, 5,
                0, 0, 0, 0, 0, 0, 1, 1 << 1, 0);
        engine.apply(p, p, engine.info(900_169));
        for (SpellEngine.SpellInfo e : List.of(
                new SpellEngine.SpellInfo(900_169, SpellEngine.EFFECT_APPLY_AURA,
                        SpellEngine.SPELL_AURA_MOD_RESISTANCE, 16, 0, 5, 5, 0f, 1 << 1))) {
            engine.auras().apply(p, e);
        }
        assertEquals(50, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(5, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES + 1));
        engine.unapplyAura(p, 900_169);
        assertEquals(40, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(0, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES + 1));
    }

    @Test
    void sendResistanceStatValuesWhenGuardsShouldNoOp() {
        SpellEngine.SpellInfo frost = engine.info(SpellEngine.FROST_ARMOR);
        SpellEngine.sendResistanceStatValues(null, frost, this::capture);
        SpellEngine.sendResistanceStatValues(p, null, this::capture);
        SpellEngine.sendResistanceStatValues(p, frost, null);
        SpellEngine.sendResistanceStatValues(p, engine.info(SpellEngine.FIREBALL), this::capture);
        SpellEngine.sendResistanceStatValues(p,
                new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                        0, 0, 10, 10, 0f, 0), this::capture);
        SpellEngine.sendResistanceStatValues(p,
                new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                        0, 0, 10, 10, 0f, 1 << 7), this::capture);
        assertTrue(valuesPayloads.isEmpty());
    }

    @Test
    void sendResistanceStatValuesWhenNegativeAmountShouldUseNegativeBuffMod() {
        Unit u = new Player();
        SpellEngine.SpellInfo debuff = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                0, 0, -10, -10, 0f, 1);
        engine.auras().apply(u, debuff);
        SpellEngine.sendResistanceStatValues(u, debuff, this::capture);
        assertEquals(-10, u.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(-10, u.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSNEGATIVE));
        assertTrue(valuesPayloads.size() >= 1);
    }

    @Test
    void putTemplateWhenMiscZeroShouldKeepSeededFrostArmorSchoolMask() {
        engine.putTemplate(SpellEngine.FROST_ARMOR, SpellEngine.EFFECT_APPLY_AURA,
                SpellEngine.SPELL_AURA_MOD_RESISTANCE, 16, 60, 30, 30, 0f,
                0, SpellCooldowns.GCD_NORMAL_MS, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(1, engine.info(SpellEngine.FROST_ARMOR).misc());
        assertEquals(30, engine.info(SpellEngine.FROST_ARMOR).minDmg());
    }

    @Test
    void putTemplateWhenMiscPositiveShouldReplaceSeed() {
        engine.putTemplate(SpellEngine.FROST_ARMOR, SpellEngine.EFFECT_APPLY_AURA,
                SpellEngine.SPELL_AURA_MOD_RESISTANCE, 16, 60, 30, 30, 0f,
                0, SpellCooldowns.GCD_NORMAL_MS, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 << 2, 0, 0);
        assertEquals(1 << 2, engine.info(SpellEngine.FROST_ARMOR).misc());
    }

    @Test
    void putTemplateWhenMiscZeroAndNoSeedShouldStayZero() {
        engine.putTemplate(999_002, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                0, 0, 5, 5, 0f, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertEquals(0, engine.info(999_002).misc());
    }

    /**
     * Frost Armor (and similar) has Effect2 APPLY_AURA. A second holder used to get
     * {@code auraDurationMs} 30s fallback and, on expire, cleared the whole spell (30 min → 30 s).
     */
    @Test
    void castWhenMultiEffectAuraShouldKeepOneHolderForThirtyMinutes() {
        engine.putTemplate(900_170, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                16, 0, 30, 30, 0f, 0, 0, 0, SpellEngine.FROST_ARMOR_DURATION_MS,
                SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE, 5, 5,
                0, 0, 0, 0, 0, 0, 1, 1 << 1, 0);
        p.spells.add(900_170);
        engine.cast(p, map, 1_000L, 900_170, 1, unitTarget(p.guid), this::capture);
        assertEquals(1, p.auras.size());
        assertEquals(SpellEngine.FROST_ARMOR_DURATION_MS, p.auras.get(0).durationMs());
        assertEquals(1_000L + SpellEngine.FROST_ARMOR_DURATION_MS, p.auras.get(0).expireAtMs());
        AuraSlots.expireTimed(p, 1_000L + 30_000L, null);
        assertTrue(p.hasAura(900_170));
        AuraSlots.expireTimed(p, 1_000L + SpellEngine.FROST_ARMOR_DURATION_MS, null);
        assertFalse(p.hasAura(900_170));
    }

    /** Re-cast refreshes expire on the single holder; other spell ids in the list are skipped. */
    @Test
    void applyWhenSameAuraRecastShouldRefreshExpireOnExistingHolder() {
        p.auras.add(new Unit.Aura(999, 1_000, 1));
        engine.apply(p, p, engine.info(SpellEngine.FROST_ARMOR), 1_000L);
        engine.apply(p, p, engine.info(SpellEngine.FROST_ARMOR), 5_000L);
        assertEquals(1, p.auras.stream().filter(a -> a.spellId() == SpellEngine.FROST_ARMOR).count());
        Unit.Aura frost = p.auras.stream()
                .filter(a -> a.spellId() == SpellEngine.FROST_ARMOR)
                .findFirst()
                .orElseThrow();
        assertEquals(5_000L + SpellEngine.FROST_ARMOR_DURATION_MS, frost.expireAtMs());
    }

    private void capture(int op, byte[] payload) {
        ops.add(op);
        last.put(op, payload);
        if (op == Opcodes.SMSG_UPDATE_OBJECT || op == Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT) {
            valuesPayloads.add(payload);
        }
    }

    private static org.tbc.common.WowBuffer unitTarget(long guid) {
        org.tbc.common.WowBuffer b = new org.tbc.common.WowBuffer(16);
        b.putU32(SpellCastTargets.UNIT);
        b.putPackedGuid(guid);
        return b;
    }
}
