package org.tbc.launcher;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogPaneLinesTest {
    @Test
    void uiVisibleWhenStartupInfoShouldKeepLine() {
        assertTrue(LogPaneLines.uiVisible(
                "starting world jar=D:\\x\\tbc-world.jar mtimeMs=1"));
        assertTrue(LogPaneLines.uiVisible(
                "21:06:27.591 INFO  o.t.world.WorldMain - tbc-world listening 0.0.0.0:8085 tick 50ms"));
        assertTrue(LogPaneLines.uiVisible(
                "21:06:27.470 INFO  o.t.w.world.World - realm 1 online"));
    }

    @Test
    void uiVisibleWhenUnknownScriptNameSpamShouldHideFromPane() {
        assertFalse(LogPaneLines.uiVisible(
                "21:06:27.375 WARN  o.t.w.s.ScriptRegistry - unknown ScriptName boss_skeram, falling back to generic AI"));
        assertFalse(LogPaneLines.uiVisible(
                "21:06:27.375 WARN  o.t.w.s.ScriptRegistry - unknown ScriptName mob_anubisath_sentinel, falling back to generic AI"));
    }

    @Test
    void uiVisibleWhenNullOrBlankShouldHide() {
        assertFalse(LogPaneLines.uiVisible(null));
        assertFalse(LogPaneLines.uiVisible(""));
    }
}
