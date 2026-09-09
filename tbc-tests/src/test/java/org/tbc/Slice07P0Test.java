package org.tbc;

import org.tbc.bdd.WowClientDouble;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.world.World;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Slice 7 spell P0 wire beyond the Gherkin: kill path parity, cast timer, cooldowns, auras. */
class Slice07P0Test {
    private static final World.Account ACC =
            new World.Account(1, "PLAYER", new byte[40], 3, 1, "Win", "x86");
    private static final int FIREBALL = 133;

    /** Fireball rank 1: Spell.dbc CastingTimeIndex 16 → SpellCastTimes.dbc 1500 ms. */
    private static final int FIREBALL_CAST_MS = 1500;

    /**
     * TP-SL07-003 — Spell::prepare sends SMSG_SPELL_START with the cast timer and only Spell::update
     * (SPELL_STATE_PREPARING, m_timer reaches 0) runs cast(): TakePower, effects, SMSG_SPELL_GO, damage log.
     */
    @Test
    void tpSl07CastTimeDelaysSpellGo() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        Player p = mageWithFireball(world, client);
        Creature c = kobold(world, p);
        int manaBefore = p.power();
        int hpBefore = c.health();
        client.clear();
        client.castSpell(world, FIREBALL, 1, c.guid);
        byte[] start = client.payload(Opcodes.SMSG_SPELL_START);
        int off = WowClientDouble.skipPackedGuid(start, 0);
        off = WowClientDouble.skipPackedGuid(start, off);
        assertEquals(FIREBALL, WowClientDouble.u32le(start, off));
        assertEquals(FIREBALL_CAST_MS, WowClientDouble.u32le(start, off + 4 + 1 + 2));
        assertFalse(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(manaBefore, p.power());
        world.tick(FIREBALL_CAST_MS - 100);
        assertFalse(client.saw(Opcodes.SMSG_SPELL_GO));
        assertEquals(hpBefore, c.health());
        world.tick(100);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        assertTrue(client.saw(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG));
        assertTrue(c.health() < hpBefore);
        assertEquals(manaBefore - 30, client.valuesField(p.guid, UpdateFields.UNIT_FIELD_POWER1));
    }

    /** TP-SL07-011 — Unit::DealDamage → Unit::Kill for spell damage too: XP log, PLAYER_XP, lootable corpse. */
    @Test
    void tpSl07SpellKillRewardsAndLoots() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        Player p = mageWithFireball(world, client);
        Creature c = kobold(world, p);
        c.setHealth(1);
        client.clear();
        client.castSpell(world, FIREBALL, 1, c.guid);
        world.tick(FIREBALL_CAST_MS);
        assertFalse(c.alive());
        byte[] log = client.payload(Opcodes.SMSG_LOG_XPGAIN);
        assertEquals(22, log.length);
        assertEquals(c.guid, WowClientDouble.u64le(log, 0));
        assertEquals(50, WowClientDouble.u32le(log, 8));
        assertEquals(50, client.valuesField(p.guid, UpdateFields.PLAYER_XP));
        client.clear();
        client.loot(world, c.guid);
        assertTrue(client.saw(Opcodes.SMSG_LOOT_RESPONSE));
    }

    /**
     * TP-SL07-004 / TP-SL07-012 — Spell::update: caster position differs from the cast position while the
     * timer runs and SPELL_INTERRUPT_FLAG_MOVEMENT is set → cancel(): SendInterrupted (SMSG_SPELL_FAILURE +
     * SMSG_SPELL_FAILED_OTHER to the set) and SendCastResult(SPELL_FAILED_INTERRUPTED) to the caster.
     */
    @Test
    void tpSl07MovingCancelsCastBar() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        Player p = mageWithFireball(world, client);
        Creature c = kobold(world, p);
        int manaBefore = p.power();
        int hpBefore = c.health();
        client.clear();
        client.castSpell(world, FIREBALL, 1, c.guid);
        world.tick(500);
        client.heartbeat(world, p.x + 1f, p.y, p.z, p.o);
        world.tick(100);

        byte[] res = client.payload(Opcodes.SMSG_CAST_RESULT);
        assertEquals(FIREBALL, WowClientDouble.u32le(res, 0));
        assertEquals(SPELL_FAILED_INTERRUPTED, res[4] & 0xFF);
        assertEquals(1, res[5] & 0xFF, "castCount echoed");
        byte[] fail = client.payload(Opcodes.SMSG_SPELL_FAILURE);
        int off = WowClientDouble.skipPackedGuid(fail, 0);
        assertEquals(FIREBALL, WowClientDouble.u32le(fail, off));
        assertEquals(SPELL_FAILED_INTERRUPTED, fail[off + 4] & 0xFF);
        assertEquals(off + 5, fail.length);
        byte[] other = client.payload(Opcodes.SMSG_SPELL_FAILED_OTHER);
        int off2 = WowClientDouble.skipPackedGuid(other, 0);
        assertEquals(FIREBALL, WowClientDouble.u32le(other, off2));
        assertEquals(off2 + 4, other.length, "no result byte");

        world.tick(FIREBALL_CAST_MS);
        assertFalse(client.saw(Opcodes.SMSG_SPELL_GO), "cancelled cast never lands");
        assertEquals(manaBefore, p.power());
        assertEquals(hpBefore, c.health());
    }

    private static final int SPELL_FAILED_INTERRUPTED = 0x25;

    /**
     * TP-SL07-005 — Spell::prepare AddGCD(StartRecoveryCategory 133, 1500 ms) at cast start; CheckCast
     * HasGCD → SPELL_FAILED_NOT_READY (0x3F) for any spell in that category until it expires. Nothing is
     * sent for the GCD itself (Player::AddGCD updateClient=false; the client runs its own GCD timer).
     */
    @Test
    void tpSl07GlobalCooldownRefusesRecast() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        Player p = mageWithFireball(world, client);
        p.spells.add(LESSER_HEAL);
        Creature c = kobold(world, p);
        client.clear();
        client.castSpell(world, FIREBALL, 1, c.guid);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_START));
        assertFalse(client.saw(Opcodes.SMSG_SPELL_COOLDOWN), "no cooldown packet for a plain GCD");
        client.clear();
        world.advanceMs(100);
        world.tick(100);

        client.castSpell(world, LESSER_HEAL, 2, p.guid);

        byte[] res = client.payload(Opcodes.SMSG_CAST_RESULT);
        assertEquals(LESSER_HEAL, WowClientDouble.u32le(res, 0));
        assertEquals(SPELL_FAILED_NOT_READY, res[4] & 0xFF);
        assertEquals(2, res[5] & 0xFF);
        assertFalse(client.saw(Opcodes.SMSG_SPELL_START));
        world.advanceMs(1400);
        world.tick(1400);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO), "the first cast still lands");
        client.clear();
        client.castSpell(world, LESSER_HEAL, 3, p.guid);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_START), "GCD over → accepted");
        assertFalse(client.saw(Opcodes.SMSG_CAST_RESULT));
    }

    private static final int LESSER_HEAL = 2050;
    private static final int FROST_NOVA = 122;
    /** Spell.dbc RecoveryTime for Frost Nova rank 1. */
    private static final int FROST_NOVA_RECOVERY_MS = 25_000;
    private static final int SPELL_FAILED_NOT_READY = 0x3F;

    /**
     * TP-SL07-005 — Spell::cast → AddCooldown(RecoveryTime); CheckCast IsSpellReady → NOT_READY.
     * No SMSG_SPELL_COOLDOWN on a normal cast (the client mirrors Spell.dbc). GCD 1500 ms is shorter
     * than the 25 s recovery, so after the GCD the spell is still blocked.
     */
    @Test
    void tpSl07RecoveryTimeRefusesRecast() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        Player p = mageWithFireball(world, client);
        p.spells.add(FROST_NOVA);
        client.clear();
        client.castSpell(world, FROST_NOVA, 1, p.guid);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO));
        assertFalse(client.saw(Opcodes.SMSG_SPELL_COOLDOWN), "client tracks RecoveryTime from Spell.dbc");
        world.advanceMs(org.tbc.world.spell.SpellCooldowns.GCD_NORMAL_MS);
        world.tick(org.tbc.world.spell.SpellCooldowns.GCD_NORMAL_MS);
        client.clear();
        client.castSpell(world, FROST_NOVA, 2, p.guid);
        byte[] res = client.payload(Opcodes.SMSG_CAST_RESULT);
        assertEquals(FROST_NOVA, WowClientDouble.u32le(res, 0));
        assertEquals(SPELL_FAILED_NOT_READY, res[4] & 0xFF);
        assertEquals(2, res[5] & 0xFF);
        assertFalse(client.saw(Opcodes.SMSG_SPELL_START));
        world.advanceMs(FROST_NOVA_RECOVERY_MS - org.tbc.world.spell.SpellCooldowns.GCD_NORMAL_MS);
        world.tick(FROST_NOVA_RECOVERY_MS - org.tbc.world.spell.SpellCooldowns.GCD_NORMAL_MS);
        client.clear();
        client.castSpell(world, FROST_NOVA, 3, p.guid);
        assertTrue(client.saw(Opcodes.SMSG_SPELL_GO), "RecoveryTime over → accepted");
        assertFalse(client.saw(Opcodes.SMSG_CAST_RESULT));
    }

    /**
     * TP-SL07-005 — remaining RecoveryTime survives LogoutPlayer save / login via character_spell_cooldown
     * (in-memory: PlayerPersist copy). SendInitialSpells writes the leftover ms; recast is still NOT_READY.
     */
    @Test
    void tpSl07CooldownSurvivesRelog() {
        World world = World.inMemory();
        WowClientDouble client = new WowClientDouble();
        Player p = mageWithFireball(world, client);
        p.spells.add(FROST_NOVA);
        client.castSpell(world, FROST_NOVA, 1, p.guid);
        long guid = p.guid;
        client.session().logout(world, true);
        WowClientDouble again = new WowClientDouble();
        again.connect(ACC);
        again.login(world, guid);
        byte[] spells = again.payload(Opcodes.SMSG_INITIAL_SPELLS);
        int[] cd = initialSpellCooldown(spells, FROST_NOVA);
        assertTrue(cd[0] > 0 && cd[0] <= FROST_NOVA_RECOVERY_MS, "remaining ms " + cd[0]);
        assertEquals(0, cd[1], "itemId");
        assertEquals(0, cd[2], "category");
        Player p2 = again.session().player();
        p2.spells.add(FROST_NOVA);
        again.clear();
        again.castSpell(world, FROST_NOVA, 2, p2.guid);
        byte[] res = again.payload(Opcodes.SMSG_CAST_RESULT);
        assertEquals(FROST_NOVA, WowClientDouble.u32le(res, 0));
        assertEquals(SPELL_FAILED_NOT_READY, res[4] & 0xFF);
    }

    /** login-burst.md SMSG_INITIAL_SPELLS: unk u8, spellCount u16, spells, cooldownCount u16, then entries. */
    private static int[] initialSpellCooldown(byte[] p, int spellId) {
        int off = 1;
        int n = p[off] & 0xFF | ((p[off + 1] & 0xFF) << 8);
        off += 2 + n * 4;
        int cdn = p[off] & 0xFF | ((p[off + 1] & 0xFF) << 8);
        off += 2;
        for (int i = 0; i < cdn; i++) {
            int id = p[off] & 0xFF | ((p[off + 1] & 0xFF) << 8);
            int item = p[off + 2] & 0xFF | ((p[off + 3] & 0xFF) << 8);
            int cat = p[off + 4] & 0xFF | ((p[off + 5] & 0xFF) << 8);
            int remain = WowClientDouble.u32le(p, off + 6);
            if (id == spellId) {
                return new int[]{remain, item, cat};
            }
            off += 14;
        }
        return new int[]{0, 0, 0};
    }

    private static Player mageWithFireball(World world, WowClientDouble client) {
        client.connect(ACC);
        Player created = world.characters.create(ACC.id(), "Pyro", 1, 8, 0, 1, 1, 1, 1, 0, world.objectMgr);
        client.login(world, created.guid);
        Player p = client.session().player();
        p.spells.add(FIREBALL);
        return p;
    }

    private static Creature kobold(World world, Player p) {
        Creature c = world.objectMgr.spawnCreature(6, 0, p.x, p.y, p.z, p.o, world.scripts);
        world.map(p.mapId, p.instanceId).add(c);
        return c;
    }
}
