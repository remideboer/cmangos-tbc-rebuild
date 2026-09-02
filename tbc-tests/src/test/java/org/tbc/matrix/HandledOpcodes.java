package org.tbc.matrix;

import org.tbc.world.net.wow8606.Opcodes;

import java.util.Set;

/** Opcodes with a dedicated branch in WorldSession.handle (including Handle_NULL-style early returns). */
final class HandledOpcodes {
    private HandledOpcodes() {}

    static final Set<Integer> SET = Set.of(
            Opcodes.CMSG_WARDEN_DATA,
            Opcodes.CMSG_PING,
            Opcodes.CMSG_AUTH_SESSION,
            Opcodes.CMSG_CHAR_ENUM,
            Opcodes.CMSG_CHAR_CREATE,
            Opcodes.CMSG_CHAR_DELETE,
            Opcodes.CMSG_PLAYER_LOGIN,
            Opcodes.MSG_MOVE_WORLDPORT_ACK,
            Opcodes.CMSG_FORCE_MOVE_ROOT_ACK,
            Opcodes.CMSG_FORCE_MOVE_UNROOT_ACK,
            Opcodes.CMSG_LOGOUT_REQUEST,
            Opcodes.CMSG_LOGOUT_CANCEL,
            Opcodes.CMSG_PLAYER_LOGOUT,
            Opcodes.CMSG_MESSAGECHAT,
            Opcodes.CMSG_NAME_QUERY,
            Opcodes.CMSG_PET_NAME_QUERY,
            Opcodes.CMSG_GUILD_QUERY,
            Opcodes.CMSG_ITEM_QUERY_SINGLE,
            Opcodes.CMSG_PAGE_TEXT_QUERY,
            Opcodes.CMSG_QUEST_QUERY,
            Opcodes.CMSG_GAMEOBJECT_QUERY,
            Opcodes.CMSG_CREATURE_QUERY,
            Opcodes.CMSG_WHOIS,
            Opcodes.CMSG_QUERY_TIME,
            Opcodes.CMSG_TIME_SYNC_RESP,
            Opcodes.CMSG_SET_ACTIVE_MOVER,
            Opcodes.CMSG_ZONEUPDATE,
            Opcodes.CMSG_CONTACT_LIST,
            Opcodes.CMSG_SET_ACTION_BUTTON,
            Opcodes.CMSG_TUTORIAL_FLAG,
            Opcodes.CMSG_NEXT_CINEMATIC_CAMERA,
            Opcodes.CMSG_COMPLETE_CINEMATIC,
            Opcodes.CMSG_ATTACKSWING,
            Opcodes.CMSG_ATTACKSTOP,
            Opcodes.CMSG_SETSHEATHED,
            Opcodes.CMSG_SET_SELECTION,
            Opcodes.CMSG_LOOT,
            Opcodes.CMSG_LOOT_MONEY,
            Opcodes.CMSG_LOOT_RELEASE,
            Opcodes.CMSG_CAST_SPELL,
            Opcodes.CMSG_GOSSIP_HELLO,
            Opcodes.CMSG_QUESTGIVER_HELLO,
            Opcodes.CMSG_QUESTGIVER_QUERY_QUEST,
            Opcodes.CMSG_QUESTGIVER_ACCEPT_QUEST,
            Opcodes.CMSG_QUESTGIVER_COMPLETE_QUEST,
            Opcodes.CMSG_QUESTGIVER_CHOOSE_REWARD,
            Opcodes.CMSG_LIST_INVENTORY,
            Opcodes.CMSG_GROUP_INVITE,
            Opcodes.CMSG_GROUP_ACCEPT,
            Opcodes.CMSG_GROUP_DISBAND,
            Opcodes.CMSG_INITIATE_TRADE,
            Opcodes.CMSG_WHO,
            Opcodes.CMSG_ADD_FRIEND,
            Opcodes.CMSG_SEND_MAIL,
            Opcodes.MSG_AUCTION_HELLO,
            Opcodes.CMSG_BATTLEMASTER_JOIN,
            Opcodes.CMSG_BATTLEMASTER_JOIN_ARENA,
            Opcodes.CMSG_REPOP_REQUEST,
            Opcodes.CMSG_RECLAIM_CORPSE,
            Opcodes.CMSG_SELF_RES,
            Opcodes.CMSG_PET_ACTION,
            Opcodes.CMSG_JOIN_CHANNEL,
            Opcodes.CMSG_BUY_ITEM,
            Opcodes.CMSG_BUY_ITEM_IN_SLOT,
            Opcodes.CMSG_LEARN_TALENT,
            Opcodes.CMSG_TRAINER_LIST,
            Opcodes.CMSG_ACTIVATETAXI,
            Opcodes.CMSG_ACTIVATETAXIEXPRESS,
            Opcodes.CMSG_GAMEOBJ_USE,
            Opcodes.CMSG_GMTICKET_CREATE,
            Opcodes.CMSG_INSPECT,
            Opcodes.CMSG_DUEL_ACCEPTED,
            Opcodes.CMSG_TOGGLE_PVP,
            Opcodes.CMSG_OPEN_ITEM,
            Opcodes.CMSG_AREA_SPIRIT_HEALER_QUERY,
            Opcodes.CMSG_AREA_SPIRIT_HEALER_QUEUE,
            Opcodes.MSG_PVP_LOG_DATA,
            Opcodes.CMSG_VOICE_SESSION_ENABLE,
            Opcodes.CMSG_SWAP_INV_ITEM,
            Opcodes.CMSG_SWAP_ITEM,
            Opcodes.CMSG_DESTROYITEM,
            Opcodes.CMSG_AREATRIGGER
    );

    static boolean handled(int id) {
        if (id >= Opcodes.MSG_MOVE_START_FORWARD && id <= Opcodes.MSG_MOVE_HEARTBEAT) {
            return true;
        }
        return SET.contains(id);
    }
}
