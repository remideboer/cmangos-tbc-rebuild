@slice07
Feature: Slice 7 starter spell cast
  Cast Fireball (id 133) at Kobold Vermin 6. Mana spent and
  SMSG_SPELLNONMELEEDAMAGELOG match the health delta. Out of range is
  SMSG_CAST_RESULT 0x5C. In-process mock 8606 client. Do not mark p0_client.

  Background:
    Given a logged-in caster who knows Fireball 133 next to Kobold Vermin 6

  @tp-sl07-001
  Scenario: Cast Fireball, spend mana, combat log matches health
    When the player casts Fireball 133 on the kobold
    Then mana spent equals the Fireball cost
    And SMSG_SPELLNONMELEEDAMAGELOG damage matches the kobold health loss
    And the server does not send SMSG_CAST_RESULT

  @tp-sl07-002
  Scenario: Out of range cast does not disconnect
    When the player stands 40 yards from the kobold
    And the player casts Fireball 133 on the kobold
    Then the server sends SMSG_CAST_RESULT with result 0x5C
    And the spell session still answers CMSG_PING with SMSG_PONG

  @negative
  Scenario: Unknown spell id is ignored
    When the player casts unknown spell 1 on the kobold
    Then the server does not send SMSG_CAST_RESULT
    And the server does not send SMSG_SPELL_GO

  @negative
  Scenario: Unlearned spell is not known
    When the player casts unlearned spell 2050 on the kobold
    Then the server sends SMSG_CAST_RESULT with result 0x3B

  @negative
  Scenario: No mana fails without spending
    Given the caster has 0 mana
    When the player casts Fireball 133 on the kobold
    Then the server sends SMSG_CAST_RESULT with result 0x50
    And mana remaining is 0

  @negative
  Scenario: Truncated cast payload does not crash
    When the mock client sends CMSG_CAST_SPELL with fewer than 4 bytes
    Then the spell session still answers CMSG_PING with SMSG_PONG

  @negative
  Scenario: Dummy with no plugin does not crash
    When the player casts dummy 836
    Then the spell session still answers CMSG_PING with SMSG_PONG
