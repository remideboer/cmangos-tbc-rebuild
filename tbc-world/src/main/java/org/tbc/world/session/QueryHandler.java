package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.tbc.world.gm.GmCommands;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** CMSG query replies from spec packet layouts. Missing templates use the high-bit fail uint32. */
public final class QueryHandler {
    private QueryHandler() {}

    public static void creature(WorldSession session, World world, WowBuffer in) {
        int entry = readU32(in);
        readU64(in);
        ObjectMgr.CreatureTemplate t = world.objectMgr.creatures.get(entry);
        if (t == null) {
            session.send(Opcodes.SMSG_CREATURE_QUERY_RESPONSE, fail(entry));
            return;
        }
        WowBuffer out = new WowBuffer(128);
        out.putU32(entry);
        out.putCString(nz(t.name()));
        out.putU8(0);
        out.putU8(0);
        out.putU8(0);
        out.putCString(nz(t.subName()));
        out.putCString(nz(t.iconName()));
        out.putU32(t.typeFlags());
        out.putU32(t.type());
        out.putU32(t.family());
        out.putU32(t.rank());
        out.putU32(0);
        out.putU32(t.petSpellDataId());
        out.putU32(t.display());
        out.putU32(t.display2());
        out.putU32(t.display3());
        out.putU32(t.display4());
        out.putFloat(t.healthMultiplier());
        out.putFloat(t.powerMultiplier());
        out.putU8(t.racialLeader());
        session.send(Opcodes.SMSG_CREATURE_QUERY_RESPONSE, out.array());
    }

    public static void gameObject(WorldSession session, World world, WowBuffer in) {
        int entry = readU32(in);
        readU64(in);
        ObjectMgr.GameObjectTemplate t = world.objectMgr.gameObjects.get(entry);
        if (t == null) {
            session.send(Opcodes.SMSG_GAMEOBJECT_QUERY_RESPONSE, fail(entry));
            return;
        }
        WowBuffer out = new WowBuffer(160);
        out.putU32(entry);
        out.putU32(t.type);
        out.putU32(t.displayId);
        out.putCString(nz(t.name));
        out.putU8(0);
        out.putU8(0);
        out.putU8(0);
        out.putCString(nz(t.iconName));
        out.putCString(nz(t.openingText));
        out.putCString(nz(t.closingText));
        for (int i = 0; i < 24; i++) {
            out.putU32(t.data[i]);
        }
        out.putFloat(t.size);
        session.send(Opcodes.SMSG_GAMEOBJECT_QUERY_RESPONSE, out.array());
    }

    public static void item(WorldSession session, World world, WowBuffer in) {
        int itemId = readU32(in);
        ObjectMgr.ItemTemplate t = world.objectMgr.items.get(itemId);
        if (t == null) {
            session.send(Opcodes.SMSG_ITEM_QUERY_SINGLE_RESPONSE, fail(itemId));
            return;
        }
        session.send(Opcodes.SMSG_ITEM_QUERY_SINGLE_RESPONSE, writeItem(t));
    }

    public static void quest(WorldSession session, World world, WowBuffer in) {
        int id = readU32(in);
        ObjectMgr.QuestTemplate t = world.objectMgr.quests.get(id);
        if (t == null) {
            session.send(Opcodes.SMSG_QUEST_QUERY_RESPONSE, fail(id));
            return;
        }
        WowBuffer out = new WowBuffer(256);
        out.putU32(id);
        out.putU32(2);
        out.putU32(t.minLevel());
        out.putU32(0);
        out.putU32(t.type());
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        for (int i = 0; i < 4; i++) {
            out.putU32(0);
            out.putU32(0);
        }
        for (int i = 0; i < 6; i++) {
            out.putU32(0);
            out.putU32(0);
        }
        out.putU32(0);
        out.putFloat(0);
        out.putFloat(0);
        out.putU32(0);
        out.putCString(nz(t.title()));
        out.putCString("");
        out.putCString("");
        out.putCString("");
        for (int i = 0; i < 4; i++) {
            out.putU32(0);
            out.putU32(0);
            out.putU32(0);
            out.putU32(0);
        }
        for (int i = 0; i < 4; i++) {
            out.putCString("");
        }
        session.send(Opcodes.SMSG_QUEST_QUERY_RESPONSE, out.array());
    }

    public static void pageText(WorldSession session, World world, WowBuffer in) {
        int pageId = readU32(in);
        ObjectMgr.PageText page = world.objectMgr.pageTexts.get(pageId);
        WowBuffer out = new WowBuffer(64);
        out.putU32(pageId);
        if (page == null) {
            out.putCString("Item page missing.");
            out.putU32(0);
        } else {
            out.putCString(nz(page.text()));
            out.putU32(page.nextPage());
        }
        session.send(Opcodes.SMSG_PAGE_TEXT_QUERY_RESPONSE, out.array());
    }

    public static void npcText(WorldSession session, World world, WowBuffer in) {
        int textId = readU32(in);
        readU64(in);
        ObjectMgr.NpcText gossip = world.objectMgr.npcTexts.get(textId);
        WowBuffer out = new WowBuffer(256);
        out.putU32(textId);
        for (int i = 0; i < Content.MAX_GOSSIP_TEXT_OPTIONS; i++) {
            if (gossip == null) {
                out.putFloat(0f);
                out.putCString(Content.DEFAULT_NPC_TEXT);
                out.putCString(Content.DEFAULT_NPC_TEXT);
                out.putU32(0);
                for (int e = 0; e < 6; e++) {
                    out.putU32(0);
                }
            } else {
                ObjectMgr.NpcTextSlot slot = gossip.slots()[i];
                String text0 = nz(slot.text0());
                String text1 = nz(slot.text1());
                out.putFloat(slot.probability());
                out.putCString(text0.isEmpty() ? text1 : text0);
                out.putCString(text1.isEmpty() ? text0 : text1);
                out.putU32(slot.language());
                int[] emotes = slot.emotes();
                for (int e = 0; e < 6; e++) {
                    out.putU32(emotes[e]);
                }
            }
        }
        session.send(Opcodes.SMSG_NPC_TEXT_UPDATE, out.array());
    }

    public static void petName(WorldSession session, WowBuffer in) {
        int petNumber = readU32(in);
        long guid = readU64(in);
        Pet pet = session.player() == null ? null : session.player().pet;
        if (pet == null || pet.guid != guid) {
            WowBuffer out = new WowBuffer(10);
            out.putU32(petNumber);
            out.putU8(0);
            out.putU32(0);
            out.putU8(0);
            session.send(Opcodes.SMSG_PET_NAME_QUERY_RESPONSE, out.array());
            return;
        }
        WowBuffer out = new WowBuffer(64);
        out.putU32(petNumber);
        out.putCString(nz(pet.name));
        out.putU32(0);
        out.putU8(0);
        session.send(Opcodes.SMSG_PET_NAME_QUERY_RESPONSE, out.array());
    }

    public static void guild(WorldSession session, WowBuffer in) {
        int guildId = readU32(in);
        WowBuffer out = new WowBuffer(32);
        out.putU32(guildId);
        out.putCString(session.player() != null ? nz(session.player().guildName) : "");
        for (int i = 0; i < 10; i++) {
            out.putU8(0);
        }
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        session.send(Opcodes.SMSG_GUILD_QUERY_RESPONSE, out.array());
    }

    public static void whois(WorldSession session, World world, WowBuffer in) {
        if (session.account() == null || session.account().gmlevel() < GmCommands.SEC_ADMINISTRATOR) {
            return;
        }
        String name = in.getCString();
        if (name == null || name.isBlank()) {
            return;
        }
        Player target = world.playerByName(name.trim());
        if (target == null || target.session == null || target.session.account() == null) {
            return;
        }
        String acc = nz(target.session.account().username());
        if (acc.isEmpty()) {
            acc = "Unknown";
        }
        String email = "Unknown";
        String ip = "Unknown";
        if (world.login != null) {
            String[] extra = lookupAccountContact(world, target.session.account().id());
            if (extra[0] != null && !extra[0].isEmpty()) {
                acc = extra[0];
            }
            if (extra[1] != null && !extra[1].isEmpty()) {
                email = extra[1];
            }
            if (extra[2] != null && !extra[2].isEmpty()) {
                ip = extra[2];
            }
        }
        String msg = name.trim() + "'s account is " + acc + ", e-mail: " + email + ", last ip: " + ip;
        WowBuffer out = new WowBuffer(msg.length() + 1);
        out.putCString(msg);
        session.send(Opcodes.SMSG_WHOIS, out.array());
    }

    private static String[] lookupAccountContact(World world, int accountId) {
        String username = "";
        String email = "";
        String ip = "";
        try (Connection c = world.login.get();
             PreparedStatement ps = c.prepareStatement("SELECT username, email FROM account WHERE id = ?")) {
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                username = rs.getString(1) == null ? "" : rs.getString(1);
                email = rs.getString(2) == null ? "" : rs.getString(2);
            }
        } catch (Exception ignored) {
        }
        try (Connection c = world.login.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT ip FROM account_logons WHERE accountId = ? ORDER BY loginTime DESC LIMIT 1")) {
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getString(1) != null) {
                ip = rs.getString(1);
            }
        } catch (Exception ignored) {
        }
        return new String[]{username, email, ip};
    }

    private static byte[] writeItem(ObjectMgr.ItemTemplate t) {
        WowBuffer out = new WowBuffer(700);
        out.putU32(t.entry);
        out.putU32(t.itemClass);
        out.putU32(t.subClass);
        out.putU32(t.unk);
        out.putCString(nz(t.name));
        out.putU8(0);
        out.putU8(0);
        out.putU8(0);
        out.putU32(t.displayId);
        out.putU32(t.quality);
        out.putU32(t.flags);
        out.putU32(t.buyPrice);
        out.putU32(t.sellPrice);
        out.putU32(t.inventoryType);
        out.putU32(t.allowableClass);
        out.putU32(t.allowableRace);
        out.putU32(t.itemLevel);
        out.putU32(t.requiredLevel);
        out.putU32(t.requiredSkill);
        out.putU32(t.requiredSkillRank);
        out.putU32(t.requiredSpell);
        out.putU32(t.requiredHonorRank);
        out.putU32(t.requiredCityRank);
        out.putU32(t.requiredReputationFaction);
        out.putU32(t.requiredReputationRank);
        out.putU32(t.maxCount);
        out.putU32(t.stackable);
        out.putU32(t.containerSlots);
        for (int i = 0; i < 10; i++) {
            out.putU32(t.statType[i]);
            out.putU32(t.statValue[i]);
        }
        for (int i = 0; i < 5; i++) {
            out.putFloat(t.dmgMin[i]);
            out.putFloat(t.dmgMax[i]);
            out.putU32(t.dmgType[i]);
        }
        out.putU32(t.armor);
        out.putU32(t.holyRes);
        out.putU32(t.fireRes);
        out.putU32(t.natureRes);
        out.putU32(t.frostRes);
        out.putU32(t.shadowRes);
        out.putU32(t.arcaneRes);
        out.putU32(t.delay);
        out.putU32(t.ammoType);
        out.putFloat(t.rangedModRange);
        for (int i = 0; i < 5; i++) {
            out.putU32(0);
            out.putU32(0);
            out.putU32(0);
            out.putU32(-1);
            out.putU32(0);
            out.putU32(-1);
        }
        out.putU32(t.bonding);
        out.putCString(nz(t.description));
        out.putU32(t.pageText);
        out.putU32(t.languageId);
        out.putU32(t.pageMaterial);
        out.putU32(t.startQuest);
        out.putU32(t.lockId);
        out.putU32(t.material);
        out.putU32(t.sheath);
        out.putU32(t.randomProperty);
        out.putU32(t.randomSuffix);
        out.putU32(t.block);
        out.putU32(t.itemSet);
        out.putU32(t.maxDurability);
        out.putU32(t.area);
        out.putU32(t.map);
        out.putU32(t.bagFamily);
        out.putU32(t.totemCategory);
        for (int i = 0; i < 3; i++) {
            out.putU32(t.socketColor[i]);
            out.putU32(t.socketContent[i]);
        }
        out.putU32(t.socketBonus);
        out.putU32(t.gemProperties);
        out.putU32(t.requiredDisenchantSkill);
        out.putFloat(t.armorDamageModifier);
        out.putU32(t.duration);
        return out.array();
    }

    private static byte[] fail(int entry) {
        WowBuffer b = new WowBuffer(4);
        b.putU32(entry | 0x80000000);
        return b.array();
    }

    private static int readU32(WowBuffer in) {
        return in.remaining() >= 4 ? in.getU32() : 0;
    }

    private static long readU64(WowBuffer in) {
        return in.remaining() >= 8 ? in.getU64() : 0;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
