Feature: New Bollywood songs are fetched automatically
  The cron job pulls songs from the free music API, publishes them to Kafka,
  and the consumer stores them so they appear in the "New songs" list.

  Scenario: Songs fetched by the job appear in the new songs list
    Given the free music API offers these "bollywood" songs:
      | id | title           | artist        | released   |
      | a1 | Tum Hi Ho       | Arijit Singh  | 2013-04-01 |
      | a2 | Kesariya        | Arijit Singh  | 2022-07-17 |
      | a3 | Chaleya         | Arijit Singh  | 2023-08-14 |
    When the song ingestion job runs
    Then the new songs list eventually shows, newest release first:
      | Chaleya   |
      | Kesariya  |
      | Tum Hi Ho |
    And the song "Kesariya" by "Arijit Singh" is playable with artwork

  Scenario: Running the job again does not duplicate songs
    Given the free music API offers these "bollywood" songs:
      | id | title     | artist       | released   |
      | a1 | Tum Hi Ho | Arijit Singh | 2013-04-01 |
      | a2 | Kesariya  | Arijit Singh | 2022-07-17 |
    When the song ingestion job runs
    Then the catalogue eventually contains 2 songs
    When the song ingestion job runs again
    Then the catalogue still contains 2 songs

  Scenario: The job survives the music API being down
    Given the free music API is down
    When the song ingestion job runs
    Then the catalogue still contains 0 songs
