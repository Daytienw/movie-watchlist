package com.daytien.movie_watchlist.service;

import com.daytien.movie_watchlist.dto.MovieResponseDto;
import com.daytien.movie_watchlist.dto.MovieSearchResultDto;
import com.daytien.movie_watchlist.dto.OmdbSearchResponseDto;
import com.daytien.movie_watchlist.exception.OmdbException;
import com.daytien.movie_watchlist.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class MovieService {

    private static final Logger log = LoggerFactory.getLogger(MovieService.class);

    /**
     * Seed words for the "add movies" screen. OMDb has no random, popular or
     * discover endpoint, so variety is built out of the one primitive it does
     * offer: title search. These are common enough to appear in a lot of film
     * titles, and OMDb orders matches by popularity, so they surface
     * recognisable films rather than obscure ones.
     */
    private static final List<String> DISCOVERY_TERMS = List.of(
            "love", "night", "star", "king", "dark", "blood", "city", "last",
            "man", "girl", "war", "life", "death", "world", "day", "fire",
            "dream", "home", "road", "time", "black", "lost", "great", "hard",
            "red", "gold", "wild", "cold", "moon", "sun", "river", "train",
            "summer", "winter", "storm", "ghost", "queen", "child", "father",
            "brother", "house", "island", "sea", "iron", "glass", "green",
            "silent", "run", "fall", "rise", "young"
    );

    /**
     * How many seed words go into one screenful. Deliberately larger than the
     * number of films returned: exactly one film is taken per word, so no two
     * cards on screen share a search term. Filling the grid from one or two
     * words instead would surface obviously related titles — Black Widow, Men
     * in Black, Black Panther — which reads as an algorithm, not a shelf.
     */
    private static final int TERMS_PER_REQUEST = 18;

    private static final int SUGGESTION_LIMIT = 12;

    private final RestTemplate restTemplate;

    /**
     * Results per seed word. A term's films don't change, so this is cached
     * indefinitely: the randomness comes from which terms get picked, not from
     * re-querying. Total OMDb cost is therefore bounded by DISCOVERY_TERMS
     * across the whole process, however many times the page is loaded.
     */
    private final Map<String, List<MovieSearchResultDto>> discoveryCache = new ConcurrentHashMap<>();

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

    /**
     * Title search against OMDb's s= endpoint, which returns a list of matches
     * rather than the single exact-title hit that fetchMovieFromOmdb gives.
     * Restricted to type=movie so series and games stay out of a film watchlist.
     */
    public List<MovieSearchResultDto> searchMovies(String query) {
        URI url = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("apikey", apiKey)
                .queryParam("s", query)
                .queryParam("type", "movie")
                .build()
                .encode()
                .toUri();

        OmdbSearchResponseDto result = restTemplate.getForObject(url, OmdbSearchResponseDto.class);

        if (result == null) {
            return List.of();
        }

        if ("False".equalsIgnoreCase(result.getResponse())) {
            String error = result.getError() != null ? result.getError() : "OMDb request failed";
            // "Movie not found!" just means zero matches, which is an empty
            // result rather than an error worth propagating to the client.
            if (error.toLowerCase().contains("not found")) {
                return List.of();
            }
            throw new OmdbException(error);
        }

        return result.getSearch() != null ? result.getSearch() : List.of();
    }

    /**
     * A different, unrelated screenful of films each time the "add movies" page
     * is opened.
     *
     * OMDb offers no random or popular endpoint, so variety is built from title
     * search. The important detail is that each seed word contributes at most
     * one film: a search for "black" returns Black Widow, Black Panther, Black
     * Adam and so on, so drawing a screenful from a few words would put visibly
     * related titles side by side. Drawing many words and taking one film from
     * each breaks that link.
     *
     * Each word's results are cached, so the whole feature stays within
     * DISCOVERY_TERMS requests for the life of the process however often the
     * page is refreshed — the variety comes from the draw, not from re-querying.
     */
    public List<MovieSearchResultDto> getSuggestions() {
        List<String> terms = new ArrayList<>(DISCOVERY_TERMS);
        Collections.shuffle(terms);
        List<String> chosen = terms.subList(0, Math.min(TERMS_PER_REQUEST, terms.size()));

        // Pure I/O wait, so the terms resolve concurrently rather than serially.
        List<MovieSearchResultDto> picks = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<MovieSearchResultDto>> pending = chosen.stream()
                    .map(term -> executor.submit(() -> pickOne(term)))
                    .toList();

            for (Future<MovieSearchResultDto> future : pending) {
                try {
                    MovieSearchResultDto movie = future.get();
                    if (movie != null) {
                        picks.add(movie);
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ExecutionException failure) {
                    // One failed term costs one card, not the whole screen.
                    log.warn("Could not resolve a discovery term", failure.getCause());
                }
            }
        }

        // Different words occasionally land on the same film.
        Map<String, MovieSearchResultDto> unique = new LinkedHashMap<>();
        for (MovieSearchResultDto movie : picks) {
            unique.putIfAbsent(movie.getImdbId(), movie);
        }

        List<MovieSearchResultDto> suggestions = new ArrayList<>(unique.values());
        Collections.shuffle(suggestions);
        return suggestions.size() > SUGGESTION_LIMIT
                ? List.copyOf(suggestions.subList(0, SUGGESTION_LIMIT))
                : List.copyOf(suggestions);
    }

    /**
     * One film for a seed word, chosen at random from its matches so the same
     * word doesn't always yield the same title. Films without artwork are
     * skipped — a poster-less card looks broken.
     */
    private MovieSearchResultDto pickOne(String term) {
        List<MovieSearchResultDto> candidates = discover(term).stream()
                .filter(movie -> movie.getImdbId() != null)
                .filter(movie -> movie.getPoster() != null && !"N/A".equalsIgnoreCase(movie.getPoster()))
                .toList();

        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    /** Matches for one seed word, resolved at most once per process. */
    private List<MovieSearchResultDto> discover(String term) {
        List<MovieSearchResultDto> cached = discoveryCache.get(term);
        if (cached != null) {
            return cached;
        }
        // A duplicate fetch under a race is harmless, and avoiding
        // computeIfAbsent keeps network I/O out of the map's locking.
        List<MovieSearchResultDto> found = searchMovies(term);
        discoveryCache.put(term, found);
        return found;
    }
}
