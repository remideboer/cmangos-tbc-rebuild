package org.tbc.editor;

import javax.swing.JComponent;

/** Pluggable editor area. v1 is characters; creature/item/spell are later domains. */
public interface EditorDomain {
    String title();

    JComponent view();
}
