package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.PlayerNames;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

/** Channels and text emote. Layout: spec/03-protocol/packets/chat.md */
public final class ChannelHandler {
    public static final int YOU_JOINED = 0x02;
    public static final int YOU_LEFT = 0x03;
    public static final int WRONG_PASSWORD = 0x04;
    public static final int NOT_MEMBER = 0x05;
    public static final int NOT_MODERATOR = 0x06;
    public static final int PASSWORD_CHANGED = 0x07;
    public static final int OWNER_CHANGED = 0x08;
    public static final int PLAYER_NOT_FOUND = 0x09;
    public static final int NOT_OWNER = 0x0A;
    public static final int CHANNEL_OWNER = 0x0B;
    public static final int MODE_CHANGE = 0x0C;
    public static final int PLAYER_KICKED = 0x12;
    public static final int BANNED = 0x13;
    public static final int PLAYER_BANNED = 0x14;
    public static final int PLAYER_ALREADY_MEMBER = 0x17;
    public static final int INVITE = 0x18;
    public static final int INVITE_WRONG_FACTION = 0x19;
    public static final int PLAYER_INVITED = 0x1D;
    public static final int PLAYER_INVITE_BANNED = 0x1E;
    public static final int MEMBER_FLAG_NONE = 0x00;
    public static final int MEMBER_FLAG_OWNER = 0x01;
    public static final int MEMBER_FLAG_MODERATOR = 0x02;
    public static final int MEMBER_FLAG_MUTED = 0x08;
    public static final int CHANNEL_ID_GENERAL = 1;

    private ChannelHandler() {}

    public static void join(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() >= 4) {
            in.getU32();
        }
        if (in.remaining() > 0) {
            in.getU8();
        }
        if (in.remaining() > 0) {
            in.getU8();
        }
        String name = in.remaining() > 0 ? in.getCString() : "";
        String password = in.remaining() > 0 ? in.getCString() : "";
        if (name.isEmpty()) {
            return;
        }
        var bans = world.channelBans.get(name);
        if (bans != null && bans.contains(s.player().guid)) {
            notify(s, BANNED, name);
            return;
        }
        String want = world.channelPasswords.getOrDefault(name, "");
        if (!want.isEmpty() && !want.equals(password)) {
            WowBuffer n = new WowBuffer(32);
            n.putU8(WRONG_PASSWORD);
            n.putCString(name);
            s.send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
            return;
        }
        s.channels.add(name);
        if (!"General".equals(name)) {
            long guid = s.player().guid;
            var flags = world.channelMemberFlags.computeIfAbsent(name, k -> new java.util.concurrent.ConcurrentHashMap<>());
            if (world.channelOwners.putIfAbsent(name, guid) == null) {
                flags.put(guid, MEMBER_FLAG_OWNER | MEMBER_FLAG_MODERATOR);
            } else {
                flags.putIfAbsent(guid, MEMBER_FLAG_NONE);
            }
        }
        WowBuffer n = new WowBuffer(32);
        n.putU8(YOU_JOINED);
        n.putCString(name);
        n.putU8(0);
        n.putU32(CHANNEL_ID_GENERAL);
        n.putU32(0);
        s.send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
        sendList(s, name);
    }

    public static void leave(WorldSession s, WowBuffer in) {
        if (in.remaining() >= 4) {
            in.getU32();
        }
        String name = in.remaining() > 0 ? in.getCString() : "";
        if (name.isEmpty()) {
            return;
        }
        s.channels.remove(name);
        WowBuffer n = new WowBuffer(32);
        n.putU8(YOU_LEFT);
        n.putCString(name);
        n.putU32(CHANNEL_ID_GENERAL);
        n.putU8(0);
        s.send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
    }

    /** Channel::SetPassword — member sets password; PASSWORD_CHANGED to self. */
    public static void password(WorldSession s, World world, WowBuffer in) {
        String name = in.remaining() > 0 ? in.getCString() : "";
        String pass = in.remaining() > 0 ? in.getCString() : "";
        if (name.isEmpty() || !s.channels.contains(name)) {
            return;
        }
        world.channelPasswords.put(name, pass == null ? "" : pass);
        WowBuffer n = new WowBuffer(32);
        n.putU8(PASSWORD_CHANGED);
        n.putCString(name);
        n.putU64(s.player().guid);
        s.send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
    }

    /** Channel::SendChannelOwnerResponse — member gets CHANNEL_OWNER + name. */
    public static void owner(WorldSession s, World world, WowBuffer in) {
        String name = in.remaining() > 0 ? in.getCString() : "";
        if (name.isEmpty()) {
            return;
        }
        if (!s.channels.contains(name)) {
            notify(s, NOT_MEMBER, name);
            return;
        }
        Player p = s.player();
        Long ownerGuid = world.channelOwners.get(name);
        Player owner = ownerGuid != null ? world.playerByGuid(ownerGuid) : p;
        String ownerName = owner != null && owner.name != null && !owner.name.isEmpty() ? owner.name : "Nobody";
        WowBuffer n = new WowBuffer(64);
        n.putU8(CHANNEL_OWNER);
        n.putCString(name);
        n.putCString(ownerName);
        s.send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
    }

    /** Channel::SetOwner — transfer; OWNER_CHANGED when more than one member. */
    public static void setOwner(WorldSession s, World world, WowBuffer in) {
        String channel = in.remaining() > 0 ? in.getCString() : "";
        String raw = in.remaining() > 0 ? in.getCString() : "";
        String targetName = PlayerNames.normalize(raw);
        if (channel.isEmpty() || targetName == null) {
            return;
        }
        Player p = s.player();
        if (!s.channels.contains(channel)) {
            notify(s, NOT_MEMBER, channel);
            return;
        }
        Long ownerGuid = world.channelOwners.get(channel);
        if (ownerGuid == null || ownerGuid != p.guid) {
            notify(s, NOT_OWNER, channel);
            return;
        }
        Player target = world.playerByName(targetName);
        if (target == null || target.session == null || !target.session.channels.contains(channel)) {
            WowBuffer n = new WowBuffer(64);
            n.putU8(PLAYER_NOT_FOUND);
            n.putCString(channel);
            n.putCString(targetName);
            s.send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
            return;
        }
        world.channelOwners.put(channel, target.guid);
        int members = 0;
        for (Player m : world.playersOnline()) {
            if (m.session != null && m.session.channels.contains(channel)) {
                members++;
            }
        }
        if (members <= 1) {
            return;
        }
        WowBuffer n = new WowBuffer(32);
        n.putU8(OWNER_CHANGED);
        n.putCString(channel);
        n.putU64(target.guid);
        byte[] pkt = n.array();
        for (Player m : world.playersOnline()) {
            if (m.session != null && m.session.channels.contains(channel)) {
                m.session.send(Opcodes.SMSG_CHANNEL_NOTIFY, pkt);
            }
        }
    }

    private static void notify(WorldSession s, int type, String channel) {
        WowBuffer n = new WowBuffer(32);
        n.putU8(type);
        n.putCString(channel);
        s.send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
    }

    /** Channel::SetModerator — not a member → NOT_MEMBER. */
    public static void moderator(WorldSession s, World world, WowBuffer in) {
        setMode(s, world, in, MEMBER_FLAG_MODERATOR, true);
    }

    /** Channel::SetModerator(..., false) via CMSG_CHANNEL_UNMODERATOR. */
    public static void unmoderator(WorldSession s, World world, WowBuffer in) {
        setMode(s, world, in, MEMBER_FLAG_MODERATOR, false);
    }

    /** Channel::SetMute(..., true) via CMSG_CHANNEL_MUTE. */
    public static void mute(WorldSession s, World world, WowBuffer in) {
        setMode(s, world, in, MEMBER_FLAG_MUTED, true);
    }

    /** Channel::SetMute(..., false) via CMSG_CHANNEL_UNMUTE. */
    public static void unmute(WorldSession s, World world, WowBuffer in) {
        setMode(s, world, in, MEMBER_FLAG_MUTED, false);
    }

    /** Channel::Invite via CMSG_CHANNEL_INVITE. */
    public static void invite(WorldSession s, World world, WowBuffer in) {
        String channel = in.remaining() > 0 ? in.getCString() : "";
        String raw = in.remaining() > 0 ? in.getCString() : "";
        String targetName = PlayerNames.normalize(raw);
        if (channel.isEmpty() || targetName == null) {
            return;
        }
        Player p = s.player();
        if (!s.channels.contains(channel)) {
            notify(s, NOT_MEMBER, channel);
            return;
        }
        Player target = world.playerByName(targetName);
        if (target == null || target.session == null) {
            WowBuffer n = new WowBuffer(64);
            n.putU8(PLAYER_NOT_FOUND);
            n.putCString(channel);
            n.putCString(targetName);
            s.send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
            return;
        }
        if (target.session.channels.contains(channel)) {
            WowBuffer n = new WowBuffer(32);
            n.putU8(PLAYER_ALREADY_MEMBER);
            n.putCString(channel);
            n.putU64(target.guid);
            s.send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
            return;
        }
        var bans = world.channelBans.get(channel);
        if (bans != null && bans.contains(target.guid)) {
            WowBuffer n = new WowBuffer(64);
            n.putU8(PLAYER_INVITE_BANNED);
            n.putCString(channel);
            n.putCString(targetName);
            s.send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
            return;
        }
        if (target.team != p.team) {
            notify(s, INVITE_WRONG_FACTION, channel);
            return;
        }
        if (!ignores(target, p.guid)) {
            WowBuffer n = new WowBuffer(32);
            n.putU8(INVITE);
            n.putCString(channel);
            n.putU64(p.guid);
            target.session.send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
        }
        WowBuffer invited = new WowBuffer(48);
        invited.putU8(PLAYER_INVITED);
        invited.putCString(channel);
        invited.putCString(targetName);
        s.send(Opcodes.SMSG_CHANNEL_NOTIFY, invited.array());
    }

    /** Channel::Kick via CMSG_CHANNEL_KICK (KickOrBan ban=false). */
    public static void kick(WorldSession s, World world, WowBuffer in) {
        kickOrBan(s, world, in, false);
    }

    /** Channel::Ban via CMSG_CHANNEL_BAN (KickOrBan ban=true). */
    public static void ban(WorldSession s, World world, WowBuffer in) {
        kickOrBan(s, world, in, true);
    }

    private static void kickOrBan(WorldSession s, World world, WowBuffer in, boolean ban) {
        String channel = in.remaining() > 0 ? in.getCString() : "";
        String raw = in.remaining() > 0 ? in.getCString() : "";
        String targetName = PlayerNames.normalize(raw);
        if (channel.isEmpty() || targetName == null) {
            return;
        }
        Player p = s.player();
        if (!s.channels.contains(channel)) {
            notify(s, NOT_MEMBER, channel);
            return;
        }
        var flags = world.channelMemberFlags.computeIfAbsent(channel, k -> new java.util.concurrent.ConcurrentHashMap<>());
        int mine = flags.getOrDefault(p.guid, MEMBER_FLAG_NONE);
        if ((mine & MEMBER_FLAG_MODERATOR) == 0) {
            notify(s, NOT_MODERATOR, channel);
            return;
        }
        Player target = world.playerByName(targetName);
        if (target == null || target.session == null || !target.session.channels.contains(channel)) {
            WowBuffer n = new WowBuffer(64);
            n.putU8(PLAYER_NOT_FOUND);
            n.putCString(channel);
            n.putCString(targetName);
            s.send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
            return;
        }
        Long ownerGuid = world.channelOwners.get(channel);
        if (ownerGuid != null && ownerGuid == target.guid && ownerGuid != p.guid) {
            notify(s, NOT_OWNER, channel);
            return;
        }
        int type = PLAYER_KICKED;
        if (ban) {
            var bans = world.channelBans.computeIfAbsent(channel, k -> java.util.concurrent.ConcurrentHashMap.newKeySet());
            if (bans.add(target.guid)) {
                type = PLAYER_BANNED;
            }
        }
        WowBuffer n = new WowBuffer(32);
        n.putU8(type);
        n.putCString(channel);
        n.putU64(target.guid);
        n.putU64(p.guid);
        byte[] pkt = n.array();
        for (Player m : world.playersOnline()) {
            if (m.session != null && m.session.channels.contains(channel)) {
                m.session.send(Opcodes.SMSG_CHANNEL_NOTIFY, pkt);
            }
        }
        target.session.channels.remove(channel);
        flags.remove(target.guid);
    }

    private static boolean ignores(Player who, long guid) {
        for (Player.Friend f : who.friends) {
            if (f.guid == guid && (f.flags & SocialHandler.SOCIAL_FLAG_IGNORED) != 0) {
                return true;
            }
        }
        return false;
    }

    private static void setMode(WorldSession s, World world, WowBuffer in, int flag, boolean set) {
        String channel = in.remaining() > 0 ? in.getCString() : "";
        String raw = in.remaining() > 0 ? in.getCString() : "";
        String targetName = PlayerNames.normalize(raw);
        if (channel.isEmpty() || targetName == null) {
            return;
        }
        Player p = s.player();
        if (!s.channels.contains(channel)) {
            notify(s, NOT_MEMBER, channel);
            return;
        }
        var flags = world.channelMemberFlags.computeIfAbsent(channel, k -> new java.util.concurrent.ConcurrentHashMap<>());
        int mine = flags.getOrDefault(p.guid, MEMBER_FLAG_NONE);
        if ((mine & MEMBER_FLAG_MODERATOR) == 0) {
            notify(s, NOT_MODERATOR, channel);
            return;
        }
        Player target = world.playerByName(targetName);
        if (target == null || target.session == null || !target.session.channels.contains(channel)) {
            WowBuffer n = new WowBuffer(64);
            n.putU8(PLAYER_NOT_FOUND);
            n.putCString(channel);
            n.putCString(targetName);
            s.send(Opcodes.SMSG_CHANNEL_NOTIFY, n.array());
            return;
        }
        Long ownerGuid = world.channelOwners.get(channel);
        if ((flag & MEMBER_FLAG_MODERATOR) != 0 && ownerGuid != null
                && ownerGuid == p.guid && ownerGuid == target.guid) {
            return;
        }
        if (ownerGuid != null && ownerGuid == target.guid && ownerGuid != p.guid) {
            notify(s, NOT_OWNER, channel);
            return;
        }
        int oldFlag = flags.getOrDefault(target.guid, MEMBER_FLAG_NONE);
        boolean has = (oldFlag & flag) != 0;
        if (has == set) {
            return;
        }
        int newFlag = set ? (oldFlag | flag) : (oldFlag & ~flag);
        flags.put(target.guid, newFlag);
        WowBuffer n = new WowBuffer(32);
        n.putU8(MODE_CHANGE);
        n.putCString(channel);
        n.putU64(target.guid);
        n.putU8(oldFlag);
        n.putU8(newFlag);
        byte[] pkt = n.array();
        for (Player m : world.playersOnline()) {
            if (m.session != null && m.session.channels.contains(channel)) {
                m.session.send(Opcodes.SMSG_CHANNEL_NOTIFY, pkt);
            }
        }
    }

    public static void list(WorldSession s, WowBuffer in) {
        String name = in.remaining() > 0 ? in.getCString() : "";
        sendList(s, name);
    }

    public static void textEmote(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        int emote = in.remaining() >= 4 ? in.getU32() : 0;
        int num = in.remaining() >= 4 ? in.getU32() : 0;
        long target = in.remaining() >= 8 ? in.getU64() : 0;
        WowBuffer out = new WowBuffer(32);
        out.putU64(p.guid);
        out.putU32(emote);
        out.putU32(num);
        out.putU32(1);
        out.putU8(0);
        byte[] payload = out.array();
        s.send(Opcodes.SMSG_TEXT_EMOTE, payload);
        Player other = world.playerByGuid(target);
        if (other != null && other.session != null) {
            other.session.send(Opcodes.SMSG_TEXT_EMOTE, payload);
        }
    }

    private static void sendList(WorldSession s, String name) {
        Player p = s.player();
        WowBuffer list = new WowBuffer(32);
        list.putU8(0);
        list.putCString(name);
        list.putU8(0);
        list.putU32(1);
        list.putU64(p.guid);
        list.putU8(0);
        s.send(Opcodes.SMSG_CHANNEL_LIST, list.array());
    }
}
