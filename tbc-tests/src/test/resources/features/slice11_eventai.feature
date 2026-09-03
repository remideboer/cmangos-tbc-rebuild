@slice11
Feature: Slice 11 EventAI
  Garrick Padfoot ACID row 10301 casts Defensive Stance 7164 on aggro.
  Missing ScriptName logs and does not crash. Do not mark p0_client.

  Background:
    Given a logged-in character standing next to Garrick Padfoot 103

  @tp-sl11-001
  Scenario: Garrick casts 7164 on aggro and unknown ScriptName logs
    When the player aggros Garrick Padfoot
    Then the server sends SMSG_SPELL_START for spell 7164
    And the server sends SMSG_SPELL_GO for spell 7164
    When a creature with missing ScriptName is spawned
    Then the eventai session still answers CMSG_PING with SMSG_PONG
