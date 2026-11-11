package com.musicapp.repository;

import com.musicapp.domain.PlayHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayHistoryRepository extends JpaRepository<PlayHistory, Long> {

    @Query(value = "select h from PlayHistory h join fetch h.song where h.userId = :userId order by h.playedAt desc, h.id desc",
            countQuery = "select count(h) from PlayHistory h where h.userId = :userId")
    Page<PlayHistory> findByUser(@Param("userId") String userId, Pageable pageable);

    @Modifying
    @Query("delete from PlayHistory h where h.userId = :userId")
    int deleteByUser(@Param("userId") String userId);
}
