package com.daytien.movie_watchlist.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daytien.movie_watchlist.dto.WatchlistRequestDto;
import com.daytien.movie_watchlist.dto.WatchlistResponseDto;
import com.daytien.movie_watchlist.service.WatchlistService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;



@RequestMapping("/api/watchlist/")
@RestController
public class WatchlistController {
    private final WatchlistService watchlistService;

    public WatchlistController (WatchlistService watchlistService){
        this.watchlistService = watchlistService;
    }

    @PostMapping
    public ResponseEntity<WatchlistResponseDto> createWatchlistItem(@RequestBody WatchlistRequestDto payload){
        WatchlistResponseDto savedWatchlist = watchlistService.create(payload);

        return new ResponseEntity<>(savedWatchlist, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity <List<WatchlistResponseDto>> getAllWatchlistItems() {
        List <WatchlistResponseDto> items = watchlistService.getAll();
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("{id}")
    public ResponseEntity<WatchlistResponseDto> getWatchlistById(@PathVariable("id") Long id) {
        WatchlistResponseDto item = watchlistService.getById(id);
        return ResponseEntity.ok(item);
    }
    
    @PutMapping("{id}")
    public ResponseEntity<WatchlistResponseDto> putMethodName(@PathVariable("id") Long id, @RequestBody WatchlistRequestDto watchlistRequestDto) {
        //TODO: process PUT request
        WatchlistResponseDto item = watchlistService.updateItem(id, watchlistRequestDto);

        return ResponseEntity.ok(item);

    }

    @DeleteMapping("{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable("id") Long id){

        watchlistService.deleteItem(id);

        return ResponseEntity.noContent().build();

    }

}
