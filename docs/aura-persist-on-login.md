# Aura remaining duration on logout / login

Lab bug (fixed): buffs survived relog as icons/modifiers but the client showed **0s** and they never expired.

## Why

| Layer | Required |
|-------|----------|
| SQL / memory | `character_aura.remaintime` = remaining ms (`−1` only for permanent) |
| Server holder | `expireAtMs = now + remaintime` after load |
| Client | `SMSG_UPDATE_AURA_DURATION` (slot + remain ms) after create-self |

Cast already sends duration via `AuraSlots.sendApply`. Login must call `SpellEngine.sendPersistedAuraDurations` **after** create-self in `LoginBurst` (CMaNGOS `SpellAuraHolder::SendAuraDuration`).

## Do not

- Save timed auras (`durationMs > 0`) as `remaintime = −1` just because `expireAtMs <= 0`.
- Treat `UNIT_FIELD_AURA` on create-self as sufficient for TP-SL07-017 — assert the duration opcode too.

## Code map

| Step | Type |
|------|------|
| Save / load | `CharacterStore.writeAuras` / `loadAuras` |
| Re-apply mods + slots | `SpellEngine.restorePersistedAuras` |
| Wire duration | `SpellEngine.sendPersistedAuraDurations` ← `LoginBurst` |
| Expire pulse | `AuraSlots.expireTimed` (needs `durationMs > 0` **and** `expireAtMs > 0`) |

## Tests

- `Slice07P0Test.tpSl07AuraSurvivesRelog` / `tpSl07AuraDurationSurvivesRelog`
- `CharacterStoreAuraTest` (including timed-missing-expire must not reload permanent)
- `SpellEngineRestoreAurasTest` (`sendPersistedAuraDurations*`)

Cursor rule (fires on login/persist/aura files): `.cursor/rules/aura-persist-login.mdc`.
