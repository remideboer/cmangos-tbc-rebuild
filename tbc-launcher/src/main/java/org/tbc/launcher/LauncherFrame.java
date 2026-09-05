package org.tbc.launcher;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/** Operator UI. Process rules live in ServerProcessService. */
public final class LauncherFrame extends JFrame {
    private final ServerProcessService service;
    private final JLabel authStatus = new JLabel("Auth: stopped");
    private final JLabel worldStatus = new JLabel("World: stopped");
    private final JLabel status = new JLabel("MySQL and the 8606 client stay external. This is not a slice.");
    private final JButton startBtn = new JButton("Start servers");
    private final JButton stopBtn = new JButton("Stop servers");
    private final JButton restartBtn = new JButton("Restart servers");
    private final JButton adminBtn = new JButton("Open admin");
    private final JButton editorBtn = new JButton("Open editor");
    private boolean closing;

    public LauncherFrame(ServerProcessService service) {
        super("TBC Launcher");
        this.service = service;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(520, 220);
        setLocationRelativeTo(null);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(startBtn);
        buttons.add(stopBtn);
        buttons.add(restartBtn);
        buttons.add(adminBtn);
        buttons.add(editorBtn);
        JPanel running = new JPanel(new GridLayout(2, 1, 0, 4));
        running.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        running.add(authStatus);
        running.add(worldStatus);
        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(buttons, BorderLayout.NORTH);
        add(running, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        startBtn.addActionListener(e -> run("Starting servers…", () -> {
            service.startServers();
            return "Servers started.";
        }));
        stopBtn.addActionListener(e -> run("Stopping servers…", () -> {
            service.stopServers();
            return "Servers stopped.";
        }));
        restartBtn.addActionListener(e -> run("Restarting servers…", () -> {
            service.restartServers();
            return "Servers restarted.";
        }));
        adminBtn.addActionListener(e -> run("Opening admin…", () -> {
            service.openAdmin();
            return "Admin opened.";
        }));
        editorBtn.addActionListener(e -> run("Opening editor…", () -> {
            service.openEditor();
            return "Editor opened.";
        }));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
        refreshRunning();
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
    }
}
