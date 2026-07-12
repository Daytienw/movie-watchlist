package com.daytien.movie_watchlist.dto;


import java.time.LocalDateTime;

import com.daytien.movie_watchlist.entity.WatchlistStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WatchlistResponseDto {

    private Long id;
    private String imdbId;
    private String title;
    private String year;
    private String poster;
    private WatchlistStatus status;
    private LocalDateTime addedDate;

}
