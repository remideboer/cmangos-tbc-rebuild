package org.tbc.editor.quest;

import org.tbc.world.map.RegionMinimap;
import org.tbc.world.map.WorldMapArea;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Paints a region minimap plus quest markers. No client MPQ assets. */
public final class QuestMapCanvas extends JPanel {
    private QuestMapModel model = new QuestMapModel();
    private Path overlayPath;
    private BufferedImage regionImage;
    private String hoverText = "";
    private Consumer<String> hoverListener = s -> {};

    public QuestMapCanvas() {
        setPreferredSize(new Dimension(640, 480));
        setBackground(new Color(32, 40, 32));
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                float[] img = viewToImage(e.getX(), e.getY());
                model.clickPixel(img[0], img[1]);
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                float[] img = viewToImage(e.getX(), e.getY());
                float[] world = model.viewToWorld(img[0], img[1]);
                hoverText = String.format("X %.2f  Y %.2f", world[0], world[1]);
                hoverListener.accept(hoverText);
                setToolTipText(hoverText);
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double next = model.zoom() * (e.getWheelRotation() < 0 ? 1.1 : 0.9);
                model.setZoom(Math.max(0.1, Math.min(8, next)));
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    public void setHoverListener(Consumer<String> hoverListener) {
        this.hoverListener = hoverListener == null ? s -> {} : hoverListener;
    }

    public void setModel(QuestMapModel model) {
        this.model = model == null ? new QuestMapModel() : model;
        repaint();
    }

    public QuestMapModel model() {
        return model;
    }

    public void loadRegion(WorldMapArea area, RegionMinimap.Raster raster) {
        if (raster == null || raster.empty()) {
            regionImage = null;
            model.setRegion(area, 1, 1);
            repaint();
            return;
        }
        BufferedImage img = new BufferedImage(raster.width(), raster.height(), BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, raster.width(), raster.height(), raster.argb(), 0, raster.width());
        regionImage = img;
        model.setRegion(area, raster.width(), raster.height());
        repaint();
    }

    public void setOverlayPath(Path overlayPath) {
        this.overlayPath = overlayPath;
        repaint();
    }

    public boolean overlayMissing() {
        return overlayPath != null && !Files.isRegularFile(overlayPath);
    }

    public String hoverText() {
        return hoverText;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int dw = (int) Math.max(1, model.imageWidth() * model.zoom());
        int dh = (int) Math.max(1, model.imageHeight() * model.zoom());
        int ox = (int) model.panX();
        int oy = (int) model.panY();
        if (regionImage != null) {
            g2.drawImage(regionImage, ox, oy, dw, dh, null);
        } else {
            g2.setColor(new Color(48, 56, 48));
            g2.fillRect(ox, oy, dw, dh);
            g2.setColor(Color.GRAY);
            WorldMapArea region = model.region();
            g2.drawString(region == null ? "No region" : region.displayName() + " (no map tiles)", ox + 8, oy + 20);
        }
        if (overlayMissing()) {
            g2.setColor(Color.ORANGE);
            g2.drawString("Overlay image missing", 12, getHeight() - 8);
        }
        int i = 0;
        for (QuestMapModel.Marker m : model.visibleMarkers()) {
            float[] pix = model.markerPixel(m);
            int px = ox + (int) (pix[0] * model.zoom());
            int py = oy + (int) (pix[1] * model.zoom());
            g2.setColor(color(m.kind()));
            g2.fillOval(px - 4, py - 4, 8, 8);
            g2.setColor(Color.WHITE);
            g2.drawString(m.kind().name(), px + 6, py);
            i++;
        }
        g2.setColor(Color.LIGHT_GRAY);
        if (i == 0) {
            g2.drawString("No markers", 12, getHeight() - 24);
        }
        if (!hoverText.isEmpty()) {
            g2.drawString(hoverText, 12, 16);
        }
    }

    private float[] viewToImage(int viewX, int viewY) {
        double z = model.zoom() <= 0 ? 1 : model.zoom();
        return new float[]{
                (float) ((viewX - model.panX()) / z),
                (float) ((viewY - model.panY()) / z)
        };
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
