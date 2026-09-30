package org.tbc.world.spell;

import org.tbc.world.entity.Player;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TP-SL26-096 — SPELL_EFFECT_APPLY_AREA_AURA_PARTY (35). Devotion Aura 465.
 * CMaNGOS EffectApplyAreaAura: CreateAura + MOD_RESISTANCE; DurationIndex −1 permanent.
 */
class SpellEngineApplyAreaAuraPartyTest {
    private static SpellEngine.SpellInfo devotionAura() {
        // spell_template 465: EffectBasePoints+1 = 55 armor, misc school mask bit 0, duration permanent.
        return new SpellEngine.SpellInfo(
                SpellEngine.DEVOTION_AURA,
                SpellEngine.EFFECT_APPLY_AREA_AURA_PARTY,
                SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                0, 0, 55, 55, 0f, 1);
    }

    @Test
    void applyAreaAuraPartyWhenLivingShouldAddAura() {
        SpellEngine eng = new SpellEngine();
        assertTrue(eng.knownEffect(SpellEngine.EFFECT_APPLY_AREA_AURA_PARTY));
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 40);
        eng.apply(p, p, devotionAura(), 1_000L);
        assertEquals(1, p.auras.size());
        assertEquals(SpellEngine.DEVOTION_AURA, p.auras.get(0).spellId());
        assertEquals(0, p.auras.get(0).expireAtMs());
        assertEquals(0, AuraSlots.slotOf(p, SpellEngine.DEVOTION_AURA));
        assertEquals(SpellEngine.DEVOTION_AURA, p.getInt(UpdateFields.UNIT_FIELD_AURA));
        assertEquals(95, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(55, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void applyAreaAuraPartyWhenPermanentShouldSurviveExpirePulse() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        eng.apply(p, p, devotionAura(), 1_000L);
        assertEquals(0, p.auras.get(0).expireAtMs());
        List<Integer> expired = new ArrayList<>();
        AuraSlots.expireTimed(p, 1_000L + 60_000L, (op, pay) -> {}, expired::add);
        assertTrue(expired.isEmpty());
        assertEquals(1, p.auras.size());
        assertEquals(SpellEngine.DEVOTION_AURA, p.getInt(UpdateFields.UNIT_FIELD_AURA));
    }

    @Test
    void castWhenDevotionAuraShouldSendAuraAndResistanceValues() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.guid = 1;
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 40);
        p.setInt(UpdateFields.UNIT_FIELD_POWER1, 200);
        p.setPower(200);
        p.spells.add(SpellEngine.DEVOTION_AURA);
        GameMap map = new GameMap(0, 0);
        map.add(p);
        Set<Integer> ops = new HashSet<>();
        List<byte[]> values = new ArrayList<>();
        eng.cast(p, map, 0, SpellEngine.DEVOTION_AURA, 1, unitTarget(p.guid), (op, pay) -> {
            ops.add(op);
            if (op == Opcodes.SMSG_UPDATE_OBJECT || op == Opcodes.SMSG_COMPRESSED_UPDATE_OBJECT) {
                values.add(pay);
            }
        });
        assertEquals(95, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
        assertEquals(SpellEngine.DEVOTION_AURA, p.getInt(UpdateFields.UNIT_FIELD_AURA));
        assertTrue(ops.contains(Opcodes.SMSG_SPELL_GO));
        assertTrue(values.size() >= 2);
    }

    @Test
    void applyAreaAuraPartyWhenDeadOrMissingShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        Player dead = new Player();
        dead.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        dead.setHealth(0);
        eng.apply(dead, dead, devotionAura());
        assertEquals(0, dead.auras.size());
        Player live = new Player();
        live.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        live.setHealth(100);
        eng.applyAreaAuraParty(null, SpellEngine.DEVOTION_AURA);
        eng.applyAreaAuraParty(dead, SpellEngine.DEVOTION_AURA);
        eng.applyAreaAuraParty(live, 0);
        eng.applyAreaAuraParty(live, 999_999);
        eng.applyAreaAuraParty(live, SpellEngine.FIREBALL);
        assertEquals(0, live.auras.size());
    }

    @Test
    void applyAreaAuraPartyWhenCatalogSpellShouldApplyOnLiving() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 40);
        eng.applyAreaAuraParty(p, SpellEngine.DEVOTION_AURA);
        assertEquals(1, p.auras.size());
        assertEquals(95, p.getInt(UpdateFields.UNIT_FIELD_RESISTANCES));
    }

    @Test
    void areaAuraPermanentWhenGuardsOrTimedShouldBeFalse() {
        assertTrue(!SpellEngine.areaAuraPermanent(null));
        SpellEngine.SpellInfo timed = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_APPLY_AREA_AURA_PARTY, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                0, 0, 55, 55, 0f, 1).withDuration(30_000);
        assertTrue(!SpellEngine.areaAuraPermanent(timed));
        SpellEngine.SpellInfo amp = new SpellEngine.SpellInfo(
                1, SpellEngine.EFFECT_APPLY_AREA_AURA_PARTY, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                0, 0, 55, 55, 0f, 1).withAmplitude(3_000);
        assertTrue(!SpellEngine.areaAuraPermanent(amp));
        assertTrue(SpellEngine.areaAuraPermanent(devotionAura()));
    }

    @Test
    void applyAreaAuraPartyWhenRecastShouldRefreshPermanentHolder() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        eng.apply(p, p, devotionAura(), 1_000L);
        eng.apply(p, p, devotionAura(), 5_000L);
        assertEquals(1, p.auras.size());
        assertEquals(0, p.auras.get(0).expireAtMs());
        assertEquals(SpellEngine.DEVOTION_AURA, p.getInt(UpdateFields.UNIT_FIELD_AURA));
    }

    @Test
    void applyAreaAuraPartyWhenTimedDurationShouldExpire() {
        SpellEngine eng = new SpellEngine();
        Player p = new Player();
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        SpellEngine.SpellInfo timed = new SpellEngine.SpellInfo(
                900_465, SpellEngine.EFFECT_APPLY_AREA_AURA_PARTY, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                0, 0, 10, 10, 0f, 1).withDuration(5_000);
        eng.apply(p, p, timed, 1_000L);
        assertEquals(1_000L + 5_000L, p.auras.get(0).expireAtMs());
        AuraSlots.expireTimed(p, 1_000L + 5_000L, (op, pay) -> {});
        assertEquals(0, p.auras.size());
    }

    private static org.tbc.common.WowBuffer unitTarget(long guid) {
        org.tbc.common.WowBuffer b = new org.tbc.common.WowBuffer(16);
        b.putU32(SpellCastTargets.UNIT);
        b.putPackedGuid(guid);
        return b;
    }
}
