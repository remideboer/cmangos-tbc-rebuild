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
 * SPELL_AURA_MOD_RESISTANCE / MOD_ATTACK_POWER sheet VALUES — Frost Armor, Battle Shout.
 * JaCoCo: every branch of {@link SpellEngine#sendAuraStatValues} / {@link SpellEngine#unapplyAura}.
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
    void sendUnapplyAuraValuesWhenGuardsOrFrostArmorShouldCoverBranches() {
        engine.sendUnapplyAuraValues(null, SpellEngine.FROST_ARMOR, this::capture);
        engine.sendUnapplyAuraValues(p, 0, this::capture);
        engine.sendUnapplyAuraValues(p, -1, this::capture);
        engine.sendUnapplyAuraValues(p, SpellEngine.FROST_ARMOR, null);
        engine.sendUnapplyAuraValues(p, 999_999, this::capture);
        assertTrue(valuesPayloads.isEmpty());
        engine.apply(p, p, engine.info(SpellEngine.FROST_ARMOR), 1L);
        valuesPayloads.clear();
        ops.clear();
        engine.unapplyAura(p, SpellEngine.FROST_ARMOR);
        engine.sendUnapplyAuraValues(p, SpellEngine.FROST_ARMOR, this::capture);
        assertTrue(valuesPayloads.size() >= 1);
    }

    @Test
    void sendUnapplyAuraValuesWhenMultiEffectShouldSendExtras() {
        engine.putTemplate(900_171, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                16, 0, 30, 30, 0f, 0, 0, 0, SpellEngine.FROST_ARMOR_DURATION_MS,
                SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE, 5, 5,
                0, 0, 0, 0, 0, 0, 1, 1 << 1, 0);
        valuesPayloads.clear();
        engine.sendUnapplyAuraValues(p, 900_171, this::capture);
        // Primary + extra Effect2 resistance → at least one VALUES (both have misc masks).
        assertTrue(valuesPayloads.size() >= 1);
    }

    @Test
    void sendAuraStatValuesWhenGuardsShouldNoOp() {
        SpellEngine.SpellInfo frost = engine.info(SpellEngine.FROST_ARMOR);
        SpellEngine.sendAuraStatValues(null, frost, this::capture);
        SpellEngine.sendAuraStatValues(p, null, this::capture);
        SpellEngine.sendAuraStatValues(p, frost, null);
        SpellEngine.sendAuraStatValues(p, engine.info(SpellEngine.FIREBALL), this::capture);
        SpellEngine.sendAuraStatValues(p,
                new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                        0, 0, 10, 10, 0f, 0), this::capture);
        SpellEngine.sendAuraStatValues(p,
                new SpellEngine.SpellInfo(1, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                        0, 0, 10, 10, 0f, 1 << 7), this::capture);
        assertTrue(valuesPayloads.isEmpty());
    }

    @Test
    void sendAuraStatValuesWhenNegativeAmountShouldUseNegativeBuffMod() {
        Unit u = new Player();
        SpellEngine.SpellInfo debuff = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_APPLY_AURA, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                0, 0, -10, -10, 0f, 1);
        engine.auras().apply(u, debuff);
        SpellEngine.sendAuraStatValues(u, debuff, this::capture);
        assertEquals(-10, u.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(-10, u.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSNEGATIVE));
        assertTrue(valuesPayloads.size() >= 1);
    }

    @Test
    void sendAuraStatValuesWhenAttackPowerShouldPushModsField() {
        SpellEngine.SpellInfo shout = engine.info(SpellEngine.BATTLE_SHOUT);
        engine.auras().apply(p, shout);
        assertEquals(305, p.getInt(UpdateFields.UNIT_FIELD_ATTACK_POWER_MODS));
        SpellEngine.sendAuraStatValues(p, shout, this::capture);
        assertTrue(valuesPayloads.size() >= 1);
    }

    @Test
    void sendAuraStatValuesWhenRangedAttackPowerShouldPushModsField() {
        SpellEngine.SpellInfo rap = engine.info(SpellEngine.ATTACK_POWER_RANGED_60);
        engine.auras().apply(p, rap);
        assertEquals(60, p.getInt(UpdateFields.UNIT_FIELD_RANGED_ATTACK_POWER_MODS));
        SpellEngine.sendAuraStatValues(p, rap, this::capture);
        assertTrue(valuesPayloads.size() >= 1);
    }

    @Test
    void sendAuraStatValuesWhenModStatStaminaShouldPushHealthPool() {
        p.applyClasslessCreateStats(20, 100, 20, 20, 20, 20, 20);
        SpellEngine.SpellInfo fort = new SpellEngine.SpellInfo(
                1243, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STAT,
                0, 0, 27, 27, 0f, 2);
        engine.auras().apply(p, fort);
        SpellEngine.sendAuraStatValues(p, fort, this::capture);
        assertTrue(valuesPayloads.size() >= 1);
        assertEquals(310, p.maxHealth());
    }

    @Test
    void sendAuraStatValuesWhenModStatIntellectShouldPushManaPool() {
        p.applyClasslessCreateStats(20, 100, 20, 20, 20, 20, 20);
        int manaBefore = p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1);
        SpellEngine.SpellInfo ai = new SpellEngine.SpellInfo(
                1459, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STAT,
                0, 0, 10, 10, 0f, 3);
        engine.auras().apply(p, ai);
        SpellEngine.sendAuraStatValues(p, ai, this::capture);
        assertTrue(valuesPayloads.size() >= 1);
        assertTrue(p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1) > manaBefore);
    }

    @Test
    void sendAuraStatValuesWhenModStatMiscOutOfRangeShouldNoOp() {
        SpellEngine.SpellInfo bad = new SpellEngine.SpellInfo(
                999010, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STAT,
                0, 0, 5, 5, 0f, 99);
        SpellEngine.sendAuraStatValues(p, bad, this::capture);
        assertTrue(valuesPayloads.isEmpty());
    }

    @Test
    void sendAuraStatValuesWhenModStatNegativeAmountShouldUseNegStat() {
        p.applyClasslessCreateStats(20, 100, 20, 20, 20, 20, 20);
        SpellEngine.SpellInfo curse = new SpellEngine.SpellInfo(
                999011, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STAT,
                0, 0, -5, -5, 0f, 2);
        engine.auras().apply(p, curse);
        SpellEngine.sendAuraStatValues(p, curse, this::capture);
        assertTrue(valuesPayloads.size() >= 1);
    }

    @Test
    void sendAuraStatValuesWhenModStatAllStatsShouldPushHealthAndMana() {
        p.applyClasslessCreateStats(20, 100, 20, 20, 20, 20, 20);
        SpellEngine.SpellInfo all = new SpellEngine.SpellInfo(
                999012, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STAT,
                0, 0, 2, 2, 0f, -1);
        engine.auras().apply(p, all);
        SpellEngine.sendAuraStatValues(p, all, this::capture);
        assertTrue(valuesPayloads.size() >= 1);
        assertTrue(p.maxHealth() > 40);
        assertTrue(p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1) > 100);
    }

    @Test
    void sendAuraStatValuesWhenModStatStrengthOnlyShouldOmitHealthManaFields() {
        p.applyClasslessCreateStats(20, 100, 20, 20, 20, 20, 20);
        int maxHp = p.maxHealth();
        SpellEngine.SpellInfo str = new SpellEngine.SpellInfo(
                999013, SpellEngine.EFFECT_APPLY_AURA, AuraEngine.SPELL_AURA_MOD_STAT,
                0, 0, 5, 5, 0f, 0);
        engine.auras().apply(p, str);
        SpellEngine.sendAuraStatValues(p, str, this::capture);
        assertTrue(valuesPayloads.size() >= 1);
        assertEquals(maxHp, p.maxHealth());
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
