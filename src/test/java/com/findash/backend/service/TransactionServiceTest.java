package com.findash.backend.service;

import com.findash.backend.dto.TransactionRequest;
import com.findash.backend.exception.BadRequestException;
import com.findash.backend.exception.UnauthorizedException;
import com.findash.backend.model.*;
import com.findash.backend.repository.TransactionRepository;
import com.findash.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {
    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private final TransactionService transactionService =
            new TransactionService(transactionRepository, userRepository);

    @Test
    void shouldFailForNegativeAmount() {
        UUID userId = UUID.randomUUID();

        TransactionRequest request = new TransactionRequest();
        request.setAmount(BigDecimal.valueOf(-100.0));
        request.setUserId(userId);

        User user = new User();
        user.setId(userId);
        user.setStatus(Status.ACTIVE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> {
            transactionService.createTransaction(request, Role.ADMIN);
        });
    }

    @Test
    void shouldFailForNonAdmin() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(BigDecimal.valueOf(100.0));

        assertThrows(UnauthorizedException.class, () -> {
            transactionService.createTransaction(request, Role.VIEWER);
        });
    }

    @Test
    void shouldFailForInactiveUser() {
        UUID userId = UUID.randomUUID();

        TransactionRequest request = new TransactionRequest();
        request.setAmount(BigDecimal.valueOf(100.0));
        request.setUserId(userId);

        User user = new User();
        user.setId(userId);
        user.setStatus(Status.INACTIVE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> {
            transactionService.createTransaction(request, Role.ADMIN);
        });
    }

    @Test
    void shouldCreateTransaction() {
        UUID userId = UUID.randomUUID();

        TransactionRequest request = new TransactionRequest();
        request.setAmount(BigDecimal.valueOf(100.0));
        request.setUserId(userId);
        request.setType(TransactionType.INCOME);
        request.setCategory(Category.RENT);
        request.setDate(LocalDate.now());

        User user = new User();
        user.setId(userId);
        user.setStatus(Status.ACTIVE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        var response = transactionService.createTransaction(request, Role.ADMIN);

        assertEquals(BigDecimal.valueOf(100.0), response.getAmount());
    }
}