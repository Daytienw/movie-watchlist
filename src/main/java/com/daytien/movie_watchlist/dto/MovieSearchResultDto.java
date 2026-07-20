package com.daytien.movie_watchlist.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * One row of an OMDb search result. The @JsonProperty bindings are required:
 * OMDb capitalises its keys, and Jackson matches names case-sensitively.
 */
@Getter
@Setter
public class MovieSearchResultDto {

    @JsonProperty("Title")
    private String title;

    @JsonProperty("Year")
    private String year;

    @JsonProperty("imdbID")
    private String imdbId;

    @JsonProperty("Poster")
    private String poster;

    @JsonProperty("Type")
    private String type;
}
