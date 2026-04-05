package com.findash.backend.dto;

import com.findash.backend.model.Role;
import com.findash.backend.model.Status;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {
    private UUID id;
    private String name;
    private String email;
    private Role role;
    private Status status;
    private LocalDateTime createdAt;
}
