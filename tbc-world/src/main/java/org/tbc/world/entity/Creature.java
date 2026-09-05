package org.tbc.world.entity;

import org.tbc.world.ai.EventAi;
import org.tbc.world.ai.MotionMaster;
import org.tbc.world.ai.UnitAI;
import org.tbc.world.combat.ThreatManager;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.script.BossScript;

public final class Creature extends Unit {
    public static final int TYPEMASK_UNIT = 0x0009;
    /** C++ CREATURE_EXTRA_FLAG_GUARD */
    public static final int CREATURE_EXTRA_FLAG_GUARD = 0x00000400;
    /** C++ CREATURE_EXTRA_FLAG_NO_AGGRO_ON_SIGHT */
    public static final int CREATURE_EXTRA_FLAG_NO_AGGRO_ON_SIGHT = 0x00000002;
    public int spawnId;
    public int respawnDelayMs = 300_000;
    public long respawnAtMs;
    public float spawnX, spawnY, spawnZ, spawnO;
    public int npcFlags;
    public String aiName = "";
    public UnitAI ai;
    public EventAi eventAi;
    public BossScript script;
    public boolean pet;
    public boolean playerControlledPet;
    public boolean charmer;
    public boolean temporarySummon;
    public boolean totem;
    public int extraFlags;
    public boolean neutralToAll;
    /** CMaNGOS Creature m_detectionRange default / GetDetectionRange. */
    public float detectionRange = 18f;
    public boolean evading;
    public long evadeHomeAtMs;
    public long taggedBy;
    public boolean lootable;
    public int lootGold;
    public final java.util.List<org.tbc.world.loot.LootSlot> lootItems = new java.util.ArrayList<>();
    public long combatStartMs;
    public long lastHitMs;
    public int meleeCooldownMs;
    public final MotionMaster motion = new MotionMaster();
    public final ThreatManager threatManager = new ThreatManager();
    public boolean combatMovement = true;
    public int movementType;
    public float spawnDist;

    public Creature() {
        super(UpdateFields.UNIT_END, TYPEID_UNIT);
        setInt(UpdateFields.OBJECT_FIELD_TYPE, TYPEMASK_UNIT);
        setFloat(UpdateFields.OBJECT_FIELD_SCALE_X, 1.0f);
    }

    public void applyTemplate(int entry, String name, int display, int faction, int hp, int level) {
        this.entry = entry;
        this.name = name;
        this.level = level;
        this.faction = faction;
        setGuid(UpdateFields.OBJECT_FIELD_GUID, guid);
        setInt(UpdateFields.OBJECT_FIELD_ENTRY, entry);
        setInt(UpdateFields.UNIT_FIELD_DISPLAYID, display);
        setInt(UpdateFields.UNIT_FIELD_NATIVEDISPLAYID, display);
        setInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE, faction);
        setInt(UpdateFields.UNIT_FIELD_LEVEL, level);
        setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, hp);
        setInt(UpdateFields.UNIT_FIELD_HEALTH, hp);
        applyCombatStats(1f, 3f, 2000, 1.5f);
    }

    public void applyCombatStats(float minDmg, float maxDmg, int attackTime, float reach) {
        if (minDmg <= 0f && maxDmg <= 0f) {
            minDmg = 1f;
            maxDmg = 3f;
        } else if (maxDmg < minDmg) {
            maxDmg = minDmg;
        }
        if (attackTime <= 0) {
            attackTime = 2000;
        }
        if (reach <= 0f) {
            reach = 1.5f;
        }
        setFloat(UpdateFields.UNIT_FIELD_MINDAMAGE, minDmg);
        setFloat(UpdateFields.UNIT_FIELD_MAXDAMAGE, maxDmg);
        setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME, attackTime);
        setFloat(UpdateFields.UNIT_FIELD_COMBATREACH, reach);
    }

    public double spawnDistance2d(float px, float py) {
        double dx = px - spawnX;
        double dy = py - spawnY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public void startOocMotion() {
        if (movementType == MotionMaster.RANDOM && spawnDist > 0) {
            motion.moveRandom(spawnDist);
        }
    }
}
