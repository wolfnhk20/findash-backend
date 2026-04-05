package com.findash.backend.controller;

import com.findash.backend.exception.BadRequestException;
import com.findash.backend.model.Status;
import com.findash.backend.model.User;
import com.findash.backend.repository.UserRepository;
import com.findash.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestParam String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getStatus() != Status.ACTIVE) {
            throw new BadRequestException("User account is inactive");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return Map.of(
                "token", token,
                "userId", user.getId(),
                "email", user.getEmail(),
                "role", user.getRole().name()
        );
    }
}