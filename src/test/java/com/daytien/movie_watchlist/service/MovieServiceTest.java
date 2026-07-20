package com.daytien.movie_watchlist.service;

import com.daytien.movie_watchlist.dto.MovieResponseDto;
import com.daytien.movie_watchlist.dto.MovieSearchResultDto;
import com.daytien.movie_watchlist.dto.OmdbSearchResponseDto;
import com.daytien.movie_watchlist.exception.OmdbException;
import com.daytien.movie_watchlist.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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

    @Test
    void fetchMovieFromOmdb_throwsResourceNotFoundExceptionWhenOmdbReportsMovieNotFound() {
        MovieResponseDto omdbError = new MovieResponseDto();
        omdbError.setResponse("False");
        omdbError.setError("Movie not found!");
        when(restTemplate.getForObject(any(URI.class), eq(MovieResponseDto.class))).thenReturn(omdbError);

        assertThatThrownBy(() -> movieService.fetchMovieFromOmdb("Nonexistent Movie"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Movie not found!");
    }

    @Test
    void searchMovies_returnsMatches() {
        MovieSearchResultDto first = new MovieSearchResultDto();
        first.setTitle("Inception");
        first.setYear("2010");
        first.setImdbId("tt1375666");
        OmdbSearchResponseDto omdbResponse = new OmdbSearchResponseDto();
        omdbResponse.setResponse("True");
        omdbResponse.setSearch(List.of(first));
        when(restTemplate.getForObject(any(URI.class), eq(OmdbSearchResponseDto.class))).thenReturn(omdbResponse);

        List<MovieSearchResultDto> results = movieService.searchMovies("inception");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getImdbId()).isEqualTo("tt1375666");
    }

    /** Zero matches is an empty list, not a 404 — the user is still typing. */
    @Test
    void searchMovies_whenOmdbReportsNotFound_returnsEmptyList() {
        OmdbSearchResponseDto omdbResponse = new OmdbSearchResponseDto();
        omdbResponse.setResponse("False");
        omdbResponse.setError("Movie not found!");
        when(restTemplate.getForObject(any(URI.class), eq(OmdbSearchResponseDto.class))).thenReturn(omdbResponse);

        assertThat(movieService.searchMovies("zzzznotarealmovie")).isEmpty();
    }

    @Test
    void searchMovies_propagatesRealOmdbFailures() {
        OmdbSearchResponseDto omdbResponse = new OmdbSearchResponseDto();
        omdbResponse.setResponse("False");
        omdbResponse.setError("Invalid API key!");
        when(restTemplate.getForObject(any(URI.class), eq(OmdbSearchResponseDto.class))).thenReturn(omdbResponse);

        assertThatThrownBy(() -> movieService.searchMovies("inception"))
                .isInstanceOf(OmdbException.class)
                .hasMessage("Invalid API key!");
    }

    @Test
    void searchMovies_requestsOnlyMoviesFromOmdb() {
        OmdbSearchResponseDto omdbResponse = new OmdbSearchResponseDto();
        omdbResponse.setResponse("True");
        omdbResponse.setSearch(List.of());
        when(restTemplate.getForObject(any(URI.class), eq(OmdbSearchResponseDto.class))).thenReturn(omdbResponse);

        movieService.searchMovies("inception");

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForObject(uriCaptor.capture(), eq(OmdbSearchResponseDto.class));
        assertThat(uriCaptor.getValue().toString())
                .contains("s=inception")
                .contains("type=movie");
    }

    private MovieSearchResultDto film(String imdbId, String title, String poster) {
        MovieSearchResultDto movie = new MovieSearchResultDto();
        movie.setImdbId(imdbId);
        movie.setTitle(title);
        movie.setYear("1994");
        movie.setPoster(poster);
        return movie;
    }

    /**
     * Answers each OMDb search with films unique to the seed word in the URL,
     * so the tests can tell which terms were actually queried.
     */
    private void stubDiscoveryPerTerm() {
        when(restTemplate.getForObject(any(URI.class), eq(OmdbSearchResponseDto.class)))
                .thenAnswer(invocation -> {
                    URI uri = invocation.getArgument(0);
                    String term = uri.getQuery().replaceAll(".*s=([^&]*).*", "$1");
                    OmdbSearchResponseDto response = new OmdbSearchResponseDto();
                    response.setResponse("True");
                    List<MovieSearchResultDto> films = new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                        films.add(film("tt-" + term + "-" + i, term + " film " + i,
                                "https://example.com/" + term + i + ".jpg"));
                    }
                    response.setSearch(films);
                    return response;
                });
    }

    @Test
    void getSuggestions_returnsAScreenfulOfFilms() {
        stubDiscoveryPerTerm();

        List<MovieSearchResultDto> suggestions = movieService.getSuggestions();

        assertThat(suggestions).hasSize(12);
        assertThat(suggestions).allSatisfy(movie -> {
            assertThat(movie.getTitle()).isNotBlank();
            assertThat(movie.getPoster()).startsWith("https://");
        });
    }

    @Test
    void getSuggestions_variesBetweenCalls() {
        stubDiscoveryPerTerm();

        Set<String> shapes = new HashSet<>();
        for (int i = 0; i < 12; i++) {
            shapes.add(movieService.getSuggestions().stream()
                    .map(MovieSearchResultDto::getImdbId)
                    .sorted()
                    .collect(Collectors.joining(",")));
        }

        // Same seed words in a different order would still be a repeat, so this
        // asserts the draw genuinely moves rather than just reshuffling one set.
        assertThat(shapes).hasSizeGreaterThan(1);
    }

    /**
     * The free OMDb tier allows 1,000 requests a day. Caching per seed word means
     * refreshing the page forever can never cost more than one request per term.
     */
    @Test
    void getSuggestions_neverExceedsOneOmdbCallPerSeedTerm() {
        stubDiscoveryPerTerm();

        for (int i = 0; i < 40; i++) {
            movieService.getSuggestions();
        }

        ArgumentCaptor<URI> uris = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate, atLeastOnce()).getForObject(uris.capture(), eq(OmdbSearchResponseDto.class));

        Set<String> distinctTerms = uris.getAllValues().stream()
                .map(uri -> uri.getQuery().replaceAll(".*s=([^&]*).*", "$1"))
                .collect(Collectors.toSet());

        // One call per distinct term and no more, despite 40 page loads.
        assertThat(uris.getAllValues()).hasSize(distinctTerms.size());
    }

    /**
     * The point of the feature: no two films on screen may come from the same
     * seed word, or the grid fills with visibly related titles (Black Widow,
     * Black Panther, Men in Black).
     */
    @Test
    void getSuggestions_returnsNoTwoFilmsFromTheSameSeedTerm() {
        stubDiscoveryPerTerm();

        for (int attempt = 0; attempt < 10; attempt++) {
            List<String> sourceTerms = movieService.getSuggestions().stream()
                    // Stubbed ids are "tt-<term>-<n>", so the term is recoverable.
                    .map(movie -> movie.getImdbId().split("-")[1])
                    .toList();

            assertThat(sourceTerms).doesNotHaveDuplicates();
        }
    }

    /** A card with no artwork looks broken, so those films are dropped. */
    @Test
    void getSuggestions_omitsFilmsWithoutAPoster() {
        OmdbSearchResponseDto response = new OmdbSearchResponseDto();
        response.setResponse("True");
        response.setSearch(List.of(
                film("tt1", "Has poster", "https://example.com/a.jpg"),
                film("tt2", "No poster", "N/A"),
                film("tt3", "Null poster", null)));
        when(restTemplate.getForObject(any(URI.class), eq(OmdbSearchResponseDto.class)))
                .thenReturn(response);

        List<MovieSearchResultDto> suggestions = movieService.getSuggestions();

        assertThat(suggestions).extracting(MovieSearchResultDto::getTitle)
                .containsExactly("Has poster");
    }

    /** Two seed words can match the same film; it should appear once. */
    @Test
    void getSuggestions_dedupesFilmsMatchedByBothTerms() {
        OmdbSearchResponseDto response = new OmdbSearchResponseDto();
        response.setResponse("True");
        response.setSearch(List.of(film("tt1", "Shared", "https://example.com/a.jpg")));
        when(restTemplate.getForObject(any(URI.class), eq(OmdbSearchResponseDto.class)))
                .thenReturn(response);

        assertThat(movieService.getSuggestions()).hasSize(1);
    }

    @Test
    void fetchMovieFromOmdb_throwsOmdbExceptionForOtherOmdbFailures() {
        MovieResponseDto omdbError = new MovieResponseDto();
        omdbError.setResponse("False");
        omdbError.setError("Invalid API key!");
        when(restTemplate.getForObject(any(URI.class), eq(MovieResponseDto.class))).thenReturn(omdbError);

        assertThatThrownBy(() -> movieService.fetchMovieFromOmdb("Inception"))
                .isInstanceOf(OmdbException.class)
                .hasMessage("Invalid API key!");
    }
}
