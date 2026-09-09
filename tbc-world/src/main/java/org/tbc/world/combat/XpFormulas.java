package org.tbc.world.combat;

import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;

/** CMaNGOS Formulas.h namespace MaNGOS::XP — kill experience. */
public final class XpFormulas {
    /** ContentLevels: base XP bonus for CONTENT_1_60 / CONTENT_61_70 maps. */
    private static final int CONTENT_1_60_BONUS = 45;
    private static final int CONTENT_61_70_BONUS = 235;
    private static final int OUTLAND_MAP = 530;
    /** creature_template.Rank: 0 normal, 1 elite, 2 rare elite, 3 world boss, 4 rare. */
    private static final int RANK_NORMAL = 0;
    private static final int RANK_RARE = 4;

    private XpFormulas() {}

    /** IsTrivialLevelDifference — grey mobs give nothing. */
    static boolean isTrivialLevelDifference(int unitLvl, int targetLvl) {
        if (unitLvl <= targetLvl) {
            return false;
        }
        int diff = unitLvl - targetLvl;
        return switch (unitLvl / 5) {
            case 0, 1 -> diff > 4;
            case 2, 3 -> diff > 5;
            case 4, 5 -> diff > 6;
            case 6, 7 -> diff > 7;
            default -> diff > 8;
        };
    }

    static int zeroDifference(int unitLevel) {
        if (unitLevel < 8) {
            return 5;
        }
        if (unitLevel < 10) {
            return 6;
        }
        if (unitLevel < 12) {
            return 7;
        }
        if (unitLevel < 16) {
            return 8;
        }
        if (unitLevel < 20) {
            return 9;
        }
        if (unitLevel < 30) {
            return 11;
        }
        if (unitLevel < 40) {
            return 12;
        }
        if (unitLevel < 45) {
            return 13;
        }
        if (unitLevel < 50) {
            return 14;
        }
        if (unitLevel < 55) {
            return 15;
        }
        if (unitLevel < 60) {
            return 16;
        }
        return 17;
    }

    /** BaseGain(unit_level, mob_level, content). */
    static float baseGain(int unitLevel, int mobLevel, boolean outland) {
        float base = unitLevel * 5 + (outland ? CONTENT_61_70_BONUS : CONTENT_1_60_BONUS);
        if (mobLevel >= unitLevel) {
            int diff = Math.min(mobLevel - unitLevel, 4);
            return base * (1.0f + 0.05f * diff);
        }
        if (!isTrivialLevelDifference(unitLevel, mobLevel)) {
            return base * (1.0f - (float) (unitLevel - mobLevel) / zeroDifference(unitLevel));
        }
        return 0f;
    }

    /** Gain(unit, target): no XP from pets/totems; elite ×2 (raid) or ×2.5 (non-raid); Rate.XP.Kill 1.0. */
    public static int gain(Player player, Creature target) {
        if (target.totem || target.pet) {
            return 0;
        }
        float xp = baseGain(player.level, target.level, player.mapId == OUTLAND_MAP);
        if (xp == 0f) {
            return 0;
        }
        if (target.rank != RANK_NORMAL && target.rank != RANK_RARE) {
            xp *= 2.5f;
        }
        return Math.round(xp);
    }
}
