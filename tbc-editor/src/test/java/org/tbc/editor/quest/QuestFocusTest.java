package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;
import org.tbc.content.dbc.WdbcFile;
import org.tbc.world.content.ObjectMgr;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Container;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestFocusTest {
    @Test
    void ofWhenTurnInMissingShouldMarkTheGiverAsTurnIn() {
        QuestDocument quest = QuestDocument.newOwned(95001);
        quest.setGiverNpc(5);
        QuestRoles.Marks marks = QuestRoles.of(quest);
        assertTrue(marks.creatures().get(5).contains(QuestRoles.Role.GIVER));
        assertTrue(marks.creatures().get(5).contains(QuestRoles.Role.TURN_IN));
    }

    @Test
    void presentQuestWhenGiverKillAndTurnInShouldHighlightAndGraphThem() {
        QuestDomain domain = domain();
        QuestDocument quest = QuestDocument.newOwned(95002);
        quest.setGiverNpc(15271);
        quest.setTurnInNpc(15272);
        quest.setReqCreature(0, 15273, 1);
        quest.setReqCreature(1, -50, 1);
        domain.presentQuest(quest);
        QuestRoles.Marks marks = domain.canvas().questHighlights();
        assertTrue(marks.creatures().get(15271).contains(QuestRoles.Role.GIVER));
        assertTrue(marks.creatures().get(15272).contains(QuestRoles.Role.TURN_IN));
        assertTrue(marks.creatures().get(15273).contains(QuestRoles.Role.OBJECTIVE));
        assertTrue(marks.objects().get(50).contains(QuestRoles.Role.OBJECTIVE));
        assertTrue(domain.graphCanvas().model().nodes().stream()
                .anyMatch(n -> n.kind() == QuestGraphModel.Kind.GIVER));
        assertTrue(domain.graphCanvas().model().nodes().stream()
                .anyMatch(n -> n.kind() == QuestGraphModel.Kind.OBJECTIVE));
    }

    @Test
    void npcMenuWhenBuiltShouldExposeEditorsBesideAResizableMap() {
        QuestDomain domain = domain();
        assertTrue(hasSplit(domain.view()));
        JPopupMenu menu = domain.npcMenu();
        assertTrue(hasClass(menu, JTextField.class));
        assertTrue(hasText(menu, "Save changes"));
        assertTrue(hasText(menu, "Clear changes"));
        assertTrue(hasText(menu, "Beast"));
        assertTrue(hasText(menu, "Creature type"));
        assertTrue(hasText(menu, "Faction"));
    }

    @Test
    void factionWhenTemplatePointsAtFactionDbcShouldShowTheName() {
        byte[] strings = "\0Stormwind\0".getBytes(StandardCharsets.UTF_8);
        int[] faction = new int[23];
        faction[0] = 72;
        faction[22] = 1;
        int[] template = new int[] {12, 72};
        NpcFactions names = NpcFactions.join(
                new WdbcFile(2, 8, List.of(template), new byte[] {0}),
                new WdbcFile(23, 92, List.of(faction), strings));
        assertEquals("Stormwind", names.name(12));
        assertTrue(new QuestService.ZoneNpc(1, "Guard", 10, 12, "Stormwind").toString().contains("12 — Stormwind"));
    }

    private static QuestDomain domain() {
        return new QuestDomain(new QuestService(new ObjectMgr(), Path.of("target", "npc-edit-test")), s -> {});
    }

    private static boolean hasSplit(JComponent view) {
        for (Component c : view.getComponents()) {
            if (c instanceof JSplitPane split && split.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasClass(Component c, Class<?> type) {
        if (type.isInstance(c)) {
            return true;
        }
        if (c instanceof Container box) {
            for (Component child : box.getComponents()) {
                if (hasClass(child, type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasText(Component c, String text) {
        if (c instanceof JButton button && text.equals(button.getText())) {
            return true;
        }
        if (c instanceof JLabel label && label.getText() != null && label.getText().contains(text)) {
            return true;
        }
        if (c instanceof JComboBox<?> box) {
            for (int i = 0; i < box.getItemCount(); i++) {
                if (String.valueOf(box.getItemAt(i)).contains(text)) {
                    return true;
                }
            }
        }
        if (c instanceof Container box) {
            for (Component child : box.getComponents()) {
                if (hasText(child, text)) {
                    return true;
                }
            }
        }
        return false;
    }
}
