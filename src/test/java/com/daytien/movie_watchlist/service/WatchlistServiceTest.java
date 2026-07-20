package com.daytien.movie_watchlist.service;

import com.daytien.movie_watchlist.dto.WatchlistRequestDto;
import com.daytien.movie_watchlist.dto.WatchlistResponseDto;
import com.daytien.movie_watchlist.entity.User;
import com.daytien.movie_watchlist.entity.WatchlistItem;
import com.daytien.movie_watchlist.entity.WatchlistStatus;
import com.daytien.movie_watchlist.exception.DuplicateResourceException;
import com.daytien.movie_watchlist.exception.ResourceNotFoundException;
import com.daytien.movie_watchlist.repository.WatchlistRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    private WatchlistService watchlistService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        watchlistService = new WatchlistService(watchlistRepository);

        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setEmail("owner@example.com");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private WatchlistRequestDto requestDto() {
        WatchlistRequestDto dto = new WatchlistRequestDto();
        dto.setImdbId("tt1375666");
        dto.setTitle("Inception");
        dto.setYear("2010");
        dto.setPoster("poster-url");
        dto.setStatus(WatchlistStatus.TO_WATCH);
        return dto;
    }

    private WatchlistItem existingItem(Long id, User owner) {
        WatchlistItem item = new WatchlistItem();
        item.setId(id);
        item.setImdbId("tt1375666");
        item.setTitle("Inception");
        item.setYear("2010");
        item.setPoster("poster-url");
        item.setStatus(WatchlistStatus.TO_WATCH);
        item.setUser(owner);
        return item;
    }

    @Test
    void create_savesItemForCurrentUserAndReturnsResponse() {
        WatchlistItem saved = existingItem(1L, currentUser);
        when(watchlistRepository.save(any(WatchlistItem.class))).thenReturn(saved);

        WatchlistResponseDto response = watchlistService.create(requestDto());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getImdbId()).isEqualTo("tt1375666");
        assertThat(response.getTitle()).isEqualTo("Inception");
        assertThat(response.getStatus()).isEqualTo(WatchlistStatus.TO_WATCH);
    }

    @Test
    void create_whenUserAlreadyHasThatMovie_throwsDuplicateResource() {
        when(watchlistRepository.existsByUserIdAndImdbId(1L, "tt1375666")).thenReturn(true);

        assertThatThrownBy(() -> watchlistService.create(requestDto()))
                .isInstanceOf(DuplicateResourceException.class);

        verify(watchlistRepository, never()).save(any());
    }

    /**
     * Regression: imdbId used to carry a global unique constraint, so whoever
     * added a film first locked every other user out of it.
     */
    @Test
    void create_allowsADifferentUserToAddTheSameMovie() {
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(otherUser, null, List.of()));

        // User 2 has never added this film, even though user 1 has.
        when(watchlistRepository.existsByUserIdAndImdbId(2L, "tt1375666")).thenReturn(false);
        when(watchlistRepository.save(any(WatchlistItem.class))).thenReturn(existingItem(2L, otherUser));

        WatchlistResponseDto response = watchlistService.create(requestDto());

        assertThat(response.getImdbId()).isEqualTo("tt1375666");
        verify(watchlistRepository).save(any(WatchlistItem.class));
    }

    @Test
    void getAll_withNoStatusFilter_delegatesToFindByUserId() {
        Pageable pageable = Pageable.ofSize(20);
        WatchlistItem item = existingItem(1L, currentUser);
        when(watchlistRepository.findByUserId(eq(1L), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(item)));

        Page<WatchlistResponseDto> result = watchlistService.getAll(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Inception");
        verify(watchlistRepository, never()).findByUserIdAndStatus(any(), any(), any());
    }

    @Test
    void getAll_withStatusFilter_delegatesToFindByUserIdAndStatus() {
        Pageable pageable = Pageable.ofSize(20);
        WatchlistItem item = existingItem(1L, currentUser);
        when(watchlistRepository.findByUserIdAndStatus(eq(1L), eq(WatchlistStatus.WATCHED), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(item)));

        Page<WatchlistResponseDto> result = watchlistService.getAll(WatchlistStatus.WATCHED, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(watchlistRepository).findByUserIdAndStatus(1L, WatchlistStatus.WATCHED, pageable);
    }

    @Test
    void getById_returnsItemWhenOwnedByCurrentUser() {
        when(watchlistRepository.findById(1L)).thenReturn(Optional.of(existingItem(1L, currentUser)));

        WatchlistResponseDto response = watchlistService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getById_throwsResourceNotFoundExceptionWhenItemDoesNotExist() {
        when(watchlistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_throwsResourceNotFoundExceptionWhenItemBelongsToAnotherUser() {
        User otherUser = new User();
        otherUser.setId(2L);
        when(watchlistRepository.findById(1L)).thenReturn(Optional.of(existingItem(1L, otherUser)));

        assertThatThrownBy(() -> watchlistService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItem_updatesFieldsWhenOwnedByCurrentUser() {
        WatchlistItem existing = existingItem(1L, currentUser);
        when(watchlistRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(watchlistRepository.save(any(WatchlistItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WatchlistRequestDto update = requestDto();
        update.setTitle("Inception (Updated)");
        update.setStatus(WatchlistStatus.WATCHED);

        WatchlistResponseDto response = watchlistService.updateItem(1L, update);

        assertThat(response.getTitle()).isEqualTo("Inception (Updated)");
        assertThat(response.getStatus()).isEqualTo(WatchlistStatus.WATCHED);
    }

    @Test
    void updateItem_throwsResourceNotFoundExceptionWhenItemDoesNotExist() {
        when(watchlistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.updateItem(99L, requestDto()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItem_throwsResourceNotFoundExceptionWhenItemBelongsToAnotherUser() {
        User otherUser = new User();
        otherUser.setId(2L);
        when(watchlistRepository.findById(1L)).thenReturn(Optional.of(existingItem(1L, otherUser)));

        assertThatThrownBy(() -> watchlistService.updateItem(1L, requestDto()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(watchlistRepository, never()).save(any());
    }

    @Test
    void deleteItem_deletesWhenOwnedByCurrentUser() {
        when(watchlistRepository.findById(1L)).thenReturn(Optional.of(existingItem(1L, currentUser)));

        watchlistService.deleteItem(1L);

        verify(watchlistRepository).deleteById(1L);
    }

    @Test
    void deleteItem_throwsResourceNotFoundExceptionWhenItemDoesNotExist() {
        when(watchlistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.deleteItem(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(watchlistRepository, never()).deleteById(any());
    }

    @Test
    void deleteItem_throwsResourceNotFoundExceptionWhenItemBelongsToAnotherUser() {
        User otherUser = new User();
        otherUser.setId(2L);
        when(watchlistRepository.findById(1L)).thenReturn(Optional.of(existingItem(1L, otherUser)));

        assertThatThrownBy(() -> watchlistService.deleteItem(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(watchlistRepository, never()).deleteById(any());
    }
}
