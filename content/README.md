# Content compile pipeline

Author **deltas** here; `tbc-content` compiles them into server SQL + client DBC + an overlay patch MPQ.

## Layout

| Path | Role |
|------|------|
| `spells/*.yaml` | Spell deltas (`kind: spell`) |
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
