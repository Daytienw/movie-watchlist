package com.daytien.movie_watchlist.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * Raw envelope OMDb wraps search results in. Internal to MovieService — the
 * controller returns the unwrapped list of results instead.
 */
@Getter
@Setter
public class OmdbSearchResponseDto {

    @JsonProperty("Search")
    private List<MovieSearchResultDto> search;

    @JsonProperty("totalResults")
    private String totalResults;

    @JsonProperty("Response")
    private String response;

    @JsonProperty("Error")
    private String error;
}
