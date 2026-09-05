@slice09
Feature: Slice 9 social and economy
  Two mock 8606 clients on one in-memory world: party invite and list,
  party chat, trade both-accept, who display cap, friends, and mail take-item.
  Do not mark p0_client.

  Background:
    Given two logged-in characters in range named Alpha and Bravo

  @tp-sl09-001
  Scenario: Party invite, list, party chat, and trade
    When Alpha invites Bravo to the party
    Then Bravo received SMSG_GROUP_INVITE
    When Bravo accepts the party invite
    Then both received SMSG_GROUP_LIST omitting themselves
    When Alpha sends party chat hello
    Then Bravo received CHAT_MSG_PARTY
    Given Alpha has item 25 in a bag slot
    When they trade that item and both accept
    Then Bravo bags contain item 25
    And Alpha no longer has that item guid

  @tp-sl09-002
  Scenario: Mail send with item and take by guid-low
    Given Alpha has item 25 in a bag slot
    And Alpha has 100 copper
    When Alpha mails that item to Bravo
    And Bravo takes the mailed item using uint32 guid-low
    Then Bravo bags contain item 25
    And Alpha no longer has that item guid

  @tp-sl09-003
  Scenario: Who lists the other player and caps at 49
    When Alpha queries who
    Then SMSG_WHO lists Bravo and displayCount is at most 49
    Given 48 extra in-world players of the same faction
    When Alpha queries who
    Then SMSG_WHO displayCount is 49 and matchCount is 50

  @tp-sl09-004
  Scenario: Friend persists across logout
    When Alpha adds Bravo as a friend
    And both characters log out and relog
    Then Alpha contact list contains Bravo

  @tp-sl09-005
  Scenario: Guild invite and accept
    Given Alpha created a guild named "Lions"
    When Alpha invites Bravo to the guild
    Then Bravo received SMSG_GUILD_INVITE from Alpha for "Lions"
    When Bravo accepts the guild invite
    Then both received SMSG_GUILD_EVENT joined Bravo

  @tp-sl09-006
  Scenario: Auction sell item
    Given Alpha has item 25 in a bag slot
    And Alpha has 100 copper
    When Alpha lists that item at Chilton for 12 hours starting at 100 copper
    Then Alpha received SMSG_AUCTION_COMMAND_RESULT started ok
    And Alpha no longer has that item guid

  @tp-sl09-007
  Scenario: Auction place bid
    Given Bravo has 200 copper
    When Bravo bids 100 copper on auction 1 at Chilton
    Then Bravo received SMSG_AUCTION_COMMAND_RESULT bid placed ok

  @tp-sl09-008
  Scenario: Delete friend
    When Alpha adds Bravo as a friend
    And Alpha removes Bravo as a friend
    Then Alpha received friend result 5

  @negative
  Scenario: Invite unknown name
    When Alpha invites Nobody to the party
    Then Alpha received party result 1

  @negative
  Scenario: Trade too far
    Given Bravo is 20 yards away
    When Alpha initiates trade with Bravo
    Then Alpha received trade status 10

  @negative
  Scenario: Cannot friend self
    When Alpha adds Alpha as a friend
    Then Alpha received friend result 9

  @negative
  Scenario: Truncated who still answers ping
    When Alpha sends CMSG_WHO with fewer than 8 bytes
    Then the social session still answers CMSG_PING with SMSG_PONG
