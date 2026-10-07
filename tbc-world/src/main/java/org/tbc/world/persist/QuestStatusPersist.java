package org.tbc.world.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Player;

/** character_queststatus rows: quest log slots and rewarded quests (CMaNGOS Player::_SaveQuestStatus / _LoadQuestStatus). Split out of CharacterStore; CharacterStore.save/load stay the public surface. */
final class QuestStatusPersist {

    private QuestStatusPersist() {
    }
    static void write(Connection c, Player p) throws Exception {
        PreparedStatement del = c.prepareStatement("DELETE FROM character_queststatus WHERE guid = ?");
        del.setInt(1, Guid.low(p.guid));
        del.executeUpdate();
        PreparedStatement ins = c.prepareStatement(
                "INSERT INTO character_queststatus (guid, quest, status, rewarded, explored, timer, "
                        + "mobcount1, mobcount2, mobcount3, mobcount4, itemcount1, itemcount2, itemcount3, itemcount4) "
                        + "VALUES (?,?,?,?,0,0,?,?,?,?,?,?,?,?)");
        for (int slot = 0; slot < p.questLogId.length; slot++) {
            int quest = p.questLogId[slot];
            if (quest == 0) {
                continue;
            }
            ins.setInt(1, Guid.low(p.guid));
            ins.setInt(2, quest);
            ins.setInt(3, dbQuestStatus(p.questLogState[slot]));
            ins.setInt(4, 0);
            ins.setInt(5, p.questLogCounts[slot][0]);
            ins.setInt(6, p.questLogCounts[slot][1]);
            ins.setInt(7, p.questLogCounts[slot][2]);
            ins.setInt(8, p.questLogCounts[slot][3]);
            ins.setInt(9, p.questLogItemCount[slot][0]);
            ins.setInt(10, p.questLogItemCount[slot][1]);
            ins.setInt(11, p.questLogItemCount[slot][2]);
            ins.setInt(12, p.questLogItemCount[slot][3]);
            ins.addBatch();
        }
        for (int quest : p.rewardedQuests) {
            if (questInLog(p, quest)) {
                continue;
            }
            ins.setInt(1, Guid.low(p.guid));
            ins.setInt(2, quest);
            ins.setInt(3, 1);
            ins.setInt(4, 1);
            for (int i = 5; i <= 12; i++) {
                ins.setInt(i, 0);
            }
            ins.addBatch();
        }
        ins.executeBatch();
    }

    static void load(Connection c, Player p) throws Exception {
        PreparedStatement ps = c.prepareStatement(
                "SELECT quest, status, rewarded, mobcount1, mobcount2, mobcount3, mobcount4, "
                        + "itemcount1, itemcount2, itemcount3, itemcount4 FROM character_queststatus WHERE guid = ?");
        ps.setInt(1, Guid.low(p.guid));
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int quest = rs.getInt("quest");
            if (rs.getInt("rewarded") != 0) {
                p.rewardedQuests.add(quest);
                continue;
            }
            int slot = freeQuestSlot(p);
            if (slot < 0) {
                continue;
            }
            p.questLogId[slot] = quest;
            p.questLogState[slot] = logQuestState(rs.getInt("status"));
            p.questLogCounts[slot][0] = rs.getInt("mobcount1");
            p.questLogCounts[slot][1] = rs.getInt("mobcount2");
            p.questLogCounts[slot][2] = rs.getInt("mobcount3");
            p.questLogCounts[slot][3] = rs.getInt("mobcount4");
            p.questLogItemCount[slot][0] = rs.getInt("itemcount1");
            p.questLogItemCount[slot][1] = rs.getInt("itemcount2");
            p.questLogItemCount[slot][2] = rs.getInt("itemcount3");
            p.questLogItemCount[slot][3] = rs.getInt("itemcount4");
        }
        Content.syncQuestLogFields(p);
    }

    /** QuestDef.h QUEST_STATUS_COMPLETE 1 / INCOMPLETE 3 / FAILED 5. */
    static int dbQuestStatus(int logState) {
        if (logState == org.tbc.world.content.Content.QUEST_STATE_COMPLETE) {
            return 1;
        }
        if (logState == org.tbc.world.content.Content.QUEST_STATE_FAIL) {
            return 5;
        }
        return 3;
    }

    static int logQuestState(int status) {
        if (status == 1) {
            return org.tbc.world.content.Content.QUEST_STATE_COMPLETE;
        }
        if (status == 5) {
            return org.tbc.world.content.Content.QUEST_STATE_FAIL;
        }
        return 0;
    }

    static boolean questInLog(Player p, int quest) {
        for (int id : p.questLogId) {
            if (id == quest) {
                return true;
            }
        }
        return false;
    }

    static int freeQuestSlot(Player p) {
        for (int i = 0; i < p.questLogId.length; i++) {
            if (p.questLogId[i] == 0) {
                return i;
            }
        }
        return -1;
    }
}
