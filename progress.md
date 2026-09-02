# Slice progress

Status: `not_started` | `in_progress` | `p0_junit` | `p0_client` | `done`

`done` requires P0 pass. `skip` is not pass. See `spec/07-rebuild/test-plan.md`.
JUnit: `mvn -f tbc-server/pom.xml test` (TP-INV, matrix, slice 5–8 Gherkin).

Sidecar: `tbc-admin` is a Swing operator tool for `tbcrealmd.account` (create / role / password / delete). It is **not** a slice, **not** SOAP/RA, and **not** `p0_client`. Passwords are stored as SRP6 `v`/`s` only.

Official 8606 client **entered world** (lab 2026-09-02). Slice 4 is `p0_client`, not `done`: two-client move/chat P0 (`TP-SL04-003` observer, `004`/`005`/`010`) still open. Single-client leftover: `CMSG_PLAYER_LOGOUT` is a no-op; JUnit covers empty `SMSG_CONTACT_LIST`, combat-blocked logout, gmlevel-0 delayed `SMSG_LOGOUT_RESPONSE`, and logout/re-enter.

Lab: account **REMI** (id 1007, expansion 1), character **piep** (guid 9032, gnome warrior, Coldridge). Auth `proof ok`; `CMSG_PLAYER_LOGIN` reached in-world. Starter outfit from `CharStartOutfit.dbc`. Live slices 5–8: `CharacterStore.save` is one SQL transaction; map 0/1 `creature` rows spawn and nearby units are sent as `UPDATE_OBJECT`. `.tele x y z [map]` (SEC_MODERATOR) for Goldshire. Live 8606: melee range/HP/corpse and warrior rage + Heroic Strike **78** on piep. Slice 9: two 8606 clients opened the trade window. Chat language: after create-self the burst now sends a skill VALUES block plus `SMSG_LEARNED_SPELL` for Common **668** / Gnomish **7340** so the 8606 language menu can refresh. Relog required after world restart.

| Slice | Status | Notes |
|------:|--------|-------|
| 0 scaffold | done | Maven reactor, conf, AGENTS.md, Cursor rule |
| 1 Auth | p0_client | SRP6 + realm list on live 8606 (`proof ok REMI`). Ban/lock still JUnit-only. |
| 2 World session | p0_client | Live 8606 reached character screen then world (addon zlib, AuthCrypt, AUTH_OK). |
| 3 Characters | p0_client | Live create/enum **piep**; row survived world restart. Duplicate-name / expansion / delete P0 still JUnit-only. |
| 4 Enter world / move / chat | p0_client | **`TP-SL04-001` pass** (8606 in starting zone). JUnit: login-burst, UPDATEFLAG_SELF 0x71, empty friends list, MovementInfo, say/whisper (tag 0, whisper lang 0, Common skill 98 at 300/300), logout handshake, combat-block, `CMSG_PLAYER_LOGOUT` no-op, time-sync accepted. Live `/say` needs relog after language-skill fix. Two-client observer P0 not yet. |
| 5 Persistence | p0_junit | Gherkin + `WowClientDouble`. SQL `save` is one transaction (characters + actions + inventory savepoint). `clearOnline` on world start. Live relog/vendor-buy on piep still needs a client click. **Not `p0_client`.** |
| 6 Combat | p0_junit | Gherkin + `WowClientDouble`. Live: `creature_template` SELECT uses ModelId1 and quoted Rank (MySQL reserved); map 0/1 `creature` rows instantiated; nearby `UPDATE_OBJECT` on login/move. Attack/loot/evade on piep still needs a client click. **Not `p0_client`.** |
| 7 Spells | p0_junit | Gherkin Fireball 133 + JUnit Heroic Strike **78**. Lab 8606: rage bar, melee rage, HS 78 on piep (still not `p0_client` in the two-client sense; single-client HS click done). **Not `done`.** |
| 8 Content | p0_junit | Gherkin Goldshire NPC 240, vendor 54, quest 783. Live NPCs come from map-0 spawns; `.tele` to Goldshire (`-9465 16 57`). Gossip/quest on 8606 still needs a client click. **Not `p0_client`.** |
| 9 Social / economy | p0_junit | Gherkin two doubles: invite/`SMSG_GROUP_LIST` (HIGHGUID_GROUP 0x1F50), party chat 0x02, trade both-accept, who display cap 49, `character_social` + `SMSG_CONTACT_LIST` after relog, mail take-item **uint32** guid-low. Live 8606: trade window opens (REMI/REMI2). Party/mail still need a client click. **Not `p0_client`.** |
| 10 Instances / PvP | p0_junit | areatrigger_teleport + group bind; WSG map 489 + WS 1545/1546 |
| 11 EventAI | p0_junit | EventAI rows; ScriptName fallback log |
| 12 Combat math | p0_junit | Melee table 5% miss + 5% dodge at L1 |
| 13 Spell catalogs | p0_junit | Effect ids from spec; dummy without plugin |
| 14 Living world | p0_junit | Buy, trainer, taxi, talent, weather |
| 15 Social leftover | p0_junit | Group list / AH hello / rolls stubs |
| 16 PvP leftover | p0_junit | Arena join map 562; Hellfire WS fields |
| 17 Death | p0_junit | Repop / reclaim / self-res |
| 18 Pets | p0_junit | Pet bar opcode path |
| 19 Channels | p0_junit | Join + notify + list |
| 20 Items leftover | p0_junit | Buy / open item consume |
| 21 Guild leftover | p0_junit | Guild leader delete refuse |
| 22 Honor / inspect / duel | p0_junit | Inspect packed GUID; duel countdown 3 |
| 23 Arena teams | p0_junit | Join arena + PvP log |
| 24 AV / AB / EY | p0_junit | Same BG queue path, different map |
| 25 Outdoor PvP | p0_junit | Hellfire tower world-state fields 2476/2478 |
| 26 Spell algorithms | p0_junit | GO use; effect catalog |
| 27 Transports | p0_junit | FORCE_*_ACK + MovementInfo |
| 28 Misc packets | p0_junit | Tickets, LFG disabled on login |
| 29 GM commands | p0_junit | .help/.dismount SEC_PLAYER; .die SEC_ADMIN; .appear + LowerSecurity; SQL overlay |
| 30 ScriptDevAI registry | p0_junit | ScriptNames from spec; missing names log |
| 31 TBC raids | p0_junit | `boss_gruul` Growth **36300** / 30 s |
| 32 TBC 5-mans | p0_junit | ScriptName registry entries from spec files |
| 33 Classic raids | p0_junit | ScriptName registry entries from spec files |
| 34 Class scripts | p0_junit | Execute 5308→20647; UA dispel 31117 |
