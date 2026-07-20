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
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "watchlistItems",
    // Uniqueness is per user, not global: two people must both be able to
    // add the same film. A bare unique=true on imdbId would let whoever
    // added it first block everyone else forever.
    uniqueConstraints = @UniqueConstraint(
        name = "uk_watchlist_user_imdb",
        columnNames = {"user_id", "imdbId"}
    )
)
@Getter
@Setter
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "imdbId", nullable = false)
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
