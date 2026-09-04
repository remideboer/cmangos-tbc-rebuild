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
    public boolean evading;
    public long evadeHomeAtMs;
    public long taggedBy;
    public boolean lootable;
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
        setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME, 2000);
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
