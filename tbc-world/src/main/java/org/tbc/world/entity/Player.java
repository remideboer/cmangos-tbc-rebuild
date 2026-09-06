package org.tbc.world.entity;

import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.WorldSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Player extends Unit {
    public static final int TYPEMASK_PLAYER = 0x0019;
    public static final int AT_LOGIN_FIRST = 0x20;
    public static final int REST_STATE_NORMAL = 0x02;
    public static final int PLAYER_CONTROLLED_DEBUFF_LIMIT = 40;
    public static final int EQUIPMENT_SLOT_END = 19;
    public static final int EQUIPMENT_SLOT_MAINHAND = 15;
    public static final int EQUIPMENT_SLOT_OFFHAND = 16;
    public static final int INVENTORY_SLOT_BAG_START = 19;
    public static final int INVENTORY_SLOT_BAG_END = 23;
    public static final int INVENTORY_SLOT_ITEM_START = 23;
    public static final int INVENTORY_SLOT_ITEM_END = 39;
    public static final int BANK_SLOT_ITEM_START = 39;
    public static final int BANK_SLOT_ITEM_END = 67;
    /** Player.h BANK_SLOT_BAG_START / BANK_SLOT_BAG_END. */
    public static final int BANK_SLOT_BAG_START = 67;
    public static final int BANK_SLOT_BAG_END = 74;
    public static final int MAX_VISIBLE_ITEM_OFFSET = 16;
    public static final int POWER_RAGE = 1;
    public static final int POWER_RAGE_MAX = 1000;
    /** ChrClasses.dbc. CLASSMASK_WAND_USERS in SharedDefines.h. spec/03-protocol/enums.md */
    public static final int CLASS_PRIEST = 5;
    public static final int CLASS_MAGE = 8;
    public static final int CLASS_WARLOCK = 9;
    public static final int PLAYER_FLAGS_GHOST = 0x00000010;
    /** Player.h PLAYER_FLAGS_PVP_DESIRED — permanent PvP preference. */
    public static final int PLAYER_FLAGS_PVP_DESIRED = 0x00000200;

    public WorldSession session;
    public int accountId;
    public long selection;
    public long lootGuid;
    private int gossipMenuId;
    private int[] gossipOptionIds;
    private int[] gossipActionMenus;
    private int[] gossipActionPois;
    public int race;
    public int clazz;
    public int gender;
    private int drunk;

    public int drunkValue() {
        return drunk;
    }

    /** CMaNGOS Player::SetDrunkValue — PLAYER_BYTES_3 low 16: gender | (drunk & 0xFFFE). */
    public void setDrunkValue(int newDrunk) {
        if (newDrunk < 0) {
            newDrunk = 0;
        }
        if (newDrunk > 0xFFFF) {
            newDrunk = 0xFFFF;
        }
        drunk = newDrunk;
        int bytes = getInt(UpdateFields.PLAYER_BYTES_3);
        int packed = (gender & 0xFF) | (drunk & 0xFFFE);
        setInt(UpdateFields.PLAYER_BYTES_3, (bytes & ~0xFFFF) | packed);
    }

    /** CMaNGOS ReputationMgr::ModifyReputation — standing += amount. */
    public void modifyReputation(int factionId, int amount) {
        if (factionId <= 0 || amount == 0) {
            return;
        }
        reputation.merge(factionId, amount, Integer::sum);
    }

    public int reputationStanding(int factionId) {
        return reputation.getOrDefault(factionId, 0);
    }
    public int skin, face, hairStyle, hairColor, facialHair;
    public int money;
    public int xp;
    public int cinematic;
    public int atLogin;
    public int difficulty;
    /** CMaNGOS m_bgData.joinPos — saved when porting into a BG. */
    public int bgEntryMap;
    public float bgEntryX, bgEntryY, bgEntryZ, bgEntryO;
    public boolean hasBgEntry;
    public int guildId;
    /** CMaNGOS m_enteredInstances — account hourly new-instance cap (Player.h). */
    public static final int NEW_INSTANCE_LIMIT_PER_HOUR = 5;
    public final java.util.Map<Integer, Long> enteredInstances = new java.util.HashMap<>();
    public int guildIdInvited;
    public int guildRank;
    public int guildRankRights;
    public boolean guildLeader;
    public int gmLevel;
    public int team; // 67 horde / 469 alliance
    public int bindMap, bindZone;
    public float bindX, bindY, bindZ;

    /** CMaNGOS Player::SetHomebindToLocation — hearth map/area/xyz. */
    public void setHomebindToLocation(int mapId, int areaId, float x, float y, float z) {
        bindMap = mapId;
        bindZone = areaId;
        bindX = x;
        bindY = y;
        bindZ = z;
        dirty = true;
    }
    public boolean online;
    public boolean ghost;
    public long ghostTimeMs;
    /** 0 = inactive; else wall clock when KillPlayer timer forces repop. */
    public long deathTimerEndsAtMs;
    public final java.util.Set<Long> afkReporterGuids = new java.util.HashSet<>();
    public long logoutAtMs;
    public boolean logoutRequest;
    public int restFlags;
    public final int[] actionButtons = new int[132];
    public final List<Integer> spells = new ArrayList<>();
    private final java.util.Set<Integer> unlearnableSkills = new java.util.HashSet<>();
    public final int[] tut = new int[8];
    public final Map<Integer, Item> items = new HashMap<>();
    /** CMaNGOS ReputationMgr standing keyed by Faction.dbc id (spell EffectMiscValue). */
    private final Map<Integer, Integer> reputation = new HashMap<>();
    public final List<Integer> knownTitles = new ArrayList<>();
    public int honorPoints;
    public int arenaPoints;
    public int watchedFaction;
    public int powerType;
    public int displayId;
    public boolean dirty;
    public int nextSaveMs;
    public int timeSyncCounter;
    public long nextTimeSyncMs;
    public int zoneClient;
    public Pet pet;
    public Group group;
    public TradeData trade;
    public final List<Friend> friends = new ArrayList<>();
    public String motdLine = "";
    public boolean resting;
    public float restBonus;
    public final Map<Integer, Item> buyback = new HashMap<>();
    public final int[] questLogId = new int[25];
    public final int[] questLogState = new int[25];

    /**
     * CMaNGOS Player::AreaExploredOrEventHappens — mark quest complete in log
     * (SPELL_EFFECT_QUEST_COMPLETE). Does not turn in.
     */
    public void areaExploredOrEventHappens(int questId) {
        if (questId == 0) {
            return;
        }
        for (int i = 0; i < questLogId.length; i++) {
            if (questLogId[i] == questId) {
                questLogState[i] = 1;
                return;
            }
        }
    }

    /** CMaNGOS Player::FailQuest — QUEST_STATE_FAIL 0x2 (quest.md). Stays in log. */
    public void failQuest(int questId) {
        if (questId == 0) {
            return;
        }
        for (int i = 0; i < questLogId.length; i++) {
            if (questLogId[i] == questId) {
                questLogState[i] = 0x2;
                return;
            }
        }
    }
    public int comboPoints;
    private long comboTargetGuid;

    public int comboPoints() {
        return comboPoints;
    }

    /** CMaNGOS Unit::AddComboPoints — same target accumulates, else reset; clamp 0..5. */
    public void addComboPoints(Unit target, int count) {
        if (target == null || count == 0) {
            return;
        }
        if (target.guid == comboTargetGuid) {
            comboPoints += count;
        } else {
            comboTargetGuid = target.guid;
            comboPoints = count;
        }
        if (comboPoints > 5) {
            comboPoints = 5;
        }
        if (comboPoints < 0) {
            comboPoints = 0;
        }
    }

    public int selectedTitle;
    public boolean pvpFlagged;
    /** Sanctuary zone — HandleTogglePvP ignores (AREA_FLAG_SANCTUARY stand-in until AreaTable). */
    public boolean pvpSanctuary;
    public Player duelOpponent;
    public int duelPhase;
    public Corpse corpse;
    public long deleteDateMs;
    /** Pending resurrect from SMSG_RESURRECT_REQUEST (Player.cpp m_resurrect*). */
    public long resurrectGuid;
    public int resurrectMap;
    public float resurrectX, resurrectY, resurrectZ;
    public int resurrectHealth;
    public int resurrectMana;

    /** CMaNGOS Player::AddResurrectRequest — pending SMSG_RESURRECT_REQUEST data. */
    public void addResurrectRequest(long casterGuid, int mapId, float x, float y, float z, int health, int mana) {
        if (resurrectGuid != 0) {
            return;
        }
        resurrectGuid = casterGuid;
        resurrectMap = mapId;
        resurrectX = x;
        resurrectY = y;
        resurrectZ = z;
        resurrectHealth = health;
        resurrectMana = mana;
    }
    public int taxiPath;
    public final int[] taxiMask = new int[16];
    public boolean mounted;
    public String lfgComment = "";
    public int instanceId;
    public int bgTypeId;
    public String guildName = "";
    public Item guildBankItem;
    public int guildBankTabs;
    public boolean looking;
    public boolean channeling;

    /** CMaNGOS Unit::InterruptSpell — stop current channeled/generic cast. */
    public void interruptCast() {
        channeling = false;
    }

    public boolean nextMeleeSwingQueued;
    private int nextMeleeBonus;
    /** Camera viewpoint guid; 0 = self (CMSG_FAR_SIGHT / Camera::SetView). */
    private long cameraViewGuid;
    /** Pending ritual/GM summon (Player::m_summon_*). */
    public long summonerGuid;
    public long summonExpireMs;
    public int summonMapId;
    public float summonX, summonY, summonZ;
    public int afkReports;
    public int arenaTeam;
    public int arenaTeamId2, arenaTeamId3, arenaTeamId5;
    /** Pending arena invite team id (Player::m_ArenaTeamIdInvited). */
    public int arenaTeamIdInvited;
    public int honorToday;
    public int honorYesterday;
    public int yesterdayContrib;
    public final long[] totems = new long[4];

    /** CMaNGOS Totem::UnSummon on every MAX_TOTEM_SLOT. */
    public void destroyAllTotems() {
        Arrays.fill(totems, 0);
    }

    /** CMaNGOS Player::DurabilityPointsLoss — floor 0. */
    public void durabilityPointsLoss(Item item, int points) {
        if (item == null || points <= 0) {
            return;
        }
        item.durability = Math.max(0, item.durability - points);
    }

    /**
     * CMaNGOS DurabilityPointsLossAll. Equipped always; backpack (and bags) when inventory.
     */
    public void durabilityPointsLossAll(int points, boolean inventory) {
        for (int s = 0; s < EQUIPMENT_SLOT_END; s++) {
            Item it = itemAt(0, s);
            if (it != null) {
                durabilityPointsLoss(it, points);
            }
        }
        if (!inventory) {
            return;
        }
        for (int s = INVENTORY_SLOT_ITEM_START; s < INVENTORY_SLOT_ITEM_END; s++) {
            Item it = itemAt(0, s);
            if (it != null) {
                durabilityPointsLoss(it, points);
            }
        }
    }
    public float lastAckSpeed;
    /** Last CMSG_MOVE_SPLINE_DONE movementCounter (movement.md). */
    public int lastSplineDoneCounter;

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
        applyLanguageSkills();
        movement.x = x;
        movement.y = y;
        movement.z = z;
        movement.o = o;
        applyEquippedVisuals();
    }

    public void setGhost(boolean g) {
        ghost = g;
        int flags = getInt(UpdateFields.PLAYER_FLAGS);
        if (g) {
            flags |= PLAYER_FLAGS_GHOST;
        } else {
            flags &= ~PLAYER_FLAGS_GHOST;
        }
        setInt(UpdateFields.PLAYER_FLAGS, flags);
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

    public int firstFreeBagSlot() {
        for (int s = INVENTORY_SLOT_ITEM_START; s < INVENTORY_SLOT_ITEM_END; s++) {
            if (itemAt(0, s) == null) {
                return s;
            }
        }
        return -1;
    }

    public int firstFreeBankSlot() {
        for (int s = BANK_SLOT_ITEM_START; s < BANK_SLOT_ITEM_END; s++) {
            if (itemAt(0, s) == null) {
                return s;
            }
        }
        return -1;
    }

    public static final class Friend {
        public long guid;
        public int flags = 1;
        public String note = "";
    }

    public static final class TradeData {
        public Player partner;
        public boolean accepted;
        public int gold;
        public final Item[] slots = new Item[7];
    }

    public boolean isWandUser() {
        return clazz == CLASS_PRIEST || clazz == CLASS_MAGE || clazz == CLASS_WARLOCK;
    }

    public boolean hasOffhandWeapon() {
        for (Item it : items.values()) {
            if (it.bag == 0 && it.slot == EQUIPMENT_SLOT_OFFHAND) {
                return true;
            }
        }
        return false;
    }

    public void queueNextMeleeSwing() {
        queueNextMeleeSwing(0);
    }

    public void queueNextMeleeSwing(int bonusDamage) {
        nextMeleeSwingQueued = true;
        nextMeleeBonus = bonusDamage;
    }

    public boolean hasNextMeleeSwingQueued() {
        return nextMeleeSwingQueued;
    }

    public int consumeNextMeleeSwing() {
        int bonus = nextMeleeBonus;
        nextMeleeSwingQueued = false;
        nextMeleeBonus = 0;
        return bonus;
    }

    public int queuedNextMeleeBonus() {
        return nextMeleeSwingQueued ? nextMeleeBonus : 0;
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

    /** PLAYER_FARSIGHT — set by SPELL_EFFECT_ADD_FARSIGHT; CMSG_FAR_SIGHT reads it. */
    public long farSightGuid() {
        return getGuid(UpdateFields.PLAYER_FARSIGHT);
    }

    public void setFarSightGuid(long guid) {
        setGuid(UpdateFields.PLAYER_FARSIGHT, guid);
    }

    public long cameraViewGuid() {
        return cameraViewGuid;
    }

    /** Camera::SetView / ResetView with update_far_sight_field=false (CMSG_FAR_SIGHT). */
    public void setCameraViewGuid(long guid) {
        cameraViewGuid = guid;
    }

    /** Offer a summon destination (SMSG_SUMMON_REQUEST path). */
    public void offerSummon(long summoner, int mapId, float x, float y, float z, long expireAtMs) {
        summonerGuid = summoner;
        summonMapId = mapId;
        summonX = x;
        summonY = y;
        summonZ = z;
        summonExpireMs = expireAtMs;
    }

    /**
     * CMSG_SUMMON_RESPONSE — CMaNGOS SummonIfPossible. Returns true when teleport should run.
     */
    public boolean summonIfPossible(boolean agree, long summoner, long nowMs) {
        if (summoner != summonerGuid) {
            return false;
        }
        if (!agree) {
            summonExpireMs = 0;
            return false;
        }
        if (summonExpireMs < nowMs) {
            return false;
        }
        summonExpireMs = 0;
        summonerGuid = 0;
        return true;
    }

    /** PLAYER_BYTES_2 byte 2. CMaNGOS GetBankBagSlotCount. */
    public int bankBagSlotCount() {
        return (getInt(UpdateFields.PLAYER_BYTES_2) >> 16) & 0xFF;
    }

    /** PLAYER_BYTES_2 byte 2. CMaNGOS SetBankBagSlotCount. */
    public void setBankBagSlotCount(int count) {
        int pb2 = getInt(UpdateFields.PLAYER_BYTES_2);
        pb2 = (pb2 & ~(0xFF << 16)) | ((count & 0xFF) << 16);
        setInt(UpdateFields.PLAYER_BYTES_2, pb2);
    }

    public int createSelfFlags() {
        return PLAYER_CREATE_FLAGS;
    }

    public int observerFlags() {
        return UPDATEFLAG_HIGHGUID | UPDATEFLAG_LIVING | UPDATEFLAG_HAS_POSITION;
    }

    /** CMaNGOS PLAYER_SKILL_INDEX / MAKE_PAIR32(id, step) / MAKE_SKILL_VALUE. */
    public void setSkill(int slot, int skillId, int value, int max) {
        setSkill(slot, skillId, value, max, 0);
    }

    public void setSkill(int slot, int skillId, int value, int max, int step) {
        if (slot < 0 || slot >= 127) {
            return;
        }
        int base = UpdateFields.PLAYER_SKILL_INFO_1_1 + slot * 3;
        setInt(base, (skillId & 0xFFFF) | ((step & 0xFFFF) << 16));
        setInt(base + 1, (value & 0xFFFF) | ((max & 0xFFFF) << 16));
    }

    public boolean hasSkill(int skillId) {
        int want = skillId & 0xFFFF;
        for (int slot = 0; slot < 127; slot++) {
            if ((getInt(UpdateFields.PLAYER_SKILL_INFO_1_1 + slot * 3) & 0xFFFF) == want) {
                return true;
            }
        }
        return false;
    }

    public void learnSkill(int skillId, int value, int max, int step) {
        learnSkill(skillId, value, max, step, false);
    }

    /** When canUnlearn, CMSG_UNLEARN_SKILL may clear the skill (SkillRaceClassInfo SKILL_FLAG_CAN_UNLEARN). */
    public void learnSkill(int skillId, int value, int max, int step, boolean canUnlearn) {
        int free = -1;
        int want = skillId & 0xFFFF;
        for (int slot = 0; slot < 127; slot++) {
            int id = getInt(UpdateFields.PLAYER_SKILL_INFO_1_1 + slot * 3) & 0xFFFF;
            if (id == want) {
                setSkill(slot, skillId, value, max, step);
                if (canUnlearn) {
                    unlearnableSkills.add(want);
                }
                return;
            }
            if (free < 0 && id == 0) {
                free = slot;
            }
        }
        if (free >= 0) {
            setSkill(free, skillId, value, max, step);
            if (canUnlearn) {
                unlearnableSkills.add(want);
            }
        }
    }

    /** SetSkillStep(id, 0) — clear skill when SKILL_FLAG_CAN_UNLEARN. */
    public boolean unlearnSkill(int skillId) {
        int want = skillId & 0xFFFF;
        if (!unlearnableSkills.contains(want)) {
            return false;
        }
        for (int slot = 0; slot < 127; slot++) {
            int base = UpdateFields.PLAYER_SKILL_INFO_1_1 + slot * 3;
            if ((getInt(base) & 0xFFFF) == want) {
                setSkill(slot, 0, 0, 0, 0);
                unlearnableSkills.remove(want);
                return true;
            }
        }
        return false;
    }

    void applyLanguageSkills() {
        for (int skill : org.tbc.world.content.ChrStatic.languageSkills(race)) {
            learnSkill(skill, 300, 300, 0);
        }
        for (int spell : org.tbc.world.content.ChrStatic.languageSpells(race)) {
            if (!spells.contains(spell)) {
                spells.add(spell);
            }
        }
    }

    /** Domain setup: both players are in an active duel (logout.md cantLogout). */
    public void engageDuel(Player other) {
        if (other == null) {
            return;
        }
        duelOpponent = other;
        other.duelOpponent = this;
        duelPhase = 1;
        other.duelPhase = 1;
    }

    /** Domain setup: taxi flight in progress (logout.md InstantLogout). */
    public void startTaxiFlight(int pathId) {
        taxiPath = pathId;
    }

    public void clearTaxiFlight() {
        taxiPath = 0;
    }

    /**
     * Board an MO transport (movement.md type 15). Sets MOVEFLAG_ONTRANSPORT + t_guid.
     * In-memory worlds must seed the GO; DB {@code transports.period} is not loaded here.
     */
    public void boardMoTransport(GameObject transport) {
        if (!org.tbc.world.spell.GameObjectUse.isMoTransport(transport)) {
            return;
        }
        movement.moveFlags |= org.tbc.world.net.wow8606.MovementInfo.MOVEFLAG_ONTRANSPORT;
        movement.transportGuid = transport.guid;
        movement.tx = 0;
        movement.ty = 0;
        movement.tz = 0;
        movement.to = 0;
        movement.tTime = transport.pathProgress;
    }

    public void leaveMoTransport() {
        movement.moveFlags &= ~org.tbc.world.net.wow8606.MovementInfo.MOVEFLAG_ONTRANSPORT;
        movement.transportGuid = 0;
        movement.tTime = 0;
    }

    public boolean taxiKnown(int node) {
        if (node < 1) {
            return false;
        }
        int field = (node - 1) / 32;
        int bit = 1 << ((node - 1) % 32);
        return field < taxiMask.length && (taxiMask[field] & bit) == bit;
    }

    public void learnTaxi(int node) {
        if (node < 1) {
            return;
        }
        int field = (node - 1) / 32;
        if (field < taxiMask.length) {
            taxiMask[field] |= 1 << ((node - 1) % 32);
        }
    }

    public void prepareGossipMenu(int menuId, int[] optionIds) {
        prepareGossipMenu(menuId, optionIds, null, null);
    }

    public void prepareGossipMenu(int menuId, int[] optionIds, int[] actionMenus) {
        prepareGossipMenu(menuId, optionIds, actionMenus, null);
    }

    public void prepareGossipMenu(int menuId, int[] optionIds, int[] actionMenus, int[] actionPois) {
        gossipMenuId = menuId;
        gossipOptionIds = optionIds == null ? new int[0] : optionIds.clone();
        gossipActionMenus = actionMenus == null ? new int[gossipOptionIds.length] : actionMenus.clone();
        gossipActionPois = actionPois == null ? new int[gossipOptionIds.length] : actionPois.clone();
    }

    public boolean hasGossipOption(int menuId, int listId) {
        return gossipOptionIds != null && menuId == gossipMenuId
                && listId >= 0 && listId < gossipOptionIds.length;
    }

    public int gossipOptionId(int listId) {
        return gossipOptionIds[listId];
    }

    public int gossipActionMenu(int listId) {
        return gossipActionMenus[listId];
    }

    public int gossipActionPoi(int listId) {
        return gossipActionPois[listId];
    }

    /** CMaNGOS Player::CanEnterNewInstance — GM skip; re-enter same id free; else size < 5/hour. */
    public boolean canEnterNewInstance(int instanceId) {
        if (gmLevel > 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        enteredInstances.entrySet().removeIf(e -> e.getValue() < now);
        if (enteredInstances.containsKey(instanceId)) {
            return true;
        }
        return enteredInstances.size() < NEW_INSTANCE_LIMIT_PER_HOUR;
    }

    /** CMaNGOS Player::AddNewInstanceId — records id for one hour. */
    public void addNewInstanceId(int instanceId) {
        if (!enteredInstances.containsKey(instanceId)) {
            enteredInstances.put(instanceId, System.currentTimeMillis() + 3_600_000L);
        }
    }
}
