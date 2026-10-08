package org.tbc.editor;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.GraphicsEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class EditorLaunchArgsTest {
    @Test
    void confPathWhenFirstNonFlagShouldBeUsed() {
        assertEquals("conf/mangosd.conf", EditorMain.confPath(null));
        assertEquals("conf/mangosd.conf", EditorMain.confPath(new String[0]));
        assertEquals("conf/local-mangosd.conf", EditorMain.confPath(new String[]{"conf/local-mangosd.conf"}));
        assertEquals("conf/local-mangosd.conf",
                EditorMain.confPath(new String[]{"--quest", "conf/local-mangosd.conf"}));
        assertEquals("conf/local-mangosd.conf",
                EditorMain.confPath(new String[]{"conf/local-mangosd.conf", "--quest"}));
        assertEquals("conf/mangosd.conf", EditorMain.confPath(new String[]{"--quest"}));
        assertEquals("conf/mangosd.conf", EditorMain.confPath(new String[]{null, "  "}));
    }

    @Test
    void questFlagWhenPresentShouldBeTrue() {
        assertFalse(EditorMain.questFlag(null));
        assertFalse(EditorMain.questFlag(new String[0]));
        assertFalse(EditorMain.questFlag(new String[]{"conf/local-mangosd.conf"}));
        assertTrue(EditorMain.questFlag(new String[]{"conf/local-mangosd.conf", "--quest"}));
        assertTrue(EditorMain.questFlag(new String[]{"--quest", "conf/local-mangosd.conf"}));
    }
}

class EditorFrameSelectDomainTest {
    @Test
    void selectDomainWhenKnownShouldSelectThatCard() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "EditorFrame needs a display (CI uses xvfb-run)");
        EditorFrame frame = new EditorFrame();
        frame.addDomain(stub("Characters"));
        frame.addDomain(stub("Quests"));
        assertEquals(0, frame.selectedDomainIndex());
        frame.selectDomain("Quests");
        assertEquals(1, frame.selectedDomainIndex());
        frame.selectDomain("missing");
        assertEquals(1, frame.selectedDomainIndex());
        frame.dispose();
    }

    private static EditorDomain stub(String title) {
        return new EditorDomain() {
            @Override
            public String title() {
                return title;
            }

            @Override
            public javax.swing.JComponent view() {
                return new JPanel();
            }
        };
    }
}
