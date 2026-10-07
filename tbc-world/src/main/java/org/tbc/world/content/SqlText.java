package org.tbc.world.content;

/** Null-safe text helpers for the SQL loaders. */
final class SqlText {

    private SqlText() {
    }

    static String nz(String s) {
        return s == null ? "" : s;
    }
}
