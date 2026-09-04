@slice06
Feature: Slice 6 melee combat and loot
  Auto-attack a starter-zone Kobold Vermin (id 6) until it dies, then loot.
  Evade resets spawn and health. In-process mock 8606 client. Do not mark p0_client.

  Background:
    Given a logged-in character standing next to Kobold Vermin 6

  @tp-sl06-001
  Scenario: Auto-attack until dead then loot
    When the player auto-attacks until the kobold is dead
    Then the server has sent SMSG_ATTACKERSTATEUPDATE
    When the player loots the corpse
    Then SMSG_LOOT_RESPONSE is a corpse window for that guid

  @tp-sl06-002
  Scenario: Mob evades past leash and resets
    Given the player is in combat with the kobold
    When the player runs past the 30 yard leash
    Then the kobold is at spawn with full health and an empty threat list

  @tp-sl06-003
  Scenario: Creature starts auto-attack when the player swings
    When the player starts auto-attack
    Then SMSG_ATTACKSTART includes the creature attacking the player

  @negative
  Scenario: Living creature has no loot window
    When the player loots the living kobold
    Then the server does not send SMSG_LOOT_RESPONSE

  @negative
  Scenario: Evade clears loot
    Given the player is in combat with the kobold
    When the player runs past the 30 yard leash
    And the player loots the kobold
    Then the server does not send SMSG_LOOT_RESPONSE

  @negative
  Scenario: Other account cannot loot the tagged corpse
    When the player auto-attacks until the kobold is dead
    And a second mock client loots the same corpse
    Then the second client does not receive SMSG_LOOT_RESPONSE

  @negative
  Scenario: Truncated loot payload does not crash
    When the mock client sends CMSG_LOOT with fewer than 8 bytes
    Then the combat session still answers CMSG_PING with SMSG_PONG

  @negative
  Scenario: Pursuit timeout evades without loot
    Given the player is in combat with the kobold
    When the creature pursuit timer expires
    Then the kobold is at spawn with full health and an empty threat list
    And the player loots the kobold
    Then the server does not send SMSG_LOOT_RESPONSE
