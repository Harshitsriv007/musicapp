Feature: Accounts
  Logging in shows the same liked songs and play history on every device.

  Background:
    Given the catalogue contains the songs "Tum Hi Ho, Kesariya, Chaiyya Chaiyya"

  Scenario: Likes and history follow the account to another device
    When listener "phone" likes "Kesariya"
    And listener "phone" plays "Tum Hi Ho"
    And "phone" signs up as "riya" with password "secret-123"
    When "laptop" logs in as "riya" with password "secret-123"
    And listener "laptop" likes "Chaiyya Chaiyya"
    Then the liked songs of "laptop" are "Chaiyya Chaiyya, Kesariya"
    And the play history of "laptop" is "Tum Hi Ho"
    And the liked songs of "phone" are "Chaiyya Chaiyya, Kesariya"

  Scenario: Logging in merges likes without duplicates
    When "phone" signs up as "riya" with password "secret-123"
    And listener "phone" likes "Kesariya"
    And listener "tablet" likes "Kesariya"
    And listener "tablet" likes "Tum Hi Ho"
    And "tablet" logs in as "riya" with password "secret-123"
    Then the liked songs of "tablet" are "Tum Hi Ho, Kesariya"

  Scenario: Forgot password sets a new one and logs out other devices
    When "phone" signs up as "riya" with password "secret-123"
    And "laptop" resets the password of "riya" to "new-pass-456"
    Then logging in as "riya" with password "secret-123" is rejected
    And requests from "phone" are rejected until it logs in again
    When "phone" logs in as "riya" with password "new-pass-456"
    Then the liked songs of "phone" are empty

  Scenario: Wrong password, taken username and logout
    When "phone" signs up as "riya" with password "secret-123"
    Then signing up as "Riya" is rejected as a conflict
    And logging in as "riya" with password "wrong-pass" is rejected
    When "phone" logs out
    Then requests from "phone" are rejected until it logs in again
