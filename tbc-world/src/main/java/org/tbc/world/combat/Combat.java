package org.tbc.world.combat;

import org.tbc.common.WowBuffer;
import org.tbc.world.ai.EventAi;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.net.wow8606.UpdateFields;

/** Auto-attack, evade, corpse loot. Packets: combat-log.md, loot.md. */
public final class Combat {
    public static final float LEASH_RADIUS = 30f;
    public static final float ATTACK_DISTANCE = 5f;
    /** CMaNGOS BASE_MELEERANGE_OFFSET */
    public static final float BASE_MELEERANGE_OFFSET = 1.33f;
    /** CMaNGOS MELEE_LEEWAY (8/3) when both units run. */
    public static final float MELEE_LEEWAY = 8.0f / 3.0f;
    /** CMaNGOS Unit::GetDetectionRange default / same-level aggro. */
    public static final float DEFAULT_DETECTION = 18f;
    public static final int PURSUIT_MS = 15_000;
    public static final int HITINFO_NORMALSWING2 = 0x00000002;
    public static final int HITINFO_LEFTSWING = 0x00000004;
    public static final int HITINFO_MISS = 0x00000010;
    public static final int HITINFO_CRITICALHIT = 0x00000080;
    public static final int HITINFO_BLOCK = 0x00000800;
    public static final int HITINFO_GLANCING = 0x00004000;
    public static final int HITINFO_CRUSHING = 0x00008000;
    public static final int HITINFO_NOACTION = 0x00010000;
    public static final int HITINFO_SWINGNOHITSOUND = 0x00080000;
    public static final int VICTIM_UNAFFECTED = 0;
    public static final int VICTIM_NORMAL = 1;
    public static final int VICTIM_DODGE = 2;
    public static final int VICTIM_PARRY = 3;
    public static final int VICTIM_BLOCKS = 5;
    public static final int VICTIM_EVADES = 6;
    public static final int LOOT_CORPSE = 1;
    public static final int LOOT_SLOT_OWNER = 4;

    private final MeleeTable table;

    public Combat() {
        this(MeleeTable.DEFAULT);
    }

    public Combat(MeleeTable table) {
        this.table = table;
    }

    /** CMaNGOS GetCombinedCombatReach(forMeleeRange=true). */
    public static float meleeRange(Unit attacker) {
        return meleeRange(attacker, null, false);
    }

    public static float meleeRange(Unit attacker, Unit victim) {
        return meleeRange(attacker, victim, false);
    }

    public static float meleeRange(Unit attacker, Unit victim, boolean movingLeeway) {
        float reach = attacker.getFloat(UpdateFields.UNIT_FIELD_COMBATREACH);
        if (victim != null) {
            reach += victim.getFloat(UpdateFields.UNIT_FIELD_COMBATREACH);
        }
        reach += BASE_MELEERANGE_OFFSET;
        if (reach < ATTACK_DISTANCE) {
            reach = ATTACK_DISTANCE;
        }
        if (movingLeeway) {
            reach += MELEE_LEEWAY;
        }
        return reach;
    }

    /** CMaNGOS CanReachWithMeleeAttack leeway: both moving and not walking. */
    public static boolean meleeLeeway(Unit a, Unit b) {
        return movingNotWalking(a) && movingNotWalking(b);
    }

    private static boolean movingNotWalking(Unit u) {
        if (u == null) {
            return false;
        }
        int f = u.movement.moveFlags;
        if ((f & org.tbc.world.net.wow8606.MovementInfo.MOVEFLAG_WALK_MODE) != 0) {
            return false;
        }
        int moving = org.tbc.world.net.wow8606.MovementInfo.MOVEFLAG_FORWARD
                | org.tbc.world.net.wow8606.MovementInfo.MOVEFLAG_BACKWARD
                | org.tbc.world.net.wow8606.MovementInfo.MOVEFLAG_STRAFE_LEFT
                | org.tbc.world.net.wow8606.MovementInfo.MOVEFLAG_STRAFE_RIGHT;
        return (f & moving) != 0;
    }

    /** CMaNGOS Unit::GetAttackDistance (rate 1, no detect auras). */
    public static float attackDistance(Creature c, Unit target) {
        if (c == null || target == null) {
            return 0f;
        }
        float dist = c.detectionRange;
        if (dist == 0f) {
            return 0f;
        }
        int levelDif = target.level - c.level;
        if (levelDif < -25) {
            levelDif = -25;
        }
        dist -= levelDif;
        if (dist < ATTACK_DISTANCE) {
            dist = ATTACK_DISTANCE;
        }
        return dist;
    }

    /** CMaNGOS UnitAI::MoveInLineOfSight + DetectOrAttack (no stealth alert). */
    public static boolean canAggroOnSight(Creature c, Unit target, Factions factions) {
        if (c == null || target == null || !c.alive() || !target.alive() || c.inCombat) {
            return false;
        }
        if (c.ai == null || !c.ai.aggroOnSight()) {
            return false;
        }
        if ((c.extraFlags & Creature.CREATURE_EXTRA_FLAG_NO_AGGRO_ON_SIGHT) != 0) {
            return false;
        }
        if (target instanceof Player pl
                && (pl.getInt(UpdateFields.PLAYER_FLAGS) & Player.PLAYER_FLAGS_GHOST) != 0) {
            return false;
        }
        if (factions == null) {
            return false;
        }
        if (!Relations.canInitiateAttack(c) || !Relations.canAttack(c, target, factions)) {
            return false;
        }
        boolean redBar = factions.reaction(target, c) == FactionTemplate.REP_HOSTILE;
        FactionTemplate self = factions.template(c);
        if (self != null && self.isNeutralToAll() && !redBar) {
            return false;
        }
        if (!redBar && !Relations.canAttackOnSight(c, target, factions)) {
            return false;
        }
        return c.distance2d(target) <= attackDistance(c, target);
    }

    public void startAttack(Player p, Creature c, long nowMs) {
        p.inCombat = true;
        p.victim = c.guid;
        c.inCombat = true;
        c.victim = p.guid;
        c.setGuid(UpdateFields.UNIT_FIELD_TARGET, p.guid);
        p.setGuid(UpdateFields.UNIT_FIELD_TARGET, c.guid);
        c.setInt(UpdateFields.UNIT_FIELD_FLAGS, c.getInt(UpdateFields.UNIT_FIELD_FLAGS) | Unit.UNIT_FLAG_IN_COMBAT);
        p.setInt(UpdateFields.UNIT_FIELD_FLAGS, p.getInt(UpdateFields.UNIT_FIELD_FLAGS) | Unit.UNIT_FLAG_IN_COMBAT);
        c.lastHitMs = nowMs;
        c.lastMeleeMs = nowMs;
        int swing = c.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_BASEATTACKTIME);
        c.meleeCooldownMs = swing > 0 ? swing : 2000;
        if (c.combatStartMs == 0) {
            c.combatStartMs = nowMs;
        }
        if (c.combatMovement) {
            c.motion.moveChase(p);
        } else {
            c.motion.moveIdle();
        }
    }

    public void stopAttack(Player p) {
        p.inCombat = false;
        p.victim = 0;
        clearCombatVisual(p);
    }

    public MeleeTable.Result swing(Player p, Creature c, long nowMs) {
        return swing(p, c, nowMs, EventAi.NOOP);
    }

    public MeleeTable.Result swingOffhand(Player p, Creature c, long nowMs) {
        return swing(p, c, nowMs, EventAi.NOOP, true);
    }

    public MeleeTable.Result swing(Player p, Creature c, long nowMs, EventAi.SpellCast deathCast) {
        return swing(p, c, nowMs, deathCast, false);
    }

    public MeleeTable.Result swing(Player p, Creature c, long nowMs, EventAi.SpellCast deathCast, boolean offhand) {
        if (!c.alive()) {
            return new MeleeTable.Result(MeleeTable.Outcome.MISS, 0, 0);
        }
        int min = offhand ? offhandMin(p) : meleeMin(p);
        int max = offhand ? offhandMax(p) : meleeMax(p);
        int bonus = offhand ? 0 : p.queuedNextMeleeBonus();
        MeleeTable.Result r = table.rollOne(p, c, min + bonus, max + bonus, offhand);
        if (!offhand) {
            p.consumeNextMeleeSwing();
        }
        if (r.damage() > 0) {
            c.setHealth(c.health() - r.damage());
            c.threat += r.threat();
            c.threatManager.add(p, r.threat());
            c.victim = c.threatManager.highestGuid();
            c.lastHitMs = nowMs;
            if (c.taggedBy == 0) {
                c.taggedBy = p.guid;
            }
        }
        if (!c.alive()) {
            c.inCombat = false;
            c.lootable = true;
            c.victim = 0;
            c.respawnAtMs = nowMs + Math.max(1, c.respawnDelayMs);
            clearCombatVisual(c);
            stopAttack(p);
            if (c.eventAi != null) {
                c.eventAi.onDeath(c, p, deathCast == null ? EventAi.NOOP : deathCast);
            }
        }
        return r;
    }

    public MeleeTable.Result swing(Creature attacker, Player victim, long nowMs) {
        return swing(attacker, victim, nowMs, EventAi.NOOP);
    }

    public MeleeTable.Result swing(Creature attacker, Player victim, long nowMs, EventAi.SpellCast killCast) {
        if (!victim.alive() || attacker.evading) {
            return new MeleeTable.Result(MeleeTable.Outcome.MISS, 0, 0);
        }
        MeleeTable.Result r = table.rollOne(attacker, victim, meleeMin(attacker), meleeMax(attacker));
        if (r.damage() > 0) {
            victim.setHealth(victim.health() - r.damage());
            attacker.lastHitMs = nowMs;
        }
        if (!victim.alive()) {
            attacker.inCombat = false;
            attacker.victim = 0;
            clearCombatVisual(attacker);
            stopAttack(victim);
            if (attacker.eventAi != null) {
                attacker.eventAi.onKill(attacker, victim, killCast == null ? EventAi.NOOP : killCast);
            }
        }
        return r;
    }

    private static void clearCombatVisual(Unit u) {
        u.setGuid(UpdateFields.UNIT_FIELD_TARGET, 0);
        u.setInt(UpdateFields.UNIT_FIELD_FLAGS, u.getInt(UpdateFields.UNIT_FIELD_FLAGS) & ~Unit.UNIT_FLAG_IN_COMBAT);
    }

    private static int meleeMin(Unit attacker) {
        return Math.round(attacker.getFloat(UpdateFields.UNIT_FIELD_MINDAMAGE));
    }

    private static int meleeMax(Unit attacker) {
        return Math.round(attacker.getFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE));
    }

    private static int offhandMin(Unit attacker) {
        return Math.round(attacker.getFloat(UpdateFields.UNIT_FIELD_MINOFFHANDDAMAGE));
    }

    private static int offhandMax(Unit attacker) {
        return Math.round(attacker.getFloat(UpdateFields.UNIT_FIELD_MAXOFFHANDDAMAGE));
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
        evade(c, EventAi.NOOP);
    }

    public void evade(Creature c, EventAi.SpellCast cast) {
        c.evading = true;
        c.inCombat = false;
        c.victim = 0;
        clearCombatVisual(c);
        c.threat = 0;
        c.threatManager.reset();
        c.taggedBy = 0;
        c.lootable = false;
        c.lootGold = 0;
        c.lootItems.clear();
        c.combatStartMs = 0;
        c.setHealth(c.maxHealth());
        double homeDist = Math.hypot(c.x - c.spawnX, c.y - c.spawnY);
        if (homeDist < 0.5) {
            finishEvade(c, cast, true);
            return;
        }
        c.setInt(UpdateFields.UNIT_FIELD_FLAGS, c.getInt(UpdateFields.UNIT_FIELD_FLAGS) | Unit.UNIT_FLAG_EVADING_HOME);
        c.motion.moveHome();
        if (c.eventAi != null) {
            c.eventAi.onEvade(c, cast == null ? EventAi.NOOP : cast);
        }
    }

    public void finishEvade(Creature c, EventAi.SpellCast cast) {
        finishEvade(c, cast, false);
    }

    private void finishEvade(Creature c, EventAi.SpellCast cast, boolean fireEvadeEvent) {
        c.relocate(c.spawnX, c.spawnY, c.spawnZ, c.spawnO);
        c.motion.moveIdle();
        c.setInt(UpdateFields.UNIT_FIELD_FLAGS, c.getInt(UpdateFields.UNIT_FIELD_FLAGS) & ~Unit.UNIT_FLAG_EVADING_HOME);
        c.evading = false;
        if (c.eventAi != null) {
            EventAi.SpellCast sink = cast == null ? EventAi.NOOP : cast;
            if (fireEvadeEvent) {
                c.eventAi.onEvade(c, sink);
            }
            c.eventAi.onReachedHome(c, sink);
        }
    }

    public void respawn(Creature c) {
        if (c == null) {
            return;
        }
        c.setHealth(c.maxHealth());
        c.lootable = false;
        c.lootGold = 0;
        c.lootItems.clear();
        c.taggedBy = 0;
        c.respawnAtMs = 0;
        c.inCombat = false;
        c.victim = 0;
        c.evading = false;
        c.relocate(c.spawnX, c.spawnY, c.spawnZ, c.spawnO);
        c.motion.moveIdle();
        c.startOocMotion();
    }

    public byte[] lootResponse(Player p, Creature c) {
        if (!canLoot(p, c)) {
            return null;
        }
        return encodeLoot(c.guid, c.lootGold, c.lootItems);
    }

    public Item takeItem(Player p, Creature c, int lootSlot, long itemGuid) {
        if (!canLoot(p, c) || itemGuid == 0) {
            return null;
        }
        int idx = -1;
        org.tbc.world.loot.LootSlot found = null;
        for (int i = 0; i < c.lootItems.size(); i++) {
            if (c.lootItems.get(i).slot() == lootSlot) {
                found = c.lootItems.get(i);
                idx = i;
                break;
            }
        }
        if (found == null) {
            return null;
        }
        int bagSlot = p.firstFreeBagSlot();
        if (bagSlot < 0) {
            return null;
        }
        Item it = new Item(itemGuid, found.itemId());
        it.ownerGuid = Guid.low(p.guid);
        it.bag = 0;
        it.slot = bagSlot;
        it.count = found.count();
        it.displayId = found.displayId();
        p.items.put(Guid.low(it.guid), it);
        p.dirty = true;
        c.lootItems.remove(idx);
        return it;
    }

    public boolean takeMoney(Player p, Creature c) {
        if (!canLoot(p, c)) {
            return false;
        }
        int gold = c.lootGold;
        c.lootGold = 0;
        if (gold > 0) {
            p.setMoney(p.money + gold);
        }
        return true;
    }

    public byte[] encodeLootRemoved(int lootIndex) {
        WowBuffer b = new WowBuffer(1);
        b.putU8(lootIndex);
        return b.array();
    }

    private static boolean canLoot(Player p, Creature c) {
        if (p == null || c == null || !c.lootable) {
            return false;
        }
        return c.taggedBy == 0 || c.taggedBy == p.guid;
    }

    public byte[] encodeAttack(Unit attacker, Unit victim, MeleeTable.Result r) {
        return encodeAttack(attacker, victim, r, false);
    }

    public byte[] encodeAttack(Unit attacker, Unit victim, MeleeTable.Result r, boolean spellSwing) {
        return encodeAttack(attacker, victim, r, spellSwing, false);
    }

    public byte[] encodeAttack(Unit attacker, Unit victim, MeleeTable.Result r, boolean spellSwing, boolean leftSwing) {
        int hitInfo = leftSwing ? HITINFO_LEFTSWING : HITINFO_NORMALSWING2;
        int victimState = VICTIM_NORMAL;
        switch (r.outcome()) {
            case MISS -> {
                hitInfo |= HITINFO_MISS;
                victimState = VICTIM_UNAFFECTED;
            }
            case DODGE -> victimState = VICTIM_DODGE;
            case PARRY -> victimState = VICTIM_PARRY;
            case BLOCK -> {
                hitInfo |= HITINFO_BLOCK;
                victimState = VICTIM_BLOCKS;
            }
            case GLANCE -> hitInfo |= HITINFO_GLANCING;
            case CRIT -> hitInfo |= HITINFO_CRITICALHIT;
            case CRUSH -> hitInfo |= HITINFO_CRUSHING;
            case EVADE -> {
                hitInfo |= HITINFO_MISS | HITINFO_SWINGNOHITSOUND;
                victimState = VICTIM_EVADES;
            }
            default -> {
            }
        }
        if (spellSwing) {
            hitInfo |= HITINFO_NOACTION;
        }
        WowBuffer b = new WowBuffer(64);
        b.putU32(hitInfo);
        b.putPackedGuid(attacker.guid);
        b.putPackedGuid(victim.guid);
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
        b.putU32(r.blocked());
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
        return encodeLoot(guid, gold, java.util.List.of());
    }

    public byte[] encodeLoot(long guid, int gold, java.util.List<org.tbc.world.loot.LootSlot> items) {
        if (items == null) {
            items = java.util.List.of();
        }
        WowBuffer b = new WowBuffer(16 + items.size() * 26);
        b.putU64(guid);
        b.putU8(LOOT_CORPSE);
        b.putU32(gold);
        b.putU8(items.size());
        for (org.tbc.world.loot.LootSlot it : items) {
            b.putU8(it.slot());
            b.putU32(it.itemId());
            b.putU32(it.count());
            b.putU32(it.displayId());
            b.putU32(0);
            b.putU32(0);
            b.putU8(LOOT_SLOT_OWNER);
        }
        return b.array();
    }

    public byte[] encodeLootRelease(long guid) {
        WowBuffer b = new WowBuffer(9);
        b.putU64(guid);
        b.putU8(1);
        return b.array();
    }
}
