package com.studentfinance.service;

import com.studentfinance.dto.TransactionRequest;
import com.studentfinance.dto.BalanceSummaryResponse;
import com.studentfinance.dto.TransactionResponse;
import com.studentfinance.dto.CategoryDto;
import com.studentfinance.entity.Category;
import com.studentfinance.entity.Transaction;
import com.studentfinance.entity.User;
import com.studentfinance.entity.TransactionType;
import com.studentfinance.entity.NotificationType;
import com.studentfinance.entity.SavingsGoal;
import com.studentfinance.repository.SavingsGoalRepository;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import com.studentfinance.repository.CategoryRepository;
import com.studentfinance.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.studentfinance.service.CurrentUserService;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;
    private final SavingsGoalRepository savingsGoalRepository;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;
    private final BudgetService budgetService;
    private final FinancialIdempotencyService idempotencyService;

    public TransactionService(TransactionRepository transactionRepository,
                              CategoryRepository categoryRepository,
                              CurrentUserService currentUserService,
                              SavingsGoalRepository savingsGoalRepository,
                              SubscriptionService subscriptionService,
                              NotificationService notificationService,
                              BudgetService budgetService,
                              FinancialIdempotencyService idempotencyService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
        this.savingsGoalRepository = savingsGoalRepository;
        this.subscriptionService = subscriptionService;
        this.notificationService = notificationService;
        this.budgetService = budgetService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getAll(String type, String search, String fromDate, String toDate,
                                             Long categoryId, String paymentMethod) {
        Long userId = currentUserService.getCurrentUser().getId();
        List<Transaction> transactions = transactionRepository.findAllByUser_Id(userId);
        return transactions.stream()
                .filter(t -> type == null || t.getType().name().equalsIgnoreCase(type))
                .filter(t -> search == null || (
                        t.getDescription().toLowerCase().contains(search.toLowerCase()) ||
                        t.getCategory().getName().toLowerCase().contains(search.toLowerCase()) ||
                        t.getPaymentMethod().name().toLowerCase().contains(search.toLowerCase())))
                .filter(t -> categoryId == null || t.getCategory().getId().equals(categoryId))
                .filter(t -> paymentMethod == null || t.getPaymentMethod().name().equalsIgnoreCase(paymentMethod))
                .filter(t -> {
                    if (fromDate == null && toDate == null) return true;
                    if (fromDate != null && t.getTransactionDate().isBefore(java.time.LocalDate.parse(fromDate))) return false;
                    if (toDate != null && t.getTransactionDate().isAfter(java.time.LocalDate.parse(toDate))) return false;
                    return true;
                })
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        verifyOwnership(transaction);
        return toResponse(transaction);
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        return create(request, null);
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request, String idempotencyKey) {
        var currentUser = currentUserService.getCurrentUser();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return createInternal(request, null, currentUser);
        }
        List<Object> fingerprint = java.util.Arrays.asList(
                request.amount().stripTrailingZeros().toPlainString(), request.transactionDate(), request.description(),
                request.type(), request.paymentMethod(), request.categoryId(), request.notes());
        return idempotencyService.execute(currentUser, "TRANSACTION_CREATE", idempotencyKey, fingerprint,
                TransactionResponse.class, () -> createInternal(request, idempotencyKey, currentUser));
    }

    private TransactionResponse createInternal(TransactionRequest request, String idempotencyKey,
                                               User currentUser) {
        notificationService.lockOwner(currentUser);
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category"));
        if (!category.getType().equals(request.type())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category type mismatch");
        }
        if (request.type() == TransactionType.EXPENSE) {
            validateSpendableBalance(currentUser.getId(), request.amount(), null);
        }
        Transaction transaction = new Transaction();
        transaction.setUser(currentUser);
        transaction.setCategory(category);
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setPaymentMethod(request.paymentMethod());
        transaction.setNotes(SubscriptionService.preservePaymentMarkers(transaction.getNotes(), request.notes()));
        Transaction saved = transactionRepository.save(transaction);
        String typeLabel = saved.getType() == TransactionType.INCOME ? "income" : "expense";
        notificationService.create(currentUser, NotificationType.TRANSACTION_RECORDED,
                "Transaction recorded", "Your " + currency(saved.getAmount()) + " " + typeLabel + " has been recorded.",
                saved.getId(), idempotencyKey == null || idempotencyKey.isBlank()
                        ? "transaction:" + saved.getId() + ":recorded"
                        : "transaction:request:" + idempotencyKey.trim());
        if (saved.getType() == TransactionType.EXPENSE) {
            budgetService.notifyBudgetForExpense(currentUser, saved.getCategory(), saved.getTransactionDate());
        }
        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        notificationService.lockOwner(currentUserService.getCurrentUser());
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        verifyOwnership(transaction);
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid category"));
        if (!category.getType().equals(request.type())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category type mismatch");
        }
        if (request.type() == TransactionType.EXPENSE) {
            validateSpendableBalance(transaction.getUser().getId(), request.amount(), transaction.getId());
        }
        Category previousCategory = transaction.getCategory();
        TransactionType previousType = transaction.getType();
        java.time.LocalDate previousDate = transaction.getTransactionDate();
        transaction.setCategory(category);
        transaction.setType(request.type());
        transaction.setAmount(request.amount());
        transaction.setDescription(request.description());
        transaction.setTransactionDate(request.transactionDate());
        transaction.setPaymentMethod(request.paymentMethod());
        transaction.setNotes(SubscriptionService.preservePaymentMarkers(transaction.getNotes(), request.notes()));
        Transaction saved = transactionRepository.save(transaction);
        if (previousType == TransactionType.EXPENSE) {
            budgetService.notifyBudgetForExpense(saved.getUser(), previousCategory, previousDate);
        }
        if (saved.getType() == TransactionType.EXPENSE) {
            budgetService.notifyBudgetForExpense(saved.getUser(), saved.getCategory(), saved.getTransactionDate());
        }
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        notificationService.lockOwner(currentUserService.getCurrentUser());
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        verifyOwnership(transaction);
        User user = transaction.getUser();
        Category category = transaction.getCategory();
        TransactionType type = transaction.getType();
        java.time.LocalDate date = transaction.getTransactionDate();
        transactionRepository.delete(transaction);
        if (type == TransactionType.EXPENSE) {
            budgetService.notifyBudgetForExpense(user, category, date);
        }
    }

    @Transactional(readOnly = true)
    public BalanceSummaryResponse getBalanceSummary() {
        return calculateBalanceSummary(currentUserService.getCurrentUser().getId(), null);
    }

    private BalanceSummaryResponse calculateBalanceSummary(Long userId, Long transactionIdBeingUpdated) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        List<Transaction> userTransactions = transactionRepository.findAllByUser_Id(userId);
        for (Transaction existing : userTransactions) {
            if (transactionIdBeingUpdated != null && transactionIdBeingUpdated.equals(existing.getId())) {
                continue;
            }
            if (existing.getType() == TransactionType.INCOME) {
                totalIncome = totalIncome.add(existing.getAmount());
            } else if (existing.getType() == TransactionType.EXPENSE) {
                totalExpenses = totalExpenses.add(existing.getAmount());
            }
        }
        BigDecimal totalBalance = totalIncome.subtract(totalExpenses);
        BigDecimal reservedSavings = savingsGoalRepository.findAllByUser_Id(userId).stream()
                .map(SavingsGoal::getCurrentAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal subscriptionCommitments = subscriptionService.calculateUnpaidMonthlyCommitments(userId, userTransactions);
        BigDecimal spendableBalance = totalBalance.subtract(reservedSavings).subtract(subscriptionCommitments);
        return new BalanceSummaryResponse(totalBalance, reservedSavings, spendableBalance);
    }

    private void validateSpendableBalance(Long userId, BigDecimal newExpense, Long transactionIdBeingUpdated) {
        BigDecimal spendableBalance = calculateBalanceSummary(userId, transactionIdBeingUpdated).spendableBalance();
        if (newExpense.compareTo(spendableBalance) > 0) {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"));
            currencyFormat.setCurrency(java.util.Currency.getInstance("INR"));
            currencyFormat.setMinimumFractionDigits(2);
            currencyFormat.setMaximumFractionDigits(2);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Insufficient spendable balance. Your available amount after reserved savings and active subscriptions is "
                            + currencyFormat.format(spendableBalance) + ".");
        }
    }

    private String currency(BigDecimal amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"));
        format.setCurrency(java.util.Currency.getInstance("INR"));
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        return format.format(amount);
    }

    private void verifyOwnership(Transaction transaction) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        if (!transaction.getUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found");
        }
    }

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAmount(),
                t.getTransactionDate(),
                t.getDescription(),
                t.getType(),
                t.getPaymentMethod(),
                new CategoryDto(t.getCategory().getId(), t.getCategory().getName()),
                SubscriptionService.userNotes(t.getNotes())
        );
    }
}
