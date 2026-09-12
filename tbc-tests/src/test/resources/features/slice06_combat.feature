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

  @tp-sl06-018
  Scenario: Out of range auto-attack does not pull the creature
    Given the player is 40 yards from the kobold
    When the player starts auto-attack
    Then the server has sent SMSG_ATTACKSWING_NOTINRANGE
    And SMSG_ATTACKSTART does not include the creature attacking the player
    And the kobold is not in combat

  @tp-sl06-018
  Scenario: Neutral creature does not aggro from an out-of-melee swing
    Given the kobold is faction 32 versus player faction 115
    And the player is 10 yards from the kobold
    When the player starts auto-attack
    And 500 ms elapse on the world
    Then SMSG_ATTACKSTART does not include the creature attacking the player
    And the kobold is not in combat

  @tp-sl06-006
  Scenario: Autostore corpse loot into bags
    When the player auto-attacks until the kobold is dead
    And the player loots the corpse
    Then SMSG_LOOT_RESPONSE is a corpse window for that guid
    When the player takes loot slot 0
    Then SMSG_ITEM_PUSH_RESULT is a loot push of item 25
    And SMSG_LOOT_REMOVED is loot slot 0
    And the backpack shows looted item 25 on the wire

  @tp-sl06-007
  Scenario: Take corpse copper as solo looter
    When the player auto-attacks until the kobold is dead
    And the player loots the corpse
    Then SMSG_LOOT_RESPONSE is a corpse window for that guid
    When the player takes the corpse copper
    Then player copper increased by the corpse gold
    And the server has sent SMSG_LOOT_CLEAR_MONEY
    And the server has not sent SMSG_LOOT_MONEY_NOTIFY

  @tp-sl06-016
  Scenario: Empty corpse is no longer lootable
    When the player auto-attacks until the kobold is dead
    And the player loots the corpse
    Then SMSG_LOOT_RESPONSE is a corpse window for that guid
    When the player takes loot slot 0
    And the player takes the corpse copper
    Then SMSG_LOOT_RELEASE_RESPONSE is for the kobold
    And the corpse is not lootable on the wire

  @tp-sl06-008
  Scenario: Corpse respawns after delay with health update
    Given the kobold respawn delay is 1 ms
    When the player auto-attacks until the kobold is dead
    And 50 ms elapse on the world
    Then the kobold is alive with full health
    And the server has sent an update object for unit health

  @tp-sl06-009
  Scenario: Leash evade sends attack stop and health update
    Given the player is in combat with the kobold
    When the player runs past the 30 yard leash
    Then SMSG_ATTACKSTOP is the kobold stopping attack on the player
    And the server has sent an update object for unit health

  @tp-sl06-019
  Scenario: Unreachable victim evades melee then resets after ten seconds
    Given the player is in combat with the kobold
    And the player is 20 yards above the kobold
    When 50 ms elapse on the world
    Then the kobold is in combat
    When the player starts auto-attack
    Then SMSG_ATTACKERSTATEUPDATE is an evade swing
    When 10000 ms elapse on the world
    Then SMSG_ATTACKSTOP is the kobold stopping attack on the player
    And the kobold is at spawn with full health and an empty threat list

  @tp-sl06-020
  Scenario: Player leaves combat when the creature evades
    Given the player is in combat with the kobold
    When the player runs past the 30 yard leash
    Then the player is not in combat
    And SMSG_ATTACKSTOP is the player stopping attack on the kobold

  @tp-sl06-021
  Scenario: Creature chases when the player leaves melee
    Given the player is in combat with the kobold
    When the player is 15 yards from the kobold
    And 150 ms elapse on the world
    Then the server has sent SMSG_MONSTER_MOVE
    And the server has not sent SMSG_ATTACKERSTATEUPDATE

  @tp-sl06-022
  Scenario: Creature evades when it is too far from combat start
    Given the player is in combat with the kobold
    When the kobold is 91 yards from combat start
    Then the kobold is not in combat
    And the kobold is alive with full health
    And SMSG_ATTACKSTOP is the kobold stopping attack on the player

  @tp-sl06-023
  Scenario: Player cannot melee a creature they are not facing
    Given the player is 2 yards from the kobold facing away
    When the player starts auto-attack
    Then the server has sent SMSG_ATTACKSWING_BADFACING
    And the server has not sent SMSG_ATTACKERSTATEUPDATE

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
