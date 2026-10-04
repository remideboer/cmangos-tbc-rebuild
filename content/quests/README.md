# Quest content (`content/quests`)

Server-owned quest YAML for the Visual Quest Editor. This tree is **not** a spell/DBC delta.

`YamlDeltaLoader` / `build.bat` **skip** `content/quests/` so drafts do not fail unknown `kind: quest`.

| Path | Role |
|------|------|
| `drafts/<id>.yaml` | Working copy: runtime fields plus authoring-only `editor:` (overlay path/hash, calibration, notes, per-marker Z hints). Save-draft never publishes. |
| `published/<id>.yaml` | Runtime only (no `editor:`). Loaded by `ObjectMgr.loadPublishedQuestOverlays` without writing `tbc-db`. |

Stable ids live in **95000–99999** (avoids tbc-db rows and Hero quests 90001–90036 / NPCs 91001+).

Publish is an explicit editor action after validation. Do not insert into `tbc-db`.
