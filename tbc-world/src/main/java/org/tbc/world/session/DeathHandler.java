package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Corpse;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.map.GraveyardManager;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.pvp.PvpObjectives;
import org.tbc.world.spell.SpellCastTargets;
import org.tbc.world.world.World;

/** Repop, reclaim, spirit healer. Layout: spec/03-protocol/packets/death.md */
public final class DeathHandler {
    /** WorldSafeLocs id 4 — Northshire Abbey, closest to human start. */
    public static final int GY_ELWYNN_MAP = 0;
    public static final float GY_ELWYNN_X = -9115.27f;
    public static final float GY_ELWYNN_Y = 423.261f;
    public static final float GY_ELWYNN_Z = 92.5f;
    public static final int CORPSE_RECLAIM_DELAY_FIRST_MS = 30_000;
    public static final float CORPSE_RECLAIM_RADIUS = 39f;
    /** Player::KillPlayer — 6 × MINUTE × IN_MILLISECONDS. */
    public static final int DEATH_TIMER_MS = 6 * 60 * 1000;

    private DeathHandler() {}

    /** Player::KillPlayer — start continent death timer; no corpse yet. */
    public static void killPlayer(WorldSession s, World world) {
        Player p = s.player();
        if (p.ghost || !p.alive()) {
            return;
        }
        p.setHealth(0);
        // Non-instance maps only (death.md); continents 0/1/530.
        if (p.mapId == 0 || p.mapId == 1 || p.mapId == 530) {
            p.deathTimerEndsAtMs = world.nowMs() + DEATH_TIMER_MS;
        } else {
            p.deathTimerEndsAtMs = 0;
        }
    }

    /** Player Update: m_deathTimer expired → BuildPlayerRepop + RepopAtGraveyard. */
    public static void tickDeathTimers(World world) {
        long now = world.nowMs();
        for (Player p : world.playersOnline()) {
            if (p.session == null || p.deathTimerEndsAtMs == 0 || now < p.deathTimerEndsAtMs) {
                continue;
            }
            if (p.ghost || p.alive()) {
                p.deathTimerEndsAtMs = 0;
                continue;
            }
            repop(p.session, world);
        }
    }

    public static void repop(WorldSession s, World world) {
        Player p = s.player();
        if (p.alive() || p.ghost) {
            return;
        }
        p.deathTimerEndsAtMs = 0;
        float deathX = p.x;
        float deathY = p.y;
        float deathZ = p.z;
        p.setGhost(true);
        p.ghostTimeMs = world.nowMs();
        p.setHealth(1);
        Corpse corpse = new Corpse();
        corpse.ownerGuid = p.guid;
        corpse.mapId = p.mapId;
        corpse.relocate(deathX, deathY, deathZ, p.o);
        corpse.expireAtMs = world.nowMs() + Corpse.RESURRECTABLE_MS;
        p.corpse = corpse;
        world.corpses.put(p.guid, corpse);
        if (p.auras.stream().noneMatch(a -> a.spellId() == PvpObjectives.GHOST_AURA)) {
            p.auras.add(new Unit.Aura(PvpObjectives.GHOST_AURA, 0, 1));
        }
        GraveyardManager.Loc gy = world.graveyards.closest(p.mapId, deathX, deathY, deathZ, p.team,
                world.terrain.area(p.mapId, deathX, deathY));
        if (gy != null) {
            float z = world.terrain.at(gy.map(), gy.x(), gy.y(), gy.z());
            world.teleport(p, gy.map(), gy.x(), gy.y(), z, gy.o());
            WowBuffer loc = new WowBuffer(16);
            loc.putU32(gy.map());
            loc.putFloat(gy.x());
            loc.putFloat(gy.y());
            loc.putFloat(z);
            s.send(Opcodes.SMSG_DEATH_RELEASE_LOC, loc.array());
        }
        sendWaterWalk(s, true);
        sendGhostValues(s, p);
        WowBuffer delay = new WowBuffer(4);
        delay.putU32(CORPSE_RECLAIM_DELAY_FIRST_MS);
        s.send(Opcodes.SMSG_CORPSE_RECLAIM_DELAY, delay.array());
        WowBuffer q = new WowBuffer(32);
        q.putU8(1);
        q.putU32(corpse.mapId);
        q.putFloat(corpse.x);
        q.putFloat(corpse.y);
        q.putFloat(corpse.z);
        q.putU32(corpse.mapId);
        s.send(Opcodes.MSG_CORPSE_QUERY, q.array());
        s.send(Opcodes.SMSG_SPELL_GO, world.spells.encodeGo(
                p.guid, p.guid, PvpObjectives.GHOST_AURA, world.nowMs(), new SpellCastTargets()));
    }

    public static void query(WorldSession s) {
        Player p = s.player();
        if (p.corpse == null) {
            s.send(Opcodes.MSG_CORPSE_QUERY, new byte[]{0});
            return;
        }
        Corpse corpse = p.corpse;
        WowBuffer q = new WowBuffer(32);
        q.putU8(1);
        q.putU32(corpse.mapId);
        q.putFloat(corpse.x);
        q.putFloat(corpse.y);
        q.putFloat(corpse.z);
        q.putU32(corpse.mapId);
        s.send(Opcodes.MSG_CORPSE_QUERY, q.array());
    }

    /** ObjectAccessor::RemoveOldCorpses. world-loop.md WUPDATE_CORPSES. */
    public static void removeOldCorpses(World world) {
        long now = world.nowMs();
        var it = world.corpses.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            Corpse corpse = e.getValue();
            if (corpse == null || !corpse.expired(now)) {
                continue;
            }
            Player owner = world.playerByGuid(e.getKey());
            if (owner != null && owner.corpse == corpse) {
                owner.corpse = null;
            }
            it.remove();
        }
    }

    public static void reclaim(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() >= 8) {
            in.getU64();
        }
        Player p = s.player();
        if (!p.ghost || p.corpse == null) {
            return;
        }
        if (world.nowMs() < p.ghostTimeMs + CORPSE_RECLAIM_DELAY_FIRST_MS) {
            return;
        }
        if (p.distance2d(p.corpse) > CORPSE_RECLAIM_RADIUS) {
            return;
        }
        resurrect(s, p);
        int max = p.maxHealth() == 0 ? 100 : p.maxHealth();
        p.setHealth(max / 2);
    }

    public static void spiritHealer(WorldSession s, World world) {
        Player p = s.player();
        resurrect(s, p);
        int max = p.maxHealth() == 0 ? 100 : p.maxHealth();
        p.setHealth(max / 2);
        for (Item it : p.items.values()) {
            it.durability = (int) (it.durability * 0.75);
        }
        if (p.level >= 11) {
            p.auras.add(new Unit.Aura(PvpObjectives.SICKNESS, 0, 1));
            s.send(Opcodes.SMSG_SPELL_GO, world.spells.encodeGo(
                    p.guid, p.guid, PvpObjectives.SICKNESS, world.nowMs(), new SpellCastTargets()));
        }
    }

    /**
     * HandleAreaSpiritHealerQueryOpcode + Creature::SendAreaSpiritHealerQueryOpcode.
     * Must be on a BG map; healer must be spirit service.
     */
    public static void areaSpiritQuery(WorldSession s, World world, WowBuffer in) {
        Player p = s.player();
        int map = p.mapId;
        boolean bg = map == 489 || map == 529 || map == 30 || map == 566;
        if (!bg) {
            return;
        }
        long guid = in.remaining() >= 8 ? in.getU64() : 0;
        Creature unit = Content.creature(world.map(p.mapId, p.instanceId), guid);
        if (unit == null) {
            return;
        }
        int flags = unit.npcFlags;
        if ((flags & (UNIT_NPC_FLAG_SPIRITHEALER | UNIT_NPC_FLAG_SPIRITGUIDE)) == 0) {
            return;
        }
        // No channeled resurrect spell yet → nextResurrectMs = 0.
        WowBuffer data = new WowBuffer(12);
        data.putU64(unit.guid);
        data.putU32(0);
        s.send(Opcodes.SMSG_AREA_SPIRIT_HEALER_TIME, data.array());
    }

    static final int UNIT_NPC_FLAG_SPIRITHEALER = 0x00004000;
    static final int UNIT_NPC_FLAG_SPIRITGUIDE = 0x00008000;

    /** WorldSession::HandleSelfResOpcode — cast PLAYER_SELF_RES_SPELL then clear. */
    public static void selfRes(WorldSession s, World world) {
        Player p = s.player();
        int spellId = p.getInt(UpdateFields.PLAYER_SELF_RES_SPELL);
        if (spellId == 0) {
            return;
        }
        s.send(Opcodes.SMSG_SPELL_GO, world.spells.encodeGo(
                p.guid, p.guid, spellId, world.nowMs(), new SpellCastTargets()));
        p.setInt(UpdateFields.PLAYER_SELF_RES_SPELL, 0);
    }

    /** Player::SendResurrectRequest — stores pending data and emits SMSG_RESURRECT_REQUEST. */
    public static void offerResurrect(WorldSession s, long casterGuid, String name,
                                      boolean spiritHealer, int health, int mana) {
        Player p = s.player();
        p.resurrectGuid = casterGuid;
        p.resurrectMap = p.mapId;
        p.resurrectX = p.x;
        p.resurrectY = p.y;
        p.resurrectZ = p.z;
        p.resurrectHealth = health;
        p.resurrectMana = mana;
        String n = name == null ? "" : name;
        byte[] nameBytes = n.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        WowBuffer out = new WowBuffer(8 + 4 + nameBytes.length + 1 + 2);
        out.putU64(casterGuid);
        out.putU32(nameBytes.length + 1);
        out.putBytes(nameBytes);
        out.putU8(0);
        out.putU8(spiritHealer ? 1 : 0);
        out.putU8(0);
        s.send(Opcodes.SMSG_RESURRECT_REQUEST, out.array());
    }

    /** WorldSession::HandleResurrectResponseOpcode. */
    public static void resurrectResponse(WorldSession s, World world, WowBuffer in) {
        if (in.remaining() < 9) {
            return;
        }
        long guid = in.getU64();
        int status = in.getU8();
        Player p = s.player();
        // Ghosts keep HP 1; Unit.alive() is true — CMaNGOS IsAlive() is false for ghosts.
        if (!p.ghost) {
            return;
        }
        if (status == 0) {
            clearResurrectRequest(p);
            return;
        }
        if (p.resurrectGuid != guid) {
            return;
        }
        resurrect(s, p);
        int max = p.maxHealth() == 0 ? 100 : p.maxHealth();
        int hp = p.resurrectHealth;
        p.setHealth(hp > 0 && hp < max ? hp : max);
        if (p.resurrectMana > 0) {
            p.setPower(p.resurrectMana);
        }
        clearResurrectRequest(p);
    }

    private static void clearResurrectRequest(Player p) {
        p.resurrectGuid = 0;
        p.resurrectMap = 0;
        p.resurrectX = p.resurrectY = p.resurrectZ = 0;
        p.resurrectHealth = 0;
        p.resurrectMana = 0;
    }

    private static void resurrect(WorldSession s, Player p) {
        p.setGhost(false);
        p.auras.removeIf(a -> a.spellId() == PvpObjectives.GHOST_AURA);
        WowBuffer hide = new WowBuffer(16);
        hide.putU32(0xFFFFFFFF);
        hide.putFloat(0);
        hide.putFloat(0);
        hide.putFloat(0);
        s.send(Opcodes.SMSG_DEATH_RELEASE_LOC, hide.array());
        sendWaterWalk(s, false);
        sendGhostValues(s, p);
    }

    private static void sendGhostValues(WorldSession s, Player p) {
        var upd = UpdateBuilder.maybeCompress(UpdateBuilder.values(p, UpdateFields.PLAYER_FLAGS));
        s.send(upd.opcode(), upd.payload());
    }

    private static void sendWaterWalk(WorldSession s, boolean water) {
        WowBuffer b = new WowBuffer(16);
        b.putPackedGuid(s.player().guid);
        b.putU32(s.nextMoveOrder());
        s.send(water ? Opcodes.SMSG_MOVE_WATER_WALK : Opcodes.SMSG_MOVE_LAND_WALK, b.array());
    }
}
