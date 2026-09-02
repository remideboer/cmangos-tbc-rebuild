package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.ChrStatic;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** spec/03-protocol/packets/login-burst.md — order is normative. */
public final class LoginBurst {
    public static final int[] ORDER = {
            Opcodes.MSG_SET_DUNGEON_DIFFICULTY,
            Opcodes.SMSG_LOGIN_VERIFY_WORLD,
            Opcodes.SMSG_ACCOUNT_DATA_TIMES,
            Opcodes.SMSG_FEATURE_SYSTEM_STATUS,
            Opcodes.SMSG_EXPECTED_SPAM_RECORDS,
            Opcodes.SMSG_MOTD,
            Opcodes.SMSG_SET_REST_START,
            Opcodes.SMSG_BINDPOINTUPDATE,
            Opcodes.SMSG_TUTORIAL_FLAGS,
            Opcodes.SMSG_INSTANCE_DIFFICULTY,
            Opcodes.SMSG_INITIAL_SPELLS,
            Opcodes.SMSG_SEND_UNLEARN_SPELLS,
            Opcodes.SMSG_ACTION_BUTTONS,
            Opcodes.SMSG_INITIALIZE_FACTIONS,
            Opcodes.SMSG_LOGIN_SETTIMESPEED,
            Opcodes.SMSG_UPDATE_OBJECT,
            Opcodes.SMSG_CONTACT_LIST,
            Opcodes.SMSG_INIT_WORLD_STATES,
            Opcodes.SMSG_TIME_SYNC_REQ,
            Opcodes.SMSG_LFG_DISABLED
    };

    private LoginBurst() {}

    public static List<Integer> send(WorldSession s, Player p, World world) {
        List<Integer> sent = new ArrayList<>();
        s.send(Opcodes.MSG_SET_DUNGEON_DIFFICULTY, u32(p.difficulty, 1, 0));
        sent.add(Opcodes.MSG_SET_DUNGEON_DIFFICULTY);
        WowBuffer vw = new WowBuffer(20);
        vw.putU32(p.mapId);
        vw.putFloat(p.x);
        vw.putFloat(p.y);
        vw.putFloat(p.z);
        vw.putFloat(p.o);
        s.send(Opcodes.SMSG_LOGIN_VERIFY_WORLD, vw.array());
        sent.add(Opcodes.SMSG_LOGIN_VERIFY_WORLD);
        s.send(Opcodes.SMSG_ACCOUNT_DATA_TIMES, new byte[128]);
        sent.add(Opcodes.SMSG_ACCOUNT_DATA_TIMES);
        s.send(Opcodes.SMSG_FEATURE_SYSTEM_STATUS, new byte[]{2, 0});
        sent.add(Opcodes.SMSG_FEATURE_SYSTEM_STATUS);
        s.send(Opcodes.SMSG_EXPECTED_SPAM_RECORDS, u32(0));
        sent.add(Opcodes.SMSG_EXPECTED_SPAM_RECORDS);
        WowBuffer motd = new WowBuffer(64);
        String[] lines = world.motd.split("@");
        motd.putU32(lines.length);
        for (String line : lines) {
            motd.putCString(line);
        }
        s.send(Opcodes.SMSG_MOTD, motd.array());
        sent.add(Opcodes.SMSG_MOTD);
        s.send(Opcodes.SMSG_SET_REST_START, u32(0));
        sent.add(Opcodes.SMSG_SET_REST_START);
        WowBuffer bind = new WowBuffer(20);
        bind.putFloat(p.bindX);
        bind.putFloat(p.bindY);
        bind.putFloat(p.bindZ);
        bind.putU32(p.bindMap);
        bind.putU32(p.bindZone);
        s.send(Opcodes.SMSG_BINDPOINTUPDATE, bind.array());
        sent.add(Opcodes.SMSG_BINDPOINTUPDATE);
        WowBuffer tut = new WowBuffer(32);
        for (int t : p.tut) {
            tut.putU32(t);
        }
        s.send(Opcodes.SMSG_TUTORIAL_FLAGS, tut.array());
        sent.add(Opcodes.SMSG_TUTORIAL_FLAGS);
        s.send(Opcodes.SMSG_INSTANCE_DIFFICULTY, u32(p.difficulty, 0));
        sent.add(Opcodes.SMSG_INSTANCE_DIFFICULTY);
        p.applyCreateFields();
        WowBuffer spells = new WowBuffer(8 + p.spells.size() * 4);
        spells.putU8(0);
        spells.putU16(p.spells.size());
        for (int id : p.spells) {
            spells.putU16(id);
            spells.putU16(0);
        }
        spells.putU16(0);
        s.send(Opcodes.SMSG_INITIAL_SPELLS, spells.array());
        sent.add(Opcodes.SMSG_INITIAL_SPELLS);
        s.send(Opcodes.SMSG_SEND_UNLEARN_SPELLS, u32(0));
        sent.add(Opcodes.SMSG_SEND_UNLEARN_SPELLS);
        WowBuffer ab = new WowBuffer(132 * 4);
        for (int b : p.actionButtons) {
            ab.putU32(b);
        }
        s.send(Opcodes.SMSG_ACTION_BUTTONS, ab.array());
        sent.add(Opcodes.SMSG_ACTION_BUTTONS);
        WowBuffer fac = new WowBuffer(4 + 128 * 5);
        fac.putU32(0x80);
        for (int i = 0; i < 128; i++) {
            fac.putU8(0);
            fac.putU32(0);
        }
        s.send(Opcodes.SMSG_INITIALIZE_FACTIONS, fac.array());
        sent.add(Opcodes.SMSG_INITIALIZE_FACTIONS);
        WowBuffer time = new WowBuffer(8);
        time.putU32(packedTime());
        time.putFloat(0.01666667f);
        s.send(Opcodes.SMSG_LOGIN_SETTIMESPEED, time.array());
        sent.add(Opcodes.SMSG_LOGIN_SETTIMESPEED);
        if (p.cinematic == 0) {
            p.cinematic = 1;
            int cine = org.tbc.world.content.ChrStatic.race(p.race).cinematic();
            s.send(Opcodes.SMSG_TRIGGER_CINEMATIC, u32(cine));
        }
        p.applyCreateFields();
        sendInventory(s, p);
        var upd = UpdateBuilder.maybeCompress(UpdateBuilder.createUnit(p, true, (int) world.nowMs()));
        s.send(upd.opcode(), upd.payload());
        sent.add(Opcodes.SMSG_UPDATE_OBJECT);
        sendKnownLanguages(s, p);
        SocialHandler.contactList(s, world);
        sent.add(Opcodes.SMSG_CONTACT_LIST);
        WowBuffer ws = new WowBuffer(16);
        ws.putU32(p.mapId);
        ws.putU32(p.zoneId);
        ws.putU32(p.zoneId);
        ws.putU16(1);
        ws.putU32(0xC77);
        ws.putU32(0);
        s.send(Opcodes.SMSG_INIT_WORLD_STATES, ws.array());
        sent.add(Opcodes.SMSG_INIT_WORLD_STATES);
        s.send(Opcodes.SMSG_TIME_SYNC_REQ, u32(p.timeSyncCounter));
        sent.add(Opcodes.SMSG_TIME_SYNC_REQ);
        s.send(Opcodes.SMSG_LFG_DISABLED, new byte[0]);
        sent.add(Opcodes.SMSG_LFG_DISABLED);
        p.setInt(UpdateFields.OBJECT_FIELD_GUID, (int) p.guid);
        return sent;
    }

    /**
     * ChatFrame.lua builds the language menu on PLAYER_ENTERING_WORLD / LANGUAGE_LIST_CHANGED.
     * Create-self skills can arrive after that event; SMSG_LEARNED_SPELL (0x12B, uint32 id) is the
     * in-world learn opcode and refreshes GetNumLanguages().
     */
    static void sendKnownLanguages(WorldSession s, Player p) {
        int[] fields = languageSkillFields(p);
        if (fields.length > 0) {
            s.send(Opcodes.SMSG_UPDATE_OBJECT, UpdateBuilder.values(p, fields));
        }
        for (int spell : ChrStatic.languageSpells(p.race)) {
            WowBuffer learned = new WowBuffer(4);
            learned.putU32(spell);
            s.send(Opcodes.SMSG_LEARNED_SPELL, learned.array());
        }
    }

    public static int[] languageSkillFields(Player p) {
        int[] tmp = new int[32];
        int n = 0;
        for (int slot = 0; slot < 127; slot++) {
            int base = UpdateFields.PLAYER_SKILL_INFO_1_1 + slot * 3;
            int id = p.getInt(base) & 0xFFFF;
            if (id != 0 && ChrStatic.isLanguageSkill(id)) {
                tmp[n++] = base;
                tmp[n++] = base + 1;
            }
        }
        return Arrays.copyOf(tmp, n);
    }

    public static void sendInventory(WorldSession s, Player p) {
        for (Item it : p.items.values()) {
            var pkt = UpdateBuilder.maybeCompress(UpdateBuilder.createItem(it, p.guid));
            s.send(pkt.opcode(), pkt.payload());
        }
    }

    public static int packedTime() {
        LocalDateTime t = LocalDateTime.now();
        int year = t.getYear() - 1900;
        int month = t.getMonthValue() - 1;
        int mday = t.getDayOfMonth();
        int wday = t.getDayOfWeek().getValue() % 7;
        return ((year - 100) << 24) | (month << 20) | ((mday - 1) << 14) | (wday << 11) | (t.getHour() << 6) | t.getMinute();
    }

    private static byte[] u32(int... v) {
        WowBuffer b = new WowBuffer(v.length * 4);
        for (int x : v) {
            b.putU32(x);
        }
        return b.array();
    }
}
