package com.daytien.movie_watchlist.controller;

import java.util.List;

import com.daytien.movie_watchlist.dto.MovieResponseDto;
import com.daytien.movie_watchlist.dto.MovieSearchResultDto;
import com.daytien.movie_watchlist.service.MovieService;

import jakarta.validation.constraints.NotBlank;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    // Endpoint: http://localhost:8080/api/movie?title=Inception
    @GetMapping("/api/movie")
    public MovieResponseDto getMovie(@RequestParam("title") String title) {
        return movieService.fetchMovieFromOmdb(title);
    }

    // Endpoint: http://localhost:8080/api/movies/search?q=inception
    // Returns every match rather than the single exact-title hit above.
    @GetMapping("/api/movies/search")
    public List<MovieSearchResultDto> searchMovies(@RequestParam("q") @NotBlank String q) {
        return movieService.searchMovies(q);
    }

    // Endpoint: http://localhost:8080/api/movies/suggestions
    // A curated starting set, so the add-movies screen isn't empty on arrival.
    @GetMapping("/api/movies/suggestions")
    public List<MovieSearchResultDto> suggestions() {
        return movieService.getSuggestions();
    }
}
