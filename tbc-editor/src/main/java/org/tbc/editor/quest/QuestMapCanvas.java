package org.tbc.editor.quest;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.nio.file.Files;
import java.nio.file.Path;

/** Paints a user-supplied overlay plus quest markers. No client MPQ assets. */
public final class QuestMapCanvas extends JPanel {
    private QuestMapModel model = new QuestMapModel();
    private Path overlayPath;

    public QuestMapCanvas() {
        setPreferredSize(new Dimension(640, 480));
        setBackground(new Color(32, 40, 32));
    }

    public void setModel(QuestMapModel model) {
        this.model = model == null ? new QuestMapModel() : model;
        repaint();
    }

    public QuestMapModel model() {
        return model;
    }

    public void setOverlayPath(Path overlayPath) {
        this.overlayPath = overlayPath;
        repaint();
    }

    public boolean overlayMissing() {
        return overlayPath != null && !Files.isRegularFile(overlayPath);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        if (overlayMissing()) {
            g2.setColor(Color.ORANGE);
            g2.drawString("Overlay image missing", 12, 20);
        }
        int i = 0;
        for (QuestMapModel.Marker m : model.visibleMarkers()) {
            int px = (int) (320 + m.x() * model.zoom() + model.panX());
            int py = (int) (240 - m.y() * model.zoom() + model.panY());
            g2.setColor(color(m.kind()));
            g2.fillOval(px - 4, py - 4, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawString(m.kind().name(), px + 6, py);
            i++;
        }
        if (i == 0) {
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString("No markers", 12, 40);
        }
    }

    private static Color color(QuestMapModel.MarkerKind kind) {
        return switch (kind) {
            case GIVER -> Color.GREEN;
            case TURN_IN -> Color.CYAN;
            case KILL_TARGET -> Color.RED;
            case QUEST_OBJECT -> Color.YELLOW;
            case AREA, ROUTE -> Color.MAGENTA;
            case NOTE -> Color.GRAY;
        };
    }

    public JComponent view() {
        return this;
    }
}
