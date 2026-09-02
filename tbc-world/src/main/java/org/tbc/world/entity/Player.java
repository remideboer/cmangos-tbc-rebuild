package org.tbc.world.entity;

import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.WorldSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Player extends Unit {
    public static final int TYPEMASK_PLAYER = 0x0019;
    public static final int AT_LOGIN_FIRST = 0x20;
    public static final int REST_STATE_NORMAL = 0x02;
    public static final int PLAYER_CONTROLLED_DEBUFF_LIMIT = 40;
    public static final int EQUIPMENT_SLOT_END = 19;
    public static final int INVENTORY_SLOT_ITEM_START = 23;
    public static final int INVENTORY_SLOT_ITEM_END = 39;
    public static final int MAX_VISIBLE_ITEM_OFFSET = 16;
    public static final int POWER_RAGE = 1;
    public static final int POWER_RAGE_MAX = 1000;

    public WorldSession session;
    public int accountId;
    public long selection;
    public int race;
    public int clazz;
    public int gender;
    public int skin, face, hairStyle, hairColor, facialHair;
    public int money;
    public int xp;
    public int cinematic;
    public int atLogin;
    public int difficulty;
    public int guildId;
    public int gmLevel;
    public int team; // 67 horde / 469 alliance
    public int bindMap, bindZone;
    public float bindX, bindY, bindZ;
    public boolean online;
    public boolean ghost;
    public long logoutAtMs;
    public boolean logoutRequest;
    public int restFlags;
    public final int[] actionButtons = new int[132];
    public final List<Integer> spells = new ArrayList<>();
    public final int[] tut = new int[8];
    public final Map<Integer, Item> items = new HashMap<>();
    public final List<Integer> knownTitles = new ArrayList<>();
    public int honorPoints;
    public int arenaPoints;
    public int watchedFaction;
    public int powerType;
    public int displayId;
    public boolean dirty;
    public long firstSaveAtMs;
    public int timeSyncCounter;
    public long nextTimeSyncMs;
    public int zoneClient;
    public Pet pet;
    public Group group;
    public String motdLine = "";
    public boolean resting;
    public float restBonus;
    public final Map<Integer, Item> buyback = new HashMap<>();
    public final int[] questLogId = new int[25];
    public final int[] questLogState = new int[25];
    public int comboPoints;
    public int selectedTitle;
    public boolean pvpFlagged;
    public Player duelOpponent;
    public int duelPhase;
    public Corpse corpse;
    public int taxiPath;
    public boolean mounted;
    public String lfgComment = "";
    public int instanceId;
    public int bgTypeId;
    public int arenaTeamId2, arenaTeamId3, arenaTeamId5;

    public Player() {
        super(UpdateFields.PLAYER_END, TYPEID_PLAYER);
        setInt(UpdateFields.OBJECT_FIELD_TYPE, TYPEMASK_PLAYER);
        setFloat(UpdateFields.OBJECT_FIELD_SCALE_X, 1.0f);
        setFloat(UpdateFields.UNIT_MOD_CAST_SPEED, 1.0f);
        setInt(UpdateFields.UNIT_FIELD_FLAGS, UNIT_FLAG_PLAYER_CONTROLLED);
        setInt(UpdateFields.PLAYER_FIELD_WATCHED_FACTION_INDEX, -1);
        setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME, 2000);
        setInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME + 1, 2000);
        setInt(UpdateFields.UNIT_FIELD_RANGEDATTACKTIME, 2000);
    }

    public void applyCreateFields() {
        setGuid(UpdateFields.OBJECT_FIELD_GUID, guid);
        setInt(UpdateFields.OBJECT_FIELD_TYPE, TYPEMASK_PLAYER);
        setFloat(UpdateFields.OBJECT_FIELD_SCALE_X, 1.0f);
        int bytes0 = (race & 0xFF) | ((clazz & 0xFF) << 8) | ((gender & 0xFF) << 16) | ((powerType & 0xFF) << 24);
        setInt(UpdateFields.UNIT_FIELD_BYTES_0, bytes0);
        int pb = (skin & 0xFF) | ((face & 0xFF) << 8) | ((hairStyle & 0xFF) << 16) | ((hairColor & 0xFF) << 24);
        setInt(UpdateFields.PLAYER_BYTES, pb);
        int pb2 = (facialHair & 0xFF) | (REST_STATE_NORMAL << 24);
        setInt(UpdateFields.PLAYER_BYTES_2, pb2);
        setInt(UpdateFields.PLAYER_BYTES_3, gender & 0xFF);
        int sheath = itemAt(0, 15) != null ? 1 : 0;
        setInt(UpdateFields.UNIT_FIELD_BYTES_2, sheath | (PLAYER_CONTROLLED_DEBUFF_LIMIT << 8));
        setInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE, faction);
        setInt(UpdateFields.UNIT_FIELD_DISPLAYID, displayId);
        setInt(UpdateFields.UNIT_FIELD_NATIVEDISPLAYID, displayId);
        setInt(UpdateFields.UNIT_FIELD_FLAGS, UNIT_FLAG_PLAYER_CONTROLLED);
        setFloat(UpdateFields.UNIT_MOD_CAST_SPEED, 1.0f);
        setInt(UpdateFields.UNIT_FIELD_LEVEL, level);
        setInt(UpdateFields.PLAYER_FIELD_COINAGE, money);
        setInt(UpdateFields.PLAYER_FIELD_WATCHED_FACTION_INDEX, watchedFaction);
        if (powerType == POWER_RAGE) {
            setInt(UpdateFields.UNIT_FIELD_MAXPOWER2, POWER_RAGE_MAX);
        }
        movement.x = x;
        movement.y = y;
        movement.z = z;
        movement.o = o;
        applyEquippedVisuals();
    }

    @Override
    public int power() {
        return getInt(UpdateFields.UNIT_FIELD_POWER1 + powerType);
    }

    @Override
    public int maxPower() {
        return getInt(UpdateFields.UNIT_FIELD_MAXPOWER1 + powerType);
    }

    @Override
    public void setPower(int v) {
        int max = maxPower();
        setInt(UpdateFields.UNIT_FIELD_POWER1 + powerType, Math.max(0, Math.min(v, max == 0 ? v : max)));
    }

    /** CMaNGOS Player::RewardRage; stored rage is display×10 (max 1000). */
    public void rewardRageFromHit(int damage, boolean crit) {
        if (powerType != POWER_RAGE || damage <= 0) {
            return;
        }
        float conv = (0.0091107836f * level * level) + (3.225598133f * level) + 4.2652911f;
        float speed = getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME) / 1000.0f;
        float hitFactor = speed * (crit ? 7f : 3.5f);
        float addRage = (damage / conv * 7.5f + hitFactor) / 2.0f;
        setPower(power() + (int) (addRage * 10));
    }

    public Item itemAt(int bag, int slot) {
        for (Item it : items.values()) {
            if (it.bag == bag && it.slot == slot) {
                return it;
            }
        }
        return null;
    }

    public void applyEquippedVisuals() {
        for (Item it : items.values()) {
            if (it.bag != 0 || it.slot < 0 || it.slot >= EQUIPMENT_SLOT_END) {
                continue;
            }
            setGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2,
                    Guid.HIGH_ITEM | (Guid.low(it.guid) & 0xFFFFFFFFL));
            setInt(UpdateFields.PLAYER_VISIBLE_ITEM_1_0 + it.slot * MAX_VISIBLE_ITEM_OFFSET, it.entry);
        }
    }

    public void setMoney(int copper) {
        money = Math.max(0, copper);
        setInt(UpdateFields.PLAYER_FIELD_COINAGE, money);
    }

    public int createSelfFlags() {
        return PLAYER_CREATE_FLAGS;
    }

    public int observerFlags() {
        return UPDATEFLAG_HIGHGUID | UPDATEFLAG_LIVING | UPDATEFLAG_HAS_POSITION;
    }
}
