package org.tbc.world.session;

import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;

/**
 * CMaNGOS Player::GetTrainerSpellState — green/red/gray for trainer list and buy.
 */
public final class TrainerService {
    private TrainerService() {}

    public static int state(Player p, ObjectMgr.TrainerSpell t) {
        if (p == null || t == null) {
            return TrainerHandler.TRAINER_SPELL_RED;
        }
        if (p.spells.contains(t.spell())) {
            return TrainerHandler.TRAINER_SPELL_GRAY;
        }
        if (p.level < t.reqLevel()) {
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
        if (t.primaryProfessionFirstRank() && primaryProfessionCount(p) >= Player.MAX_PRIMARY_TRADE_SKILL) {
            return TrainerHandler.TRAINER_SPELL_RED;
        }
        return TrainerHandler.TRAINER_SPELL_GREEN;
    }

    /** CMaNGOS GetFreePrimaryProfessionPoints — occupied primary trade skills. */
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
