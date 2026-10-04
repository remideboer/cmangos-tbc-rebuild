package org.tbc.editor.quest;

import org.tbc.world.map.RegionMinimap;
import org.tbc.world.map.WorldMapArea;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/** Paints a region minimap plus quest markers. No client MPQ assets. */
public final class QuestMapCanvas extends JPanel {
    private QuestMapModel model = new QuestMapModel();
    private Path overlayPath;
    private BufferedImage regionImage;
    private String hoverText = "";
    private Consumer<String> hoverListener = s -> {};
    private boolean panning;
    private int lastX;
    private int lastY;
    private float popupImgX;
    private float popupImgY;
    private JPopupMenu contextMenu = new JPopupMenu();
    private List<MapSpawnLayer.Pin> spawns = List.of();
    private boolean showSpawns = true;

    public QuestMapCanvas() {
        setPreferredSize(new Dimension(640, 480));
        setBackground(new Color(32, 40, 32));
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    panning = true;
                    lastX = e.getX();
                    lastY = e.getY();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
                    float[] img = viewToImage(e.getX(), e.getY());
                    popupImgX = img[0];
                    popupImgY = img[1];
                    contextMenu = buildContextMenu();
                    if (isShowing()) {
                        contextMenu.show(QuestMapCanvas.this, e.getX(), e.getY());
                    }
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON1) {
                    float[] img = viewToImage(e.getX(), e.getY());
                    model.clickPixel(img[0], img[1]);
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    panning = false;
                    setCursor(Cursor.getDefaultCursor());
                }
                if (e.isPopupTrigger() && e.getButton() == MouseEvent.BUTTON3) {
                    float[] img = viewToImage(e.getX(), e.getY());
                    popupImgX = img[0];
                    popupImgY = img[1];
                    contextMenu = buildContextMenu();
                    if (isShowing()) {
                        contextMenu.show(QuestMapCanvas.this, e.getX(), e.getY());
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!panning && (e.getModifiersEx() & MouseEvent.BUTTON2_DOWN_MASK) == 0) {
                    return;
                }
                panning = true;
                int dx = e.getX() - lastX;
                int dy = e.getY() - lastY;
                lastX = e.getX();
                lastY = e.getY();
                model.pan(dx, dy);
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                float[] img = viewToImage(e.getX(), e.getY());
                float[] world = model.viewToWorld(img[0], img[1]);
                MapSpawnLayer.Pin pin = showSpawns
                        ? MapSpawnLayer.nearest(spawns, world[0], world[1], hoverReachYards()) : null;
                hoverText = pin == null
                        ? String.format("X %.2f  Y %.2f", world[0], world[1])
                        : pin.name() + " (" + pin.entry() + ")";
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

    public JPopupMenu contextMenu() {
        return contextMenu;
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

    public void setSpawns(List<MapSpawnLayer.Pin> spawns) {
        this.spawns = spawns == null ? List.of() : List.copyOf(spawns);
        repaint();
    }

    public List<MapSpawnLayer.Pin> spawns() {
        return spawns;
    }

    public void setShowSpawns(boolean showSpawns) {
        this.showSpawns = showSpawns;
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
        if (showSpawns) {
            for (MapSpawnLayer.Pin pin : spawns) {
                float[] pix = model.worldToPixel(pin.x(), pin.y());
                int px = ox + (int) (pix[0] * model.zoom());
                int py = oy + (int) (pix[1] * model.zoom());
                g2.setColor(pin.kind() == MapSpawnLayer.Kind.CREATURE
                        ? new Color(80, 160, 255) : new Color(255, 170, 40));
                g2.fillRect(px - 2, py - 2, 4, 4);
            }
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

    private JPopupMenu buildContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        addTool(menu, "Select", QuestMapModel.Tool.SELECT);
        addTool(menu, "Place giver", QuestMapModel.Tool.PLACE_GIVER);
        addTool(menu, "Place turn-in", QuestMapModel.Tool.PLACE_TURN_IN);
        addTool(menu, "Place kill", QuestMapModel.Tool.PLACE_KILL);
        addTool(menu, "Place object", QuestMapModel.Tool.PLACE_OBJECT);
        addTool(menu, "Place area", QuestMapModel.Tool.PLACE_AREA);
        addTool(menu, "Place route", QuestMapModel.Tool.PLACE_ROUTE);
        addTool(menu, "Place note", QuestMapModel.Tool.PLACE_NOTE);
        menu.addSeparator();
        JMenuItem undo = new JMenuItem("Undo");
        undo.addActionListener(e -> {
            model.undo();
            repaint();
        });
        JMenuItem redo = new JMenuItem("Redo");
        redo.addActionListener(e -> {
            model.redo();
            repaint();
        });
        JMenuItem fit = new JMenuItem("Fit to region");
        fit.addActionListener(e -> {
            model.fitToRegion();
            repaint();
        });
        JMenuItem del = new JMenuItem("Delete selected");
        del.setEnabled(model.selected() != null);
        del.addActionListener(e -> {
            model.deleteSelected();
            repaint();
        });
        menu.add(undo);
        menu.add(redo);
        menu.add(fit);
        menu.add(del);
        return menu;
    }

    private void addTool(JPopupMenu menu, String label, QuestMapModel.Tool tool) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> {
            model.setTool(tool);
            if (tool != QuestMapModel.Tool.SELECT) {
                model.clickPixel(popupImgX, popupImgY);
            }
            repaint();
        });
        menu.add(item);
    }

    /** At least 30 yards, or 8 screen pixels when the zone is zoomed out. */
    private float hoverReachYards() {
        WorldMapArea area = model.region();
        if (area == null || model.imageWidth() <= 0 || model.imageHeight() <= 0) {
            return 30f;
        }
        float perPixel = Math.max(
                Math.abs(area.locTop() - area.locBottom()) / model.imageWidth(),
                Math.abs(area.locLeft() - area.locRight()) / model.imageHeight());
        double z = model.zoom() <= 0 ? 1 : model.zoom();
        return Math.max(30f, (float) (perPixel * 8 / z));
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
