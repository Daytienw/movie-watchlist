package com.daytien.movie_watchlist.service;

import com.daytien.movie_watchlist.dto.MovieResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private MovieService movieService;

    @BeforeEach
    void setUp() {
        movieService = new MovieService(restTemplate);
        ReflectionTestUtils.setField(movieService, "baseUrl", "https://www.omdbapi.com");
        ReflectionTestUtils.setField(movieService, "apiKey", "test-api-key");
    }

    @Test
    void fetchMovieFromOmdb_returnsResponseFromRestTemplate() {
        MovieResponseDto expected = new MovieResponseDto();
        expected.setTitle("Inception");
        expected.setYear("2010");
        expected.setPlot("A thief who steals corporate secrets...");
        when(restTemplate.getForObject(any(URI.class), eq(MovieResponseDto.class))).thenReturn(expected);

        MovieResponseDto actual = movieService.fetchMovieFromOmdb("Inception");

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void fetchMovieFromOmdb_buildsUrlWithBaseUrlApiKeyAndTitle() {
        when(restTemplate.getForObject(any(URI.class), eq(MovieResponseDto.class))).thenReturn(new MovieResponseDto());

        movieService.fetchMovieFromOmdb("The Matrix");

        ArgumentCaptor<URI> urlCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForObject(urlCaptor.capture(), eq(MovieResponseDto.class));
        String url = urlCaptor.getValue().toString();

        assertThat(url).startsWith("https://www.omdbapi.com");
        assertThat(url).contains("apikey=test-api-key");
        assertThat(url).contains("t=The%20Matrix");
    }

    @Test
    void fetchMovieFromOmdb_returnsNullWhenRestTemplateReturnsNull() {
        when(restTemplate.getForObject(any(URI.class), eq(MovieResponseDto.class))).thenReturn(null);

        MovieResponseDto actual = movieService.fetchMovieFromOmdb("Unknown Movie Title 12345");

        assertThat(actual).isNull();
    }
}
