package com.daytien.movie_watchlist.dto;


import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WatchlistResponse {
    
    private Long id;
    private String imdbId;
    private String title;
    private String year;
    private String poster;
    private String status;
    private LocalDateTime addedDate;
    
}
