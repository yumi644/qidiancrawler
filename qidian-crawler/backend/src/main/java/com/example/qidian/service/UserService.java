package com.example.qidian.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.qidian.domain.User;
import com.example.qidian.repo.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByPhone(String phone) {
        return userRepository.findByphone(phone);
    }

    public int insertUser(User user) {
        return userRepository.insertUser(user);
    }

    public int deleteUser(User user) {
        return userRepository.deleteUser(user);
    }

    public Optional<User> updateUser(User user) {
        return userRepository.updateUser(user);
    }
}
