package org.tbc.world.entity;

import org.tbc.common.WowBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReputationMgrTest {
    @Test
    void setInactiveWhenVisibleShouldSetInactiveFlag() {
        ReputationMgr r = new ReputationMgr();
        r.seedCreateDefaults(ReputationMgr.TEAM_ALLIANCE);
        r.setInactive(ReputationMgr.LIST_STORMWIND, true);
        assertEquals(ReputationMgr.FLAG_INACTIVE,
                r.flags(ReputationMgr.LIST_STORMWIND) & ReputationMgr.FLAG_INACTIVE);
        assertEquals(ReputationMgr.FLAG_VISIBLE,
                r.flags(ReputationMgr.LIST_STORMWIND) & ReputationMgr.FLAG_VISIBLE);
    }

    @Test
    void setInactiveWhenUnknownListShouldNoOp() {
        ReputationMgr r = new ReputationMgr();
        r.setInactive(ReputationMgr.LIST_STORMWIND, true);
        assertEquals(0, r.flags(ReputationMgr.LIST_STORMWIND));
    }

    @Test
    void setInactiveWhenHiddenShouldNoOp() {
        ReputationMgr r = new ReputationMgr();
        r.put(ReputationMgr.LIST_STORMWIND, ReputationMgr.FLAG_VISIBLE | ReputationMgr.FLAG_HIDDEN);
        r.setInactive(ReputationMgr.LIST_STORMWIND, true);
        assertEquals(0, r.flags(ReputationMgr.LIST_STORMWIND) & ReputationMgr.FLAG_INACTIVE);
    }

    @Test
    void setInactiveWhenInvisibleForcedShouldNoOp() {
        ReputationMgr r = new ReputationMgr();
        r.put(ReputationMgr.LIST_STORMWIND, ReputationMgr.FLAG_VISIBLE | ReputationMgr.FLAG_INVISIBLE_FORCED);
        r.setInactive(ReputationMgr.LIST_STORMWIND, true);
        assertEquals(0, r.flags(ReputationMgr.LIST_STORMWIND) & ReputationMgr.FLAG_INACTIVE);
    }

    @Test
    void setInactiveWhenNotVisibleShouldNoOp() {
        ReputationMgr r = new ReputationMgr();
        r.put(ReputationMgr.LIST_STORMWIND, 0);
        r.setInactive(ReputationMgr.LIST_STORMWIND, true);
        assertEquals(0, r.flags(ReputationMgr.LIST_STORMWIND) & ReputationMgr.FLAG_INACTIVE);
    }

    @Test
    void setInactiveWhenOffShouldClearFlag() {
        ReputationMgr r = new ReputationMgr();
        r.seedCreateDefaults(ReputationMgr.TEAM_ALLIANCE);
        r.setInactive(ReputationMgr.LIST_STORMWIND, true);
        r.setInactive(ReputationMgr.LIST_STORMWIND, false);
        assertEquals(0, r.flags(ReputationMgr.LIST_STORMWIND) & ReputationMgr.FLAG_INACTIVE);
        assertEquals(ReputationMgr.STORMWIND_ALLIANCE_FLAGS, r.flags(ReputationMgr.LIST_STORMWIND));
    }

    @Test
    void setInactiveWhenAlreadySetShouldNoOp() {
        ReputationMgr r = new ReputationMgr();
        r.seedCreateDefaults(ReputationMgr.TEAM_ALLIANCE);
        r.setInactive(ReputationMgr.LIST_STORMWIND, true);
        int once = r.flags(ReputationMgr.LIST_STORMWIND);
        r.setInactive(ReputationMgr.LIST_STORMWIND, true);
        assertEquals(once, r.flags(ReputationMgr.LIST_STORMWIND));
    }

    @Test
    void seedCreateDefaultsWhenHordeShouldLeaveStormwindEmpty() {
        ReputationMgr r = new ReputationMgr();
        r.seedCreateDefaults(67);
        assertEquals(0, r.flags(ReputationMgr.LIST_STORMWIND));
    }

    @Test
    void writeInitialWhenStormwindSeededShouldWriteSlot19() {
        ReputationMgr r = new ReputationMgr();
        r.seedCreateDefaults(ReputationMgr.TEAM_ALLIANCE);
        WowBuffer out = new WowBuffer(4 + ReputationMgr.SLOTS * 5);
        r.writeInitial(out);
        WowBuffer in = new WowBuffer(out.array());
        assertEquals(0x80, in.getU32());
        for (int i = 0; i < ReputationMgr.LIST_STORMWIND; i++) {
            assertEquals(0, in.getU8());
            assertEquals(0, in.getU32());
        }
        assertEquals(ReputationMgr.STORMWIND_ALLIANCE_FLAGS, in.getU8());
        assertEquals(0, in.getU32());
    }
}
