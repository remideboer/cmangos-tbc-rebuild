package org.tbc.bdd;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.session.SocialHandler;
import org.tbc.world.world.World;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SocialSteps {
    private static final World.Account ACC_A =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final World.Account ACC_B =
            new World.Account(2, "OTHER", new byte[40], 3, 1, "Win", "x86");

    private World world;
    private WowClientDouble alpha;
    private WowClientDouble bravo;
    private long tradedGuid;
    private int bagSlot;
    private int mailId;
    private int mailedLow;

    @Given("two logged-in characters in range named Alpha and Bravo")
    public void twoInRange() {
        world = World.inMemory();
        alpha = new WowClientDouble();
        bravo = new WowClientDouble();
        alpha.connect(ACC_A);
        bravo.connect(ACC_B);
        Player a = world.characters.create(ACC_A.id(), "Alpha", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        Player b = world.characters.create(ACC_B.id(), "Bravo", 1, 1, 0, 1, 1, 1, 1, 0, world.objectMgr);
        alpha.login(world, a.guid);
        bravo.login(world, b.guid);
        Player pa = alpha.session().player();
        bravo.session().player().relocate(pa.x, pa.y, pa.z, pa.o);
        world.map(pa.mapId, pa.instanceId).add(bravo.session().player());
    }

    @When("Alpha invites Bravo to the party")
    public void inviteBravo() {
        bravo.clear();
        alpha.clear();
        alpha.groupInvite(world, "Bravo");
    }

    @When("Alpha invites Nobody to the party")
    public void inviteNobody() {
        alpha.clear();
        alpha.groupInvite(world, "Nobody");
    }

    @Then("Bravo received SMSG_GROUP_INVITE")
    public void bravoInvite() {
        assertTrue(bravo.saw(Opcodes.SMSG_GROUP_INVITE));
        byte[] p = bravo.payload(Opcodes.SMSG_GROUP_INVITE);
        assertEquals("Alpha", new String(p, 0, p.length - 1));
    }

    @When("Bravo accepts the party invite")
    public void acceptInvite() {
        alpha.clear();
        bravo.clear();
        bravo.groupAccept(world);
    }

    @Then("both received SMSG_GROUP_LIST omitting themselves")
    public void groupList() {
        assertTrue(alpha.saw(Opcodes.SMSG_GROUP_LIST));
        assertTrue(bravo.saw(Opcodes.SMSG_GROUP_LIST));
        assertEquals("Bravo", otherName(alpha.payload(Opcodes.SMSG_GROUP_LIST)));
        assertEquals("Alpha", otherName(bravo.payload(Opcodes.SMSG_GROUP_LIST)));
        long g = WowClientDouble.u64le(alpha.payload(Opcodes.SMSG_GROUP_LIST), 4);
        assertEquals(0x1F50, (int) (g >>> 48));
    }

    @When("Alpha sends party chat hello")
    public void partyChat() {
        bravo.clear();
        alpha.partyChat(world, "hello");
    }

    @Then("Bravo received CHAT_MSG_PARTY")
    public void partyChatSeen() {
        byte[] p = bravo.payload(Opcodes.SMSG_MESSAGECHAT);
        assertEquals(0x02, p[0] & 0xFF);
    }

    @Given("Alpha has item {int} in a bag slot")
    public void alphaHasItem(int entry) {
        Player p = alpha.session().player();
        bagSlot = p.firstFreeBagSlot();
        assertTrue(bagSlot >= 0);
        Item it = new Item(world.nextItemGuid(), entry);
        it.ownerGuid = Guid.low(p.guid);
        it.bag = 0;
        it.slot = bagSlot;
        it.count = 1;
        p.items.put(Guid.low(it.guid), it);
        tradedGuid = it.guid;
        mailedLow = Guid.low(it.guid);
        p.setMoney(Math.max(p.money, 100));
    }

    @Given("Alpha has {int} copper")
    public void alphaCopper(int copper) {
        alpha.session().player().setMoney(copper);
    }

    @When("they trade that item and both accept")
    public void tradeItem() {
        Player a = alpha.session().player();
        Player b = bravo.session().player();
        b.relocate(a.x, a.y, a.z, a.o);
        bravo.clear();
        alpha.clear();
        alpha.initiateTrade(world, b.guid);
        bravo.beginTrade(world);
        alpha.setTradeItem(world, 0, 0, bagSlot);
        alpha.acceptTrade(world);
        bravo.acceptTrade(world);
    }

    @Then("Bravo bags contain item {int}")
    public void bravoHas(int entry) {
        assertTrue(bravo.session().player().items.values().stream().anyMatch(i -> i.entry == entry));
    }

    @Then("Alpha no longer has that item guid")
    public void alphaLost() {
        assertFalse(alpha.session().player().items.containsKey(Guid.low(tradedGuid)));
    }

    @When("Alpha mails that item to Bravo")
    public void mailItem() {
        alpha.clear();
        bravo.clear();
        alpha.sendMail(world, 1, "Bravo", "sword", "here", tradedGuid);
        byte[] r = alpha.payload(Opcodes.SMSG_SEND_MAIL_RESULT);
        assertEquals(0, WowClientDouble.u32le(r, 4));
        assertEquals(0, WowClientDouble.u32le(r, 8));
        mailId = WowClientDouble.u32le(r, 0);
        assertTrue(mailId > 0);
    }

    @When("Bravo takes the mailed item using uint32 guid-low")
    public void takeMail() {
        bravo.clear();
        bravo.takeMailItem(world, 1, mailId, mailedLow);
        byte[] r = bravo.payload(Opcodes.SMSG_SEND_MAIL_RESULT);
        assertEquals(SocialHandler.MAIL_ITEM_TAKEN, WowClientDouble.u32le(r, 4));
        assertEquals(0, WowClientDouble.u32le(r, 8));
        assertEquals(mailedLow, WowClientDouble.u32le(r, 12));
    }

    @When("Alpha queries who")
    public void queryWho() {
        alpha.clear();
        alpha.who(world);
    }

    @Then("SMSG_WHO lists Bravo and displayCount is at most {int}")
    public void whoLists(int cap) {
        byte[] p = alpha.payload(Opcodes.SMSG_WHO);
        int display = WowClientDouble.u32le(p, 0);
        assertTrue(display <= cap);
        assertTrue(display >= 2);
        assertTrue(whoNames(p).contains("Bravo"));
    }

    @Given("{int} extra in-world players of the same faction")
    public void extraPlayers(int n) {
        Player a = alpha.session().player();
        for (int i = 0; i < n; i++) {
            Player e = new Player();
            e.guid = 10_000L + i;
            e.name = "Extra" + i;
            e.level = 1;
            e.race = 1;
            e.clazz = 1;
            e.gender = 0;
            e.zoneId = a.zoneId;
            e.team = a.team;
            e.relocate(a.x, a.y, a.z, a.o);
            world.map(a.mapId, a.instanceId).add(e);
        }
    }

    @Then("SMSG_WHO displayCount is {int} and matchCount is {int}")
    public void whoCap(int display, int match) {
        byte[] p = alpha.payload(Opcodes.SMSG_WHO);
        assertEquals(display, WowClientDouble.u32le(p, 0));
        assertEquals(match, WowClientDouble.u32le(p, 4));
    }

    @When("Alpha adds Bravo as a friend")
    public void addBravo() {
        alpha.clear();
        alpha.addFriend(world, "Bravo");
        assertTrue(alpha.saw(Opcodes.SMSG_FRIEND_STATUS));
    }

    @When("Alpha removes Bravo as a friend")
    public void delBravo() {
        alpha.clear();
        alpha.delFriend(world, bravo.session().player().guid);
    }

    @When("Alpha adds Alpha as a friend")
    public void addSelf() {
        alpha.clear();
        alpha.addFriend(world, "Alpha");
    }

    @When("both characters log out and relog")
    public void relogBoth() {
        long ag = alpha.session().player().guid;
        long bg = bravo.session().player().guid;
        alpha.logout(world);
        bravo.logout(world);
        alpha = new WowClientDouble();
        bravo = new WowClientDouble();
        alpha.connect(ACC_A);
        bravo.connect(ACC_B);
        alpha.login(world, ag);
        bravo.login(world, bg);
    }

    @Given("Alpha created a guild named {string}")
    public void alphaCreatedGuild(String name) {
        alpha.clear();
        alpha.guildCreate(world, name);
        assertTrue(alpha.saw(Opcodes.SMSG_GUILD_ROSTER));
    }

    @When("Alpha invites Bravo to the guild")
    public void guildInviteBravo() {
        bravo.clear();
        alpha.clear();
        alpha.guildInvite(world, "Bravo");
    }

    @Then("Bravo received SMSG_GUILD_INVITE from Alpha for {string}")
    public void bravoGuildInvite(String guild) {
        assertTrue(bravo.saw(Opcodes.SMSG_GUILD_INVITE));
        WowBuffer b = new WowBuffer(bravo.payload(Opcodes.SMSG_GUILD_INVITE));
        assertEquals("Alpha", b.getCString());
        assertEquals(guild, b.getCString());
    }

    @When("Bravo accepts the guild invite")
    public void bravoAcceptGuild() {
        alpha.clear();
        bravo.clear();
        bravo.guildAccept(world);
    }

    @Then("both received SMSG_GUILD_EVENT joined Bravo")
    public void guildJoinedEvent() {
        assertJoined(alpha.payload(Opcodes.SMSG_GUILD_EVENT));
        assertJoined(bravo.payload(Opcodes.SMSG_GUILD_EVENT));
    }

    @When("Alpha lists that item at Chilton for {int} hours starting at {int} copper")
    public void alphaSellAuction(int hours, int bid) {
        Player p = alpha.session().player();
        org.tbc.world.entity.Creature ah = null;
        for (org.tbc.world.entity.Creature c : world.map(p.mapId, p.instanceId).creatures.values()) {
            if (c.entry == org.tbc.world.content.Content.NPC_AUCTIONEER_CHILTON) {
                ah = c;
                break;
            }
        }
        assertTrue(ah != null);
        p.relocate(ah.x, ah.y, ah.z, ah.o);
        alpha.clear();
        alpha.auctionSell(world, ah.guid, tradedGuid, bid, 0, hours * 60);
    }

    @Then("Alpha received SMSG_AUCTION_COMMAND_RESULT started ok")
    public void auctionStartedOk() {
        assertTrue(alpha.saw(Opcodes.SMSG_AUCTION_COMMAND_RESULT));
        byte[] p = alpha.payload(Opcodes.SMSG_AUCTION_COMMAND_RESULT);
        assertTrue(WowClientDouble.u32le(p, 0) != 0);
        assertEquals(0, WowClientDouble.u32le(p, 4));
        assertEquals(0, WowClientDouble.u32le(p, 8));
    }

    @Given("Bravo has {int} copper")
    public void bravoCopper(int copper) {
        bravo.session().player().setMoney(copper);
    }

    @When("Bravo bids {int} copper on auction {int} at Chilton")
    public void bravoBid(int price, int auctionId) {
        Player p = bravo.session().player();
        org.tbc.world.entity.Creature ah = null;
        for (org.tbc.world.entity.Creature c : world.map(p.mapId, p.instanceId).creatures.values()) {
            if (c.entry == org.tbc.world.content.Content.NPC_AUCTIONEER_CHILTON) {
                ah = c;
                break;
            }
        }
        assertTrue(ah != null);
        p.relocate(ah.x, ah.y, ah.z, ah.o);
        bravo.clear();
        bravo.auctionBid(world, ah.guid, auctionId, price);
    }

    @Then("Bravo received SMSG_AUCTION_COMMAND_RESULT bid placed ok")
    public void auctionBidOk() {
        assertTrue(bravo.saw(Opcodes.SMSG_AUCTION_COMMAND_RESULT));
        byte[] p = bravo.payload(Opcodes.SMSG_AUCTION_COMMAND_RESULT);
        assertEquals(1, WowClientDouble.u32le(p, 0));
        assertEquals(2, WowClientDouble.u32le(p, 4));
        assertEquals(0, WowClientDouble.u32le(p, 8));
        assertEquals(1, WowClientDouble.u32le(p, 12));
    }

    @Then("Alpha contact list contains Bravo")
    public void contactHasBravo() {
        byte[] p = alpha.payload(Opcodes.SMSG_CONTACT_LIST);
        assertTrue(p.length >= 16, "SMSG_CONTACT_LIST missing after relog");
        assertEquals(SocialHandler.FRIEND_LIST, WowClientDouble.u32le(p, 0));
        assertEquals(1, WowClientDouble.u32le(p, 4));
        long guid = WowClientDouble.u64le(p, 8);
        assertEquals(bravo.session().player().guid, guid);
    }

    @Then("Alpha received party result {int}")
    public void partyResult(int result) {
        byte[] p = alpha.payload(Opcodes.SMSG_PARTY_COMMAND_RESULT);
        WowBuffer b = new WowBuffer(p);
        assertEquals(0, b.getU32());
        b.getCString();
        assertEquals(result, b.getU32());
    }

    @Given("Bravo is {int} yards away")
    public void bravoAway(int yards) {
        Player a = alpha.session().player();
        bravo.session().player().relocate(a.x + yards, a.y, a.z, a.o);
    }

    @When("Alpha initiates trade with Bravo")
    public void initiate() {
        alpha.clear();
        alpha.initiateTrade(world, bravo.session().player().guid);
    }

    @Then("Alpha received trade status {int}")
    public void tradeStatus(int status) {
        byte[] p = alpha.payload(Opcodes.SMSG_TRADE_STATUS);
        assertEquals(status, WowClientDouble.u32le(p, 0));
    }

    @Then("Alpha received friend result {int}")
    public void friendResult(int result) {
        byte[] p = alpha.payload(Opcodes.SMSG_FRIEND_STATUS);
        assertEquals(result, p[0] & 0xFF);
    }

    @When("Alpha sends CMSG_WHO with fewer than {int} bytes")
    public void truncatedWho(int n) {
        assertEquals(8, n);
        alpha.clear();
        alpha.handle(world, Opcodes.CMSG_WHO, new byte[3]);
    }

    @Then("the social session still answers CMSG_PING with SMSG_PONG")
    public void pong() {
        alpha.clear();
        alpha.ping(world, 4);
        assertTrue(alpha.saw(Opcodes.SMSG_PONG));
        assertEquals(4, WowClientDouble.u32le(alpha.payload(Opcodes.SMSG_PONG), 0));
    }

    private static void assertJoined(byte[] p) {
        WowBuffer b = new WowBuffer(p);
        assertEquals(0x03, b.getU8());
        assertEquals(1, b.getU8());
        assertEquals("Bravo", b.getCString());
        assertTrue(b.getU64() != 0);
    }

    private static String otherName(byte[] list) {
        int count = WowClientDouble.u32le(list, 12);
        assertEquals(1, count);
        int i = 16;
        StringBuilder n = new StringBuilder();
        while (i < list.length && list[i] != 0) {
            n.append((char) (list[i] & 0xFF));
            i++;
        }
        return n.toString();
    }

    private static String whoNames(byte[] p) {
        int display = WowClientDouble.u32le(p, 0);
        int i = 8;
        StringBuilder all = new StringBuilder();
        for (int r = 0; r < display; r++) {
            while (i < p.length && p[i] != 0) {
                all.append((char) (p[i] & 0xFF));
                i++;
            }
            i++;
            while (i < p.length && p[i] != 0) {
                i++;
            }
            i++;
            i += 4 + 4 + 4 + 1 + 4;
            all.append(',');
        }
        return all.toString();
    }
}
