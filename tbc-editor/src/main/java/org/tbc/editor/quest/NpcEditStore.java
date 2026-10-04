package org.tbc.editor.quest;

import java.sql.SQLException;

/** Writes dragged positions and shared template name, display, and gear. */
public final class NpcEditStore {
    @FunctionalInterface
    public interface Sql {
        void update(String sql, Object... args) throws SQLException;
    }

    private NpcEditStore() {}

    public static void save(Sql sql, NpcEditSession session) throws SQLException {
        if (sql == null || session == null) {
            return;
        }
        for (NpcEditSession.Pose pose : session.moved()) {
            sql.update("UPDATE creature SET position_x = ?, position_y = ?, position_z = ? WHERE guid = ?",
                    pose.x(), pose.y(), pose.z(), pose.guid());
        }
        for (NpcEditSession.Look look : session.changedLooks()) {
            try {
                sql.update(
                        "UPDATE creature_template SET Name = ?, DisplayId1 = ?, EquipmentTemplateId = ?, CreatureType = ?, Faction = ? WHERE Entry = ?",
                        look.name(), look.displayId(), look.equipmentId(), look.creatureType(), look.faction(),
                        look.entry());
            } catch (SQLException displayColumn) {
                sql.update(
                        "UPDATE creature_template SET Name = ?, ModelId1 = ?, EquipmentTemplateId = ?, CreatureType = ?, Faction = ? WHERE Entry = ?",
                        look.name(), look.displayId(), look.equipmentId(), look.creatureType(), look.faction(),
                        look.entry());
            }
        }
    }
}
