Feature: Browse songs in folders
  The home screen groups songs into folders by decade, artist and movie,
  and a whole folder can be liked or downloaded at once.

  Background:
    Given the free music API offers these "bollywood" songs:
      | id | title                          | artist         | released   |
      | b1 | Kesariya (From "Brahmastra")   | Arijit Singh   | 2022-07-17 |
      | b2 | Deva Deva (From "Brahmastra")  | Arijit Singh   | 2022-08-08 |
      | b3 | Chaleya (From "Jawan")         | Arijit Singh   | 2023-08-14 |
      | b4 | Tum Kya Mile                   | Shreya Ghoshal | 2023-06-28 |
      | b5 | Tum Se Hi                      | Mohit Chauhan  | 2007-10-26 |
    When the song ingestion job runs
    Then the catalogue eventually contains 5 songs

  Scenario: Decade folders, latest first, every decade shown
    Then the "decades" folders are:
      | folder | songs |
      | Latest | 0     |
      | 20s    | 4     |
      | 10s    | 0     |
      | 00s    | 1     |
      | 90s    | 0     |
      | 80s    | 0     |
      | 70s    | 0     |
      | 60s    | 0     |
      | 50s    | 0     |
    And the "decade" folder "2000" contains "Tum Se Hi"
    And the "decade" folder "2020" contains "Kesariya (From \"Brahmastra\"), Deva Deva (From \"Brahmastra\"), Chaleya (From \"Jawan\"), Tum Kya Mile"

  Scenario: Artist folders, biggest first
    Then the "artists" folders are:
      | folder         | songs |
      | Arijit Singh   | 3     |
      | Mohit Chauhan  | 1     |
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

  Scenario: Original release years from MusicBrainz move songs into the right decade
    Given the free music API offers these "bollywood" songs:
      | id | title                    | artist         | released   |
      | c1 | Lag Ja Gale - Lata Mangeshkar | Old Is Gold | 2023-02-01 |
      | c2 | Lag Ja Gale Lofi Mashup       | Old Is Gold | 2023-02-01 |
    And MusicBrainz knows "Lag Ja Gale" by "Lata Mangeshkar" was first released in "1964"
    When the song ingestion job runs
    Then the catalogue eventually contains 7 songs
    When the release-year lookup runs
    Then the "decade" folder "1960" eventually contains "Lag Ja Gale - Lata Mangeshkar"
    And the "decade" folder "2020" contains "Kesariya (From \"Brahmastra\"), Deva Deva (From \"Brahmastra\"), Chaleya (From \"Jawan\"), Tum Kya Mile, Lag Ja Gale Lofi Mashup"
