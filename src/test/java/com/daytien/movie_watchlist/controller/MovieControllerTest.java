package com.daytien.movie_watchlist.controller;

import com.daytien.movie_watchlist.dto.MovieResponse;
import com.daytien.movie_watchlist.security.SecurityConfig;
import com.daytien.movie_watchlist.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovieController.class)
@Import(SecurityConfig.class)
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @Test
    void getMovie_withTitleParam_returnsMovieJson() throws Exception {
        MovieResponse response = new MovieResponse();
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
    void getMovie_withoutTitleParam_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/movie"))
                .andExpect(status().isBadRequest());
    }
}
