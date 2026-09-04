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
