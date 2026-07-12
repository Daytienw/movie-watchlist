package com.daytien.movie_watchlist.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "watchlistItems")
@Getter
@Setter
public class WatchlistItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "imdbId", nullable = false, unique = true)
    private String imdbId;
    
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "year", nullable = false)
    private String year;

    @Column(name = "poster", nullable = false)
    private String poster;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WatchlistStatus status;
    
    @Column(name = "addedDate", nullable = false)
    private LocalDateTime addedDate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

}
