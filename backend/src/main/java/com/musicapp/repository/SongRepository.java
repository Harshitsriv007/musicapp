package com.musicapp.repository;

import com.musicapp.domain.DownloadStatus;
import com.musicapp.domain.Song;
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
              and (:q is null
                   or lower(s.title) like lower(concat('%', :q, '%'))
                   or lower(s.artist) like lower(concat('%', :q, '%'))
                   or lower(s.album) like lower(concat('%', :q, '%')))
            """)
    Page<Song> findFiltered(@Param("q") String query, @Param("status") DownloadStatus status, Pageable pageable);
}
