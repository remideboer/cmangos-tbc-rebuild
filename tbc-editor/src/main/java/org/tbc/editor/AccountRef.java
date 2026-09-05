package org.tbc.editor;

/** Login account identity for the editor. No verifier or password. */
public record AccountRef(int id, String username, int expansion) {}
