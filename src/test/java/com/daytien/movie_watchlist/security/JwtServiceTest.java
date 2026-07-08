package com.daytien.movie_watchlist.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.ExpiredJwtException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET_KEY =
            "Y2E5ZTFkMzRiOGE5YjYyMDFlYzg0M2U5MmFmNzM1YjY4YzFkZDUwMmU0YTM4YTk2YmU4YzhkMmI0MmU0MzgxYg==";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "expirationTime", 3600000L);
    }

    private UserDetails userDetails(String email) {
        UserBuilder builder = org.springframework.security.core.userdetails.User.withUsername(email);
        return builder.password("password").authorities("USER").build();
    }

    @Test
    void generateToken_thenExtractUsername_returnsOriginalEmail() {
        UserDetails userDetails = userDetails("daytien@gmail.com");

        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractUsername(token)).isEqualTo("daytien@gmail.com");
    }

    @Test
    void isTokenValid_forMatchingUserAndUnexpiredToken_returnsTrue() {
        UserDetails userDetails = userDetails("daytien@gmail.com");
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_forDifferentUser_returnsFalse() {
        UserDetails tokenOwner = userDetails("daytien@gmail.com");
        UserDetails otherUser = userDetails("someoneelse@gmail.com");
        String token = jwtService.generateToken(tokenOwner);

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenValid_forExpiredToken_throwsExpiredJwtException() {
        ReflectionTestUtils.setField(jwtService, "expirationTime", -1000L);
        UserDetails userDetails = userDetails("daytien@gmail.com");
        String token = jwtService.generateToken(userDetails);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void getExpirationTime_returnsConfiguredValue() {
        assertThat(jwtService.getExpirationTime()).isEqualTo(3600000L);
    }
}
