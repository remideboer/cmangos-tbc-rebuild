package org.tbc.editor;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Nested master–detail: accounts, character table, scalar form. */
public final class CharacterDomain implements EditorDomain {
    private static final String[] COLS = {
            "Guid", "Account", "Name", "Race", "Class", "Level", "Map", "Online"
    };
    private static final int[] PLAYABLE_CLASSES = {1, 2, 3, 4, 5, 7, 8, 9, 11};

    private final CharacterService service;
    private final Consumer<String> status;
    private final JPanel root = new JPanel(new BorderLayout());
    private final JTextField search = new JTextField(12);
    private final DefaultListModel<AccountRef> accountModel = new DefaultListModel<>();
    private final JList<AccountRef> accountList = new JList<>(accountModel);
    private final DefaultTableModel tableModel = new DefaultTableModel(COLS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel raceLabel = new JLabel(" ");
    private final JLabel classLabel = new JLabel(" ");
    private final JTextField name = field();
    private final JComboBox<String> gender = new JComboBox<>(new String[]{"Male", "Female"});
    private final JTextField level = field();
    private final JTextField xp = field();
    private final JTextField money = field();
    private final JTextField map = field();
    private final JTextField zone = field();
    private final JTextField x = field();
    private final JTextField y = field();
    private final JTextField z = field();
    private final JTextField o = field();
    private final JTextField bindMap = field();
    private final JTextField bindZone = field();
    private final JTextField bindX = field();
    private final JTextField bindY = field();
    private final JTextField bindZ = field();
    private final JTextField skin = field();
    private final JTextField face = field();
    private final JTextField hair = field();
    private final JTextField hairColor = field();
    private final JTextField facial = field();
    private final JTextField atLogin = field();
    private final JTextField cinematic = field();
    private final JButton newBtn = new JButton("New");
    private final JButton saveBtn = new JButton("Save");
    private final JButton revertBtn = new JButton("Revert");
    private final JButton deleteBtn = new JButton("Delete");
    private final JButton searchBtn = new JButton("Search");
    private final JButton allBtn = new JButton("All characters");
    private CharacterDetail loaded;
    private CharacterDraft snapshot;
    private boolean ignoreTable;
    private boolean ignoreAccount;
    private int lastTableRow = -1;
    private int lastAccountIndex = -1;

    public CharacterDomain(CharacterService service, Consumer<String> status) {
        this.service = service;
        this.status = status;
        accountList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountList.setCellRenderer((list, value, index, selected, focus) -> {
            JLabel l = new JLabel(value == null ? "" : value.username());
            l.setOpaque(true);
            if (selected) {
                l.setBackground(list.getSelectionBackground());
                l.setForeground(list.getSelectionForeground());
            } else {
                l.setBackground(list.getBackground());
                l.setForeground(list.getForeground());
            }
            return l;
        });
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        JPanel left = new JPanel(new BorderLayout(4, 4));
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        searchRow.add(new JLabel("Search"));
        search.setPreferredSize(new Dimension(120, search.getPreferredSize().height));
        searchRow.add(search);
        searchRow.add(searchBtn);
        JPanel leftNorth = new JPanel(new BorderLayout());
        leftNorth.add(searchRow, BorderLayout.NORTH);
        leftNorth.add(allBtn, BorderLayout.SOUTH);
        left.add(leftNorth, BorderLayout.NORTH);
        left.add(new JScrollPane(accountList), BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(200, 0));
        JPanel form = buildForm();
        JSplitPane midRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(table), form);
        midRight.setResizeWeight(0.45);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, midRight);
        split.setResizeWeight(0.18);
        root.add(split, BorderLayout.CENTER);
        searchBtn.addActionListener(e -> searchNow());
        search.addActionListener(e -> searchNow());
        allBtn.addActionListener(e -> {
            if (!confirmLeave()) {
                return;
            }
            search.setText("");
            ignoreAccount = true;
            accountList.clearSelection();
            lastAccountIndex = -1;
            ignoreAccount = false;
            reload(true);
        });
        accountList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || ignoreAccount) {
                return;
            }
            if (!confirmLeave()) {
                restoreAccountSelection();
                return;
            }
            lastAccountIndex = accountList.getSelectedIndex();
            reload(false);
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || ignoreTable) {
                return;
            }
            if (!confirmLeave()) {
                restoreTableSelection();
                return;
            }
            lastTableRow = table.getSelectedRow();
            Long guid = selectedGuid();
            if (guid == null) {
                clearForm();
                return;
            }
            run("Loading character…", () -> service.get(guid), this::showDetail);
        });
        newBtn.addActionListener(e -> newCharacter());
        saveBtn.addActionListener(e -> save());
        revertBtn.addActionListener(e -> revert());
        deleteBtn.addActionListener(e -> deleteSelected());
        DocumentListener dirty = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateButtons();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateButtons();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateButtons();
            }
        };
        for (JTextField f : new JTextField[]{
                name, level, xp, money, map, zone, x, y, z, o,
                bindMap, bindZone, bindX, bindY, bindZ,
                skin, face, hair, hairColor, facial, atLogin, cinematic}) {
            f.getDocument().addDocumentListener(dirty);
        }
        gender.addActionListener(e -> updateButtons());
        reload(true);
    }

    @Override
    public String title() {
        return "Characters";
    }

    @Override
    public JComponent view() {
        return root;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new BorderLayout());
        JPanel fields = fields(
                "Race", raceLabel,
                "Class", classLabel,
                "Name", name,
                "Gender", gender,
                "Level", level,
                "XP", xp,
                "Money", money,
                "Map", map,
                "Zone", zone,
                "X", x,
                "Y", y,
                "Z", z,
                "O", o,
                "Bind map", bindMap,
                "Bind zone", bindZone,
                "Bind X", bindX,
                "Bind Y", bindY,
                "Bind Z", bindZ,
                "Skin", skin,
                "Face", face,
                "Hair", hair,
                "Hair color", hairColor,
                "Facial hair", facial,
                "At login", atLogin,
                "Cinematic", cinematic);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(newBtn);
        buttons.add(saveBtn);
        buttons.add(revertBtn);
        buttons.add(deleteBtn);
        form.add(new JScrollPane(fields), BorderLayout.CENTER);
        form.add(buttons, BorderLayout.SOUTH);
        form.setPreferredSize(new Dimension(320, 0));
        return form;
    }

    private void searchNow() {
        if (!confirmLeave()) {
            return;
        }
        reload(true);
    }

    private void restoreAccountSelection() {
        ignoreAccount = true;
        if (lastAccountIndex >= 0 && lastAccountIndex < accountModel.size()) {
            accountList.setSelectedIndex(lastAccountIndex);
        } else {
            accountList.clearSelection();
        }
        ignoreAccount = false;
    }

    private void restoreTableSelection() {
        ignoreTable = true;
        if (lastTableRow >= 0 && lastTableRow < table.getRowCount()) {
            table.setRowSelectionInterval(lastTableRow, lastTableRow);
        } else {
            table.clearSelection();
        }
        ignoreTable = false;
    }

    private void reload(boolean refreshAccounts) {
        String q = search.getText();
        Integer accountId = selectedAccountId();
        run("Loading…", () -> {
            List<AccountRef> acc = refreshAccounts ? service.listAccounts(q) : null;
            List<CharacterSummary> chars = service.listCharacters(accountId, q);
            return new LoadResult(acc, chars);
        }, result -> {
            if (refreshAccounts && result.accounts() != null) {
                fillAccounts(result.accounts());
            }
            fillTable(result.characters());
            int n = result.characters().size();
            String extra = loaded != null && loaded.online()
                    ? " Online characters cannot be saved."
                    : "";
            status.accept(n + " character(s)." + extra);
            updateButtons();
        });
    }

    private void fillAccounts(List<AccountRef> rows) {
        AccountRef keep = accountList.getSelectedValue();
        ignoreAccount = true;
        accountModel.clear();
        for (AccountRef a : rows) {
            accountModel.addElement(a);
        }
        if (keep != null) {
            for (int i = 0; i < accountModel.size(); i++) {
                if (accountModel.get(i).id() == keep.id()) {
                    accountList.setSelectedIndex(i);
                    lastAccountIndex = i;
                    break;
                }
            }
        }
        ignoreAccount = false;
    }

    private void fillTable(List<CharacterSummary> rows) {
        Long keep = selectedGuid();
        ignoreTable = true;
        tableModel.setRowCount(0);
        for (CharacterSummary s : rows) {
            tableModel.addRow(new Object[]{
                    s.guid(),
                    s.accountName(),
                    s.name(),
                    CharacterLabels.race(s.race()),
                    CharacterLabels.clazz(s.clazz()),
                    s.level(),
                    s.map(),
                    s.online() ? "yes" : "no"
            });
        }
        if (keep != null) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                if (keep.equals(tableModel.getValueAt(i, 0))) {
                    table.setRowSelectionInterval(i, i);
                    lastTableRow = i;
                    ignoreTable = false;
                    return;
                }
            }
        }
        table.clearSelection();
        lastTableRow = -1;
        ignoreTable = false;
        if (loaded != null) {
            boolean still = false;
            for (CharacterSummary s : rows) {
                if (s.guid() == loaded.guid()) {
                    still = true;
                    break;
                }
            }
            if (!still) {
                clearForm();
            }
        }
    }

    private void showDetail(CharacterDetail d) {
        loaded = d;
        snapshot = draftFrom(d);
        raceLabel.setText(CharacterLabels.race(d.race()));
        classLabel.setText(CharacterLabels.clazz(d.clazz()));
        name.setText(d.name());
        gender.setSelectedIndex(d.gender() == 1 ? 1 : 0);
        level.setText(Integer.toString(d.level()));
        xp.setText(Integer.toString(d.xp()));
        money.setText(Integer.toString(d.money()));
        map.setText(Integer.toString(d.map()));
        zone.setText(Integer.toString(d.zone()));
        x.setText(Float.toString(d.x()));
        y.setText(Float.toString(d.y()));
        z.setText(Float.toString(d.z()));
        o.setText(Float.toString(d.o()));
        bindMap.setText(Integer.toString(d.bindMap()));
        bindZone.setText(Integer.toString(d.bindZone()));
        bindX.setText(Float.toString(d.bindX()));
        bindY.setText(Float.toString(d.bindY()));
        bindZ.setText(Float.toString(d.bindZ()));
        skin.setText(Integer.toString(d.skin()));
        face.setText(Integer.toString(d.face()));
        hair.setText(Integer.toString(d.hairStyle()));
        hairColor.setText(Integer.toString(d.hairColor()));
        facial.setText(Integer.toString(d.facialHair()));
        atLogin.setText(Integer.toString(d.atLogin()));
        cinematic.setText(Integer.toString(d.cinematic()));
        boolean online = d.online();
        setFormEnabled(!online);
        String who = d.name() + "  (guid " + d.guid() + ")";
        status.accept(who + (online ? "  Online characters cannot be saved." : ""));
        snapshot = draftFromFormOrNull();
        updateButtons();
    }

    private void clearForm() {
        loaded = null;
        snapshot = null;
        raceLabel.setText(" ");
        classLabel.setText(" ");
        name.setText("");
        gender.setSelectedIndex(0);
        for (JTextField f : new JTextField[]{
                level, xp, money, map, zone, x, y, z, o,
                bindMap, bindZone, bindX, bindY, bindZ,
                skin, face, hair, hairColor, facial, atLogin, cinematic}) {
            f.setText("");
        }
        setFormEnabled(false);
        updateButtons();
    }

    private void save() {
        if (loaded == null) {
            return;
        }
        CharacterDraft draft;
        try {
            draft = draftFromForm();
        } catch (RuntimeException e) {
            status.accept(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            return;
        }
        long guid = loaded.guid();
        run("Saving…", () -> service.update(guid, draft), d -> {
            status.accept("Saved " + d.name() + ".");
            showDetail(d);
            reload(false);
        });
    }

    private void revert() {
        if (loaded == null) {
            return;
        }
        long guid = loaded.guid();
        run("Reverting…", () -> service.get(guid), this::showDetail);
    }

    private void deleteSelected() {
        if (loaded == null) {
            return;
        }
        int ok = JOptionPane.showConfirmDialog(
                root,
                "Delete " + loaded.name() + " (guid " + loaded.guid() + ")?",
                "Delete character",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        long guid = loaded.guid();
        run("Deleting…", () -> {
            service.delete(guid);
            return Boolean.TRUE;
        }, ignored -> {
            status.accept("Deleted.");
            clearForm();
            reload(false);
        });
    }

    private void newCharacter() {
        if (!confirmLeave()) {
            return;
        }
        AccountRef selected = accountList.getSelectedValue();
        run("Loading accounts…", () -> service.listAccounts(null), acc -> showNewDialog(acc, selected));
    }

    private void showNewDialog(List<AccountRef> acc, AccountRef selected) {
        if (acc.isEmpty()) {
            status.accept("No accounts.");
            return;
        }
        JComboBox<AccountRef> accountBox = new JComboBox<>(acc.toArray(AccountRef[]::new));
        accountBox.setRenderer((list, value, index, sel, focus) -> {
            JLabel l = new JLabel(value == null ? "" : value.username());
            l.setOpaque(true);
            if (sel) {
                l.setBackground(list.getSelectionBackground());
                l.setForeground(list.getSelectionForeground());
            } else {
                l.setBackground(list.getBackground());
                l.setForeground(list.getForeground());
            }
            return l;
        });
        if (selected != null) {
            accountBox.setSelectedItem(selected);
        }
        JTextField nm = new JTextField(12);
        JComboBox<IdLabel> race = new JComboBox<>(new IdLabel[]{
                new IdLabel(1, "Human"), new IdLabel(2, "Orc"), new IdLabel(3, "Dwarf"),
                new IdLabel(4, "Night Elf"), new IdLabel(5, "Undead"), new IdLabel(6, "Tauren"),
                new IdLabel(7, "Gnome"), new IdLabel(8, "Troll"),
                new IdLabel(10, "Blood Elf"), new IdLabel(11, "Draenei")
        });
        IdLabel[] classes = new IdLabel[PLAYABLE_CLASSES.length];
        for (int i = 0; i < PLAYABLE_CLASSES.length; i++) {
            int id = PLAYABLE_CLASSES[i];
            classes[i] = new IdLabel(id, CharacterLabels.clazz(id));
        }
        JComboBox<IdLabel> clazz = new JComboBox<>(classes);
        JComboBox<String> g = new JComboBox<>(new String[]{"Male", "Female"});
        JTextField sk = new JTextField("0");
        JTextField fc = new JTextField("0");
        JTextField hr = new JTextField("0");
        JTextField hc = new JTextField("0");
        JTextField fh = new JTextField("0");
        if (!form("New character", fields(
                "Account", accountBox,
                "Name", nm,
                "Race", race,
                "Class", clazz,
                "Gender", g,
                "Skin", sk,
                "Face", fc,
                "Hair", hr,
                "Hair color", hc,
                "Facial hair", fh))) {
            return;
        }
        AccountRef account = (AccountRef) accountBox.getSelectedItem();
        IdLabel r = (IdLabel) race.getSelectedItem();
        IdLabel c = (IdLabel) clazz.getSelectedItem();
        if (account == null || r == null || c == null) {
            status.accept("Account, race, and class are required.");
            return;
        }
        run("Creating…", () -> service.create(
                account.id(),
                nm.getText(),
                r.id(),
                c.id(),
                g.getSelectedIndex(),
                parseInt("Skin", sk.getText()),
                parseInt("Face", fc.getText()),
                parseInt("Hair", hr.getText()),
                parseInt("Hair color", hc.getText()),
                parseInt("Facial hair", fh.getText())), d -> {
            status.accept("Created " + d.name() + ".");
            search.setText("");
            reload(true);
            showDetail(d);
        });
    }

    private boolean confirmLeave() {
        if (!dirty()) {
            return true;
        }
        int r = JOptionPane.showConfirmDialog(
                root,
                "Discard unsaved changes?",
                "Unsaved changes",
                JOptionPane.YES_NO_OPTION);
        return r == JOptionPane.YES_OPTION;
    }

    private boolean dirty() {
        if (loaded == null || loaded.online() || snapshot == null) {
            return false;
        }
        CharacterDraft now = draftFromFormOrNull();
        return now != null && !now.equals(snapshot);
    }

    private void updateButtons() {
        boolean online = loaded != null && loaded.online();
        boolean has = loaded != null;
        boolean d = dirty();
        saveBtn.setEnabled(has && !online && d);
        revertBtn.setEnabled(has && !online && d);
        deleteBtn.setEnabled(has && !online);
        name.setEnabled(has && !online);
        gender.setEnabled(has && !online);
    }

    private void setFormEnabled(boolean on) {
        name.setEnabled(on);
        gender.setEnabled(on);
        for (JTextField f : new JTextField[]{
                level, xp, money, map, zone, x, y, z, o,
                bindMap, bindZone, bindX, bindY, bindZ,
                skin, face, hair, hairColor, facial, atLogin, cinematic}) {
            f.setEnabled(on);
        }
    }

    private Integer selectedAccountId() {
        AccountRef a = accountList.getSelectedValue();
        return a == null ? null : a.id();
    }

    private Long selectedGuid() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        Object v = tableModel.getValueAt(row, 0);
        return v instanceof Long l ? l : ((Number) v).longValue();
    }

    private CharacterDraft draftFromForm() {
        return new CharacterDraft(
                name.getText(),
                gender.getSelectedIndex(),
                parseInt("Skin", skin.getText()),
                parseInt("Face", face.getText()),
                parseInt("Hair", hair.getText()),
                parseInt("Hair color", hairColor.getText()),
                parseInt("Facial hair", facial.getText()),
                parseInt("Level", level.getText()),
                parseInt("XP", xp.getText()),
                parseInt("Money", money.getText()),
                parseInt("Map", map.getText()),
                parseInt("Zone", zone.getText()),
                parseFloat("X", x.getText()),
                parseFloat("Y", y.getText()),
                parseFloat("Z", z.getText()),
                parseFloat("O", o.getText()),
                parseInt("Bind map", bindMap.getText()),
                parseInt("Bind zone", bindZone.getText()),
                parseFloat("Bind X", bindX.getText()),
                parseFloat("Bind Y", bindY.getText()),
                parseFloat("Bind Z", bindZ.getText()),
                parseInt("At login", atLogin.getText()),
                parseInt("Cinematic", cinematic.getText()));
    }

    private CharacterDraft draftFromFormOrNull() {
        try {
            return draftFromForm();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static CharacterDraft draftFrom(CharacterDetail d) {
        return new CharacterDraft(
                d.name(), d.gender(), d.skin(), d.face(), d.hairStyle(), d.hairColor(), d.facialHair(),
                d.level(), d.xp(), d.money(), d.map(), d.zone(), d.x(), d.y(), d.z(), d.o(),
                d.bindMap(), d.bindZone(), d.bindX(), d.bindY(), d.bindZ(), d.atLogin(), d.cinematic());
    }

    private <T> void run(String busy, Supplier<T> work, Consumer<T> ok) {
        status.accept(busy);
        setBusy(true);
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() {
                return work.get();
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    ok.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    status.accept("Interrupted.");
                } catch (ExecutionException e) {
                    Throwable c = e.getCause() == null ? e : e.getCause();
                    status.accept(c.getMessage() == null ? c.getClass().getSimpleName() : c.getMessage());
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        newBtn.setEnabled(!busy);
        searchBtn.setEnabled(!busy);
        allBtn.setEnabled(!busy);
        if (busy) {
            saveBtn.setEnabled(false);
            revertBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
        } else {
            updateButtons();
        }
    }

    private boolean form(String title, JPanel fields) {
        Window owner = ancestor();
        JDialog dlg = owner instanceof JFrame f
                ? new JDialog(f, title, true)
                : new JDialog(owner, title, ModalityType.APPLICATION_MODAL);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        south.add(ok);
        south.add(cancel);
        dlg.add(fields, BorderLayout.CENTER);
        dlg.add(south, BorderLayout.SOUTH);
        final boolean[] accepted = {false};
        ok.addActionListener(e -> {
            accepted[0] = true;
            dlg.dispose();
        });
        cancel.addActionListener(e -> dlg.dispose());
        dlg.pack();
        dlg.setLocationRelativeTo(root);
        dlg.setVisible(true);
        return accepted[0];
    }

    private Window ancestor() {
        Component c = root;
        while (c != null && !(c instanceof Window)) {
            c = c.getParent();
        }
        return (Window) c;
    }

    private static JPanel fields(Object... labelAndComp) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints lc = new GridBagConstraints();
        lc.gridx = 0;
        lc.anchor = GridBagConstraints.EAST;
        lc.insets = new Insets(2, 2, 2, 6);
        GridBagConstraints fc = new GridBagConstraints();
        fc.gridx = 1;
        fc.fill = GridBagConstraints.HORIZONTAL;
        fc.weightx = 1;
        fc.insets = new Insets(2, 2, 2, 2);
        for (int i = 0; i < labelAndComp.length; i += 2) {
            lc.gridy = i / 2;
            fc.gridy = i / 2;
            p.add(new JLabel(String.valueOf(labelAndComp[i])), lc);
            p.add((Component) labelAndComp[i + 1], fc);
        }
        return p;
    }

    private static JTextField field() {
        return new JTextField(10);
    }

    private static int parseInt(String label, String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new EditorException(label + " must be an integer.");
        }
    }

    private static float parseFloat(String label, String text) {
        try {
            return Float.parseFloat(text.trim());
        } catch (NumberFormatException e) {
            throw new EditorException(label + " must be a number.");
        }
    }

    private record LoadResult(List<AccountRef> accounts, List<CharacterSummary> characters) {}

    private record IdLabel(int id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }
}
