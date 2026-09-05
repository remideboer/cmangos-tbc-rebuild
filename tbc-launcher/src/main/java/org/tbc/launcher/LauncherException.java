package org.tbc.launcher;

/** Operator-facing launcher error. */
public final class LauncherException extends RuntimeException {
    public LauncherException(String message) {
        super(message);
    }

    public LauncherException(String message, Throwable cause) {
        super(message, cause);
    }
}
