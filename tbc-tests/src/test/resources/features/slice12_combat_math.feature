@slice12
Feature: Slice 12 offhand auto-attack timer
  Dual-wield white swings use a separate offhand timer and HITINFO_LEFTSWING.
  In-process mock 8606 client. Do not mark p0_client.

  Background:
    Given a logged-in character standing next to Kobold Vermin 6
    And the player has an offhand weapon

  Scenario: Offhand swing after the offhand timer
    When the player starts auto-attack
    Then no SMSG_ATTACKERSTATEUPDATE has HITINFO_LEFTSWING
    When 1999 ms elapse on the combat session
    Then no SMSG_ATTACKERSTATEUPDATE has HITINFO_LEFTSWING
    When 1 ms elapse on the combat session
    Then a SMSG_ATTACKERSTATEUPDATE has HITINFO_LEFTSWING

  Scenario: Offhand timer uses UNIT_FIELD_BASEATTACKTIME plus one
    And the player's offhand attack time is 500 ms
    When the player starts auto-attack
    Then no SMSG_ATTACKERSTATEUPDATE has HITINFO_LEFTSWING
    When 499 ms elapse on the combat session
    Then no SMSG_ATTACKERSTATEUPDATE has HITINFO_LEFTSWING
    When 1 ms elapse on the combat session
    Then a SMSG_ATTACKERSTATEUPDATE has HITINFO_LEFTSWING

    Scenario: Melee range includes UNIT_FIELD_COMBATREACH
    And the player's combat reach is 4 yards
    When the attacker is 6 yards from the kobold
    And the player starts auto-attack
    Then the server has sent SMSG_ATTACKERSTATEUPDATE
