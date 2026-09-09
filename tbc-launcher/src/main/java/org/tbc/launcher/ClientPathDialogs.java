package org.tbc.launcher;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.nio.file.Path;
import java.util.Optional;

/** The two client-path dialogs, behind an interface so the frame can be tested without modal windows. */
public interface ClientPathDialogs {
    enum Choice { BROWSE, CLEAR, CANCEL }

    /** File chooser for the client exe; empty when cancelled. */
    Optional<Path> browse();

    /** Shows the current path and asks what to do with it. */
    Choice manage(Path current);

    static ClientPathDialogs swing(Component parent) {
        return new ClientPathDialogs() {
            @Override
            public Optional<Path> browse() {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle("Select the WoW 2.4.3 client (Wow.exe)");
                fc.setFileFilter(new FileNameExtensionFilter("Executable (*.exe)", "exe"));
                if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION || fc.getSelectedFile() == null) {
                    return Optional.empty();
                }
                return Optional.of(fc.getSelectedFile().toPath());
            }

            @Override
            public Choice manage(Path current) {
                int r = JOptionPane.showOptionDialog(
                        parent,
                        "Client: " + current,
                        "Client path",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        new Object[]{"Change…", "Reset", "Cancel"},
                        "Cancel");
                if (r == JOptionPane.YES_OPTION) {
                    return Choice.BROWSE;
                }
                if (r == JOptionPane.NO_OPTION) {
                    return Choice.CLEAR;
                }
                return Choice.CANCEL;
            }
        };
    }
}
