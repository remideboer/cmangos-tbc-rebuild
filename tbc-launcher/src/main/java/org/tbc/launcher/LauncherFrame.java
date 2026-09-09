package org.tbc.launcher;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/** Operator UI. Process rules live in ServerProcessService. */
public final class LauncherFrame extends JFrame {
    static final int LOG_ROWS = 10;
    static final int LOG_MAX_LINES = 500;
    private final ServerProcessService service;
    private final JLabel authStatus = new JLabel("Auth: stopped");
    private final JLabel worldStatus = new JLabel("World: stopped");
    private final JTextArea authLog = logArea();
    private final JTextArea worldLog = logArea();
    private final JLabel status = new JLabel("MySQL and the 8606 client stay external. This is not a slice.");
    private final JButton startBtn = new JButton("Start servers");
    private final JButton stopBtn = new JButton("Stop servers");
    private final JButton restartBtn = new JButton("Restart servers");
    private final JButton adminBtn = new JButton("Open admin");
    private final JButton editorBtn = new JButton("Open editor");
    private final JButton clientBtn = new JButton("Start client");
    private final JButton clientPathBtn = new JButton("Client path…");
    private final ClientLauncher client;
    private final ClientPathDialogs dialogs;
    private boolean closing;

    public LauncherFrame(ServerProcessService service, ClientLauncher client) {
        this(service, client, null);
    }

    LauncherFrame(ServerProcessService service, ClientLauncher client, ClientPathDialogs dialogs) {
        super("TBC Launcher");
        this.service = service;
        this.client = client;
        this.dialogs = dialogs != null ? dialogs : ClientPathDialogs.swing(this);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(640, 520);
        setLocationRelativeTo(null);
        JPanel servers = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        servers.add(startBtn);
        servers.add(stopBtn);
        servers.add(restartBtn);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        tools.add(adminBtn);
        tools.add(editorBtn);
        tools.add(clientBtn);
        tools.add(clientPathBtn);
        JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 0));
        buttons.add(servers);
        buttons.add(tools);
        JPanel center = new JPanel(new GridBagLayout());
        center.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 4, 8);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        center.add(authStatus, c);
        c.gridx = 1;
        c.weightx = 1;
        c.weighty = 0.5;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 4, 0);
        center.add(scroll(authLog, "Auth log"), c);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 8);
        center.add(worldStatus, c);
        c.gridx = 1;
        c.weightx = 1;
        c.weighty = 0.5;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);
        center.add(scroll(worldLog, "World log"), c);
        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(buttons, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        service.setAuthLogListener(line -> appendLog(authLog, line));
        service.setWorldLogListener(line -> appendLog(worldLog, line));
        startBtn.addActionListener(e -> {
            authLog.setText("");
            worldLog.setText("");
            run("Starting servers…", () -> {
                service.startServers();
                return "Servers started.";
            });
        });
        stopBtn.addActionListener(e -> run("Stopping servers…", () -> {
            service.stopServers();
            return "Servers stopped.";
        }));
        restartBtn.addActionListener(e -> {
            authLog.setText("");
            worldLog.setText("");
            run("Restarting servers…", () -> {
                service.restartServers();
                return "Servers restarted.";
            });
        });
        adminBtn.addActionListener(e -> run("Opening admin…", () -> {
            service.openAdmin();
            return "Admin opened.";
        }));
        editorBtn.addActionListener(e -> run("Opening editor…", () -> {
            service.openEditor();
            return "Editor opened.";
        }));
        clientBtn.addActionListener(e -> onStartClient());
        clientPathBtn.addActionListener(e -> onClientPath());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
        refreshRunning();
    }

    JTextArea authLogArea() {
        return authLog;
    }

    JTextArea worldLogArea() {
        return worldLog;
    }

    private static JTextArea logArea() {
        JTextArea a = new JTextArea(LOG_ROWS, 48);
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return a;
    }

    private static JScrollPane scroll(JTextArea area, String title) {
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(BorderFactory.createTitledBorder(title));
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        return sp;
    }

    private static void appendLog(JTextArea area, String line) {
        SwingUtilities.invokeLater(() -> {
            area.append(line);
            area.append("\n");
            String text = area.getText();
            int lines = 1;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    lines++;
                }
            }
            if (lines > LOG_MAX_LINES) {
                int drop = lines - LOG_MAX_LINES;
                int cut = 0;
                for (int i = 0; i < text.length() && drop > 0; i++) {
                    if (text.charAt(i) == '\n') {
                        drop--;
                        cut = i + 1;
                    }
                }
                area.setText(text.substring(cut));
            }
            area.setCaretPosition(area.getDocument().getLength());
        });
    }

    /** Unset path → ask for the exe first; then start the client in its own folder. */
    private void onStartClient() {
        if (!client.hasClientPath() && !browseClientPath()) {
            status.setText("Client path not set.");
            return;
        }
        run("Starting client…", () -> {
            client.startClient();
            return "Client started.";
        });
    }

    /** Unset → browse; set → change, reset or keep. */
    private void onClientPath() {
        if (!client.hasClientPath()) {
            browseClientPath();
            return;
        }
        ClientPathDialogs.Choice choice = dialogs.manage(client.clientPath().orElseThrow());
        if (choice == ClientPathDialogs.Choice.CLEAR) {
            client.clearClientPath();
            status.setText("Client path reset.");
        } else if (choice == ClientPathDialogs.Choice.BROWSE) {
            browseClientPath();
        }
    }

    private boolean browseClientPath() {
        var picked = dialogs.browse();
        if (picked.isEmpty()) {
            return false;
        }
        try {
            client.setClientPath(picked.get());
        } catch (LauncherException e) {
            status.setText(e.getMessage());
            return false;
        }
        status.setText("Client: " + picked.get());
        return true;
    }

    private void onClose() {
        if (closing) {
            return;
        }
        if (!service.isAuthRunning() && !service.isWorldRunning()) {
            dispose();
            return;
        }
        int r = JOptionPane.showOptionDialog(
                this,
                "Servers are running. Stop them before exit?",
                "TBC Launcher",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"Stop", "Leave running", "Cancel"},
                "Cancel");
        if (r == JOptionPane.CANCEL_OPTION || r == JOptionPane.CLOSED_OPTION) {
            return;
        }
        if (r == JOptionPane.NO_OPTION) {
            dispose();
            return;
        }
        closing = true;
        run("Stopping servers…", () -> {
            service.stopServers();
            return "Servers stopped.";
        }, () -> dispose());
    }

    private void run(String busy, Supplier<String> work) {
        run(busy, work, null);
    }

    private void run(String busy, Supplier<String> work, Runnable afterOk) {
        status.setText(busy);
        setBusy(true);
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return work.get();
            }

            @Override
            protected void done() {
                setBusy(false);
                refreshRunning();
                try {
                    status.setText(get());
                    if (afterOk != null) {
                        afterOk.run();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    status.setText("Interrupted.");
                    closing = false;
                } catch (ExecutionException e) {
                    closing = false;
                    Throwable c = e.getCause() == null ? e : e.getCause();
                    status.setText(c.getMessage() == null ? c.getClass().getSimpleName() : c.getMessage());
                }
            }
        }.execute();
    }

    private void refreshRunning() {
        authStatus.setText(service.isAuthRunning() ? "Auth: running" : "Auth: stopped");
        worldStatus.setText(service.isWorldRunning() ? "World: running" : "World: stopped");
    }

    private void setBusy(boolean busy) {
        startBtn.setEnabled(!busy);
        stopBtn.setEnabled(!busy);
        restartBtn.setEnabled(!busy);
        adminBtn.setEnabled(!busy);
        editorBtn.setEnabled(!busy);
        clientBtn.setEnabled(!busy);
        clientPathBtn.setEnabled(!busy);
    }
}
