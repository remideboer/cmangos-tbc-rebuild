package org.tbc.editor.quest;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.function.IntConsumer;

/** Mind-map of quest relationships. Layout drag does not move world coordinates. */
public final class QuestGraphCanvas extends JPanel {
    private QuestGraphModel model = new QuestGraphModel();
    private boolean panning;
    private boolean draggingNode;
    private int lastX;
    private int lastY;
    private int prerequisiteCandidate;
    private int turnInCandidate;
    private Runnable createChild = () -> {};
    private Runnable jumpToMap = () -> {};
    private IntConsumer inspectQuest = id -> {};
    private JPopupMenu contextMenu = new JPopupMenu();

    public QuestGraphCanvas() {
        setPreferredSize(new Dimension(640, 480));
        setBackground(new Color(24, 28, 34));
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
                    selectAt(e.getX(), e.getY());
                    contextMenu = buildMenu();
                    if (isShowing()) {
                        contextMenu.show(QuestGraphCanvas.this, e.getX(), e.getY());
                    }
                    return;
                }
                if (e.getButton() == MouseEvent.BUTTON1) {
                    draggingNode = selectAt(e.getX(), e.getY());
                    lastX = e.getX();
                    lastY = e.getY();
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    panning = false;
                    draggingNode = false;
                    setCursor(Cursor.getDefaultCursor());
                }
                if (e.getButton() == MouseEvent.BUTTON1) {
                    draggingNode = false;
                }
                if (e.isPopupTrigger() && e.getButton() == MouseEvent.BUTTON3) {
                    selectAt(e.getX(), e.getY());
                    contextMenu = buildMenu();
                    if (isShowing()) {
                        contextMenu.show(QuestGraphCanvas.this, e.getX(), e.getY());
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (panning || (e.getModifiersEx() & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
                    panning = true;
                    model.pan(e.getX() - lastX, e.getY() - lastY);
                    lastX = e.getX();
                    lastY = e.getY();
                    repaint();
                    return;
                }
                if (draggingNode && model.node(model.selectedId()) != null) {
                    double[] g = toGraph(e.getX(), e.getY());
                    model.moveNode(model.selectedId(), g[0], g[1]);
                    lastX = e.getX();
                    lastY = e.getY();
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                double[] g = toGraph(e.getX(), e.getY());
                QuestGraphModel.Node n = model.hit(g[0], g[1]);
                setToolTipText(n == null ? null : n.hover());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 2) {
                    return;
                }
                double[] g = toGraph(e.getX(), e.getY());
                QuestGraphModel.Node n = model.hit(g[0], g[1]);
                if (n == null) {
                    return;
                }
                if (n.kind() == QuestGraphModel.Kind.GIVER || n.kind() == QuestGraphModel.Kind.TURN_IN) {
                    jumpToMap.run();
                } else if (n.kind() == QuestGraphModel.Kind.QUEST) {
                    inspectQuest.accept(n.questId());
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                model.zoomBy(e.getWheelRotation() < 0 ? 1.1 : 0.9);
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    public void setModel(QuestGraphModel model) {
        this.model = model == null ? new QuestGraphModel() : model;
        repaint();
    }

    public QuestGraphModel model() {
        return model;
    }

    public JPopupMenu contextMenu() {
        return contextMenu;
    }

    public void armPrerequisite(int questId) {
        this.prerequisiteCandidate = questId;
    }

    public void armTurnIn(int npc) {
        this.turnInCandidate = npc;
    }

    public void setCreateChild(Runnable createChild) {
        this.createChild = createChild == null ? () -> {} : createChild;
    }

    public void setJumpToMap(Runnable jumpToMap) {
        this.jumpToMap = jumpToMap == null ? () -> {} : jumpToMap;
    }

    public void setInspectQuest(IntConsumer inspectQuest) {
        this.inspectQuest = inspectQuest == null ? id -> {} : inspectQuest;
    }

    private boolean selectAt(int x, int y) {
        double[] g = toGraph(x, y);
        QuestGraphModel.Node n = model.hit(g[0], g[1]);
        if (n == null) {
            model.select("");
            return false;
        }
        model.select(n.id());
        if (n.kind() == QuestGraphModel.Kind.QUEST) {
            if (model.focus() == null || n.questId() != model.focus().id()) {
                prerequisiteCandidate = n.questId();
            }
            inspectQuest.accept(n.questId());
        }
        return true;
    }

    private JPopupMenu buildMenu() {
        JPopupMenu menu = new JPopupMenu();
        item(menu, "Connect as prerequisite", () -> {
            if (model.focus() != null && prerequisiteCandidate != 0) {
                model.linkPrev(prerequisiteCandidate);
            }
            repaint();
        });
        item(menu, "Link turn-in", () -> {
            if (turnInCandidate != 0) {
                model.setTurnIn(turnInCandidate);
            }
            repaint();
        });
        item(menu, "Create child quest", () -> createChild.run());
        item(menu, "Detach prev", () -> {
            model.detachPrev();
            repaint();
        });
        item(menu, "Hide branch", () -> {
            model.collapseSelected();
            repaint();
        });
        return menu;
    }

    private static void item(JPopupMenu menu, String text, Runnable action) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    private double[] toGraph(int x, int y) {
        double zoom = viewZoom();
        double gx = (x - getWidth() / 2.0 - viewPanX()) / zoom;
        double gy = (y - getHeight() / 2.0 - viewPanY()) / zoom;
        return new double[] {gx, gy};
    }

    private double viewPanX() {
        return model.focus() == null ? 0 : model.focus().editor().graph().panX();
    }

    private double viewPanY() {
        return model.focus() == null ? 0 : model.focus().editor().graph().panY();
    }

    private double viewZoom() {
        double z = model.focus() == null ? 1 : model.focus().editor().graph().zoom();
        return z <= 0 ? 1 : z;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double zoom = viewZoom();
        double ox = getWidth() / 2.0 + viewPanX();
        double oy = getHeight() / 2.0 + viewPanY();
        g2.setStroke(new BasicStroke(1.5f));
        for (QuestGraphModel.Edge e : model.edges()) {
            QuestGraphModel.Node a = model.node(e.from());
            QuestGraphModel.Node b = model.node(e.to());
            if (a == null || b == null) {
                continue;
            }
            int x1 = (int) (ox + a.x() * zoom);
            int y1 = (int) (oy + a.y() * zoom);
            int x2 = (int) (ox + b.x() * zoom);
            int y2 = (int) (oy + b.y() * zoom);
            int cx = (x1 + x2) / 2;
            int cy = (y1 + y2) / 2 - 16;
            g2.setColor(new Color(140, 150, 160));
            g2.draw(new java.awt.geom.QuadCurve2D.Float(x1, y1, cx, cy, x2, y2));
        }
        for (QuestGraphModel.Node n : model.nodes()) {
            int x = (int) (ox + n.x() * zoom);
            int y = (int) (oy + n.y() * zoom);
            g2.setColor(fill(n));
            g2.fillRoundRect(x - 64, y - 22, 128, 44, 12, 12);
            g2.setColor(n.id().equals(model.selectedId()) ? Color.WHITE : new Color(40, 44, 52));
            g2.drawRoundRect(x - 64, y - 22, 128, 44, 12, 12);
            g2.setColor(Color.WHITE);
            String title = n.title();
            if (title.length() > 18) {
                title = title.substring(0, 17) + "…";
            }
            g2.drawString(title, x - 56, y + 4);
        }
    }

    private static Color fill(QuestGraphModel.Node n) {
        if (n.issue()) {
            return new Color(140, 64, 48);
        }
        return switch (n.kind()) {
            case QUEST -> new Color(48, 96, 140);
            case GIVER -> new Color(48, 120, 72);
            case TURN_IN -> new Color(120, 96, 48);
            case OBJECTIVE -> new Color(72, 72, 110);
            case NOTE -> new Color(80, 80, 80);
        };
    }
}
