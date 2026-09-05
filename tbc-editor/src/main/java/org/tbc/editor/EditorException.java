package org.tbc.editor;

/** Operator-facing editor error. */
public final class EditorException extends RuntimeException {
    public EditorException(String message) {
        super(message);
    }

    public EditorException(String message, Throwable cause) {
        super(message, cause);
    }
}
