package org.tbc.world.spell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.common.Codes;
import org.tbc.common.WowBuffer;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.map.GameMap;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.pvp.Honor;
import org.tbc.world.script.ClassScripts;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.DoubleSupplier;

/** CMSG_CAST_SPELL + SMSG_CAST_RESULT 0x130. SPELL_CAST_OK 0xFF is never sent. */
public final class SpellEngine {
    private static final Logger log = LoggerFactory.getLogger(SpellEngine.class);

    public static final int SPELL_FAILED_BAD_TARGETS = 0x0B;
    public static final int SPELL_FAILED_NOT_KNOWN = 0x3B;
    public static final int SPELL_FAILED_NO_POWER = 0x50;
    public static final int SPELL_FAILED_OUT_OF_RANGE = 0x5C;
    public static final int SPELL_CAST_OK = 0xFF;
    public static final int EFFECT_SCHOOL_DAMAGE = 2;
    public static final int EFFECT_HEAL = 10;
    public static final int EFFECT_APPLY_AURA = 6;
    public static final int EFFECT_WEAPON_DAMAGE = 58;
    public static final int EFFECT_ENERGIZE = 30;
    public static final int EFFECT_ADD_HONOR = 45;
    public static final int EFFECT_LEARN_SPELL = 36;
    public static final int EFFECT_CREATE_ITEM = 24;
    public static final int EFFECT_OPEN_LOCK = 33;
    public static final int EFFECT_TRIGGER_SPELL = 64;
    public static final int EFFECT_DUMMY = 3;
    public static final int EFFECT_SCRIPT = 77;
    public static final int CAST_FLAG_UNKNOWN2 = 0x2;
    public static final int CAST_FLAG_UNKNOWN9 = 0x100;
    public static final int FIREBALL = 133;
    public static final int LOGINEFFECT = 836;
    public static final int SPELL_MISS_MISS = 1;
    private static final double MAGIC_MISS = 0.04;

    private static final Set<Integer> KNOWN_EFFECTS = Set.of(
            EFFECT_SCHOOL_DAMAGE, EFFECT_HEAL, EFFECT_APPLY_AURA, EFFECT_WEAPON_DAMAGE,
            EFFECT_ENERGIZE, EFFECT_ADD_HONOR, EFFECT_LEARN_SPELL, EFFECT_CREATE_ITEM, EFFECT_OPEN_LOCK,
            EFFECT_TRIGGER_SPELL, EFFECT_DUMMY, EFFECT_SCRIPT);

    public record SpellInfo(int id, int effect, int aura, int school, int mana, int minDmg, int maxDmg, float maxRange, int misc) {
        public SpellInfo(int id, int effect, int aura, int school, int mana, int minDmg, int maxDmg, float maxRange) {
            this(id, effect, aura, school, mana, minDmg, maxDmg, maxRange, 0);
        }
    }

    private final Map<Integer, SpellInfo> spells = new HashMap<>();
    private final DoubleSupplier missRoll;

    public SpellEngine() {
        this(() -> ThreadLocalRandom.current().nextDouble());
    }

    public SpellEngine(DoubleSupplier missRoll) {
        this.missRoll = missRoll;
        spells.put(78, new SpellInfo(78, EFFECT_WEAPON_DAMAGE, 0, 0, 150, 1, 3, 5f));
        spells.put(FIREBALL, new SpellInfo(FIREBALL, EFFECT_SCHOOL_DAMAGE, 0, 4, 30, 8, 12, 30f));
        spells.put(2050, new SpellInfo(2050, EFFECT_HEAL, 0, 1, 20, 10, 14, 0f));
        spells.put(ClassScripts.SPELL_EXECUTE, new SpellInfo(ClassScripts.SPELL_EXECUTE, EFFECT_DUMMY, 0, 0, 0, 0, 0, 5f));
        spells.put(30108, new SpellInfo(30108, EFFECT_APPLY_AURA, 3, 5, 0, 0, 0, 30f));
        spells.put(36300, new SpellInfo(36300, EFFECT_APPLY_AURA, 0, 0, 0, 0, 0, 0f));
        spells.put(LOGINEFFECT, new SpellInfo(LOGINEFFECT, EFFECT_DUMMY, 0, 0, 0, 0, 0, 0f));
    }

    public static SpellEngine alwaysHit() {
        return new SpellEngine(() -> 1.0);
    }

    public SpellInfo info(int id) {
        return spells.get(id);
    }

    public void catalogDummy(int effectId) {
        log.debug("spell effect {} dummy/script has no plugin", effectId);
    }

    public boolean knownEffect(int effect) {
        return KNOWN_EFFECTS.contains(effect);
    }

    public void sendFail(BiConsumer<Integer, byte[]> send, int spellId, int result, int castCount) {
        if (result == SPELL_CAST_OK) {
            return;
        }
        WowBuffer b = new WowBuffer(16);
        b.putU32(spellId);
        b.putU8(result);
        b.putU8(castCount);
        send.accept(Opcodes.SMSG_CAST_RESULT, b.array());
    }

    public static int opcodeCastResult() {
        return Codes.SMSG_CAST_RESULT;
    }

    public boolean cast(Player caster, GameMap map, long nowMs, int spellId, int castCount, WowBuffer rest,
                     BiConsumer<Integer, byte[]> send) {
        if (spellId == 0) {
            return false;
        }
        SpellInfo sp = info(spellId);
        if (sp == null) {
            return false;
        }
        if (!caster.spells.contains(spellId) && spellId != LOGINEFFECT) {
            sendFail(send, spellId, SPELL_FAILED_NOT_KNOWN, castCount);
            return false;
        }
        SpellCastTargets targets = SpellCastTargets.read(rest);
        Unit target = resolve(caster, map, targets.unitGuid);
        if (target == null) {
            sendFail(send, spellId, SPELL_FAILED_BAD_TARGETS, castCount);
            return false;
        }
        if (outOfRange(caster, target, sp)) {
            sendFail(send, spellId, SPELL_FAILED_OUT_OF_RANGE, castCount);
            return false;
        }
        if (sp.mana > 0 && caster.power() < sp.mana) {
            sendFail(send, spellId, SPELL_FAILED_NO_POWER, castCount);
            return false;
        }
        send.accept(Opcodes.SMSG_SPELL_START, encodeStart(caster.guid, sp.id, castCount, targets));
        if (sp.mana > 0) {
            caster.setPower(caster.power() - sp.mana);
            var pwr = UpdateBuilder.maybeCompress(
                    UpdateBuilder.values(caster, UpdateFields.UNIT_FIELD_POWER1 + caster.powerType));
            send.accept(pwr.opcode(), pwr.payload());
        }
        int dmg = 0;
        if (sp.id == 78) {
            caster.queueNextMeleeSwing(Math.max(1, (sp.minDmg + sp.maxDmg) / 2));
        } else {
            dmg = apply(caster, target, sp);
        }
        boolean schoolMiss = sp.effect == EFFECT_SCHOOL_DAMAGE && dmg == 0;
        if (schoolMiss) {
            send.accept(Opcodes.SMSG_SPELL_GO, encodeGo(caster.guid, target.guid, sp.id, nowMs, targets, SPELL_MISS_MISS));
            send.accept(Opcodes.SMSG_SPELLLOGMISS, encodeSpellLogMiss(sp.id, caster.guid, target.guid));
        } else {
            send.accept(Opcodes.SMSG_SPELL_GO, encodeGo(caster.guid, target.guid, sp.id, nowMs, targets));
            if (dmg > 0) {
                send.accept(Opcodes.SMSG_SPELLNONMELEEDAMAGELOG, encodeDamageLog(target.guid, caster.guid, sp, dmg));
                var hp = UpdateBuilder.maybeCompress(UpdateBuilder.values(target, UpdateFields.UNIT_FIELD_HEALTH));
                send.accept(hp.opcode(), hp.payload());
            }
        }
        return true;
    }

    public int apply(Unit caster, Unit target, SpellInfo sp) {
        if (sp == null || target == null) {
            return 0;
        }
        if (sp.effect == EFFECT_SCHOOL_DAMAGE && missRoll.getAsDouble() < MAGIC_MISS) {
            return 0;
        }
        if (sp.effect == EFFECT_SCHOOL_DAMAGE || sp.effect == EFFECT_WEAPON_DAMAGE) {
            int dmg = Math.max(1, (sp.minDmg + sp.maxDmg) / 2);
            target.setHealth(target.health() - dmg);
            return dmg;
        }
        if (sp.effect == EFFECT_HEAL) {
            int heal = Math.max(1, (sp.minDmg + sp.maxDmg) / 2);
            if (caster instanceof Player player
                    && missRoll.getAsDouble() < player.getFloat(UpdateFields.PLAYER_SPELL_CRIT_PERCENTAGE1) / 100.0) {
                heal += heal / 2;
            }
            target.setHealth(target.health() + heal);
            return 0;
        }
        if (sp.effect == EFFECT_APPLY_AURA) {
            target.auras.add(new Unit.Aura(sp.id, 30_000, 1));
            return 0;
        }
        if (sp.effect == EFFECT_ENERGIZE) {
            energize(target, Math.max(1, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_ADD_HONOR) {
            addHonor(target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_LEARN_SPELL) {
            learnSpell(target, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_CREATE_ITEM) {
            int count = Math.max(0, (sp.minDmg + sp.maxDmg) / 2);
            long guid = target instanceof Player p ? p.items.size() + 1L : 0;
            createItem(target, sp.misc(), count, guid);
            return 0;
        }
        if (sp.effect == EFFECT_TRIGGER_SPELL) {
            SpellInfo nested = info(sp.misc());
            if (nested == null) {
                return 0;
            }
            return apply(caster, target, nested);
        }
        if (sp.effect == EFFECT_DUMMY || sp.effect == EFFECT_SCRIPT) {
            catalogDummy(sp.effect);
            if (sp.id == ClassScripts.SPELL_EXECUTE) {
                ClassScripts.warriorExecute(100);
            }
        }
        return 0;
    }

    /** Effect 30 — restore power (spell-algorithms.md). */
    public void energize(Unit target, int amount) {
        if (target == null || amount <= 0) {
            return;
        }
        target.setPower(target.power() + amount);
    }

    /** Effect 24 — add item id × count from damage; SMSG_ITEM_PUSH_RESULT created=1. */
    public Item createItem(Unit target, int itemId, int count, long itemGuid) {
        return createItem(target, itemId, count, itemGuid, null);
    }

    public Item createItem(Unit target, int itemId, int count, long itemGuid, BiConsumer<Integer, byte[]> send) {
        if (!(target instanceof Player p) || itemId <= 0 || count <= 0 || itemGuid == 0) {
            return null;
        }
        int slot = p.firstFreeBagSlot();
        if (slot < 0) {
            return null;
        }
        Item it = new Item(itemGuid, itemId);
        it.ownerGuid = Guid.low(p.guid);
        it.bag = 0;
        it.slot = slot;
        it.count = count;
        p.items.put(Guid.low(it.guid), it);
        p.dirty = true;
        if (send != null) {
            send.accept(Opcodes.SMSG_ITEM_PUSH_RESULT, encodeCreateItemPush(p, it, count));
        }
        return it;
    }

    byte[] encodeCreateItemPush(Player p, Item it, int count) {
        WowBuffer b = new WowBuffer(48);
        b.putU64(p.guid);
        b.putU32(0);
        b.putU32(1);
        b.putU32(1);
        b.putU8(it.bag);
        b.putU32(it.slot);
        b.putU32(it.entry);
        b.putU32(0);
        b.putU32(0);
        b.putU32(count);
        b.putU32(count);
        return b.array();
    }

    /** Effect 36 — add TriggerSpell / misc to the player's book (spells-and-auras.md). */
    public void learnSpell(Unit target, int spellId) {
        if (!(target instanceof Player p) || spellId <= 0) {
            return;
        }
        if (!p.spells.contains(spellId)) {
            p.spells.add(spellId);
        }
    }

    /** CMSG_CANCEL_AURA — unapply auras of this spell id (spell.md). */
    public void cancelAura(Unit target, int spellId) {
        if (target == null || spellId <= 0) {
            return;
        }
        target.auras.removeIf(a -> a.spellId() == spellId);
    }

    public static final int SPELL_AURA_PERIODIC_DAMAGE = 3;

    /** Amplitude tick: PERIODIC_DAMAGE → SMSG_PERIODICAURALOG; damage = health delta. combat-log.md */
    public void tickPeriodic(Unit caster, Unit target, SpellInfo sp, BiConsumer<Integer, byte[]> send) {
        if (caster == null || target == null || sp == null || send == null) {
            return;
        }
        if (sp.aura != SPELL_AURA_PERIODIC_DAMAGE) {
            return;
        }
        int dmg = (sp.minDmg + sp.maxDmg) / 2;
        int before = target.health();
        target.setHealth(before - dmg);
        int dealt = before - target.health();
        send.accept(Opcodes.SMSG_PERIODICAURALOG, encodePeriodicDamageLog(target.guid, caster.guid, sp, dealt));
    }

    byte[] encodePeriodicDamageLog(long target, long caster, SpellInfo sp, int damage) {
        WowBuffer b = new WowBuffer(64);
        b.putPackedGuid(target);
        b.putPackedGuid(caster);
        b.putU32(sp.id);
        b.putU32(1);
        b.putU32(SPELL_AURA_PERIODIC_DAMAGE);
        b.putU32(damage);
        b.putU32(sp.school);
        b.putU32(0);
        b.putU32(0);
        return b.array();
    }

    /** Effect 45 — add honor points from damage (spell-algorithms.md). */
    public void addHonor(Unit target, int amount) {
        if (!(target instanceof Player p) || amount <= 0) {
            return;
        }
        Honor.reward(p, null, 0, amount);
    }

    static boolean outOfRange(Unit caster, Unit target, SpellInfo sp) {
        return target != caster && sp.maxRange > 0 && caster.distance2d(target) > sp.maxRange;
    }

    public static Unit resolve(Player caster, GameMap map, long guid) {
        if (guid == 0 || guid == caster.guid) {
            return caster;
        }
        Creature c = map.creatures.get(guid);
        if (c != null) {
            return c;
        }
        return map.players.get(guid);
    }

    public byte[] encodeStart(long caster, int spellId, int castCount, SpellCastTargets targets) {
        WowBuffer b = new WowBuffer(64);
        b.putPackedGuid(caster);
        b.putPackedGuid(caster);
        b.putU32(spellId);
        b.putU8(castCount);
        b.putU16(CAST_FLAG_UNKNOWN2);
        b.putU32(0);
        targets.write(b);
        return b.array();
    }

    public byte[] encodeGo(long caster, long hit, int spellId, long nowMs, SpellCastTargets targets) {
        return encodeGo(caster, hit, spellId, nowMs, targets, 0);
    }

    public byte[] encodeGo(long caster, long target, int spellId, long nowMs, SpellCastTargets targets, int missInfo) {
        WowBuffer b = new WowBuffer(80);
        b.putPackedGuid(caster);
        b.putPackedGuid(caster);
        b.putU32(spellId);
        b.putU16(CAST_FLAG_UNKNOWN9);
        b.putU32((int) nowMs);
        if (missInfo == 0) {
            b.putU8(1);
            b.putU64(target);
            b.putU8(0);
        } else {
            b.putU8(0);
            b.putU8(1);
            b.putU64(target);
            b.putU8(missInfo);
        }
        targets.write(b);
        return b.array();
    }

    byte[] encodeSpellLogMiss(int spellId, long caster, long target) {
        WowBuffer b = new WowBuffer(32);
        b.putU32(spellId);
        b.putU64(caster);
        b.putU8(0);
        b.putU32(1);
        b.putU64(target);
        b.putU8(SPELL_MISS_MISS);
        return b.array();
    }

    public byte[] encodeDamageLog(long target, long attacker, SpellInfo sp, int damage) {
        WowBuffer b = new WowBuffer(64);
        b.putPackedGuid(target);
        b.putPackedGuid(attacker);
        b.putU32(sp.id);
        b.putU32(damage);
        b.putU8(sp.school);
        b.putU32(0);
        b.putU32(0);
        b.putU8(0);
        b.putU8(0);
        b.putU32(0);
        b.putU32(0);
        b.putU8(0);
        return b.array();
    }
}
