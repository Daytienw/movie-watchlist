package com.daytien.movie_watchlist.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.daytien.movie_watchlist.dto.WatchlistRequestDto;
import com.daytien.movie_watchlist.dto.WatchlistResponseDto;
import com.daytien.movie_watchlist.entity.User;
import com.daytien.movie_watchlist.entity.WatchlistItem;
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

     public List<WatchlistResponseDto> getAll(){
        List<WatchlistItem> list = watchlistRepository.findAll(); 
        List<WatchlistResponseDto> results = new ArrayList<>();
        
        for (WatchlistItem item : list){
            WatchlistResponseDto dto = new WatchlistResponseDto();
            
            dto.setId(item.getId());
            dto.setImdbId(item.getImdbId());
            dto.setTitle(item.getTitle());
            dto.setYear(item.getYear());
            dto.setPoster(item.getPoster());
            dto.setStatus(item.getStatus());
            dto.setAddedDate(item.getAddedDate());
            results.add(dto);
            
        }
        return results;


     }

     public WatchlistResponseDto getById(Long id) {
        WatchlistItem item = watchlistRepository.findById(id).orElseThrow();
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

        WatchlistResponseDto response = new WatchlistResponseDto();

        return watchlistRepository.findById(id).map(existingItem -> {
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
        }).orElseThrow(() -> new RuntimeException("Watchlist item not found with id: " + id));
     }

     public void deleteItem(Long id){
        if (!watchlistRepository.existsById(id)){
            throw new RuntimeException("Watchlist item not found with id: " + id);
        }
        else {
            watchlistRepository.deleteById(id);
        }

     }

    
}
