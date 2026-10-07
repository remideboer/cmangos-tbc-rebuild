package org.tbc.world.session;

import org.tbc.common.Codes;
import org.tbc.common.WowBuffer;
import org.tbc.world.content.ChrStatic;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.PlayerNames;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import java.util.List;

/** Character screen (CMaNGOS CharacterHandler.cpp): enum, create, delete, rename, declined names, login. */
public final class CharacterHandler {
    private CharacterHandler() {}

    /** STATUS_AUTHED opcodes. */
    public static void register(OpcodeTable t) {
        t.register(Opcodes.CMSG_CHAR_ENUM, (s, w, in) -> charEnum(s, w))
                .register(Opcodes.CMSG_CHAR_CREATE, CharacterHandler::charCreate)
                .register(Opcodes.CMSG_CHAR_DELETE, CharacterHandler::charDelete)
                .register(Opcodes.CMSG_CHAR_RENAME, CharacterHandler::charRename)
                .register(Opcodes.CMSG_SET_PLAYER_DECLINED_NAMES, CharacterHandler::setPlayerDeclinedNames)
                .register(Opcodes.CMSG_PLAYER_LOGIN, CharacterHandler::login);
    }

    public static void charEnum(WorldSession s, World world) {
        List<Player> list = world.characters.enumAccount(s.account().id(), world.objectMgr);
        WowBuffer out = new WowBuffer(64);
        out.putU8(list.size());
        for (Player p : list) {
            out.putU64(p.guid);
            out.putCString(p.name);
            out.putU8(p.race);
            out.putU8(p.clazz);
            out.putU8(p.gender);
            out.putU8(p.skin);
            out.putU8(p.face);
            out.putU8(p.hairStyle);
            out.putU8(p.hairColor);
            out.putU8(p.facialHair);
            out.putU8(p.level);
            out.putU32(p.zoneId);
            out.putU32(p.mapId);
            out.putFloat(p.x);
            out.putFloat(p.y);
            out.putFloat(p.z);
            out.putU32(p.guildId);
            int flags = 0;
            if (p.ghost) {
                flags |= 0x2000;
            }
            if ((p.atLogin & Player.AT_LOGIN_RENAME) != 0) {
                flags |= 0x4000;
            }
            out.putU32(flags);
            out.putU8((p.atLogin & Player.AT_LOGIN_FIRST) != 0 ? 1 : 0);
            out.putU32(0);
            out.putU32(0);
            out.putU32(0);
            for (int i = 0; i < 20; i++) {
                Item it = p.itemAt(0, i);
                if (it == null) {
                    out.putU32(0);
                    out.putU8(0);
                    out.putU32(0);
                } else {
                    out.putU32(it.displayId);
                    out.putU8(it.inventoryType);
                    out.putU32(it.enchant);
                }
            }
        }
        s.send(Opcodes.SMSG_CHAR_ENUM, out.array());
    }

    public static void charCreate(WorldSession s, World world, WowBuffer in) {
        String name = in.getCString();
        int race = in.getU8();
        int clazz = in.getU8();
        int gender = in.getU8();
        int skin = in.getU8();
        int face = in.getU8();
        int hair = in.getU8();
        int hairColor = in.getU8();
        int facial = in.getU8();
        if (in.remaining() > 0) {
            in.getU8();
        }
        if (!ChrStatic.playable(race, clazz)) {
            s.send(Opcodes.SMSG_CHAR_CREATE, new byte[]{(byte) Codes.CHAR_CREATE_ERROR});
            return;
        }
        if (world.characters.nameInUse(name)) {
            s.send(Opcodes.SMSG_CHAR_CREATE, new byte[]{(byte) Codes.CHAR_CREATE_NAME_IN_USE});
            return;
        }
        var r = ChrStatic.race(race);
        if (r.expansion() > s.account().expansion()) {
            s.send(Opcodes.SMSG_CHAR_CREATE, new byte[]{(byte) Codes.CHAR_CREATE_EXPANSION});
            return;
        }
        Player p = world.characters.create(s.account().id(), name, race, clazz, gender, skin, face, hair, hairColor,
                facial, world.objectMgr);
        if (p == null) {
            s.send(Opcodes.SMSG_CHAR_CREATE, new byte[]{(byte) Codes.CHAR_CREATE_ERROR});
            return;
        }
        s.send(Opcodes.SMSG_CHAR_CREATE, new byte[]{(byte) Codes.CHAR_CREATE_SUCCESS});
    }

    public static void charDelete(WorldSession s, World world, WowBuffer in) {
        long guid = in.getU64();
        boolean ok = world.characters.delete(s.account().id(), guid);
        s.send(Opcodes.SMSG_CHAR_DELETE,
                new byte[]{(byte) (ok ? Codes.CHAR_DELETE_SUCCESS : Codes.CHAR_DELETE_FAILED_GUILD_LEADER)});
    }

    /** HandleCharRenameOpcode — STATUS_AUTHED, fail is uint8 only. */
    public static void charRename(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 8) {
            s.send(Opcodes.SMSG_CHAR_RENAME, new byte[]{(byte) Codes.CHAR_NAME_NO_NAME});
            return;
        }
        long guid = in.getU64();
        String newname = in.getCString();
        String normalized = PlayerNames.normalize(newname);
        if (normalized == null) {
            s.send(Opcodes.SMSG_CHAR_RENAME, new byte[]{(byte) Codes.CHAR_NAME_NO_NAME});
            return;
        }
        int res = PlayerNames.check(normalized);
        if (res != Codes.CHAR_NAME_SUCCESS) {
            s.send(Opcodes.SMSG_CHAR_RENAME, new byte[]{(byte) res});
            return;
        }
        if (!world.characters.renameAtLogin(s.account().id(), guid, normalized)) {
            s.send(Opcodes.SMSG_CHAR_RENAME, new byte[]{(byte) Codes.CHAR_CREATE_ERROR});
            return;
        }
        WowBuffer out = new WowBuffer(32);
        out.putU8(Codes.RESPONSE_SUCCESS);
        out.putU64(guid);
        out.putCString(normalized);
        s.send(Opcodes.SMSG_CHAR_RENAME, out.array());
    }

    /** HandleSetPlayerDeclinedNamesOpcode — Cyrillic persist, else result 1. */
    public static void setPlayerDeclinedNames(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 8) {
            sendDeclinedNamesResult(s, 1, 0);
            return;
        }
        long guid = in.getU64();
        String name = world.characters.nameByGuid(guid);
        if (name == null || !PlayerNames.cyrillicFirst(name)) {
            sendDeclinedNamesResult(s, 1, guid);
            return;
        }
        String name2 = in.remaining() > 0 ? in.getCString() : "";
        if (!name.equals(name2)) {
            sendDeclinedNamesResult(s, 1, guid);
            return;
        }
        String[] cases = new String[PlayerNames.MAX_DECLINED_NAME_CASES];
        for (int i = 0; i < cases.length; i++) {
            String raw = in.remaining() > 0 ? in.getCString() : "";
            String n = PlayerNames.normalize(raw);
            if (n == null) {
                sendDeclinedNamesResult(s, 1, guid);
                return;
            }
            cases[i] = n;
        }
        if (!PlayerNames.checkDeclinedNames(name, cases)) {
            sendDeclinedNamesResult(s, 1, guid);
            return;
        }
        world.characters.setDeclinedNames(guid, cases);
        sendDeclinedNamesResult(s, 0, guid);
    }

    private static void sendDeclinedNamesResult(WorldSession s, int result, long guid) {
        WowBuffer out = new WowBuffer(12);
        out.putU32(result);
        out.putU64(guid);
        s.send(Opcodes.SMSG_SET_PLAYER_DECLINED_NAMES_RESULT, out.array());
    }

    /** HandlePlayerLoginOpcode — load, refuse unknown (0x05) or already online elsewhere (0x02), then enter world. */
    public static void login(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 8) {
            return;
        }
        long guid = in.getU64();
        Player p = world.characters.load(s.account().id(), guid, world.objectMgr);
        if (p == null) {
            s.send(Opcodes.SMSG_CHARACTER_LOGIN_FAILED, new byte[]{0x05});
            return;
        }
        if (p.online && p.session != null && p.session != s) {
            s.send(Opcodes.SMSG_CHARACTER_LOGIN_FAILED, new byte[]{0x02});
            return;
        }
        s.enterWorld(world, p);
    }
}
