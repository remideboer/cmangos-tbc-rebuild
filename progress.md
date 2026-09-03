# Slice progress

Status: `not_started` | `in_progress` | `p0_junit` | `p0_client` | `done`

`done` requires P0 pass. `skip` is not pass. See `spec/07-rebuild/test-plan.md`.
JUnit: `mvn -f tbc-server/pom.xml test` (TP-INV, TP-NEG, SL01–03 P0, matrix, Gherkin slices 4–11, Slice14P0Test, Slice15P0Test, `LaterP0Test` 16–30, `SpecFilesP0Test` 31–34).

Sidecar: `tbc-admin` is a Swing operator tool for `tbcrealmd.account` (create / role / password / delete). It is **not** a slice, **not** SOAP/RA, and **not** `p0_client`. Passwords are stored as SRP6 `v`/`s` only.

Official 8606 client **entered world** (lab 2026-09-02). Slice 4 is `p0_client`, not `done`: two-client Gherkin (`TP-SL04-003/004/005/010`) is **p0_junit**. Observer move/name (`003`/`005`) still need a two-client 8606 lab before `done`; do not tick `p0_client` on `004`/`010` until that lab click. Single-client leftover: `CMSG_PLAYER_LOGOUT` is a no-op; JUnit covers empty `SMSG_CONTACT_LIST`, combat-blocked logout, gmlevel-0 delayed `SMSG_LOGOUT_RESPONSE`, and logout/re-enter.

Slice 4 requirements stack is complete (nothing to invent): vision → `FR-MOV-001/002`, `FR-WLD-005`, `FR-SOC-001` → US-013 / US-020 / US-021 → `TP-SL04-003/004/005/010` → `movement.md` / `chat.md` / `queries.md` → `WorldSession` `handleMove` / `handleChat` / `handleNameQuery` / `revealNearby`. Later-slice YAML `us: []` is spec hygiene, not a slice-4 blocker. US-020 “no falling through vmaps” has no TP and is untestable in CI without DataDir vmaps — do not add collision here. Slices 5–34 P0 are **p0_junit** (Gherkin / `LaterP0Test` / spec-file tests). None of 5–34 are `done` or `p0_client` except where a live 8606 click is already noted. Official two-client lab for SL04-003/005 is still open (**Loop B**, not CI).

Lab: account **REMI** (id 1007, expansion 1), character **piep** (guid 9032, gnome warrior, Coldridge). Auth `proof ok`; `CMSG_PLAYER_LOGIN` reached in-world. Starter outfit from `CharStartOutfit.dbc`. Live slices 5–8: `CharacterStore.save` is one SQL transaction; map 0/1 `creature` rows spawn and nearby units are sent as `UPDATE_OBJECT`. `.tele x y z [map]` (SEC_MODERATOR) for Goldshire. Live 8606: melee range/HP/corpse and warrior rage + Heroic Strike **78** on piep. Slice 9: two 8606 clients opened the trade window. Chat language: after create-self the burst now sends a skill VALUES block plus `SMSG_LEARNED_SPELL` for Common **668** / Gnomish **7340** so the 8606 language menu can refresh. Relog required after world restart.

| Slice | Status | Notes |
|------:|--------|-------|
| 0 scaffold | done | Maven reactor, conf, AGENTS.md, Cursor rule |
| 1 Auth | p0_client | Live 8606 SRP6 + realm list. P0 JUnit: wrong password, ban, IP ban, IP lock, sessionkey, build 8606. Not `done` (lab already entered realm select). |
| 2 World session | p0_client | Live 8606 character screen. P0 JUnit: addon-then-AUTH_OK + expansion 1, OS reject, reconnect, overspeed kick (27 s, SEC_PLAYER). |
| 3 Characters | p0_client | Live create/enum **piep**. P0 JUnit: Alliance+Horde+duplicate **0x32**, BE expansion **0x39**, delete **0x3B**, guild-leader **0x3E**. |
| 4 Enter world / move / chat | p0_client | **`TP-SL04-001` pass** (8606 in starting zone). Two-double Gherkin **p0_junit**: observer `MSG_MOVE_HEARTBEAT` packed GUID + `fallTime` (`003`), say type `0x01` (`004`), other-player create without `UPDATEFLAG_SELF` + name query (`005`), whisper `0x07`/`0x09` (`010`). Not `done` until 003/005 are labbed. JUnit: login-burst, UPDATEFLAG_SELF 0x71, empty friends list, MovementInfo, say/whisper (tag 0, whisper lang 0, Common skill 98 at 300/300), logout handshake, combat-block, `CMSG_PLAYER_LOGOUT` no-op, time-sync accepted. Live `/say` works after language-skill relog. |
| 5 Persistence | p0_junit | Gherkin + `WowClientDouble`. SQL `save` is one transaction (characters + actions + inventory savepoint). `clearOnline` on world start. Live relog/vendor-buy on piep still needs a client click. **Not `p0_client`.** |
| 6 Combat | p0_junit | Gherkin + `WowClientDouble`. Live: `creature_template` SELECT uses ModelId1 and quoted Rank (MySQL reserved); map 0/1 `creature` rows instantiated; nearby `UPDATE_OBJECT` on login/move. Attack/loot/evade on piep still needs a client click. **Not `p0_client`.** |
| 7 Spells | p0_junit | Gherkin Fireball 133 + JUnit Heroic Strike **78**. Lab 8606: rage bar, melee rage, HS 78 on piep (still not `p0_client` in the two-client sense; single-client HS click done). **Not `done`.** |
| 8 Content | p0_junit | Gherkin Goldshire NPC 240, vendor 54, quest 783. Live NPCs come from map-0 spawns; `.tele` to Goldshire (`-9465 16 57`). Gossip/quest on 8606 still needs a client click. **Not `p0_client`.** |
| 9 Social / economy | p0_junit | Gherkin two doubles: invite/`SMSG_GROUP_LIST` (HIGHGUID_GROUP 0x1F50), party chat 0x02, trade both-accept, who display cap 49, `character_social` + `SMSG_CONTACT_LIST` after relog, mail take-item **uint32** guid-low. Live 8606: trade window opens (REMI/REMI2). Party/mail still need a client click. **Not `p0_client`.** |
| 10 Instances / PvP | p0_junit | Gherkin RFC AT **2230** map 389 shared bind + reset; WSG WAIT_JOIN **2** then port map **489**. **Not `p0_client`.** |
| 11 EventAI | p0_junit | Gherkin Garrick **103** spell **7164**; missing ScriptName log. **Not `p0_client`.** |
| 12 Combat math | p0_junit | `MeleeTableTest` 5% miss/dodge/parry/block; no glance L1 vs L1 |
| 13 Spell catalogs | p0_junit | `SpellEngine.knownEffect` + dummy/script no crash |
| 14 Living world | p0_junit | Wire: `Slice14P0Test` swap VALUES (`TP-SL14-001`), Llane 911 Battle Shout **6673** (`002`), Dungar taxi `SMSG_MONSTER_MOVE` (`003`), Elwynn `game_weather` snapshot (`004`). Not `p0_client`. |
| 15 Social leftover | p0_junit | Wire: `Slice15P0Test` NBG roll timeout 60000 (`TP-SL15-001`), raid `GROUP_LIST` + ready-check requester guid (`002`), roster gender (`003`), guild bank `SMSG_GUILD_BANK_LIST` (`004`), Chilton 8670 `BuildAuctionInfo` + delay 300 (`005`). Not `p0_client`. |
| 16 PvP leftover | p0_junit | Arena map **562**; Hellfire WS **2476/2478**; LFG list; WSG flag aura **23333** |
| 17 Death | p0_junit | Repop ghost **8326** + corpse + `SMSG_DEATH_RELEASE_LOC`; reclaim 50%; spirit healer **15007** |
| 18 Pets | p0_junit | `SMSG_PET_SPELLS`; stable **0x08/0x09**; warlock dismiss vs hunter; totem destroy |
| 19 Channels | p0_junit | Join YOU_JOINED **0x02**; list GUIDs; text emote; voice ignored |
| 20 Items leftover | p0_junit | Sell buyback slot **74**; socket enchant; repair gold |
| 21 Guild leftover | p0_junit | Guild query name; bank tab + 6 permissions; roll + ping |
| 22 Honor / inspect / duel | p0_junit | Honor cap **75000** + midnight roll; inspect packed; duel **3000** ms + OOB |
| 23 Arena teams | p0_junit | Petition turn-in; PvP log type 0; AFK aura **43680** |
| 24 AV / AB / EY | p0_junit | Spec timers **240000/300000**; AB tick table; EY points `{75,85,100,500}` |
| 25 Outdoor PvP | p0_junit | Silithyst **200**/ **30754**; TF lock **33377**; Halaa **15**/GY **993**/**33795** |
| 26 Spell algorithms | p0_junit | GO loot; talent wipe **14867**; cancel channelling |
| 27 Transports | p0_junit | `MOVEFLAG_ONTRANSPORT`; FORCE_RUN_SPEED_ACK; cancel mount **78** |
| 28 Misc packets | p0_junit | Push quest; master loot give; GMTICKET HASTEXT **0x06**; LFG accept |
| 29 GM commands | p0_junit | `.help`/`.dismount` SEC_PLAYER; `.die` SEC_ADMIN; `.appear` + LowerSecurity; overlay |
| 30 ScriptDevAI registry | p0_junit | `boss_gruul` Growth **36300** / 30 s; missing name log |
| 31 TBC raids | p0_junit | `SpecFilesP0Test` Karazhan–Sunwell + ZA files |
| 32 TBC 5-mans | p0_junit | Spec file existence for HFC/CF/Auch/TK/CoT |
| 33 Classic raids | p0_junit | MC/BWL/AQ/Naxx/ZG/Onyxia + `world-remaining.md` |
| 34 Class scripts | p0_junit | Execute **5308→20647**; UA **30108**; spec grep |
