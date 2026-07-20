package com.daytien.movie_watchlist.controller;

import com.daytien.movie_watchlist.entity.User;
import com.daytien.movie_watchlist.exception.DuplicateResourceException;
import com.daytien.movie_watchlist.exception.GlobalExceptionHandler;
import com.daytien.movie_watchlist.security.JwtAuthFilter;
import com.daytien.movie_watchlist.security.JwtService;
import com.daytien.movie_watchlist.security.SecurityConfig;
import com.daytien.movie_watchlist.service.AuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

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

    private static final String VALID_SIGNUP_JSON = """
            {
                "email": "daytien@gmail.com",
                "password": "correct-horse",
                "fullName": "aaron dylan"
            }
            """;

    private User registeredUser() {
        User registeredUser = new User();
        registeredUser.setId(1L);
        registeredUser.setFullName("aaron dylan");
        registeredUser.setEmail("daytien@gmail.com");
        registeredUser.setPassword("$2a$10$hashed");
        return registeredUser;
    }

    @Test
    @WithMockUser
    void signup_withValidBody_returnsCreatedUser() throws Exception {
        when(authenticationService.signup(any())).thenReturn(registeredUser());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(VALID_SIGNUP_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("daytien@gmail.com"))
                .andExpect(jsonPath("$.fullName").value("aaron dylan"));

        verify(authenticationService).signup(any());
    }

    /**
     * Regression: signup used to return the User entity directly, putting the
     * BCrypt hash in the response body.
     */
    @Test
    @WithMockUser
    void signup_neverExposesPasswordOrUserDetailsInternals() throws Exception {
        when(authenticationService.signup(any())).thenReturn(registeredUser());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(VALID_SIGNUP_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.authorities").doesNotExist())
                .andExpect(jsonPath("$.enabled").doesNotExist());
    }

    @Test
    @WithMockUser
    void signup_withEmptyBody_returnsBadRequestNotServerError() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.fullName").exists());

        verifyNoInteractions(authenticationService);
    }

    @Test
    @WithMockUser
    void signup_withMalformedEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "not-an-email",
                                    "password": "correct-horse",
                                    "fullName": "aaron dylan"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());

        verifyNoInteractions(authenticationService);
    }

    @Test
    @WithMockUser
    void signup_withShortPassword_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "daytien@gmail.com",
                                    "password": "short",
                                    "fullName": "aaron dylan"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        verifyNoInteractions(authenticationService);
    }

    @Test
    @WithMockUser
    void signup_withDuplicateEmail_returnsConflict() throws Exception {
        when(authenticationService.signup(any()))
                .thenThrow(new DuplicateResourceException("An account with that email already exists"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(VALID_SIGNUP_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void login_withBadCredentials_returnsUnauthorized() throws Exception {
        when(authenticationService.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "daytien@gmail.com",
                                    "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void login_withValidCredentials_returnsToken() throws Exception {
        User authenticatedUser = new User();
        authenticatedUser.setEmail("daytien@gmail.com");
        when(authenticationService.authenticate(any())).thenReturn(authenticatedUser);
        when(jwtService.generateToken(authenticatedUser)).thenReturn("jwt-token");
        when(jwtService.getExpirationTime()).thenReturn(3600000L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                    "email": "daytien@gmail.com",
                                    "password": "apple"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.expiresIn").value(3600000));
    }
}
