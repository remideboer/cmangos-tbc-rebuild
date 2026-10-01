package org.tbc.world.classless;

import org.tbc.world.content.ObjectMgr.ItemTemplate;
import org.tbc.world.entity.Player;

/**
 * Per-item reductions on unproficient armor only. Cloth / proficient pieces are
 * unchanged. Recalc by calling {@link #accumulate} for each equipped armor piece.
 */
public final class ArmorPenaltyPolicy {
    public record GearMods(int armor, int strength, int agility, float speedPenaltyPct) {
        public static final GearMods ZERO = new GearMods(0, 0, 0, 0f);
    }

    private ArmorPenaltyPolicy() {
    }

    public static int armorMask(int subClass) {
        if (subClass < 0 || subClass > 31) {
            return 0;
        }
        return 1 << subClass;
    }

    /**
     * Scale one piece's armor / Str / Agi and return its speed penalty contribution.
     * Non-armor or proficient pieces pass through unchanged (speed 0).
     */
    public static GearMods forPiece(Player p, ItemTemplate t) {
        if (p == null || t == null || t.itemClass != Player.ITEM_CLASS_ARMOR) {
            return GearMods.ZERO;
        }
        int mask = armorMask(t.subClass);
        boolean proficient = (p.armorProficiency() & mask) != 0;
        int step = ClasslessConfig.get().armorStep(t.subClass);
        if (proficient || step <= 0) {
            int str = 0;
            int agi = 0;
            for (int i = 0; i < t.statType.length; i++) {
                if (t.statType[i] == 4) {
                    str += t.statValue[i];
                } else if (t.statType[i] == 3) {
                    agi += t.statValue[i];
                }
            }
            return new GearMods(t.armor, str, agi, 0f);
        }
        ClasslessConfig cfg = ClasslessConfig.get();
        float armorCut = cfg.armorReductionForStep(step);
        float statCut = cfg.strAgiReductionForStep(step);
        float speedCut = cfg.speedReductionForStep(step);
        int armor = Math.round(t.armor * (1f - armorCut));
        int str = 0;
        int agi = 0;
        for (int i = 0; i < t.statType.length; i++) {
            if (t.statType[i] == 4) {
                str += Math.round(t.statValue[i] * (1f - statCut));
            } else if (t.statType[i] == 3) {
                agi += Math.round(t.statValue[i] * (1f - statCut));
            }
        }
        return new GearMods(armor, str, agi, speedCut);
    }
}
