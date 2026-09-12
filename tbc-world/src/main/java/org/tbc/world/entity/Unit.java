package org.tbc.world.entity;

import org.tbc.world.net.wow8606.MovementInfo;
import org.tbc.world.net.wow8606.UpdateFields;

import java.util.ArrayList;
import java.util.List;

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
    public static final int UNIT_FLAG_UNTARGETABLE = 0x00010000;
    public static final int UNIT_FLAG_STUNNED = 0x00040000;
    public static final int UNIT_FLAG_IN_COMBAT = 0x00080000;
    public static final int UNIT_FLAG_TAXI_FLIGHT = 0x00100000;
    public static final int UNIT_FLAG_UNINTERACTIBLE = 0x02000000;
    public static final int UNIT_FLAG_SKINNABLE = 0x04000000;
    /** Unit.h UNIT_FLAG_PREVENT_ANIM — Feign Death. */
    public static final int UNIT_FLAG_PREVENT_ANIM = 0x20000000;
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
    /** SpellDefines.h ShapeshiftForm — UNIT_FIELD_BYTES_2 byte 3. */
    public static final int FORM_NONE = 0;
    public static final int FORM_BATTLESTANCE = 0x11;

    public MovementInfo movement = new MovementInfo();
    public long victim;
    public boolean inCombat;
    private int extraAttacks;
    private boolean rooted;
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

    public void sit() {
        setStandState(UNIT_STAND_STATE_SIT);
    }

    public void stand() {
        setStandState(UNIT_STAND_STATE_STAND);
    }

    public boolean isStanding() {
        return standState() == UNIT_STAND_STATE_STAND;
    }

    public int standState() {
        return getInt(UpdateFields.UNIT_FIELD_BYTES_1) & 0xFF;
    }

    /** Allowed client animstates: stand/sit/sleep/kneel (spell.md CMSG_STANDSTATECHANGE). */
    public void applyStandState(int state) {
        setStandState(state);
    }

    private void setStandState(int state) {
        int bytes = getInt(UpdateFields.UNIT_FIELD_BYTES_1);
        setInt(UpdateFields.UNIT_FIELD_BYTES_1, (bytes & ~0xFF) | (state & 0xFF));
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
