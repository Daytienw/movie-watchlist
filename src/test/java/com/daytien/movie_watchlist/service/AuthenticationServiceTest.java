package com.daytien.movie_watchlist.service;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.daytien.movie_watchlist.dto.LoginUserDto;
import com.daytien.movie_watchlist.dto.RegisterUserDto;
import com.daytien.movie_watchlist.entity.User;
import com.daytien.movie_watchlist.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(userRepository, authenticationManager, passwordEncoder);
    }

    @Test
    void signup_encodesPasswordAndSavesUser() {
        RegisterUserDto input = new RegisterUserDto();
        input.setFullName("aaron dylan");
        input.setEmail("daytien@gmail.com");
        input.setPassword("apple");

        when(passwordEncoder.encode("apple")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authenticationService.signup(input);

        assertThat(result.getFullName()).isEqualTo("aaron dylan");
        assertThat(result.getEmail()).isEqualTo("daytien@gmail.com");
        assertThat(result.getPassword()).isEqualTo("encoded-password");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void authenticate_withValidCredentials_returnsUser() {
        LoginUserDto input = new LoginUserDto();
        input.setEmail("daytien@gmail.com");
        input.setPassword("apple");

        User user = new User();
        user.setEmail("daytien@gmail.com");
        when(userRepository.findByEmail("daytien@gmail.com")).thenReturn(Optional.of(user));

        User result = authenticationService.authenticate(input);

        assertThat(result).isSameAs(user);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getPrincipal()).isEqualTo("daytien@gmail.com");
        assertThat(tokenCaptor.getValue().getCredentials()).isEqualTo("apple");
    }

    @Test
    void authenticate_whenAuthenticationManagerRejectsCredentials_propagatesException() {
        LoginUserDto input = new LoginUserDto();
        input.setEmail("daytien@gmail.com");
        input.setPassword("wrong-password");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.authenticate(input))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void authenticate_whenUserNotFoundAfterAuthentication_throwsNoSuchElementException() {
        LoginUserDto input = new LoginUserDto();
        input.setEmail("missing@gmail.com");
        input.setPassword("apple");

        when(userRepository.findByEmail("missing@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.authenticate(input))
                .isInstanceOf(NoSuchElementException.class);
    }
}
