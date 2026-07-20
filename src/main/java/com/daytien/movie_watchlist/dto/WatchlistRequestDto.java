package com.daytien.movie_watchlist.dto;

import com.daytien.movie_watchlist.entity.WatchlistStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WatchlistRequestDto {

    @NotBlank(message = "imdbId is required")
    @Pattern(regexp = "tt\\d{7,8}", message = "imdbId must look like tt1375666")
    private String imdbId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Year is required")
    private String year;

    @NotBlank(message = "Poster is required")
    private String poster;

    @NotNull(message = "Status is required and must be TO_WATCH or WATCHED")
    private WatchlistStatus status;
}
