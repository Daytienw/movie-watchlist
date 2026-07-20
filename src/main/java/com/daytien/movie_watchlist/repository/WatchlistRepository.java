package com.daytien.movie_watchlist.repository;

import com.daytien.movie_watchlist.entity.WatchlistItem;
import com.daytien.movie_watchlist.entity.WatchlistStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {

    Page<WatchlistItem> findByUserId(Long userId, Pageable pageable);

    Page<WatchlistItem> findByUserIdAndStatus(Long userId, WatchlistStatus status, Pageable pageable);

    boolean existsByUserIdAndImdbId(Long userId, String imdbId);

}
