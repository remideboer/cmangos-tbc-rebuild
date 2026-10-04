package org.tbc.editor.quest;

import org.tbc.editor.EditorDomain;
import org.tbc.editor.EditorException;
import org.tbc.world.map.RegionMinimap;
import org.tbc.world.map.Terrain;
import org.tbc.world.map.WorldMapArea;
import org.tbc.world.map.WorldMapAreas;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final QuestGraphModel graphModel = new QuestGraphModel();
    private final QuestGraphCanvas graphCanvas = new QuestGraphCanvas();
    private final JTabbedPane tabs = new JTabbedPane();
    private final JComboBox<WorldMapArea> mapCombo;
    private final JLabel zoneLabel = new JLabel("Zone");
    private final JTextField idField = new JTextField("95001", 8);
    private final JTextField title = new JTextField(20);
    private final JTextArea details = new JTextArea(4, 20);
    private final JTextField giver = new JTextField(8);
    private final JTextField turnIn = new JTextField(8);
    private final JTextField prevQuest = new JTextField(8);
    private final JLabel idCaption = new JLabel(" ");
    private final JButton copyDraft = new JButton("Copy as draft");
    private final DefaultListModel<QuestService.ZoneQuest> zoneQuestModel = new DefaultListModel<>();
    private final JList<QuestService.ZoneQuest> zoneQuestList = new JList<>(zoneQuestModel);
    private final DefaultListModel<QuestService.ZoneNpc> zoneNpcModel = new DefaultListModel<>();
    private final JList<QuestService.ZoneNpc> zoneNpcList = new JList<>(zoneNpcModel);
    private final DefaultListModel<QuestService.CreatureHit> searchModel = new DefaultListModel<>();
    private final JList<QuestService.CreatureHit> searchList = new JList<>(searchModel);
    private final JTextField creatureQuery = new JTextField(12);
    private final JTextField minLevel = new JTextField(4);
    private final JTextField maxLevel = new JTextField(4);
    private final JTextField factionFilter = new JTextField(6);
    private final JComboBox<TypeChoice> creatureType = new JComboBox<>(TypeChoice.values());
    private final JCheckBox zoneOnly = new JCheckBox("This zone only", true);
    private boolean catalogView;
    private boolean refreshingLists;
    private int catalogQuestId;
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
        this.mapCombo.setPrototypeDisplayValue(WorldMapAreas.ELWYNN);
        this.mapCombo.setPreferredSize(new Dimension(280, 24));
        this.mapCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                                   boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof WorldMapArea area) {
                    setText(area.displayName());
                }
                return this;
            }
        });
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
        JPanel zoneBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        zoneBar.add(zoneLabel);
        zoneBar.add(mapCombo);
        zoneBar.add(new JLabel("Tool"));
        zoneBar.add(tools);
        JCheckBox showSpawns = new JCheckBox("Spawns", true);
        showSpawns.addActionListener(e -> canvas.setShowSpawns(showSpawns.isSelected()));
        zoneBar.add(showSpawns);
        JPanel north = new JPanel(new BorderLayout());
        north.add(toolbar, BorderLayout.NORTH);
        north.add(zoneBar, BorderLayout.SOUTH);
        newBtn.addActionListener(e -> newDoc());
        saveBtn.addActionListener(e -> save());
        loadBtn.addActionListener(e -> load());
        validateBtn.addActionListener(e -> refreshValidation());
        publishBtn.addActionListener(e -> publish());
        tools.addActionListener(e -> canvas.model().setTool((QuestMapModel.Tool) tools.getSelectedItem()));
        mapCombo.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                applySelectedMap();
            }
        });
        mapCombo.getEditor().addActionListener(e -> applySelectedMap());
        canvas.setHoverListener(status);

        zoneQuestList.setVisibleRowCount(6);
        zoneNpcList.setVisibleRowCount(6);
        searchList.setVisibleRowCount(6);
        searchList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                   boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof QuestService.CreatureHit hit) {
                    setText(hit.label());
                }
                return this;
            }
        });
        zoneQuestList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !refreshingLists) {
                openZoneQuest(zoneQuestList.getSelectedValue());
            }
        });
        copyDraft.setEnabled(false);
        copyDraft.addActionListener(e -> copySelectedCatalog());

        JPanel thisQuest = new JPanel(new GridLayout(0, 2, 4, 4));
        thisQuest.add(new JLabel("Title"));
        thisQuest.add(title);
        thisQuest.add(new JLabel("Id"));
        thisQuest.add(idCaption);
        thisQuest.add(new JLabel("Previous quest"));
        thisQuest.add(prevQuest);
        thisQuest.add(new JLabel("Details"));
        thisQuest.add(new JScrollPane(details));
        thisQuest.add(new JLabel(" "));
        thisQuest.add(copyDraft);

        JPanel who = new JPanel(new GridLayout(0, 2, 4, 4));
        who.add(new JLabel("Giver"));
        who.add(giver);
        who.add(new JLabel("Turn-in"));
        who.add(turnIn);

        JPanel zoneLists = new JPanel(new GridLayout(2, 1, 4, 4));
        zoneLists.add(labeledScroll("Quests", zoneQuestList));
        JPanel npcBox = new JPanel(new BorderLayout());
        npcBox.add(labeledScroll("NPCs", zoneNpcList), BorderLayout.CENTER);
        JPanel npcButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton asGiver = new JButton("Use as giver");
        JButton asTurnIn = new JButton("Use as turn-in");
        asGiver.addActionListener(e -> useNpc(zoneNpcList.getSelectedValue(), true));
        asTurnIn.addActionListener(e -> useNpc(zoneNpcList.getSelectedValue(), false));
        npcButtons.add(asGiver);
        npcButtons.add(asTurnIn);
        npcBox.add(npcButtons, BorderLayout.SOUTH);
        zoneLists.add(npcBox);

        JPanel find = new JPanel(new BorderLayout());
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(creatureQuery);
        filters.add(creatureType);
        filters.add(new JLabel("Lv"));
        filters.add(minLevel);
        filters.add(new JLabel("–"));
        filters.add(maxLevel);
        filters.add(new JLabel("Faction"));
        filters.add(factionFilter);
        filters.add(zoneOnly);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> runCreatureSearch());
        filters.add(searchBtn);
        find.add(filters, BorderLayout.NORTH);
        find.add(new JScrollPane(searchList), BorderLayout.CENTER);
        JPanel findButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton searchGiver = new JButton("Use as giver");
        JButton searchTurn = new JButton("Use as turn-in");
        JButton searchKill = new JButton("Add as kill objective");
        searchGiver.addActionListener(e -> useSearch(true));
        searchTurn.addActionListener(e -> useSearch(false));
        searchKill.addActionListener(e -> addKillFromSearch());
        findButtons.add(searchGiver);
        findButtons.add(searchTurn);
        findButtons.add(searchKill);
        find.add(findButtons, BorderLayout.SOUTH);

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.add(section("This quest", thisQuest));
        stack.add(section("Who", who));
        stack.add(section("In this zone", zoneLists));
        stack.add(section("Find a creature", find));

        JPanel east = new JPanel(new BorderLayout());
        east.add(new JScrollPane(stack), BorderLayout.CENTER);
        JSplitPane reports = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                section("Checks", new JScrollPane(issues)), new JScrollPane(preview));
        reports.setResizeWeight(0.6);
        reports.setPreferredSize(new Dimension(420, 180));
        east.add(reports, BorderLayout.SOUTH);
        east.setPreferredSize(new Dimension(420, 0));

        graphCanvas.setModel(graphModel);
        graphCanvas.setJumpToMap(() -> tabs.setSelectedIndex(0));
        graphCanvas.setInspectQuest(this::showQuest);
        graphCanvas.setCreateChild(this::createChild);
        JPanel graphBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton fitGraph = new JButton("Fit");
        JButton collapseGraph = new JButton("Collapse");
        fitGraph.addActionListener(e -> {
            graphModel.fit();
            graphCanvas.repaint();
        });
        collapseGraph.addActionListener(e -> {
            graphModel.collapseSelected();
            graphCanvas.repaint();
        });
        graphBar.add(fitGraph);
        graphBar.add(collapseGraph);
        JPanel graphPage = new JPanel(new BorderLayout());
        graphPage.add(graphBar, BorderLayout.NORTH);
        graphPage.add(graphCanvas, BorderLayout.CENTER);
        tabs.addTab("Map", canvas);
        tabs.addTab("Graph", graphPage);

        root.add(north, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
        root.add(east, BorderLayout.EAST);
        pullFromDoc();
        applySelectedMap();
        rebuildGraph();
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

    public JComboBox<WorldMapArea> zoneCombo() {
        return mapCombo;
    }

    public JLabel zoneLabel() {
        return zoneLabel;
    }

    public String tabTitle(int index) {
        return tabs.getTitleAt(index);
    }

    public QuestGraphCanvas graphCanvas() {
        return graphCanvas;
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
        Object sel = mapCombo.getSelectedItem();
        WorldMapArea area = selectedArea();
        if (area == null) {
            if (sel != null && !sel.toString().isBlank()) {
                status.accept("Unknown map: " + sel);
            }
            return;
        }
        doc.setMapId(area.mapId());
        doc.setZoneOrSort(area.areaId());
        RegionMinimap.Raster image = raster.apply(area);
        canvas.loadRegion(area, image);
        canvas.setSpawns(MapSpawnLayer.inArea(service.creatures(), area));
        refreshZoneLists();
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
            rebuildGraph();
            status.accept("New quest " + doc.id());
        } catch (Exception ex) {
            status.accept(message(ex));
        }
    }

    private void save() {
        if (catalogView) {
            status.accept("Catalog quests are read-only. Copy as draft first.");
            return;
        }
        try {
            pushToDoc();
            service.saveDraft(doc);
            rebuildGraph();
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
            rebuildGraph();
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
        Set<Integer> flagged = new HashSet<>();
        for (QuestValidator.Issue i : r.issues()) {
            if (i.markerId() != null && i.markerId().startsWith("quest:")) {
                flagged.add(parseInt(i.markerId().substring("quest:".length())));
            }
        }
        graphModel.markIssues(flagged);
        graphCanvas.repaint();
    }

    private void publish() {
        if (catalogView) {
            status.accept("Catalog quests are read-only. Copy as draft first.");
            return;
        }
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
        idCaption.setText(Integer.toString(doc.id()));
        title.setText(doc.title());
        details.setText(doc.details());
        giver.setText(Integer.toString(doc.giverNpc()));
        turnIn.setText(Integer.toString(doc.turnInNpc()));
        prevQuest.setText(Integer.toString(doc.prevQuestId()));
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
        doc.setPrevQuestId(parseInt(prevQuest.getText()));
        doc.setMinLevel(Math.max(1, doc.minLevel()));
        doc.setQuestLevel(Math.max(1, doc.questLevel()));
    }

    private void showQuest(int questId) {
        if (questId == doc.id()) {
            title.requestFocusInWindow();
            return;
        }
        pushToDoc();
        for (QuestDocument other : loadChain()) {
            if (other.id() == questId) {
                doc = other;
                pullFromDoc();
                rebuildGraph();
                title.requestFocusInWindow();
                return;
            }
        }
    }

    private void createChild() {
        try {
            pushToDoc();
            int next = nextOwnedId();
            QuestDocument child = service.create(next);
            child.setPrevQuestId(doc.id());
            child.setTitle("Follow-up " + next);
            child.setMapId(doc.mapId());
            child.setZoneOrSort(doc.zoneOrSort());
            service.saveDraft(child);
            rebuildGraph();
            status.accept("Child quest " + next + " (draft, not published)");
        } catch (Exception ex) {
            status.accept(message(ex));
        }
    }

    private int nextOwnedId() {
        int next = QuestDocument.MIN_OWNED_ID;
        for (QuestDocument other : loadChain()) {
            if (other.id() >= next) {
                next = other.id() + 1;
            }
        }
        if (next > QuestDocument.MAX_OWNED_ID) {
            throw new EditorException("No free quest id in the server-owned range.");
        }
        return next;
    }

    private void rebuildGraph() {
        int npc = parseInt(turnIn.getText());
        if (npc == 0) {
            npc = parseInt(giver.getText());
        }
        graphCanvas.armTurnIn(npc);
        graphCanvas.armPrerequisite(parseInt(prevQuest.getText()));
        graphModel.setCreatureNames(creatureNames());
        graphModel.rebuild(doc, loadChain());
        graphCanvas.repaint();
    }

    private List<QuestDocument> loadChain() {
        List<QuestDocument> chain = new ArrayList<>();
        try {
            for (QuestDocument other : service.store().loadAllDrafts()) {
                chain.add(other.id() == doc.id() ? doc : other);
            }
        } catch (RuntimeException ignored) {
            chain.clear();
        }
        if (chain.stream().noneMatch(d -> d.id() == doc.id())) {
            chain.add(doc);
        }
        return chain;
    }

    private void refreshZoneLists() {
        refreshingLists = true;
        zoneQuestModel.clear();
        zoneNpcModel.clear();
        WorldMapArea area = selectedArea();
        if (area != null) {
            for (QuestService.ZoneQuest q : service.questsInArea(area)) {
                zoneQuestModel.addElement(q);
            }
            for (QuestService.ZoneNpc n : service.npcsInArea(area)) {
                zoneNpcModel.addElement(n);
            }
        }
        refreshingLists = false;
    }

    private void openZoneQuest(QuestService.ZoneQuest row) {
        if (row == null) {
            return;
        }
        if (row.draft()) {
            try {
                catalogView = false;
                catalogQuestId = 0;
                copyDraft.setEnabled(false);
                doc = service.loadDraft(row.id());
                pullFromDoc();
                applySelectedMap();
                status.accept("Draft " + row.id());
            } catch (Exception ex) {
                status.accept(message(ex));
            }
            return;
        }
        catalogView = true;
        catalogQuestId = row.id();
        copyDraft.setEnabled(true);
        QuestDocument preview = service.copyCatalogQuest(row.id(), QuestDocument.MIN_OWNED_ID);
        title.setText(preview.title());
        details.setText(preview.details());
        giver.setText(Integer.toString(preview.giverNpc()));
        turnIn.setText(Integer.toString(preview.turnInNpc()));
        prevQuest.setText(Integer.toString(preview.prevQuestId()));
        idCaption.setText(row.id() + " catalog");
        status.accept(row.title() + " is a catalog quest. Copy as draft to edit it.");
    }

    private void copySelectedCatalog() {
        if (!catalogView || catalogQuestId == 0) {
            return;
        }
        try {
            int id = service.nextOwnedId();
            if (doc != null && id == doc.id()) {
                id++;
            }
            doc = service.copyCatalogQuest(catalogQuestId, id);
            WorldMapArea area = selectedArea();
            if (area != null && doc.mapId() == 0) {
                doc.setMapId(area.mapId());
            }
            catalogView = false;
            catalogQuestId = 0;
            copyDraft.setEnabled(false);
            pullFromDoc();
            rebuildGraph();
            status.accept("Draft " + doc.id() + " (not published)");
        } catch (Exception ex) {
            status.accept(message(ex));
        }
    }

    private void useNpc(QuestService.ZoneNpc npc, boolean giverRole) {
        if (npc == null || catalogView) {
            status.accept(catalogView ? "Copy as draft before assigning an NPC." : "Select an NPC in this zone.");
            return;
        }
        pushToDoc();
        if (giverRole) {
            doc.setGiverNpc(npc.entry());
            giver.setText(Integer.toString(npc.entry()));
        } else {
            doc.setTurnInNpc(npc.entry());
            turnIn.setText(Integer.toString(npc.entry()));
        }
        rebuildGraph();
    }

    private void useSearch(boolean giverRole) {
        QuestService.CreatureHit hit = searchList.getSelectedValue();
        if (hit == null) {
            return;
        }
        useNpc(new QuestService.ZoneNpc(hit.entry(), hit.name(), hit.level(), hit.faction()), giverRole);
    }

    private void addKillFromSearch() {
        QuestService.CreatureHit hit = searchList.getSelectedValue();
        if (hit == null || catalogView) {
            return;
        }
        pushToDoc();
        for (int i = 0; i < 4; i++) {
            if (doc.reqCreatureId(i) == 0) {
                doc.setReqCreature(i, hit.entry(), 1);
                rebuildGraph();
                status.accept("Kill objective " + hit.name());
                return;
            }
        }
        status.accept("All four kill objectives are already set.");
    }

    private void runCreatureSearch() {
        WorldMapArea zone = zoneOnly.isSelected() ? selectedArea() : null;
        Integer min = blankToNull(minLevel.getText());
        Integer max = blankToNull(maxLevel.getText());
        Integer faction = blankToNull(factionFilter.getText());
        TypeChoice type = (TypeChoice) creatureType.getSelectedItem();
        int typeId = type == null ? -1 : type.type;
        List<QuestService.CreatureHit> hits = service.searchCreatures(new QuestService.CreatureQuery(
                creatureQuery.getText(), typeId, min, max, faction, zone));
        searchModel.clear();
        for (QuestService.CreatureHit hit : hits) {
            searchModel.addElement(hit);
        }
        status.accept(hits.size() + " creatures");
    }

    private Map<Integer, String> creatureNames() {
        Map<Integer, String> names = new HashMap<>();
        remember(names, doc.giverNpc());
        remember(names, doc.turnInNpc());
        for (int i = 0; i < 4; i++) {
            remember(names, doc.reqCreatureId(i));
        }
        return names;
    }

    private void remember(Map<Integer, String> names, int entry) {
        String name = service.creatureName(entry);
        if (name != null) {
            names.put(entry, name);
        }
    }

    private static Integer blankToNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static JPanel section(String title, java.awt.Component body) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private static JScrollPane labeledScroll(String title, JList<?> list) {
        JScrollPane pane = new JScrollPane(list);
        pane.setBorder(BorderFactory.createTitledBorder(title));
        return pane;
    }

    private enum TypeChoice {
        ANY(-1, "Any type"),
        BEAST(1, "Beast"),
        DRAGONKIN(2, "Dragonkin"),
        DEMON(3, "Demon"),
        ELEMENTAL(4, "Elemental"),
        GIANT(5, "Giant"),
        UNDEAD(6, "Undead"),
        HUMANOID(7, "Humanoid"),
        CRITTER(8, "Critter"),
        MECHANICAL(9, "Mechanical"),
        TOTEM(11, "Totem"),
        PET(12, "Non-combat Pet"),
        GAS(13, "Gas Cloud");

        final int type;
        final String label;

        TypeChoice(int type, String label) {
            this.type = type;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
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
