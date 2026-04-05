package com.findash.backend.controller;

import com.findash.backend.dto.AnalyticsResponse;
import com.findash.backend.dto.TransactionRequest;
import com.findash.backend.dto.TransactionResponse;
import com.findash.backend.model.*;
import com.findash.backend.repository.UserRepository;
import com.findash.backend.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    public TransactionController(TransactionService transactionService,
                                 UserRepository userRepository) {
        this.transactionService = transactionService;
        this.userRepository = userRepository;
    }

    private User getLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public TransactionResponse createTransaction(
            @RequestBody @Valid TransactionRequest request
    ) {
        User user = getLoggedInUser();
        return transactionService.createTransaction(request, user.getRole());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public TransactionResponse updateTransaction(
            @PathVariable UUID id,
            @RequestBody Transaction transaction
    ) {
        User user = getLoggedInUser();
        return transactionService.updateTransaction(id, transaction, user.getRole());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteTransaction(@PathVariable UUID id) {
        User user = getLoggedInUser();
        transactionService.deleteTransaction(id, user.getRole());
        return "Transaction deleted successfully";
    }

    @PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
    @GetMapping("/{id}")
    public TransactionResponse getTransaction(@PathVariable UUID id) {
        User user = getLoggedInUser();
        return transactionService.getTransactionById(id, user.getId(), user.getRole());
    }

    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','VIEWER')")
    @GetMapping
    public List<TransactionResponse> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        User user = getLoggedInUser();

        return transactionService.getTransactions(
                page,
                size,
                userId,
                user.getRole(),
                type,
                category,
                search,
                startDate,
                endDate,
                sortBy,
                sortDir
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','VIEWER')")
    @GetMapping("/analytics")
    public AnalyticsResponse getAnalytics(
            @RequestParam(required = false) UUID userId
    ) {
        User user = getLoggedInUser();
        return transactionService.getAnalytics(userId, user.getRole());
    }
}