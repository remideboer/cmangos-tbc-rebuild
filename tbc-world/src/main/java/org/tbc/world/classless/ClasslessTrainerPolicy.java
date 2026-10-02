package org.tbc.world.classless;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.session.TrainerHandler;

/**
 * Class trainers list/buy for classless with no spell whitelist; normal classes unchanged.
 * Level / chain / money gates stay in {@link org.tbc.world.session.TrainerService}.
 */
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
        return ClasslessCharacterPolicy.isClassless(p);
    }

    /** Keep trainer list rows (classless: all spell ids; other classes: no id whitelist). */
    public static boolean listIncludes(Player p, int spellId) {
        if (spellId <= 0) {
            return false;
        }
        return true;
    }
}
