package com.findash.backend.service;

import com.findash.backend.dto.UserRequest;
import com.findash.backend.exception.DuplicateResourceException;
import com.findash.backend.model.Role;
import com.findash.backend.model.User;
import com.findash.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserService userService = new UserService(userRepository);

    @Test
    void shouldCreateUser() {
        UserRequest request = new UserRequest();
        request.setName("Ayush");
        request.setEmail("test@gmail.com");
        request.setRole(Role.ADMIN);

        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);

        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setName("Ayush");
        savedUser.setEmail("test@gmail.com");
        savedUser.setRole(Role.ADMIN);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        var response = userService.createUser(request);

        assertEquals("Ayush", response.getName());
        assertEquals("test@gmail.com", response.getEmail());
    }

    @Test
    void shouldThrowWhenEmailExists() {
        UserRequest request = new UserRequest();
        request.setEmail("test@gmail.com");

        when(userRepository.existsByEmail("test@gmail.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> {
            userService.createUser(request);
        });
    }
}