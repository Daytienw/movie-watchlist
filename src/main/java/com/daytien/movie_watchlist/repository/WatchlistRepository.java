package com.daytien.movie_watchlist.repository;

import com.daytien.movie_watchlist.entity.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {

    
}
