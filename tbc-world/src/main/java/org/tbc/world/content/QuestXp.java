package org.tbc.world.content;

/** QuestDef.cpp Quest::XPValue — RewMoneyMaxLevel scaled by quest level, then greyed by player level. */
public final class QuestXp {
    private QuestXp() {}

    public static int xpValue(int playerLevel, int questLevel, int rewMoneyMaxLevel) {
        if (rewMoneyMaxLevel <= 0) {
            return 0;
        }
        float fullxp = 0f;
        if (questLevel >= 65) {
            fullxp = rewMoneyMaxLevel / 6.0f;
        } else if (questLevel == 64) {
            fullxp = rewMoneyMaxLevel / 4.8f;
        } else if (questLevel == 63) {
            fullxp = rewMoneyMaxLevel / 3.6f;
        } else if (questLevel == 62) {
            fullxp = rewMoneyMaxLevel / 2.4f;
        } else if (questLevel == 61) {
            fullxp = rewMoneyMaxLevel / 1.2f;
        } else if (questLevel > 0 && questLevel <= 60) {
            fullxp = rewMoneyMaxLevel / 0.6f;
        }
        if (playerLevel <= questLevel + 5) {
            return (int) Math.ceil(fullxp);
        }
        if (playerLevel == questLevel + 6) {
            return (int) Math.ceil(fullxp * 0.8f);
        }
        if (playerLevel == questLevel + 7) {
            return (int) Math.ceil(fullxp * 0.6f);
        }
        if (playerLevel == questLevel + 8) {
            return (int) Math.ceil(fullxp * 0.4f);
        }
        if (playerLevel == questLevel + 9) {
            return (int) Math.ceil(fullxp * 0.2f);
        }
        return (int) Math.ceil(fullxp * 0.1f);
    }
}
