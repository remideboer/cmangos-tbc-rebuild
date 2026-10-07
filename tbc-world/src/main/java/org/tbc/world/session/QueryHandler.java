package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.content.ObjectMgr;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guild;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.gm.GmCommands;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.world.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** CMSG query replies from spec packet layouts. Missing templates use the high-bit fail uint32. */
public final class QueryHandler {
    private QueryHandler() {}

    /** Logged-in query opcodes. CMSG_GUILD_QUERY stays in WorldSession (STATUS_AUTHED). */
    public static void register(OpcodeTable t) {
        t.register(Opcodes.CMSG_CREATURE_QUERY, QueryHandler::creature)
                .register(Opcodes.CMSG_GAMEOBJECT_QUERY, QueryHandler::gameObject)
                .register(Opcodes.CMSG_ITEM_QUERY_SINGLE, QueryHandler::item)
                .register(Opcodes.CMSG_QUEST_QUERY, QueryHandler::quest)
                .register(Opcodes.CMSG_PAGE_TEXT_QUERY, QueryHandler::pageText)
                .register(Opcodes.CMSG_ITEM_TEXT_QUERY, QueryHandler::itemText)
                .register(Opcodes.CMSG_NPC_TEXT_QUERY, QueryHandler::npcText)
                .register(Opcodes.CMSG_PET_NAME_QUERY, QueryHandler::petName)
                .register(Opcodes.CMSG_WHOIS, QueryHandler::whois);
    }

    public static void creature(WorldSession session, World world, WowBuffer in) {
        int entry = readU32(in);
        long guid = readU64(in);
        ObjectMgr.CreatureTemplate t = world.objectMgr.creature(entry);
        // HandleCreatureQueryOpcode answers from creature_template. Only player-controlled
        // companion bodies (synthetic entry, no template) use the Companion SubName reply —
        // never a normal map NPC that happens to share a missing-template guid.
        Creature companion = t != null ? null : companionBody(session.player(), world, guid, entry);
        if (t == null && companion == null) {
            session.send(Opcodes.SMSG_CREATURE_QUERY_RESPONSE, fail(entry));
            return;
        }
        WowBuffer out = new WowBuffer(128);
        out.putU32(entry);
        if (companion != null) {
            out.putCString(nz(companion.name));
            out.putU8(0);
            out.putU8(0);
            out.putU8(0);
            out.putCString("Companion");
            out.putCString("");
            out.putU32(0);
            out.putU32(7);
            out.putU32(0);
            out.putU32(0);
            out.putU32(0);
            out.putU32(0);
            out.putU32(companion.getInt(UpdateFields.UNIT_FIELD_DISPLAYID));
            out.putU32(0);
            out.putU32(0);
            out.putU32(0);
            out.putFloat(1f);
            out.putFloat(1f);
            out.putU8(0);
        } else {
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
        }
        session.send(Opcodes.SMSG_CREATURE_QUERY_RESPONSE, out.array());
    }

    public static void gameObject(WorldSession session, World world, WowBuffer in) {
        int entry = readU32(in);
        readU64(in);
        ObjectMgr.GameObjectTemplate t = world.objectMgr.gameObject(entry);
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
        ObjectMgr.ItemTemplate t = world.objectMgr.item(itemId);
        if (t == null) {
            session.send(Opcodes.SMSG_ITEM_QUERY_SINGLE_RESPONSE, fail(itemId));
            return;
        }
        session.send(Opcodes.SMSG_ITEM_QUERY_SINGLE_RESPONSE, encodeItemQuery(t));
    }

    /** Login/inventory refresh — push proto so client SoR $MW/$mw match item_template. */
    public static void sendItemQuery(WorldSession session, ObjectMgr.ItemTemplate t) {
        if (session == null || t == null) {
            return;
        }
        session.send(Opcodes.SMSG_ITEM_QUERY_SINGLE_RESPONSE, encodeItemQuery(t));
    }

    /** Wire bytes for {@link Opcodes#SMSG_ITEM_QUERY_SINGLE_RESPONSE} (queries.md). */
    public static byte[] encodeItemQuery(ObjectMgr.ItemTemplate t) {
        return writeItem(t);
    }

    public static void quest(WorldSession session, World world, WowBuffer in) {
        int id = readU32(in);
        ObjectMgr.QuestTemplate t = world.objectMgr.quest(id);
        if (t == null) {
            session.send(Opcodes.SMSG_QUEST_QUERY_RESPONSE, fail(id));
            return;
        }
        WowBuffer out = new WowBuffer(256);
        out.putU32(id);
        out.putU32(2);
        out.putU32(t.questLevel() != 0 ? t.questLevel() : t.minLevel());
        out.putU32(t.zoneOrSort());
        out.putU32(t.type());
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(t.rewMoney());
        out.putU32(t.rewMoneyMaxLevel());
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(0);
        out.putU32(t.rewItemId1());
        out.putU32(t.rewItemCount1());
        for (int i = 0; i < 3; i++) {
            out.putU32(0);
            out.putU32(0);
        }
        out.putU32(t.rewChoiceItemId1());
        out.putU32(t.rewChoiceItemCount1());
        out.putU32(t.rewChoiceItemId2());
        out.putU32(t.rewChoiceItemCount2());
        for (int i = 0; i < 4; i++) {
            out.putU32(0);
            out.putU32(0);
        }
        out.putU32(0);
        out.putFloat(0);
        out.putFloat(0);
        out.putU32(0);
        out.putCString(nz(t.title()));
        out.putCString(nz(t.objectives()));
        out.putCString(nz(t.details()));
        out.putCString("");
        for (int i = 0; i < 4; i++) {
            int creature = t.reqCreatureOrGOId(i);
            if (creature < 0) {
                creature = (-creature) | 0x80000000;
            }
            out.putU32(creature);
            out.putU32(t.reqCreatureOrGOCount(i));
            out.putU32(t.reqItemId(i));
            out.putU32(t.reqItemCount(i));
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

    /** CMaNGOS MailHandler HandleItemTextQuery. Layout: spec/03-protocol/packets/misc-player.md */
    public static void itemText(WorldSession session, World world, WowBuffer in) {
        int itemTextId = readU32(in);
        readU32(in);
        readU32(in);
        WowBuffer out = new WowBuffer(64);
        out.putU32(itemTextId);
        out.putCString(world.objectMgr.itemText(itemTextId));
        session.send(Opcodes.SMSG_ITEM_TEXT_QUERY_RESPONSE, out.array());
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

    public static void petName(WorldSession session, World world, WowBuffer in) {
        int petNumber = readU32(in);
        long guid = readU64(in);
        Player player = session.player();
        Creature body = mapCreature(player, world, guid);
        int bodyPetNumber = body == null ? 0 : body.getInt(UpdateFields.UNIT_FIELD_PETNUMBER);
        Pet pet = player == null ? null : player.pet;
        boolean ownerPetMatches = pet != null && pet.guid == guid;
        boolean bodyMatches = !ownerPetMatches && body != null
                && (bodyPetNumber == 0 || bodyPetNumber == petNumber);
        if (!bodyMatches && !ownerPetMatches) {
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
        out.putCString(bodyMatches ? nz(body.name) : nz(pet.name));
        out.putU32(bodyMatches
                ? body.getInt(UpdateFields.UNIT_FIELD_PET_NAME_TIMESTAMP)
                : pet.nameTimestamp);
        out.putU8(0);
        session.send(Opcodes.SMSG_PET_NAME_QUERY_RESPONSE, out.array());
    }

    private static Creature mapCreature(Player player, World world, long guid) {
        if (player == null) {
            return null;
        }
        return world.map(player.mapId, player.instanceId).creatures.get(guid);
    }

    /** Synthetic offline-character companion only — not every map creature without a template. */
    private static Creature companionBody(Player player, World world, long guid, int entry) {
        Creature body = mapCreature(player, world, guid);
        if (body == null || body.entry != entry || !body.playerControlledPet) {
            return null;
        }
        return body;
    }

    public static void guild(WorldSession session, World world, WowBuffer in) {
        int guildId = readU32(in);
        Guild g = world.objectMgr.guilds.get(guildId);
        Player p = session.player();
        String name = g != null ? g.name : (p != null ? nz(p.guildName) : "");
        WowBuffer out = new WowBuffer(256);
        out.putU32(guildId);
        out.putCString(name);
        for (int i = 0; i < 10; i++) {
            if (g != null && i < g.ranks.size()) {
                out.putCString(g.ranks.get(i).name);
            } else {
                out.putU8(0);
            }
        }
        out.putU32(g != null ? g.emblemStyle : 0);
        out.putU32(g != null ? g.emblemColor : 0);
        out.putU32(g != null ? g.borderStyle : 0);
        out.putU32(g != null ? g.borderColor : 0);
        out.putU32(g != null ? g.backgroundColor : 0);
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
        out.putU32(0xFFFF_FFFF); // CMaNGOS ItemHandler: uint32(-1) unk 2.0.3
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
        // ItemHandler.cpp — 5 × (spellId, trigger, charges, cooldown, category, categoryCooldown).
        // Empty slots stay 0/−1; non-empty must carry ON_USE ids or login item-query wipes client use.
        for (int i = 0; i < 5; i++) {
            int spellId = t.spellId[i];
            if (spellId != 0) {
                out.putU32(spellId);
                out.putU32(t.spellTrigger[i]);
                out.putU32(t.spellCharges[i]);
                out.putU32(-1);
                out.putU32(0);
                out.putU32(-1);
            } else {
                out.putU32(0);
                out.putU32(0);
                out.putU32(0);
                out.putU32(-1);
                out.putU32(0);
                out.putU32(-1);
            }
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
