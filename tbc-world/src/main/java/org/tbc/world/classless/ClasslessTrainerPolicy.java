package org.tbc.world.classless;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.session.TrainerHandler;

/**
 * Class trainers list/buy for classless with no spell whitelist; normal classes unchanged.
 * Level / chain / money gates stay in {@link org.tbc.world.session.TrainerService}.
 * Classless buy/list price is {@code 100 × 2^classSpellsLearned} (ignores trainer-row spellCost).
 */
public final class ClasslessTrainerPolicy {
    /** First Hero trainer purchase copper; each next spell doubles. */
    public static final int CLASSLESS_TRAINER_BASE_COST = 100;

    private ClasslessTrainerPolicy() {
    }

    /**
     * @return true when this classless player may use the class trainer NPC.
     */
    public static boolean isTrainerOf(Player p, Creature c, ObjectMgr mgr) {
        if (!ClasslessCharacterPolicy.isClassless(p) || c == null || mgr == null) {
            return false;
        }
        if (mgr.spellsForTrainer(c.entry).isEmpty()) {
            return false;
        }
        if (mgr.trainerType(c.entry) != TrainerHandler.TRAINER_TYPE_CLASS) {
            return false;
        }
        return HeroClassUnlock.trainerClassUnlocked(p, mgr.trainerClass.getOrDefault(c.entry, 0));
    }

    public static boolean mayBuy(Player p, int spellId) {
        return ClasslessCharacterPolicy.isClassless(p);
    }

    /** Keep trainer list rows (classless: all spell ids; other classes: no id whitelist). */
    public static boolean listIncludes(Player p, int spellId) {
        if (spellId <= 0) {
            return false;
        }
        return true;
    }

    /**
     * Trainer-bought class abilities on the player: spells excluding Auto Attack 6603 and
     * racial language spells (create always grants Common/Orcish etc.).
     */
    public static int classSpellsLearned(Player p) {
        if (p == null || p.spells == null) {
            return 0;
        }
        int n = 0;
        for (int spell : p.spells) {
            if (spell == ClasslessConfig.AUTO_ATTACK) {
                continue;
            }
            if (org.tbc.world.content.ChrStatic.isLanguageSpell(spell)) {
                continue;
            }
            n++;
        }
        return n;
    }

    /**
     * Classless trainer price: {@code 100 × 2^learned}. Clamps overflow to
     * {@link Integer#MAX_VALUE}. Normal classes use raw {@code spellCost} via {@link #effectiveCost}.
     */
    public static int buyCost(int learned) {
        int n = Math.max(0, learned);
        if (n >= 31) {
            return Integer.MAX_VALUE;
        }
        long cost = (long) CLASSLESS_TRAINER_BASE_COST << n;
        return cost > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cost;
    }

    /** List/buy copper — geometric for classless (ignores row cost), raw otherwise. */
    public static int effectiveCost(Player p, int spellCost) {
        if (!ClasslessCharacterPolicy.isClassless(p)) {
            return Math.max(0, spellCost);
        }
        return buyCost(classSpellsLearned(p));
    }
}
