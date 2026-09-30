package org.tbc.world.session;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;

/**
 * CMaNGOS Player::GetTrainerSpellState — green/red/gray for trainer list and buy.
 */
public final class TrainerService {
    private TrainerService() {}

    public static int state(Player p, ObjectMgr.TrainerSpell t) {
        return state(p, t, null);
    }

    public static int state(Player p, ObjectMgr.TrainerSpell t, ObjectMgr mgr) {
        if (p == null || t == null) {
            return TrainerHandler.TRAINER_SPELL_RED;
        }
        if (p.spells.contains(t.spell())) {
            return TrainerHandler.TRAINER_SPELL_GRAY;
        }
        int reqLevel = t.reqLevel();
        if (reqLevel <= 0 && mgr != null) {
            reqLevel = mgr.spellBaseLevel.getOrDefault(t.spell(), 0);
        }
        if (p.level < reqLevel) {
            return TrainerHandler.TRAINER_SPELL_RED;
        }
        if (t.reqSkill() != 0 && p.skillValue(t.reqSkill()) < t.reqSkillValue()) {
            return TrainerHandler.TRAINER_SPELL_RED;
        }
        if (t.reqAbility0() != 0 && !p.spells.contains(t.reqAbility0())) {
            return TrainerHandler.TRAINER_SPELL_RED;
        }
        if (t.reqAbility1() != 0 && !p.spells.contains(t.reqAbility1())) {
            return TrainerHandler.TRAINER_SPELL_RED;
        }
        if (t.reqAbility2() != 0 && !p.spells.contains(t.reqAbility2())) {
            return TrainerHandler.TRAINER_SPELL_RED;
        }
        if (mgr != null) {
            ObjectMgr.SpellChainNode chain = mgr.spellChain.get(t.spell());
            if (chain != null) {
                if (chain.prev() != 0 && !p.spells.contains(chain.prev())) {
                    return TrainerHandler.TRAINER_SPELL_RED;
                }
                if (chain.req() != 0 && !p.spells.contains(chain.req())) {
                    return TrainerHandler.TRAINER_SPELL_RED;
                }
            }
        }
        if (t.primaryProfessionFirstRank() && primaryProfessionCount(p) >= PRIMARY_PROFESSIONS.length) {
            return TrainerHandler.TRAINER_SPELL_RED;
        }
        return TrainerHandler.TRAINER_SPELL_GREEN;
    }

    /** Occupied primary trade skills (SharedDefines profession category). */
    public static int primaryProfessionCount(Player p) {
        int n = 0;
        for (int skill : PRIMARY_PROFESSIONS) {
            if (p.hasSkill(skill)) {
                n++;
            }
        }
        return n;
    }

    /** SharedDefines.h primary professions (not cooking/fishing/first-aid). */
    static final int[] PRIMARY_PROFESSIONS = {
            164, 165, 171, 182, 186, 197, 202, 333, 393, 755, 773
    };
}
