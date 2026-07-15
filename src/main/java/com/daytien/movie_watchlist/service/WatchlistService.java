package com.daytien.movie_watchlist.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.daytien.movie_watchlist.dto.WatchlistRequestDto;
import com.daytien.movie_watchlist.dto.WatchlistResponseDto;
import com.daytien.movie_watchlist.entity.User;
import com.daytien.movie_watchlist.entity.WatchlistItem;
import com.daytien.movie_watchlist.entity.WatchlistStatus;
import com.daytien.movie_watchlist.exception.ResourceNotFoundException;
import com.daytien.movie_watchlist.repository.WatchlistRepository;

@Service
public class WatchlistService {
    private final WatchlistRepository watchlistRepository;

    public WatchlistService (WatchlistRepository watchlistRepository){
        this.watchlistRepository = watchlistRepository;
    }

     public WatchlistResponseDto create(WatchlistRequestDto input) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        WatchlistItem item = new WatchlistItem();
        WatchlistResponseDto response = new WatchlistResponseDto();
        item.setImdbId(input.getImdbId());
        item.setTitle(input.getTitle());
        item.setYear(input.getYear());
        item.setPoster(input.getPoster());
        item.setStatus(input.getStatus());
        item.setAddedDate(LocalDateTime.now());
        item.setUser(currentUser);
        WatchlistItem savedItem = watchlistRepository.save(item);
        response.setId(savedItem.getId());
        response.setImdbId(savedItem.getImdbId());
        response.setTitle(savedItem.getTitle());
        response.setYear(savedItem.getYear());
        response.setPoster(savedItem.getPoster());
        response.setStatus(savedItem.getStatus());
        response.setAddedDate(savedItem.getAddedDate());

        return response;


     }

     public Page<WatchlistResponseDto> getAll(WatchlistStatus status, Pageable pageable){
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Page<WatchlistItem> page = (status == null)
                ? watchlistRepository.findByUserId(currentUser.getId(), pageable)
                : watchlistRepository.findByUserIdAndStatus(currentUser.getId(), status, pageable);

        return page.map(item -> {
            WatchlistResponseDto dto = new WatchlistResponseDto();
            dto.setId(item.getId());
            dto.setImdbId(item.getImdbId());
            dto.setTitle(item.getTitle());
            dto.setYear(item.getYear());
            dto.setPoster(item.getPoster());
            dto.setStatus(item.getStatus());
            dto.setAddedDate(item.getAddedDate());
            return dto;
        });
     }

     public WatchlistResponseDto getById(Long id) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        WatchlistItem item = watchlistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist item not found with id: " + id));
        if (!item.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Watchlist item not found with id: " + id);
        }
        WatchlistResponseDto response = new WatchlistResponseDto();
        response.setId(item.getId());
        response.setImdbId(item.getImdbId());
        response.setTitle(item.getTitle());
        response.setYear(item.getYear());
        response.setPoster(item.getPoster());
        response.setStatus(item.getStatus());
        response.setAddedDate(item.getAddedDate());
        

        return response;

     }

     public WatchlistResponseDto updateItem(Long id, WatchlistRequestDto incomingData){
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        WatchlistResponseDto response = new WatchlistResponseDto();

        return watchlistRepository.findById(id).map(existingItem -> {
        if (!existingItem.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Watchlist item not found with id: " + id);
        }
        existingItem.setImdbId(incomingData.getImdbId());
        existingItem.setTitle(incomingData.getTitle());
        existingItem.setYear(incomingData.getYear());
        existingItem.setPoster(incomingData.getPoster());
        existingItem.setStatus(incomingData.getStatus());
        WatchlistItem savedItem = watchlistRepository.save(existingItem);
        response.setId(savedItem.getId());
        response.setImdbId(savedItem.getImdbId());
        response.setTitle(savedItem.getTitle());
        response.setYear(savedItem.getYear());
        response.setPoster(savedItem.getPoster());
        response.setStatus(savedItem.getStatus());
        response.setAddedDate(savedItem.getAddedDate());
        return response;
        }).orElseThrow(() -> new ResourceNotFoundException("Watchlist item not found with id: " + id));
     }

     public void deleteItem(Long id){
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        WatchlistItem item = watchlistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist item not found with id: " + id));
        if (!item.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Watchlist item not found with id: " + id);
        }
        watchlistRepository.deleteById(id);
     }

    
}
