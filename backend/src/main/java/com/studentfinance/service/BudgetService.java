package com.studentfinance.service;

import com.studentfinance.dto.BudgetRequest;
import com.studentfinance.dto.BudgetResponse;
import com.studentfinance.entity.Budget;
import com.studentfinance.entity.Category;
import com.studentfinance.entity.User;
import com.studentfinance.repository.BudgetRepository;
import com.studentfinance.repository.CategoryRepository;
import com.studentfinance.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    public BudgetService(BudgetRepository budgetRepository,
                         CategoryRepository categoryRepository,
                         TransactionRepository transactionRepository,
                         CurrentUserService currentUserService) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAll(String monthStr) {
        User user = currentUserService.getCurrentUser();
        List<Budget> budgets;
        if (monthStr != null && !monthStr.isBlank()) {
            LocalDate month = parseMonth(monthStr);
            budgets = budgetRepository.findAllByUser_IdAndMonth(user.getId(), month);
        } else {
            budgets = budgetRepository.findAllByUser_Id(user.getId());
        }

        return budgets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BudgetResponse getById(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
        verifyOwnership(budget);
        return toResponse(budget);
    }

    @Transactional
    public BudgetResponse create(BudgetRequest request) {
        User user = currentUserService.getCurrentUser();
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category"));

        LocalDate month = parseMonth(request.month());

        budgetRepository.findByUser_IdAndCategory_IdAndMonth(user.getId(), category.getId(), month)
                .ifPresent(b -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget already exists for this category and month");
                });

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .amount(request.amount())
                .month(month)
                .build();

        Budget saved = budgetRepository.save(budget);
        return toResponse(saved);
    }

    @Transactional
    public BudgetResponse update(Long id, BudgetRequest request) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
        verifyOwnership(budget);

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category"));

        LocalDate month = parseMonth(request.month());

        budget.setCategory(category);
        budget.setAmount(request.amount());
        budget.setMonth(month);

        Budget saved = budgetRepository.save(budget);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found"));
        verifyOwnership(budget);
        budgetRepository.delete(budget);
    }

    private void verifyOwnership(Budget budget) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        if (!budget.getUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found");
        }
    }

    private BudgetResponse toResponse(Budget budget) {
        LocalDate startOfMonth = budget.getMonth().withDayOfMonth(1);
        LocalDate endOfMonth = budget.getMonth().withDayOfMonth(budget.getMonth().lengthOfMonth());

        BigDecimal spent = transactionRepository.calculateSpentAmount(
                budget.getUser().getId(),
                budget.getCategory().getId(),
                startOfMonth,
                endOfMonth
        );

        String monthStr = budget.getMonth().format(DateTimeFormatter.ofPattern("yyyy-MM"));

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getAmount(),
                budget.getAmount(),
                spent,
                monthStr
        );
    }

    private LocalDate parseMonth(String monthStr) {
        if (monthStr == null || monthStr.isBlank()) {
            return LocalDate.now().withDayOfMonth(1);
        }
        if (monthStr.length() == 7) { // "yyyy-MM"
            return YearMonth.parse(monthStr).atDay(1);
        }
        return LocalDate.parse(monthStr).withDayOfMonth(1);
    }
}
