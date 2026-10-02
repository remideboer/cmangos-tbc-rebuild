package org.tbc.world.entity;

import org.tbc.common.WowBuffer;
import org.tbc.world.net.wow8606.MovementInfo;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.ArrayList;
import java.util.List;
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
    /** SharedDefines.h POWER_MANA — UNIT_FIELD_BYTES_0 power byte / POWER1. */
    public static final int POWER_MANA = 0;
    /** ChrClasses.dbc CLASS_MAGE — creature UnitClass that uses mana. */
    public static final int CLASS_MAGE = 8;
    /** SharedDefines.h UNIT_DYNFLAG_DEAD. */
    public static final int UNIT_DYNFLAG_DEAD = 0x0020;
    /** SharedDefines.h UNIT_DYNFLAG_TRACK_UNIT — Hunter's Mark. */
    public static final int UNIT_DYNFLAG_TRACK_UNIT = 0x0002;
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
    /** CMaNGOS GetMaxPositiveAuraModifier(SPELL_AURA_MOD_INCREASE_SWIM_SPEED); 0 = none. */
    private int increaseSwimSpeedPct;
    /** CMaNGOS GetTotalAuraModifier(SPELL_AURA_SAFE_FALL) — yards subtracted from fall height. */
    private int safeFallBonus;
    /** CMaNGOS m_speed_rate[MOVE_RUN]; default 1.0. */
    private float runSpeedRate = 1.0f;
    /** CMaNGOS m_speed_rate[MOVE_SWIM]; default 1.0. */
    private float swimSpeedRate = 1.0f;
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
     * CMaNGOS Unit::SetFeignDeath success path — FLAGS_2 FEIGN_DEATH + DYNFLAG_DEAD.
     * PLAYER_CONTROLLED success → CombatStop (resist roll later; always success for now).
     * NPC never CombatStop (AttackStop / threat offline later).
     */
    public void setFeignDeath(boolean apply) {
        int f2 = getInt(UpdateFields.UNIT_FIELD_FLAGS_2);
        int dyn = getInt(UpdateFields.UNIT_DYNAMIC_FLAGS);
        if (apply) {
            if ((getInt(UpdateFields.UNIT_FIELD_FLAGS) & UNIT_FLAG_PLAYER_CONTROLLED) != 0) {
                combatStop();
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
        // Unit::UpdateSpeed MOVE_RUN: positive main_speed_mod then strongest slow.
        float rate = increaseSpeedPct != 0 ? (100.0f + increaseSpeedPct) / 100.0f : 1.0f;
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
        if (rate < 0.01f) {
            rate = 0.01f;
        }
        if (rate == swimSpeedRate) {
            return;
        }
        swimSpeedRate = rate;
        sendSwimSpeedChange();
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

    /** Allowed client animstates: stand/sit/sleep/kneel (spell.md CMSG_STANDSTATECHANGE). */
    public void applyStandState(int state) {
        setStandState(state);
    }

    private void setStandState(int state) {
        int next = state & 0xFF;
        if (standState() == next) {
            return;
        }
        int bytes = getInt(UpdateFields.UNIT_FIELD_BYTES_1);
        setInt(UpdateFields.UNIT_FIELD_BYTES_1, (bytes & ~0xFF) | next);
        if (!isSeatedState() && leaveSeatedAuras != null) {
            leaveSeatedAuras.accept(this);
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
