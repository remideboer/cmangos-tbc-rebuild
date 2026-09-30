# Seal of Righteousness — tip / combat alignment (8606)

## Verdict

On 8606, **live tip tokens** (`$MW` / `$mw` / `$MWS` / `$mwb`) do **not** reliably match server combat.
Lab saw action bar **6**, buff **10**, combat log **5** (claymore) with a correct one-formula DBC in `patch-enUS-3.MPQ`.

Causes (can stack):

| Source | Effect |
|--------|--------|
| Tip reads **Item.dbc** / unit display damage | Speed/avg can differ from `item_template` (e.g. 3.5s vs 2.9s → tip trunc **6**) |
| Tip may include **AP** in `$MW`/`$mw` | CMaNGOS SoR uses **base** weapon damage only |
| Buff vs bar evaluate at different times | Snapshot vs live → **10** vs **6** |

Toggling `$MW` vs `$MWB` and UNIT-vs-item on the server did not clear 6/10/5.

## Contract (current)

| Surface | What the player sees |
|---------|----------------------|
| Action bar / buff tip | Plain text: *additional Holy damage based on your weapon (**see combat log**)* — **no false number** |
| Combat log | Server `sealOfRighteousnessDamage` (claymore 3–5 @ 2.9s, SP 0 → **5**) |

The phrase **see combat log** is an overlay marker: if tips still show a holy **number**, `Spell.dbc` from `patch-enUS-3.MPQ` is not what the client is using (wrong exe, stale WDB, or patch not mounted).

## Server formula (unchanged)

```text
1.2 * (m1 * 1.2 * 1.03 * speed / 100) + 0.03 * avg + 1  [+ holySp * 0.108 * speed]
```

`m1` = EffectBasePoints+1 (rank 1 → 108). Weapon from `MainhandWeaponStats`. Holy SP from `Player.holySpellPower()` when gear registers it.

## Lab checklist

1. Rebuild/install overlay (`build.bat` or compile + `install-client-patch.bat`).
2. Fully quit → **`wowme.exe`** (not stock `Wow.exe`).
3. Hover SoR: text must include **see combat log**, not “6” / “10” Holy.
4. Swing: combat log Holy = **5** on Battleworn Claymore @ SP 0.

## Future (optional)

- Patch **Item.dbc** weapon lines to match `item_template`, then revisit a numeric tip.
- Or a client that evaluates tip tokens from server-pushed values (not stock 8606).

## Related

- YAML: `content/spells/seal-of-righteousness.yaml`
- Unsigned client: `content/unsigned-client-8606.md`
- Java: `SpellEngine.sealOfRighteousnessDamage`
