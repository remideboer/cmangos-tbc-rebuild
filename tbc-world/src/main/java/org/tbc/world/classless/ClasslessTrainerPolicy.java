package org.tbc.world.classless;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.session.TrainerHandler;

/** Class trainers list/buy for classless via eligibility catalog; normal classes unchanged. */
public final class ClasslessTrainerPolicy {
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
        return mgr.trainerType(c.entry) == TrainerHandler.TRAINER_TYPE_CLASS;
    }

    public static boolean mayBuy(Player p, int spellId) {
        return ClasslessCharacterPolicy.isClassless(p)
                && ClasslessConfig.get().trainerSpellEligible(spellId);
    }

    /** Keep trainer list rows that are eligible (or all rows when not classless). */
    public static boolean listIncludes(Player p, int spellId) {
        if (!ClasslessCharacterPolicy.isClassless(p)) {
            return true;
        }
        return ClasslessConfig.get().trainerSpellEligible(spellId);
    }
}
