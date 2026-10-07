package org.tbc.world.content;

import org.tbc.world.content.ObjectMgr.QuestExtras;
import org.tbc.world.content.ObjectMgr.QuestTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

/**
 * SQL load of the quest family: quest_template (richest SELECT first, no row cap), creature / gameobject /
 * areatrigger relations and the quest extras columns. Carved from ObjectMgr in refactoring plan cycle 4.2.
 */
final class QuestLoader {
    private final ObjectMgr m;

    private QuestLoader(ObjectMgr m) {
        this.m = m;
    }

    /** Same order as ObjectMgr.load. */
    static void load(ObjectMgr m, Connection c) {
        QuestLoader l = new QuestLoader(m);
        l.loadQuests(c);
        l.loadQuestRelations(c);
        l.loadQuestExtras(c);
    }
    /** quest_template SELECTs, richest first. No row cap. */
    static java.util.List<String> questTemplateQueries() {
        String full = "SELECT entry, Title, MinLevel, Type, Details, Objectives, "
                + "ReqCreatureOrGOId1, ReqCreatureOrGOCount1, ReqCreatureOrGOId2, ReqCreatureOrGOCount2, "
                + "ReqCreatureOrGOId3, ReqCreatureOrGOCount3, ReqCreatureOrGOId4, ReqCreatureOrGOCount4, "
                + "ReqItemId1, ReqItemCount1, ReqItemId2, ReqItemCount2, ReqItemId3, ReqItemCount3, "
                + "ReqItemId4, ReqItemCount4, QuestLevel, RewMoneyMaxLevel, RewItemId1, RewItemCount1, "
                + "RewChoiceItemId1, RewChoiceItemCount1, RewChoiceItemId2, RewChoiceItemCount2, "
                + "PrevQuestId, RewOrReqMoney, RequiredRaces, ZoneOrSort FROM quest_template";
        return java.util.List.of(
                full,
                "SELECT entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1, ReqItemId1, ReqItemCount1, "
                        + "QuestLevel, RewMoneyMaxLevel, RewItemId1, RewItemCount1, "
                        + "RewChoiceItemId1, RewChoiceItemCount1, RewChoiceItemId2, RewChoiceItemCount2 FROM quest_template",
                "SELECT Entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1, ReqItemId1, ReqItemCount1, "
                        + "QuestLevel, RewMoneyMaxLevel, RewItemId1, RewItemCount1, "
                        + "RewChoiceItemId1, RewChoiceItemCount1, RewChoiceItemId2, RewChoiceItemCount2 FROM quest_template",
                "SELECT entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1, ReqItemId1, ReqItemCount1, "
                        + "QuestLevel, RewMoneyMaxLevel, RewItemId1, RewItemCount1 FROM quest_template",
                "SELECT Entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1, ReqItemId1, ReqItemCount1, "
                        + "QuestLevel, RewMoneyMaxLevel, RewItemId1, RewItemCount1 FROM quest_template",
                "SELECT entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1, ReqItemId1, ReqItemCount1 FROM quest_template",
                "SELECT Entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1, ReqItemId1, ReqItemCount1 FROM quest_template",
                "SELECT entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1 FROM quest_template",
                "SELECT Entry, Title, MinLevel, Type, ReqCreatureOrGOId1, ReqCreatureOrGOCount1 FROM quest_template",
                "SELECT entry, Title, MinLevel, Type FROM quest_template",
                "SELECT Entry, Title, MinLevel, Type FROM quest_template");
    }

    /** Starter and turn-in links. Index 0 offers, index 1 finishes. */
    static java.util.List<String> questRelationQueries() {
        return java.util.List.of(
                "SELECT id, quest FROM creature_questrelation",
                "SELECT id, quest FROM creature_involvedrelation");
    }

    private void loadQuests(Connection c) {
        for (String sql : questTemplateQueries()) {
            try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                int cols = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    if (cols >= 34) {
                        m.quests.put(rs.getInt("entry"), fullQuest(rs));
                    } else {
                        int reqId = cols >= 6 ? rs.getInt(5) : 0;
                        int reqCount = cols >= 6 ? rs.getInt(6) : 0;
                        int itemId = cols >= 8 ? rs.getInt(7) : 0;
                        int itemCount = cols >= 8 ? rs.getInt(8) : 0;
                        int qLevel = cols >= 12 ? rs.getInt(9) : 0;
                        int maxMoney = cols >= 12 ? rs.getInt(10) : 0;
                        int rewItem = cols >= 12 ? rs.getInt(11) : 0;
                        int rewCount = cols >= 12 ? rs.getInt(12) : 0;
                        int choiceId1 = cols >= 16 ? rs.getInt(13) : 0;
                        int choiceCount1 = cols >= 16 ? rs.getInt(14) : 0;
                        int choiceId2 = cols >= 16 ? rs.getInt(15) : 0;
                        int choiceCount2 = cols >= 16 ? rs.getInt(16) : 0;
                        m.quests.put(rs.getInt(1), new QuestTemplate(rs.getInt(1), SqlText.nz(rs.getString(2)),
                                rs.getInt(3), rs.getInt(4), 0, "", "", reqId, reqCount, itemId, itemCount,
                                qLevel, maxMoney, rewItem, rewCount, choiceId1, choiceCount1, choiceId2, choiceCount2));
                    }
                }
                return;
            } catch (Exception ignored) {
            }
        }
    }

    private static QuestTemplate fullQuest(ResultSet rs) throws Exception {
        return new QuestTemplate(rs.getInt("entry"), SqlText.nz(rs.getString("Title")), rs.getInt("MinLevel"), rs.getInt("Type"),
                rs.getInt("RewOrReqMoney"), SqlText.nz(rs.getString("Details")), SqlText.nz(rs.getString("Objectives")),
                rs.getInt("ReqCreatureOrGOId1"), rs.getInt("ReqCreatureOrGOCount1"),
                rs.getInt("ReqItemId1"), rs.getInt("ReqItemCount1"),
                rs.getInt("QuestLevel"), rs.getInt("RewMoneyMaxLevel"),
                rs.getInt("RewItemId1"), rs.getInt("RewItemCount1"),
                rs.getInt("RewChoiceItemId1"), rs.getInt("RewChoiceItemCount1"),
                rs.getInt("RewChoiceItemId2"), rs.getInt("RewChoiceItemCount2"),
                rs.getInt("ReqCreatureOrGOId2"), rs.getInt("ReqCreatureOrGOCount2"),
                rs.getInt("ReqCreatureOrGOId3"), rs.getInt("ReqCreatureOrGOCount3"),
                rs.getInt("ReqCreatureOrGOId4"), rs.getInt("ReqCreatureOrGOCount4"),
                rs.getInt("ReqItemId2"), rs.getInt("ReqItemCount2"),
                rs.getInt("ReqItemId3"), rs.getInt("ReqItemCount3"),
                rs.getInt("ReqItemId4"), rs.getInt("ReqItemCount4"),
                rs.getInt("PrevQuestId"), rs.getInt("RequiredRaces"), rs.getInt("ZoneOrSort"));
    }

    private void loadQuestRelations(Connection c) {
        java.util.List<String> sqls = questRelationQueries();
        loadOneRelation(c, sqls.get(0), m.questGivers);
        loadOneRelation(c, sqls.get(1), m.questInvolved);
        loadOneRelation(c, "SELECT id, quest FROM gameobject_questrelation", m.goQuestGivers);
        loadOneRelation(c, "SELECT id, quest FROM gameobject_involvedrelation", m.goQuestInvolved);
        loadAreaTriggerQuests(c);
    }

    private void loadAreaTriggerQuests(Connection c) {
        try (PreparedStatement ps = c.prepareStatement("SELECT id, quest FROM areatrigger_involvedrelation");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.areaTriggerQuests.put(rs.getInt(1), rs.getInt(2));
            }
        } catch (Exception ignored) {
        }
    }

    private void loadQuestExtras(Connection c) {
        String sql = "SELECT entry, ReqSpellCast1, SpecialFlags, QuestFlags, LimitTime, PointMapId, PointX, PointY, "
                + "RequiredMinRepFaction, RequiredMinRepValue, RewRepFaction1, RewRepValue1 FROM quest_template";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                m.questExtras.put(rs.getInt(1), new QuestExtras(
                        rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6),
                        rs.getFloat(7), rs.getFloat(8), rs.getInt(9), rs.getInt(10), rs.getInt(11), rs.getInt(12)));
            }
        } catch (Exception ignored) {
        }
    }

    private static void loadOneRelation(Connection c, String sql, Map<Integer, List<Integer>> dest) {
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ObjectMgr.addQuestRelation(dest, rs.getInt(1), rs.getInt(2));
            }
        } catch (Exception ignored) {
        }
    }
}
