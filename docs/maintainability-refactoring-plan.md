# Maintainability refactoring plan (tbc-world)

Agent-executable plan to turn `tbc-world` into a modular codebase without changing a single wire
byte. Spec stays the gate, CMaNGOS the control flow, the existing Slice/Gherkin suites the oracle.

## 1. Analysis

The server works and is well tested (Slice P0, Gherkin, `WowClientDouble`, domain JUnit). The
problem is concentration, not feature count. Measured 2026-10-07 (`Files.readAllLines` line counts):

| Hotspot | Size | Symptom |
|---|---|---|
| `content/ObjectMgr.java` | 5768 lines | 57 public mutable collections, 26 `seed*` methods; in-memory seed, SQL load and catalog mixed |
| `spell/SpellEngine.java` | 4152 lines | public mutable wiring (`objectMgr`, `visibilityUpdater`, `skillLineAbilities`, roll suppliers) |
| `session/WorldSession.java` | 2449 lines | `handle()` = ~45 pre-switch status/move gates + switch with 175 `case Opcodes.` labels |
| `session/LaterOpcodes.java` | 733 lines | second dispatcher (if-chain) for "later" opcodes |
| `world/World.java` | 1759 lines | 141 `.session` references (broadcast via `Player.session`), 6 channel maps, inline `tick()` orchestration |
| `entity/Player.java` 2059, `content/Content.java` 1677 (JaCoCo-gated), `persist/CharacterStore.java` 1651, `session/SocialHandler.java` 1240, `session/GuildHandler.java` 1034, `session/InventoryHandler.java` 994 | | large but already single-family |

Module facts: 8 Maven modules; `tbc-world` has 158 main / 291 test files; `tbc-tests` 62 files,
9 Gherkin features. 205 static handler methods already share the shape
`(WorldSession, World, WowBuffer)`.

Repo gates a refactor must respect:

- `jacoco-check` BRANCH `COVEREDRATIO 1.00` on `PlayerPersist`, `Combat`, `MeleeTable`,
  `SpellEngine`, `SpellCastTargets`, `Content` (`tbc-world/pom.xml`). Include patterns are
  prefixes (`Content*`, `SpellEngine*`): a new class with that prefix is gated automatically.
  Moving code out of a gated class changes the denominator; new classes carved from gated code
  must be added to the includes in the same cycle.
- `.cursor/rules/git-workflow.mdc`: `refac/<slug>` branch from `main`, commit only on full
  `mvn -f tbc-server/pom.xml test`, squash-merge to `main`, never push.
- `.cursor/rules/tdd-bdd.mdc`: RED tests are for behavior. A pure move has no RED; it has a
  characterization test (structure pin) plus the existing byte oracle.

## 2. Target dependency direction

```text
net.wow8606 (WorldSocket, Netty)      -> session (WorldSession, SessionGate, OpcodeTable)
session                               -> session handlers per family (static or injected)
handlers                              -> world services (Broadcaster, ChannelRegistry, WorldTicker)
handlers, services                    -> domain (spell, combat, content, map, entity)
domain                                -> content.catalog interfaces -> ObjectMgr (facade: seed + load)
domain                                -> persist (CharacterStore + *Persist)
```

Rules: `entity`/`spell`/`combat`/`content`/`map` must not depend on `session`; `session` must not
import `io.netty`; production must not import `org.tbc.bdd`. Enforced by
`tbc-world/src/test/java/org/tbc/world/ArchitectureRulesTest.java`.

## 3. Baseline (2026-10-07)

Pinned as ratchets in `ArchitectureRulesTest` (lower only):

| Ratchet | Value |
|---|---|
| `WorldSession.java` lines | 2449 |
| `WorldSession.java` `case Opcodes.` lines | 175 |
| `LaterOpcodes.java` lines | 733 |
| `World.java` lines | 1759 |
| `World.java` `.session` occurrences | 141 |
| `ObjectMgr.java` lines | 5768 |
| `SpellEngine.java` lines | 4152 |
| domain lines naming `org.tbc.world.session.` (entity/spell/combat/content/map) | 20 (`Player` 1, `Content` 4, `ObjectMgr` 14, `GraveyardManager` 1) |

Hard rules at baseline: no `org.tbc.bdd` import in main (0); `io.netty` only in `net/` and
`WorldMain.java`. Full reactor green at baseline (`main` 41b033c).

## 4. Cycle template (every refactor step)

1. `git -C tbc-server status` clean of task files; `git switch -c refac/<slug>` from `main`.
2. Oracle first. Existing Slice/Gherkin tests are the byte oracle. Add a characterization test
   that pins structure (registered opcode set equals the old switch list; tick order; ratchet).
   No `@VisibleForTesting`, no test-only getters.
3. Move minimally. No behavior or byte change, no new features, opcodes or ids.
4. `mvn -f tbc-server/pom.xml test` — full reactor, JaCoCo on. Module runs are only a shortcut.
5. Lower the matching ratchet in `ArchitectureRulesTest` to the new measured value.
6. Commit `refac(<area>): <what> — no behavior change`; `git merge --squash` to `main`; delete the
   branch; full reactor green again.
7. Stop rules. Slice/Gherkin failure: fix the move, never the oracle. JaCoCo failure on a gated
   class: add a same-module JUnit for the moved branch; never `-Djacoco.skip`. The move needs a
   behavior change: stop, report `blocked`, split into a `feat/` or `fix/` branch with a RED test.
8. One family or one class extraction per cycle. After 10 cycles without a user message:
   summarize and wait.

## 5. Phases and cycles

### Phase 0 — Baseline gate (1 cycle, `refac/arch-baseline`)

- `ArchitectureRulesTest` with the hard rules and ratchets above.
- This document.
- Exit: full reactor green; squash to `main`.

### Phase 1 — One dispatcher: `OpcodeTable` (4 cycles)

| Cycle | Branch | Files | Oracle | Exit |
|---|---|---|---|---|
| 1.1 | `refac/opcode-table-query` | `session/PacketOperation.java` (`void handle(WorldSession, World, WowBuffer)`), `session/OpcodeTable.java` (`register`, `dispatch`, `opcodes()`), `WorldSession.handle` `default -> if (!table.dispatch) handleRest` | `OpcodeTableTest` (registered set == exact list; unknown opcode → false, nothing sent); `EarlySliceP0Test`, `Slice04*` name-query | QueryHandler cases removed from the switch; case ratchet lowered |
| 1.2 | `refac/opcode-table-later` | move each `LaterOpcodes.handle` family (Inventory 22, Loot 6, Taxi 4, Binder, Talent, Weather, …) into `register`; delete `LaterOpcodes` when empty | `Slice14`–`Slice30` P0, `LaterP0Test` | `LATER_OPCODES_MAX_LINES` → 0 |
| 1.3 | `refac/opcode-table-<family>` (one branch per family) | Social 30, Guild 35, Channel 17, Pet 13, Death 11, Petition 11, Group 9, Lfg 9, ArenaTeam 9, Auction 8 | the family's Slice/Gherkin tests | only session-owned opcodes remain in `WorldSession` (auth, char, login, logout, move, ping) |
| 1.4 | `refac/session-gate` | `session/SessionGate.java`: ordered pre-switch routing with status semantics (`STATUS_NEVER` → AUTH_SESSION/PING only; `STATUS_AUTHED` → char opcodes; recently-logged-out → `CMSG_UPDATE_ACCOUNT_DATA` only) | `SessionGateTest` per status; `Slice04LogoutTest`, `TP-INV-*` | `WorldSession.handle` ≤ 40 lines |

### Phase 2 — Session state and large session methods (4 cycles)

| Cycle | Branch | Move | Oracle |
|---|---|---|---|
| 2.1 | `refac/channel-registry` | 6 channel maps from `World` + `WorldSession.channels` → `world/ChannelRegistry`; `ChannelHandler` receives it | `Slice09SocialTest`, channel Gherkin |
| 2.2 | `refac/movement-handler` | `handleMove*`, `handleKnockBackAck`, `handleMoveFlagChangeAck`, `handleFallReset`, `isLivingMoveOpcode`, `isForceSpeedChangeAck` → `session/MovementHandler` | `slice04_world.feature`, `TP-SL04-003` |
| 2.3 | `refac/character-handler` | char enum/create/delete/rename/declined-names/login → `session/CharacterHandler` (`LoginBurst` stays) | `Slice03RenameTest`, `EarlySliceP0Test` |
| 2.4 | `refac/session-feature-state` | `bgQueue`, `worldStates2476/2478` → `pvp/BgQueueState`; `pendingInviteFrom`, `lastTicket` → `session/SocialState`; one accessor each | `Slice16P0Test`, `Slice09GroupTest` |

### Phase 3 — Unload the `World` composition root (4 cycles)

| Cycle | Branch | Move | Oracle / gate |
|---|---|---|---|
| 3.1 | `refac/broadcaster` | `world/Broadcaster` (`toPlayer`, `nearby`, `nearbyAndSelf`) replaces `.session` patterns in `World` (141), `CompanionBehavior` (12), `Content` (6) | `WORLD_MAX_SESSION_REFS` lowered per cycle |
| 3.2 | `refac/world-ticker` | `World.tick` body → `world/WorldTicker` ordered steps (sessions → companions → `spells.update` → periodic auras → expire → party auras → weather → creatures → quests → combat drop) | `WorldTickerTest` pins order with a recording step list |
| 3.3 | `refac/spellengine-injection` | `objectMgr`, `skillLineAbilities`, `visibilityUpdater`, `craftSkillRoll`, `gatherSkillRoll` private, via constructor / `alwaysHit(...)` | gated: every new branch covered by `tbc-world` JUnit |
| 3.4 | `refac/world-config` | `world/WorldConfig` record (`motd`, `realmId`, `instantLogout`, `maxOverspeedPings`, `saveIntervalMs`, `inboundOpcodeTrace`, say/yell range) with `fromConf(Conf)` and `inMemory()` | `World.inMemory()` unchanged for tests |

### Phase 4 — Split `ObjectMgr` (3 cycles)

| Cycle | Branch | Move |
|---|---|---|
| 4.1 | `refac/objectmgr-seed` | 26 `seed*` methods → `content/seed/*Seed.java` package-private per domain; `ObjectMgr.load(null, …)` calls them; `OBJECT_MGR_MAX_LINES` lowered |
| 4.2 | `refac/objectmgr-load` | SQL loaders per table → `content/load/*Loader.java`; `ObjectMgr.load(DbPool, …)` delegates |
| 4.3 | `refac/content-catalog` | `content/catalog/{Creature,Item,Quest,GameObject,Spawn}Catalog` interfaces; `ObjectMgr implements` all; handlers and `Content` take the narrow interface; mutation only through named methods (`addSpawnIfMissing`) |

### Phase 5 — Domain engines (4 cycles, JaCoCo-aware)

| Cycle | Branch | Move | JaCoCo |
|---|---|---|---|
| 5.1 | `refac/spell-effects` | effect dispatch → `spell/effects/<Family>Effects.java` via `Map<Integer, EffectHandler>` | add new classes to includes; do not name them `SpellEngine*` unless gating is intended |
| 5.2 | `refac/content-split` | `content/QuestGiver`, `Gossip`, `Vendor`, `Banker`; `Content` becomes a facade | `Content*` prefix already gates; add others explicitly |
| 5.3 | `refac/characterstore-split` | `persist/InventoryPersist`, `QuestStatusPersist`, `AuraPersist`, `SpellPersist` beside `PlayerPersist`; `CharacterStore.save/load` stays the only public surface | `PlayerPersist*` gated |
| 5.4 | `refac/player-split` (optional, last) | `PlayerQuestLog`, `PlayerStats` only if tests and ratchets allow | — |

### Phase 6 — Cleanup and documentation (1–2 cycles)

- Remove `World` compatibility delegations once callers are gone.
- `tbc-server/docs/architecture.md`: package boundaries and dependency direction.
- Freeze `ArchitectureRulesTest` ratchets at final values; one line in `AGENTS.md`.

## 6. Decision rules

- Static handler without state stays static, registered as a method reference. A handler that
  needs injected state (registry, config) becomes an instance with a constructor.
- The byte contract never changes on a `refac/` branch. Suspected bug: stop, open a `fix/` branch
  with a RED test.
- New class carved from a gated class: same-module JUnit covers every branch; update pom includes
  in the same cycle.
- `WowClientDouble` keeps driving `session.handle(world, opcode, payload)`; that signature is
  frozen.
- No ArchUnit or other framework; the source-scan JUnit is enough.

## 7. Out of scope

`mangos-tbc`, `tbc-db`, `playerbots`, Warden, SOAP/RA, AH-bot internals, extractors, Calendar,
player vehicles, official client assets. No new opcodes, spell ids or world-state ids.
