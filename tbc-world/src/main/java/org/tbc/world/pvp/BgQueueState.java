package org.tbc.world.pvp;

/**
 * One session's battleground queue slot (CMaNGOS Player::m_bgBattleGroundQueueID, single slot in this rebuild):
 * the queued map id (0 = not queued) and whether the Eye of the Storm init world states were already sent.
 */
public final class BgQueueState {
    private int queuedMap;
    private boolean eotsWorldStatesSent;

    public boolean queued() {
        return queuedMap != 0;
    }

    public int queuedMap() {
        return queuedMap;
    }

    public void join(int map) {
        queuedMap = map;
    }

    public void leave() {
        queuedMap = 0;
    }

    public boolean eotsWorldStatesSent() {
        return eotsWorldStatesSent;
    }

    public void markEotsWorldStatesSent() {
        eotsWorldStatesSent = true;
    }
}
