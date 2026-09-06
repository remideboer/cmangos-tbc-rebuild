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

  @tp-sl10-004
  Scenario: Solo sets dungeon difficulty to heroic
    Given Alpha is level 70
    When Alpha sets dungeon difficulty to 1
    Then Alpha difficulty is 1
    And Alpha received MSG_SET_DUNGEON_DIFFICULTY mode 1 inGroup 0

  @tp-sl10-005
  Scenario: Sixth new instance this hour is aborted
    Given Alpha is a non-GM player
    When Alpha enters five new Ragefire instances then leaves each
    And Alpha enters Ragefire trigger 2230 again
    Then Alpha received SMSG_TRANSFER_ABORTED for map 389 reason 3
    And Alpha is on map 0

  @tp-sl10-006
  Scenario: Leave WSG returns to entry point
    When Alpha joins the WSG queue
    And Alpha ports into the battleground
    Then Alpha is on map 489
    When Alpha leaves the battlefield type 2
    Then Alpha is on map 0
    And Alpha received SMSG_NEW_WORLD for map 0
