package com.daytien.movie_watchlist.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.daytien.movie_watchlist.entity.User;
import com.daytien.movie_watchlist.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @Test
    void allUsers_returnsAllUsersFromRepository() {
        userService = new UserService(userRepository);

        User first = new User();
        first.setEmail("first@example.com");
        User second = new User();
        second.setEmail("second@example.com");
        when(userRepository.findAll()).thenReturn(List.of(first, second));

        List<User> result = userService.allUsers();

        assertThat(result).containsExactly(first, second);
    }

    @Test
    void allUsers_returnsEmptyListWhenNoUsersExist() {
        userService = new UserService(userRepository);

        when(userRepository.findAll()).thenReturn(List.of());

        List<User> result = userService.allUsers();

        assertThat(result).isEmpty();
    }
}
