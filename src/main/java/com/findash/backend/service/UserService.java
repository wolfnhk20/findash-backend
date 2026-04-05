package com.findash.backend.service;

import com.findash.backend.dto.UserRequest;
import com.findash.backend.dto.UserResponse;
import com.findash.backend.exception.BadRequestException;
import com.findash.backend.exception.DuplicateResourceException;
import com.findash.backend.exception.ResourceNotFoundException;
import com.findash.backend.exception.UnauthorizedException;
import com.findash.backend.model.Role;
import com.findash.backend.model.Status;
import com.findash.backend.model.User;
import com.findash.backend.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(UserRequest request) {

        log.info("Creating user with email {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Duplicate email attempt: {}", request.getEmail());
            throw new DuplicateResourceException("Email already exists!");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());

        User savedUser = userRepository.save(user);

        log.info("User created successfully with id {}", savedUser.getId());

        return mapToResponse(savedUser);
    }

    public User getUserById(UUID id) {

        log.info("Fetching user by id {}", id);

        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found: {}", id);
                    return new ResourceNotFoundException("User not found");
                });
    }

    public UserResponse updateUser(UUID id, User updatedUser, Role authRole) {

        log.info("Updating user {}", id);

        if (authRole != Role.ADMIN) {
            log.error("Unauthorized update attempt by role {}", authRole);
            throw new UnauthorizedException("Only admin can update users");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found: {}", id);
                    return new ResourceNotFoundException("User not found");
                });

        if (updatedUser.getName() != null) {
            user.setName(updatedUser.getName());
        }

        if (updatedUser.getEmail() != null) {
            userRepository.findByEmail(updatedUser.getEmail())
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(u -> {
                        log.warn("Duplicate email update attempt: {}", updatedUser.getEmail());
                        throw new DuplicateResourceException("Email already exists");
                    });

            user.setEmail(updatedUser.getEmail());
        }

        if (updatedUser.getRole() != null) {
            user.setRole(updatedUser.getRole());
        }

        if (updatedUser.getStatus() != null) {
            user.setStatus(updatedUser.getStatus());
        }

        User savedUser = userRepository.save(user);

        log.info("User updated successfully {}", id);

        return mapToResponse(savedUser);
    }

    public UserResponse deactivateUser(UUID id, Role authRole) {

        log.info("Deactivating user {}", id);

        if (authRole != Role.ADMIN) {
            log.error("Unauthorized deactivate attempt by role {}", authRole);
            throw new UnauthorizedException("Only admin can deactivate users");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found: {}", id);
                    return new ResourceNotFoundException("User not found");
                });

        if (user.getStatus() == Status.INACTIVE) {
            log.warn("User already inactive {}", id);
            throw new BadRequestException("User already inactive");
        }

        user.setStatus(Status.INACTIVE);

        User savedUser = userRepository.save(user);

        log.info("User deactivated successfully {}", id);

        return mapToResponse(savedUser);
    }

    public List<UserResponse> getUsers(
            int page,
            int size,
            Role authRole,
            Role filterRole,
            String sortBy,
            String sortDir
    ) {

        log.info("Fetching users | page={} size={} role={}", page, size, authRole);

        if (authRole == Role.VIEWER) {
            log.error("Viewer attempted to access user list");
            throw new UnauthorizedException("Access denied");
        }

        if (size > 50) size = 50;

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<User> userPage;

        if (filterRole != null) {
            userPage = userRepository.findByRole(filterRole, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        log.info("Users fetched successfully");

        return userPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public UserResponse getUserByEmail(String email) {

        log.info("Fetching user by email {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email {}", email);
                    return new ResourceNotFoundException("User not found");
                });

        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}