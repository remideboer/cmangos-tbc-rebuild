package org.tbc.world.world;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat channel state (CMaNGOS Channel.cpp / ChannelMgr): membership, owner, member flags,
 * password, bans, announcement and moderation toggles. "General" is public — no owner.
 */
public final class ChannelRegistry {
    public static final int MEMBER_FLAG_NONE = 0x00;
    public static final int MEMBER_FLAG_OWNER = 0x01;
    public static final int MEMBER_FLAG_MODERATOR = 0x02;
    public static final int MEMBER_FLAG_MUTED = 0x08;

    private static final String GENERAL = "General";

    private final Map<String, Set<Long>> members = new ConcurrentHashMap<>();
    private final Map<String, String> passwords = new ConcurrentHashMap<>();
    private final Map<String, Long> owners = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentHashMap<Long, Integer>> memberFlags = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> bans = new ConcurrentHashMap<>();
    private final Map<String, Boolean> announcements = new ConcurrentHashMap<>();
    private final Map<String, Boolean> moderation = new ConcurrentHashMap<>();

    public boolean isMember(String channel, long guid) {
        Set<Long> m = members.get(channel);
        return m != null && m.contains(guid);
    }

    /** Channel::Join — first joiner of a custom channel becomes owner + moderator. */
    public void join(String channel, long guid) {
        members.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(guid);
        if (GENERAL.equals(channel)) {
            return;
        }
        ConcurrentHashMap<Long, Integer> flags = flagsOf(channel);
        if (owners.putIfAbsent(channel, guid) == null) {
            flags.put(guid, MEMBER_FLAG_OWNER | MEMBER_FLAG_MODERATOR);
        } else {
            flags.putIfAbsent(guid, MEMBER_FLAG_NONE);
        }
    }

    /** Channel::Leave — membership only; flags and owner are untouched. */
    public void leave(String channel, long guid) {
        Set<Long> m = members.get(channel);
        if (m != null) {
            m.remove(guid);
        }
    }

    /** Player logout — Player::CleanupChannels. */
    public void leaveAll(long guid) {
        for (Set<Long> m : members.values()) {
            m.remove(guid);
        }
    }

    /** Channel::KickOrBan — membership and flags both go. */
    public void removeMember(String channel, long guid) {
        leave(channel, guid);
        flagsOf(channel).remove(guid);
    }

    public Long owner(String channel) {
        return owners.get(channel);
    }

    public void setOwner(String channel, long guid) {
        owners.put(channel, guid);
    }

    public int flags(String channel, long guid) {
        ConcurrentHashMap<Long, Integer> flags = memberFlags.get(channel);
        return flags == null ? MEMBER_FLAG_NONE : flags.getOrDefault(guid, MEMBER_FLAG_NONE);
    }

    public void setFlags(String channel, long guid, int flags) {
        flagsOf(channel).put(guid, flags);
    }

    public String password(String channel) {
        return passwords.getOrDefault(channel, "");
    }

    public void setPassword(String channel, String password) {
        passwords.put(channel, password == null ? "" : password);
    }

    public boolean isBanned(String channel, long guid) {
        Set<Long> b = bans.get(channel);
        return b != null && b.contains(guid);
    }

    /** @return true when the guid was newly banned. */
    public boolean ban(String channel, long guid) {
        return bans.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(guid);
    }

    /** @return true when the guid was banned and is now unbanned. */
    public boolean unban(String channel, long guid) {
        Set<Long> b = bans.get(channel);
        return b != null && b.remove(guid);
    }

    /** Channel::ToggleAnnouncements — custom channels default on, General off. @return new state. */
    public boolean toggleAnnouncements(String channel) {
        boolean on = !announcements.getOrDefault(channel, !GENERAL.equals(channel));
        announcements.put(channel, on);
        return on;
    }

    /** Channel::ToggleModeration — default off. @return new state. */
    public boolean toggleModeration(String channel) {
        boolean on = !moderation.getOrDefault(channel, false);
        moderation.put(channel, on);
        return on;
    }

    private ConcurrentHashMap<Long, Integer> flagsOf(String channel) {
        return memberFlags.computeIfAbsent(channel, k -> new ConcurrentHashMap<>());
    }
}
