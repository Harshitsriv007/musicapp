package com.musicapp.repository;

import com.musicapp.domain.Song;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SongRepository extends JpaRepository<Song, Long> {

    Optional<Song> findBySourceAndExternalId(String source, String externalId);

    /** Catalogue listing with optional text search; {@code downloadedBy} limits it to that listener's downloads. */
    @Query("""
            select s from Song s
            where (:downloadedBy is null
                   or (s.downloadStatus = com.musicapp.domain.DownloadStatus.DOWNLOADED
                       and exists (select 1 from UserDownload d where d.userId = :downloadedBy and d.song.id = s.id)))
              and (:releasedFrom is null or coalesce(s.originalReleaseDate, s.releaseDate) >= :releasedFrom)
              and (:releasedTo is null or coalesce(s.originalReleaseDate, s.releaseDate) <= :releasedTo)
              and (:artist is null or s.artist = :artist)
              and (:movie is null or s.movie = :movie)
              and (:q is null
                   or lower(s.title) like lower(concat('%', :q, '%'))
                   or lower(s.artist) like lower(concat('%', :q, '%'))
                   or lower(s.album) like lower(concat('%', :q, '%'))
                   or lower(s.movie) like lower(concat('%', :q, '%')))
            """)
    Page<Song> findFiltered(@Param("q") String query, @Param("downloadedBy") String downloadedBy,
                            @Param("releasedFrom") LocalDate releasedFrom, @Param("releasedTo") LocalDate releasedTo,
                            @Param("artist") String artist, @Param("movie") String movie,
                            Pageable pageable);

    /**
     * Search by song name, artist, movie or album. {@code pattern} is "%text%" and {@code prefix} "text%",
     * both lower-case with LIKE wildcards escaped by '!'. Best matches first: title starts with the
     * text, title contains it, artist matches, then movie/album.
     */
    @Query("""
            select s from Song s
            where lower(s.title) like :pattern escape '!'
               or lower(s.artist) like :pattern escape '!'
               or lower(s.movie) like :pattern escape '!'
               or lower(s.album) like :pattern escape '!'
            order by
              case
                when lower(s.title) like :prefix escape '!' then 0
                when lower(s.title) like :pattern escape '!' then 1
                when lower(s.artist) like :pattern escape '!' then 2
                else 3
              end,
              s.releaseDate desc, s.id desc
            """)
    List<Song> search(@Param("pattern") String pattern, @Param("prefix") String prefix, Pageable pageable);

    /** Artist folders whose name matches, biggest first. */
    @Query("""
            select s.artist, count(s), max(s.imageUrl) from Song s
            where lower(s.artist) like :pattern escape '!'
            group by s.artist
            order by count(s) desc, s.artist
            """)
    List<Object[]> searchArtistFolders(@Param("pattern") String pattern, Pageable pageable);

    /** Movie folders whose name matches, biggest first. */
    @Query("""
            select s.movie, count(s), max(s.imageUrl) from Song s
            where lower(s.movie) like :pattern escape '!'
            group by s.movie
            order by count(s) desc, s.movie
            """)
    List<Object[]> searchMovieFolders(@Param("pattern") String pattern, Pageable pageable);

    /** Songs released (originally, else uploaded) on or after {@code from} – the "Latest" folder. */
    @Query("select count(s) from Song s where coalesce(s.originalReleaseDate, s.releaseDate) >= :from")
    long countReleasedSince(@Param("from") LocalDate from);

    @Query("""
            select s.imageUrl from Song s
            where coalesce(s.originalReleaseDate, s.releaseDate) >= :from and s.imageUrl is not null
            order by coalesce(s.originalReleaseDate, s.releaseDate) desc
            """)
    List<String> imagesReleasedSince(@Param("from") LocalDate from, Pageable pageable);

    List<Song> findByMetadataCheckedAtIsNullOrderByIdAsc(Pageable pageable);

    /** [year, song count, cover image], newest year first – rolled up into decade folders. Uses the
     *  original release year when MusicBrainz knows it, otherwise the upload year. */
    @Query("""
            select year(coalesce(s.originalReleaseDate, s.releaseDate)), count(s), max(s.imageUrl) from Song s
            where coalesce(s.originalReleaseDate, s.releaseDate) is not null
            group by year(coalesce(s.originalReleaseDate, s.releaseDate))
            order by year(coalesce(s.originalReleaseDate, s.releaseDate)) desc
            """)
    List<Object[]> yearFolders(Pageable pageable);

    /** Folders: [artist, song count, cover image], biggest first. */
    @Query("""
            select s.artist, count(s), max(s.imageUrl) from Song s
            where s.artist is not null and s.artist <> ''
            group by s.artist
            order by count(s) desc, s.artist
            """)
    List<Object[]> artistFolders(Pageable pageable);

    /** Folders: [movie, song count, cover image], biggest first. */
    @Query("""
            select s.movie, count(s), max(s.imageUrl) from Song s
            where s.movie is not null and s.movie <> ''
            group by s.movie
            order by count(s) desc, s.movie
            """)
    List<Object[]> movieFolders(Pageable pageable);
}
