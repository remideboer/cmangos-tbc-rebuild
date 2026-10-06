package org.tbc.world.companion;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.PetHandler;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;

/** LANG_ADDON bridge for the companion-only party-level control bar. */
public final class CompanionPartyAddon {
    public static final String PREFIX = "CompanionPartyBar";
    public static final int LANG_ADDON = 0xFFFFFFFF;
    private static final int CHAT_MSG_WHISPER = 0x07;

    private CompanionPartyAddon() {
    }

    public static boolean handleInbound(WorldSession session, World world, String message) {
        if (session == null || message == null) {
            return false;
        }
        int tab = message.indexOf('\t');
        if (tab <= 0 || !PREFIX.equals(message.substring(0, tab))) {
            return false;
        }
        String body = message.substring(tab + 1);
        if ("enable".equals(body)) {
            session.setCompanionPartyAddonEnabled(true);
            pushState(session);
        } else if (body.startsWith("Action;")) {
            action(session, world, body);
        }
        return true;
    }

    public static void pushState(WorldSession session) {
        if (session == null || !session.companionPartyAddonEnabled()) {
            return;
        }
        Player owner = session.player();
        if (owner == null || owner.companion == null || owner.pet == null) {
            send(session, "State;0");
            return;
        }
        StringBuilder body = new StringBuilder("State;1;").append(owner.pet.name);
        for (int packed : owner.pet.actionBar) {
            body.append(';').append(packed & 0xFFFFFF)
                    .append(',').append((packed >>> 24) & 0xFF);
        }
        send(session, body.toString());
    }

    private static void action(WorldSession session, World world, String body) {
        Player owner = session.player();
        if (!session.companionPartyAddonEnabled()
                || owner == null || owner.companion == null || owner.pet == null) {
            return;
        }
        String[] fields = body.split(";", -1);
        if (fields.length != 3) {
            return;
        }
        int slot;
        long target;
        try {
            slot = Integer.parseInt(fields[1]);
            target = parseGuid(fields[2]);
        } catch (NumberFormatException e) {
            return;
        }
        if (slot < 1 || slot > owner.pet.actionBar.length) {
            return;
        }
        int packed = owner.pet.actionBar[slot - 1];
        WowBuffer packet = new WowBuffer(20);
        packet.putU64(owner.pet.guid);
        packet.putU32(packed);
        packet.putU64(target);
        PetHandler.action(session, world, packet);
        pushState(session);
    }

    private static long parseGuid(String value) {
        if (value.startsWith("0x") || value.startsWith("0X")) {
            return Long.parseUnsignedLong(value.substring(2), 16);
        }
        return Long.parseUnsignedLong(value);
    }

    private static void send(WorldSession session, String body) {
        Player player = session.player();
        if (player == null) {
            return;
        }
        String message = PREFIX + "\t" + body;
        session.send(Opcodes.SMSG_MESSAGECHAT,
                WorldSession.chatPacket(CHAT_MSG_WHISPER, LANG_ADDON,
                        player.guid, player.guid, message, 0));
    }
}
