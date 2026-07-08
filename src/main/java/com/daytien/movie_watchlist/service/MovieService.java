package com.daytien.movie_watchlist.service;

import com.daytien.movie_watchlist.dto.MovieResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class MovieService {

    private final RestTemplate restTemplate;

    @Value("${omdb.api.base-url}")
    private String baseUrl;

    @Value("${omdb.api.key}")
    private String apiKey;

    // Inject RestTemplate via constructor
    public MovieService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public MovieResponseDto fetchMovieFromOmdb(String movieTitle) {
        // Construct URL: https://omdbapi.com
        String url = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("apikey", apiKey)
                .queryParam("t", movieTitle)
                .toUriString();

        // Make the GET request and automatically map JSON to MovieResponse class
        return restTemplate.getForObject(url, MovieResponseDto.class);
    }
}
