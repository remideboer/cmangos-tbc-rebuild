package org.tbc.admin;

/** Operator-facing account admin error. Never includes a password. */
public final class AccountException extends RuntimeException {
    public AccountException(String message) {
        super(message);
    }

    public AccountException(String message, Throwable cause) {
        super(message, cause);
    }
}
