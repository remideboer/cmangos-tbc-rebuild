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
    /** CMaNGOS CONFIG_FLOAT_LEASH_RADIUS — victim 2d from last refresh after pursuit. */
    public static final float LEASH_RADIUS = 30f;
    /**
     * Outdoor combat-start hard leash when template Leash is 0.
     * CMaNGOS CombatManager comment: drop combat at 90 yd; template Leash is the same check.
     */
    public static final float COMBAT_START_LEASH = 90f;
    /** CMaNGOS CombatManager::StartEvadeTimer. */
    public static final int EVADE_TIMER_MS = 10_000;
    /** CMaNGOS CREATURE_Z_ATTACK_RANGE_MELEE. */
    public static final float CREATURE_Z_ATTACK_RANGE_MELEE = 3f;
    public static final float ATTACK_DISTANCE = 5f;
    /** CMaNGOS Unit::UpdateMeleeAttackingState swingError timer. */
    public static final int SWING_ERROR_RETRY_MS = 100;
    public static final int SWING_ERROR_NONE = 0;
    public static final int SWING_ERROR_NOT_IN_RANGE = 1;
    public static final int SWING_ERROR_BAD_FACING = 2;
    /** CMaNGOS UpdateMeleeAttackingState HasInArc(2π/3). */
    public static final float MELEE_FACING_ARC = (float) (2.0 * Math.PI / 3.0);
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

    /**
     * CMaNGOS Unit::CanReachWithMeleeAttack — creatures use 2d; equality hits so chase-stop can swing.
     */
    public static boolean canReachWithMeleeAttack(Unit attacker, Unit victim) {
        if (attacker == null || victim == null || !victim.alive()) {
            return false;
        }
        return attacker.distance2d(victim) <= meleeRange(attacker, victim, meleeLeeway(attacker, victim));
    }

    /** CMaNGOS UpdateMeleeAttackingState HasInArc — stacked targets count as facing. */
    public static boolean hasMeleeFacing(Unit attacker, Unit victim) {
        if (attacker == null || victim == null) {
            return false;
        }
        if (attacker.distance2d(victim) < 1e-4) {
            return true;
        }
        return hasInArc(attacker, victim, MELEE_FACING_ARC);
    }

    /** CMaNGOS WorldObject::HasInArc. */
    public static boolean hasInArc(Unit attacker, Unit victim, float arc) {
        if (attacker == null || victim == null) {
            return false;
        }
        if (attacker == victim) {
            return true;
        }
        arc = normalizeOrientation(arc);
        float angle = angleTo(attacker.x, attacker.y, victim.x, victim.y);
        angle -= attacker.o;
        angle = normalizeOrientation(angle);
        if (angle > Math.PI) {
            angle -= (float) (2.0 * Math.PI);
        }
        float half = arc / 2f;
        return angle >= -half && angle <= half;
    }

    static float angleTo(float x, float y, float ox, float oy) {
        float ang = (float) Math.atan2(oy - y, ox - x);
        return ang >= 0 ? ang : (float) (2.0 * Math.PI + ang);
    }

    static float normalizeOrientation(float o) {
        double twoPi = Math.PI * 2.0;
        if (o < 0) {
            double mod = (-o) % twoPi;
            return (float) (-mod + twoPi);
        }
        return (float) (o % twoPi);
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
                && (pl.ghost || (pl.getInt(UpdateFields.PLAYER_FLAGS) & Player.PLAYER_FLAGS_GHOST) != 0)) {
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
        c.lastMeleeMs = nowMs;
        refreshCombatTimer(c, nowMs);
        int swing = c.getInt(org.tbc.world.net.wow8606.UpdateFields.UNIT_FIELD_BASEATTACKTIME);
        c.meleeCooldownMs = swing > 0 ? swing : 2000;
        if (c.combatStartMs == 0) {
            c.combatStartMs = nowMs;
            c.combatStartX = c.x;
            c.combatStartY = c.y;
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

    /**
     * CMaNGOS HostileRefManager::getSize — a living in-combat creature still hates this player.
     */
    public boolean hasHostiles(Player p, Iterable<Creature> creatures) {
        if (p == null || creatures == null) {
            return false;
        }
        for (Creature c : creatures) {
            if (!c.inCombat || !c.alive()) {
                continue;
            }
            if (c.victim == p.guid || c.threatManager.threatOf(p) > 0f) {
                return true;
            }
        }
        return false;
    }

    /**
     * CMaNGOS CombatManager player path: IN_COMBAT and empty HostileRefManager → HandleExitCombat.
     * Duel partners are player hostiles (not on the creature threat list).
     */
    public boolean shouldLeaveCombat(Player p, Iterable<Creature> creatures) {
        return p != null && p.inCombat && p.duelOpponent == null && !hasHostiles(p, creatures);
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
            refreshCombatTimer(c, nowMs);
            if (c.taggedBy == 0) {
                c.taggedBy = p.guid;
            }
        }
        if (!c.alive()) {
            creatureDied(c, p, nowMs, deathCast);
        }
        return r;
    }

    /** Unit::Kill for a creature victim (any damage type): combat stop, lootable, respawn timer, EventAI death. */
    public void creatureDied(Creature c, Player killer, long nowMs, EventAi.SpellCast deathCast) {
        if (c.taggedBy == 0) {
            c.taggedBy = killer.guid;
        }
        c.inCombat = false;
        c.lootable = true;
        c.victim = 0;
        c.respawnAtMs = nowMs + Math.max(1, c.respawnDelayMs);
        c.motion.moveIdle();
        clearCombatVisual(c);
        stopAttack(killer);
        if (c.eventAi != null) {
            c.eventAi.onDeath(c, killer, deathCast == null ? EventAi.NOOP : deathCast);
        }
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
            refreshCombatTimer(attacker, nowMs);
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
        float leash = combatStartLeashYards(c);
        if (leash > 0) {
            double fromStart = Math.hypot(c.x - c.combatStartX, c.y - c.combatStartY);
            if (fromStart > leash) {
                return true;
            }
        }
        int pursuit = c.pursuitMs > 0 ? c.pursuitMs : PURSUIT_MS;
        if (nowMs - c.lastHitMs < pursuit) {
            return false;
        }
        return Math.hypot(victim.x - c.lastRefreshX, victim.y - c.lastRefreshY) > LEASH_RADIUS;
    }

    /**
     * Template Leash from combat-start; outdoor default 90 yd when Leash is 0 (not dungeons).
     */
    public static float combatStartLeashYards(Creature c) {
        if (c.leashYards > 0) {
            return c.leashYards;
        }
        return usesDefaultCombatStartLeash(c.mapId) ? COMBAT_START_LEASH : 0f;
    }

    /** Continents: Eastern Kingdoms, Kalimdor, Outland. CMaNGOS skips this timer path in dungeons. */
    public static boolean usesDefaultCombatStartLeash(int mapId) {
        return mapId == 0 || mapId == 1 || mapId == 530;
    }

    /** CMaNGOS CombatManager::IsInEvadeMode — timer or HOME. */
    public static boolean inEvadeMode(Creature c) {
        return c.evading || c.evadeTimerMs > 0;
    }

    /**
     * CMaNGOS SelectHostileTarget + CombatManager evade timer: unreachable starts 10 s,
     * reachable StopEvade, expiry EvadeTimerExpired.
     */
    public boolean tickUnreachableEvade(Creature c, Player victim, int diff) {
        if (!c.inCombat || !c.alive()) {
            c.evadeTimerMs = 0;
            return false;
        }
        if (c.evadeTimerMs > 0) {
            if (c.evadeTimerMs <= diff) {
                c.evadeTimerMs = 0;
                return true;
            }
            c.evadeTimerMs -= diff;
        }
        if (victim == null || !c.combatMovement) {
            return false;
        }
        if (canReachVictim(c, victim)) {
            c.evadeTimerMs = 0;
            return false;
        }
        if (c.evadeTimerMs <= 0) {
            c.evadeTimerMs = EVADE_TIMER_MS;
        }
        return false;
    }

    static boolean canReachVictim(Creature c, Player victim) {
        if (c.chaseUnreachable) {
            return false;
        }
        float z = Math.abs(c.z - victim.z)
                - c.getFloat(UpdateFields.UNIT_FIELD_COMBATREACH)
                - victim.getFloat(UpdateFields.UNIT_FIELD_COMBATREACH);
        if (z < 0f) {
            z = 0f;
        }
        return z <= CREATURE_Z_ATTACK_RANGE_MELEE;
    }

    private static void refreshCombatTimer(Creature c, long nowMs) {
        c.lastHitMs = nowMs;
        c.lastRefreshX = c.x;
        c.lastRefreshY = c.y;
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
        c.evadeTimerMs = 0;
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
        c.evadeTimerMs = 0;
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
        c.evadeTimerMs = 0;
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
        p.setGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + bagSlot * 2,
                Guid.HIGH_ITEM | (Guid.low(it.guid) & 0xFFFFFFFFL));
        p.dirty = true;
        c.lootItems.remove(idx);
        finishLootIfEmpty(c);
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
        finishLootIfEmpty(c);
        return true;
    }

    /** Loot::IsLootedForAll → Creature::SetLootStatus(LOOTED): sparkle off when nothing remains. */
    static void finishLootIfEmpty(Creature c) {
        if (c.lootItems.isEmpty() && c.lootGold == 0) {
            c.lootable = false;
        }
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

    /** UNIT_DYNFLAG_LOOTABLE (UnitDynFlags). */
    public static final int UNIT_DYNFLAG_LOOTABLE = 0x0001;

    /**
     * Object::BuildValuesUpdate for UNIT_DYNAMIC_FLAGS: a dead creature shows LOOTABLE only to a viewer
     * whose loot it is (m_loot->CanLoot); alive → never lootable.
     */
    public static int dynamicFlagsFor(Creature c, Player viewer) {
        int flags = c.getInt(UpdateFields.UNIT_DYNAMIC_FLAGS) & ~UNIT_DYNFLAG_LOOTABLE;
        if (!c.alive() && canLoot(viewer, c)) {
            flags |= UNIT_DYNFLAG_LOOTABLE;
        }
        return flags;
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
