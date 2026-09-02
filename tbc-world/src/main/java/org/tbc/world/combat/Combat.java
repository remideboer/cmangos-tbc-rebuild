package org.tbc.world.combat;

import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Player;

/** Auto-attack, evade, corpse loot. Packets: combat-log.md, loot.md. */
public final class Combat {
    public static final float LEASH_RADIUS = 30f;
    public static final int PURSUIT_MS = 15_000;
    public static final int HITINFO_NORMALSWING2 = 0x00000002;
    public static final int HITINFO_MISS = 0x00000010;
    public static final int HITINFO_CRITICALHIT = 0x00000080;
    public static final int HITINFO_BLOCK = 0x00000800;
    public static final int HITINFO_GLANCING = 0x00004000;
    public static final int VICTIM_UNAFFECTED = 0;
    public static final int VICTIM_NORMAL = 1;
    public static final int VICTIM_DODGE = 2;
    public static final int VICTIM_PARRY = 3;
    public static final int VICTIM_BLOCKS = 5;
    public static final int LOOT_CORPSE = 1;

    private final MeleeTable table;

    public Combat() {
        this(MeleeTable.DEFAULT);
    }

    public Combat(MeleeTable table) {
        this.table = table;
    }

    public void startAttack(Player p, Creature c, long nowMs) {
        p.inCombat = true;
        p.victim = c.guid;
        c.inCombat = true;
        c.victim = p.guid;
        c.lastHitMs = nowMs;
        if (c.combatStartMs == 0) {
            c.combatStartMs = nowMs;
        }
    }

    public void stopAttack(Player p) {
        p.inCombat = false;
        p.victim = 0;
    }

    public MeleeTable.Result swing(Player p, Creature c, long nowMs) {
        if (!c.alive() || c.evading) {
            return new MeleeTable.Result(MeleeTable.Outcome.MISS, 0, 0);
        }
        MeleeTable.Result r = table.rollOne(p, c, 1, 3);
        if (r.damage() > 0) {
            c.setHealth(c.health() - r.damage());
            c.threat += r.threat();
            c.lastHitMs = nowMs;
            if (c.taggedBy == 0) {
                c.taggedBy = p.guid;
            }
        }
        if (!c.alive()) {
            c.inCombat = false;
            c.lootable = true;
            c.victim = 0;
            stopAttack(p);
        }
        return r;
    }

    public boolean shouldEvade(Creature c, Player victim, long nowMs) {
        if (!c.inCombat || !c.alive()) {
            return false;
        }
        if (victim == null) {
            return true;
        }
        if (c.spawnDistance2d(victim.x, victim.y) > LEASH_RADIUS) {
            return true;
        }
        return nowMs - c.lastHitMs >= PURSUIT_MS;
    }

    public void evade(Creature c) {
        c.evading = true;
        c.inCombat = false;
        c.victim = 0;
        c.threat = 0;
        c.taggedBy = 0;
        c.lootable = false;
        c.combatStartMs = 0;
        c.setHealth(c.maxHealth());
        c.relocate(c.spawnX, c.spawnY, c.spawnZ, c.spawnO);
        c.evading = false;
    }

    public byte[] lootResponse(Player p, Creature c) {
        if (c == null || !c.lootable) {
            return null;
        }
        if (c.taggedBy != 0 && c.taggedBy != p.guid) {
            return null;
        }
        return encodeLoot(c.guid, 0, 0);
    }

    public byte[] encodeAttack(Player p, Creature c, MeleeTable.Result r) {
        int hitInfo = HITINFO_NORMALSWING2;
        int victimState = VICTIM_NORMAL;
        switch (r.outcome()) {
            case MISS -> {
                hitInfo = HITINFO_MISS;
                victimState = VICTIM_UNAFFECTED;
            }
            case DODGE -> victimState = VICTIM_DODGE;
            case PARRY -> victimState = VICTIM_PARRY;
            case BLOCK -> {
                hitInfo |= HITINFO_BLOCK;
                victimState = VICTIM_BLOCKS;
            }
            case GLANCE -> hitInfo |= HITINFO_GLANCING;
            default -> {
            }
        }
        WowBuffer b = new WowBuffer(64);
        b.putU32(hitInfo);
        b.putPackedGuid(p.guid);
        b.putPackedGuid(c.guid);
        b.putU32(r.damage());
        b.putU8(1);
        b.putU32(1);
        b.putFloat(r.damage());
        b.putU32(r.damage());
        b.putU32(0);
        b.putU32(0);
        b.putU32(victimState);
        b.putU32(0);
        b.putU32(0);
        b.putU32(0);
        return b.array();
    }

    public byte[] encodeAttackStart(long attacker, long victim) {
        WowBuffer b = new WowBuffer(16);
        b.putU64(attacker);
        b.putU64(victim);
        return b.array();
    }

    public byte[] encodeAttackStop(long attacker, long victim, boolean attackerDead) {
        WowBuffer b = new WowBuffer(24);
        b.putPackedGuid(attacker);
        b.putPackedGuid(victim);
        b.putU32(attackerDead ? 1 : 0);
        return b.array();
    }

    public byte[] encodeLoot(long guid, int gold, int itemCount) {
        WowBuffer b = new WowBuffer(16);
        b.putU64(guid);
        b.putU8(LOOT_CORPSE);
        b.putU32(gold);
        b.putU8(itemCount);
        return b.array();
    }

    public byte[] encodeLootRelease(long guid) {
        WowBuffer b = new WowBuffer(9);
        b.putU64(guid);
        b.putU8(1);
        return b.array();
    }
}
