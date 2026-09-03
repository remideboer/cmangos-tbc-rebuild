@slice04
Feature: Slice 4 two-client world
  Two mock 8606 clients on one in-memory world see each other move, say, and name-query.
  Do not mark p0_client.

  Background:
    Given two logged-in characters in visibility named Alpha and Bravo

  @tp-sl04-003
  Scenario: Observer sees movement with fallTime
    When Alpha heartbeats one yard east
    Then Bravo received Alpha movement with packed GUID and fallTime
    And Alpha did not receive a movement echo

  @tp-sl04-004
  Scenario: Say reaches a player in range
    When Alpha says test
    Then Bravo received CHAT_MSG_SAY

  @tp-sl04-005
  Scenario: Other player create and name query
    Then Bravo saw Alpha's model without UPDATEFLAG_SELF
    And Alpha saw Bravo's model without UPDATEFLAG_SELF
    When Alpha queries Bravo's name
    Then Alpha received Bravo's name

  @tp-sl04-010
  Scenario: Whisper delivers to target and inform to sender
    When Alpha whispers Bravo hello
    Then Bravo received CHAT_MSG_WHISPER
    And Alpha received CHAT_MSG_WHISPER_INFORM

  @negative
  Scenario: Say does not reach forty yards
    Given Bravo stands 40 yards from Alpha
    When Alpha says test
    Then Bravo did not receive CHAT_MSG_SAY
