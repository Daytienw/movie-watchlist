package com.daytien.movie_watchlist.controller;

import com.daytien.movie_watchlist.dto.WatchlistRequestDto;
import com.daytien.movie_watchlist.dto.WatchlistResponseDto;
import com.daytien.movie_watchlist.entity.WatchlistStatus;
import com.daytien.movie_watchlist.exception.DuplicateResourceException;
import com.daytien.movie_watchlist.exception.GlobalExceptionHandler;
import com.daytien.movie_watchlist.exception.ResourceNotFoundException;
import com.daytien.movie_watchlist.security.JwtAuthFilter;
import com.daytien.movie_watchlist.security.SecurityConfig;
import com.daytien.movie_watchlist.service.WatchlistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WatchlistController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class WatchlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WatchlistService watchlistService;

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

    private WatchlistResponseDto sampleResponse() {
        WatchlistResponseDto dto = new WatchlistResponseDto();
        dto.setId(1L);
        dto.setImdbId("tt1375666");
        dto.setTitle("Inception");
        dto.setYear("2010");
        dto.setPoster("poster-url");
        dto.setStatus(WatchlistStatus.TO_WATCH);
        dto.setAddedDate(LocalDateTime.of(2026, 7, 14, 0, 0));
        return dto;
    }

    private static final String CREATE_REQUEST_JSON = """
            {
              "imdbId": "tt1375666",
              "title": "Inception",
              "year": "2010",
              "poster": "poster-url",
              "status": "TO_WATCH"
            }
            """;

    @Test
    @WithMockUser
    void createWatchlistItem_returnsCreated() throws Exception {
        when(watchlistService.create(any(WatchlistRequestDto.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/watchlist/")
                        .contentType("application/json")
                        .content(CREATE_REQUEST_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Inception"));
    }

    @Test
    @WithMockUser
    void getAllWatchlistItems_returnsPageOfItems() throws Exception {
        Page<WatchlistResponseDto> page = new PageImpl<>(List.of(sampleResponse()));
        when(watchlistService.getAll(eq(null), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/watchlist/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Inception"));
    }

    @Test
    @WithMockUser
    void getWatchlistById_returnsItem() throws Exception {
        when(watchlistService.getById(1L)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/watchlist/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Inception"));
    }

    @Test
    @WithMockUser
    void getWatchlistById_returnsNotFoundWhenItemMissing() throws Exception {
        when(watchlistService.getById(99L)).thenThrow(new ResourceNotFoundException("Watchlist item not found with id: 99"));

        mockMvc.perform(get("/api/watchlist/99"))
                .andExpect(status().isNotFound());
    }

    private static final String UPDATE_REQUEST_JSON = """
            {
              "imdbId": "tt1375666",
              "title": "Inception (Updated)",
              "year": "2010",
              "poster": "poster-url",
              "status": "WATCHED"
            }
            """;

    @Test
    @WithMockUser
    void putMethodName_updatesAndReturnsItem() throws Exception {
        WatchlistResponseDto updatedResponse = sampleResponse();
        updatedResponse.setTitle("Inception (Updated)");
        updatedResponse.setStatus(WatchlistStatus.WATCHED);

        when(watchlistService.updateItem(eq(1L), any(WatchlistRequestDto.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/watchlist/1")
                        .contentType("application/json")
                        .content(UPDATE_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Inception (Updated)"))
                .andExpect(jsonPath("$.status").value("WATCHED"));
    }

    @Test
    @WithMockUser
    void deleteItem_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/watchlist/1"))
                .andExpect(status().isNoContent());

        verify(watchlistService).deleteItem(1L);
    }

    @Test
    @WithMockUser
    void deleteItem_returnsNotFoundWhenItemMissing() throws Exception {
        doAnswer(invocation -> {
            throw new ResourceNotFoundException("Watchlist item not found with id: 99");
        }).when(watchlistService).deleteItem(anyLong());

        mockMvc.perform(delete("/api/watchlist/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void createWatchlistItem_withEmptyBody_returnsBadRequestNotServerError() throws Exception {
        mockMvc.perform(post("/api/watchlist/")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.imdbId").exists())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.status").exists());

        verifyNoInteractions(watchlistService);
    }

    @Test
    @WithMockUser
    void createWatchlistItem_withUnknownStatusValue_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/watchlist/")
                        .contentType("application/json")
                        .content("""
                                {
                                  "imdbId": "tt1375666",
                                  "title": "Inception",
                                  "year": "2010",
                                  "poster": "poster-url",
                                  "status": "NOT_A_STATUS"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(watchlistService);
    }

    /**
     * PUT is full-replacement, so an incomplete body has to be rejected up
     * front rather than nulling out NOT NULL columns and surfacing as a 500.
     */
    @Test
    @WithMockUser
    void updateWatchlistItem_withPartialBody_returnsBadRequestNotServerError() throws Exception {
        mockMvc.perform(put("/api/watchlist/1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "status": "WATCHED"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(watchlistService);
    }

    @Test
    @WithMockUser
    void createWatchlistItem_whenAlreadyOnWatchlist_returnsConflict() throws Exception {
        when(watchlistService.create(any(WatchlistRequestDto.class)))
                .thenThrow(new DuplicateResourceException("That movie is already on your watchlist"));

        mockMvc.perform(post("/api/watchlist/")
                        .contentType("application/json")
                        .content(CREATE_REQUEST_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void createWatchlistItem_withoutAuthentication_returnsForbidden() throws Exception {
        mockMvc.perform(post("/api/watchlist/")
                        .contentType("application/json")
                        .content(CREATE_REQUEST_JSON))
                .andExpect(status().isForbidden());
    }
}
