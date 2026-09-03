@slice10
Feature: Slice 10 instances and WSG
  Two mock 8606 clients enter Ragefire Chasm on one instance id,
  re-enter while bound, reset when empty, and port into WSG after WAIT_JOIN.
  Do not mark p0_client.

  Background:
    Given two grouped characters in visibility named Alpha and Bravo

  @tp-sl10-001
  Scenario: Grouped players share a Ragefire instance
    When both enter Ragefire trigger 2230
    Then both are on map 389 with the same instance id
    And both received SMSG_NEW_WORLD for map 389

  @tp-sl10-002
  Scenario: Bound re-enter then reset when empty
    When both enter Ragefire trigger 2230
    And they leave to Elwynn and re-enter Ragefire
    Then both are on map 389 with the same instance id
    When they leave to Elwynn
    And Alpha resets instances
    Then Alpha received SMSG_INSTANCE_RESET for map 389

  @tp-sl10-003
  Scenario: WSG invite then port lands on map 489
    When Alpha joins the WSG queue
    Then Alpha received WAIT_JOIN status 2
    When Alpha ports into the battleground
    Then Alpha is on map 489
    And Alpha received SMSG_NEW_WORLD for map 489
