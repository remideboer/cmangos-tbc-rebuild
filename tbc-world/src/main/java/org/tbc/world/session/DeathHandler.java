package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Corpse;
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

    private DeathHandler() {}

    public static void repop(WorldSession s, World world) {
        Player p = s.player();
        if (p.alive() || p.ghost) {
            return;
        }
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
