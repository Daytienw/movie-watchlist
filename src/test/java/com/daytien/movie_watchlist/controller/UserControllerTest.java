package com.daytien.movie_watchlist.controller;

import com.daytien.movie_watchlist.entity.User;
import com.daytien.movie_watchlist.security.JwtAuthFilter;
import com.daytien.movie_watchlist.security.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    private RequestPostProcessor authenticatedAs(User user) {
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private User currentUser() {
        User user = new User();
        user.setId(1L);
        user.setFullName("aaron dylan");
        user.setEmail("daytien@gmail.com");
        user.setPassword("$2a$10$hashed");
        return user;
    }

    @Test
    void authenticatedUser_returnsPrincipalFromSecurityContext() throws Exception {
        mockMvc.perform(get("/api/users/me").with(authenticatedAs(currentUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("daytien@gmail.com"))
                .andExpect(jsonPath("$.fullName").value("aaron dylan"));
    }

    /**
     * Regression: the endpoint used to return the User entity directly, which
     * serialized the BCrypt hash plus the whole UserDetails contract.
     */
    @Test
    void authenticatedUser_neverExposesPasswordOrUserDetailsInternals() throws Exception {
        mockMvc.perform(get("/api/users/me").with(authenticatedAs(currentUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.authorities").doesNotExist())
                .andExpect(jsonPath("$.enabled").doesNotExist())
                .andExpect(jsonPath("$.accountNonExpired").doesNotExist())
                .andExpect(jsonPath("$.accountNonLocked").doesNotExist())
                .andExpect(jsonPath("$.credentialsNonExpired").doesNotExist());
    }

    @Test
    void authenticatedUser_withoutAuthentication_isRejected() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }
}
