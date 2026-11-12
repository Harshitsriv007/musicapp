Feature: Search songs by name, artist or movie
  One search box finds songs, artist folders and movie folders.

  Background:
    Given the free music API offers these "bollywood" songs:
      | id | title                          | artist         | released   |
      | s1 | Kesariya (From "Brahmastra")   | Arijit Singh   | 2022-07-17 |
      | s2 | Deva Deva (From "Brahmastra")  | Arijit Singh   | 2022-08-08 |
      | s3 | Chaleya (From "Jawan")         | Arijit Singh   | 2023-08-14 |
      | s4 | Tum Kya Mile                   | Shreya Ghoshal | 2023-06-28 |
      | s5 | Mile Ho Tum (Reprise)          | Neha Kakkar    | 2021-02-01 |
      | s6 | 100% Love                      | DJ Snake Fan   | 2024-01-01 |
    When the song ingestion job runs
    Then the catalogue eventually contains 6 songs

  Scenario: Search by song name, best matches first
    When I search for "mile"
    Then the found songs are "Mile Ho Tum (Reprise), Tum Kya Mile"

  Scenario: Search by artist, any case
    When I search for "SHREYA"
    Then the found artists are "Shreya Ghoshal"
    And the found songs are "Tum Kya Mile"

  Scenario: Search by movie name
    When I search for "brahma"
    Then the found movies are "Brahmastra"
    And the found songs are "Deva Deva (From \"Brahmastra\"), Kesariya (From \"Brahmastra\")"

  Scenario: Special characters are matched literally
    When I search for "100%"
    Then the found songs are "100% Love"
    When I search for "%"
    Then the found songs are "100% Love"

  Scenario: Nothing found
    When I search for "zzz"
    Then the found songs are empty
