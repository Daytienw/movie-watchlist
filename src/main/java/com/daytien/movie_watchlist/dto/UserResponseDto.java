package com.daytien.movie_watchlist.dto;

import java.time.LocalDateTime;

import com.daytien.movie_watchlist.entity.User;

import lombok.Getter;
import lombok.Setter;

/**
 * Safe outward-facing view of a User. The entity itself must never be
 * serialized to a response: it carries the BCrypt hash and, because it
 * implements UserDetails, a pile of Spring Security plumbing fields.
 */
@Getter
@Setter
public class UserResponseDto {

    private Long id;
    private String fullName;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponseDto from(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
