Feature: Download songs into our own database
  Downloaded songs are stored in MySQL and played from our API, so replaying
  them never calls the external music API and seeking does not buffer.

  Background:
    Given the free music API offers these "bollywood" songs:
      | id | title     | artist       | released   |
      | a1 | Tum Hi Ho | Arijit Singh | 2013-04-01 |
      | a2 | Kesariya  | Arijit Singh | 2022-07-17 |
    And the music API serves 2500000 bytes of audio for song "a2"
    When the song ingestion job runs
    Then the catalogue eventually contains 2 songs

  Scenario: A downloaded song is played from our database
    When listener "riya" downloads "Kesariya"
    Then "Kesariya" is eventually downloaded
    And the audio of "Kesariya" played from our API matches the original
    And seeking in "Kesariya" returns only the requested bytes
    And "Kesariya" appears in the downloaded songs
    When the audio of "Kesariya" is played 3 more times
    Then the music API served the audio of song "a2" only once

  Scenario: Download all liked songs, then remove one
    When listener "riya" likes "Kesariya"
    And listener "riya" downloads all liked songs
    Then "Kesariya" is eventually downloaded
    When listener "riya" removes the download of "Kesariya"
    Then "Kesariya" is not downloaded
    And the audio of "Kesariya" is no longer served

  Scenario: A failing source marks the download as failed
    When listener "riya" downloads "Tum Hi Ho"
    Then "Tum Hi Ho" eventually has download status "FAILED"
