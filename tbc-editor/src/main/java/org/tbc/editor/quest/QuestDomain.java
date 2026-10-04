package org.tbc.editor.quest;

import org.tbc.editor.EditorDomain;
import org.tbc.editor.EditorException;
import org.tbc.world.map.RegionMinimap;
import org.tbc.world.map.Terrain;
import org.tbc.world.map.WorldMapArea;
import org.tbc.world.map.WorldMapAreas;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;
import java.util.function.Function;

/** Quest authoring domain: named map, canvas, inspector, draft/publish. */
public final class QuestDomain implements EditorDomain {
    private final QuestService service;
    private final Consumer<String> status;
    private final WorldMapAreas maps;
    private final Function<WorldMapArea, RegionMinimap.Raster> raster;
    private final JPanel root = new JPanel(new BorderLayout());
    private final QuestMapCanvas canvas = new QuestMapCanvas();
    private final JComboBox<WorldMapArea> mapCombo;
    private final JTextField idField = new JTextField("95001", 8);
    private final JTextField title = new JTextField(20);
    private final JTextArea details = new JTextArea(4, 20);
    private final JTextField giver = new JTextField(8);
    private final JTextField turnIn = new JTextField(8);
    private final JTextArea issues = new JTextArea(8, 28);
    private final JTextArea preview = new JTextArea(8, 28);
    private final JCheckBox ackDup = new JCheckBox("Acknowledge near-duplicate spawns");
    private final JComboBox<QuestMapModel.Tool> tools = new JComboBox<>(QuestMapModel.Tool.values());
    private QuestDocument doc;

    public QuestDomain(QuestService service, Consumer<String> status) {
        this(service, status, WorldMapAreas.seeded(),
                area -> RegionMinimap.render(area, Terrain.NONE, 256, 256));
    }

    public QuestDomain(QuestService service, Consumer<String> status, WorldMapAreas maps,
                       Function<WorldMapArea, RegionMinimap.Raster> raster) {
        this.service = service;
        this.status = status;
        this.maps = maps == null ? WorldMapAreas.seeded() : maps;
        this.raster = raster == null
                ? area -> RegionMinimap.render(area, Terrain.NONE, 256, 256)
                : raster;
        this.doc = service.create(95001);
        this.mapCombo = new JComboBox<>(this.maps.list().toArray(WorldMapArea[]::new));
        this.mapCombo.setEditable(true);
        issues.setEditable(false);
        preview.setEditable(false);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton newBtn = new JButton("New");
        JButton saveBtn = new JButton("Save draft");
        JButton loadBtn = new JButton("Load draft");
        JButton validateBtn = new JButton("Validate");
        JButton publishBtn = new JButton("Publish");
        toolbar.add(new JLabel("Id"));
        toolbar.add(idField);
        toolbar.add(newBtn);
        toolbar.add(saveBtn);
        toolbar.add(loadBtn);
        toolbar.add(validateBtn);
        toolbar.add(publishBtn);
        toolbar.add(ackDup);
        toolbar.add(new JLabel("Map"));
        toolbar.add(mapCombo);
        toolbar.add(new JLabel("Tool"));
        toolbar.add(tools);
        newBtn.addActionListener(e -> newDoc());
        saveBtn.addActionListener(e -> save());
        loadBtn.addActionListener(e -> load());
        validateBtn.addActionListener(e -> refreshValidation());
        publishBtn.addActionListener(e -> publish());
        tools.addActionListener(e -> canvas.model().setTool((QuestMapModel.Tool) tools.getSelectedItem()));
        mapCombo.addActionListener(e -> applySelectedMap());
        canvas.setHoverListener(status);

        JPanel inspector = new JPanel(new GridLayout(0, 2, 4, 4));
        inspector.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        inspector.add(new JLabel("Title"));
        inspector.add(title);
        inspector.add(new JLabel("Giver NPC"));
        inspector.add(giver);
        inspector.add(new JLabel("Turn-in NPC"));
        inspector.add(turnIn);
        inspector.add(new JLabel("Details"));
        inspector.add(new JScrollPane(details));

        JPanel east = new JPanel(new BorderLayout());
        east.add(inspector, BorderLayout.NORTH);
        JSplitPane reports = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(issues), new JScrollPane(preview));
        reports.setResizeWeight(0.5);
        east.add(reports, BorderLayout.CENTER);
        east.setPreferredSize(new Dimension(360, 0));

        JList<String> lookup = new JList<>();
        JTextField lookupQ = new JTextField();
        JButton lookupBtn = new JButton("Lookup NPC");
        lookupBtn.addActionListener(e -> {
            var hits = service.lookupCreatures(lookupQ.getText());
            String[] rows = hits.stream()
                    .map(h -> h.entry() + " " + h.name() + " fac=" + h.faction())
                    .toArray(String[]::new);
            lookup.setListData(rows);
        });
        JPanel south = new JPanel(new BorderLayout());
        JPanel lookBar = new JPanel(new BorderLayout());
        lookBar.add(lookupQ, BorderLayout.CENTER);
        lookBar.add(lookupBtn, BorderLayout.EAST);
        south.add(lookBar, BorderLayout.NORTH);
        south.add(new JScrollPane(lookup), BorderLayout.CENTER);
        east.add(south, BorderLayout.SOUTH);

        root.add(toolbar, BorderLayout.NORTH);
        root.add(canvas, BorderLayout.CENTER);
        root.add(east, BorderLayout.EAST);
        pullFromDoc();
        applySelectedMap();
    }

    @Override
    public String title() {
        return "Quests";
    }

    @Override
    public JComponent view() {
        return root;
    }

    public QuestDocument document() {
        return doc;
    }

    public QuestMapCanvas canvas() {
        return canvas;
    }

    public void selectNamedMap(String displayName) {
        WorldMapArea area = maps.byDisplayName(displayName);
        if (area == null) {
            status.accept("Unknown map: " + displayName);
            return;
        }
        mapCombo.setSelectedItem(area);
        applySelectedMap();
    }

    private void applySelectedMap() {
        WorldMapArea area = selectedArea();
        if (area == null) {
            return;
        }
        doc.setMapId(area.mapId());
        doc.setZoneOrSort(area.areaId());
        RegionMinimap.Raster image = raster.apply(area);
        canvas.loadRegion(area, image);
        boolean missing = image == null || image.empty()
                || allDark(image);
        status.accept(missing
                ? area.displayName() + " — no terrain tiles (coordinates still mapped)"
                : area.displayName());
    }

    private static boolean allDark(RegionMinimap.Raster image) {
        int dark = 0xFF202428;
        for (int c : image.argb()) {
            if (c != dark && c != 0) {
                return false;
            }
        }
        return true;
    }

    private WorldMapArea selectedArea() {
        Object sel = mapCombo.getSelectedItem();
        if (sel instanceof WorldMapArea area) {
            return area;
        }
        if (sel != null) {
            return maps.byDisplayName(sel.toString());
        }
        return null;
    }

    private void newDoc() {
        try {
            doc = service.create(Integer.parseInt(idField.getText().trim()));
            pullFromDoc();
            applySelectedMap();
            status.accept("New quest " + doc.id());
        } catch (Exception ex) {
            status.accept(message(ex));
        }
    }

    private void save() {
        try {
            pushToDoc();
            service.saveDraft(doc);
            status.accept("Draft saved.");
        } catch (Exception ex) {
            status.accept(message(ex));
        }
    }

    private void load() {
        try {
            doc = service.loadDraft(Integer.parseInt(idField.getText().trim()));
            pullFromDoc();
            applySelectedMap();
            status.accept("Draft loaded.");
        } catch (Exception ex) {
            status.accept(message(ex));
        }
    }

    private void refreshValidation() {
        pushToDoc();
        QuestValidator.Report r = service.validate(doc);
        StringBuilder sb = new StringBuilder();
        for (QuestValidator.Issue i : r.issues()) {
            sb.append(i.severity()).append(" ");
            if (i.markerId() != null) {
                sb.append("[").append(i.markerId()).append("] ");
            }
            sb.append(i.message()).append('\n');
        }
        issues.setText(sb.toString());
        preview.setText(service.publishDiff(doc));
    }

    private void publish() {
        try {
            pushToDoc();
            service.publish(doc, ackDup.isSelected());
            preview.setText(service.previewRuntimeYaml(doc));
            status.accept("Published " + doc.id());
        } catch (Exception ex) {
            status.accept(message(ex));
        }
    }

    private void pullFromDoc() {
        idField.setText(Integer.toString(doc.id()));
        title.setText(doc.title());
        details.setText(doc.details());
        giver.setText(Integer.toString(doc.giverNpc()));
        turnIn.setText(Integer.toString(doc.turnInNpc()));
        WorldMapArea hit = maps.byAreaId(doc.zoneOrSort());
        if (hit == null) {
            hit = maps.byMapAndArea(doc.mapId(), doc.zoneOrSort());
        }
        if (hit != null) {
            mapCombo.setSelectedItem(hit);
        }
    }

    private void pushToDoc() {
        doc.setTitle(title.getText());
        doc.setDetails(details.getText());
        doc.setObjectives(details.getText());
        WorldMapArea area = selectedArea();
        if (area != null) {
            doc.setMapId(area.mapId());
            doc.setZoneOrSort(area.areaId());
        }
        doc.setGiverNpc(parseInt(giver.getText()));
        doc.setTurnInNpc(parseInt(turnIn.getText()));
        doc.setMinLevel(Math.max(1, doc.minLevel()));
        doc.setQuestLevel(Math.max(1, doc.questLevel()));
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String message(Exception ex) {
        if (ex instanceof EditorException) {
            return ex.getMessage();
        }
        Throwable c = ex.getCause() == null ? ex : ex.getCause();
        return c.getMessage() == null ? c.getClass().getSimpleName() : c.getMessage();
    }
}
