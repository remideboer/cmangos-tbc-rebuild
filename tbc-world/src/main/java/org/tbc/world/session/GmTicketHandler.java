package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.net.wow8606.Opcodes;

/** GM ticket read/update/delete/system status (GMTicketHandler.cpp). */
public final class GmTicketHandler {
    private GmTicketHandler() {}

    public static void register(OpcodeTable t) {
        t.register(Opcodes.CMSG_GMTICKET_CREATE, (s, w, in) -> create(s, in))
                .register(Opcodes.CMSG_GMTICKET_GETTICKET, (s, w, in) -> getTicket(s))
                .register(Opcodes.CMSG_GMTICKET_UPDATETEXT, (s, w, in) -> updateText(s, in))
                .register(Opcodes.CMSG_GMTICKET_DELETETICKET, (s, w, in) -> deleteTicket(s))
                .register(Opcodes.CMSG_GMTICKET_SYSTEMSTATUS, (s, w, in) -> systemStatus(s));
    }

    /** HandleGMTicketCreateOpcode — remember the text, answer GMTICKET_RESPONSE_CREATE_SUCCESS (0 here). */
    public static void create(WorldSession s, WowBuffer in) {
        s.social().openTicket(in.getCString());
        WowBuffer ok = new WowBuffer(4);
        ok.putU32(0);
        s.send(Opcodes.SMSG_GMTICKET_CREATE, ok.array());
    }

    /** GMTICKET_STATUS_HASTEXT 0x06 only with an open ticket; else DEFAULT 0x0A. */
    public static void getTicket(WorldSession s) {
        if (!s.social().hasTicket()) {
            WowBuffer none = new WowBuffer(4);
            none.putU32(0x0A);
            s.send(Opcodes.SMSG_GMTICKET_GETTICKET, none.array());
            return;
        }
        WowBuffer t = new WowBuffer(32);
        t.putU32(0x06);
        t.putCString(s.social().ticket());
        t.putU8(0);
        t.putFloat(0);
        t.putFloat(0);
        t.putFloat(0);
        t.putU8(0);
        t.putU8(0);
        s.send(Opcodes.SMSG_GMTICKET_GETTICKET, t.array());
    }

    /** HandleGMTicketUpdateTextOpcode — UPDATE_SUCCESS 4 / UPDATE_ERROR 5. */
    public static void updateText(WorldSession s, WowBuffer in) {
        String message = in.remaining() > 0 ? in.getCString() : "";
        if (!s.social().hasTicket() || message.isEmpty()) {
            WowBuffer err = new WowBuffer(4);
            err.putU32(5);
            s.send(Opcodes.SMSG_GMTICKET_UPDATETEXT, err.array());
            return;
        }
        s.social().openTicket(message);
        WowBuffer ok = new WowBuffer(4);
        ok.putU32(4);
        s.send(Opcodes.SMSG_GMTICKET_UPDATETEXT, ok.array());
    }

    /** HandleGMTicketDeleteTicketOpcode — TICKET_DELETED 9 / NOT_EXIST 0. */
    public static void deleteTicket(WorldSession s) {
        if (!s.social().hasTicket()) {
            WowBuffer none = new WowBuffer(4);
            none.putU32(0);
            s.send(Opcodes.SMSG_GMTICKET_DELETETICKET, none.array());
            return;
        }
        s.social().clearTicket();
        WowBuffer del = new WowBuffer(4);
        del.putU32(9);
        s.send(Opcodes.SMSG_GMTICKET_DELETETICKET, del.array());
    }

    /** HandleGMTicketSystemStatusOpcode — queue enabled = 1. */
    public static void systemStatus(WorldSession s) {
        WowBuffer st = new WowBuffer(4);
        st.putU32(1);
        s.send(Opcodes.SMSG_GMTICKET_SYSTEMSTATUS, st.array());
    }
}
