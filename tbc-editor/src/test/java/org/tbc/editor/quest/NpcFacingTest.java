package org.tbc.editor.quest;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcFacingTest {
    private static final float STEP = (float) (Math.PI / 8);

    @Test
    void snapWhenOffTickShouldLandOnNearestOfSixteen() {
        assertEquals(0f, NpcFacing.snap(0.01f), 1e-5f);
        assertEquals(STEP, NpcFacing.snap(STEP + 0.05f), 1e-5f);
        assertEquals(16, uniqueTicks());
    }

    @Test
    void rightWhenOnTickShouldRotateClockwiseOneStep() {
        assertEquals(NpcFacing.wrap(-STEP), NpcFacing.right(0f), 1e-5f);
        assertEquals(0f, NpcFacing.right(STEP), 1e-5f);
    }

    @Test
    void leftWhenOnTickShouldRotateCounterclockwiseOneStep() {
        assertEquals(STEP, NpcFacing.left(0f), 1e-5f);
        assertEquals(0f, NpcFacing.left(NpcFacing.wrap(-STEP)), 1e-5f);
    }

    @Test
    void rightWhenOffTickShouldSnapThenStep() {
        assertEquals(NpcFacing.wrap(-STEP), NpcFacing.right(0.04f), 1e-5f);
    }

    @Test
    void wrapWhenNegativeOrPastTwoPiShouldStayInRange() {
        assertTrue(NpcFacing.wrap(-0.1f) >= 0f);
        assertTrue(NpcFacing.wrap(-0.1f) < Math.PI * 2);
        assertEquals(0f, NpcFacing.wrap((float) (Math.PI * 2)), 1e-5f);
    }

    @Test
    void tickWhenSnappedShouldNumberZeroThroughFifteen() {
        assertEquals(0, NpcFacing.tick(0f));
        assertEquals(4, NpcFacing.tick((float) (Math.PI / 2)));
        assertEquals(15, NpcFacing.tick(NpcFacing.wrap(-STEP)));
    }

    @Test
    void screenDirWhenZeroShouldPointUpOnTheMinimap() {
        float[] d = NpcFacing.screenDir(0f);
        assertEquals(0f, d[0], 1e-5f);
        assertTrue(d[1] < 0f);
    }

    private static int uniqueTicks() {
        Set<Integer> ticks = new HashSet<>();
        for (int i = 0; i < 16; i++) {
            ticks.add(NpcFacing.tick(i * STEP + 0.01f));
        }
        return ticks.size();
    }
}
