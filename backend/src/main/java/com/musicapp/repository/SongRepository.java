package com.musicapp.repository;

import com.musicapp.domain.DownloadStatus;
import com.musicapp.domain.Song;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SongRepository extends JpaRepository<Song, Long> {

    Optional<Song> findBySourceAndExternalId(String source, String externalId);

    /** Catalogue listing with optional text search and optional download-status filter. */
    @Query("""
            select s from Song s
            where (:status is null or s.downloadStatus = :status)
              and (:year is null or year(s.releaseDate) = :year)
              and (:artist is null or s.artist = :artist)
              and (:movie is null or s.movie = :movie)
              and (:q is null
                   or lower(s.title) like lower(concat('%', :q, '%'))
                   or lower(s.artist) like lower(concat('%', :q, '%'))
                   or lower(s.album) like lower(concat('%', :q, '%')))
            """)
    Page<Song> findFiltered(@Param("q") String query, @Param("status") DownloadStatus status,
                            @Param("year") Integer year, @Param("artist") String artist, @Param("movie") String movie,
                            Pageable pageable);

    /** Folders: [year, song count, cover image], newest year first. */
    @Query("""
            select year(s.releaseDate), count(s), max(s.imageUrl) from Song s
            where s.releaseDate is not null
            group by year(s.releaseDate)
            order by year(s.releaseDate) desc
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
