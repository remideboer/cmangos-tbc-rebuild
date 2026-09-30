package org.tbc.content.dbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** One DBC column from a TBC binding file (WoW-Spell-Editor Bindings_243_tbc). */
public final class BindingField {
    public enum Kind {
        UINT, INT, FLOAT, STRING
    }

    private final String name;
    private final Kind kind;
    private final int index;

    public BindingField(String name, Kind kind, int index) {
        this.name = name;
        this.kind = kind;
        this.index = index;
    }

    public String name() {
        return name;
    }

    public Kind kind() {
        return kind;
    }

    public int index() {
        return index;
    }

    public boolean isString() {
        return kind == Kind.STRING;
    }
}
