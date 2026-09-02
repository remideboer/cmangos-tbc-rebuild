package org.tbc.admin;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/** Operator UI. SQL/SRP live in AccountService. */
public final class AccountFrame extends JFrame {
    private static final String[] COLS = {"Id", "Username", "Role", "Expansion"};
    private static final String[] ROLES = {"Player", "Moderator", "Gamemaster", "Administrator"};
    private static final String[] EXPANSIONS = {"Classic (0)", "TBC (1)"};

    private final AccountService service;
    private final DefaultTableModel model = new DefaultTableModel(COLS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel(" ");
    private final JButton newBtn = new JButton("New");
    private final JButton roleBtn = new JButton("Set role");
    private final JButton passBtn = new JButton("Change password");
    private final JButton delBtn = new JButton("Delete");
    private final JButton refreshBtn = new JButton("Refresh");

    public AccountFrame(AccountService service) {
        super("TBC Account Admin");
        this.service = service;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(720, 420);
        setLocationRelativeTo(null);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(newBtn);
        buttons.add(roleBtn);
        buttons.add(passBtn);
        buttons.add(delBtn);
        buttons.add(refreshBtn);
        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(buttons, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        newBtn.addActionListener(e -> newAccount());
        roleBtn.addActionListener(e -> setRole());
        passBtn.addActionListener(e -> changePassword());
        delBtn.addActionListener(e -> deleteAccount());
        refreshBtn.addActionListener(e -> refresh());
        refresh();
    }

    private void refresh() {
        run("Loading…", service::list, rows -> {
            model.setRowCount(0);
            for (AccountRow r : rows) {
                model.addRow(new Object[]{
                        r.id(), r.username(), AccountService.roleName(r.gmlevel()), expansionLabel(r.expansion())
                });
            }
            status.setText(rows.size() + " account(s). Character rows are not deleted with an account.");
        });
    }

    private void newAccount() {
        JTextField user = new JTextField(16);
        JPasswordField pass = new JPasswordField(16);
        JPasswordField confirm = new JPasswordField(16);
        JComboBox<String> role = new JComboBox<>(new DefaultComboBoxModel<>(ROLES));
        JComboBox<String> exp = new JComboBox<>(new DefaultComboBoxModel<>(EXPANSIONS));
        exp.setSelectedIndex(1);
        if (!form("New account", fields(
                "Username", user,
                "Password", pass,
                "Confirm", confirm,
                "Role", role,
                "Expansion", exp))) {
            return;
        }
        String username = user.getText();
        String p = new String(pass.getPassword());
        String c = new String(confirm.getPassword());
        int gm = role.getSelectedIndex();
        int expansion = exp.getSelectedIndex();
        run("Creating…", () -> service.create(username, p, c, gm, expansion), row -> {
            status.setText("Created " + row.username() + ".");
            refresh();
        });
    }

    private void setRole() {
        Integer id = selectedId();
        if (id == null) {
            return;
        }
        JComboBox<String> role = new JComboBox<>(new DefaultComboBoxModel<>(ROLES));
        int row = table.getSelectedRow();
        String current = String.valueOf(model.getValueAt(row, 2));
        for (int i = 0; i < ROLES.length; i++) {
            if (ROLES[i].equals(current)) {
                role.setSelectedIndex(i);
                break;
            }
        }
        if (!form("Set role", fields("Role", role))) {
            return;
        }
        int gm = role.getSelectedIndex();
        run("Saving role…", () -> service.setRole(id, gm), r -> {
            status.setText(r.username() + " is now " + AccountService.roleName(r.gmlevel()) + ".");
            refresh();
        });
    }

    private void changePassword() {
        Integer id = selectedId();
        if (id == null) {
            return;
        }
        JPasswordField pass = new JPasswordField(16);
        JPasswordField confirm = new JPasswordField(16);
        if (!form("Change password", fields("Password", pass, "Confirm", confirm))) {
            return;
        }
        String p = new String(pass.getPassword());
        String c = new String(confirm.getPassword());
        run("Saving password…", () -> service.setPassword(id, p, c), r ->
                status.setText("Password updated for " + r.username() + "."));
    }

    private void deleteAccount() {
        Integer id = selectedId();
        if (id == null) {
            return;
        }
        int row = table.getSelectedRow();
        String name = String.valueOf(model.getValueAt(row, 1));
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete account " + name + " from the login database?",
                "Delete", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        run("Deleting…", () -> {
            service.delete(id);
            return name;
        }, deleted -> {
            status.setText("Deleted " + deleted + ".");
            refresh();
        });
    }

    private Integer selectedId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            status.setText("Select an account.");
            return null;
        }
        return (Integer) model.getValueAt(row, 0);
    }

    private <T> void run(String busy, Supplier<T> work, java.util.function.Consumer<T> ok) {
        status.setText(busy);
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
                    status.setText("Interrupted.");
                } catch (ExecutionException e) {
                    Throwable c = e.getCause() == null ? e : e.getCause();
                    status.setText(c.getMessage() == null ? c.getClass().getSimpleName() : c.getMessage());
                }
            }
        }.execute();
    }

    private void setBusy(boolean busy) {
        newBtn.setEnabled(!busy);
        roleBtn.setEnabled(!busy);
        passBtn.setEnabled(!busy);
        delBtn.setEnabled(!busy);
        refreshBtn.setEnabled(!busy);
    }

    private boolean form(String title, JPanel fields) {
        JDialog dlg = new JDialog(this, title, true);
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
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        return accepted[0];
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
            p.add((java.awt.Component) labelAndComp[i + 1], fc);
        }
        return p;
    }

    private static String expansionLabel(int expansion) {
        return expansion == 1 ? "TBC (1)" : "Classic (" + expansion + ")";
    }
}
