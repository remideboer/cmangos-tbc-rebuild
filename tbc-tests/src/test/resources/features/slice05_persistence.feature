@slice05
Feature: Slice 5 character persistence
  Logout SaveToDB keeps gold, position, action bars, bags, and rest.
  In-process mock 8606 client. No official client. Do not mark p0_client.

  Background:
    Given an in-memory world

  @tp-sl05-001
  Scenario: Relog keeps gold, position, and action bars
    Given a logged-in character with gold 12345 and a modified action button
    When the player logs out and relogs on a new mock client
    Then SMSG_LOGIN_VERIFY_WORLD has the saved map and position
    And SMSG_ACTION_BUTTONS has 132 packed uint32 and the modified button

  @tp-sl05-002
  Scenario: Bag item survives immediate logout
    Given a logged-in character who obtained item 25
    When the player logs out immediately and relogs
    Then the reconstructed bags still contain item 25

  @tp-sl05-003
  Scenario: Inn rest XP does not decrease
    Given a logged-in character resting in an inn with rest bonus 100
    When the player logs out and relogs later
    Then rest bonus is not below 100
    And the login burst still contains SMSG_SET_REST_START

  @tp-sl05-004
  Scenario: Save does not duplicate the character
    Given a logged-in character
    When the store saves the character twice
    Then the store still has exactly one row for that guid

  @tp-sl05-005
  Scenario: World start clears online flags
    Given a character left online as after a crash
    When the world clears online accounts
    Then no character is online

  @tp-sl05-006
  Scenario: Periodic save keeps gold after crash without logout
    Given a logged-in character with gold 12345 and a modified action button
    When the first periodic save window elapses
    And the world process is killed without logout
    And the player relogs on a new mock client
    Then SMSG_LOGIN_VERIFY_WORLD has the saved map and position
    And SMSG_ACTION_BUTTONS has 132 packed uint32 and the modified button

  @tp-sl05-007
  Scenario: Disconnect save keeps gold after combat TCP close
    Given a logged-in character with gold 12345 and a modified action button
    And the player is in combat
    When the TCP connection closes
    And 60 seconds elapse
    And the player relogs on a new mock client
    Then SMSG_LOGIN_VERIFY_WORLD has the saved map and position
    And SMSG_ACTION_BUTTONS has 132 packed uint32 and the modified button

  @negative
  Scenario: Unknown guid does not create a character
    Given a logged-in character
    When the player requests logout
    And the mock client sends CMSG_PLAYER_LOGIN with a guid that is not on the account
    Then the server sends SMSG_CHARACTER_LOGIN_FAILED with uint8 0x05
    And the character count for the account is unchanged

  @negative
  Scenario: Duplicate in-world login is refused
    Given a character already logged in
    When a second mock client logs in the same guid
    Then the server sends SMSG_CHARACTER_LOGIN_FAILED with uint8 0x02

  @negative
  Scenario: Buyback is not persisted
    Given a buyback slot occupied
    When the player logs out and relogs
    Then bags do not contain the buyback item

  @negative
  Scenario: Empty action bar does not invent buttons
    Given all 132 action buttons are zero
    When the player logs out and relogs
    Then SMSG_ACTION_BUTTONS is 132 zero uint32

  @negative
  Scenario: Truncated login payload does not crash
    Given a logged-in character
    When the player requests logout
    And the mock client sends CMSG_PLAYER_LOGIN with fewer than 8 bytes
    Then the session still answers CMSG_PING with SMSG_PONG

  @negative
  Scenario: Child-table SQL failure still keeps gold
    Given the action-bar table is unavailable
    When the player logs out with gold 50
    Then reconstructed gold is 50

  @negative
  Scenario: Load must not apply another account's guid
    Given a logged-in character
    When the player requests logout
    And load is asked for this guid on a different account id
    Then load returns empty and no SMSG login burst is sent
