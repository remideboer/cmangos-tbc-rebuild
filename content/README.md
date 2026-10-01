# Content compile pipeline

Author **deltas** here; `tbc-content` compiles them into server SQL + client DBC + an overlay patch MPQ.

## Modify a spell (checklist)

YAML under `content/spells/` is **not** live until you compile and install. A green JUnit on the YAML (or a `server:` SQL row alone) does **not** update the 8606 tip/`$d` duration — that comes from client `Spell.dbc` in the overlay MPQ.

| Step | What | Where |
|------|------|--------|
| 1 | Add/edit delta | `content/spells/*.yaml` — `client.Spell.*` and/or `server.*` |
| 2 | Assert the delta | `tbc-content` JUnit (e.g. `PaladinBlessingsDurationYamlTest`) |
| 3 | Compile | `build.bat` **or** `java -jar tbc-content\…jar compile --content content --base-dbc <DataDir>\dbc --out content\out --mpq-name patch-tbc-custom.MPQ` |
| 4 | Install overlay | `build.bat` does this when lab `Data` exists; else `content\install-client-patch.bat` → `WoW-2.4.3-client\Data\enUS\patch-enUS-3.MPQ` |
| 5 | Apply SQL (if `server:`) | `content\out\spell_template_patch.sql` → world DB from `conf\local-mangosd.conf` `WorldDatabaseInfo` (e.g. `mysql … tbcmangos < content\out\spell_template_patch.sql`). **Required for buff timers / aura length** — client tip alone is not enough. Restart world after apply. |
| 6 | Restart client | Fully quit **wowme.exe**, clear done by install (`Cache`/`WDB`), relaunch unsigned client |

**Do not skip step 4–6** after changing `client.Spell` (DurationIndex, descriptions, …). Do not skip **step 5** when the delta has `server:` — otherwise the spellbook tip can say 30 min while the buff bar still ticks 10 min (stock `DurationIndex` 6). Examples: `paladin-seals-duration.yaml`, `paladin-blessings-duration.yaml`.

```bat
rem Example (credentials from conf\local-mangosd.conf WorldDatabaseInfo):
mysql -h127.0.0.1 -P3306 -uroot -p tbcmangos < content\out\spell_template_patch.sql
```

**Locked MPQ:** quit the client before install (`patch-enUS-3.MPQ` in use → copy fails). **#131 File Corrupt:** stock `Wow.exe` rejects custom `Spell.dbc` — use `wowme.exe` ([unsigned-client-8606.md](unsigned-client-8606.md)).

Commit **YAML + tests** only (`content/out/` and client MPQs are gitignored / not committed).

## Layout

| Path | Role |
|------|------|
| `spells/*.yaml` | Spell deltas (`kind: spell`) |
| `classes/*.yaml` | ChrClasses / CharBaseInfo deltas (`kind: chrclasses`, `kind: charbaseinfo`) — e.g. Classless id 6 |
| `items/`, `quests/`, `talents/` | Reserved for later domains (empty for now) |
| `bindings/tbc243/` | DBC field layouts (from WoW-Spell-Editor Bindings_243_tbc) |
| `out/` | Generated artifacts (**gitignored**) |
| `install-client-patch.bat` | Mounts the overlay into the lab 8606 client |
| `unsigned-client-8606.md` | How/why `wowme.exe` was unsigned (offsets + recreate) |
| `sor-tip-combat-align.md` | SoR tip / action-bar / combat number alignment contract |
| `patch-wowme.ps1` | Rebuild `wowme.exe` from stock `Wow.exe` |

## YAML (spell)

```yaml
kind: spell
id: 20154
client:
  Spell:
    AuraDescription_lang_enUS: "…"   # → SpellToolTip0
    Description_lang_enUS: "…"       # → SpellDescription0
server:                              # optional → spell_template UPDATE
  EffectBasePoints1: 107
```

Unknown `kind` fails the build. Client-only or server-only deltas are fine.

## Build

From `tbc-server/`:

```bat
build.bat
```

If `CONTENT_BASE_DBC` is set, or `conf\local-mangosd.conf` `DataDir` points at a tree with `dbc\Spell.dbc`, the batch also:

1. Compiles YAML → `content\out\` (DBC + `patch-tbc-custom.MPQ` + SQL).
2. Installs that overlay via [`install-client-patch.bat`](install-client-patch.bat) whenever lab client `Data` is present (`CONTENT_CLIENT_DATA` or `../../WoW-2.4.3-client/Data` from this folder). Opt out with `CONTENT_INSTALL_PATCH=0`.

Client Data resolution order:

1. `CONTENT_CLIENT_DATA` env var  
2. `../../WoW-2.4.3-client/Data` (in-repo lab client; content lives under `tbc-server/content`)

Base DBC for compile: `CONTENT_BASE_DBC`, or `DataDir` from `conf\local-mangosd.conf` (`DataDir\dbc\Spell.dbc`). `build.bat` normalizes `/` to `\` in that path.

Or Maven profile (requires base DBC; does **not** auto-install — use `build.bat` or the install script for that):

```bat
set CONTENT_BASE_DBC=D:\path\to\DataDir\dbc
mvn -pl tbc-content -am package -Pcontent
```

CI / machines without DataDir skip content compile; unit tests still pass.

## Client install (8606)

Stock `Wow.exe` only opens a fixed MPQ list. A custom name like `patch-tbc-custom.MPQ` is **never** mounted — use the locale patch slot instead.

[`install-client-patch.bat`](install-client-patch.bat):

1. Copies the compiled overlay to `Data\enUS\patch-enUS-3.MPQ` (highest-priority enUS archive; backs up any prior file as `*.bak.<timestamp>`).
2. Deletes leftover `Data\patch-tbc-custom.MPQ` if present.
3. Clears `Cache\` and `WDB\` under the client root when those folders exist.
4. Does **not** touch `common.MPQ` / `expansion.MPQ` / lower `patch*.MPQ`.

### ERROR #131 File Corrupt (known 8606 signature check)

A **structurally valid** custom `Spell.dbc` still crashes stock 8606 with:

`ERROR #131 (0x85100083) File Corrupt … File: DBFilesClient\Spell.dbc`

Full notes (why, byte offsets, recreate script): **[unsigned-client-8606.md](unsigned-client-8606.md)**. Recreate with `content\patch-wowme.ps1`.

Lab client: keep stock `Wow.exe`. Use sibling `wowme.exe` (patched copy). The TBC launcher remembers `Wow.exe` but starts `wowme.exe` when present. Do not overwrite stock `Wow.exe`.

`build.bat` installs the overlay automatically after a successful content compile when lab `Data` exists. Opt out: `CONTENT_INSTALL_PATCH=0`. Standalone: `install-client-patch.bat`. To recover from #131 on stock exe: delete `Data\enUS\patch-enUS-3.MPQ` and restart.

Standalone re-install (after a compile, or to re-mount an existing `content\out\patch-tbc-custom.MPQ`):

```bat
content\install-client-patch.bat
content\install-client-patch.bat path\to\other.MPQ
```

Fully quit and restart the **unsigned** 8606 client after install.

Optional SQL: `content\out\spell_template_patch.sql` (empty unless a delta has `server:`).

Re-running compile backs up existing `content\out` files before overwrite; the install script backs up any prior client `patch-enUS-3.MPQ`.

## Classless character (ChrClasses id 6)

Creates the unused Death Knight slot as **Classless** on the 8606 create screen (warrior icon).

### Author

1. [`classes/classless.yaml`](classes/classless.yaml) — `kind: chrclasses`, id **6**, `filename: WARRIOR` (stock `CLASS_ICON_TCOORDS["WARRIOR"]`; no GlueXML).
2. [`classes/classless-charbaseinfo.yaml`](classes/classless-charbaseinfo.yaml) — `kind: charbaseinfo` race×6 rows for playable races `1–8, 10, 11` (without these, `GetAvailableClasses` hides the class).
3. Bindings: `ChrClasses.txt` col **filename** (string); `CharBaseInfo.txt` packed `RaceID`/`ClassID`.

### Install

1. Base DBC tree must include `ChrClasses.dbc` and `CharBaseInfo.dbc` (`CONTENT_BASE_DBC` / DataDir).
2. Compile + install overlay (`build.bat`, or `java -jar tbc-content… compile` then [`install-client-patch.bat`](install-client-patch.bat)).
3. Install backs up any prior `Data\enUS\patch-enUS-3.MPQ` as `patch-enUS-3.MPQ.bak.<timestamp>` and clears Cache/WDB.
4. Fully quit the client, start **wowme.exe**, open character create — Classless with warrior icon; create submits class byte **6**.
5. Server: `ClasslessConfig.enabled` (default true).

### Rollback

1. Fully quit **wowme.exe** / Wow.
2. Restore the newest `Data\enUS\patch-enUS-3.MPQ.bak.*` over `patch-enUS-3.MPQ`, **or** delete `patch-enUS-3.MPQ` to drop the overlay entirely.
3. Delete `Cache\` and `WDB\` under the lab client (or re-run install which clears them).
4. Restart. YAML/bindings in git are unchanged by rollback — only the client MPQ reverts.

LUA multi-power bars (mana+rage+energy) remain a follow-up; the server already writes MAXPOWER1/2/4. No GlueXML in this pass.
