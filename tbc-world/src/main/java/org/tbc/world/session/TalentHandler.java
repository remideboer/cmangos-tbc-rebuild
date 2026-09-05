package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;

/** CMSG_LEARN_TALENT. Player.cpp LearnTalent. Layout: spec/03-protocol/packets/talents.md */
public final class TalentHandler {
    /** DBCStructure.h MAX_TALENT_RANK. */
    public static final int MAX_TALENT_RANK = 5;
    /** Talent.dbc 124 Improved Heroic Strike; RankID[0..2] = spell_template 12282/12663/12664. */
    public static final int TALENT_IMPROVED_HEROIC_STRIKE = 124;
    public static final int SPELL_IMPROVED_HEROIC_STRIKE_R1 = 12282;
    public static final int SPELL_IMPROVED_HEROIC_STRIKE_R2 = 12663;
    public static final int SPELL_IMPROVED_HEROIC_STRIKE_R3 = 12664;
    /** TalentTab.dbc 161 Arms; ClassMask warrior. */
    public static final int TALENT_TAB_WARRIOR_ARMS = 161;
    public static final int CLASSMASK_WARRIOR = 1;

    private TalentHandler() {}

    public static void learn(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 8) {
            return;
        }
        int talentId = in.getU32();
        int talentRank = in.getU32();
        Player p = s.player();
        int curPoints = p.getInt(UpdateFields.PLAYER_CHARACTER_POINTS1);
        if (curPoints == 0) {
            return;
        }
        if (talentRank >= MAX_TALENT_RANK) {
            return;
        }
        ObjectMgr.Talent talent = world.objectMgr.talents.get(talentId);
        if (talent == null) {
            return;
        }
        ObjectMgr.TalentTab tab = world.objectMgr.talentTabs.get(talent.tab());
        if (tab == null) {
            return;
        }
        int classMask = p.clazz <= 0 ? 0 : 1 << (p.clazz - 1);
        if ((classMask & tab.classMask()) == 0) {
            return;
        }
        int curMax = 0;
        for (int k = MAX_TALENT_RANK - 1; k >= 0; k--) {
            int sid = talent.rank(k);
            if (sid != 0 && p.spells.contains(sid)) {
                curMax = k + 1;
                break;
            }
        }
        if (curMax >= talentRank + 1) {
            return;
        }
        int cost = talentRank - curMax + 1;
        if (curPoints < cost) {
            return;
        }
        if (talent.dependsOn() > 0) {
            ObjectMgr.Talent dep = world.objectMgr.talents.get(talent.dependsOn());
            if (dep != null) {
                boolean enough = false;
                for (int i = talent.dependsOnRank(); i < MAX_TALENT_RANK; i++) {
                    int sid = dep.rank(i);
                    if (sid != 0 && p.spells.contains(sid)) {
                        enough = true;
                    }
                }
                if (!enough) {
                    return;
                }
            }
        }
        if (talent.dependsOnSpell() != 0 && !p.spells.contains(talent.dependsOnSpell())) {
            return;
        }
        int spent = 0;
        if (talent.row() > 0) {
            for (ObjectMgr.Talent tmp : world.objectMgr.talents.values()) {
                if (tmp.tab() != talent.tab()) {
                    continue;
                }
                for (int j = 0; j < MAX_TALENT_RANK; j++) {
                    int sid = tmp.rank(j);
                    if (sid != 0 && p.spells.contains(sid)) {
                        spent += j + 1;
                    }
                }
            }
        }
        if (spent < talent.row() * MAX_TALENT_RANK) {
            return;
        }
        int spellId = talent.rank(talentRank);
        if (spellId == 0) {
            return;
        }
        if (p.spells.contains(spellId)) {
            return;
        }
        for (int i = 0; i < talentRank; i++) {
            int lower = talent.rank(i);
            if (lower != 0) {
                p.spells.remove(Integer.valueOf(lower));
            }
        }
        p.spells.add(spellId);
        p.setInt(UpdateFields.PLAYER_CHARACTER_POINTS1, curPoints - cost);
        WowBuffer learned = new WowBuffer(4);
        learned.putU32(spellId);
        s.send(Opcodes.SMSG_LEARNED_SPELL, learned.array());
    }
}
