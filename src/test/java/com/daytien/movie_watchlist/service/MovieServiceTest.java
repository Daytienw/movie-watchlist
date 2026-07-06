package com.daytien.movie_watchlist.service;

import com.daytien.movie_watchlist.dto.MovieResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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
        MovieResponse expected = new MovieResponse();
        expected.setTitle("Inception");
        expected.setYear("2010");
        expected.setPlot("A thief who steals corporate secrets...");
        when(restTemplate.getForObject(anyString(), eq(MovieResponse.class))).thenReturn(expected);

        MovieResponse actual = movieService.fetchMovieFromOmdb("Inception");

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void fetchMovieFromOmdb_buildsUrlWithBaseUrlApiKeyAndTitle() {
        when(restTemplate.getForObject(anyString(), eq(MovieResponse.class))).thenReturn(new MovieResponse());

        movieService.fetchMovieFromOmdb("The Matrix");

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).getForObject(urlCaptor.capture(), eq(MovieResponse.class));
        String url = urlCaptor.getValue();

        assertThat(url).startsWith("https://www.omdbapi.com");
        assertThat(url).contains("apikey=test-api-key");
        assertThat(url).contains("t=The%20Matrix");
    }

    @Test
    void fetchMovieFromOmdb_returnsNullWhenRestTemplateReturnsNull() {
        when(restTemplate.getForObject(anyString(), eq(MovieResponse.class))).thenReturn(null);

        MovieResponse actual = movieService.fetchMovieFromOmdb("Unknown Movie Title 12345");

        assertThat(actual).isNull();
    }
}
