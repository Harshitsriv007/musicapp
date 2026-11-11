Feature: Liked songs and listening history
  Each listener (device) has their own liked songs and play history.

  Background:
    Given the catalogue contains the songs "Tum Hi Ho, Kesariya, Chaiyya Chaiyya"

  Scenario: A listener likes and unlikes a song
    When listener "riya" likes "Kesariya"
    And listener "riya" likes "Kesariya"
    Then the liked songs of "riya" are "Kesariya"
    And the liked songs of "aman" are empty
    And "Kesariya" is shown as liked for "riya" but not for "aman"
    When listener "riya" unlikes "Kesariya"
    Then the liked songs of "riya" are empty

  Scenario: Liked songs are listed most recent first
    When listener "riya" likes "Tum Hi Ho"
    And listener "riya" likes "Chaiyya Chaiyya"
    Then the liked songs of "riya" are "Chaiyya Chaiyya, Tum Hi Ho"

  Scenario: Play history shows the most recently played song first
    When listener "riya" plays "Tum Hi Ho"
    And listener "riya" plays "Chaiyya Chaiyya"
    And listener "riya" plays "Tum Hi Ho"
    Then the play history of "riya" is "Tum Hi Ho, Chaiyya Chaiyya, Tum Hi Ho"
    And the play history of "aman" is empty
    When listener "riya" clears the play history
    Then the play history of "riya" is empty

  Scenario: Liking a song that does not exist
    Then liking an unknown song is rejected as not found
