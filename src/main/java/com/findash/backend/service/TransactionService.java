package com.findash.backend.service;

import com.findash.backend.dto.*;
import com.findash.backend.exception.BadRequestException;
import com.findash.backend.exception.ResourceNotFoundException;
import com.findash.backend.exception.UnauthorizedException;
import com.findash.backend.model.*;
import com.findash.backend.repository.TransactionRepository;
import com.findash.backend.repository.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    public TransactionService(TransactionRepository transactionRepository,
                              UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public TransactionResponse createTransaction(TransactionRequest request, Role role) {

        log.info("Attempting to create transaction for user {}", request.getUserId());

        if (role != Role.ADMIN) {
            log.error("Unauthorized create attempt by role {}", role);
            throw new UnauthorizedException("Only admin can create transactions");
        }

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Invalid transaction amount: {}", request.getAmount());
            throw new BadRequestException("Amount must be positive");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> {
                    log.error("User not found: {}", request.getUserId());
                    return new ResourceNotFoundException("User not found");
                });

        if (user.getStatus() == Status.INACTIVE) {
            log.warn("Attempt to create transaction for inactive user {}", user.getId());
            throw new BadRequestException("Cannot create transaction for inactive user");
        }

        Transaction txn = new Transaction();
        txn.setAmount(request.getAmount());
        txn.setType(request.getType());
        txn.setCategory(request.getCategory());
        txn.setDate(request.getDate());
        txn.setDescription(request.getDescription());
        txn.setUser(user);

        Transaction saved = transactionRepository.save(txn);

        log.info("Transaction created successfully with id {}", saved.getId());

        return mapToResponse(saved);
    }

    public List<TransactionResponse> getTransactions(
            int page,
            int size,
            UUID userId,
            Role role,
            TransactionType type,
            Category category,
            String search,
            LocalDate startDate,
            LocalDate endDate,
            String sortBy,
            String sortDir
    ) {

        log.info("Fetching transactions | page={} size={} userId={} role={}", page, size, userId, role);

        if (size > 50) size = 50;

        if ((startDate != null && endDate == null) || (startDate == null && endDate != null)) {
            log.warn("Invalid date filter: startDate={} endDate={}", startDate, endDate);
            throw new BadRequestException("Both startDate and endDate are required");
        }

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Transaction> transactionPage;

        if (search != null && !search.isBlank()) {
            transactionPage = transactionRepository.search(search, pageable);
        }

        else if (startDate != null && endDate != null) {
            transactionPage = transactionRepository
                    .findByDateBetween(startDate, endDate, pageable);
        }

        else if (role == Role.VIEWER) {
            if (userId == null) {
                log.error("Viewer tried to fetch without userId");
                throw new UnauthorizedException("User ID required for viewer");
            }
            transactionPage = transactionRepository.findByUserId(userId, pageable);
        }

        else if (userId != null) {
            transactionPage = transactionRepository.findByUserId(userId, pageable);
        }

        else {
            transactionPage = transactionRepository.findAll(pageable);
        }

        List<Transaction> filtered = transactionPage.getContent();

        if (type != null) {
            filtered = filtered.stream()
                    .filter(t -> t.getType() == type)
                    .toList();
        }

        if (category != null) {
            filtered = filtered.stream()
                    .filter(t -> t.getCategory() == category)
                    .toList();
        }

        log.info("Transactions fetched successfully");

        return filtered.stream()
                .map(this::mapToResponse)
                .toList();
    }

    public TransactionResponse getTransactionById(UUID id, UUID userId, Role role) {

        log.info("Fetching transaction by id {}", id);

        Transaction txn = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Transaction not found: {}", id);
                    return new ResourceNotFoundException("Transaction not found");
                });

        if (role == Role.VIEWER && !txn.getUser().getId().equals(userId)) {
            log.error("Unauthorized access to transaction {} by user {}", id, userId);
            throw new UnauthorizedException("Access denied");
        }

        return mapToResponse(txn);
    }

    public TransactionResponse updateTransaction(UUID id, Transaction updated, Role role) {

        log.info("Updating transaction {}", id);

        if (role != Role.ADMIN) {
            log.error("Unauthorized update attempt by role {}", role);
            throw new UnauthorizedException("Only admin can update");
        }

        Transaction txn = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Transaction not found: {}", id);
                    return new ResourceNotFoundException("Transaction not found");
                });

        if (txn.getUser().getStatus() == Status.INACTIVE) {
            log.warn("Update attempt on inactive user's transaction {}", id);
            throw new BadRequestException("Inactive user");
        }

        if (updated.getAmount() != null)
            txn.setAmount(updated.getAmount());

        if (updated.getCategory() != null)
            txn.setCategory(updated.getCategory());

        if (updated.getType() != null)
            txn.setType(updated.getType());

        if (updated.getDate() != null)
            txn.setDate(updated.getDate());

        if (updated.getDescription() != null)
            txn.setDescription(updated.getDescription());

        log.info("Transaction updated successfully {}", id);

        return mapToResponse(transactionRepository.save(txn));
    }

    public void deleteTransaction(UUID id, Role role) {

        log.info("Deleting transaction {}", id);

        if (role != Role.ADMIN) {
            log.error("Unauthorized delete attempt by role {}", role);
            throw new UnauthorizedException("Only admin can delete");
        }

        Transaction txn = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Transaction not found: {}", id);
                    return new ResourceNotFoundException("Transaction not found");
                });

        if (txn.getUser().getStatus() == Status.INACTIVE) {
            log.warn("Delete attempt on inactive user's transaction {}", id);
            throw new BadRequestException("Inactive user");
        }

        transactionRepository.delete(txn);

        log.info("Transaction deleted successfully {}", id);
    }

    public AnalyticsResponse getAnalytics(UUID userId, Role role) {

        log.info("Fetching analytics for userId={} role={}", userId, role);

        List<Transaction> transactions;

        if (role == Role.VIEWER) {
            if (userId == null)
                throw new UnauthorizedException("User ID required");

            transactions = transactionRepository.findByUserId(userId);
        } else {
            transactions = (userId != null) ?
                    transactionRepository.findByUserId(userId) :
                    transactionRepository.findAll();
        }

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;

        Map<Category, CategoryAnalytics> categoryTotals = new HashMap<>();
        Map<String, BigDecimal> monthlySpend = new HashMap<>();

        for (Transaction t : transactions) {

            BigDecimal amt = t.getAmount();
            Category cat = t.getCategory();

            CategoryAnalytics existing = categoryTotals.getOrDefault(
                    cat,
                    CategoryAnalytics.builder()
                            .income(BigDecimal.ZERO)
                            .expense(BigDecimal.ZERO)
                            .build()
            );

            if (t.getType() == TransactionType.INCOME) {
                income = income.add(amt);
                existing = existing.toBuilder().income(existing.getIncome().add(amt)).build();
            } else {
                expense = expense.add(amt);
                existing = existing.toBuilder().expense(existing.getExpense().add(amt)).build();

                String month = YearMonth.from(t.getDate()).toString();
                monthlySpend.put(month,
                        monthlySpend.getOrDefault(month, BigDecimal.ZERO).add(amt));
            }

            categoryTotals.put(cat, existing);
        }

        BigDecimal net = income.subtract(expense);

        BigDecimal avg = monthlySpend.isEmpty() ? BigDecimal.ZERO :
                monthlySpend.values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(monthlySpend.size()), 2, RoundingMode.HALF_UP);

        log.info("Analytics computed successfully");

        return AnalyticsResponse.builder()
                .totalIncome(income)
                .totalExpense(expense)
                .netBalance(net)
                .categoryTotals(categoryTotals)
                .monthlySpend(monthlySpend)
                .averageMonthlySpend(avg)
                .build();
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .type(t.getType())
                .category(t.getCategory())
                .date(t.getDate())
                .description(t.getDescription())
                .userId(t.getUser().getId())
                .createdAt(t.getCreatedAt())
                .build();
    }
}