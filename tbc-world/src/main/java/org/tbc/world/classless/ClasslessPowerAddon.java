package org.tbc.world.classless;

import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.WorldSession;
import org.tbc.world.world.World;

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
        return handleInbound(session, null, message);
    }

    public static boolean handleInbound(WorldSession session, World world, String message) {
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
        if (body.startsWith("SpendStat;")) {
            spend(session, world, body);
            return true;
        }
        if (body.startsWith("ApplyStats;")) {
            applyStats(session, world, body);
            return true;
        }
        if ("ResetStats".equals(body)) {
            resetStats(session, world);
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
        pushStats(session);
        pushResetCost(session);
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

    public static void pushStats(WorldSession session) {
        if (session == null || !session.heroPowerAddonEnabled()) {
            return;
        }
        Player p = session.player();
        if (!ClasslessCharacterPolicy.isClassless(p)) {
            return;
        }
        send(session, "StatUpdate;" + p.heroStats.unspent()
                + ";" + p.heroStats.spent(HeroStatAllocation.STR)
                + ";" + p.heroStats.spent(HeroStatAllocation.AGI)
                + ";" + p.heroStats.spent(HeroStatAllocation.STA)
                + ";" + p.heroStats.spent(HeroStatAllocation.INTELLECT)
                + ";" + p.heroStats.spent(HeroStatAllocation.SPI));
    }

    public static void pushResetCost(WorldSession session) {
        if (session == null || !session.heroPowerAddonEnabled()) {
            return;
        }
        Player p = session.player();
        if (!ClasslessCharacterPolicy.isClassless(p)) {
            return;
        }
        int count = p.heroStats.resetCount();
        send(session, "ResetCost;" + HeroStatResetCost.nextCopperCost(count) + ";" + count);
    }

    public static void pushStatsIfDinged(WorldSession session, int[] changedFields) {
        if (changedFields == null) {
            return;
        }
        for (int f : changedFields) {
            if (f == UpdateFields.UNIT_FIELD_LEVEL) {
                pushStats(session);
                return;
            }
        }
    }

    private static void spend(WorldSession session, World world, String body) {
        Player p = session.player();
        if (!ClasslessCharacterPolicy.isClassless(p)) {
            return;
        }
        String[] parts = body.split(";");
        if (parts.length < 3) {
            return;
        }
        int stat;
        int amount;
        try {
            stat = Integer.parseInt(parts[1]);
            amount = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return;
        }
        int oldHp = p.health();
        int oldMaxHp = p.maxHealth();
        int oldMana = p.getInt(UpdateFields.UNIT_FIELD_POWER1);
        int oldMaxMana = p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1);
        if (!p.spendHeroStat(stat, amount)) {
            return;
        }
        pushStatValues(session, world, p, oldHp, oldMaxHp, oldMana, oldMaxMana);
        pushStats(session);
    }

    private static void applyStats(WorldSession session, World world, String body) {
        Player p = session.player();
        if (!ClasslessCharacterPolicy.isClassless(p)) {
            return;
        }
        String[] parts = body.split(";");
        if (parts.length < 6) {
            return;
        }
        int[] spent = new int[5];
        try {
            for (int i = 0; i < 5; i++) {
                spent[i] = Integer.parseInt(parts[i + 1]);
            }
        } catch (NumberFormatException e) {
            return;
        }
        int oldHp = p.health();
        int oldMaxHp = p.maxHealth();
        int oldMana = p.getInt(UpdateFields.UNIT_FIELD_POWER1);
        int oldMaxMana = p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1);
        if (!p.applyHeroStats(spent)) {
            return;
        }
        pushStatValues(session, world, p, oldHp, oldMaxHp, oldMana, oldMaxMana);
        pushStats(session);
    }

    private static void resetStats(WorldSession session, World world) {
        Player p = session.player();
        if (!ClasslessCharacterPolicy.isClassless(p)) {
            return;
        }
        int oldHp = p.health();
        int oldMaxHp = p.maxHealth();
        int oldMana = p.getInt(UpdateFields.UNIT_FIELD_POWER1);
        int oldMaxMana = p.getInt(UpdateFields.UNIT_FIELD_MAXPOWER1);
        if (!p.resetHeroStats()) {
            return;
        }
        pushStatValues(session, world, p, oldHp, oldMaxHp, oldMana, oldMaxMana);
        if (world != null) {
            var coin = UpdateBuilder.maybeCompress(
                    UpdateBuilder.values(p, UpdateFields.PLAYER_FIELD_COINAGE));
            session.send(coin.opcode(), coin.payload());
        }
        pushStats(session);
        pushResetCost(session);
    }

    private static void pushStatValues(WorldSession session, World world, Player p,
            int oldHp, int oldMaxHp, int oldMana, int oldMaxMana) {
        if (world == null) {
            return;
        }
        world.objectMgr.applyEquippedMelee(p);
        p.restoreResourcePercent(oldHp, oldMaxHp, oldMana, oldMaxMana);
        var upd = UpdateBuilder.maybeCompress(
                UpdateBuilder.values(p,
                        UpdateFields.UNIT_FIELD_STAT0, UpdateFields.UNIT_FIELD_STAT1,
                        UpdateFields.UNIT_FIELD_STAT2, UpdateFields.UNIT_FIELD_STAT3,
                        UpdateFields.UNIT_FIELD_STAT4, UpdateFields.UNIT_FIELD_RESISTANCES,
                        UpdateFields.UNIT_FIELD_MAXHEALTH, UpdateFields.UNIT_FIELD_HEALTH,
                        UpdateFields.UNIT_FIELD_MAXPOWER1, UpdateFields.UNIT_FIELD_POWER1));
        session.send(upd.opcode(), upd.payload());
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
