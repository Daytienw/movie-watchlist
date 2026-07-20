package com.daytien.movie_watchlist.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.daytien.movie_watchlist.dto.UserResponseDto;
import com.daytien.movie_watchlist.entity.User;

@RequestMapping("/api/users")
@RestController
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> authenticatedUser(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(UserResponseDto.from(currentUser));
    }
}
