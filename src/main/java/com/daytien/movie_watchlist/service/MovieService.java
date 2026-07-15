package com.daytien.movie_watchlist.service;

import com.daytien.movie_watchlist.dto.MovieResponseDto;
import com.daytien.movie_watchlist.exception.OmdbException;
import com.daytien.movie_watchlist.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

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
        // Build as a URI (not a String) so RestTemplate doesn't re-encode
        // the already-encoded query params a second time.
        URI url = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("apikey", apiKey)
                .queryParam("t", movieTitle)
                .build()
                .encode()
                .toUri();

        // Make the GET request and automatically map JSON to MovieResponse class
        MovieResponseDto movie = restTemplate.getForObject(url, MovieResponseDto.class);

        // OMDb signals failures with HTTP 200 and {"Response":"False","Error":"..."}
        // rather than a non-2xx status, so we have to check the payload ourselves.
        if (movie != null && "False".equalsIgnoreCase(movie.getResponse())) {
            String error = movie.getError() != null ? movie.getError() : "OMDb request failed";
            if (error.toLowerCase().contains("not found")) {
                throw new ResourceNotFoundException(error);
            }
            throw new OmdbException(error);
        }

        return movie;
    }
}
