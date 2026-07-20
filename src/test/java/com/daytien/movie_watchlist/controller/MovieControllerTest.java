package com.daytien.movie_watchlist.controller;

import java.util.List;

import com.daytien.movie_watchlist.dto.MovieResponseDto;
import com.daytien.movie_watchlist.dto.MovieSearchResultDto;
import com.daytien.movie_watchlist.exception.GlobalExceptionHandler;
import com.daytien.movie_watchlist.security.JwtAuthFilter;
import com.daytien.movie_watchlist.security.SecurityConfig;
import com.daytien.movie_watchlist.service.MovieService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovieController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private JwtAuthFilter jwtAuthFilter;

    @MockitoBean
    private AuthenticationProvider authenticationProvider;

    @BeforeEach
    void setUpJwtAuthFilter() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtAuthFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser
    void getMovie_withTitleParam_returnsMovieJson() throws Exception {
        MovieResponseDto response = new MovieResponseDto();
        response.setTitle("Inception");
        response.setYear("2010");
        response.setPlot("A thief who steals corporate secrets...");
        when(movieService.fetchMovieFromOmdb("Inception")).thenReturn(response);

        mockMvc.perform(get("/api/movie").param("title", "Inception"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Title").value("Inception"))
                .andExpect(jsonPath("$.Year").value("2010"))
                .andExpect(jsonPath("$.Plot").value("A thief who steals corporate secrets..."));

        verify(movieService).fetchMovieFromOmdb("Inception");
    }

    @Test
    @WithMockUser
    void getMovie_withoutTitleParam_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/movie"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void searchMovies_returnsListOfMatches() throws Exception {
        MovieSearchResultDto result = new MovieSearchResultDto();
        result.setTitle("Inception");
        result.setYear("2010");
        result.setImdbId("tt1375666");
        result.setPoster("https://example.com/poster.jpg");
        when(movieService.searchMovies("inception")).thenReturn(List.of(result));

        mockMvc.perform(get("/api/movies/search").param("q", "inception"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].Title").value("Inception"))
                .andExpect(jsonPath("$[0].imdbID").value("tt1375666"))
                .andExpect(jsonPath("$[0].Poster").value("https://example.com/poster.jpg"));

        verify(movieService).searchMovies("inception");
    }

    @Test
    @WithMockUser
    void searchMovies_withNoMatches_returnsEmptyArray() throws Exception {
        when(movieService.searchMovies("zzzz")).thenReturn(List.of());

        mockMvc.perform(get("/api/movies/search").param("q", "zzzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser
    void searchMovies_withBlankQuery_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/movies/search").param("q", "  "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchMovies_withoutAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/movies/search").param("q", "inception"))
                .andExpect(status().isForbidden());
    }
}
