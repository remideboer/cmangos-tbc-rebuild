@slice08
Feature: Slice 8 content load
  Goldshire NPCs from creature rows, gossip vendor buy of Worn Shortsword 25,
  and starter quest 783 A Threat Within. In-process mock 8606 client. Do not mark p0_client.

  Background:
    Given a logged-in character in Elwynn

  @tp-sl08-001
  Scenario: Goldshire NPCs exist
    When the player enters Goldshire
    Then Marshal Dughan 240 is on map 0

  @tp-sl08-002
  Scenario: Gossip vendor buy one item
    Given the player has 50 copper
    When the player talks to Corina Steele 54
    Then the server sends SMSG_GOSSIP_MESSAGE
    When the player buys item 25 from that vendor
    Then bags contain item 25
    And gold is 15 copper

  @tp-sl08-004
  Scenario: Random-movement creature wanders out of combat
    Given Kobold Vermin 6 has MovementType 1 and spawndist 10
    When one second elapses out of combat
    Then SMSG_MONSTER_MOVE is a walk spline for that kobold within spawn distance

  @negative
  Scenario: Vendor buy without copper fails
    Given the player has 0 copper
    When the player talks to Corina Steele 54
    And the player buys item 25 from that vendor
    Then the server sends SMSG_INVENTORY_CHANGE_FAILURE with result 29
    And bags do not contain item 25

  @negative
  Scenario: Kobold is not a vendor
    When the player buys item 25 from Kobold Vermin 6
    Then bags do not contain item 25

  @negative
  Scenario: Truncated gossip does not crash
    When the mock client sends CMSG_GOSSIP_HELLO with fewer than 8 bytes
    Then the content session still answers CMSG_PING with SMSG_PONG

  @negative
  Scenario: Turn-in without accepting does nothing
    When the player talks to Marshal McBride 197
    And the player turns in quest 783
    Then the server does not send SMSG_QUESTGIVER_QUEST_COMPLETE
