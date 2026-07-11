package com.daytien.movie_watchlist.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.daytien.movie_watchlist.entity.User;
import com.daytien.movie_watchlist.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository){

        this.userRepository = userRepository;
    }

    public List<User> allUsers() {

       return userRepository.findAll();

    }
    
}
