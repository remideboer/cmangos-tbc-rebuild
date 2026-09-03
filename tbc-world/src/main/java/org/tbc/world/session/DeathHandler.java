package org.tbc.world.session;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Corpse;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.Opcodes;
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
        p.ghost = true;
        p.ghostTimeMs = world.nowMs();
        p.setHealth(1);
        Corpse corpse = new Corpse();
        corpse.ownerGuid = p.guid;
        corpse.mapId = p.mapId;
        corpse.relocate(deathX, deathY, deathZ, p.o);
        p.corpse = corpse;
        if (p.auras.stream().noneMatch(a -> a.spellId() == PvpObjectives.GHOST_AURA)) {
            p.auras.add(new Unit.Aura(PvpObjectives.GHOST_AURA, 0, 1));
        }
        world.teleport(p, GY_ELWYNN_MAP, GY_ELWYNN_X, GY_ELWYNN_Y, GY_ELWYNN_Z, 0);
        WowBuffer loc = new WowBuffer(16);
        loc.putU32(GY_ELWYNN_MAP);
        loc.putFloat(GY_ELWYNN_X);
        loc.putFloat(GY_ELWYNN_Y);
        loc.putFloat(GY_ELWYNN_Z);
        s.send(Opcodes.SMSG_DEATH_RELEASE_LOC, loc.array());
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
        p.ghost = false;
        p.auras.removeIf(a -> a.spellId() == PvpObjectives.GHOST_AURA);
        int max = p.maxHealth() == 0 ? 100 : p.maxHealth();
        p.setHealth(max / 2);
    }

    public static void spiritHealer(WorldSession s, World world) {
        Player p = s.player();
        p.ghost = false;
        p.auras.removeIf(a -> a.spellId() == PvpObjectives.GHOST_AURA);
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
}
