package com.daytien.movie_watchlist.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WatchlistRequest {

    private String imdbId;
    private String title;
    private String year;
    private String poster;
}
