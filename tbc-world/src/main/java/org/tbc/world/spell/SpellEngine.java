package org.tbc.world.spell;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tbc.common.Codes;
import org.tbc.common.WowBuffer;
import org.tbc.world.content.Content;
import org.tbc.world.entity.Corpse;
import org.tbc.world.entity.Creature;
import org.tbc.world.entity.DynamicObject;
import org.tbc.world.entity.GameObject;
import org.tbc.world.entity.Guid;
import org.tbc.world.entity.Item;
import org.tbc.world.entity.Pet;
import org.tbc.world.entity.Player;
import org.tbc.world.entity.Unit;
import org.tbc.world.map.GameMap;
import org.tbc.world.map.GraveyardManager;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateBuilder;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.pvp.Honor;
import org.tbc.world.script.ClassScripts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public static final int EFFECT_INSTAKILL = 1;
    public static final int EFFECT_SCHOOL_DAMAGE = 2;
    public static final int EFFECT_TELEPORT_UNITS = 5;
    public static final int EFFECT_TELEPORT_UNITS_FACE_CASTER = 43;
    public static final int EFFECT_POWER_DRAIN = 8;
    public static final int EFFECT_HEALTH_LEECH = 9;
    public static final int EFFECT_HEAL = 10;
    public static final int EFFECT_BIND = 11;
    public static final int EFFECT_QUEST_COMPLETE = 16;
    public static final int EFFECT_WEAPON_DAMAGE_NOSCHOOL = 17;
    public static final int EFFECT_WEAPON_PERCENT_DAMAGE = 31;
    public static final int EFFECT_TRIGGER_MISSILE = 32;
    public static final int EFFECT_NORMALIZED_WEAPON_DMG = 121;
    public static final int EFFECT_DISTRACT = 69;
    public static final int EFFECT_SKINNING = 95;
    public static final int EFFECT_SKIN_PLAYER_CORPSE = 116;
    public static final int EFFECT_TELEPORT_GRAVEYARD = 120;
    public static final int EFFECT_CHARGE = 96;
    public static final int EFFECT_CHARGE_DEST = 149;
    public static final int EFFECT_PARRY = 22;
    public static final int EFFECT_BLOCK = 23;
    public static final int EFFECT_SPAWN = 46;
    public static final int EFFECT_PROFICIENCY = 60;
    public static final int EFFECT_SEND_EVENT = 61;
    public static final int EFFECT_RESURRECT = 18;
    public static final int EFFECT_RESURRECT_NEW = 113;
    public static final int EFFECT_SPIRIT_HEAL = 117;
    public static final int EFFECT_HEAL_MAX_HEALTH = 67;
    public static final int EFFECT_APPLY_AURA = 6;
    public static final int EFFECT_PERSISTENT_AREA_AURA = 27;
    public static final int EFFECT_APPLY_AREA_AURA_PARTY = 35;
    public static final int EFFECT_APPLY_AREA_AURA_PET = 119;
    public static final int EFFECT_APPLY_AREA_AURA_FRIEND = 128;
    public static final int EFFECT_APPLY_AREA_AURA_ENEMY = 129;
    public static final int EFFECT_APPLY_AREA_AURA_OWNER = 143;
    public static final int EFFECT_REDIRECT_THREAT = 130;
    public static final int EFFECT_LANGUAGE = 39;
    public static final int EFFECT_DODGE = 20;
    public static final int EFFECT_TRADE_SKILL = 47;
    public static final int EFFECT_SKILL = 118;
    public static final int EFFECT_WEAPON = 25;
    public static final int EFFECT_DEFENSE = 26;
    public static final int EFFECT_EVADE = 21;
    public static final int EFFECT_CALL_PET = 135;
    public static final int EFFECT_PULL = 70;
    public static final int EFFECT_SPELL_DEFENSE = 37;
    public static final int EFFECT_BIND_SIGHT = 82;
    public static final int EFFECT_STEALTH = 48;
    public static final int EFFECT_DETECT = 49;
    public static final int EFFECT_CREATE_HOUSE = 81;
    public static final int EFFECT_ENVIRONMENTAL_DAMAGE = 7;
    public static final int EFFECT_WEAPON_DAMAGE = 58;
    public static final int EFFECT_ENERGIZE = 30;
    public static final int EFFECT_ADD_HONOR = 45;
    public static final int EFFECT_LEARN_SPELL = 36;
    public static final int EFFECT_TAME_CREATURE = 55;
    public static final int EFFECT_SUMMON_PET = 56;
    public static final int EFFECT_LEARN_PET_SPELL = 57;
    public static final int EFFECT_DISPEL = 38;
    public static final int EFFECT_DISPEL_MECHANIC = 108;
    public static final int EFFECT_SUMMON_DEAD_PET = 109;
    public static final int EFFECT_SEND_TAXI = 123;
    public static final int EFFECT_PULL_TOWARDS = 124;
    public static final int EFFECT_PULL_TOWARDS_DEST = 145;
    public static final int EFFECT_STEAL_BENEFICIAL_BUFF = 126;
    public static final int EFFECT_PROSPECTING = 127;
    public static final int EFFECT_LEAP = 29;
    public static final int EFFECT_LEAP_BACK = 138;
    public static final int EFFECT_KILL_CREDIT_GROUP = 134;
    public static final int EFFECT_PLAY_SOUND = 131;
    public static final int EFFECT_PLAY_MUSIC = 132;
    public static final int EFFECT_UNLEARN_SPECIALIZATION = 133;
    public static final int EFFECT_ADD_EXTRA_ATTACKS = 19;
    public static final int EFFECT_CREATE_ITEM = 24;
    public static final int EFFECT_DUAL_WIELD = 40;
    public static final int EFFECT_SKILL_STEP = 44;
    public static final int EFFECT_OPEN_LOCK = 33;
    public static final int EFFECT_SUMMON_CHANGE_ITEM = 34;
    public static final int EFFECT_OPEN_LOCK_ITEM = 59;
    public static final int EFFECT_ENCHANT_ITEM = 53;
    public static final int EFFECT_ENCHANT_ITEM_TEMPORARY = 54;
    public static final int EFFECT_ENCHANT_HELD_ITEM = 92;
    public static final int EFFECT_CREATE_PET = 153;
    public static final int EFFECT_TRIGGER_SPELL = 64;
    public static final int EFFECT_TRIGGER_SPELL_2 = 151;
    public static final int EFFECT_SUMMON_RAF_FRIEND = 152;
    public static final int EFFECT_FORCE_CAST = 140;
    public static final int EFFECT_FORCE_CAST_WITH_VALUE = 141;
    public static final int EFFECT_TRIGGER_SPELL_WITH_VALUE = 142;
    public static final int EFFECT_POWER_BURN = 62;
    public static final int EFFECT_THREAT = 63;
    public static final int EFFECT_INTERRUPT_CAST = 68;
    public static final int EFFECT_PICKPOCKET = 71;
    public static final int EFFECT_ADD_FARSIGHT = 72;
    public static final int EFFECT_HEAL_MECHANICAL = 75;
    public static final int EFFECT_ADD_COMBO_POINTS = 80;
    public static final int EFFECT_SANCTUARY = 79;
    public static final int EFFECT_DUEL = 83;
    public static final int EFFECT_STUCK = 84;
    public static final int EFFECT_SUMMON_PLAYER = 85;
    public static final int EFFECT_ACTIVATE_OBJECT = 86;
    /** CMaNGOS MAX_PLAYER_SUMMON_DELAY (2*MINUTE) in milliseconds. */
    public static final int MAX_PLAYER_SUMMON_DELAY_MS = 120_000;
    public static final int EFFECT_INEBRIATE = 100;
    public static final int EFFECT_DISENCHANT = 99;
    public static final int EFFECT_FEED_PET = 101;
    public static final int EFFECT_DISMISS_PET = 102;
    public static final int EFFECT_REPUTATION = 103;
    public static final int EFFECT_SUMMON_OBJECT_SLOT1 = 104;
    public static final int EFFECT_SUMMON_OBJECT_SLOT2 = 105;
    public static final int EFFECT_KNOCK_BACK = 98;
    public static final int EFFECT_KNOCKBACK_FROM_POSITION = 144;
    public static final int EFFECT_DESTROY_ALL_TOTEMS = 110;
    public static final int EFFECT_DURABILITY_DAMAGE = 111;
    public static final int EFFECT_DURABILITY_DAMAGE_PCT = 115;
    public static final int EFFECT_ATTACK_ME = 114;
    public static final int EFFECT_MODIFY_THREAT_PERCENT = 125;
    public static final int EFFECT_HEAL_PCT = 136;
    public static final int EFFECT_ENERGIZE_PCT = 137;
    public static final int EFFECT_QUEST_FAIL = 147;
    public static final int EFFECT_DUMMY = 3;
    public static final int EFFECT_TRANS_DOOR = 50;
    public static final int EFFECT_SUMMON = 28;
    public static final int EFFECT_SUMMON_OBJECT_WILD = 76;
    public static final int EFFECT_SCRIPT = 77;
    public static final int EFFECT_SELF_RESURRECT = 94;
    public static final int CAST_FLAG_UNKNOWN2 = 0x2;
    public static final int CAST_FLAG_UNKNOWN9 = 0x100;
    public static final int FIREBALL = 133;
    public static final int LOGINEFFECT = 836;
    public static final int SPELL_MISS_MISS = 1;
    private static final double MAGIC_MISS = 0.04;

    private static final Set<Integer> KNOWN_EFFECTS = Set.of(
            EFFECT_SCHOOL_DAMAGE, EFFECT_TELEPORT_UNITS, EFFECT_TELEPORT_UNITS_FACE_CASTER, EFFECT_HEAL, EFFECT_HEAL_MAX_HEALTH, EFFECT_APPLY_AURA, EFFECT_APPLY_AREA_AURA_PARTY, EFFECT_APPLY_AREA_AURA_FRIEND, EFFECT_APPLY_AREA_AURA_ENEMY, EFFECT_APPLY_AREA_AURA_PET, EFFECT_APPLY_AREA_AURA_OWNER, EFFECT_WEAPON_DAMAGE,
            EFFECT_ENERGIZE, EFFECT_ADD_HONOR, EFFECT_LEARN_SPELL, EFFECT_LEARN_PET_SPELL, EFFECT_CREATE_ITEM, EFFECT_OPEN_LOCK, EFFECT_OPEN_LOCK_ITEM,
            EFFECT_ENCHANT_HELD_ITEM, EFFECT_ENCHANT_ITEM, EFFECT_ENCHANT_ITEM_TEMPORARY, EFFECT_CREATE_PET, EFFECT_TAME_CREATURE, EFFECT_SUMMON_PET, EFFECT_SUMMON_CHANGE_ITEM,
            EFFECT_TRIGGER_SPELL, EFFECT_TRIGGER_SPELL_2, EFFECT_SUMMON_RAF_FRIEND, EFFECT_TRIGGER_MISSILE, EFFECT_FORCE_CAST, EFFECT_FORCE_CAST_WITH_VALUE, EFFECT_TRIGGER_SPELL_WITH_VALUE, EFFECT_ADD_FARSIGHT, EFFECT_PICKPOCKET, EFFECT_DUMMY, EFFECT_SCRIPT, EFFECT_INSTAKILL,
            EFFECT_HEALTH_LEECH, EFFECT_POWER_DRAIN, EFFECT_ADD_COMBO_POINTS, EFFECT_INTERRUPT_CAST,
            EFFECT_SANCTUARY, EFFECT_DUEL, EFFECT_STUCK, EFFECT_SUMMON_PLAYER, EFFECT_ACTIVATE_OBJECT, EFFECT_ADD_EXTRA_ATTACKS, EFFECT_BIND, EFFECT_ATTACK_ME, EFFECT_QUEST_COMPLETE,
            EFFECT_RESURRECT, EFFECT_RESURRECT_NEW, EFFECT_SPIRIT_HEAL, EFFECT_ENVIRONMENTAL_DAMAGE, EFFECT_WEAPON_DAMAGE_NOSCHOOL, EFFECT_DISPEL,
            EFFECT_POWER_BURN, EFFECT_THREAT, EFFECT_HEAL_PCT, EFFECT_ENERGIZE_PCT, EFFECT_DISENCHANT, EFFECT_INEBRIATE, EFFECT_FEED_PET,
            EFFECT_QUEST_FAIL, EFFECT_SELF_RESURRECT, EFFECT_HEAL_MECHANICAL, EFFECT_DESTROY_ALL_TOTEMS,
            EFFECT_DURABILITY_DAMAGE, EFFECT_KNOCK_BACK, EFFECT_KNOCKBACK_FROM_POSITION, EFFECT_MODIFY_THREAT_PERCENT, EFFECT_REPUTATION, EFFECT_SUMMON_OBJECT_SLOT1,
            EFFECT_SUMMON_OBJECT_SLOT2, EFFECT_SUMMON_OBJECT_WILD, EFFECT_TRANS_DOOR, EFFECT_SUMMON,
            EFFECT_PERSISTENT_AREA_AURA, EFFECT_REDIRECT_THREAT,
            EFFECT_LANGUAGE, EFFECT_DODGE, EFFECT_TRADE_SKILL, EFFECT_SKILL, EFFECT_WEAPON, EFFECT_DEFENSE,
            EFFECT_EVADE, EFFECT_CALL_PET, EFFECT_PULL, EFFECT_SPELL_DEFENSE, EFFECT_BIND_SIGHT, EFFECT_STEALTH,
            EFFECT_DETECT, EFFECT_CREATE_HOUSE,
            EFFECT_DURABILITY_DAMAGE_PCT, EFFECT_DUAL_WIELD, EFFECT_SKILL_STEP, EFFECT_PARRY, EFFECT_BLOCK,
            EFFECT_SPAWN, EFFECT_PROFICIENCY, EFFECT_SEND_EVENT, EFFECT_WEAPON_PERCENT_DAMAGE, EFFECT_DISTRACT,
            EFFECT_DISPEL_MECHANIC, EFFECT_SUMMON_DEAD_PET, EFFECT_SEND_TAXI, EFFECT_KILL_CREDIT_GROUP, EFFECT_SKINNING, EFFECT_SKIN_PLAYER_CORPSE, EFFECT_TELEPORT_GRAVEYARD, EFFECT_CHARGE, EFFECT_CHARGE_DEST,
            EFFECT_DISMISS_PET, EFFECT_PLAY_MUSIC, EFFECT_PLAY_SOUND, EFFECT_PULL_TOWARDS, EFFECT_PULL_TOWARDS_DEST, EFFECT_LEAP_BACK,
            EFFECT_NORMALIZED_WEAPON_DMG, EFFECT_STEAL_BENEFICIAL_BUFF, EFFECT_PROSPECTING, EFFECT_UNLEARN_SPECIALIZATION,
            EFFECT_LEAP);

    /**
     * CMaNGOS EffectEmpty / EffectNULL / commented-out bodies — marker or client inform; no server mutation.
     * TRADE_SKILL / SKILL / WEAPON / DEFENSE: the skill line owns the value, not these effects.
     * CALL_PET 23498 is NULL; the hunter's Call Pet 883 is SUMMON_PET.
     * BIND_SIGHT (Gnome Car Camera 6955) is NULL; farsight is ADD_FARSIGHT 72.
     * PULL (Distract Move 15051) is DEBUG_LOG only in CMaNGOS (TODO pull toward distract center).
     * STEALTH (Base Stealth 2426) / DETECT (Detect 3050) are EffectUnused; the MOD_STEALTH /
     * MOD_STEALTH_DETECT auras own that state. CREATE_HOUSE (Create House (TEST) 6757) is EffectUnused.
     */
    private static final Set<Integer> NO_OP_EFFECTS = Set.of(
            EFFECT_LANGUAGE, EFFECT_DODGE, EFFECT_TRADE_SKILL, EFFECT_SKILL, EFFECT_WEAPON, EFFECT_DEFENSE,
            EFFECT_EVADE, EFFECT_CALL_PET, EFFECT_PULL, EFFECT_SPELL_DEFENSE, EFFECT_BIND_SIGHT, EFFECT_STEALTH,
            EFFECT_DETECT, EFFECT_CREATE_HOUSE);

    private static final Set<Integer> APPLY_AREA_AURA_EFFECTS = Set.of(
            EFFECT_APPLY_AREA_AURA_PARTY, EFFECT_APPLY_AREA_AURA_FRIEND, EFFECT_APPLY_AREA_AURA_ENEMY,
            EFFECT_APPLY_AREA_AURA_PET, EFFECT_APPLY_AREA_AURA_OWNER);

    public record SpellInfo(int id, int effect, int aura, int school, int mana, int minDmg, int maxDmg, float maxRange,
                            int misc, int equippedItemClass, int castTimeMs) {
        public SpellInfo(int id, int effect, int aura, int school, int mana, int minDmg, int maxDmg, float maxRange,
                         int misc, int equippedItemClass) {
            this(id, effect, aura, school, mana, minDmg, maxDmg, maxRange, misc, equippedItemClass, 0);
        }

        public SpellInfo(int id, int effect, int aura, int school, int mana, int minDmg, int maxDmg, float maxRange, int misc) {
            this(id, effect, aura, school, mana, minDmg, maxDmg, maxRange, misc, 0);
        }

        public SpellInfo(int id, int effect, int aura, int school, int mana, int minDmg, int maxDmg, float maxRange) {
            this(id, effect, aura, school, mana, minDmg, maxDmg, maxRange, 0);
        }

        /** Spell.dbc CastingTimeIndex → SpellCastTimes.dbc base (ms). */
        public SpellInfo withCastTime(int ms) {
            return new SpellInfo(id, effect, aura, school, mana, minDmg, maxDmg, maxRange, misc, equippedItemClass, ms);
        }
    }

    /** Spell::m_currentSpells[CURRENT_GENERIC_SPELL] while SPELL_STATE_PREPARING (cast bar running). */
    private record PendingCast(Player caster, Unit target, SpellInfo sp, int castCount, SpellCastTargets targets,
                               BiConsumer<Integer, byte[]> send, Runnable onFinished, int[] timerMs) {}

    private final Map<Long, PendingCast> pendingCasts = new HashMap<>();
    /** Fireball 133 / Lesser Heal 2050 rank 1: CastingTimeIndex 16 = 1500 ms. */
    private static final int CAST_TIME_INDEX_16_MS = 1500;

    private final Map<Integer, SpellInfo> spells = new HashMap<>();
    private final DoubleSupplier missRoll;
    private final AuraEngine auras = new AuraEngine();

    public SpellEngine() {
        this(() -> ThreadLocalRandom.current().nextDouble());
    }

    public SpellEngine(DoubleSupplier missRoll) {
        this.missRoll = missRoll;
        spells.put(78, new SpellInfo(78, EFFECT_WEAPON_DAMAGE, 0, 0, 150, 1, 3, 5f));
        spells.put(FIREBALL, new SpellInfo(FIREBALL, EFFECT_SCHOOL_DAMAGE, 0, 4, 30, 8, 12, 30f)
                .withCastTime(CAST_TIME_INDEX_16_MS));
        spells.put(2050, new SpellInfo(2050, EFFECT_HEAL, 0, 1, 20, 10, 14, 0f).withCastTime(CAST_TIME_INDEX_16_MS));
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

    /** SPELL_AURA_* modifier catalog applied by EFFECT_APPLY_AURA. */
    public AuraEngine auras() {
        return auras;
    }

    public void catalogDummy(int effectId) {
        log.debug("spell effect {} dummy/script has no plugin", effectId);
    }

    public boolean knownEffect(int effect) {
        return KNOWN_EFFECTS.contains(effect);
    }

    /**
     * Effect 140 — SPELL_EFFECT_FORCE_CAST. CMaNGOS unitTarget casts EffectTriggerSpell.
     * Portal Effect: Ironforge 17607 SQL trigger is 44089 (dest later). Nested uses catalog.
     */
    public int forceCast(Unit target, int triggerSpellId) {
        if (target == null) {
            return 0;
        }
        SpellInfo nested = info(triggerSpellId);
        if (nested == null) {
            return 0;
        }
        return apply(target, target, nested);
    }

    /**
     * Effect 141 — SPELL_EFFECT_FORCE_CAST_WITH_VALUE. CMaNGOS CastCustomSpell basePoints = damage.
     * Bloodbolt 41065 SQL trigger is 41067 (later). Nested uses catalog. Fireball 133 vehicle.
     */
    public int forceCastWithValue(Unit target, int triggerSpellId, int value) {
        if (target == null) {
            return 0;
        }
        SpellInfo nested = info(triggerSpellId);
        if (nested == null) {
            return 0;
        }
        if (value > 0) {
            return apply(target, target, new SpellInfo(
                    nested.id(), nested.effect(), nested.aura(), nested.school(), nested.mana(),
                    value, value, nested.maxRange(), nested.misc(), nested.equippedItemClass()));
        }
        return apply(target, target, nested);
    }

    /**
     * Effect 142 — SPELL_EFFECT_TRIGGER_SPELL_WITH_VALUE. CMaNGOS caster CastCustomSpell bp = damage.
     * Prayer of Mending 33076 SQL trigger is 41635 (later). Nested uses catalog. Fireball 133 vehicle.
     */
    public int triggerSpellWithValue(Unit caster, Unit target, int triggerSpellId, int value) {
        if (caster == null) {
            return 0;
        }
        SpellInfo nested = info(triggerSpellId);
        if (nested == null) {
            return 0;
        }
        if (value > 0) {
            return apply(caster, target, new SpellInfo(
                    nested.id(), nested.effect(), nested.aura(), nested.school(), nested.mana(),
                    value, value, nested.maxRange(), nested.misc(), nested.equippedItemClass()));
        }
        return apply(caster, target, nested);
    }

    /**
     * Effect 151 — SPELL_EFFECT_TRIGGER_SPELL_2. CMaNGOS caster CastSpell(unitTarget, trigger).
     * Ritual of Summoning 698 SQL trigger is 46546 (SUMMON later). Nested uses catalog.
     */
    public int triggerRitualOfSummoning(Unit caster, Unit target, int triggerSpellId) {
        if (caster == null) {
            return 0;
        }
        SpellInfo nested = info(triggerSpellId);
        if (nested == null) {
            return 0;
        }
        return apply(caster, target, nested);
    }

    /**
     * Effect 152 — SPELL_EFFECT_SUMMON_RAF_FRIEND. CMaNGOS caster CastSpell on recruiting friend.
     * Summon Friend 45927 SQL trigger is 48955 (later). Nested uses catalog. Fireball 133 vehicle.
     */
    public int summonRafFriend(Unit caster, int triggerSpellId) {
        if (!(caster instanceof Player p)) {
            return 0;
        }
        Player friend = p.recruitingFriend();
        if (friend == null) {
            return 0;
        }
        SpellInfo nested = info(triggerSpellId);
        if (nested == null) {
            return 0;
        }
        return apply(p, friend, nested);
    }

    /**
     * Effect 32 — SPELL_EFFECT_TRIGGER_MISSILE. CMaNGOS caster CastSpell at dest/unit.
     * Arcane Orb 34172 SQL trigger is 34190 (later). Nested uses catalog. Fireball 133 vehicle.
     */
    public int triggerMissile(Unit caster, Unit target, int triggerSpellId) {
        if (caster == null) {
            return 0;
        }
        SpellInfo nested = info(triggerSpellId);
        if (nested == null) {
            return 0;
        }
        return apply(caster, target, nested);
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
        return cast(caster, map, nowMs, spellId, castCount, rest, send, () -> { });
    }

    /**
     * Spell::prepare: CheckCast, SMSG_SPELL_START with the cast timer; instant spells run cast() now,
     * timed spells wait in {@link #update}. {@code onFinished} runs right after the effects land (either path).
     * Returns true when the cast was accepted (START sent).
     */
    public boolean cast(Player caster, GameMap map, long nowMs, int spellId, int castCount, WowBuffer rest,
                     BiConsumer<Integer, byte[]> send, Runnable onFinished) {
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
        send.accept(Opcodes.SMSG_SPELL_START, encodeStart(caster.guid, sp.id, castCount, sp.castTimeMs, targets));
        if (sp.castTimeMs > 0) {
            pendingCasts.put(caster.guid, new PendingCast(caster, target, sp, castCount, targets, send, onFinished,
                    new int[]{sp.castTimeMs}));
            return true;
        }
        finishCast(caster, target, sp, castCount, targets, nowMs, send);
        onFinished.run();
        return true;
    }

    /** Spell::update while SPELL_STATE_PREPARING: count the timer down, then cast(). */
    public void update(int diff, long nowMs) {
        var it = pendingCasts.values().iterator();
        List<PendingCast> due = new ArrayList<>();
        while (it.hasNext()) {
            PendingCast pc = it.next();
            pc.timerMs[0] -= diff;
            if (pc.timerMs[0] <= 0) {
                it.remove();
                due.add(pc);
            }
        }
        for (PendingCast pc : due) {
            if (pc.caster.power() < pc.sp.mana) {
                sendFail(pc.send, pc.sp.id, SPELL_FAILED_NO_POWER, pc.castCount);
                continue;
            }
            finishCast(pc.caster, pc.target, pc.sp, pc.castCount, pc.targets, nowMs, pc.send);
            pc.onFinished.run();
        }
    }

    /** Spell::cast: TakePower, effects, SMSG_SPELL_GO (+ miss / damage log). */
    private void finishCast(Player caster, Unit target, SpellInfo sp, int castCount, SpellCastTargets targets,
                            long nowMs, BiConsumer<Integer, byte[]> send) {
        if (sp.mana > 0) {
            caster.setPower(caster.power() - sp.mana);
            caster.noteManaUse();
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
    }

    public int apply(Unit caster, Unit target, SpellInfo sp) {
        if (sp == null || target == null) {
            return 0;
        }
        if (NO_OP_EFFECTS.contains(sp.effect)) {
            return 0;
        }
        if (sp.effect == EFFECT_INSTAKILL) {
            instakill(target);
            return 0;
        }
        if (sp.effect == EFFECT_TELEPORT_UNITS) {
            if (sp.id == 8690 && target instanceof Player p) {
                teleportUnits(p, p.bindMap, p.bindX, p.bindY, p.bindZ, p.o);
            }
            return 0;
        }
        if (sp.effect == EFFECT_TELEPORT_UNITS_FACE_CASTER) {
            teleportUnitsFaceCaster(caster, target);
            return 0;
        }
        if (sp.effect == EFFECT_HEALTH_LEECH) {
            return healthLeech(caster, target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
        }
        if (sp.effect == EFFECT_POWER_DRAIN) {
            return powerDrain(caster, target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
        }
        if (sp.effect == EFFECT_ADD_COMBO_POINTS) {
            addComboPoints(caster, target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_INTERRUPT_CAST) {
            interruptCast(target);
            return 0;
        }
        if (sp.effect == EFFECT_SANCTUARY) {
            sanctuary(target);
            return 0;
        }
        if (sp.effect == EFFECT_DUEL) {
            duel(caster, target, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_STUCK) {
            stuck(caster);
            return 0;
        }
        if (sp.effect == EFFECT_SUMMON_PLAYER) {
            summonPlayer(caster, target);
            return 0;
        }
        if (sp.effect == EFFECT_ACTIVATE_OBJECT) {
            GameObject go = caster instanceof Player p ? p.spellGameObjectTarget() : null;
            activateObject(go, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_ADD_EXTRA_ATTACKS) {
            addExtraAttacks(target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_BIND) {
            bindHearth(target);
            return 0;
        }
        if (sp.effect == EFFECT_ATTACK_ME) {
            attackMe(caster, target);
            return 0;
        }
        if (sp.effect == EFFECT_QUEST_COMPLETE) {
            questComplete(target, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_RESURRECT) {
            resurrect(caster, target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_RESURRECT_NEW) {
            resurrectNew(caster, target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2), sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_SPIRIT_HEAL) {
            spiritHeal(target, sp.id);
            return 0;
        }
        if (sp.effect == EFFECT_ENVIRONMENTAL_DAMAGE) {
            return environmentalDamage(caster, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
        }
        if (sp.effect == EFFECT_DISPEL) {
            dispel(target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_POWER_BURN) {
            return powerBurn(target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2), sp.misc());
        }
        if (sp.effect == EFFECT_THREAT) {
            addThreat(caster, target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_HEAL_PCT) {
            healPct(target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_ENERGIZE_PCT) {
            energizePct(target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2), sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_INEBRIATE) {
            inebriate(target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_QUEST_FAIL) {
            questFail(target, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_SELF_RESURRECT) {
            selfResurrect(caster, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_HEAL_MECHANICAL) {
            healMechanical(target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_DESTROY_ALL_TOTEMS) {
            destroyAllTotems(caster);
            return 0;
        }
        if (sp.effect == EFFECT_DURABILITY_DAMAGE) {
            durabilityDamage(target, sp.misc(), Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_DURABILITY_DAMAGE_PCT) {
            durabilityDamagePct(target, sp.misc(), Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_DUAL_WIELD) {
            dualWield(target);
            return 0;
        }
        if (sp.effect == EFFECT_SKILL_STEP) {
            skillStep(target, sp.misc(), (sp.minDmg + sp.maxDmg) / 2);
            return 0;
        }
        if (sp.effect == EFFECT_PARRY) {
            enableParry(caster);
            return 0;
        }
        if (sp.effect == EFFECT_BLOCK) {
            enableBlock(caster);
            return 0;
        }
        if (sp.effect == EFFECT_SPAWN) {
            spawn(caster);
            return 0;
        }
        if (sp.effect == EFFECT_PROFICIENCY) {
            proficiency(caster, sp.equippedItemClass(), sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_WEAPON_PERCENT_DAMAGE) {
            return weaponPercentDamage(caster, target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
        }
        if (sp.effect == EFFECT_NORMALIZED_WEAPON_DMG) {
            return normalizedWeaponDamage(caster, target);
        }
        if (sp.effect == EFFECT_DISTRACT) {
            float destX = caster != null ? caster.x : target.x;
            float destY = caster != null ? caster.y : target.y;
            distract(target, destX, destY);
            return 0;
        }
        if (sp.effect == EFFECT_DISPEL_MECHANIC) {
            dispelMechanic(target, sp.misc(), Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_SUMMON_DEAD_PET) {
            summonDeadPet(caster);
            return 0;
        }
        if (sp.effect == EFFECT_CREATE_PET) {
            createTamedPet(target, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_TAME_CREATURE) {
            tameCreature(caster, target);
            return 0;
        }
        if (sp.effect == EFFECT_SUMMON_PET) {
            summonPet(caster, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_SEND_TAXI) {
            sendTaxi(target, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_KILL_CREDIT_GROUP) {
            killCreditGroup(target, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_CHARGE) {
            charge(caster, target);
            return 0;
        }
        if (sp.effect == EFFECT_CHARGE_DEST) {
            chargeDest(caster, sp.maxRange);
            return 0;
        }
        if (sp.effect == EFFECT_OPEN_LOCK) {
            Item item = caster instanceof Player p ? p.spellItemTarget() : null;
            openLock(caster, item);
            return 0;
        }
        if (sp.effect == EFFECT_OPEN_LOCK_ITEM) {
            Item item = caster instanceof Player p ? p.spellItemTarget() : null;
            openLock(caster, item);
            return 0;
        }
        if (sp.effect == EFFECT_SUMMON_CHANGE_ITEM) {
            Item item = caster instanceof Player p ? p.spellItemTarget() : null;
            summonChangeItem(caster, item, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_ENCHANT_HELD_ITEM) {
            enchantHeldItem(target, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_ENCHANT_ITEM) {
            Item item = caster instanceof Player p ? p.spellItemTarget() : null;
            enchantItem(caster, item, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_ENCHANT_ITEM_TEMPORARY) {
            Item item = caster instanceof Player p ? p.spellItemTarget() : null;
            enchantItemTemporary(caster, item, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_PROSPECTING) {
            Item item = caster instanceof Player p ? p.spellItemTarget() : null;
            prospecting(caster, item);
            return 0;
        }
        if (sp.effect == EFFECT_DISENCHANT) {
            Item item = caster instanceof Player p ? p.spellItemTarget() : null;
            disenchant(caster, item);
            return 0;
        }
        if (sp.effect == EFFECT_FEED_PET) {
            Item food = caster instanceof Player p ? p.spellItemTarget() : null;
            feedPet(caster, food, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_PICKPOCKET) {
            pickPocket(caster, target);
            return 0;
        }
        if (sp.effect == EFFECT_SKINNING) {
            skinning(caster, target);
            return 0;
        }
        if (sp.effect == EFFECT_SKIN_PLAYER_CORPSE) {
            skinPlayerCorpse(caster, target);
            return 0;
        }
        if (sp.effect == EFFECT_TELEPORT_GRAVEYARD) {
            teleportGraveyard(target, GraveyardManager.seeded());
            return 0;
        }
        if (sp.effect == EFFECT_DISMISS_PET) {
            dismissPet(caster);
            return 0;
        }
        if (sp.effect == EFFECT_PLAY_MUSIC) {
            playMusic(target, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_PLAY_SOUND) {
            playSound(target, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_PULL_TOWARDS) {
            pullTowards(caster, target, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_PULL_TOWARDS_DEST) {
            if (caster != null) {
                float destX = caster.x + sp.maxRange * (float) Math.cos(caster.o);
                float destY = caster.y + sp.maxRange * (float) Math.sin(caster.o);
                pullTowardsDest(target, destX, destY, caster.z, sp.misc());
            }
            return 0;
        }
        if (sp.effect == EFFECT_LEAP) {
            leapForward(target, sp.maxRange);
            return 0;
        }
        if (sp.effect == EFFECT_LEAP_BACK) {
            leapBack(caster, target, sp.misc() / 10f, (sp.minDmg + sp.maxDmg) / 2 / 10f);
            return 0;
        }
        if (sp.effect == EFFECT_STEAL_BENEFICIAL_BUFF) {
            stealBeneficialBuff(caster, target, Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
            return 0;
        }
        if (sp.effect == EFFECT_UNLEARN_SPECIALIZATION) {
            unlearnSpecialization(target, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_KNOCK_BACK) {
            knockBack(caster, target, sp.misc() / 10f, Math.max(0, (sp.minDmg + sp.maxDmg) / 2) / 10f);
            return 0;
        }
        if (sp.effect == EFFECT_KNOCKBACK_FROM_POSITION) {
            if (caster != null) {
                knockBackFromPosition(target, caster.x, caster.y,
                        sp.misc() / 10f, Math.max(0, (sp.minDmg + sp.maxDmg) / 2) / 10f);
            }
            return 0;
        }
        if (sp.effect == EFFECT_MODIFY_THREAT_PERCENT) {
            modifyThreatPercent(caster, target, (sp.minDmg + sp.maxDmg) / 2);
            return 0;
        }
        if (sp.effect == EFFECT_REPUTATION) {
            modifyReputation(target, sp.misc(), (sp.minDmg + sp.maxDmg) / 2);
            return 0;
        }
        if (sp.effect == EFFECT_SUMMON_OBJECT_SLOT1) {
            summonObjectSlot(caster, 0, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_SUMMON_OBJECT_SLOT2) {
            summonObjectSlot(caster, 1, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_SUMMON_OBJECT_WILD) {
            summonObjectWild(caster, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_TRANS_DOOR) {
            transmitted(caster, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_SUMMON) {
            summon(caster, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_PERSISTENT_AREA_AURA) {
            if (caster != null) {
                persistentAreaAura(caster, sp.id, caster.x, caster.y, caster.z, 0f);
            }
            return 0;
        }
        if (sp.effect == EFFECT_SEND_EVENT) {
            sendEvent(caster, sp.misc());
            return 0;
        }
        if (sp.effect == EFFECT_REDIRECT_THREAT) {
            redirectThreat(caster, target);
            return 0;
        }
        if (sp.effect == EFFECT_SCHOOL_DAMAGE && missRoll.getAsDouble() < MAGIC_MISS) {
            return 0;
        }
        if (sp.effect == EFFECT_SCHOOL_DAMAGE || sp.effect == EFFECT_WEAPON_DAMAGE
                || sp.effect == EFFECT_WEAPON_DAMAGE_NOSCHOOL) {
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
        if (sp.effect == EFFECT_HEAL_MAX_HEALTH) {
            target.setHealth(target.maxHealth());
            return 0;
        }
        if (sp.effect == EFFECT_APPLY_AURA) {
            target.auras.add(new Unit.Aura(sp.id, 30_000, 1));
            auras.apply(target, sp);
            return 0;
        }
        if (APPLY_AREA_AURA_EFFECTS.contains(sp.effect)) {
            applyAreaAuraParty(target, sp.id);
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
        if (sp.effect == EFFECT_LEARN_PET_SPELL) {
            learnPetSpell(caster, sp.misc());
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
        if (sp.effect == EFFECT_TRIGGER_MISSILE) {
            return triggerMissile(caster, target, sp.misc());
        }
        if (sp.effect == EFFECT_TRIGGER_SPELL_2) {
            return triggerRitualOfSummoning(caster, target, sp.misc());
        }
        if (sp.effect == EFFECT_SUMMON_RAF_FRIEND) {
            return summonRafFriend(caster, sp.misc());
        }
        if (sp.effect == EFFECT_FORCE_CAST) {
            return forceCast(target, sp.misc());
        }
        if (sp.effect == EFFECT_FORCE_CAST_WITH_VALUE) {
            return forceCastWithValue(target, sp.misc(), Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
        }
        if (sp.effect == EFFECT_TRIGGER_SPELL_WITH_VALUE) {
            return triggerSpellWithValue(caster, target, sp.misc(), Math.max(0, (sp.minDmg + sp.maxDmg) / 2));
        }
        if (sp.effect == EFFECT_ADD_FARSIGHT) {
            if (caster instanceof Player p) {
                addFarsight(p, target.guid);
            }
            return 0;
        }
        if (sp.effect == EFFECT_DUMMY || sp.effect == EFFECT_SCRIPT) {
            catalogDummy(sp.effect);
            if (sp.id == ClassScripts.SPELL_EXECUTE) {
                ClassScripts.warriorExecute(100);
            }
        }
        return 0;
    }

    /** Effect 1 — SPELL_EFFECT_INSTAKILL. CMaNGOS EffectInstaKill: skip if dead, DealDamage INSTAKILL. */
    public void instakill(Unit target) {
        if (target == null || !target.alive()) {
            return;
        }
        target.setHealth(0);
    }

    /**
     * Effect 5 — SPELL_EFFECT_TELEPORT_UNITS. CMaNGOS NearTeleportTo dest; taxi is a no-op.
     * Hearthstone 8690 dest is homebind (TARGET_LOCATION_DATABASE).
     */
    public void teleportUnits(Unit target, int mapId, float x, float y, float z, float o) {
        if (target == null) {
            return;
        }
        if ((target.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_TAXI_FLIGHT) != 0) {
            return;
        }
        target.mapId = mapId;
        target.relocate(x, y, z, o);
    }

    /**
     * Effect 43 — SPELL_EFFECT_TELEPORT_UNITS_FACE_CASTER. CMaNGOS NearTeleportTo dest
     * facing -caster orientation. Summon Player 20279. Dest stand-in is caster xyz.
     */
    public void teleportUnitsFaceCaster(Unit caster, Unit target) {
        if (caster == null || target == null) {
            return;
        }
        teleportUnits(target, target.mapId, caster.x, caster.y, caster.z, -caster.o);
    }

    /**
     * Effect 9 — SPELL_EFFECT_HEALTH_LEECH. CMaNGOS EffectHealthLeech: damage living
     * target (capped at current HP), heal caster if still alive.
     */
    public int healthLeech(Unit caster, Unit target, int amount) {
        if (caster == null || target == null || !target.alive() || amount <= 0) {
            return 0;
        }
        int dealt = Math.min(amount, target.health());
        target.setHealth(target.health() - dealt);
        if (caster.alive()) {
            caster.setHealth(caster.health() + dealt);
        }
        return dealt;
    }

    /**
     * Effect 8 — SPELL_EFFECT_POWER_DRAIN. CMaNGOS EffectPowerDrain: take power from
     * living target; EnergizeBySpell caster only when caster != target.
     */
    public int powerDrain(Unit caster, Unit target, int amount) {
        if (caster == null || target == null || !target.alive() || amount <= 0) {
            return 0;
        }
        int taken = Math.min(amount, target.power());
        target.setPower(target.power() - taken);
        if (caster != target) {
            caster.setPower(caster.power() + taken);
        }
        return taken;
    }

    /** Effect 80 — SPELL_EFFECT_ADD_COMBO_POINTS. CMaNGOS EffectAddComboPoints. */
    public void addComboPoints(Unit caster, Unit target, int count) {
        if (!(caster instanceof Player p) || target == null || count <= 0) {
            return;
        }
        p.addComboPoints(target, count);
    }

    /** Effect 68 — SPELL_EFFECT_INTERRUPT_CAST. CMaNGOS InterruptSpell on living target. */
    public void interruptCast(Unit target) {
        if (!(target instanceof Player p) || !p.alive()) {
            return;
        }
        p.interruptCast();
    }

    /** Effect 79 — SPELL_EFFECT_SANCTUARY. CMaNGOS CombatStop. */
    public void sanctuary(Unit target) {
        if (target == null) {
            return;
        }
        target.combatStop();
    }

    /**
     * Effect 84 — SPELL_EFFECT_STUCK. CMaNGOS player caster, taxi no-op; continent nudge
     * GetNearPoint 10 yd along facing (hearth/dungeon paths later). Stuck 7355.
     */
    public void stuck(Unit caster) {
        if (!(caster instanceof Player p)) {
            return;
        }
        if ((p.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_TAXI_FLIGHT) != 0) {
            return;
        }
        float destX = p.x + 10f * (float) Math.cos(p.o);
        float destY = p.y + 10f * (float) Math.sin(p.o);
        p.relocate(destX, destY, p.z, p.o);
    }

    /**
     * Effect 85 — SPELL_EFFECT_SUMMON_PLAYER. CMaNGOS SetSummonPoint + SMSG_SUMMON_REQUEST.
     * Ritual of Summoning Effect 7720. Evil Twin 23445 skips. Delay 2 minutes.
     */
    public void summonPlayer(Unit caster, Unit target) {
        if (caster == null || !(target instanceof Player p)) {
            return;
        }
        if (p.hasAura(23445)) {
            return;
        }
        p.offerSummon(caster.guid, caster.mapId, caster.x, caster.y, caster.z, MAX_PLAYER_SUMMON_DELAY_MS);
    }

    /** SMSG_SUMMON_REQUEST 0x2AB: summoner guid, area, auto-decline ms. */
    public static byte[] encodeSummonRequest(Unit caster, int delayMs) {
        if (caster == null) {
            return new byte[0];
        }
        WowBuffer b = new WowBuffer(16);
        b.putU64(caster.guid);
        b.putU32(caster.areaId);
        b.putU32(delayMs);
        return b.array();
    }

    /** Effect 19 — SPELL_EFFECT_ADD_EXTRA_ATTACKS. CMaNGOS m_extraAttacks += damage, cap 5. */
    public void addExtraAttacks(Unit target, int count) {
        if (target == null) {
            return;
        }
        target.addExtraAttacks(count);
    }

    /** Effect 11 — SPELL_EFFECT_BIND. CMaNGOS EffectBind: player target only, hearth at current loc. */
    public void bindHearth(Unit target) {
        if (!(target instanceof Player p)) {
            return;
        }
        p.setHomebindToLocation(p.mapId, p.zoneId, p.x, p.y, p.z);
    }

    /**
     * Effect 114 — SPELL_EFFECT_ATTACK_ME. CMaNGOS EffectTaunt: skip if already
     * attacking caster; equalize threat to highest and force victim.
     */
    public void attackMe(Unit caster, Unit target) {
        if (caster == null || !(target instanceof Creature c)) {
            return;
        }
        if (c.victim == caster.guid) {
            return;
        }
        float added = c.threatManager.highestThreat() - c.threatManager.threatOf(caster);
        c.threatManager.add(caster, added);
        c.victim = caster.guid;
    }

    /** Effect 16 — SPELL_EFFECT_QUEST_COMPLETE. CMaNGOS AreaExploredOrEventHappens(misc). */
    public void questComplete(Unit target, int questId) {
        if (!(target instanceof Player p)) {
            return;
        }
        p.areaExploredOrEventHappens(questId);
    }

    /**
     * Effect 18 — SPELL_EFFECT_RESURRECT. CMaNGOS EffectResurrect: dead/ghost player,
     * skip if a request is already pending; HP/mana from damage percent.
     */
    public void resurrect(Unit caster, Unit target, int damagePct) {
        if (caster == null || !(target instanceof Player p)) {
            return;
        }
        if (p.alive() && !p.ghost) {
            return;
        }
        if (p.resurrectGuid != 0) {
            return;
        }
        int maxHp = p.maxHealth();
        int health = maxHp * damagePct / 100;
        int mana = p.maxPower() * damagePct / 100;
        p.addResurrectRequest(caster.guid, caster.mapId, caster.x, caster.y, caster.z, health, mana);
    }

    /**
     * Effect 113 — SPELL_EFFECT_RESURRECT_NEW. CMaNGOS EffectResurrect: health = damage,
     * mana = EffectMiscValue (not percent). Resurrection 2006 Rank 1 is 69 HP.
     */
    public void resurrectNew(Unit caster, Unit target, int health, int mana) {
        if (caster == null || !(target instanceof Player p)) {
            return;
        }
        if (p.alive() && !p.ghost) {
            return;
        }
        if (p.resurrectGuid != 0) {
            return;
        }
        p.addResurrectRequest(caster.guid, caster.mapId, caster.x, caster.y, caster.z,
                Math.max(0, health), Math.max(0, mana));
    }

    /**
     * Effect 117 — SPELL_EFFECT_SPIRIT_HEAL. CMaNGOS ResurrectPlayer(1.0f). Spirit Heal
     * 22012 requires Waiting to Resurrect 2584. Hunter/warlock pet restore is later.
     */
    public void spiritHeal(Unit target, int spellId) {
        if (!(target instanceof Player p)) {
            return;
        }
        if (p.alive() && !p.ghost) {
            return;
        }
        if (spellId == 22012 && !p.hasAura(2584)) {
            return;
        }
        p.setGhost(false);
        p.setHealth(p.maxHealth());
    }

    /**
     * Effect 7 — SPELL_EFFECT_ENVIRONMENTAL_DAMAGE. CMaNGOS damages the player caster (GO fire).
     */
    public int environmentalDamage(Unit caster, int amount) {
        if (!(caster instanceof Player p) || !p.alive() || amount <= 0) {
            return 0;
        }
        int dealt = Math.min(amount, p.health());
        p.setHealth(p.health() - dealt);
        return dealt;
    }

    /** Effect 38 — SPELL_EFFECT_DISPEL. CMaNGOS: damage is max count; 0 means 1. */
    public int dispel(Unit target, int max) {
        if (target == null) {
            return 0;
        }
        return target.dispelAuras(max);
    }

    /**
     * Effect 62 — SPELL_EFFECT_POWER_BURN. CMaNGOS: burn matching power, then HP equal to burned.
     */
    public int powerBurn(Unit target, int amount, int powerType) {
        if (target == null || !target.alive() || amount <= 0) {
            return 0;
        }
        int type = target instanceof Player p ? p.powerType : 0;
        if (type != powerType) {
            return 0;
        }
        int taken = Math.min(amount, target.power());
        if (taken <= 0) {
            return 0;
        }
        target.setPower(target.power() - taken);
        int dealt = Math.min(taken, target.health());
        target.setHealth(target.health() - dealt);
        return dealt;
    }

    /** Effect 63 — SPELL_EFFECT_THREAT. CMaNGOS: living creature threat list, AddThreat(caster, damage). */
    public void addThreat(Unit caster, Unit target, int amount) {
        if (caster == null || !(target instanceof Creature c) || !caster.alive() || !c.alive() || amount <= 0) {
            return;
        }
        c.threatManager.add(caster, amount);
    }

    /** Effect 136 — SPELL_EFFECT_HEAL_PCT. CMaNGOS: living target, maxHealth * damage / 100. */
    public void healPct(Unit target, int pct) {
        if (target == null || !target.alive() || pct <= 0) {
            return;
        }
        int add = target.maxHealth() * pct / 100;
        target.setHealth(target.health() + add);
    }

    /** Effect 137 — SPELL_EFFECT_ENERGIZE_PCT. CMaNGOS: matching power, gain = damage * max / 100. */
    public void energizePct(Unit target, int pct, int powerType) {
        if (target == null || !target.alive() || pct <= 0) {
            return;
        }
        int type = target instanceof Player p ? p.powerType : 0;
        if (type != powerType) {
            return;
        }
        int max = target.maxPower();
        if (max == 0) {
            return;
        }
        energize(target, max * pct / 100);
    }

    /** Effect 100 — SPELL_EFFECT_INEBRIATE. CMaNGOS: player target, drunk += damage * 256, cap 0xFFFF. */
    public void inebriate(Unit target, int drinks) {
        if (!(target instanceof Player p) || drinks <= 0) {
            return;
        }
        p.setDrunkValue(p.drunkValue() + drinks * 256);
    }

    /** Effect 147 — SPELL_EFFECT_QUEST_FAIL. CMaNGOS FailQuest(misc). */
    public void questFail(Unit target, int questId) {
        if (!(target instanceof Player p)) {
            return;
        }
        p.failQuest(questId);
    }

    /**
     * Effect 75 — SPELL_EFFECT_HEAL_MECHANICAL. CMaNGOS EffectHealMechanical: living
     * unitTarget, heal by damage (creature type is targeting). Mechanical Patch Kit 15057.
     */
    public void healMechanical(Unit target, int amount) {
        if (target == null || !target.alive() || amount <= 0) {
            return;
        }
        target.setHealth(target.health() + amount);
    }

    /**
     * Effect 110 — SPELL_EFFECT_DESTROY_ALL_TOTEMS. CMaNGOS UnSummon all caster totem slots.
     * Totemic Call 36936.
     */
    public void destroyAllTotems(Unit caster) {
        if (!(caster instanceof Player p)) {
            return;
        }
        p.destroyAllTotems();
    }

    /**
     * Effect 111 — SPELL_EFFECT_DURABILITY_DAMAGE. CMaNGOS DurabilityPointsLoss on
     * player slot (misc). Melt Weapon 21388 is mainhand.
     */
    public void durabilityDamage(Unit target, int slot, int points) {
        if (!(target instanceof Player p) || points <= 0) {
            return;
        }
        if (slot < 0) {
            p.durabilityPointsLossAll(points, slot < -1);
            return;
        }
        if (slot >= Player.INVENTORY_SLOT_BAG_END) {
            return;
        }
        Item item = p.itemAt(0, slot);
        if (item != null) {
            p.durabilityPointsLoss(item, points);
        }
    }

    /**
     * Effect 115 — SPELL_EFFECT_DURABILITY_DAMAGE_PCT. CMaNGOS DurabilityLoss percent of max.
     * Corrupt Weapon 23436 is ranged slot 17 at 100%.
     */
    public void durabilityDamagePct(Unit target, int slot, int pct) {
        if (!(target instanceof Player p) || pct <= 0) {
            return;
        }
        if (slot < 0 || slot >= Player.INVENTORY_SLOT_BAG_END) {
            return;
        }
        Item item = p.itemAt(0, slot);
        if (item != null) {
            p.durabilityLoss(item, pct / 100.0);
        }
    }

    /** Effect 40 — SPELL_EFFECT_DUAL_WIELD. CMaNGOS SetCanDualWield(true). Dual Wield 674. */
    public void dualWield(Unit target) {
        if (target == null) {
            return;
        }
        target.setCanDualWield(true);
    }

    /** Effect 22 — SPELL_EFFECT_PARRY. CMaNGOS SetCanParry on m_caster. Parry 3127. */
    public void enableParry(Unit caster) {
        if (caster == null) {
            return;
        }
        caster.setCanParry(true);
    }

    /** Effect 23 — SPELL_EFFECT_BLOCK. CMaNGOS SetCanBlock on m_caster. Block 107. */
    public void enableBlock(Unit caster) {
        if (caster == null) {
            return;
        }
        caster.setCanBlock(true);
    }

    /** Effect 46 — SPELL_EFFECT_SPAWN. CMaNGOS RemoveFlag UNIT_FLAG_SPAWNING on m_caster. 15750. */
    public void spawn(Unit caster) {
        if (caster == null) {
            return;
        }
        caster.clearSpawningFlag();
    }

    /**
     * Effect 60 — SPELL_EFFECT_PROFICIENCY. CMaNGOS AddWeapon/ArmorProficiency on player caster.
     * One-Handed Axes 196 is item class 2 subclass mask 1.
     */
    public void proficiency(Unit caster, int itemClass, int subClassMask) {
        if (!(caster instanceof Player p) || subClassMask == 0) {
            return;
        }
        if (itemClass == Player.ITEM_CLASS_WEAPON && (p.weaponProficiency() & subClassMask) == 0) {
            p.addWeaponProficiency(subClassMask);
        }
        if (itemClass == Player.ITEM_CLASS_ARMOR && (p.armorProficiency() & subClassMask) == 0) {
            p.addArmorProficiency(subClassMask);
        }
    }

    /** SMSG_SET_PROFICIENCY 0x127: uint8 itemClass, uint32 subclassMask. */
    public static byte[] encodeSetProficiency(int itemClass, int itemSubclassMask) {
        WowBuffer b = new WowBuffer(5);
        b.putU8(itemClass);
        b.putU32(itemSubclassMask);
        return b.array();
    }

    /**
     * Effect 31 — SPELL_EFFECT_WEAPON_PERCENT_DAMAGE. CMaNGOS CalculateDamage × (damage/100).
     * Backstab 53 Rank 1 is 150% of weapon average.
     */
    public int weaponPercentDamage(Unit caster, Unit target, int pct) {
        if (caster == null || target == null || !target.alive() || pct <= 0) {
            return 0;
        }
        float min = caster.getFloat(UpdateFields.UNIT_FIELD_MINDAMAGE);
        float max = caster.getFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE);
        int weapon = Math.max(1, (int) ((min + max) / 2f));
        int dmg = Math.max(1, weapon * pct / 100);
        target.setHealth(target.health() - dmg);
        return dmg;
    }

    /**
     * Effect 121 — SPELL_EFFECT_NORMALIZED_WEAPON_DMG. CMaNGOS CalculateDamage(normalized).
     * Without weapon DBC swing times this is the weapon average. Sinister Strike 1752 Rank 1.
     */
    public int normalizedWeaponDamage(Unit caster, Unit target) {
        return weaponPercentDamage(caster, target, 100);
    }

    /**
     * Effect 69 — SPELL_EFFECT_DISTRACT. CMaNGOS skip in-combat; SetFacingTo dest.
     * Apply uses caster xy as dest until spell targets carry a ground dest. Distract 1725.
     */
    public void distract(Unit target, float destX, float destY) {
        if (target == null || target.inCombat) {
            return;
        }
        target.setFacingTo(destX, destY);
    }

    /**
     * Effect 123 — SPELL_EFFECT_SEND_TAXI. CMaNGOS ActivateTaxiPathTo(misc) on player target.
     * Taxi Stair of Destiny to Honor Hold 34907 is path 564.
     */
    public void sendTaxi(Unit target, int pathId) {
        if (!(target instanceof Player p) || pathId <= 0) {
            return;
        }
        p.startTaxiFlight(pathId);
    }

    /**
     * Effect 134 — SPELL_EFFECT_KILL_CREDIT_GROUP. CMaNGOS RewardPlayerAndGroupAtEventCredit(misc).
     * Kill Credit Greater Diemetradon 37907 is creature 21924. Solo only in v1.
     */
    public void killCreditGroup(Unit target, int creatureId) {
        if (!(target instanceof Player p)) {
            return;
        }
        p.killedMonsterCredit(creatureId);
    }

    /**
     * Effect 96 — SPELL_EFFECT_CHARGE. CMaNGOS MoveCharge to unitTarget.
     * Charge 100 Rank 1 relocates the caster to the target (no spline in v1).
     */
    public void charge(Unit caster, Unit target) {
        if (caster == null || target == null) {
            return;
        }
        float o = (float) Math.atan2(target.y - caster.y, target.x - caster.x);
        caster.relocate(target.x, target.y, target.z, o);
    }

    /**
     * Effect 149 — SPELL_EFFECT_CHARGE_DEST. CMaNGOS MoveCharge to dest. Eagle Swoop 44732.
     * v1 dest is SpellInfo.maxRange along caster facing (TARGET_FLAG_DEST_LOCATION stand-in).
     */
    public void chargeDest(Unit caster, float dist) {
        if (caster == null) {
            return;
        }
        float destX = caster.x + dist * (float) Math.cos(caster.o);
        float destY = caster.y + dist * (float) Math.sin(caster.o);
        float o = (float) Math.atan2(destY - caster.y, destX - caster.x);
        caster.relocate(destX, destY, caster.z, o);
    }

    /**
     * Effect 33 / 59 — SPELL_EFFECT_OPEN_LOCK / OPEN_LOCK_ITEM. CMaNGOS player caster, itemTarget.
     * Opening 3365 / 3366. ITEM_DYNFLAG_UNLOCKED; loot.md clientLootType PICKPOCKETING (2).
     */
    public void openLock(Unit caster, Item item) {
        if (!(caster instanceof Player p) || item == null) {
            return;
        }
        item.flags |= Content.ITEM_DYNFLAG_UNLOCKED;
        p.showOpenLockLoot(item.guid);
    }

    /**
     * Effect 92 — SPELL_EFFECT_ENCHANT_HELD_ITEM. CMaNGOS player target mainhand TEMP_ENCHANTMENT_SLOT.
     * Flametongue Totem Effect 8230 misc 124. Different existing temp enchant is a no-op.
     */
    public void enchantHeldItem(Unit target, int enchantId) {
        if (!(target instanceof Player p) || enchantId == 0) {
            return;
        }
        Item item = p.itemAt(0, Player.EQUIPMENT_SLOT_MAINHAND);
        if (item == null) {
            return;
        }
        if (item.tempEnchant != 0 && item.tempEnchant != enchantId) {
            return;
        }
        item.tempEnchant = enchantId;
    }

    /**
     * Effect 53 — SPELL_EFFECT_ENCHANT_ITEM. CMaNGOS PERM_ENCHANTMENT_SLOT on itemTarget.
     * Sharpen Blade 2605 misc enchant 1.
     */
    public void enchantItem(Unit caster, Item item, int enchantId) {
        if (!(caster instanceof Player)) {
            return;
        }
        if (item == null) {
            return;
        }
        if (enchantId == 0) {
            return;
        }
        item.enchant = enchantId;
    }

    /**
     * Effect 54 — SPELL_EFFECT_ENCHANT_ITEM_TEMPORARY. CMaNGOS TEMP_ENCHANTMENT_SLOT on itemTarget.
     * Deadly Poison 2823 misc enchant 7. Replaces an existing temp enchant.
     */
    public void enchantItemTemporary(Unit caster, Item item, int enchantId) {
        if (!(caster instanceof Player)) {
            return;
        }
        if (item == null) {
            return;
        }
        if (enchantId == 0) {
            return;
        }
        item.tempEnchant = enchantId;
    }

    /**
     * Effects 35/119/128/129/143 — SPELL_EFFECT_APPLY_AREA_AURA_PARTY/PET/FRIEND/ENEMY/OWNER.
     * CMaNGOS EffectApplyAreaAura: living unitTarget CreateAura.
     * Devotion Aura 465. Spirit Bond 19579. Strength of Earth 31634. Alluring Aura 29485. Soul Link 25228.
     */
    public void applyAreaAuraParty(Unit target, int spellId) {
        if (target == null) {
            return;
        }
        if (!target.alive()) {
            return;
        }
        if (spellId <= 0) {
            return;
        }
        target.auras.add(new Unit.Aura(spellId, 30_000, 1));
    }

    /**
     * Effect 44 — SPELL_EFFECT_SKILL_STEP. CMaNGOS SetSkillStep(misc, damage).
     * Apprentice Blacksmith 2020 skill 164 step 1. SkillTiers caps later.
     */
    public void skillStep(Unit target, int skillId, int step) {
        if (!(target instanceof Player p)) {
            return;
        }
        if (skillId <= 0) {
            return;
        }
        if (step <= 0) {
            return;
        }
        if (step > 16) {
            return;
        }
        p.learnSkill(skillId, 1, 1, step);
    }

    /** SMSG_LOOT_RESPONSE 0x160: item guid + clientLootType 2. */
    public static byte[] encodeOpenLockLoot(long guid) {
        return encodeSkinningLoot(guid);
    }

    /**
     * Effect 127 — SPELL_EFFECT_PROSPECTING. CMaNGOS player caster, itemTarget ShowContentTo.
     * Prospecting 31252. Item loot clientLootType PICKPOCKETING (2).
     */
    public void prospecting(Unit caster, Item item) {
        if (!(caster instanceof Player p) || item == null) {
            return;
        }
        p.showProspectingLoot(item.guid);
    }

    /** SMSG_LOOT_RESPONSE 0x160: item guid + clientLootType 2. */
    public static byte[] encodeProspectingLoot(long guid) {
        return encodeSkinningLoot(guid);
    }

    /**
     * Effect 99 — SPELL_EFFECT_DISENCHANT. CMaNGOS player caster, itemTarget ShowContentTo.
     * Disenchant 13262. Item loot clientLootType PICKPOCKETING (2).
     */
    public void disenchant(Unit caster, Item item) {
        if (!(caster instanceof Player p) || item == null) {
            return;
        }
        p.showDisenchantLoot(item.guid);
    }

    /** SMSG_LOOT_RESPONSE 0x160: item guid + clientLootType 2. */
    public static byte[] encodeDisenchantLoot(long guid) {
        return encodeSkinningLoot(guid);
    }

    /**
     * Effect 101 — SPELL_EFFECT_FEED_PET. CMaNGOS player caster, living pet, DestroyItemCount 1.
     * Feed Pet 6991 trigger is Feed Pet Effect 1539. Diet/itemlevel later.
     */
    public void feedPet(Unit caster, Item food, int triggerSpell) {
        if (!(caster instanceof Player p) || food == null || food.count <= 0) {
            return;
        }
        if (p.pet == null || !p.pet.summoned) {
            return;
        }
        food.count--;
        if (food.count <= 0) {
            p.items.remove(Guid.low(food.guid));
        }
    }

    /**
     * Effect 71 — SPELL_EFFECT_PICKPOCKET. CMaNGOS player caster, creature target.
     * Pick Pocket 921. loot.md clientLootType PICKPOCKETING (2).
     */
    public void pickPocket(Unit caster, Unit target) {
        if (!(caster instanceof Player p) || !(target instanceof Creature c)) {
            return;
        }
        p.showPickpocketLoot(c.guid);
    }

    /** SMSG_LOOT_RESPONSE 0x160: guid + clientLootType 2 + gold 0 + itemCount 0. */
    public static byte[] encodePickpocketLoot(long guid) {
        return encodeSkinningLoot(guid);
    }

    /**
     * Effect 95 — SPELL_EFFECT_SKINNING. CMaNGOS player caster, creature target.
     * Skinning 8613. loot.md clientLootType PICKPOCKETING (2). Clears UNIT_FLAG_SKINNABLE.
     */
    public void skinning(Unit caster, Unit target) {
        if (!(caster instanceof Player p) || !(target instanceof Creature c)) {
            return;
        }
        c.clearSkinnableFlag();
        p.showSkinningLoot(c.guid);
    }

    /** SMSG_LOOT_RESPONSE 0x160: guid + clientLootType 2 + gold 0 + itemCount 0. */
    public static byte[] encodeSkinningLoot(long guid) {
        WowBuffer b = new WowBuffer(16);
        b.putU64(guid);
        b.putU8(2);
        b.putU32(0);
        b.putU8(0);
        return b.array();
    }

    /**
     * Effect 116 — SPELL_EFFECT_SKIN_PLAYER_CORPSE. CMaNGOS player caster, BG victim
     * RemovedInsignia. Remove Insignia 22027. SMSG_PLAYER_SKINNED + CLIENT_LOOT_CORPSE.
     */
    public void skinPlayerCorpse(Unit caster, Unit target) {
        if (!(caster instanceof Player looter) || !(target instanceof Player victim)) {
            return;
        }
        if (!isBattlegroundOrArena(victim.mapId)) {
            return;
        }
        boolean repop = false;
        if (victim.deathTimerEndsAtMs > 0) {
            victim.deathTimerEndsAtMs = 0;
            repop = true;
        }
        Corpse bones = victim.corpse;
        if (bones == null) {
            return;
        }
        victim.setLastSkinnedRepop(repop ? 1 : 0);
        bones.corpseType = Corpse.CORPSE_BONES;
        bones.setInt(UpdateFields.CORPSE_FIELD_DYNAMIC_FLAGS, Corpse.CORPSE_DYNFLAG_LOOTABLE);
        looter.showInsigniaLoot(bones.guid);
    }

    /** SMSG_PLAYER_SKINNED 0x2BC: uint8 repop (Released spirit this skin). */
    public static byte[] encodePlayerSkinned(int repop) {
        WowBuffer b = new WowBuffer(1);
        b.putU8(repop);
        return b.array();
    }

    /** SMSG_LOOT_RESPONSE 0x160: corpse guid + CLIENT_LOOT_CORPSE (1). */
    public static byte[] encodeInsigniaLoot(long guid) {
        WowBuffer b = new WowBuffer(16);
        b.putU64(guid);
        b.putU8(1);
        b.putU32(0);
        b.putU8(0);
        return b.array();
    }

    /** CMaNGOS Player::GetBattleGroundId — AV/WSG/AB/EY plus arenas. */
    private static final Set<Integer> BATTLEGROUND_OR_ARENA_MAPS = Set.of(30, 489, 529, 566, 559, 562, 572);
    /** CMaNGOS Map::IsBattleGround — not arenas. */
    private static final Set<Integer> BATTLEGROUND_MAPS = Set.of(30, 489, 529, 566);

    static boolean isBattlegroundOrArena(int mapId) {
        return BATTLEGROUND_OR_ARENA_MAPS.contains(mapId);
    }

    static boolean isBattleGround(int mapId) {
        return BATTLEGROUND_MAPS.contains(mapId);
    }

    /**
     * Effect 86 — SPELL_EFFECT_ACTIVATE_OBJECT. CMaNGOS GameObjectActions.
     * Blow Zul'Farrak Door 11195 is DESTROY (12) → UseDoorOrButton alternative.
     */
    public void activateObject(GameObject go, int action) {
        if (go == null) {
            return;
        }
        if (action == GameObjectUse.ACTION_DESTROY) {
            GameObjectUse.useDoorOrButton(go, true);
            return;
        }
        if (action == GameObjectUse.ACTION_OPEN) {
            GameObjectUse.useDoorOrButton(go, false);
        }
    }

    /**
     * Effects 104/105 — SPELL_EFFECT_SUMMON_OBJECT_SLOT1/2. CMaNGOS EffectSummonObject slots 0/1.
     * Freezing Trap 1499 misc GO 2561. Player SMSG_TOTEM_CREATED.
     */
    public void summonObjectSlot(Unit caster, int slot, int goEntry) {
        if (caster == null || goEntry <= 0 || slot < 0 || slot >= Unit.MAX_OBJECT_SLOT) {
            return;
        }
        if (caster.objectSlot(slot) != null) {
            caster.setObjectSlot(slot, null);
        }
        GameObject go = new GameObject();
        go.entry = goEntry;
        go.guid = Guid.HIGH_GAMEOBJECT | (caster.guid & 0xFFFFFFFFL);
        go.relocate(caster.x, caster.y, caster.z, caster.o);
        caster.setObjectSlot(slot, go);
        if (caster instanceof Player p) {
            p.setLastTotemCreated(slot, go.guid);
        }
    }

    /**
     * Effect 76 — SPELL_EFFECT_SUMMON_OBJECT_WILD. CMaNGOS EffectSummonObjectWild:
     * GO at caster, no object slot. Summon Rusty Chest 6464 misc 19021.
     */
    public void summonObjectWild(Unit caster, int goEntry) {
        if (caster == null || goEntry <= 0) {
            return;
        }
        GameObject go = new GameObject();
        go.entry = goEntry;
        go.guid = Guid.HIGH_GAMEOBJECT | (caster.guid & 0xFFFFFFFFL);
        go.relocate(caster.x, caster.y, caster.z, caster.o);
        caster.setLastWildObject(go);
    }

    /**
     * Effect 50 — SPELL_EFFECT_TRANS_DOOR. CMaNGOS EffectTransmitted: GO at caster.
     * Lightwell 724 misc GO 181102. No object slot (unlike SLOT1/2).
     */
    public void transmitted(Unit caster, int goEntry) {
        if (caster == null || goEntry <= 0) {
            return;
        }
        GameObject go = new GameObject();
        go.entry = goEntry;
        go.guid = Guid.HIGH_GAMEOBJECT | (caster.guid & 0xFFFFFFFFL);
        go.relocate(caster.x, caster.y, caster.z, caster.o);
        caster.setLastTransmittedObject(go);
    }

    /**
     * Effect 28 — SPELL_EFFECT_SUMMON. CMaNGOS EffectSummonType: creature at caster.
     * Eye of Kilrogg 126 misc 4277. SummonProperties DBC later.
     */
    public void summon(Unit caster, int creatureEntry) {
        if (caster == null || creatureEntry <= 0) {
            return;
        }
        Creature summoned = new Creature();
        summoned.entry = creatureEntry;
        summoned.guid = Guid.HIGH_CREATURE | (caster.guid & 0xFFFFFFFFL);
        summoned.relocate(caster.x, caster.y, caster.z, caster.o);
        caster.setLastSummon(summoned);
    }

    /**
     * Effect 130 — SPELL_EFFECT_REDIRECT_THREAT. CMaNGOS EffectRedirectThreat:
     * caster HostileRefManager SetThreatRedirection(unitTarget). Misdirection 34477 effect 3.
     */
    public void redirectThreat(Unit caster, Unit target) {
        if (caster == null || target == null) {
            return;
        }
        caster.setThreatRedirection(target.guid);
    }

    /**
     * Effect 27 — SPELL_EFFECT_PERSISTENT_AREA_AURA. CMaNGOS EffectPersistentAA:
     * dynobject at dest (caster xyz stand-in). Blizzard 10 EffectRadiusIndex1 is 0.
     */
    public void persistentAreaAura(Unit caster, int spellId, float x, float y, float z, float radius) {
        if (caster == null || spellId <= 0) {
            return;
        }
        DynamicObject dyn = new DynamicObject();
        dyn.spellId = spellId;
        dyn.radius = radius;
        dyn.guid = Guid.HIGH_DYNAMICOBJECT | (caster.guid & 0xFFFFFFFFL);
        dyn.relocate(x, y, z, 0f);
        dyn.setInt(UpdateFields.OBJECT_FIELD_ENTRY, spellId);
        dyn.setGuid(UpdateFields.DYNAMICOBJECT_CASTER, caster.guid);
        dyn.setInt(UpdateFields.DYNAMICOBJECT_BYTES, DynamicObject.DYNAMIC_OBJECT_AREA_SPELL);
        dyn.setInt(UpdateFields.DYNAMICOBJECT_SPELLID, spellId);
        dyn.setFloat(UpdateFields.DYNAMICOBJECT_RADIUS, radius);
        dyn.setFloat(UpdateFields.DYNAMICOBJECT_POS_X, x);
        dyn.setFloat(UpdateFields.DYNAMICOBJECT_POS_Y, y);
        dyn.setFloat(UpdateFields.DYNAMICOBJECT_POS_Z, z);
        caster.setLastDynObject(dyn);
    }

    /**
     * Effect 61 — SPELL_EFFECT_SEND_EVENT. CMaNGOS StartEvents_Event(misc).
     * Summon Myzrael 4141 misc event 420. dbscripts_on_event later.
     */
    public void sendEvent(Unit caster, int eventId) {
        if (caster == null || eventId <= 0) {
            return;
        }
        caster.setLastSendEvent(eventId);
    }

    /** SMSG_TOTEM_CREATED 0x412: slot, GO guid, extra 0, duration 0. */
    public static byte[] encodeTotemCreated(int slot, long guid) {
        WowBuffer b = new WowBuffer(17);
        b.putU8(slot);
        b.putU64(guid);
        b.putU32(0);
        b.putU32(0);
        return b.array();
    }

    /**
     * Effect 83 — SPELL_EFFECT_DUEL. CMaNGOS arbiter GO at midpoint; SMSG_DUEL_REQUESTED.
     * Duel 7266 misc GO 21680.
     */
    public void duel(Unit caster, Unit target, int flagEntry) {
        if (!(caster instanceof Player a)) {
            return;
        }
        if (!(target instanceof Player b) || a == b) {
            return;
        }
        if (a.duelOpponent != null || b.duelOpponent != null) {
            return;
        }
        if (flagEntry <= 0) {
            return;
        }
        GameObject flag = new GameObject();
        flag.entry = flagEntry;
        flag.guid = Guid.HIGH_GAMEOBJECT | (a.guid & 0xFFFFFFFFL);
        flag.relocate((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f, a.z, a.o);
        a.setDuelFlag(flag);
        a.duelOpponent = b;
        b.duelOpponent = a;
    }

    /** SMSG_DUEL_REQUESTED 0x167: raw arbiter GO guid, raw caster guid. */
    public static byte[] encodeDuelRequested(long flagGuid, long casterGuid) {
        WowBuffer b = new WowBuffer(16);
        b.putU64(flagGuid);
        b.putU64(casterGuid);
        return b.array();
    }

    /**
     * Effect 120 — SPELL_EFFECT_TELEPORT_GRAVEYARD. CMaNGOS player + IsBattleGround
     * RepopAtGraveyard. Graveyard Teleport Test 24253.
     */
    public void teleportGraveyard(Unit target, GraveyardManager yards) {
        if (!(target instanceof Player p) || yards == null || !isBattleGround(p.mapId)) {
            return;
        }
        GraveyardManager.Loc gy = yards.closest(p.mapId, p.x, p.y, p.z, p.team, 0);
        if (gy == null) {
            return;
        }
        p.deathTimerEndsAtMs = 0;
        teleportUnits(p, gy.map(), gy.x(), gy.y(), gy.z(), gy.o());
    }

    /**
     * Effect 102 — SPELL_EFFECT_DISMISS_PET. CMaNGOS player caster, living pet Unsummon.
     * Dismiss Pet 2641. summoned maps IsAlive; missing or not summoned is a no-op.
     */
    public void dismissPet(Unit caster) {
        if (!(caster instanceof Player p) || p.pet == null || !p.pet.summoned) {
            return;
        }
        p.pet = null;
    }

    /**
     * Effect 109 — SPELL_EFFECT_SUMMON_DEAD_PET. CMaNGOS revive existing dead pet.
     * Revive Pet 982. summoned maps IsAlive; missing/living no-op (LoadPetFromDB later).
     */
    public void summonDeadPet(Unit caster) {
        if (!(caster instanceof Player p) || p.pet == null || p.pet.summoned) {
            return;
        }
        p.pet.summoned = true;
    }

    /**
     * Effect 153 — SPELL_EFFECT_CREATE_PET. CMaNGOS hunter unitTarget, misc creature entry.
     * Create Tamed Warp Stalker 46686 misc 26037.
     */
    public void createTamedPet(Unit target, int creatureEntry) {
        if (!(target instanceof Player p) || p.clazz != Player.CLASS_HUNTER || creatureEntry <= 0) {
            return;
        }
        Pet pet = new Pet();
        pet.entry = creatureEntry;
        pet.summoned = true;
        p.pet = pet;
    }

    /**
     * Effect 55 — SPELL_EFFECT_TAMECREATURE. CMaNGOS hunter caster, pet from creature
     * target, ForcedDespawn the beast. Tame Beast 13481.
     */
    public void tameCreature(Unit caster, Unit target) {
        if (!(caster instanceof Player p) || p.clazz != Player.CLASS_HUNTER) {
            return;
        }
        if (!(target instanceof Creature c) || c.entry <= 0) {
            return;
        }
        Pet pet = new Pet();
        pet.entry = c.entry;
        pet.level = c.level;
        pet.summoned = true;
        p.pet = pet;
        c.setHealth(0);
    }

    /**
     * Effect 56 — SPELL_EFFECT_SUMMON_PET. CMaNGOS hunter LoadPetFromDB; warlock create
     * from misc. Summon Imp 688 misc 416. Call Pet 883 with no saved pet is a no-op.
     */
    public void summonPet(Unit caster, int petEntry) {
        if (!(caster instanceof Player p)) {
            return;
        }
        if (p.clazz == Player.CLASS_HUNTER) {
            if (p.pet != null && !p.pet.summoned) {
                p.pet.summoned = true;
            }
            return;
        }
        if (petEntry <= 0) {
            return;
        }
        Pet pet = new Pet();
        pet.entry = petEntry;
        pet.summoned = true;
        p.pet = pet;
    }

    /**
     * Effect 132 — SPELL_EFFECT_PLAY_MUSIC. CMaNGOS PlayMusic(misc) to player target.
     * Ribbon Pole Music 46852 is SoundEntries 12319. SMSG_PLAY_MUSIC 0x277 uint32.
     */
    public void playMusic(Unit target, int soundId) {
        if (!(target instanceof Player p)) {
            return;
        }
        p.playMusic(soundId);
    }

    /**
     * Effect 131 — SPELL_EFFECT_PLAY_SOUND. CMaNGOS PlayDirectSound(misc) to player target.
     * BOTM Jungle Madness Music 49963 is SoundEntries 7294. SMSG_PLAY_SOUND 0x2D2 uint32.
     */
    public void playSound(Unit target, int soundId) {
        if (!(target instanceof Player p)) {
            return;
        }
        p.playSound(soundId);
    }

    /** SMSG_PLAY_SOUND 0x2D2: uint32 soundId. */
    public static byte[] encodePlaySound(int soundId) {
        WowBuffer b = new WowBuffer(4);
        b.putU32(soundId);
        return b.array();
    }

    /** SMSG_PLAY_MUSIC 0x277: uint32 soundId. */
    public static byte[] encodePlayMusic(int soundId) {
        WowBuffer b = new WowBuffer(4);
        b.putU32(soundId);
        return b.array();
    }

    /** CMaNGOS Movement::gravity — EffectPullTowards projectile Z. */
    public static final float MOVEMENT_GRAVITY = 19.29110527038574f;

    /**
     * Effect 124 — SPELL_EFFECT_PULL_TOWARDS. CMaNGOS KnockBackWithAngle toward caster.
     * Magnetic Pull 28337 misc 300 is speedXY 30. Dist below 0.1 is a no-op.
     */
    public void pullTowards(Unit caster, Unit target, int misc) {
        if (caster == null || target == null) {
            return;
        }
        float dx = caster.x - target.x;
        float dy = caster.y - target.y;
        float dz = caster.z - target.z;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 0.1f) {
            return;
        }
        float speedXY = Math.max(1, misc) * 0.1f;
        float time = dist / speedXY;
        float speedZ = (dz + 0.5f * time * time * MOVEMENT_GRAVITY) / time;
        float angle = (float) Math.atan2(dy, dx);
        target.knockBackWithAngle(angle, speedXY, speedZ);
    }

    /**
     * Effect 145 — SPELL_EFFECT_PULL_TOWARDS_DEST. CMaNGOS EffectPullTowards dest branch:
     * 2D dist to dest, projectile Z from dest. Black Hole Effect 46230 misc 150 is speedXY 15.
     * v1 dest is caster xyz offset by SpellInfo.maxRange along facing.
     */
    public void pullTowardsDest(Unit target, float destX, float destY, float destZ, int misc) {
        if (target == null) {
            return;
        }
        float dx = destX - target.x;
        float dy = destY - target.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.1f) {
            return;
        }
        float speedXY = Math.max(1, misc) * 0.1f;
        float time = dist / speedXY;
        float speedZ = (destZ - target.z + 0.5f * time * time * MOVEMENT_GRAVITY) / time;
        float angle = (float) Math.atan2(dy, dx);
        target.knockBackWithAngle(angle, speedXY, speedZ);
    }

    /**
     * Effect 29 — SPELL_EFFECT_LEAP. CMaNGOS EffectLeapForward NearTeleportTo dest, keep facing.
     * Blink 1953 dest is TARGET_LOCATION_CASTER_FRONT_LEAP; v1 uses SpellInfo.maxRange along facing
     * (DBC radius stand-in).
     */
    public void leapForward(Unit target, float dist) {
        if (target == null) {
            return;
        }
        float destX = target.x + dist * (float) Math.cos(target.o);
        float destY = target.y + dist * (float) Math.sin(target.o);
        target.relocate(destX, destY, target.z, target.o);
    }

    /**
     * Effect 138 — SPELL_EFFECT_LEAP_BACK. CMaNGOS KnockBackFrom(unitTarget) on caster.
     * Negative Jump 40622 is misc 100 / damage -350. Taxi flight is a no-op.
     */
    public void leapBack(Unit caster, Unit target, float horiz, float vert) {
        if (caster == null || target == null) {
            return;
        }
        if ((caster.getInt(UpdateFields.UNIT_FIELD_FLAGS) & Unit.UNIT_FLAG_TAXI_FLIGHT) != 0) {
            return;
        }
        caster.knockBackFrom(target, horiz, vert);
    }

    /**
     * Effect 126 — SPELL_EFFECT_STEAL_BENEFICIAL_BUFF. CMaNGOS cannot steal from self;
     * RemoveAurasDueToSpellBySteal onto caster. Spellsteal 30449.
     */
    public void stealBeneficialBuff(Unit caster, Unit target, int max) {
        if (caster == null || target == null || caster == target) {
            return;
        }
        int n = max <= 0 ? 1 : max;
        int stolen = 0;
        var it = target.auras.iterator();
        while (it.hasNext() && stolen < n) {
            Unit.Aura aura = it.next();
            it.remove();
            caster.auras.add(aura);
            stolen++;
        }
    }

    /**
     * SMSG_SPELLSTEALLOG 0x333: packed victim, packed caster, steal spell, unk 0,
     * count, then each stolen spellId + uint8 0 (steal not transfer).
     */
    public static byte[] encodeSpellStealLog(Unit victim, Unit caster, int stealSpellId, int stolenSpellId) {
        WowBuffer b = new WowBuffer(32);
        b.putPackedGuid(victim.guid);
        b.putPackedGuid(caster.guid);
        b.putU32(stealSpellId);
        b.putU8(0);
        b.putU32(1);
        b.putU32(stolenSpellId);
        b.putU8(0);
        return b.array();
    }

    /**
     * Effect 108 — SPELL_EFFECT_DISPEL_MECHANIC. CMaNGOS HasMechanic(misc); damage count (0→1).
     * Escape Artist 20589 first effect is MECHANIC_ROOT 7.
     */
    public void dispelMechanic(Unit target, int mechanic, int max) {
        if (target == null) {
            return;
        }
        target.dispelMechanic(mechanic, max);
    }

    /**
     * Effect 98 — SPELL_EFFECT_KNOCK_BACK. CMaNGOS KnockBackFrom(caster, misc/10, damage/10).
     * Rooted targets skip. Knockback 10689.
     */
    public void knockBack(Unit caster, Unit target, float horiz, float vert) {
        if (caster == null || target == null) {
            return;
        }
        target.knockBackFrom(caster, horiz, vert);
    }

    /**
     * Effect 144 — SPELL_EFFECT_KNOCKBACK_FROM_POSITION. CMaNGOS KnockBackWithAngle away from dest.
     * Spectral Blast 44866 misc 125 / damage 75. Dest stand-in is caster xyz.
     */
    public void knockBackFromPosition(Unit target, float destX, float destY, float horiz, float vert) {
        if (target == null) {
            return;
        }
        float angle = (float) Math.atan2(destY - target.y, destX - target.x) + (float) Math.PI;
        target.knockBackWithAngle(angle, horiz, vert);
    }

    /**
     * Effect 125 — SPELL_EFFECT_MODIFY_THREAT_PERCENT. CMaNGOS modifyThreatPercent(caster, damage).
     * Soulshatter 32835 is -50. Percent is signed; below -100 drops the ref.
     */
    public void modifyThreatPercent(Unit caster, Unit target, int percent) {
        if (caster == null || !(target instanceof Creature c)) {
            return;
        }
        c.threatManager.modifyThreatPercent(caster, percent);
    }

    /**
     * Effect 103 — SPELL_EFFECT_REPUTATION. CMaNGOS ModifyReputation(misc faction, damage).
     * Stormpike Reputation +5 is spell 21187, faction 730.
     */
    public void modifyReputation(Unit target, int factionId, int amount) {
        if (!(target instanceof Player p) || factionId <= 0 || amount == 0) {
            return;
        }
        p.modifyReputation(factionId, amount);
    }

    /**
     * movement.md SMSG_MOVE_KNOCK_BACK (0x0EF): packed GUID, counter, vcos, vsin, horiz, -vert.
     */
    public static byte[] encodeMoveKnockBack(Unit who, int counter) {
        if (who == null || !who.hasKnockBack()) {
            return new byte[0];
        }
        WowBuffer b = new WowBuffer(32);
        b.putPackedGuid(who.guid);
        b.putU32(counter);
        b.putFloat(who.knockBackVcos());
        b.putFloat(who.knockBackVsin());
        b.putFloat(who.knockBackHoriz());
        b.putFloat(-who.knockBackVert());
        return b.array();
    }

    /**
     * Effect 94 — SPELL_EFFECT_SELF_RESURRECT. CMaNGOS: player caster, percent of max HP.
     * Soulstone is for dead/ghost.
     */
    public void selfResurrect(Unit caster, int pct) {
        if (!(caster instanceof Player p) || pct <= 0) {
            return;
        }
        if (p.alive() && !p.ghost) {
            return;
        }
        p.setGhost(false);
        p.setHealth(p.maxHealth() * pct / 100);
    }

    /** Effect 30 — restore power (spell-algorithms.md). */
    public void energize(Unit target, int amount) {
        if (target == null || amount <= 0) {
            return;
        }
        target.setPower(target.power() + amount);
    }

    /**
     * Effect 72 — SPELL_EFFECT_ADD_FARSIGHT. CMaNGOS creates a dynobject focus then
     * Camera::SetView (updates PLAYER_FARSIGHT). Java: bind focus unit guid + camera.
     */
    public void addFarsight(Player caster, long focusGuid) {
        if (caster == null || focusGuid == 0) {
            return;
        }
        caster.setFarSightGuid(focusGuid);
        caster.setCameraViewGuid(focusGuid);
    }

    /**
     * Effect 34 — SPELL_EFFECT_SUMMON_CHANGE_ITEM. CMaNGOS ConvertItem on the cast item.
     * Summon Thunderstrike 21180 EffectItemType 17223.
     */
    public void summonChangeItem(Unit caster, Item oldItem, int newItemId) {
        if (!(caster instanceof Player) || oldItem == null || newItemId <= 0) {
            return;
        }
        oldItem.entry = newItemId;
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

    /**
     * Effect 57 — SPELL_EFFECT_LEARN_PET_SPELL. CMaNGOS living pet learnSpell(trigger).
     * Fire Shield 2949 Rank 1 teaches 2947.
     */
    public void learnPetSpell(Unit caster, int spellId) {
        if (!(caster instanceof Player p) || p.pet == null || !p.pet.summoned) {
            return;
        }
        p.pet.learnSpell(spellId);
    }

    /**
     * Effect 133 — SPELL_EFFECT_UNLEARN_SPECIALIZATION. CMaNGOS removeSpell(EffectTriggerSpell).
     * Unlearn Spellfire Tailoring 41299 removes 26797. SpellInfo.misc holds the trigger id.
     */
    public void unlearnSpecialization(Unit target, int spellId) {
        if (!(target instanceof Player p) || spellId <= 0) {
            return;
        }
        p.removeSpell(spellId);
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
        return encodeStart(caster, spellId, castCount, 0, targets);
    }

    /** spell.md SMSG_SPELL_START: caster ×2 packed, spellId, castCount, castFlags, timer (ms), targets. */
    public byte[] encodeStart(long caster, int spellId, int castCount, int timerMs, SpellCastTargets targets) {
        WowBuffer b = new WowBuffer(64);
        b.putPackedGuid(caster);
        b.putPackedGuid(caster);
        b.putU32(spellId);
        b.putU8(castCount);
        b.putU16(CAST_FLAG_UNKNOWN2);
        b.putU32(timerMs);
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
