# CompanionPartyBar

Companion-only controls for the offline-character companion. The movable frame appears beside the party frames by default and shows:

- Attack, Follow, and Stay
- the four spell slots supplied by the server
- Aggressive, Defensive, and Passive

The title is the summoned character's name. Buttons send their slot and the current target GUID through `LANG_ADDON`; the server validates the slot and executes it through the normal pet-action handler.

## Install

From `tbc-server` run:

```bat
content\install-addons.bat
```

Or run `build.bat`, which packages the server and installs all AddOns when the lab client is available. Enable **CompanionPartyBar** on the character-select AddOns screen, then log in or `/reload`.

## Move and reset

- Outside combat, drag the frame by its header/background.
- Use `/cpb reset` to return to the default party-frame position.

The position is stored in `CompanionPartyBarDB`.

## Stock pet bar and rollback

The AddOn hides the stock pet bar only while the server reports an active offline-character companion. Dismissing the companion restores normal pet UI behavior, and hunter/warlock pets do not activate this frame.

To roll back, disable the AddOn or delete `Interface\AddOns\CompanionPartyBar`. No FrameXML, MPQ, or client binary is modified.
