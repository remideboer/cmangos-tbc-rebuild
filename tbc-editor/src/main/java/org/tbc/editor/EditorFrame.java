package org.tbc.editor;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** Shell: domain list + card panel. v1 registers Characters only. */
public final class EditorFrame extends JFrame {
    private final JLabel status = new JLabel(" ");
    private final DefaultListModel<String> titles = new DefaultListModel<>();
    private final JList<String> domainList = new JList<>(titles);
    private final JPanel cards = new JPanel(new CardLayout());
    private final List<EditorDomain> domains = new ArrayList<>();

    public EditorFrame() {
        super("TBC Character Editor");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 640);
        setLocationRelativeTo(null);
        domainList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        domainList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || domainList.getSelectedIndex() < 0) {
                return;
            }
            ((CardLayout) cards.getLayout()).show(cards, titles.get(domainList.getSelectedIndex()));
        });
        JPanel west = new JPanel(new BorderLayout());
        west.add(new JScrollPane(domainList), BorderLayout.CENTER);
        west.setPreferredSize(new Dimension(130, 0));
        west.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(west, BorderLayout.WEST);
        add(cards, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        add(status, BorderLayout.SOUTH);
    }

    public void addDomain(EditorDomain domain) {
        domains.add(domain);
        titles.addElement(domain.title());
        cards.add(domain.view(), domain.title());
        if (titles.size() == 1) {
            domainList.setSelectedIndex(0);
        }
    }

    public void setStatus(String text) {
        status.setText(text);
    }
}
