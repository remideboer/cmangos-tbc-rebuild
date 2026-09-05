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

  @tp-sl08-006
  Scenario: Vendor gossip lists browse-goods
    When the player talks to Corina Steele 54
    Then SMSG_GOSSIP_MESSAGE has a vendor option

  @tp-sl08-007
  Scenario: NPC text query fills default greetings
    When the player talks to Corina Steele 54
    And the player queries that gossip NPC text
    Then SMSG_NPC_TEXT_UPDATE has 8 greeting slots

  @tp-sl08-008
  Scenario: Vendor gossip select opens the shop
    When the player talks to Corina Steele 54
    And the player selects the vendor gossip option
    Then the server sends SMSG_LIST_INVENTORY for that vendor

  @tp-sl08-009
  Scenario: Trainer gossip select opens the trainer list
    When the player talks to Llane Beshere 911
    And the player selects the trainer gossip option
    Then the server sends SMSG_TRAINER_LIST for that trainer

  @tp-sl08-010
  Scenario: Banker gossip select opens the bank
    When the player talks to Olivia Burnside 2455
    And the player selects the banker gossip option
    Then the server sends SMSG_SHOW_BANK for that banker

  @tp-sl08-011
  Scenario: Taxi gossip select opens the flight map
    When the player talks to Dungar Longdrink 352
    And the player selects the taxi gossip option
    Then the server sends SMSG_SHOWTAXINODES for that flight master

  @tp-sl08-012
  Scenario: Innkeeper gossip select asks to bind
    When the player talks to Innkeeper Farley 295
    And the player selects the innkeeper gossip option
    Then the server sends SMSG_BINDER_CONFIRM for that innkeeper

  @tp-sl08-013
  Scenario: Auctioneer gossip select opens the auction house
    When the player talks to Auctioneer Chilton 8670
    And the player selects the auctioneer gossip option
    Then the server sends MSG_AUCTION_HELLO for that auctioneer

  @tp-sl08-014
  Scenario: Innkeeper binder activate binds the hearth
    When the player talks to Innkeeper Farley 295
    And the player selects the innkeeper gossip option
    And the player confirms the inn bind
    Then the server sends SMSG_TRAINER_BUY_SUCCEEDED for bind spell 3286
    And SMSG_BINDPOINTUPDATE is the player's current location
    And SMSG_PLAYERBOUND is that innkeeper

  @tp-sl08-015
  Scenario: Innkeeper gossip select opens the inn-info submenu
    When the player talks to Innkeeper Farley 295
    And the player selects the gossip option "What can I do at an inn?"
    Then SMSG_GOSSIP_MESSAGE is menu 1221 with text 1853 and no gossip options

  @tp-sl08-016
  Scenario: Gossip option with negative action menu closes the window
    Given Innkeeper Farley has a gossip option that closes the menu
    When the player talks to Innkeeper Farley 295
    And the player selects the gossip option "Goodbye"
    Then the server sends empty SMSG_GOSSIP_COMPLETE
    And the server does not send a new SMSG_GOSSIP_MESSAGE

  @tp-sl08-017
  Scenario: Gossip option with a point of interest marks the map
    Given Innkeeper Farley has a gossip option that sends the Lion's Pride Inn POI
    When the player talks to Innkeeper Farley 295
    And the player selects the gossip option "Lion's Pride Inn"
    Then SMSG_GOSSIP_POI is Lion's Pride Inn
    And the server does not send a new SMSG_GOSSIP_MESSAGE

  @tp-sl08-018
  Scenario: Gossip option with a SQL condition is omitted
    Given Innkeeper Farley has a gossip option gated by a SQL condition
    When the player talks to Innkeeper Farley 295
    Then SMSG_GOSSIP_MESSAGE does not list "Locked"

  @tp-sl08-003
  Scenario: Accept and complete starter quest 783
    When the player talks to Deputy Willem 823
    And the player accepts quest 783
    Then quest 783 is in the quest log
    When the player talks to Marshal McBride 197
    And the player turns in quest 783
    Then the server sends SMSG_QUESTGIVER_QUEST_COMPLETE for quest 783
    And quest 783 is not in the quest log

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
