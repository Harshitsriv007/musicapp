package com.musicapp.repository;

import com.musicapp.domain.UserDownload;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserDownloadRepository extends JpaRepository<UserDownload, Long> {

    /** Which of {@code songIds} are in this listener's Downloads. */
    @Query("select d.song.id from UserDownload d where d.userId = :userId and d.song.id in :songIds")
    Set<Long> findDownloadedSongIds(@Param("userId") String userId, @Param("songIds") Collection<Long> songIds);

    @Modifying
    @Query("delete from UserDownload d where d.userId = :userId and d.song.id = :songId")
    int deleteByUserAndSong(@Param("userId") String userId, @Param("songId") Long songId);

    /** True while at least one listener still keeps the song in their Downloads. */
    boolean existsBySongId(Long songId);
}
