package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;

/** CMSG_PUSHQUESTTOPARTY (QuestHandler.cpp HandlePushQuestToParty). */
public final class QuestShareHandler {
    private QuestShareHandler() {}

    public static void register(OpcodeTable t) {
        t.register(Opcodes.CMSG_PUSHQUESTTOPARTY, (s, w, in) -> pushQuestToParty(s, in));
    }

    public static void pushQuestToParty(WorldSession s, WowBuffer in) {
        Player p = s.player();
        int q = in.remaining() >= 4 ? in.getU32() : 0;
        WowBuffer result = new WowBuffer(9);
        result.putU64(p.guid);
        result.putU8(0);
        s.send(Opcodes.MSG_QUEST_PUSH_RESULT, result.array());
        if (p.group != null) {
            for (Player m : p.group.members) {
                if (m != p && m.session != null) {
                    WowBuffer d = new WowBuffer(8);
                    d.putU32(q);
                    m.session.send(Opcodes.SMSG_QUESTGIVER_QUEST_DETAILS, d.array());
                }
            }
        }
    }
}
