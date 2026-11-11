Feature: Browse songs in folders
  The home screen groups songs into folders by release year, artist and movie,
  and a whole folder can be liked or downloaded at once.

  Background:
    Given the free music API offers these "bollywood" songs:
      | id | title                          | artist         | released   |
      | b1 | Kesariya (From "Brahmastra")   | Arijit Singh   | 2022-07-17 |
      | b2 | Deva Deva (From "Brahmastra")  | Arijit Singh   | 2022-08-08 |
      | b3 | Chaleya (From "Jawan")         | Arijit Singh   | 2023-08-14 |
      | b4 | Tum Kya Mile                   | Shreya Ghoshal | 2023-06-28 |
      | b5 | Lofi Bollywood Mashup          | NFThunk        | 2024-01-10 |
    When the song ingestion job runs
    Then the catalogue eventually contains 5 songs

  Scenario: Year folders, newest first
    Then the "years" folders are:
      | folder | songs |
      | 2024   | 1     |
      | 2023   | 2     |
      | 2022   | 2     |
    And the "year" folder "2023" contains "Chaleya (From \"Jawan\"), Tum Kya Mile"

  Scenario: Artist folders, biggest first
    Then the "artists" folders are:
      | folder         | songs |
      | Arijit Singh   | 3     |
      | NFThunk        | 1     |
      | Shreya Ghoshal | 1     |
    And the "artist" folder "Shreya Ghoshal" contains "Tum Kya Mile"

  Scenario: Movie folders come from the song titles
    Then the "movies" folders are:
      | folder     | songs |
      | Brahmastra | 2     |
      | Jawan      | 1     |
    And the "movie" folder "Brahmastra" contains "Deva Deva (From \"Brahmastra\"), Kesariya (From \"Brahmastra\")"

  Scenario: Like a whole folder, then unlike it
    When listener "riya" likes every song in the "movie" folder "Brahmastra"
    Then the liked songs of "riya" contain 2 songs
    When listener "riya" unlikes every song in the "movie" folder "Brahmastra"
    Then the liked songs of "riya" are empty
