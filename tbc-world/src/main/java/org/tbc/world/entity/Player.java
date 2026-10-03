package org.tbc.world.entity;

import org.tbc.common.WowBuffer;
import org.tbc.world.classless.ClasslessCharacterPolicy;
import org.tbc.world.classless.ClasslessPowerAddon;
import org.tbc.world.content.Content;
import org.tbc.world.content.LevelStats;
import org.tbc.world.content.SkillLineAbility;
import org.tbc.world.content.WeaponSkills;
import org.tbc.world.net.wow8606.Opcodes;
import org.tbc.world.net.wow8606.UpdateFields;
import org.tbc.world.session.WorldSession;
import org.tbc.world.spell.AuraSlots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

public final class Player extends Unit {
    public static final int TYPEMASK_PLAYER = 0x0019;
    public static final int AT_LOGIN_RENAME = 0x01;
    public static final int AT_LOGIN_FIRST = 0x20;
    /** All primary trade skills may be learned (matches TrainerService.PRIMARY_PROFESSIONS). */
    public static final int MAX_PRIMARY_TRADE_SKILL = 11;
    public static final int REST_STATE_NORMAL = 0x02;
    public static final int PLAYER_CONTROLLED_DEBUFF_LIMIT = 40;
    /** ItemPrototype.h ItemClass — EquippedItemClass on proficiency spells. */
    public static final int ITEM_CLASS_WEAPON = 2;
    public static final int ITEM_CLASS_ARMOR = 4;
    public static final int EQUIPMENT_SLOT_END = 19;
    public static final int EQUIPMENT_SLOT_MAINHAND = 15;
    public static final int EQUIPMENT_SLOT_OFFHAND = 16;
    public static final int EQUIPMENT_SLOT_RANGED = 17;
    /** Player.h INVENTORY_SLOT_BAG_0 — backpack / paper-doll in CMSG bagIndex. */
    public static final int INVENTORY_SLOT_BAG_0 = 255;
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
    public static final int POWER_MANA = 0;
    public static final int POWER_RAGE = 1;
    public static final int POWER_RAGE_MAX = 1000;
    /** SharedDefines.h POWER_ENERGY / POWER_ENERGY_DEFAULT (Unit::GetCreatePowers for players). */
    public static final int POWER_ENERGY = 3;
    public static final int POWER_ENERGY_MAX = 100;
    /** ChrClasses unused TBC id — classless mode when ClasslessConfig.enabled. */
    public static final int CLASS_CLASSLESS = 6;
    /** ChrClasses.dbc. CLASSMASK_WAND_USERS in SharedDefines.h. spec/03-protocol/enums.md */
    public static final int CLASS_WARRIOR = 1;
    public static final int CLASS_PALADIN = 2;
    public static final int CLASS_HUNTER = 3;
    public static final int CLASS_PRIEST = 5;
    public static final int CLASS_MAGE = 8;
    public static final int CLASS_WARLOCK = 9;
    public static final int PLAYER_FLAGS_GHOST = 0x00000010;
    /** Player.h PLAYER_FLAGS_PVP_DESIRED — permanent PvP preference. */
    public static final int PLAYER_FLAGS_PVP_DESIRED = 0x00000200;
    /** Player.h PLAYER_FLAGS_HIDE_HELM. */
    public static final int PLAYER_FLAGS_HIDE_HELM = 0x00000400;
    /** Player.h PLAYER_FLAGS_HIDE_CLOAK. */
    public static final int PLAYER_FLAGS_HIDE_CLOAK = 0x00000800;
    /** Player.h PLAYER_FLAGS_TAXI_BENCHMARK — /timetest. */
    public static final int PLAYER_FLAGS_TAXI_BENCHMARK = 0x00020000;
    /** Player.h PLAYER_FIELD_BYTES_OFFSET_ACTION_BAR_TOGGLES. */
    public static final int PLAYER_FIELD_BYTES_OFFSET_ACTION_BAR_TOGGLES = 2;

    public WorldSession session;
    public int accountId;
    /** character_declinedname five cases; null if unset. */
    public String[] declinedNames;
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

    /** CMaNGOS ReputationMgr::ModifyReputation — standing += amount; list Standing when known. */
    public void modifyReputation(int factionId, int amount) {
        if (factionId <= 0 || amount == 0) {
            return;
        }
        reputation.merge(factionId, amount, Integer::sum);
        int listId = ReputationMgr.listIdForFaction(factionId);
        if (listId >= 0) {
            reputations.addStanding(listId, amount);
        }
    }

    public int reputationStanding(int factionId) {
        return reputation.getOrDefault(factionId, 0);
    }

    private int weaponProficiency;
    private int armorProficiency;

    public int weaponProficiency() {
        return weaponProficiency;
    }

    public int armorProficiency() {
        return armorProficiency;
    }

    /** CMaNGOS Player::AddWeaponProficiency. One-Handed Axes 196 mask 1. */
    public void addWeaponProficiency(int mask) {
        weaponProficiency |= mask;
    }

    /** Clear weapon proficiency bits (tests / unlearn paths). */
    public void clearWeaponProficiency(int mask) {
        weaponProficiency &= ~mask;
    }

    /** CMaNGOS Player::AddArmorProficiency. Plate Mail 750 mask 16. */
    public void addArmorProficiency(int mask) {
        armorProficiency |= mask;
    }

    /** Clear armor proficiency bits (tests / unlearn paths). */
    public void clearArmorProficiency(int mask) {
        armorProficiency &= ~mask;
    }
    public int skin, face, hairStyle, hairColor, facialHair;
    public int money;
    public int xp;
    /** CMaNGOS m_Played_time[PLAYED_TIME_TOTAL / PLAYED_TIME_LEVEL]. */
    public int totalPlayedTime;
    public int levelPlayedTime;
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
    /** Guild::GetGuildInviter — who sent the pending invite. */
    public long guildInviterGuid;
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
    /** WorldObject::m_GCDCatMap / m_cooldownMap — not persisted across the in-memory snapshot (GCD is 1.5 s). */
    public final org.tbc.world.spell.SpellCooldowns cooldowns = new org.tbc.world.spell.SpellCooldowns();
    private final java.util.Set<Integer> unlearnableSkills = new java.util.HashSet<>();
    private final List<Integer> dirtySkillFields = new ArrayList<>();
    /** CMaNGOS m_fishingSteps — UpdateFishingSkill grace counter. */
    public int fishingSteps;
    public final int[] tut = new int[8];
    public final Map<Integer, Item> items = new HashMap<>();
    /** ON_EQUIP spells currently applied from paper-doll (Player::ApplyItemEquipSpell). */
    private final Set<Integer> itemEquipSpellIds = new HashSet<>();
    /** CMaNGOS ReputationMgr standing keyed by Faction.dbc id (spell EffectMiscValue). */
    private final Map<Integer, Integer> reputation = new HashMap<>();
    /** CMaNGOS ReputationMgr list-id flags for SMSG_INITIALIZE_FACTIONS. */
    public final ReputationMgr reputations = new ReputationMgr();
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
    /** CMaNGOS WorldSession order counter — SMSG_FORCE_* orders the client ACKs in sequence (movement.md). */
    private int moveOrderCounter;
    public int zoneClient;
    public Pet pet;
    private Item spellItemTarget;
    private GameObject spellGameObjectTarget;
    public Group group;
    public TradeData trade;
    public final List<Friend> friends = new ArrayList<>();
    public String motdLine = "";
    public boolean resting;
    public float restBonus;
    public final Map<Integer, Item> buyback = new HashMap<>();
    public final int[] questLogId = new int[25];
    public final int[] questLogState = new int[25];
    /** PLAYER_QUEST_LOG_n_3 bytes: four kill/cast counters (SetQuestSlotCounter). */
    public final int[][] questLogCounts = new int[25][4];
    /** QuestStatusData.m_itemcount[0..3] — not packed into PLAYER_QUEST_LOG counters. */
    public final int[][] questLogItemCount = new int[25][4];
    /** character_queststatus.rewarded — quests turned in, no longer in the log. */
    public final Set<Integer> rewardedQuests = new HashSet<>();
    public final Set<Integer> dailyQuestDone = new HashSet<>();
    public final long[] questExpiry = new long[25];
    private final Map<Integer, Integer> killCredits = new HashMap<>();

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

    /**
     * CMaNGOS Player::KilledMonsterCredit — SPELL_EFFECT_KILL_CREDIT_GROUP
     * RewardPlayerAndGroupAtEventCredit(misc). Solo records the creature entry.
     */
    public void killedMonsterCredit(int creatureId) {
        if (creatureId <= 0) {
            return;
        }
        killCredits.merge(creatureId, 1, Integer::sum);
    }

    public int killCreditCount(int creatureId) {
        return killCredits.getOrDefault(creatureId, 0);
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
    /** Raw GO guid from SMSG_DUEL_REQUESTED. 0 when no arbiter is up. */
    public long duelArbiterGuid;
    private GameObject duelFlag;
    public int duelPhase;
    /** DuelHandler startTimer — milliseconds when the countdown began. 0 if not counting. */
    public long duelCountdownStartMs;
    /** Player::UpdateDuelFlag startTime — milliseconds when the duel became active. 0 before that. */
    public long duelStartedAtMs;
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
    private int lastPlayMusic;
    private int lastPlaySound;
    private Player recruitingFriend;
    private int lastTotemCreatedSlot;
    private long lastTotemCreatedGuid;
    private long lastSkinningLootGuid;
    private long lastInsigniaLootGuid;
    private int lastSkinnedRepop;
    private long lastPickpocketLootGuid;
    private long lastDisenchantLootGuid;
    private long lastProspectingLootGuid;
    private long lastOpenLockLootGuid;
    public final int[] taxiMask = new int[16];
    public boolean mounted;
    public String lfgComment = "";
    public int instanceId;
    public int bgTypeId;
    public String guildName = "";
    public Item guildBankItem;
    public int guildBankTabs;
    public boolean looking;
    /** CMaNGOS LfgInfo.autojoin — CMSG_LFG_SET_AUTOJOIN / CLEAR_AUTOJOIN. */
    public boolean lfgAutoJoin;
    public boolean channeling;

    /** CMaNGOS Unit::InterruptSpell — stop current channeled/generic cast. */
    public void interruptCast() {
        channeling = false;
    }

    public boolean nextMeleeSwingQueued;
    private int nextMeleeBonus;
    /** CURRENT_MELEE_SPELL id while SPELL_ATTR_ON_NEXT_SWING is preparing (Heroic Strike, etc.). */
    private int nextMeleeSpellId;
    private int nextMeleeCastCount;
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

    /** CMaNGOS Player::DurabilityLoss — percent of max durability, at least 1. */
    public void durabilityLoss(Item item, double percent) {
        if (item == null || item.maxDurability <= 0 || percent <= 0) {
            return;
        }
        int loss = (int) (item.maxDurability * percent);
        if (loss < 1) {
            loss = 1;
        }
        durabilityPointsLoss(item, loss);
    }

    /**
     * CMaNGOS Player::DurabilityLossAll. Equipped always; backpack (and bags) when inventory.
     */
    public void durabilityLossAll(double percent, boolean inventory) {
        for (int s = 0; s < EQUIPMENT_SLOT_END; s++) {
            Item it = itemAt(0, s);
            if (it != null) {
                durabilityLoss(it, percent);
            }
        }
        if (!inventory) {
            return;
        }
        for (int s = INVENTORY_SLOT_ITEM_START; s < INVENTORY_SLOT_ITEM_END; s++) {
            Item it = itemAt(0, s);
            if (it != null) {
                durabilityLoss(it, percent);
            }
        }
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

    /** Hero ability-point wallet; empty for ordinary classes. */
    public final org.tbc.world.classless.HeroStatAllocation heroStats =
            new org.tbc.world.classless.HeroStatAllocation();

    /** CMaNGOS m_createStats / GetCreateHealth — player_levelstats + player_classlevelstats for the level. */
    private final int[] createStats = new int[5];
    private LevelStats levelStats;
    private int createHealth;
    private int createMana;
    private int nextLevelXp;
    /** OCTRegenHPPerSpirit (health per second OOC) and PLAYER_FIELD_MOD_MANA_REGEN (mana per second). */
    private float hpRegenPerSecond;
    private float manaRegenPerSecond;
    /**
     * PLAYER_FIELD_MOD_MANA_REGEN_INTERRUPT — spirit×Meditation% + mp5 (0 without Meditation).
     * Used under the five-second rule and while in combat (same shared class pattern).
     */
    private float manaRegenInterruptPerSecond;
    /** CMaNGOS Unit::m_regenTimer / m_lastManaUseTimer (five-second rule). */
    private int regenTimerMs;
    private int lastManaUseTimerMs;

    public static final int REGEN_TIME_FULL = 2000;
    private static final int LAST_MANA_USE_MS = 5000;

    /**
     * CMaNGOS Player::InitStatsForLevel (the level-stats subset): remember the create values, then
     * write BASE_HEALTH/BASE_MANA/STAT0-4, armor = agi * 2, max health from stamina, max mana from
     * intellect, PLAYER_NEXT_LEVEL_XP and the regen rates (UpdateManaRegen); health and mana full.
     */
    public void initStatsForLevel(LevelStats ls) {
        levelStats = ls;
        LevelStats.ClassLevel cl = ls.classLevel(clazz, level);
        LevelStats.Stats st = ls.stats(race, clazz, level);
        createHealth = cl.baseHealth();
        createMana = cl.baseMana();
        nextLevelXp = ls.xpForLevel(level);
        for (int i = 0; i < 5; i++) {
            createStats[i] = st.stat(i);
        }
        hpRegenPerSecond = ls.hpRegenPerSpirit(clazz, level, st.spi());
        manaRegenPerSecond = (float) Math.sqrt(st.inte()) * ls.manaRegenPerSpirit(clazz, level, st.spi());
        // UpdateManaRegen: interrupt = mp5 + spirit×MOD_MANA_REGEN_INTERRUPT%; no Meditation → 0.
        manaRegenInterruptPerSecond = 0f;
        applyLevelStats();
        setInt(UpdateFields.UNIT_FIELD_HEALTH, maxHealth());
        setInt(UpdateFields.UNIT_FIELD_POWER1, getInt(UpdateFields.UNIT_FIELD_MAXPOWER1));
        setInt(UpdateFields.UNIT_FIELD_POWER4, getInt(UpdateFields.UNIT_FIELD_MAXPOWER4));
    }

    /**
     * CMaNGOS Player::GiveXP: SendLogXPGain, add rested bonus (kills only), GiveLevel while the total
     * passes PLAYER_NEXT_LEVEL_XP. Returns the update fields that changed so the caller can send VALUES.
     */
    public int[] giveXp(int amount, Creature victim) {
        if (amount < 1 || !alive() || level >= MAX_LEVEL || levelStats == null) {
            return new int[0];
        }
        int rest = 0;
        if (victim != null) {
            rest = Math.min((int) restBonus, amount);
            restBonus -= rest;
        }
        sendLogXpGain(amount, victim, rest);
        List<Integer> changed = new ArrayList<>();
        changed.add(UpdateFields.PLAYER_XP);
        int newXp = xp + amount + rest;
        while (nextLevelXp > 0 && newXp >= nextLevelXp && level < MAX_LEVEL) {
            newXp -= nextLevelXp;
            giveLevel(level + 1, changed);
        }
        xp = newXp;
        setInt(UpdateFields.PLAYER_XP, xp);
        return changed.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Player::SendLogXPGain: victim guid, total xp, type (0 kill / 1 other), [xp without rest, group rate], RaF. */
    private void sendLogXpGain(int amount, Creature victim, int rest) {
        if (session == null) {
            return;
        }
        WowBuffer b = new WowBuffer(22);
        b.putU64(victim != null ? victim.guid : 0);
        b.putU32(amount + rest);
        b.putU8(victim != null ? 0 : 1);
        if (victim != null) {
            b.putU32(amount);
            b.putFloat(1.0f);
        }
        b.putU8(0);
        session.send(Opcodes.SMSG_LOG_XPGAIN, b.array());
    }

    /**
     * Player::GiveLevel: SMSG_LEVELUP_INFO with the deltas to the new create values, then level, stats,
     * PLAYER_NEXT_LEVEL_XP, full health/mana (InitStatsForLevel) and InitTalentForLevel (+1 from level 10).
     */
    private void giveLevel(int newLevel, List<Integer> changed) {
        LevelStats.ClassLevel cl = levelStats.classLevel(clazz, newLevel);
        LevelStats.Stats st = levelStats.stats(race, clazz, newLevel);
        boolean hero = ClasslessCharacterPolicy.isClassless(this);
        if (session != null) {
            WowBuffer b = new WowBuffer(48);
            b.putU32(newLevel);
            if (hero) {
                for (int i = 0; i < 11; i++) {
                    b.putU32(0);
                }
            } else {
                b.putU32(cl.baseHealth() - createHealth);
                b.putU32(cl.baseMana() - createMana);
                for (int i = 1; i < 5; i++) {
                    b.putU32(0);
                }
                for (int i = 0; i < 5; i++) {
                    b.putU32(st.stat(i) - createStats[i]);
                }
            }
            session.send(Opcodes.SMSG_LEVELUP_INFO, b.array());
        }
        int fromLevel = level;
        level = newLevel;
        setInt(UpdateFields.UNIT_FIELD_LEVEL, level);
        if (hero) {
            heroStats.awardGain(levelStats, race, fromLevel, newLevel);
            ClasslessCharacterPolicy.applyStartingStats(this, levelStats);
            ClasslessCharacterPolicy.refreshCombatSkillMax(this);
            // Immediate StatUpdate so the character-pane addon shows new unspent without relog.
            ClasslessPowerAddon.pushStats(session);
        } else {
            initStatsForLevel(levelStats);
        }
        if (level >= TALENT_START_LEVEL) {
            setInt(UpdateFields.PLAYER_CHARACTER_POINTS1, getInt(UpdateFields.PLAYER_CHARACTER_POINTS1) + 1);
        }
        for (int f : LEVEL_UP_FIELDS) {
            if (!changed.contains(f)) {
                changed.add(f);
            }
        }
    }

    public static final int MAX_LEVEL = 70;
    /** CalculateTalentsPoints: talents start at level 10, one per level. */
    private static final int TALENT_START_LEVEL = 10;
    private static final int[] LEVEL_UP_FIELDS = {
        UpdateFields.UNIT_FIELD_LEVEL, UpdateFields.PLAYER_NEXT_LEVEL_XP,
        UpdateFields.UNIT_FIELD_BASE_HEALTH, UpdateFields.UNIT_FIELD_BASE_MANA,
        UpdateFields.UNIT_FIELD_STAT0, UpdateFields.UNIT_FIELD_STAT1, UpdateFields.UNIT_FIELD_STAT2,
        UpdateFields.UNIT_FIELD_STAT3, UpdateFields.UNIT_FIELD_STAT4, UpdateFields.UNIT_FIELD_RESISTANCES,
        UpdateFields.UNIT_FIELD_MAXHEALTH, UpdateFields.UNIT_FIELD_HEALTH,
        UpdateFields.UNIT_FIELD_MAXPOWER1, UpdateFields.UNIT_FIELD_POWER1,
        UpdateFields.UNIT_FIELD_MAXPOWER2, UpdateFields.UNIT_FIELD_POWER2,
        UpdateFields.UNIT_FIELD_MAXPOWER4, UpdateFields.UNIT_FIELD_POWER4,
        UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN, UpdateFields.PLAYER_CHARACTER_POINTS1,
    };

    /** Spell::TakePower → SetLastManaUse: spirit-based mana regen pauses for 5 s (MOD_MANA_REGEN_INTERRUPT). */
    public void noteManaUse() {
        lastManaUseTimerMs = LAST_MANA_USE_MS;
    }

    /**
     * CMaNGOS Player::Update → RegenerateAll every REGEN_TIME_FULL: health and rage decay only out of
     * combat; mana uses MOD_MANA_REGEN OOC, MOD_MANA_REGEN_INTERRUPT in combat or under the five-second
     * rule. Returns the unit fields that changed. Ghosts/corpses skip regen.
     */
    public int[] regenerateAll(int diff) {
        if (ghost || !alive()) {
            return new int[0];
        }
        lastManaUseTimerMs = Math.max(0, lastManaUseTimerMs - diff);
        regenTimerMs += diff;
        if (regenTimerMs < REGEN_TIME_FULL) {
            return new int[0];
        }
        int d = regenTimerMs / 100 * 100;
        regenTimerMs -= d;
        List<Integer> changed = new ArrayList<>();
        if (!inCombat) {
            if (regenerateHealth(d)) {
                changed.add(UpdateFields.UNIT_FIELD_HEALTH);
            }
            if (regenerateRage(d)) {
                changed.add(UpdateFields.UNIT_FIELD_POWER2);
            }
        }
        if (regenerateEnergy(d)) {
            changed.add(UpdateFields.UNIT_FIELD_POWER4);
        }
        if (regenerateMana(d)) {
            changed.add(UpdateFields.UNIT_FIELD_POWER1);
        }
        return changed.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Player::Regenerate(POWER_ENERGY): uint32(diff / 100) * Rate.Power.Energy 1.0, in and out of combat. */
    private boolean regenerateEnergy(int diff) {
        int cur = getInt(UpdateFields.UNIT_FIELD_POWER4);
        int max = getInt(UpdateFields.UNIT_FIELD_MAXPOWER4);
        if (cur >= max) {
            return false;
        }
        setInt(UpdateFields.UNIT_FIELD_POWER4, Math.min(max, cur + diff / 100));
        return true;
    }

    /** Player::RegenerateHealth: OCTRegenHPPerSpirit * diff / 1000 (×1.5 while sitting). */
    private boolean regenerateHealth(int diff) {
        int cur = health();
        if (cur >= maxHealth()) {
            return false;
        }
        float add = hpRegenPerSecond;
        if (!isStanding()) {
            add *= 1.5f;
        }
        int gain = (int) (add * diff / 1000f);
        if (gain == 0) {
            return false;
        }
        setHealth(cur + gain);
        return true;
    }

    /**
     * Player::Regenerate(POWER_MANA): MOD_MANA_REGEN OOC; MOD_MANA_REGEN_INTERRUPT while in combat
     * or under the five-second rule (TakePower / noteManaUse).
     */
    private boolean regenerateMana(int diff) {
        int cur = power();
        if (cur >= maxPower()) {
            return false;
        }
        boolean interrupted = lastManaUseTimerMs > 0 || inCombat;
        float rate = interrupted ? manaRegenInterruptPerSecond : manaRegenPerSecond;
        int gain = (int) (rate * (diff / 1000));
        if (gain == 0) {
            return false;
        }
        setPower(cur + gain);
        return true;
    }

    /** Player::Regenerate(POWER_RAGE): uint32(diff / 200) * 2.5 stored rage lost per tick, floor 0. */
    private boolean regenerateRage(int diff) {
        int cur = rage();
        if (cur == 0) {
            return false;
        }
        int loss = (int) ((diff / 200) * 2.5f);
        setRage(Math.max(0, cur - loss));
        return true;
    }

    /** UNIT_FIELD_POWER2 in tenths (client shows rage / 10). */
    public int rage() {
        return getInt(UpdateFields.UNIT_FIELD_POWER2);
    }

    public void setRage(int rage10) {
        setInt(UpdateFields.UNIT_FIELD_POWER2, Math.max(0, Math.min(rage10, POWER_RAGE_MAX)));
    }

    /**
     * Classless create stats from {@code ClasslessConfig} HP/mana + race-average abilities.
     * Opens mana + rage + energy pools; wires XP bar ({@code PLAYER_NEXT_LEVEL_XP}) via LevelStats.
     */
    public void applyClasslessCreateStats(int hp, int mana, int str, int agi, int sta, int inte, int spi) {
        applyClasslessCreateStats(LevelStats.defaults(), hp, mana, str, agi, sta, inte, spi);
    }

    public void applyClasslessCreateStats(LevelStats ls, int hp, int mana, int str, int agi, int sta, int inte, int spi) {
        createHealth = hp;
        createMana = mana;
        createStats[0] = str;
        createStats[1] = agi;
        createStats[2] = sta;
        createStats[3] = inte;
        createStats[4] = spi;
        powerType = POWER_MANA;
        if (ls == null) {
            ls = LevelStats.defaults();
        }
        levelStats = ls;
        if (level < 1) {
            level = 1;
        }
        nextLevelXp = ls.xpForLevel(level);
        hpRegenPerSecond = ls.hpRegenPerSpirit(CLASS_CLASSLESS, level, spi);
        manaRegenPerSecond = (float) Math.sqrt(inte) * ls.manaRegenPerSpirit(CLASS_CLASSLESS, level, spi);
        manaRegenInterruptPerSecond = 0f;
        applyLevelStats();
        setInt(UpdateFields.UNIT_FIELD_MAXPOWER2, POWER_RAGE_MAX);
        setInt(UpdateFields.UNIT_FIELD_POWER2, 0);
        setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, POWER_ENERGY_MAX);
        setInt(UpdateFields.UNIT_FIELD_POWER4, POWER_ENERGY_MAX);
        setInt(UpdateFields.UNIT_FIELD_HEALTH, maxHealth());
        setInt(UpdateFields.UNIT_FIELD_POWER1, getInt(UpdateFields.UNIT_FIELD_MAXPOWER1));
    }

    /** Re-writes the level-stat fields from the remembered create values (also after a persist copy). */
    private void applyLevelStats() {
        if (createHealth == 0) {
            return;
        }
        setInt(UpdateFields.UNIT_FIELD_BASE_HEALTH, createHealth);
        setInt(UpdateFields.UNIT_FIELD_BASE_MANA, createMana);
        for (int i = 0; i < 5; i++) {
            setInt(UpdateFields.UNIT_FIELD_STAT0 + i, createStats[i]);
        }
        setInt(UpdateFields.UNIT_FIELD_RESISTANCES, createStats[1] * 2);
        setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, createHealth + healthBonusFromStamina(createStats[2]));
        // Unit::UpdateMaxPower(POWER_MANA): create mana + intellect bonus; basemana 0 keeps the bar hidden.
        setInt(UpdateFields.UNIT_FIELD_MAXPOWER1,
                createMana == 0 ? 0 : createMana + manaBonusFromIntellect(createStats[3]));
        // InitStatsForLevel SetMaxPower(GetCreatePowers) — only the class's own bar is shown by the client.
        if (powerType == POWER_RAGE) {
            setInt(UpdateFields.UNIT_FIELD_MAXPOWER2, POWER_RAGE_MAX);
        }
        if (powerType == POWER_ENERGY) {
            setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, POWER_ENERGY_MAX);
        }
        if (clazz == CLASS_CLASSLESS) {
            setInt(UpdateFields.UNIT_FIELD_MAXPOWER2, POWER_RAGE_MAX);
            setInt(UpdateFields.UNIT_FIELD_MAXPOWER4, POWER_ENERGY_MAX);
        }
        setInt(UpdateFields.PLAYER_NEXT_LEVEL_XP, nextLevelXp);
        setInt(UpdateFields.PLAYER_XP, xp);
        setFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN, manaRegenPerSecond);
        setFloat(UpdateFields.PLAYER_FIELD_MOD_MANA_REGEN_INTERRUPT, manaRegenInterruptPerSecond);
        // InitStatsForLevel: zero POS/NEG, PCT=1.00 for MAX_SPELL_SCHOOL (client $SPH / PaperDoll).
        for (int i = 0; i < 7; i++) {
            setInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + i, 0);
            setInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_NEG + i, 0);
            setFloat(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_PCT + i, 1.0f);
        }
    }

    /**
     * CMaNGOS Player::UpdateSpellDamageBonus — advertised spell damage for client tip tokens
     * ({@code $SPH} = holy = school 1). Combat SoR reads {@link #holySpellPower()}.
     */
    public void updateSpellDamageBonusDone(int holy, int fire, int nature, int frost, int shadow, int arcane) {
        setInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + 1, holy);
        setInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + 2, fire);
        setInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + 3, nature);
        setInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + 4, frost);
        setInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + 5, shadow);
        setInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + 6, arcane);
    }

    /** Holy school advertised bonus ({@code $SPH} / SoR coeff). */
    public int holySpellPower() {
        return getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_POS + 1)
                - getInt(UpdateFields.PLAYER_FIELD_MOD_DAMAGE_DONE_NEG + 1);
    }

    /**
     * CMaNGOS Player::_ApplyItemBonuses (primary stats + armor) then UpdateStats
     * STAT_STAMINA → Unit::UpdateMaxHealth and STAT_INTELLECT → UpdateMaxPower(POWER_MANA).
     * STAT0–4, RESISTANCES, MAXHEALTH, and MAXPOWER1 from create stats plus the equipped-item
     * totals. Idempotent; the caller recomputes the extras. Armor is create-and-gear
     * agility ×2 plus item armor. Mana classes only (createMana 0 keeps the bar hidden).
     */
    public void applyGearBonuses(int stamina, int armor, int agility, int strength, int intellect, int spirit) {
        applyGearBonuses(stamina, armor, agility, strength, intellect, spirit, 0, 0);
    }

    /**
     * Same as {@link #applyGearBonuses(int, int, int, int, int, int)} plus ITEM_MOD_HEALTH /
     * ITEM_MOD_MANA ({@code HandleStatModifier(UNIT_MOD_HEALTH/MANA)}).
     */
    public void applyGearBonuses(int stamina, int armor, int agility, int strength, int intellect, int spirit,
            int itemHealth, int itemMana) {
        setInt(UpdateFields.UNIT_FIELD_STAT0, createStats[0] + strength);
        setInt(UpdateFields.UNIT_FIELD_STAT1, createStats[1] + agility);
        setInt(UpdateFields.UNIT_FIELD_STAT2, createStats[2] + stamina);
        setInt(UpdateFields.UNIT_FIELD_STAT3, createStats[3] + intellect);
        setInt(UpdateFields.UNIT_FIELD_STAT4, createStats[4] + spirit);
        setInt(UpdateFields.UNIT_FIELD_RESISTANCES, (createStats[1] + agility) * 2 + armor);
        if (createHealth != 0) {
            int maxHealth = createHealth + healthBonusFromStamina(createStats[2] + stamina) + itemHealth;
            setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, Math.max(1, maxHealth));
            if (health() > maxHealth()) {
                setHealth(maxHealth());
            }
        }
        if (createMana != 0) {
            int maxMana = createMana + manaBonusFromIntellect(createStats[3] + intellect) + itemMana;
            setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, Math.max(0, maxMana));
            if (power() > maxPower()) {
                setPower(maxPower());
            }
        }
    }

    /**
     * Player::ApplyItemEquipSpell / RemoveAurasDueToItemSpell — apply ON_EQUIP spells
     * that are on the paper-doll and clear those that left it. Does not touch cast auras.
     */
    public void syncItemEquipAuras(Set<Integer> wanted) {
        for (int id : new ArrayList<>(itemEquipSpellIds)) {
            if (!wanted.contains(id)) {
                int slot = AuraSlots.slotOf(this, id);
                if (slot >= 0) {
                    AuraSlots.clearVisible(this, slot);
                }
                itemEquipSpellIds.remove(id);
            }
        }
        for (int id : wanted) {
            itemEquipSpellIds.add(id);
            if (AuraSlots.slotOf(this, id) < 0) {
                AuraSlots.applyVisible(this, id, level, 1);
            }
        }
    }

    /** CMaNGOS Unit::UpdateMaxHealth after stamina mods (create HP + bonus from current STAT_STAMINA). */
    public void recalculateMaxHealthFromStamina() {
        if (createHealth == 0) {
            return;
        }
        int sta = getInt(UpdateFields.UNIT_FIELD_STAT2);
        int max = createHealth + healthBonusFromStamina(sta);
        if (max < 1) {
            max = 1;
        }
        setInt(UpdateFields.UNIT_FIELD_MAXHEALTH, max);
        if (health() > max) {
            setHealth(max);
        }
    }

    /** CMaNGOS UpdateMaxPower(POWER_MANA) after intellect mods. */
    public void recalculateMaxManaFromIntellect() {
        if (createMana == 0) {
            return;
        }
        int inte = getInt(UpdateFields.UNIT_FIELD_STAT3);
        int max = createMana + manaBonusFromIntellect(inte);
        if (max < 0) {
            max = 0;
        }
        setInt(UpdateFields.UNIT_FIELD_MAXPOWER1, max);
        if (getInt(UpdateFields.UNIT_FIELD_POWER1) > max) {
            setInt(UpdateFields.UNIT_FIELD_POWER1, max);
        }
    }

    /** CMaNGOS Unit::GetHealthBonusFromStamina: first 20 stamina 1 hp each, then 10 hp per point. */
    static int healthBonusFromStamina(int stamina) {
        int base = Math.min(stamina, 20);
        return base + (stamina - base) * 10;
    }

    /** CMaNGOS Unit::GetManaBonusFromIntellect: first 20 intellect 1 mana each, then 15 per point. */
    static int manaBonusFromIntellect(int intellect) {
        int base = Math.min(intellect, 20);
        return base + (intellect - base) * 15;
    }

    /**
     * Spend Hero unspent points into a base stat (0=str .. 4=spi). Refuses ordinary classes
     * and invalid amounts. Preserves current HP and mana percentages.
     */
    public boolean spendHeroStat(int stat, int amount) {
        if (!ClasslessCharacterPolicy.isClassless(this)) {
            return false;
        }
        int oldHp = health();
        int oldMaxHp = maxHealth();
        int oldMana = getInt(UpdateFields.UNIT_FIELD_POWER1);
        int oldMaxMana = getInt(UpdateFields.UNIT_FIELD_MAXPOWER1);
        if (!heroStats.spend(stat, amount)) {
            return false;
        }
        ClasslessCharacterPolicy.applyStartingStats(this, levelStats);
        restoreResourcePercent(oldHp, oldMaxHp, oldMana, oldMaxMana);
        return true;
    }

    public void restoreResourcePercent(int oldHp, int oldMaxHp, int oldMana, int oldMaxMana) {
        if (oldMaxHp > 0) {
            setHealth(Math.max(1, Math.round(oldHp * (float) maxHealth() / oldMaxHp)));
        }
        int newMaxMana = getInt(UpdateFields.UNIT_FIELD_MAXPOWER1);
        if (oldMaxMana > 0 && newMaxMana > 0) {
            setInt(UpdateFields.UNIT_FIELD_POWER1,
                    Math.min(newMaxMana, Math.round(oldMana * (float) newMaxMana / oldMaxMana)));
        }
    }

    /** Persist copy (CharacterStore snapshot) — the create values are not update fields the DB stores. */
    public void copyCreateStatsFrom(Player src) {
        levelStats = src.levelStats;
        createHealth = src.createHealth;
        createMana = src.createMana;
        nextLevelXp = src.nextLevelXp;
        hpRegenPerSecond = src.hpRegenPerSecond;
        manaRegenPerSecond = src.manaRegenPerSecond;
        manaRegenInterruptPerSecond = src.manaRegenInterruptPerSecond;
        System.arraycopy(src.createStats, 0, createStats, 0, 5);
        heroStats.copyFrom(src.heroStats);
    }

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
        // SHEATH_STATE_UNARMED (0): client draws equipped weapons/shield on back/hip until CMSG_SETSHEATHED.
        int shapeshift = shapeshiftForm();
        setInt(UpdateFields.UNIT_FIELD_BYTES_2,
                (PLAYER_CONTROLLED_DEBUFF_LIMIT << 8) | (shapeshift << 24));
        setInt(UpdateFields.UNIT_FIELD_FACTIONTEMPLATE, faction);
        setInt(UpdateFields.UNIT_FIELD_DISPLAYID, displayId);
        setInt(UpdateFields.UNIT_FIELD_NATIVEDISPLAYID, displayId);
        setInt(UpdateFields.UNIT_FIELD_FLAGS, UNIT_FLAG_PLAYER_CONTROLLED);
        setFloat(UpdateFields.UNIT_MOD_CAST_SPEED, 1.0f);
        setInt(UpdateFields.UNIT_FIELD_LEVEL, level);
        setInt(UpdateFields.PLAYER_FIELD_COINAGE, money);
        setInt(UpdateFields.PLAYER_FIELD_WATCHED_FACTION_INDEX, watchedFaction);
        applyLevelStats();
        applyLanguageSkills();
        movement.x = x;
        movement.y = y;
        movement.z = z;
        movement.o = o;
        applyEquippedVisuals();
    }

    public static final int PLAYER_EXPLORED_ZONES_SIZE = 128;

    /**
     * CMaNGOS Player::CheckAreaExploreAndOutdoor — set PLAYER_EXPLORED_ZONES bit for the map area
     * exploreFlag so the client fog-of-war uncovers.
     *
     * @return field index that changed, or -1 if already known / invalid
     */
    public int exploreAreaFlag(int areaFlag) {
        if (areaFlag == 0 || areaFlag == 0xFFFF) {
            return -1;
        }
        int offset = areaFlag / 32;
        if (offset >= PLAYER_EXPLORED_ZONES_SIZE) {
            return -1;
        }
        int bit = 1 << (areaFlag % 32);
        int field = UpdateFields.PLAYER_EXPLORED_ZONES_1 + offset;
        int curr = getInt(field);
        if ((curr & bit) != 0) {
            return -1;
        }
        setInt(field, curr | bit);
        return field;
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
        boolean classless = org.tbc.world.classless.ClasslessCharacterPolicy.isClassless(this);
        if ((!classless && powerType != POWER_RAGE) || damage <= 0) {
            return;
        }
        float conv = (0.0091107836f * level * level) + (3.225598133f * level) + 4.2652911f;
        float speed = getInt(UpdateFields.UNIT_FIELD_BASEATTACKTIME) / 1000.0f;
        float hitFactor = speed * (crit ? 7f : 3.5f);
        float addRage = (damage / conv * 7.5f + hitFactor) / 2.0f;
        int add = (int) (addRage * 10);
        if (classless) {
            setRage(rage() + add);
        } else {
            setPower(power() + add);
        }
    }

    public Item itemAt(int bag, int slot) {
        if (bag == INVENTORY_SLOT_BAG_0) {
            bag = 0;
        }
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

    /**
     * CMaNGOS Player::GetWeaponForAttack(OFF_ATTACK) — only ITEM_CLASS_WEAPON.
     * Shields/holdables in the offhand slot must not drive auto-attack LEFTSWING.
     */
    public boolean hasOffhandWeapon() {
        for (Item it : items.values()) {
            if (it.bag == 0 && it.slot == EQUIPMENT_SLOT_OFFHAND) {
                return it.itemClass == ITEM_CLASS_WEAPON;
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

    /**
     * Spell::prepare for ON_NEXT_SWING — queue CURRENT_MELEE_SPELL; cast() lands on the next mainhand swing.
     */
    public void queueNextMeleeSpell(int spellId, int castCount, int bonusDamage) {
        nextMeleeSwingQueued = true;
        nextMeleeSpellId = spellId;
        nextMeleeCastCount = castCount;
        nextMeleeBonus = bonusDamage;
    }

    public boolean hasNextMeleeSwingQueued() {
        return nextMeleeSwingQueued;
    }

    public boolean hasNextMeleeSpellQueued() {
        return nextMeleeSwingQueued && nextMeleeSpellId != 0;
    }

    public int peekNextMeleeSpellId() {
        return nextMeleeSpellId;
    }

    public int peekNextMeleeCastCount() {
        return nextMeleeCastCount;
    }

    public int consumeNextMeleeSwing() {
        int bonus = nextMeleeBonus;
        nextMeleeSwingQueued = false;
        nextMeleeBonus = 0;
        nextMeleeSpellId = 0;
        nextMeleeCastCount = 0;
        return bonus;
    }

    public int queuedNextMeleeBonus() {
        return nextMeleeSwingQueued ? nextMeleeBonus : 0;
    }

    public void applyEquippedVisuals() {
        for (Item it : items.values()) {
            if (it.bag != 0 || it.slot < 0) {
                continue;
            }
            setGuid(UpdateFields.PLAYER_FIELD_INV_SLOT_HEAD + it.slot * 2,
                    Guid.HIGH_ITEM | (Guid.low(it.guid) & 0xFFFFFFFFL));
            if (it.slot < EQUIPMENT_SLOT_END) {
                setVisibleItemSlot(it.slot, it);
            }
        }
        refreshSheath();
    }

    /**
     * CMaNGOS Player::SetVisibleItemSlot — paper-doll / 3D model entry for an equipment slot.
     * Pass {@code null} to clear (unequip).
     */
    public void setVisibleItemSlot(int slot, Item item) {
        if (slot < 0 || slot >= EQUIPMENT_SLOT_END) {
            return;
        }
        int base = UpdateFields.PLAYER_VISIBLE_ITEM_1_0 + slot * MAX_VISIBLE_ITEM_OFFSET;
        setInt(base, item != null ? item.entry : 0);
    }

    /**
     * UNIT_FIELD_BYTES_2 sheath byte after equip change.
     * Do not auto-draw (SHEATH_STATE_MELEE): CMaNGOS leaves UNARMED until CMSG_SETSHEATHED.
     * Clear to UNARMED when no weapon slots are equipped.
     */
    public void refreshSheath() {
        int sheath = getInt(UpdateFields.UNIT_FIELD_BYTES_2) & 0xFF;
        if (itemAt(0, EQUIPMENT_SLOT_MAINHAND) == null
                && itemAt(0, EQUIPMENT_SLOT_OFFHAND) == null
                && itemAt(0, EQUIPMENT_SLOT_RANGED) == null) {
            sheath = 0;
        }
        int shapeshift = shapeshiftForm();
        setInt(UpdateFields.UNIT_FIELD_BYTES_2,
                sheath | (PLAYER_CONTROLLED_DEBUFF_LIMIT << 8) | (shapeshift << 24));
    }

    public static int visibleItemEntryField(int slot) {
        return UpdateFields.PLAYER_VISIBLE_ITEM_1_0 + slot * MAX_VISIBLE_ITEM_OFFSET;
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

    /**
     * CMaNGOS HandleModStealth → SetByteFlag/RemoveByteFlag(PLAYER_FIELD_BYTES2, 1, PLAYER_FIELD_BYTE2_STEALTH).
     * Player.h PLAYER_FIELD_BYTE2_STEALTH = 0x20.
     */
    public void setStealthByte(boolean apply) {
        int bytes = getInt(UpdateFields.PLAYER_FIELD_BYTES2);
        int mask = PLAYER_FIELD_BYTE2_STEALTH << 8;
        setInt(UpdateFields.PLAYER_FIELD_BYTES2, apply ? bytes | mask : bytes & ~mask);
    }

    /**
     * CMaNGOS HandleInvisibility → SetByteFlag/RemoveByteFlag(PLAYER_FIELD_BYTES2, 1, PLAYER_FIELD_BYTE2_INVISIBILITY_GLOW).
     * Player.h PLAYER_FIELD_BYTE2_INVISIBILITY_GLOW = 0x40.
     */
    public void setInvisibilityGlow(boolean apply) {
        int bytes = getInt(UpdateFields.PLAYER_FIELD_BYTES2);
        int mask = PLAYER_FIELD_BYTE2_INVISIBILITY_GLOW << 8;
        setInt(UpdateFields.PLAYER_FIELD_BYTES2, apply ? bytes | mask : bytes & ~mask);
    }

    public static final int PLAYER_FIELD_BYTE2_STEALTH = 0x20;
    public static final int PLAYER_FIELD_BYTE2_INVISIBILITY_GLOW = 0x40;

    /**
     * CMaNGOS Player::m_environmentBreathingMultiplier — 0 = infinite breath (WATER_BREATHING),
     * 1 = normal; MOD_WATER_BREATHING scales later.
     */
    private float waterBreathingIntervalMultiplier = 1.0f;

    public void setWaterBreathingIntervalMultiplier(float multiplier) {
        this.waterBreathingIntervalMultiplier = multiplier;
    }

    public float waterBreathingIntervalMultiplier() {
        return waterBreathingIntervalMultiplier;
    }

    public int createSelfFlags() {
        return PLAYER_CREATE_FLAGS;
    }

    public int observerFlags() {
        return UPDATEFLAG_HIGHGUID | UPDATEFLAG_LIVING | UPDATEFLAG_HAS_POSITION;
    }

    /**
     * CMaNGOS Unit::SendMoveRoot for a client-controlled unit: SMSG_FORCE_MOVE_ROOT / UNROOT
     * = packed guid + uint32 order counter, sent only to the controlling session (movement.md).
     * Does not set UNIT_STAT_ROOT — that is SetImmobilizedState / AuraEngine.immobilize
     * (KillPlayer force-root alone must not block BuildPlayerRepop unroot).
     */
    @Override
    public void sendMoveRoot(boolean root) {
        if (session == null) {
            return;
        }
        WowBuffer b = new WowBuffer(12);
        b.putPackedGuid(guid);
        b.putU32(moveOrderCounter++);
        session.send(root ? Opcodes.SMSG_FORCE_MOVE_ROOT : Opcodes.SMSG_FORCE_MOVE_UNROOT, b.array());
    }

    /**
     * CMaNGOS Unit::SetWaterWalk for client-controlled: SMSG_MOVE_WATER_WALK / LAND_WALK
     * = packed guid + uint32 order counter (Unit.cpp).
     */
    @Override
    public void sendWaterWalk(boolean enable) {
        if (session == null) {
            return;
        }
        WowBuffer b = new WowBuffer(12);
        b.putPackedGuid(guid);
        b.putU32(moveOrderCounter++);
        session.send(enable ? Opcodes.SMSG_MOVE_WATER_WALK : Opcodes.SMSG_MOVE_LAND_WALK, b.array());
    }

    /**
     * CMaNGOS Unit::SetFeatherFall for client-controlled: SMSG_MOVE_FEATHER_FALL / NORMAL_FALL
     * = packed guid + uint32 order counter (Unit.cpp).
     */
    @Override
    public void sendFeatherFall(boolean enable) {
        if (session == null) {
            return;
        }
        WowBuffer b = new WowBuffer(12);
        b.putPackedGuid(guid);
        b.putU32(moveOrderCounter++);
        session.send(enable ? Opcodes.SMSG_MOVE_FEATHER_FALL : Opcodes.SMSG_MOVE_NORMAL_FALL, b.array());
    }

    /**
     * CMaNGOS Unit::SetHover for client-controlled: SMSG_MOVE_SET_HOVER / UNSET_HOVER
     * = packed guid + uint32 order counter (Unit.cpp).
     */
    @Override
    public void sendHover(boolean enable) {
        if (session == null) {
            return;
        }
        WowBuffer b = new WowBuffer(12);
        b.putPackedGuid(guid);
        b.putU32(moveOrderCounter++);
        session.send(enable ? Opcodes.SMSG_MOVE_SET_HOVER : Opcodes.SMSG_MOVE_UNSET_HOVER, b.array());
    }

    /**
     * CMaNGOS SetSpeedRate MOVE_RUN for client-controlled: SMSG_FORCE_RUN_SPEED_CHANGE
     * = packed guid + order counter + u8(0) + float GetSpeed (Unit.cpp).
     */
    @Override
    protected void sendRunSpeedChange() {
        if (session == null) {
            return;
        }
        WowBuffer b = new WowBuffer(18);
        b.putPackedGuid(guid);
        b.putU32(moveOrderCounter++);
        b.putU8(0);
        b.putFloat(runSpeed());
        session.send(Opcodes.SMSG_FORCE_RUN_SPEED_CHANGE, b.array());
    }

    /**
     * CMaNGOS SetSpeedRate MOVE_SWIM for client-controlled: SMSG_FORCE_SWIM_SPEED_CHANGE
     * = packed guid + order counter + float GetSpeed (no u8 tracking byte; Unit.cpp).
     */
    @Override
    protected void sendSwimSpeedChange() {
        if (session == null) {
            return;
        }
        WowBuffer b = new WowBuffer(17);
        b.putPackedGuid(guid);
        b.putU32(moveOrderCounter++);
        b.putFloat(swimSpeed());
        session.send(Opcodes.SMSG_FORCE_SWIM_SPEED_CHANGE, b.array());
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

    /** CMaNGOS GetSkillValueBase — low 16 of PLAYER_SKILL_INFO value word. */
    public int skillValue(int skillId) {
        int want = skillId & 0xFFFF;
        for (int slot = 0; slot < 127; slot++) {
            int base = UpdateFields.PLAYER_SKILL_INFO_1_1 + slot * 3;
            if ((getInt(base) & 0xFFFF) == want) {
                return getInt(base + 1) & 0xFFFF;
            }
        }
        return 0;
    }

    /** High 16 of PLAYER_SKILL_INFO value word. */
    public int skillMax(int skillId) {
        int want = skillId & 0xFFFF;
        for (int slot = 0; slot < 127; slot++) {
            int base = UpdateFields.PLAYER_SKILL_INFO_1_1 + slot * 3;
            if ((getInt(base) & 0xFFFF) == want) {
                return (getInt(base + 1) >>> 16) & 0xFFFF;
            }
        }
        return 0;
    }

    /** CMaNGOS GetSkillStep — PAIR32 high part of PLAYER_SKILL_INFO. */
    public int skillStep(int skillId) {
        int want = skillId & 0xFFFF;
        for (int slot = 0; slot < 127; slot++) {
            int packed = getInt(UpdateFields.PLAYER_SKILL_INFO_1_1 + slot * 3);
            if ((packed & 0xFFFF) == want) {
                return (packed >>> 16) & 0xFFFF;
            }
        }
        return 0;
    }

    public void learnSkill(int skillId, int value, int max, int step) {
        learnSkill(skillId, value, max, step, false);
    }

    /**
     * CMaNGOS Player::UpdateSkill — add {@code diff} toward max; marks dirty skill fields for VALUES.
     * @return true when value changed
     */
    public boolean updateSkill(int skillId, int diff) {
        if (skillId == 0 || diff == 0) {
            return false;
        }
        int want = skillId & 0xFFFF;
        for (int slot = 0; slot < 127; slot++) {
            int base = UpdateFields.PLAYER_SKILL_INFO_1_1 + slot * 3;
            if ((getInt(base) & 0xFFFF) != want) {
                continue;
            }
            int packed = getInt(base + 1);
            int value = packed & 0xFFFF;
            int max = (packed >>> 16) & 0xFFFF;
            if (max == 0 || value == 0 || value >= max) {
                return false;
            }
            int next = value + diff;
            if (next > max) {
                next = max;
            }
            int step = (getInt(base) >>> 16) & 0xFFFF;
            setSkill(slot, skillId, next, max, step);
            dirtySkillFields.add(base);
            dirtySkillFields.add(base + 1);
            return true;
        }
        return false;
    }

    /** Fields to push in SMSG_UPDATE_OBJECT VALUES after skill gain; cleared on take. */
    public List<Integer> takeDirtySkillFields() {
        if (dirtySkillFields.isEmpty()) {
            return List.of();
        }
        List<Integer> out = new ArrayList<>(dirtySkillFields);
        dirtySkillFields.clear();
        return out;
    }

    /**
     * CMaNGOS GetWeaponSkillIdForAttack — mainhand/offhand item GetSkill, else UNARMED on BASE.
     */
    public int weaponSkillIdForAttack(boolean offhand) {
        int slot = offhand ? EQUIPMENT_SLOT_OFFHAND : EQUIPMENT_SLOT_MAINHAND;
        Item weapon = itemAt(0, slot);
        if (weapon != null) {
            int skill = WeaponSkills.skillForWeaponSubclass(weapon.subClass);
            if (skill != 0) {
                return skill;
            }
        }
        if (!offhand) {
            return WeaponSkills.SKILL_UNARMED;
        }
        return 0;
    }

    /**
     * CMaNGOS UpdateCombatSkills — chance from room-to-cap curve; on success UpdateWeaponSkill / UpdateDefense.
     * {@code chanceRoll} is unit [0,1); success when {@code chanceRoll * 100 < finalChancePct}.
     */
    public boolean updateCombatSkills(boolean defence, boolean offhand, DoubleSupplier chanceRoll) {
        int skillId = defence ? WeaponSkills.SKILL_DEFENSE : weaponSkillIdForAttack(offhand);
        if (skillId == 0) {
            return false;
        }
        int skill = skillValue(skillId);
        int skillMax = skillMax(skillId);
        int room = skillMax - skill;
        if (skillMax == 0 || level == 0 || skill >= skillMax) {
            return false;
        }
        double skillGapLogGrowth = 0.22 * (1.0 + (12.0 / level));
        double levelCapScale = 1.0;
        double baseChance;
        if (room >= 7) {
            baseChance = (7.0 / skillMax) * levelCapScale
                    + skillGapLogGrowth * Math.log((room + 5.0) / 12.0);
        } else {
            baseChance = (room * room) / (7.0 * skillMax) * levelCapScale;
        }
        if (baseChance < 0) {
            baseChance = 0;
        }
        if (baseChance > 1) {
            baseChance = 1;
        }
        double finalChance = baseChance * 100.0;
        if (chanceRoll.getAsDouble() * 100.0 >= finalChance) {
            return false;
        }
        if (defence) {
            return updateSkill(WeaponSkills.SKILL_DEFENSE, WeaponSkills.SKILL_GAIN_DEFENSE);
        }
        return updateWeaponSkill(offhand);
    }

    /** CMaNGOS UpdateWeaponSkill — gain on equipped weapon skill or unarmed. */
    public boolean updateWeaponSkill(boolean offhand) {
        Item weapon = itemAt(0, offhand ? EQUIPMENT_SLOT_OFFHAND : EQUIPMENT_SLOT_MAINHAND);
        if (weapon != null) {
            if (weapon.subClass == WeaponSkills.ITEM_SUBCLASS_WEAPON_FISHING_POLE) {
                return false;
            }
            int skill = WeaponSkills.skillForWeaponSubclass(weapon.subClass);
            if (skill != 0) {
                return updateSkill(skill, WeaponSkills.SKILL_GAIN_WEAPON);
            }
            return false;
        }
        if (!offhand) {
            return updateSkill(WeaponSkills.SKILL_UNARMED, WeaponSkills.SKILL_GAIN_WEAPON);
        }
        return false;
    }

    /** CMaNGOS SkillChance.* defaults ×10 (UpdateSkillPro Chance tenths of a percent). */
    public static final int SKILL_CHANCE_ORANGE = 100;
    public static final int SKILL_CHANCE_YELLOW = 75;
    public static final int SKILL_CHANCE_GREEN = 25;
    public static final int SKILL_CHANCE_GREY = 0;
    public static final int SKILL_GAIN_CRAFTING = 1;
    public static final int SKILL_GAIN_GATHERING = 1;

    /** CMaNGOS SkillGainChance — orange/yellow/green/grey band as tenths (1000 = 100%). */
    public static int skillGainChanceTenths(int skillValue, int grayLevel, int greenLevel, int yellowLevel) {
        if (skillValue >= grayLevel) {
            return SKILL_CHANCE_GREY * 10;
        }
        if (skillValue >= greenLevel) {
            return SKILL_CHANCE_GREEN * 10;
        }
        if (skillValue >= yellowLevel) {
            return SKILL_CHANCE_YELLOW * 10;
        }
        return SKILL_CHANCE_ORANGE * 10;
    }

    /**
     * CMaNGOS UpdateSkillPro — {@code roll1to1000} is irand(1,1000); success when roll ≤ chanceTenths.
     */
    public boolean updateSkillPro(int skillId, int chanceTenths, int diff, IntSupplier roll1to1000) {
        if (skillId == 0 || chanceTenths <= 0 || diff == 0) {
            return false;
        }
        if (skillMax(skillId) == 0 || skillValue(skillId) == 0 || skillValue(skillId) >= skillMax(skillId)) {
            return false;
        }
        int roll = roll1to1000.getAsInt();
        if (roll > chanceTenths) {
            return false;
        }
        return updateSkill(skillId, diff);
    }

    /**
     * CMaNGOS UpdateCraftSkill — SkillLineAbility band + UpdateSkillPro (no alchemy discovery).
     */
    public boolean updateCraftSkill(int spellId, SkillLineAbility catalog, IntSupplier roll1to1000) {
        if (catalog == null || spellId == 0) {
            return false;
        }
        SkillLineAbility.Entry e = catalog.bySpell(spellId);
        if (e == null || e.skillId() == 0) {
            return false;
        }
        int value = skillValue(e.skillId());
        int green = (e.maxValue() + e.minValue()) / 2;
        int chance = skillGainChanceTenths(value, e.maxValue(), green, e.minValue());
        return updateSkillPro(e.skillId(), chance, SKILL_GAIN_CRAFTING, roll1to1000);
    }

    /**
     * CMaNGOS UpdateGatherSkill — herbalism/mining/skinning/lockpicking/jewelcrafting bands.
     * Skinning/mining step-down uses CONFIG defaults 75 when steps &gt; 0.
     */
    public boolean updateGatherSkill(int skillId, int skillValue, int redLevel, int multiplicator,
                                     IntSupplier roll1to1000) {
        int red = redLevel == 1 ? 5 : redLevel;
        int chance = skillGainChanceTenths(skillValue, red + 100, red + 50, red + 25) * Math.max(1, multiplicator);
        return switch (skillId) {
            case Content.SKILL_HERBALISM, Content.SKILL_LOCKPICKING, Content.SKILL_JEWELCRAFTING ->
                    updateSkillPro(skillId, chance, SKILL_GAIN_GATHERING, roll1to1000);
            case Content.SKILL_SKINNING -> {
                int steps = 75;
                yield updateSkillPro(skillId, chance >> (skillValue / steps), SKILL_GAIN_GATHERING, roll1to1000);
            }
            case Content.SKILL_MINING -> {
                int steps = 75;
                yield updateSkillPro(skillId, chance >> (skillValue / steps), SKILL_GAIN_GATHERING, roll1to1000);
            }
            default -> false;
        };
    }

    /**
     * CMaNGOS UpdateFishingSkill — grace steps then UpdateSkillPro(1000, 1).
     * {@code fishingSteps} accumulates; on threshold, skill +1 and steps reduce.
     */
    public boolean updateFishingSkill(IntSupplier roll0InclusiveSteps) {
        int value = skillValue(Content.SKILL_FISHING);
        if (value >= skillMax(Content.SKILL_FISHING) || skillMax(Content.SKILL_FISHING) == 0) {
            return false;
        }
        int stepsNeeded = fishingStepsNeededToLevelUp(value);
        fishingSteps++;
        if (roll0InclusiveSteps.getAsInt() == 0) {
            fishingSteps++;
        }
        if (fishingSteps < stepsNeeded) {
            return false;
        }
        fishingSteps -= stepsNeeded;
        return updateSkillPro(Content.SKILL_FISHING, 1000, 1, () -> 1);
    }

    /** CMaNGOS GetFishingStepsNeededToLevelUp. */
    public static int fishingStepsNeededToLevelUp(int skillValue) {
        if (skillValue < 75) {
            return 1;
        }
        if (skillValue <= 300) {
            return skillValue / 44;
        }
        return skillValue / 31;
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

    /** CMaNGOS Player::removeSpell — SPELL_EFFECT_UNLEARN_SPECIALIZATION. */
    public void removeSpell(int spellId) {
        if (spellId <= 0) {
            return;
        }
        spells.remove((Integer) spellId);
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

    /** Player::UpdateDuelFlag — countdown of 3 seconds, then startTime is set. */
    public void promoteDuel(long nowMs) {
        if (duelCountdownStartMs == 0 || nowMs < duelCountdownStartMs + 3000) {
            return;
        }
        duelCountdownStartMs = 0;
        duelStartedAtMs = nowMs;
        if (duelOpponent != null) {
            duelOpponent.duelCountdownStartMs = 0;
            duelOpponent.duelStartedAtMs = nowMs;
        }
    }

    /** Player::DuelComplete — clear arbiter pairing on both sides and destroy the flag GO. */
    public void completeDuel() {
        long arbiter = duelArbiterGuid;
        Player other = duelOpponent;
        duelOpponent = null;
        duelPhase = 0;
        duelCountdownStartMs = 0;
        duelStartedAtMs = 0;
        duelArbiterGuid = 0;
        setDuelFlag(null);
        victim = 0;
        if (other != null) {
            other.duelOpponent = null;
            other.duelPhase = 0;
            other.duelCountdownStartMs = 0;
            other.duelStartedAtMs = 0;
            other.duelArbiterGuid = 0;
            other.setDuelFlag(null);
            other.victim = 0;
        }
        if (arbiter == 0) {
            return;
        }
        WowBuffer d = new WowBuffer(8);
        d.putU64(arbiter);
        byte[] pkt = d.array();
        if (session != null) {
            session.send(Opcodes.SMSG_DESTROY_OBJECT, pkt);
        }
        if (other != null && other.session != null && other.session != session) {
            other.session.send(Opcodes.SMSG_DESTROY_OBJECT, pkt);
        }
    }

    public GameObject duelFlag() {
        return duelFlag;
    }

    public void setDuelFlag(GameObject flag) {
        duelFlag = flag;
    }

    /** Domain setup: taxi flight in progress (logout.md InstantLogout). */
    public void startTaxiFlight(int pathId) {
        taxiPath = pathId;
    }

    public void clearTaxiFlight() {
        taxiPath = 0;
    }

    /**
     * CMaNGOS WorldObject::PlayMusic — SMSG_PLAY_MUSIC uint32 SoundEntries id.
     * Ribbon Pole Music 46852 is sound 12319.
     */
    public void playMusic(int soundId) {
        if (soundId <= 0) {
            return;
        }
        lastPlayMusic = soundId;
    }

    public int lastPlayMusic() {
        return lastPlayMusic;
    }

    /**
     * CMaNGOS WorldObject::PlayDirectSound — SMSG_PLAY_SOUND uint32 SoundEntries id.
     * BOTM Jungle Madness Music 49963 is sound 7294.
     */
    public void playSound(int soundId) {
        if (soundId <= 0) {
            return;
        }
        lastPlaySound = soundId;
    }

    public int lastPlaySound() {
        return lastPlaySound;
    }

    /** CMaNGOS GetRecruitingFriendId session player — Refer-a-Friend summon target. */
    public void setRecruitingFriend(Player friend) {
        recruitingFriend = friend;
    }

    public Player recruitingFriend() {
        return recruitingFriend;
    }

    /**
     * CMaNGOS SMSG_TOTEM_CREATED after EffectSummonObject slot &lt; MAX_TOTEM_SLOT.
     */
    public void setLastTotemCreated(int slot, long guid) {
        lastTotemCreatedSlot = slot;
        lastTotemCreatedGuid = guid;
    }

    public int lastTotemCreatedSlot() {
        return lastTotemCreatedSlot;
    }

    public long lastTotemCreatedGuid() {
        return lastTotemCreatedGuid;
    }

    /**
     * CMaNGOS Loot::ShowContentTo after EffectSkinning — SMSG_LOOT_RESPONSE pickpocketing.
     */
    public void showSkinningLoot(long guid) {
        lastSkinningLootGuid = guid;
    }

    public long lastSkinningLootGuid() {
        return lastSkinningLootGuid;
    }

    /**
     * CMaNGOS Loot::ShowContentTo after RemovedInsignia — SMSG_LOOT_RESPONSE corpse.
     */
    public void showInsigniaLoot(long guid) {
        lastInsigniaLootGuid = guid;
    }

    public long lastInsigniaLootGuid() {
        return lastInsigniaLootGuid;
    }

    /** CMaNGOS SMSG_PLAYER_SKINNED uint8 — 1 when death timer forced repop. */
    public void setLastSkinnedRepop(int repop) {
        lastSkinnedRepop = repop;
    }

    public int lastSkinnedRepop() {
        return lastSkinnedRepop;
    }

    /**
     * CMaNGOS Loot::ShowContentTo after EffectPickPocket — SMSG_LOOT_RESPONSE pickpocketing.
     */
    public void showPickpocketLoot(long guid) {
        lastPickpocketLootGuid = guid;
    }

    public long lastPickpocketLootGuid() {
        return lastPickpocketLootGuid;
    }

    /**
     * CMaNGOS Loot::ShowContentTo after EffectDisEnchant — item loot pickpocketing.
     */
    public void showDisenchantLoot(long guid) {
        lastDisenchantLootGuid = guid;
    }

    public long lastDisenchantLootGuid() {
        return lastDisenchantLootGuid;
    }

    /**
     * CMaNGOS Loot::ShowContentTo after EffectProspecting — item loot pickpocketing.
     */
    public void showProspectingLoot(long guid) {
        lastProspectingLootGuid = guid;
    }

    public long lastProspectingLootGuid() {
        return lastProspectingLootGuid;
    }

    /**
     * CMaNGOS SendLoot after EffectOpenLock — item loot pickpocketing.
     */
    public void showOpenLockLoot(long guid) {
        lastOpenLockLootGuid = guid;
    }

    public long lastOpenLockLootGuid() {
        return lastOpenLockLootGuid;
    }

    /**
     * CMaNGOS Spell::itemTarget — food for Feed Pet 6991.
     */
    public void setSpellItemTarget(Item item) {
        spellItemTarget = item;
    }

    public Item spellItemTarget() {
        return spellItemTarget;
    }

    /**
     * CMaNGOS Spell::gameObjTarget — Blow Zul'Farrak Door 11195.
     */
    public void setSpellGameObjectTarget(GameObject go) {
        spellGameObjectTarget = go;
    }

    public GameObject spellGameObjectTarget() {
        return spellGameObjectTarget;
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
