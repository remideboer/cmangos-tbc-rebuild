package org.tbc.world.classless;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.WorldSession;

/**
 * Pushes mana/rage/energy to the HeroPowerBars client AddOn via LANG_ADDON whispers
 * (TBC UnitRage/UnitEnergy stay empty when primary power is mana).
 */
public final class ClasslessPowerAddon {
    public static final String PREFIX = "HeroPowerBars";
    public static final int LANG_ADDON = 0xFFFFFFFF;
    /** CMaNGOS TBC: CHAT_MSG_WHISPER + LANG_ADDON for addon payloads. */
    public static final int CHAT_MSG_WHISPER = 0x07;

    private ClasslessPowerAddon() {
    }

    public static boolean handleInbound(WorldSession session, String message) {
        if (session == null || message == null) {
            return false;
        }
        int tab = message.indexOf('\t');
        if (tab <= 0) {
            return false;
        }
        String prefix = message.substring(0, tab);
        String body = message.substring(tab + 1);
        if (!PREFIX.equals(prefix)) {
            return false;
        }
        if ("enable".equals(body)) {
            enable(session);
            return true;
        }
        return true; // known prefix, swallow
    }

    public static void enable(WorldSession session) {
        Player p = session.player();
        if (!ClasslessCharacterPolicy.isClassless(p)) {
            return;
        }
        session.setHeroPowerAddonEnabled(true);
        send(session, "AddonEnabled");
        pushAll(session);
    }

    public static void pushAll(WorldSession session) {
        if (session == null || !session.heroPowerAddonEnabled()) {
            return;
        }
        Player p = session.player();
        if (!ClasslessCharacterPolicy.isClassless(p)) {
            return;
        }
        push(session, Player.POWER_MANA);
        push(session, Player.POWER_RAGE);
        push(session, Player.POWER_ENERGY);
    }

    /** Push one power after a VALUES update that touched that field. */
    public static void pushIfPowerFields(WorldSession session, int[] changedFields) {
        if (session == null || !session.heroPowerAddonEnabled() || changedFields == null) {
            return;
        }
        boolean mana = false;
        boolean rage = false;
        boolean energy = false;
        for (int f : changedFields) {
            if (f == UpdateFields.UNIT_FIELD_POWER1 || f == UpdateFields.UNIT_FIELD_MAXPOWER1) {
                mana = true;
            } else if (f == UpdateFields.UNIT_FIELD_POWER2 || f == UpdateFields.UNIT_FIELD_MAXPOWER2) {
                rage = true;
            } else if (f == UpdateFields.UNIT_FIELD_POWER4 || f == UpdateFields.UNIT_FIELD_MAXPOWER4) {
                energy = true;
            }
        }
        if (mana) {
            push(session, Player.POWER_MANA);
        }
        if (rage) {
            push(session, Player.POWER_RAGE);
        }
        if (energy) {
            push(session, Player.POWER_ENERGY);
        }
    }

    public static void push(WorldSession session, int powerType) {
        Player p = session.player();
        if (p == null) {
            return;
        }
        int cur;
        int max;
        if (powerType == Player.POWER_RAGE) {
            cur = p.rage() / 10;
            max = p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER2) / 10;
        } else if (powerType == Player.POWER_ENERGY) {
            cur = p.getInt(UpdateFields.UNIT_FIELD_POWER4);
            max = p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER4);
        } else if (powerType == Player.POWER_MANA) {
            cur = p.getInt(UpdateFields.UNIT_FIELD_POWER1);
            max = p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1);
        } else {
            return;
        }
        send(session, "PowerUpdate#" + powerType + ";" + cur + ";" + max);
    }

    public static void send(WorldSession session, String body) {
        Player p = session.player();
        if (p == null) {
            return;
        }
        String msg = PREFIX + "\t" + body;
        session.send(Opcodes.SMSG_MESSAGECHAT,
                WorldSession.chatPacket(CHAT_MSG_WHISPER, LANG_ADDON, p.guid, p.guid, msg, 0));
    }
}
