Feature: Accounts
  Liked songs, play history and downloads belong to an account: the same on every device
  the listener logs in on, and never visible to anyone else. A login lasts 4 hours.

  Background:
    Given the catalogue contains the songs "Tum Hi Ho, Kesariya, Chaiyya Chaiyya"

  Scenario: Likes and history follow the account to another device
    When "phone" signs up as "riya" with password "secret-123"
    And listener "phone" likes "Kesariya"
    And listener "phone" plays "Tum Hi Ho"
    When "laptop" logs in as "riya" with password "secret-123"
    And listener "laptop" likes "Chaiyya Chaiyya"
    Then the liked songs of "laptop" are "Chaiyya Chaiyya, Kesariya"
    And the play history of "laptop" is "Tum Hi Ho"
    And the liked songs of "phone" are "Chaiyya Chaiyya, Kesariya"

  Scenario: Listeners never see each other's data
    When "phone" signs up as "riya" with password "secret-123"
    And "laptop" signs up as "aman" with password "secret-456"
    And listener "phone" likes "Kesariya"
    And listener "phone" plays "Tum Hi Ho"
    Then the liked songs of "laptop" are empty
    And the play history of "laptop" is empty
    And "Kesariya" is shown as liked for "phone" but not for "laptop"

  Scenario: Listener data needs a login
    Then listener data of "tablet" is rejected without a login
    And "tablet" can still browse the catalogue without logging in

  Scenario: A login expires after 4 hours
    When "phone" signs up as "riya" with password "secret-123"
    And listener "phone" likes "Kesariya"
    And 4 hours pass since "phone" logged in
    Then requests from "phone" are rejected until it logs in again
    When "phone" logs in as "riya" with password "secret-123"
    Then the liked songs of "phone" are "Kesariya"

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
