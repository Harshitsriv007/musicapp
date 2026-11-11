package com.musicapp.repository;

import com.musicapp.domain.LikedSong;
import java.util.Collection;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikedSongRepository extends JpaRepository<LikedSong, Long> {

    boolean existsByUserIdAndSongId(String userId, Long songId);

    @Query(value = "select l from LikedSong l join fetch l.song where l.userId = :userId order by l.likedAt desc, l.id desc",
            countQuery = "select count(l) from LikedSong l where l.userId = :userId")
    Page<LikedSong> findByUser(@Param("userId") String userId, Pageable pageable);

    @Query("select l.song.id from LikedSong l where l.userId = :userId and l.song.id in :songIds")
    Set<Long> findLikedSongIds(@Param("userId") String userId, @Param("songIds") Collection<Long> songIds);

    @Modifying
    @Query("delete from LikedSong l where l.userId = :userId and l.song.id in :songIds")
    int deleteByUserAndSongs(@Param("userId") String userId, @Param("songIds") Collection<Long> songIds);

    @Modifying
    @Query("delete from LikedSong l where l.userId = :userId and l.song.id = :songId")
    int deleteByUserAndSong(@Param("userId") String userId, @Param("songId") Long songId);

    /**
     * Moves a device's likes to an account. Songs the account already liked are skipped
     * (UPDATE IGNORE leaves them on the device) and then dropped.
     */
    @Modifying
    @Query(value = "UPDATE IGNORE liked_songs SET user_id = :to WHERE user_id = :from", nativeQuery = true)
    int reassign(@Param("from") String from, @Param("to") String to);

    @Modifying
    @Query("delete from LikedSong l where l.userId = :userId")
    int deleteByUser(@Param("userId") String userId);
}
