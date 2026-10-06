package org.tbc.launcher;

/** Which process stdout lines belong in the operator log panes. */
public final class LogPaneLines {
    private LogPaneLines() {
    }

    /**
     * Full tee still writes every line to {@code logs/*.log}. The panes hide ScriptRegistry
     * "unknown ScriptName" spam so boot INFO (listening, realm online) stays visible inside
     * {@link LauncherFrame#LOG_MAX_LINES}.
     */
    public static boolean uiVisible(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }
        return !line.contains("unknown ScriptName");
    }
}
