# HeroPowerBars

8606 AddOn: for **Hero** (class 6 / classless), replaces the single `PlayerFrameManaBar` fill with **three equal segments** — mana (blue), rage (red), energy (yellow).

Values are pushed by the server over `LANG_ADDON` (`HeroPowerBars\tPowerUpdate#…`) because stock `UnitRage` / `UnitEnergy` stay empty when primary power is mana.

Stock **FrameXML is not modified**. Deleting this AddOn fully restores the original UI (that is the backup).

## Requirements

- Server with `ClasslessPowerAddon` (LANG_ADDON enable → PowerUpdate).
- This AddOn enabled on the character.

## Install

From `tbc-server/content`:

```bat
install-addons.bat
```

Or copy this folder to:

`WoW-2.4.3-client\Interface\AddOns\HeroPowerBars\`

Then: character select → AddOns → enable **HeroPowerBars** → enter world (or `/reload`).

## Disable / restore stock bar

1. Disable the AddOn in the character AddOns list, **or**
2. Delete `Interface\AddOns\HeroPowerBars\`.

No MPQ or FrameXML restore needed.
