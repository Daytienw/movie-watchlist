package com.daytien.movie_watchlist.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieSearchResultDto {

    private String title;
    private String year;
    private String imdbID;
    private String poster;

}
