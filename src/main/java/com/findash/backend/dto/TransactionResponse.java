package com.findash.backend.dto;

import com.findash.backend.model.Category;
import com.findash.backend.model.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TransactionResponse {

    private UUID id;
    private BigDecimal amount;
    private TransactionType type;
    private Category category;
    private LocalDate date;
    private String description;
    private UUID userId;
    private LocalDateTime createdAt;
}