package org.tbc.world.entity;

import org.tbc.common.WowBuffer;
import org.tbc.world.net.wow8606.MovementInfo;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class Unit extends Entity {
    public static final int TYPEID_UNIT = 3;
    public static final int TYPEID_PLAYER = 4;
    public static final int UNIT_FLAG_SPAWNING = 0x00000002;
    public static final int UNIT_FLAG_PLAYER_CONTROLLED = 0x8;
    public static final int UNIT_FLAG_EVADING_HOME = 0x00000010;
    public static final int UNIT_FLAG_NOT_ATTACKABLE_1 = 0x00000080;
    public static final int UNIT_FLAG_IMMUNE_TO_PLAYER = 0x00000100;
    public static final int UNIT_FLAG_IMMUNE_TO_NPC = 0x00000200;
    public static final int UNIT_FLAG_PVP = 0x00001000;
    /** Unit.h UNIT_FLAG_SILENCED — silenced, 2.1.1. */
    public static final int UNIT_FLAG_SILENCED = 0x00002000;
    public static final int UNIT_FLAG_UNTARGETABLE = 0x00010000;
    /** Unit.h UNIT_FLAG_PACIFIED. */
    public static final int UNIT_FLAG_PACIFIED = 0x00020000;
    public static final int UNIT_FLAG_STUNNED = 0x00040000;
    public static final int UNIT_FLAG_IN_COMBAT = 0x00080000;
    public static final int UNIT_FLAG_TAXI_FLIGHT = 0x00100000;
    /** Unit.h UNIT_FLAG_DISARMED — melee weapons disabled. */
    public static final int UNIT_FLAG_DISARMED = 0x00200000;
    /** Unit.h UNIT_FLAG_CONFUSED — subject to confused movement. */
    public static final int UNIT_FLAG_CONFUSED = 0x00400000;
    /** Unit.h UNIT_FLAG_FLEEING — subject to fleeing movement. */
    public static final int UNIT_FLAG_FLEEING = 0x00800000;
    public static final int UNIT_FLAG_UNINTERACTIBLE = 0x02000000;
    public static final int UNIT_FLAG_SKINNABLE = 0x04000000;
    /** Unit.h UNIT_FLAG_PREVENT_ANIM — Feign Death. */
    public static final int UNIT_FLAG_PREVENT_ANIM = 0x20000000;
    /** Unit.h UNIT_FLAG2_FEIGN_DEATH. */
    public static final int UNIT_FLAG2_FEIGN_DEATH = 0x00000001;
    /** Unit.h UNIT_FLAG2_COMPREHEND_LANG. */
    public static final int UNIT_FLAG2_COMPREHEND_LANG = 0x00000008;
    /** Unit.h UNIT_FLAG2_FORCE_MOVE. */
    public static final int UNIT_FLAG2_FORCE_MOVE = 0x00000040;
    /** Unit.h UNIT_FLAG_PREPARATION. */
    public static final int UNIT_FLAG_PREPARATION = 0x00000020;
    /** Unit.h UNIT_FLAG_IMMUNE — school immunity (aura 39). */
    public static final int UNIT_FLAG_IMMUNE = 0x80000000;
    /** SharedDefines.h SPELL_SCHOOL_MASK_NORMAL. */
    public static final int SPELL_SCHOOL_MASK_NORMAL = 1;
    /** SharedDefines.h POWER_MANA — UNIT_FIELD_BYTES_0 power byte / POWER1. */
    public static final int POWER_MANA = 0;
    /** ChrClasses.dbc CLASS_MAGE — creature UnitClass that uses mana. */
    public static final int CLASS_MAGE = 8;
    /** SharedDefines.h UNIT_DYNFLAG_DEAD. */
    public static final int UNIT_DYNFLAG_DEAD = 0x0020;
    /** SharedDefines.h UNIT_DYNFLAG_TRACK_UNIT — Hunter's Mark. */
    public static final int UNIT_DYNFLAG_TRACK_UNIT = 0x0002;
    /** SharedDefines.h UNIT_DYNFLAG_SPECIALINFO — Beast Lore / Empathy. */
    public static final int UNIT_DYNFLAG_SPECIALINFO = 0x0010;
    public static final int UPDATEFLAG_SELF = 0x01;
    public static final int UPDATEFLAG_LOWGUID = 0x08;
    public static final int UPDATEFLAG_HIGHGUID = 0x10;
    public static final int UPDATEFLAG_LIVING = 0x20;
    public static final int UPDATEFLAG_HAS_POSITION = 0x40;
    public static final int PLAYER_CREATE_FLAGS = UPDATEFLAG_SELF | UPDATEFLAG_HIGHGUID | UPDATEFLAG_LIVING | UPDATEFLAG_HAS_POSITION;
    public static final int UNIT_STAND_STATE_STAND = 0;
    public static final int UNIT_STAND_STATE_SIT = 1;
    public static final int UNIT_STAND_STATE_SLEEP = 3;
    public static final int UNIT_STAND_STATE_KNEEL = 8;
    /** UNIT_FIELD_BYTES_1 byte 3 — ghosts (BuildPlayerRepop). */
    public static final int UNIT_BYTE1_FLAG_ALWAYS_STAND = 0x01;
    /** SpellDefines.h ShapeshiftForm — UNIT_FIELD_BYTES_2 byte 3. */
    public static final int FORM_NONE = 0;
    public static final int FORM_BATTLESTANCE = 0x11;

    /** Unit.cpp baseMoveSpeed[MOVE_RUN]. */
    public static final float BASE_RUN_SPEED = 7.0f;
    /** Unit.cpp baseMoveSpeed[MOVE_SWIM]. */
    public static final float BASE_SWIM_SPEED = 4.722222f;
    /** CMaNGOS baseMoveSpeed[MOVE_FLIGHT]. */
    public static final float BASE_FLIGHT_SPEED = 7.0f;
    /** ObjectVisibility.h STEALTH_TYPE_MAX — unit and trap. */
    public static final int STEALTH_TYPE_MAX = 2;
    /** ObjectVisibility.h INVISIBILITY_TYPE_MAX. */
    public static final int INVISIBILITY_TYPE_MAX = 32;
    /** SharedDefines.h WeaponAttackType. */
    public static final int BASE_ATTACK = 0;
    public static final int OFF_ATTACK = 1;
    public static final int RANGED_ATTACK = 2;
    public static final int MAX_ATTACK = 3;

    public MovementInfo movement = new MovementInfo();
    public long victim;
    public boolean inCombat;
    private int extraAttacks;
    /** CMaNGOS m_modWeaponHitChance[MAX_ATTACK] — SPELL_AURA_MOD_HIT_CHANCE + ratings. */
    private final float[] modWeaponHitChance = new float[MAX_ATTACK];
    /** CMaNGOS m_modSpellHitChance — SPELL_AURA_MOD_SPELL_HIT_CHANCE + CR_HIT_SPELL. */
    private float modSpellHitChance;
    /** CMaNGOS m_modSpellCritChance[MAX_SPELL_SCHOOL] — SPELL_AURA_MOD_SPELL_CRIT_CHANCE. */
    private final float[] modSpellCritChance = new float[7];
    private boolean rooted;
    /** Active SPELL_AURA_MOD_ROOT holders — SetImmobilizedState stacking. */
    private int rootAuraCount;
    /** Active SPELL_AURA_MOD_STUN holders — HasAuraType stacking before SetStunned(false). */
    private int stunAuraCount;
    /** Active SPELL_AURA_MOD_FEAR holders — HasAuraType for PREVENTS_FLEEING. */
    private int fearAuraCount;
    /** Active SPELL_AURA_MOD_STEALTH holders — HasAuraType before clearing visibility. */
    private int stealthAuraCount;
    /** Active SPELL_AURA_MOD_INVISIBILITY holders. */
    private int invisAuraCount;
    /**
     * CMaNGOS UnitVisibility — stealth/invis adjust who sees this unit (UpdateVisibilityAndView).
     * Default VISIBILITY_ON.
     */
    private Visibility visibility = Visibility.ON;
    /** CMaNGOS GetMaxNegativeAuraModifier(SPELL_AURA_MOD_DECREASE_SPEED); 0 = none. */
    private int decreaseSpeedPct;
    /** CMaNGOS GetMaxPositiveAuraModifier(SPELL_AURA_MOD_INCREASE_SPEED); 0 = none. */
    private int increaseSpeedPct;
    /** CMaNGOS GetMaxPositiveAuraModifier(SPELL_AURA_MOD_INCREASE_MOUNTED_SPEED); 0 = none. */
    private int mountedSpeedPct;
    /** CMaNGOS GetMaxPositiveAuraModifier(SPELL_AURA_MOD_INCREASE_SWIM_SPEED); 0 = none. */
    private int increaseSwimSpeedPct;
    /** CMaNGOS GetTotalAuraModifier(SPELL_AURA_SAFE_FALL) — yards subtracted from fall height. */
    private int safeFallBonus;
    /** CMaNGOS VisibilityData stealth detection strength (SPELL_AURA_MOD_STEALTH_DETECT). */
    private int stealthDetectStrength;
    /** CMaNGOS VisibilityData stealth strength per StealthType (SPELL_AURA_MOD_STEALTH_LEVEL). */
    private final int[] stealthStrength = new int[STEALTH_TYPE_MAX];
    /** SPELL_AURA_MOD_INVISIBILITY_DETECTION value per InvisibilityType. */
    private final int[] invisDetectValue = new int[INVISIBILITY_TYPE_MAX];
    /** spell id → {type, amount} so unapply can drop the detect mask when the type is gone. */
    private final Map<Integer, int[]> invisDetectAuras = new HashMap<>();
    private int invisDetectMask;
    /** spell id → amount for SPELL_AURA_USE_NORMAL_MOVEMENT_SPEED. */
    private final Map<Integer, Integer> normalMoveSpeedAuras = new HashMap<>();
    /** Max positive aura 191 amount. 0 means UpdateSpeed does not cap. */
    private int normalMoveSpeedCap;
    /** Spell id → {school mask, amount} for SPELL_AURA_MOD_RESISTANCE_EXCLUSIVE. */
    private final Map<Integer, int[]> exclusiveResist = new HashMap<>();
    /** Currently applied exclusive resist per school, so a new max only deltas the sheet. */
    private final int[] exclusiveResistApplied = new int[7];
    /** CMaNGOS m_speed_rate[MOVE_RUN]; default 1.0. */
    private float runSpeedRate = 1.0f;
    /** CMaNGOS m_speed_rate[MOVE_SWIM]; default 1.0. */
    private float swimSpeedRate = 1.0f;
    /** CMaNGOS m_speed_rate[MOVE_FLIGHT]; default 1.0. */
    private float flightSpeedRate = 1.0f;
    /** Aura id → spell id → amount for flight-speed auras 206–211. */
    private final Map<Integer, Map<Integer, Integer>> flightAuras = new HashMap<>();
    /** Spell id → amount for SPELL_AURA_MOD_SCALE (61) and SPELL_AURA_MOD_SCALE_2 (239). */
    private final Map<Integer, Integer> scaleAuras = new HashMap<>();
    private final Map<Integer, Integer> scale2Auras = new HashMap<>();
    /** CMaNGOS m_threatModifier[MAX_SPELL_SCHOOL], default 1. */
    private final float[] threatModifier = {1f, 1f, 1f, 1f, 1f, 1f, 1f};
    /** Living SPELL_AURA_MOD_THREAT holders. ApplyTotalThreatModifier skips the product at 0. */
    private int threatModAuraCount;
    /** Spell id → school mask for SPELL_AURA_SCHOOL_IMMUNITY. */
    private final Map<Integer, Integer> schoolImmunity = new HashMap<>();
    /** Spell id → mechanic for SPELL_AURA_MECHANIC_IMMUNITY. */
    private final Map<Integer, Integer> mechanicImmunity = new HashMap<>();
    /** Spell id → effect id for SPELL_AURA_EFFECT_IMMUNITY. */
    private final Map<Integer, Integer> effectImmunity = new HashMap<>();
    /** Spell id → school mask for SPELL_AURA_DAMAGE_IMMUNITY. */
    private final Map<Integer, Integer> damageImmunity = new HashMap<>();
    /** Faction saved by the first SPELL_AURA_FACTION_OVERRIDE still in force. */
    private boolean factionOverrideActive;
    private int savedFactionTemplate;
    private int savedFactionId;
    /** Classless unproficient-armor move-speed penalty (negative percent points, e.g. -15). */
    private float equipmentSpeedPenaltyPct;
    private boolean knockBackPending;
    private float knockBackVcos;
    private float knockBackVsin;
    private float knockBackHoriz;
    private float knockBackVert;

    /** CMaNGOS UNIT_STAT_ROOT — EffectKnockBack returns early. */
    public boolean rooted() {
        return rooted;
    }

    public void setRooted(boolean rooted) {
        this.rooted = rooted;
    }

    /** Stacking SPELL_AURA_MOD_ROOT — CMaNGOS HasAuraType before SetImmobilizedState(false). */
    public int rootAuraCount() {
        return rootAuraCount;
    }

    /** @return true if this was the first root (should SendMoveRoot true). */
    public boolean addRootAura() {
        rootAuraCount++;
        rooted = true;
        return rootAuraCount == 1;
    }

    /** @return true if no MOD_ROOT remain (should SendMoveRoot false). */
    public boolean removeRootAura() {
        if (rootAuraCount > 0) {
            rootAuraCount--;
        }
        if (rootAuraCount == 0) {
            rooted = false;
            return true;
        }
        return false;
    }

    /** Stacking SPELL_AURA_MOD_STUN — CMaNGOS HasAuraType before SetStunned(false). */
    public int stunAuraCount() {
        return stunAuraCount;
    }

    /** @return true if this was the first stun. */
    public boolean addStunAura() {
        stunAuraCount++;
        return stunAuraCount == 1;
    }

    /** @return true if no MOD_STUN remain (should SetStunned(false) + maybe unroot). */
    public boolean removeStunAura() {
        if (stunAuraCount > 0) {
            stunAuraCount--;
        }
        return stunAuraCount == 0;
    }

    /** Stacking SPELL_AURA_MOD_FEAR — CMaNGOS HasAuraType for HandlePreventFleeing. */
    public int fearAuraCount() {
        return fearAuraCount;
    }

    public boolean addFearAura() {
        fearAuraCount++;
        return fearAuraCount == 1;
    }

    /** @return true if no MOD_FEAR remain. */
    public boolean removeFearAura() {
        if (fearAuraCount > 0) {
            fearAuraCount--;
        }
        return fearAuraCount == 0;
    }

    /**
     * CMaNGOS UnitVisibility (Unit.h). Values match the C++ enum.
     */
    public enum Visibility {
        OFF(0),
        ON(1),
        GROUP_STEALTH(2),
        GROUP_INVISIBILITY(3),
        GROUP_NO_DETECT(4);

        public final int code;

        Visibility(int code) {
            this.code = code;
        }
    }

    /** UNIT_FIELD_BYTES_1 byte 2 — UNIT_VIS_FLAG_CREEP (Unit.h). */
    public static final int UNIT_VIS_FLAG_CREEP = 0x02;
    /** UNIT_FIELD_BYTES_1 byte 2 — UNIT_VIS_FLAG_UNTRACKABLE (Unit.h). */
    public static final int UNIT_VIS_FLAG_UNTRACKABLE = 0x04;

    public Visibility visibility() {
        return visibility;
    }

    public void setVisibility(Visibility v) {
        if (v != null) {
            visibility = v;
        }
    }

    /** Set/clear UNIT_VIS_FLAG_CREEP on UNIT_FIELD_BYTES_1 offset 2. */
    public void setVisFlagCreep(boolean apply) {
        int bytes = getInt(UpdateFields.UNIT_FIELD_BYTES_1);
        int mask = UNIT_VIS_FLAG_CREEP << 16;
        setInt(UpdateFields.UNIT_FIELD_BYTES_1, apply ? bytes | mask : bytes & ~mask);
    }

    public boolean hasVisFlagCreep() {
        return ((getInt(UpdateFields.UNIT_FIELD_BYTES_1) >>> 16) & 0xFF & UNIT_VIS_FLAG_CREEP) != 0;
    }

    /** Set/clear UNIT_VIS_FLAG_UNTRACKABLE on UNIT_FIELD_BYTES_1 offset 2. */
    public void setVisFlagUntrackable(boolean apply) {
        int bytes = getInt(UpdateFields.UNIT_FIELD_BYTES_1);
        int mask = UNIT_VIS_FLAG_UNTRACKABLE << 16;
        setInt(UpdateFields.UNIT_FIELD_BYTES_1, apply ? bytes | mask : bytes & ~mask);
    }

    public boolean hasVisFlagUntrackable() {
        return ((getInt(UpdateFields.UNIT_FIELD_BYTES_1) >>> 16) & 0xFF & UNIT_VIS_FLAG_UNTRACKABLE) != 0;
    }

    public int stealthAuraCount() {
        return stealthAuraCount;
    }

    public boolean addStealthAura() {
        stealthAuraCount++;
        return stealthAuraCount == 1;
    }

    public boolean removeStealthAura() {
        if (stealthAuraCount > 0) {
            stealthAuraCount--;
        }
        return stealthAuraCount == 0;
    }

    public int invisAuraCount() {
        return invisAuraCount;
    }

    public boolean addInvisAura() {
        invisAuraCount++;
        return invisAuraCount == 1;
    }

    public boolean removeInvisAura() {
        if (invisAuraCount > 0) {
            invisAuraCount--;
        }
        return invisAuraCount == 0;
    }

    /**
     * CMaNGOS isVisibleForOrDetect (simplified): self always; stealth/invis/no-detect hidden
     * from others until detect/group (later).
     */
    public boolean isVisibleTo(Unit observer) {
        if (observer == null || observer.guid == guid) {
            return true;
        }
        return switch (visibility) {
            case GROUP_STEALTH, GROUP_NO_DETECT, GROUP_INVISIBILITY, OFF -> false;
            case ON -> true;
        };
    }

    /**
     * CMaNGOS Unit::SendMoveRoot when not client-controlled: SMSG_SPLINE_MOVE_ROOT / UNROOT
     * packed guid via SendMessageToSet ({@link #messageToSet}). Players override with FORCE_*.
     */
    public BiConsumer<Integer, byte[]> messageToSet;

    public void sendMoveRoot(boolean root) {
        setRooted(root);
        if (messageToSet == null) {
            return;
        }
        WowBuffer b = new WowBuffer(9);
        b.putPackedGuid(guid);
        messageToSet.accept(root ? Opcodes.SMSG_SPLINE_MOVE_ROOT : Opcodes.SMSG_SPLINE_MOVE_UNROOT, b.array());
    }

    /**
     * CMaNGOS Unit::SetWaterWalk when not client-controlled: SMSG_SPLINE_MOVE_WATER_WALK / LAND_WALK
     * packed guid via {@link #messageToSet}. Players override with SMSG_MOVE_WATER_WALK / LAND_WALK.
     */
    public void sendWaterWalk(boolean enable) {
        if (messageToSet == null) {
            return;
        }
        WowBuffer b = new WowBuffer(9);
        b.putPackedGuid(guid);
        messageToSet.accept(enable ? Opcodes.SMSG_SPLINE_MOVE_WATER_WALK : Opcodes.SMSG_SPLINE_MOVE_LAND_WALK,
                b.array());
    }

    /**
     * CMaNGOS Unit::SetFeatherFall when not client-controlled: SMSG_SPLINE_MOVE_FEATHER_FALL / NORMAL_FALL
     * packed guid via {@link #messageToSet}. Players override with SMSG_MOVE_FEATHER_FALL / NORMAL_FALL.
     */
    public void sendFeatherFall(boolean enable) {
        if (messageToSet == null) {
            return;
        }
        WowBuffer b = new WowBuffer(9);
        b.putPackedGuid(guid);
        messageToSet.accept(enable ? Opcodes.SMSG_SPLINE_MOVE_FEATHER_FALL : Opcodes.SMSG_SPLINE_MOVE_NORMAL_FALL,
                b.array());
    }

    /**
     * CMaNGOS Unit::SetCanFly when not client-controlled: SMSG_SPLINE_MOVE_SET_FLYING / UNSET_FLYING.
     * Players override with SMSG_MOVE_SET_CAN_FLY / UNSET_CAN_FLY.
     */
    public void sendCanFly(boolean enable) {
        if (messageToSet == null) {
            return;
        }
        WowBuffer b = new WowBuffer(9);
        b.putPackedGuid(guid);
        messageToSet.accept(enable ? Opcodes.SMSG_SPLINE_MOVE_SET_FLYING : Opcodes.SMSG_SPLINE_MOVE_UNSET_FLYING,
                b.array());
    }

    /**
     * CMaNGOS Unit::SetHover when not client-controlled: SMSG_SPLINE_MOVE_SET_HOVER / UNSET_HOVER
     * packed guid via {@link #messageToSet}. Players override with SMSG_MOVE_SET_HOVER / UNSET_HOVER.
     */
    public void sendHover(boolean enable) {
        if (messageToSet == null) {
            return;
        }
        WowBuffer b = new WowBuffer(9);
        b.putPackedGuid(guid);
        messageToSet.accept(enable ? Opcodes.SMSG_SPLINE_MOVE_SET_HOVER : Opcodes.SMSG_SPLINE_MOVE_UNSET_HOVER,
                b.array());
    }

    /**
     * CMaNGOS UpdateSpeed(MOVE_RUN) after MOD_DECREASE_SPEED — rate *= (100+slow)/100,
     * then SetSpeedRate → SMSG_SPLINE_SET_RUN_SPEED (non-player) / FORCE (Player override).
     */
    public void setDecreaseSpeedPct(int pct) {
        decreaseSpeedPct = pct;
        updateRunSpeed();
    }

    public void setIncreaseSpeedPct(int pct) {
        increaseSpeedPct = pct;
        updateRunSpeed();
    }

    /**
     * Aura 32 — mounted {@code main_speed_mod}. Stored even on foot; {@link #updateRunSpeed}
     * uses it only while {@link Player#mounted} is set.
     */
    public void setMountedSpeedPct(int pct) {
        mountedSpeedPct = pct;
        updateRunSpeed();
    }

    public void setIncreaseSwimSpeedPct(int pct) {
        increaseSwimSpeedPct = pct;
        updateSwimSpeed();
    }

    public void addSafeFall(int yards) {
        safeFallBonus += yards;
    }

    public int safeFall() {
        return safeFallBonus;
    }

    /** CMaNGOS VisibilityData::AddStealthDetectionStrength. */
    public void addStealthDetect(int delta) {
        stealthDetectStrength += delta;
    }

    public int stealthDetectStrength() {
        return stealthDetectStrength;
    }

    /** CMaNGOS VisibilityData::AddStealthStrength. Type outside 0..1 is ignored. */
    public void addStealthStrength(int type, int delta) {
        if (type < 0 || type >= stealthStrength.length || delta == 0) {
            return;
        }
        stealthStrength[type] += delta;
    }

    public int stealthStrength(int type) {
        if (type < 0 || type >= stealthStrength.length) {
            return 0;
        }
        return stealthStrength[type];
    }

    /**
     * Aura 19 — SetInvisibilityDetectMask plus AddInvisibilityDetectValue.
     * Unapply clears the type bit only when no other aura of that type remains.
     */
    public void setInvisibilityDetect(int spellId, int type, int amount, boolean apply) {
        if (apply) {
            if (type < 0 || type >= invisDetectValue.length) {
                return;
            }
            invisDetectAuras.put(spellId, new int[] {type, amount});
            invisDetectValue[type] += amount;
            invisDetectMask |= 1 << type;
            return;
        }
        int[] prev = invisDetectAuras.remove(spellId);
        if (prev == null) {
            return;
        }
        type = prev[0];
        invisDetectValue[type] -= prev[1];
        boolean still = false;
        for (int[] entry : invisDetectAuras.values()) {
            if (entry[0] == type) {
                still = true;
                break;
            }
        }
        if (!still) {
            invisDetectMask &= ~(1 << type);
        }
    }

    public int invisibilityDetectMask() {
        return invisDetectMask;
    }

    public int invisibilityDetectValue(int type) {
        if (type < 0 || type >= invisDetectValue.length) {
            return 0;
        }
        return invisDetectValue[type];
    }

    /**
     * Aura 191 — max positive amount. Players cap MOVE_RUN and MOVE_SWIM in UpdateSpeed.
     * Flight speed is still unwired, so MOVE_FLIGHT is unchanged.
     */
    public void setUseNormalMovementSpeed(int spellId, int amount, boolean apply) {
        if (apply) {
            normalMoveSpeedAuras.put(spellId, amount);
        } else {
            normalMoveSpeedAuras.remove(spellId);
        }
        int max = 0;
        for (int value : normalMoveSpeedAuras.values()) {
            if (value > max) {
                max = value;
            }
        }
        if (max == normalMoveSpeedCap) {
            return;
        }
        normalMoveSpeedCap = max;
        updateRunSpeed();
        updateSwimSpeed();
        updateFlightSpeed();
    }

    /**
     * Aura 143 — highest positive exclusive resist per school bit. Recomputes the sheet delta
     * from the remaining spell contributions.
     */
    public void setExclusiveResistance(int spellId, int mask, int amount, boolean apply) {
        if (apply) {
            exclusiveResist.put(spellId, new int[] {mask, amount});
        } else {
            exclusiveResist.remove(spellId);
        }
        for (int school = 0; school < exclusiveResistApplied.length; school++) {
            int max = 0;
            for (int[] entry : exclusiveResist.values()) {
                if ((entry[0] & (1 << school)) != 0 && entry[1] > max) {
                    max = entry[1];
                }
            }
            int delta = max - exclusiveResistApplied[school];
            exclusiveResistApplied[school] = max;
            if (delta == 0) {
                continue;
            }
            setInt(UpdateFields.UNIT_FIELD_RESISTANCES + school,
                    getInt(UpdateFields.UNIT_FIELD_RESISTANCES + school) + delta);
            setInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE + school,
                    getInt(UpdateFields.UNIT_FIELD_RESISTANCEBUFFMODSPOSITIVE + school) + delta);
        }
    }

    /** CMaNGOS Unit::GetHitChance(WeaponAttackType). */
    public float weaponHitChance(int attackType) {
        return modWeaponHitChance[attackType];
    }

    /** CMaNGOS HandleModHitChance creature path / UpdateWeaponHitChances EquippedItemClass −1. */
    public void adjustWeaponHitChance(int attackType, float delta) {
        modWeaponHitChance[attackType] += delta;
    }

    /** CMaNGOS Unit::GetHitChance(SpellSchoolMask) base — m_modSpellHitChance. */
    public float spellHitChance() {
        return modSpellHitChance;
    }

    /** CMaNGOS HandleModSpellHitChance creature path / UpdateSpellHitChances aura sum. */
    public void adjustSpellHitChance(float delta) {
        modSpellHitChance += delta;
    }

    /** CMaNGOS m_modSpellCritChance[school]. */
    public float spellCritChance(int school) {
        return modSpellCritChance[school];
    }

    /** CMaNGOS HandleModSpellCritChance creature path. */
    public void adjustSpellCritChance(int school, float delta) {
        modSpellCritChance[school] += delta;
    }

    /**
     * CMaNGOS Unit::SetFeignDeath — FLAGS_2 FEIGN_DEATH + DYNFLAG_DEAD on apply always.
     * PLAYER_CONTROLLED success → CombatStop; failed resist roll → SendFeignDeathResisted, keep combat.
     * NPC never CombatStop.
     */
    public void setFeignDeath(boolean apply) {
        setFeignDeath(apply, true);
    }

    public void setFeignDeath(boolean apply, boolean success) {
        int f2 = getInt(UpdateFields.UNIT_FIELD_FLAGS_2);
        int dyn = getInt(UpdateFields.UNIT_DYNAMIC_FLAGS);
        if (apply) {
            boolean playerControlled = (getInt(UpdateFields.UNIT_FIELD_FLAGS) & UNIT_FLAG_PLAYER_CONTROLLED) != 0;
            if (playerControlled && success) {
                combatStop();
            }
            if (playerControlled && !success) {
                sendFeignDeathResisted();
            }
            setInt(UpdateFields.UNIT_FIELD_FLAGS_2, f2 | UNIT_FLAG2_FEIGN_DEATH);
            setInt(UpdateFields.UNIT_DYNAMIC_FLAGS, dyn | UNIT_DYNFLAG_DEAD);
        } else {
            setInt(UpdateFields.UNIT_FIELD_FLAGS_2, f2 & ~UNIT_FLAG2_FEIGN_DEATH);
            setInt(UpdateFields.UNIT_DYNAMIC_FLAGS, dyn & ~UNIT_DYNFLAG_DEAD);
        }
    }

    public boolean isFeigningDeath() {
        return (getInt(UpdateFields.UNIT_FIELD_FLAGS_2) & UNIT_FLAG2_FEIGN_DEATH) != 0;
    }

    public float runSpeed() {
        return runSpeedRate * BASE_RUN_SPEED;
    }

    public float swimSpeed() {
        return swimSpeedRate * BASE_SWIM_SPEED;
    }

    public float flightSpeed() {
        return flightSpeedRate * BASE_FLIGHT_SPEED;
    }

    /**
     * Auras 206–211. Unmounted flight uses 206 (sum), 208 (product) and 210 (max positive).
     * Mounted flight uses 207 (max positive), 209 (product) and 211 (max positive).
     */
    public void setFlightSpeedAura(int aura, int spellId, int amount, boolean apply) {
        Map<Integer, Integer> bucket = flightAuras.computeIfAbsent(aura, ignored -> new HashMap<>());
        if (apply) {
            bucket.put(spellId, amount);
        } else {
            bucket.remove(spellId);
        }
        updateFlightSpeed();
    }

    /** Classless armor penalty: negative percent points summed from unproficient pieces. */
    public void setEquipmentSpeedPenaltyPct(float pct) {
        if (pct > 0f) {
            pct = 0f;
        }
        if (pct < -90f) {
            pct = -90f;
        }
        if (pct == equipmentSpeedPenaltyPct) {
            return;
        }
        equipmentSpeedPenaltyPct = pct;
        updateRunSpeed();
    }

    public float equipmentSpeedPenaltyPct() {
        return equipmentSpeedPenaltyPct;
    }

    private void updateRunSpeed() {
        // Unit::UpdateSpeed MOVE_RUN: mounted uses aura 32; on foot aura 31.
        // Aura 191 caps players before the slow.
        int positive = increaseSpeedPct;
        if (this instanceof Player player && player.mounted && mountedSpeedPct != 0) {
            positive = mountedSpeedPct;
        }
        float rate = positive != 0 ? (100.0f + positive) / 100.0f : 1.0f;
        if (this instanceof Player && normalMoveSpeedCap > 0) {
            float maxRate = normalMoveSpeedCap / BASE_RUN_SPEED;
            if (rate > maxRate) {
                rate = maxRate;
            }
        }
        if (decreaseSpeedPct != 0) {
            rate *= (100.0f + decreaseSpeedPct) / 100.0f;
        }
        if (equipmentSpeedPenaltyPct != 0f) {
            rate *= (100.0f + equipmentSpeedPenaltyPct) / 100.0f;
        }
        if (rate < 0.01f) {
            rate = 0.01f;
        }
        if (rate == runSpeedRate) {
            return;
        }
        runSpeedRate = rate;
        sendRunSpeedChange();
    }

    private void updateSwimSpeed() {
        // Unit::UpdateSpeed MOVE_SWIM: main_speed_mod = MOD_INCREASE_SWIM_SPEED.
        float rate = increaseSwimSpeedPct != 0 ? (100.0f + increaseSwimSpeedPct) / 100.0f : 1.0f;
        if (this instanceof Player && normalMoveSpeedCap > 0) {
            float maxRate = normalMoveSpeedCap / BASE_SWIM_SPEED;
            if (rate > maxRate) {
                rate = maxRate;
            }
        }
        if (rate < 0.01f) {
            rate = 0.01f;
        }
        if (rate == swimSpeedRate) {
            return;
        }
        swimSpeedRate = rate;
        sendSwimSpeedChange();
    }

    private void updateFlightSpeed() {
        // Unit::UpdateSpeed MOVE_FLIGHT. Aura 191 caps players before the slow.
        boolean mounted = this instanceof Player player && player.mounted;
        int main = mounted ? flightMaxPositive(207) : flightSum(206);
        float stack = mounted ? flightProduct(209) : flightProduct(208);
        float nonStack = (100f + (mounted ? flightMaxPositive(211) : flightMaxPositive(210))) / 100f;
        float bonus = Math.max(nonStack, stack);
        float rate = main != 0 ? bonus * (100f + main) / 100f : bonus;
        if (this instanceof Player && normalMoveSpeedCap > 0) {
            float maxRate = normalMoveSpeedCap / BASE_FLIGHT_SPEED;
            if (rate > maxRate) {
                rate = maxRate;
            }
        }
        if (decreaseSpeedPct != 0) {
            rate *= (100f + decreaseSpeedPct) / 100f;
        }
        if (rate < 0.01f) {
            rate = 0.01f;
        }
        if (rate == flightSpeedRate) {
            return;
        }
        flightSpeedRate = rate;
        sendFlightSpeedChange();
    }

    private int flightSum(int aura) {
        Map<Integer, Integer> bucket = flightAuras.get(aura);
        if (bucket == null) {
            return 0;
        }
        int sum = 0;
        for (int value : bucket.values()) {
            sum += value;
        }
        return sum;
    }

    private int flightMaxPositive(int aura) {
        Map<Integer, Integer> bucket = flightAuras.get(aura);
        if (bucket == null) {
            return 0;
        }
        int max = 0;
        for (int value : bucket.values()) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private float flightProduct(int aura) {
        Map<Integer, Integer> bucket = flightAuras.get(aura);
        if (bucket == null) {
            return 1f;
        }
        float product = 1f;
        for (int value : bucket.values()) {
            product *= (100f + value) / 100f;
        }
        return product;
    }

    /** Non-player: SMSG_SPLINE_SET_FLIGHT_SPEED packed guid + float speed. */
    protected void sendFlightSpeedChange() {
        if (messageToSet == null) {
            return;
        }
        WowBuffer b = new WowBuffer(13);
        b.putPackedGuid(guid);
        b.putFloat(flightSpeed());
        messageToSet.accept(Opcodes.SMSG_SPLINE_SET_FLIGHT_SPEED, b.array());
    }

    /** Non-player: SMSG_SPLINE_SET_RUN_SPEED packed guid + float speed. */
    protected void sendRunSpeedChange() {
        if (messageToSet == null) {
            return;
        }
        WowBuffer b = new WowBuffer(13);
        b.putPackedGuid(guid);
        b.putFloat(runSpeed());
        messageToSet.accept(Opcodes.SMSG_SPLINE_SET_RUN_SPEED, b.array());
    }

    /** Non-player: SMSG_SPLINE_SET_SWIM_SPEED packed guid + float speed. */
    protected void sendSwimSpeedChange() {
        if (messageToSet == null) {
            return;
        }
        WowBuffer b = new WowBuffer(13);
        b.putPackedGuid(guid);
        b.putFloat(swimSpeed());
        messageToSet.accept(Opcodes.SMSG_SPLINE_SET_SWIM_SPEED, b.array());
    }

    /**
     * CMaNGOS Unit::KnockBackFrom — angle from {@code from} to this (self: o+π).
     * Speeds are what SMSG_MOVE_KNOCK_BACK carries (vert is inverted on the wire).
     */
    public void knockBackFrom(Unit from, float horiz, float vert) {
        if (rooted || from == null) {
            return;
        }
        float angle = from == this
                ? o + (float) Math.PI
                : (float) Math.atan2(y - from.y, x - from.x);
        knockBackWithAngle(angle, horiz, vert);
    }

    /** CMaNGOS Unit::KnockBackWithAngle — SMSG_MOVE_KNOCK_BACK direction. */
    public void knockBackWithAngle(float angle, float horiz, float vert) {
        knockBackVcos = (float) Math.cos(angle);
        knockBackVsin = (float) Math.sin(angle);
        knockBackHoriz = horiz;
        knockBackVert = vert;
        knockBackPending = true;
    }

    public boolean hasKnockBack() {
        return knockBackPending;
    }

    public float knockBackVcos() {
        return knockBackVcos;
    }

    public float knockBackVsin() {
        return knockBackVsin;
    }

    public float knockBackHoriz() {
        return knockBackHoriz;
    }

    public float knockBackVert() {
        return knockBackVert;
    }

    private boolean canDualWield;

    public boolean canDualWield() {
        return canDualWield;
    }

    /** CMaNGOS Unit::SetCanDualWield. Dual Wield 674. */
    public void setCanDualWield(boolean value) {
        canDualWield = value;
    }

    private boolean canParry;

    public boolean canParry() {
        return canParry;
    }

    /** CMaNGOS Unit::SetCanParry. Parry 3127. */
    public void setCanParry(boolean value) {
        canParry = value;
    }

    private boolean canBlock;

    public boolean canBlock() {
        return canBlock;
    }

    /** CMaNGOS Unit::SetCanBlock. Block 107. */
    public void setCanBlock(boolean value) {
        canBlock = value;
    }

    /** CMaNGOS m_ObjectSlotGuid — trap/totem GO slots (MAX_TOTEM_SLOT 4). */
    public static final int MAX_OBJECT_SLOT = 4;
    private final GameObject[] objectSlots = new GameObject[MAX_OBJECT_SLOT];

    public GameObject objectSlot(int slot) {
        if (slot < 0 || slot >= MAX_OBJECT_SLOT) {
            return null;
        }
        return objectSlots[slot];
    }

    public void setObjectSlot(int slot, GameObject go) {
        if (slot < 0 || slot >= MAX_OBJECT_SLOT) {
            return;
        }
        objectSlots[slot] = go;
    }

    /** CMaNGOS EffectSummonObjectWild — GO has no owner / object slot. */
    private GameObject lastWildObject;

    public GameObject lastWildObject() {
        return lastWildObject;
    }

    public void setLastWildObject(GameObject go) {
        lastWildObject = go;
    }

    /** CMaNGOS EffectTransmitted — summoned GO at caster (portals / Lightwell). */
    private GameObject lastTransmittedObject;

    public GameObject lastTransmittedObject() {
        return lastTransmittedObject;
    }

    public void setLastTransmittedObject(GameObject go) {
        lastTransmittedObject = go;
    }

    /** CMaNGOS EffectSummonType — summoned creature at caster (dest stand-in). */
    private Creature lastSummon;

    public Creature lastSummon() {
        return lastSummon;
    }

    public void setLastSummon(Creature summoned) {
        lastSummon = summoned;
    }

    /** CMaNGOS EffectPersistentAA — dynobject at dest. */
    private DynamicObject lastDynObject;

    public DynamicObject lastDynObject() {
        return lastDynObject;
    }

    public void setLastDynObject(DynamicObject dyn) {
        lastDynObject = dyn;
    }

    /** CMaNGOS HostileRefManager m_redirectionTargetGuid — 2.x redirects full threat. */
    private long threatRedirectionGuid;

    public long threatRedirectionGuid() {
        return threatRedirectionGuid;
    }

    public void setThreatRedirection(long guid) {
        threatRedirectionGuid = guid;
    }

    /** CMaNGOS StartEvents_Event — dbscripts_on_event id from EffectSendEvent. */
    private int lastSendEvent;

    public int lastSendEvent() {
        return lastSendEvent;
    }

    public void setLastSendEvent(int eventId) {
        lastSendEvent = eventId;
    }

    /** CMaNGOS Unit::SetStunned — ApplyModFlag(UNIT_FIELD_FLAGS, UNIT_FLAG_STUNNED, apply). */
    public void setStunned(boolean apply) {
        int flags = getInt(UpdateFields.UNIT_FIELD_FLAGS);
        setInt(UpdateFields.UNIT_FIELD_FLAGS,
                apply ? flags | UNIT_FLAG_STUNNED : flags & ~UNIT_FLAG_STUNNED);
    }

    /** CMaNGOS HandleAuraModSilence — SetFlag/RemoveFlag(UNIT_FIELD_FLAGS, UNIT_FLAG_SILENCED). */
    public void setSilenced(boolean apply) {
        int flags = getInt(UpdateFields.UNIT_FIELD_FLAGS);
        setInt(UpdateFields.UNIT_FIELD_FLAGS,
                apply ? flags | UNIT_FLAG_SILENCED : flags & ~UNIT_FLAG_SILENCED);
    }

    /** CMaNGOS HandleAuraModPacify — SetFlag/RemoveFlag(UNIT_FIELD_FLAGS, UNIT_FLAG_PACIFIED). */
    public void setPacified(boolean apply) {
        int flags = getInt(UpdateFields.UNIT_FIELD_FLAGS);
        setInt(UpdateFields.UNIT_FIELD_FLAGS,
                apply ? flags | UNIT_FLAG_PACIFIED : flags & ~UNIT_FLAG_PACIFIED);
    }

    /** CMaNGOS Unit::SetFleeing — SetFlag/RemoveFlag(UNIT_FIELD_FLAGS, UNIT_FLAG_FLEEING). */
    public void setFleeing(boolean apply) {
        int flags = getInt(UpdateFields.UNIT_FIELD_FLAGS);
        setInt(UpdateFields.UNIT_FIELD_FLAGS,
                apply ? flags | UNIT_FLAG_FLEEING : flags & ~UNIT_FLAG_FLEEING);
    }

    /** CMaNGOS Unit::SetConfused — SetFlag/RemoveFlag(UNIT_FIELD_FLAGS, UNIT_FLAG_CONFUSED). */
    public void setConfused(boolean apply) {
        int flags = getInt(UpdateFields.UNIT_FIELD_FLAGS);
        setInt(UpdateFields.UNIT_FIELD_FLAGS,
                apply ? flags | UNIT_FLAG_CONFUSED : flags & ~UNIT_FLAG_CONFUSED);
    }

    /** CMaNGOS HandleAuraModDisarm — ApplyModFlag(UNIT_FIELD_FLAGS, UNIT_FLAG_DISARMED, apply). */
    public void setDisarmed(boolean apply) {
        int flags = getInt(UpdateFields.UNIT_FIELD_FLAGS);
        setInt(UpdateFields.UNIT_FIELD_FLAGS,
                apply ? flags | UNIT_FLAG_DISARMED : flags & ~UNIT_FLAG_DISARMED);
    }

    /** CMaNGOS HandleModUnattackable — ApplyModFlag(UNIT_FIELD_FLAGS, UNIT_FLAG_UNTARGETABLE, apply). */
    public void setUntargetable(boolean apply) {
        int flags = getInt(UpdateFields.UNIT_FIELD_FLAGS);
        setInt(UpdateFields.UNIT_FIELD_FLAGS,
                apply ? flags | UNIT_FLAG_UNTARGETABLE : flags & ~UNIT_FLAG_UNTARGETABLE);
    }

    /** CMaNGOS RemoveFlag(UNIT_FIELD_FLAGS, UNIT_FLAG_SPAWNING). EffectSpawn. */
    public void clearSpawningFlag() {
        setInt(UpdateFields.UNIT_FIELD_FLAGS,
                getInt(UpdateFields.UNIT_FIELD_FLAGS) & ~UNIT_FLAG_SPAWNING);
    }

    /** CMaNGOS RemoveFlag(UNIT_FIELD_FLAGS, UNIT_FLAG_SKINNABLE). EffectSkinning. */
    public void clearSkinnableFlag() {
        setInt(UpdateFields.UNIT_FIELD_FLAGS,
                getInt(UpdateFields.UNIT_FIELD_FLAGS) & ~UNIT_FLAG_SKINNABLE);
    }

    /** CMaNGOS Unit::CombatStop — leave combat, clear victim. */
    public void combatStop() {
        inCombat = false;
        victim = 0;
    }

    public int extraAttacks() {
        return extraAttacks;
    }

    /** CMaNGOS m_extraAttacks += damage, cap 5. */
    public void addExtraAttacks(int count) {
        if (!alive() || count <= 0) {
            return;
        }
        extraAttacks += count;
        if (extraAttacks > 5) {
            extraAttacks = 5;
        }
    }

    public int level = 1;
    public String name = "";
    public int faction;
    public int entry;
    public String scriptName = "";
    public final List<Aura> auras = new ArrayList<>();
    private final List<Unit> attackers = new ArrayList<>();

    /** CMaNGOS getAttackers — units currently attacking this one. */
    public void addAttacker(Unit attacker) {
        attackers.add(attacker);
    }

    public List<Unit> attackers() {
        return attackers;
    }

    /** CMaNGOS Player::SendFeignDeathResisted — empty SMSG_FEIGN_DEATH_RESISTED. */
    public void sendFeignDeathResisted() {
    }

    /** CMaNGOS ProcessDispelList — remove up to max auras; 0 means 1. */
    public int dispelAuras(int max) {
        int n = Math.max(1, max);
        int removed = 0;
        while (removed < n && !auras.isEmpty()) {
            auras.remove(auras.size() - 1);
            removed++;
        }
        return removed;
    }

    /** CMaNGOS EffectDispelMechanic — remove up to max auras with mechanic; 0 max means 1. */
    public int dispelMechanic(int mechanic, int max) {
        if (mechanic <= 0) {
            return 0;
        }
        int n = Math.max(1, max);
        int removed = 0;
        for (int i = auras.size() - 1; i >= 0 && removed < n; i--) {
            if (auras.get(i).mechanic() == mechanic) {
                auras.remove(i);
                removed++;
            }
        }
        return removed;
    }
    public long lastMeleeMs;
    public long lastOffhandMeleeMs;
    public int lastSwingError;
    public int threat;

    public Unit(int valueCount, int typeId) {
        super(valueCount, typeId);
        updateFlags = UPDATEFLAG_HIGHGUID | UPDATEFLAG_LIVING | UPDATEFLAG_HAS_POSITION;
        setFloat(UpdateFields.OBJECT_FIELD_SCALE_X, 1.0f);
        setFloat(UpdateFields.UNIT_MOD_CAST_SPEED, 1.0f);
        setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, 1.0f);
        setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, 3.0f);
    }

    @Override
    public void relocate(float x, float y, float z, float o) {
        super.relocate(x, y, z, o);
        movement.x = x;
        movement.y = y;
        movement.z = z;
        movement.o = o;
    }

    /** CMaNGOS Unit::GetAngle + SetFacingTo / SetOrientation. EffectDistract. */
    public void setFacingTo(float destX, float destY) {
        relocate(x, y, z, (float) Math.atan2(destY - y, destX - x));
    }

    public int health() {
        return getInt(UpdateFields.UNIT_FIELD_HEALTH);
    }

    public int maxHealth() {
        return getInt(UpdateFields.UNIT_FIELD_MAXHEALTH);
    }

    public void setHealth(int h) {
        setInt(UpdateFields.UNIT_FIELD_HEALTH, Math.max(0, Math.min(h, maxHealth() == 0 ? h : maxHealth())));
    }

    public int power() {
        return getInt(UpdateFields.UNIT_FIELD_POWER1);
    }

    public int maxPower() {
        return getInt(UpdateFields.UNIT_FIELD_MAXPOWER1);
    }

    public void setPower(int v) {
        setInt(UpdateFields.UNIT_FIELD_POWER1, Math.max(0, Math.min(v, maxPower() == 0 ? v : maxPower())));
    }

    public boolean alive() {
        return health() > 0;
    }

    /**
     * Auras 61 and 239 — recompute OBJECT_FIELD_SCALE_X from both sums.
     * {@code max(0.1, (100+scale)/100 * (100+scale2)/100)}.
     */
    public void setScaleAura(boolean scale2, int spellId, int amount, boolean apply) {
        Map<Integer, Integer> map = scale2 ? scale2Auras : scaleAuras;
        if (apply) {
            map.put(spellId, amount);
        } else {
            map.remove(spellId);
        }
        int sum = 0;
        for (int value : scaleAuras.values()) {
            sum += value;
        }
        int sum2 = 0;
        for (int value : scale2Auras.values()) {
            sum2 += value;
        }
        float scale = Math.max(0.1f, (100f + sum) / 100f * (100f + sum2) / 100f);
        setFloat(UpdateFields.OBJECT_FIELD_SCALE_X, scale);
    }

    /**
     * Aura 10 — ApplyPercentModFloatVar on each school bit. −100 is clamped to −99.99 so the
     * modifier cannot become zero.
     */
    public void applyThreatPercent(int schoolMask, float amount, boolean apply) {
        float val = amount == -100f ? -99.99f : amount;
        float factor = apply ? (100f + val) / 100f : 100f / (100f + val);
        boolean any = false;
        for (int school = 0; school < threatModifier.length; school++) {
            if ((schoolMask & (1 << school)) == 0) {
                continue;
            }
            threatModifier[school] *= factor;
            any = true;
        }
        if (!any) {
            return;
        }
        if (apply) {
            threatModAuraCount++;
        } else {
            threatModAuraCount = Math.max(0, threatModAuraCount - 1);
        }
    }

    public float threatModifier(int school) {
        if (school < 0 || school >= threatModifier.length) {
            return 1f;
        }
        return threatModifier[school];
    }

    /**
     * CMaNGOS Unit::ApplyTotalThreatModifier. No aura, or a zero school mask, leaves threat as-is.
     * Otherwise the first school bit in the mask selects the multiplier.
     */
    public float applyTotalThreatModifier(float threat, int schoolMask) {
        if (threatModAuraCount <= 0 || schoolMask == 0) {
            return threat;
        }
        int school = 0;
        for (int i = 0; i < threatModifier.length; i++) {
            if ((schoolMask & (1 << i)) != 0) {
                school = i;
                break;
            }
        }
        return threat * threatModifier[school];
    }

    /** Damage threat after {@link #applyTotalThreatModifier}, on both the list and the legacy total. */
    public void creditDamageThreat(Creature victim, float raw, int schoolMask) {
        float threat = applyTotalThreatModifier(raw, schoolMask);
        victim.threatManager.add(this, threat);
        victim.threat += Math.round(threat);
    }

    /** Aura 39. The immune flag drops only when no school-immunity aura remains. */
    public void setSchoolImmunity(int spellId, int schoolMask, boolean apply) {
        if (apply) {
            schoolImmunity.put(spellId, schoolMask);
        } else {
            schoolImmunity.remove(spellId);
        }
        int flags = getInt(UpdateFields.UNIT_FIELD_FLAGS);
        if (schoolImmunity.isEmpty()) {
            setInt(UpdateFields.UNIT_FIELD_FLAGS, flags & ~UNIT_FLAG_IMMUNE);
        } else {
            setInt(UpdateFields.UNIT_FIELD_FLAGS, flags | UNIT_FLAG_IMMUNE);
        }
    }

    public boolean isSchoolImmune(int schoolMask) {
        if (schoolMask == 0) {
            return false;
        }
        for (int mask : schoolImmunity.values()) {
            if ((mask & schoolMask) != 0) {
                return true;
            }
        }
        return false;
    }

    /** Aura 77. */
    public void setMechanicImmunity(int spellId, int mechanic, boolean apply) {
        if (apply) {
            mechanicImmunity.put(spellId, mechanic);
        } else {
            mechanicImmunity.remove(spellId);
        }
    }

    public boolean isImmuneToMechanic(int mechanic) {
        return mechanicImmunity.containsValue(mechanic);
    }

    /** Aura 37. */
    public void setEffectImmunity(int spellId, int effect, boolean apply) {
        if (apply) {
            effectImmunity.put(spellId, effect);
        } else {
            effectImmunity.remove(spellId);
        }
    }

    public boolean isImmuneToEffect(int effect) {
        return effectImmunity.containsValue(effect);
    }

    /** Aura 40. */
    public void setDamageImmunity(int spellId, int schoolMask, boolean apply) {
        if (apply) {
            damageImmunity.put(spellId, schoolMask);
        } else {
            damageImmunity.remove(spellId);
        }
    }

    public boolean isImmuneToDamage(int schoolMask) {
        if (schoolMask == 0) {
            return false;
        }
        for (int mask : damageImmunity.values()) {
            if ((mask & schoolMask) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Aura 243. The first apply saves UNIT_FIELD_FACTIONTEMPLATE; every unapply restores that
     * saved value (CMaNGOS RestoreOriginalFaction, no stack).
     */
    public void overrideFaction(int templateId, boolean apply) {
        if (templateId <= 0) {
            return;
        }
        if (apply) {
            if (!factionOverrideActive) {
                savedFactionTemplate = getInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE);
                savedFactionId = faction;
                factionOverrideActive = true;
            }
            faction = templateId;
            setInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE, templateId);
            return;
        }
        if (!factionOverrideActive) {
            return;
        }
        faction = savedFactionId;
        setInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE, savedFactionTemplate);
        factionOverrideActive = false;
    }

    /**
     * CMaNGOS Unit::SetStandState → RemoveAurasWithInterruptFlags(STANDING_CANCELS) when leaving
     * a seated state. Wired by WorldSession to SpellEngine.
     */
    public java.util.function.Consumer<Unit> leaveSeatedAuras;

    public void sit() {
        setStandState(UNIT_STAND_STATE_SIT);
    }

    public void stand() {
        setStandState(UNIT_STAND_STATE_STAND);
    }

    public boolean isStanding() {
        return standState() == UNIT_STAND_STATE_STAND;
    }

    /** CMaNGOS Unit::IsSitState — sit / chair (v1: UNIT_STAND_STATE_SIT). */
    public boolean isSitState() {
        return standState() == UNIT_STAND_STATE_SIT;
    }

    /** CMaNGOS Unit::IsSeatedState — anything but stand or sleep. */
    public boolean isSeatedState() {
        int s = standState();
        return s != UNIT_STAND_STATE_SLEEP && s != UNIT_STAND_STATE_STAND;
    }

    public int standState() {
        return getInt(UpdateFields.UNIT_FIELD_BYTES_1) & 0xFF;
    }

    /**
     * Client-requested anim (CMSG_STANDSTATECHANGE). The client already played it, so
     * CMaNGOS SetStandState(state, acknowledge=true) does not echo SMSG_STANDSTATE_UPDATE.
     */
    public void applyStandState(int state) {
        setStandState(state, true);
    }

    private void setStandState(int state) {
        setStandState(state, false);
    }

    private void setStandState(int state, boolean acknowledge) {
        int next = state & 0xFF;
        if (standState() == next) {
            return;
        }
        int bytes = getInt(UpdateFields.UNIT_FIELD_BYTES_1);
        setInt(UpdateFields.UNIT_FIELD_BYTES_1, (bytes & ~0xFF) | next);
        if (!isSeatedState() && leaveSeatedAuras != null) {
            leaveSeatedAuras.accept(this);
        }
        if (!acknowledge && this instanceof Player player && player.session != null) {
            WowBuffer stand = new WowBuffer(1);
            stand.putU8(next);
            player.session.send(Opcodes.SMSG_STANDSTATE_UPDATE, stand.array());
        }
    }

    /** UNIT_FIELD_BYTES_1 byte 3 (UNIT_BYTES_1_OFFSET_MISC_FLAGS). */
    public void setBytes1MiscFlags(int flags) {
        int bytes = getInt(UpdateFields.UNIT_FIELD_BYTES_1);
        setInt(UpdateFields.UNIT_FIELD_BYTES_1, (bytes & 0x00FFFFFF) | ((flags & 0xFF) << 24));
    }

    /** UNIT_FIELD_BYTES_2 byte 3 — CMaNGOS SetShapeshiftForm. */
    public int shapeshiftForm() {
        return (getInt(UpdateFields.UNIT_FIELD_BYTES_2) >>> 24) & 0xFF;
    }

    public void setShapeshiftForm(int form) {
        int bytes = getInt(UpdateFields.UNIT_FIELD_BYTES_2);
        setInt(UpdateFields.UNIT_FIELD_BYTES_2, (bytes & 0x00FFFFFF) | ((form & 0xFF) << 24));
    }

    public boolean hasAura(int spellId) {
        for (Aura a : auras) {
            if (a.spellId() == spellId) {
                return true;
            }
        }
        return false;
    }

    public record Aura(int spellId, int durationMs, int stacks, int mechanic, long expireAtMs,
                       int amplitudeMs, long nextTickAtMs, long casterGuid) {
        public Aura(int spellId, int durationMs, int stacks) {
            this(spellId, durationMs, stacks, 0, 0, 0, 0, 0);
        }

        public Aura(int spellId, int durationMs, int stacks, int mechanic) {
            this(spellId, durationMs, stacks, mechanic, 0, 0, 0, 0);
        }

        public Aura(int spellId, int durationMs, int stacks, int mechanic, long expireAtMs) {
            this(spellId, durationMs, stacks, mechanic, expireAtMs, 0, 0, 0);
        }

        public Aura withNextTick(long nextTickAtMs) {
            return new Aura(spellId, durationMs, stacks, mechanic, expireAtMs, amplitudeMs, nextTickAtMs, casterGuid);
        }
    }
}
