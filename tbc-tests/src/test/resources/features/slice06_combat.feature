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

  @tp-sl06-005
    Scenario: Hostile creature aggros when the player enters detection range
    Given the player is 10 yards from the kobold
    When 50 ms elapse on the world
    Then SMSG_ATTACKSTART includes the creature attacking the player

  @tp-sl06-006
  Scenario: Autostore corpse loot into bags
    When the player auto-attacks until the kobold is dead
    And the player loots the corpse
    Then SMSG_LOOT_RESPONSE is a corpse window for that guid
    When the player takes loot slot 0
    Then SMSG_ITEM_PUSH_RESULT is a loot push of item 25
    And SMSG_LOOT_REMOVED is loot slot 0

  @tp-sl06-007
  Scenario: Take corpse copper as solo looter
    When the player auto-attacks until the kobold is dead
    And the player loots the corpse
    Then SMSG_LOOT_RESPONSE is a corpse window for that guid
    When the player takes the corpse copper
    Then player copper increased by the corpse gold
    And the server has sent SMSG_LOOT_CLEAR_MONEY
    And the server has not sent SMSG_LOOT_MONEY_NOTIFY

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
  Scenario: Truncated autostore loot does not crash
    When the mock client sends CMSG_AUTOSTORE_LOOT_ITEM with no bytes
    Then the combat session still answers CMSG_PING with SMSG_PONG

  @negative
  Scenario: Pursuit timeout evades without loot
    Given the player is in combat with the kobold
    When the creature pursuit timer expires
    Then the kobold is at spawn with full health and an empty threat list
    And the player loots the kobold
    Then the server does not send SMSG_LOOT_RESPONSE
