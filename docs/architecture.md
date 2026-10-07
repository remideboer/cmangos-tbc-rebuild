# tbc-server architecture

Package boundaries and dependency direction for the Java 8606 rebuild. Enforced by
`tbc-world/src/test/java/org/tbc/world/ArchitectureRulesTest.java` (hard rules + ratchets) and the
JaCoCo branch gate in `tbc-world/pom.xml`. Refactoring history: `maintainability-refactoring-plan.md`.

## Modules (Maven reactor)

| Module | Role | May depend on |
|---|---|---|
| `tbc-common` | `WowBuffer`, `DbPool`, `Conf`, crypto | — |
| `tbc-auth` | realmd (3724): SRP6, realm list, writes `account.sessionkey` | `tbc-common` |
| `tbc-world` | worldd (8085): everything below | `tbc-common` |
| `tbc-content` | YAML quest/spell deltas → SQL / DBC patches | `tbc-common` |
| `tbc-admin`, `tbc-editor`, `tbc-launcher` | operator tools over the same catalogs and stores | `tbc-world`, `tbc-common` |
| `tbc-tests` | `WowClientDouble`, Gherkin glue, `Slice*P0Test` | all of the above (test scope) |

Auth and world are two processes; the only handoff is `account.sessionkey` in MySQL.

## `tbc-world` packages

```
net.wow8606  ──► session ──► world ──► {content, spell, combat, entity, map, pvp, loot, ai, companion}
                                 │
                                 └──► persist ──► entity
```

Arrows point from the depender to the dependency. The domain row must not depend back on
`session`; the ratchet `DOMAIN_SESSION_DEPENDENCIES_MAX` counts the remaining legacy lines
(seeds that reference handler constants, `Gossip` → `TrainerHandler`/`TaxiHandler`/`AuctionHandler`).

| Package | Owns | Notes |
|---|---|---|
| `net.wow8606` | Netty transport, header crypto, `Opcodes`, `UpdateBuilder`/`UpdateFields` | only place `io.netty` is imported (plus `WorldMain`) |
| `session` | `WorldSession` (auth, char, login, logout, move, ping), `SessionGate` (status routing), `OpcodeTable` + one `*Handler` per opcode family, `LoginBurst`, `PacketSink` | handlers are static `PacketOperation`s registered in `OpcodeTable.loggedIn()`; `WorldSession.handle(world, opcode, payload)` is frozen for `WowClientDouble` |
| `world` | `World` composition root, `WorldConfig` (record), `WorldTicker` (ordered tick steps), `Broadcaster` (`toPlayer`, `nearby`, `nearbyAndSelf`), `ChannelRegistry`, `WorldTimers` | `World.inMemory()` is the test harness entry |
| `content` | `ObjectMgr` (templates, relations, spawns) + `*Seed` (in-memory defaults) + `*Loader` (one SQL table each) + `SqlText`; `catalog/` read interfaces; `Content` facade over `QuestGiver`, `Gossip`, `Vendor`, `Banker` | consumers read through `catalog/*Catalog`; spawn mutation only via `addSpawnIfMissing` / `moveSpawn` (`RAW_CATALOG_MAP_READS_MAX = 0`) |
| `spell` | `SpellEngine` (cast flow, auras, cooldowns), `SpellCastTargets`, `effects/` (`EffectTable` → `<Family>Effects`) | effect handlers are lambdas over public `SpellEngine` methods; `SMSG_CAST_RESULT = 0x130` |
| `combat` | `Combat`, `MeleeTable`, threat | formulas from `docs/tbc-game-mechanics.md` |
| `entity` | `Unit`, `Player`, `Creature`, `GameObject`, `Item`, `Guid`, update-field storage | no SQL, no packets |
| `persist` | `CharacterStore` (orchestrates `save`/`load`/`clearOnline`), `PlayerPersist` (snapshot copy), `InventoryPersist`, `QuestStatusPersist`, `AuraPersist`, `SpellPersist` (one table family each) | SQL lives only here; `load` returns a new `Player`; null `DbPool` = in-memory |
| `map`, `vmap`, `mmap` | `GameMap`, grids, visibility, graveyards, LoS / pathing | |
| `pvp`, `loot`, `ai`, `companion`, `classless`, `profession`, `gm`, `script`, `events` | feature domains | reached from handlers via `World` |

## Rules that tests enforce

- No `org.tbc.bdd` import in production; `io.netty` only in `net/` and `WorldMain`.
- Ratchets (lower only): `WorldSession` lines / `case Opcodes.` labels, `World` lines / `.session`
  refs, `ObjectMgr`, `SpellEngine`, `Content`, `CharacterStore` lines, domain → session lines,
  raw catalog map reads. Current values are the constants at the top of `ArchitectureRulesTest`.
- JaCoCo `BRANCH` 1.00 on `PlayerPersist`, `Combat`, `MeleeTable`, `SpellEngine`,
  `SpellCastTargets`, `spell/effects/*`, `Content`, `QuestGiver`, `Gossip`, `Vendor`, `Banker`.
  A new branch in a gated class needs a same-module JUnit for every outcome.

## Where new code goes

| You are adding… | Put it in | Register / gate |
|---|---|---|
| a C2S opcode | the matching `session/<Family>Handler` | `OpcodeTable.loggedIn()`; Gherkin + `WowClientDouble` |
| a spell effect | `spell/effects/<Family>Effects` | `EffectTable.standard()`; JaCoCo gated |
| NPC dialog / vendor / quest behavior | `content/Gossip`, `Vendor`, `QuestGiver`, `Banker` | keep `Content` a facade (`CONTENT_MAX_LINES`) |
| a persisted table | `persist/<Table>Persist` static `write`/`load(Connection, Player)` | call from `CharacterStore.save`/`load`; H2 round-trip test |
| a template catalog read | a `content/catalog` interface method on `ObjectMgr` | never `mgr.items.get(...)` from a handler |
| a tick step | `WorldTicker` | `WorldTickerTest` pins the order |
| config | `WorldConfig` record (`fromConf`, `inMemory`) | |

Deferred (plan 5.4): `PlayerQuestLog` / `PlayerStats` were not split out — quest-log arrays on
`Player` have ~130 production and ~320 test references, so the move is not a small, oracle-neutral
diff. Revisit only with a dedicated plan.
