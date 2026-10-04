package org.tbc.world.spell;

import org.tbc.world.entity.Creature;
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

    @Test
    void updatePartyAreaAurasWhenGroupedInRangeShouldApplyDevotionToAlly() {
        SpellEngine eng = new SpellEngine();
        Player paladin = groupedPaladin(1);
        Player ally = groupedAlly(2, paladin);
        paladin.relocate(0, 0, 0, 0);
        ally.relocate(5, 0, 0, 0);
        GameMap map = new GameMap(0, 0);
        map.add(paladin);
        map.add(ally);
        eng.apply(paladin, paladin, devotionAura(), 1_000L);
        eng.updatePartyAreaAuras(paladin, map);
        assertEquals(SpellEngine.DEVOTION_AURA, ally.getInt(UpdateFields.UNIT_FIELD_AURA));
        assertEquals(55, ally.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void updatePartyAreaAurasWhenAllyLeavesRangeShouldDropCopy() {
        SpellEngine eng = new SpellEngine();
        Player paladin = groupedPaladin(1);
        Player ally = groupedAlly(2, paladin);
        paladin.relocate(0, 0, 0, 0);
        ally.relocate(5, 0, 0, 0);
        GameMap map = new GameMap(0, 0);
        map.add(paladin);
        map.add(ally);
        eng.apply(paladin, paladin, devotionAura(), 1_000L);
        eng.updatePartyAreaAuras(paladin, map);
        assertEquals(SpellEngine.DEVOTION_AURA, ally.getInt(UpdateFields.UNIT_FIELD_AURA));
        ally.relocate(40, 0, 0, 0);
        eng.updatePartyAreaAuras(paladin, map);
        assertEquals(0, ally.getInt(UpdateFields.UNIT_FIELD_AURA));
        assertEquals(0, ally.auras.size());
        assertEquals(0, ally.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void updatePartyAreaAurasWhenGuardsShouldNoOp() {
        SpellEngine eng = new SpellEngine();
        eng.updatePartyAreaAuras(null, new GameMap(0, 0));
        Creature cr = new Creature();
        cr.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        cr.setHealth(100);
        eng.updatePartyAreaAuras(cr, new GameMap(0, 0));
        Player lone = groupedPaladin(1);
        eng.apply(lone, lone, devotionAura(), 1_000L);
        eng.updatePartyAreaAuras(lone, null);
        assertEquals(1, lone.auras.size());
        Player noGroup = new Player();
        noGroup.guid = 9;
        noGroup.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        noGroup.setHealth(100);
        eng.apply(noGroup, noGroup, devotionAura(), 1_000L);
        eng.updatePartyAreaAuras(noGroup, new GameMap(0, 0));
        assertEquals(1, noGroup.auras.size());
        Player paladin = groupedPaladin(4);
        Player ally = groupedAlly(5, paladin);
        paladin.relocate(0, 0, 0, 0);
        ally.relocate(2, 0, 0, 0);
        GameMap map = new GameMap(0, 0);
        map.add(paladin);
        map.add(ally);
        paladin.auras.add(new org.tbc.world.entity.Unit.Aura(99999, 0, 1, 0, 0, 0, 0, paladin.guid));
        eng.apply(paladin, paladin, eng.info(SpellEngine.FROST_ARMOR), 1_000L);
        eng.putTemplate(900_465, SpellEngine.EFFECT_APPLY_AREA_AURA_PARTY, SpellEngine.SPELL_AURA_MOD_RESISTANCE,
                0, 0, 1, 1, 0f, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0);
        paladin.auras.add(new org.tbc.world.entity.Unit.Aura(900_465, 0, 1, 0, 0, 0, 0, paladin.guid));
        eng.applyAreaAuraParty(paladin, SpellEngine.DEVOTION_AURA);
        ally.auras.add(new org.tbc.world.entity.Unit.Aura(99999, 0, 1, 0, 0, 0, 0, paladin.guid));
        ally.auras.add(new org.tbc.world.entity.Unit.Aura(SpellEngine.FROST_ARMOR, 0, 1, 0, 0, 0, 0, paladin.guid));
        ally.auras.add(new org.tbc.world.entity.Unit.Aura(SpellEngine.FROST_ARMOR, 0, 1, 0, 0, 0, 0, 88L));
        eng.updatePartyAreaAuras(paladin, map);
        assertEquals(SpellEngine.DEVOTION_AURA, ally.getInt(UpdateFields.UNIT_FIELD_AURA));
        ally.auras.add(new org.tbc.world.entity.Unit.Aura(SpellEngine.DEVOTION_AURA, 0, 1, 0, 0, 0, 0, 99L));
        eng.updatePartyAreaAuras(paladin, map);
        int slot = AuraSlots.slotOf(ally, SpellEngine.DEVOTION_AURA);
        if (slot >= 0) {
            AuraSlots.clearVisible(ally, slot);
        }
        ally.relocate(40, 0, 0, 0);
        eng.updatePartyAreaAuras(paladin, map);
        assertEquals(0, ally.getInt(UpdateFields.UNIT_FIELD_AURA));
    }

    @Test
    void updatePartyAreaAurasWhenUngroupedOrDeadOrSubgroupShouldNotApply() {
        SpellEngine eng = new SpellEngine();
        Player paladin = groupedPaladin(1);
        Player ally = groupedAlly(2, paladin);
        Player stranger = new Player();
        stranger.guid = 3;
        stranger.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        stranger.setHealth(100);
        paladin.relocate(0, 0, 0, 0);
        ally.relocate(1, 0, 0, 0);
        stranger.relocate(1, 0, 0, 0);
        GameMap map = new GameMap(0, 0);
        map.add(paladin);
        map.add(ally);
        map.add(stranger);
        eng.apply(paladin, paladin, devotionAura(), 1_000L);
        ally.setHealth(0);
        eng.updatePartyAreaAuras(paladin, map);
        assertEquals(0, ally.getInt(UpdateFields.UNIT_FIELD_AURA));
        ally.setHealth(100);
        paladin.group.subgroups.put(ally.guid, 1);
        eng.updatePartyAreaAuras(paladin, map);
        assertEquals(0, ally.getInt(UpdateFields.UNIT_FIELD_AURA));
        paladin.group.subgroups.put(ally.guid, 0);
        eng.updatePartyAreaAuras(paladin, map);
        assertEquals(SpellEngine.DEVOTION_AURA, ally.getInt(UpdateFields.UNIT_FIELD_AURA));
        assertEquals(0, stranger.getInt(UpdateFields.UNIT_FIELD_AURA));
        eng.updatePartyAreaAuras(paladin, map);
        assertEquals(55, ally.getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE));
    }

    @Test
    void updatePartyAreaAurasWhenCasterDropsAuraShouldDropAllyCopy() {
        SpellEngine eng = new SpellEngine();
        Player paladin = groupedPaladin(1);
        Player ally = groupedAlly(2, paladin);
        paladin.relocate(0, 0, 0, 0);
        ally.relocate(2, 0, 0, 0);
        GameMap map = new GameMap(0, 0);
        map.add(paladin);
        map.add(ally);
        eng.apply(paladin, paladin, devotionAura(), 1_000L);
        eng.updatePartyAreaAuras(paladin, map);
        eng.unapplyAura(paladin, SpellEngine.DEVOTION_AURA);
        paladin.auras.clear();
        AuraSlots.clearVisible(paladin, 0);
        eng.updatePartyAreaAuras(paladin, map);
        assertEquals(0, ally.getInt(UpdateFields.UNIT_FIELD_AURA));
        assertEquals(0, SpellEngine.areaAuraRadiusYards(null), 0.01f);
        SpellEngine.SpellInfo other = new SpellEngine.SpellInfo(
                19579, SpellEngine.EFFECT_APPLY_AREA_AURA_PARTY,
                SpellEngine.SPELL_AURA_MOD_RESISTANCE, 0, 0, 1, 1, 0f, 1);
        assertEquals(0, SpellEngine.areaAuraRadiusYards(other), 0.01f);
        assertEquals(SpellEngine.DEVOTION_AURA_RADIUS_YARDS,
                SpellEngine.areaAuraRadiusYards(devotionAura()), 0.01f);
    }

    @Test
    void updatePartyAreaAurasWhenOtherMapOrForeignHolderShouldSkip() {
        SpellEngine eng = new SpellEngine();
        Player paladin = groupedPaladin(1);
        Player ally = groupedAlly(2, paladin);
        paladin.relocate(0, 0, 0, 0);
        ally.relocate(1, 0, 0, 0);
        GameMap map = new GameMap(0, 0);
        map.add(paladin);
        map.add(ally);
        eng.apply(paladin, paladin, devotionAura(), 1_000L);
        org.tbc.world.entity.Unit.Aura held = paladin.auras.get(0);
        paladin.auras.set(0, new org.tbc.world.entity.Unit.Aura(
                held.spellId(), held.durationMs(), held.stacks(), held.mechanic(),
                held.expireAtMs(), held.amplitudeMs(), held.nextTickAtMs(), 99L));
        eng.updatePartyAreaAuras(paladin, map);
        assertEquals(0, ally.getInt(UpdateFields.UNIT_FIELD_AURA));
        paladin.auras.set(0, held);
        ally.mapId = 1;
        eng.updatePartyAreaAuras(paladin, map);
        assertEquals(0, ally.getInt(UpdateFields.UNIT_FIELD_AURA));
    }

    private static Player groupedPaladin(long guid) {
        Player p = new Player();
        p.guid = guid;
        p.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        p.setHealth(100);
        p.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 0);
        org.tbc.world.entity.Group g = new org.tbc.world.entity.Group();
        g.members.add(p);
        g.leaderGuid = guid;
        p.group = g;
        return p;
    }

    private static Player groupedAlly(long guid, Player paladin) {
        Player a = new Player();
        a.guid = guid;
        a.setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, 100);
        a.setHealth(100);
        a.setInt(UpdateFields.UNIT_FIELD_RESISTANCES, 0);
        a.group = paladin.group;
        paladin.group.members.add(a);
        return a;
    }

    private static org.tbc.common.WowBuffer unitTarget(long guid) {
        org.tbc.common.WowBuffer b = new org.tbc.common.WowBuffer(16);
        b.putU32(SpellCastTargets.UNIT);
        b.putPackedGuid(guid);
        return b;
    }
}
