# Unsigned 8606 lab client (`wowme.exe`)

Stock Blizzard `Wow.exe` (build **8606**) refuses modified DBC files inside MPQs with:

```text
ERROR #131 (0x85100083) File Corrupt
File: DBFilesClient\Spell.dbc
```

That is a **signaturefile / content-hash** check (and related archive RSA), not a corrupt WDBC. The lab needed an unsigned sibling binary so the content pipeline can overlay `Spell.dbc` without replacing stock `Wow.exe`.

## Policy

| File | Role |
|------|------|
| `WoW-2.4.3-client\Wow.exe` | **Stock** — never overwrite. Official enUS MD5 `57c5c03097103e15f9abe2803aebdc3c` (anzz1/wow-client-checksums). |
| `WoW-2.4.3-client\wowme.exe` | **Patched copy** of stock — used for lab play with custom DBCs. |
| Launcher `client.path` | Points at `Wow.exe`; at start, prefers sibling `wowme.exe` if present ([`ClientLauncher.resolveLaunchExecutable`](../tbc-server/tbc-launcher/src/main/java/org/tbc/launcher/ClientLauncher.java)). |

Do **not** commit `wowme.exe` as a substitute for documenting the patch. Recreate with [`patch-wowme.ps1`](patch-wowme.ps1) whenever stock is restored.

## Why stock rejects custom `Spell.dbc`

1. MPQs contain / consult `signaturefile` entries (path + hash).
2. When a file is read, the client compares a 16-byte hash of the payload to the signature entry.
3. Mismatch → push error `0x85100083` → ERROR #131 “File Corrupt”.
4. Separately, opening some archives runs an RSA-style `SFileAuthenticateArchiveEx` path (same family as [Blizzard-Patching](../Blizzard-Patching/README.md) / in-client `wow-patch.mpq` auth). That path matters for signed patch packages; the DBC lab crash is dominated by (2)–(3).

Community reports for the same #131 on edited Spell.dbc + 2.4.3: [stoneharry/WoW-Spell-Editor#192](https://github.com/stoneharry/WoW-Spell-Editor/issues/192) (resolved by running wowme / unsigned client).

## How `wowme.exe` was created

1. Copy `Wow.exe` → `wowme.exe` (same size, ~8 272 528 bytes).
2. Apply three surgical byte patches (file offsets below; image base `0x00400000`).
3. Leave `Wow.exe` bytes unchanged.

Verified stock identity before patch: MD5 `57c5c03097103e15f9abe2803aebdc3c`.

### Patch A — skip ERROR #131 on hash mismatch (DBC / signed file path)

| | |
|--|--|
| Function | File-hash gate starting at file offset `0x253F60` |
| Site | `0x253F85`: `test al, al` / `jz` over the error block |
| Original | `84 C0 74 1E` |
| Patched | `84 C0 EB 1E` (`jz` → `jmp`) |

If the 16-byte compare reports a mismatch, stock shows #131. The unconditional jump always takes the success return (`bl` stays 1).

### Patch B — 16-byte compare always “equal”

| | |
|--|--|
| Function | Memcmp-style compare at `0x253D00` (length `0x10`), sole caller is the gate above |
| Original | Starts `8B 44 24 04 56 BA …` |
| Patched | `33 C0 C2 04 00` (`xor eax, eax` / `ret 4`) → treat as match |

Belt-and-suspenders with patch A.

### Patch C — archive authenticate wrapper always succeeds

| | |
|--|--|
| Function | Wrapper that pushes `"ARCHIVE"` and calls into Mopaq auth (`0x253B20`, `ret 0x18`) |
| Site | Rewrite from `0x253B49` (19 bytes) |
| Effect | Force `authresult = 5`, `bl = 1`, store result, jump past the failure/`GetLastError` path |

Same idea as [`SFileAuthenticateArchiveEx_fixed.cpp`](../Blizzard-Patching/Read/SFileAuthenticateArchiveEx_fixed.cpp) (authresult ≥ 5 means “accept”), adapted to this 8606 Windows binary. Enables unsigned archive auth for lab / future in-client patch experiments; not a substitute for patch A for Spell.dbc.

## Recreate

From `tbc-server` (PowerShell):

```powershell
.\content\patch-wowme.ps1
```

Optional:

```powershell
.\content\patch-wowme.ps1 -ClientDir 'D:\path\to\WoW-2.4.3-client'
```

The script refuses to modify `Wow.exe`, copies stock → `wowme.exe`, applies A/B/C, and prints MD5s.

## Related lab wiring

- Overlay mount: `Data\enUS\patch-enUS-3.MPQ` (stock open list; custom names like `patch-tbc-custom.MPQ` are never loaded) — [`install-client-patch.bat`](install-client-patch.bat).
- `build.bat` installs the overlay after a successful content compile when lab client `Data` is present (`CONTENT_INSTALL_PATCH=0` to skip).
- After install: fully quit and start **`wowme.exe`** (or the TBC launcher). Stock `Wow.exe` + overlay still #131 by design.

## Out of scope / next

Unsigning only allows the client to **load** modified DBCs. Seal of Righteousness tips are plain text (**see combat log**); combat log is the damage authority — [`sor-tip-combat-align.md`](sor-tip-combat-align.md).
