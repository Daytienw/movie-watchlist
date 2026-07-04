package com.daytien.movie_watchlist.controller;

import com.daytien.movie_watchlist.dto.MovieResponse;
import com.daytien.movie_watchlist.service.MovieService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    // Endpoint: http://localhost:8080/api/movie?title=Inception
    @GetMapping("/api/movie")
    public MovieResponse getMovie(@RequestParam String title) {
        return movieService.fetchMovieFromOmdb(title);
    }
}
