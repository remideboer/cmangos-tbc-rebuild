package org.tbc.world.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.world.content.ObjectMgr.CreatureTemplate;
import org.tbc.world.content.ObjectMgr.LootRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * SQL load of the creature family: creature_template (column-set fallbacks), equipment, MinLevelMana,
 * creature_model_info and the creature / gameobject loot templates. Carved from ObjectMgr in refactoring
 * plan cycle 4.2.
 */
final class CreatureLoader {
    private static final Logger log = LoggerFactory.getLogger(CreatureLoader.class);
    private final ObjectMgr m;

    private CreatureLoader(ObjectMgr m) {
        this.m = m;
    }

    /** Same order as ObjectMgr.load. */
    static void load(ObjectMgr m, Connection c) {
        CreatureLoader l = new CreatureLoader(m);
        l.loadCreatures(c);
        l.loadEquipment(c);
        l.loadCreatureMana(c);
        l.loadModelInfo(c);
        l.loadCreatureLoot(c);
        l.loadGameObjectLoot(c);
    }
    /** Side-load gear ids. A missing column must not fail the creature_template name/display load. */
    private void loadEquipment(Connection c) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT Entry, EquipmentTemplateId FROM creature_template");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt(2);
                if (id > 0) {
                    m.equipmentByEntry.put(rs.getInt(1), id);
                }
            }
        } catch (Exception e) {
            log.warn("creature equipment id load failed: {}", e.getMessage());
        }
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT entry, equipentry1, equipentry2, equipentry3 FROM creature_equip_template");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.equipmentItems.put(rs.getInt(1), new int[] {rs.getInt(2), rs.getInt(3), rs.getInt(4)});
            }
        } catch (Exception e) {
            log.warn("creature_equip_template load failed: {}", e.getMessage());
        }
    }

    private void loadCreatures(Connection c) {
        if (loadCreaturesSql(c,
                "SELECT Entry, Name, SubName, IconName, DisplayId1, DisplayId2, DisplayId3, DisplayId4, "
                        + "CreatureTypeFlags, CreatureType, Family, `Rank`, PetSpellDataId, HealthMultiplier, "
                        + "PowerMultiplier, RacialLeader, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName, "
                        + "AIName, ExtraFlags, MinMeleeDmg, MaxMeleeDmg, MeleeBaseAttackTime, LootId, MinLootGold, MaxLootGold, InhabitType "
                        + "FROM creature_template",
                true, true)) {
            log.info("loaded {} creature_template rows", m.creatures.size());
            return;
        }
        if (loadCreaturesSql(c,
                "SELECT Entry, Name, SubName, IconName, ModelId1, ModelId2, ModelId3, ModelId4, "
                        + "CreatureTypeFlags, CreatureType, Family, `Rank`, PetSpellDataId, HealthMultiplier, "
                        + "PowerMultiplier, RacialLeader, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName, "
                        + "AIName, ExtraFlags, MinMeleeDmg, MaxMeleeDmg, MeleeBaseAttackTime, LootId, MinLootGold, MaxLootGold, InhabitType "
                        + "FROM creature_template",
                true, true)) {
            log.info("loaded {} creature_template rows", m.creatures.size());
            return;
        }
        if (loadCreaturesSql(c,
                "SELECT Entry, Name, SubName, IconName, ModelId1, ModelId2, ModelId3, ModelId4, "
                        + "CreatureTypeFlags, CreatureType, Family, `Rank`, PetSpellDataId, HealthMultiplier, "
                        + "PowerMultiplier, RacialLeader, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName, "
                        + "AIName, ExtraFlags, MinMeleeDmg, MaxMeleeDmg, MeleeBaseAttackTime, LootId, MinLootGold, MaxLootGold "
                        + "FROM creature_template",
                true, true)) {
            log.info("loaded {} creature_template rows", m.creatures.size());
            return;
        }
        if (loadCreaturesSql(c,
                "SELECT Entry, Name, SubName, IconName, DisplayId1, DisplayId2, DisplayId3, DisplayId4, "
                        + "CreatureTypeFlags, CreatureType, Family, `Rank`, PetSpellDataId, HealthMultiplier, "
                        + "PowerMultiplier, RacialLeader, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName, "
                        + "AIName, ExtraFlags, MinMeleeDmg, MaxMeleeDmg, MeleeBaseAttackTime, LootId, MinLootGold, MaxLootGold "
                        + "FROM creature_template",
                true, true)) {
            log.info("loaded {} creature_template rows", m.creatures.size());
            return;
        }
        if (loadCreaturesSql(c,
                "SELECT Entry, Name, SubName, IconName, ModelId1, ModelId2, ModelId3, ModelId4, "
                        + "CreatureTypeFlags, CreatureType, Family, `Rank`, PetSpellDataId, HealthMultiplier, "
                        + "PowerMultiplier, RacialLeader, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName "
                        + "FROM creature_template",
                true, false)) {
            log.info("loaded {} creature_template rows", m.creatures.size());
            return;
        }
        if (loadCreaturesSql(c,
                "SELECT Entry, Name, SubName, IconName, DisplayId1, DisplayId2, DisplayId3, DisplayId4, "
                        + "CreatureTypeFlags, CreatureType, Family, `Rank`, PetSpellDataId, HealthMultiplier, "
                        + "PowerMultiplier, RacialLeader, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName "
                        + "FROM creature_template",
                true, false)) {
            log.info("loaded {} creature_template rows", m.creatures.size());
            return;
        }
        if (loadCreaturesSimple(c,
                "SELECT Entry, Name, ModelId1, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName "
                        + "FROM creature_template")) {
            log.info("loaded {} creature_template rows (simple)", m.creatures.size());
            return;
        }
        if (loadCreaturesSimple(c,
                "SELECT Entry, Name, DisplayId1, Faction, MinLevelHealth, MinLevel, NpcFlags, ScriptName "
                        + "FROM creature_template")) {
            log.info("loaded {} creature_template rows (simple)", m.creatures.size());
            return;
        }
        if (loadCreaturesSimple(c,
                "SELECT entry, name, modelid_1, faction_A, minhealth, minlevel, npcflag, ScriptName "
                        + "FROM creature_template")) {
            log.info("loaded {} creature_template rows (trinity)", m.creatures.size());
            return;
        }
        log.warn("creature_template column mismatch, using seed templates");
    }

    /** creature_template MinLevelMana → {@link ObjectMgr#creatureMana} (CMaNGOS SelectLevel). */
    private void loadCreatureMana(Connection c) {
        String[] sqls = {
                "SELECT Entry, MinLevelMana FROM creature_template WHERE MinLevelMana > 0",
                "SELECT entry, minmana FROM creature_template WHERE minmana > 0",
                "SELECT Entry, MinLevelMana FROM creature_template WHERE MinLevelMana > 0"
        };
        for (String sql : sqls) {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                int n = 0;
                while (rs.next()) {
                    int mana = rs.getInt(2);
                    if (mana > 0) {
                        m.creatureMana.put(rs.getInt(1), mana);
                        n++;
                    }
                }
                log.info("loaded MinLevelMana for {} creatures", n);
                return;
            } catch (Exception e) {
                log.debug("creature mana query skipped: {}", e.getMessage());
            }
        }
    }

    private boolean loadCreaturesSql(Connection c, String sql, boolean full, boolean combat) {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int entry = rs.getInt(1);
                String name = ObjectMgr.nz(rs.getString(2));
                if (full && combat) {
                    int inhabit = inhabitTypeOrDefault(rs);
                    m.creatures.put(entry, new CreatureTemplate(
                            entry, name, rs.getInt(5), rs.getInt(17), Math.max(1, rs.getInt(18)), rs.getInt(19),
                            rs.getInt(20), ObjectMgr.nz(rs.getString(21)), "", 0,
                            ObjectMgr.nz(rs.getString(3)), ObjectMgr.nz(rs.getString(4)), rs.getInt(6), rs.getInt(7), rs.getInt(8),
                            rs.getInt(9), rs.getInt(10), rs.getInt(11), rs.getInt(12), rs.getInt(13),
                            rs.getFloat(14), rs.getFloat(15), rs.getInt(16),
                            ObjectMgr.nz(rs.getString(22)), rs.getInt(23), rs.getFloat(24), rs.getFloat(25),
                            Math.max(1, rs.getInt(26)), 0f, rs.getInt(27), rs.getInt(28), rs.getInt(29), inhabit));
                } else if (full) {
                    m.creatures.put(entry, new CreatureTemplate(
                            entry, name, rs.getInt(5), rs.getInt(17), Math.max(1, rs.getInt(18)), rs.getInt(19),
                            rs.getInt(20), ObjectMgr.nz(rs.getString(21)), "", 0,
                            ObjectMgr.nz(rs.getString(3)), ObjectMgr.nz(rs.getString(4)), rs.getInt(6), rs.getInt(7), rs.getInt(8),
                            rs.getInt(9), rs.getInt(10), rs.getInt(11), rs.getInt(12), rs.getInt(13),
                            rs.getFloat(14), rs.getFloat(15), rs.getInt(16)));
                } else {
                    m.creatures.put(entry, new CreatureTemplate(
                            entry, name, rs.getInt(5), rs.getInt(17), Math.max(1, rs.getInt(18)), rs.getInt(19),
                            rs.getInt(20), ObjectMgr.nz(rs.getString(21)), "", 0));
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("creature_template query failed: {}", e.getMessage());
            return false;
        }
    }

    private static int inhabitTypeOrDefault(ResultSet rs) {
        try {
            int v = rs.getInt("InhabitType");
            if (rs.wasNull() || v <= 0) {
                return org.tbc.world.map.CreatureGrounding.DEFAULT_INHABIT;
            }
            return v;
        } catch (Exception e) {
            return org.tbc.world.map.CreatureGrounding.DEFAULT_INHABIT;
        }
    }

    private void loadModelInfo(Connection c) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT modelid, combat_reach FROM creature_model_info");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.modelCombatReach.put(rs.getInt(1), rs.getFloat(2));
            }
            log.info("loaded {} creature_model_info rows", m.modelCombatReach.size());
        } catch (Exception e) {
            log.debug("creature_model_info load skipped: {}", e.getMessage());
        }
    }

    private void loadCreatureLoot(Connection c) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT entry, item, ChanceOrQuestChance, mincountOrRef, maxcount FROM creature_loot_template");
             ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                int minCount = rs.getInt(4);
                if (minCount < 0) {
                    // Reference rows are not expanded yet.
                    continue;
                }
                float chanceRaw = rs.getFloat(3);
                boolean needsQuest = chanceRaw < 0f;
                float chance = Math.abs(chanceRaw);
                m.creatureLoot.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>())
                        .add(new LootRow(rs.getInt(2), chance, minCount, Math.max(minCount, rs.getInt(5)), needsQuest));
                n++;
            }
            log.info("loaded {} creature_loot_template rows", n);
        } catch (Exception e) {
            log.debug("creature_loot_template load skipped: {}", e.getMessage());
        }
    }

    private void loadGameObjectLoot(Connection c) {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT entry, item, ChanceOrQuestChance, mincountOrRef, maxcount FROM gameobject_loot_template");
             ResultSet rs = ps.executeQuery()) {
            int n = 0;
            while (rs.next()) {
                int minCount = rs.getInt(4);
                if (minCount < 0) {
                    continue;
                }
                float chanceRaw = rs.getFloat(3);
                boolean needsQuest = chanceRaw < 0f;
                float chance = Math.abs(chanceRaw);
                m.gameObjectLoot.computeIfAbsent(rs.getInt(1), k -> new ArrayList<>())
                        .add(new LootRow(rs.getInt(2), chance, minCount, Math.max(minCount, rs.getInt(5)), needsQuest));
                n++;
            }
            log.info("loaded {} gameobject_loot_template rows", n);
        } catch (Exception e) {
            log.debug("gameobject_loot_template load skipped: {}", e.getMessage());
        }
    }

    private boolean loadCreaturesSimple(Connection c, String sql) {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int entry = rs.getInt(1);
                m.creatures.put(entry, new CreatureTemplate(
                        entry, ObjectMgr.nz(rs.getString(2)), rs.getInt(3), rs.getInt(4),
                        Math.max(1, rs.getInt(5)), rs.getInt(6), rs.getInt(7), ObjectMgr.nz(rs.getString(8)), "", 0));
            }
            return true;
        } catch (Exception e) {
            log.warn("creature_template simple query failed: {}", e.getMessage());
            return false;
        }
    }
}
