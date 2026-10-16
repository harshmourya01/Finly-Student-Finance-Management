package com.studentfinance.service;

import com.studentfinance.dto.AddMoneyRequest;
import com.studentfinance.dto.SavingsGoalRequest;
import com.studentfinance.dto.SavingsGoalResponse;
import com.studentfinance.entity.SavingsGoal;
import com.studentfinance.entity.User;
import com.studentfinance.entity.NotificationType;
import com.studentfinance.repository.SavingsGoalRepository;
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
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final FinancialIdempotencyService idempotencyService;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository,
                              CurrentUserService currentUserService,
                              NotificationService notificationService,
                              FinancialIdempotencyService idempotencyService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> getAll() {
        User user = currentUserService.getCurrentUser();
        return savingsGoalRepository.findAllByUser_Id(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SavingsGoalResponse getById(Long id) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found"));
        verifyOwnership(goal);
        return toResponse(goal);
    }

    @Transactional
    public SavingsGoalResponse create(SavingsGoalRequest request) {
        User user = currentUserService.getCurrentUser();
        notificationService.lockOwner(user);
        LocalDate targetDate = parseDate(request.targetDate());

        BigDecimal current = request.currentAmount() != null ? request.currentAmount() : BigDecimal.ZERO;
        validateSavedAmount(request.targetAmount(), current);

        SavingsGoal goal = SavingsGoal.builder()
                .user(user)
                .name(request.name().trim())
                .targetAmount(request.targetAmount())
                .currentAmount(current)
                .targetDate(targetDate)
                .build();

        SavingsGoal saved = savingsGoalRepository.save(goal);
        notifyCompletionIfReached(saved, BigDecimal.ZERO);
        return toResponse(saved);
    }

    @Transactional
    public SavingsGoalResponse update(Long id, SavingsGoalRequest request) {
        notificationService.lockOwner(currentUserService.getCurrentUser());
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found"));
        verifyOwnership(goal);

        BigDecimal previousSaved = goal.getCurrentAmount();
        BigDecimal previousTarget = goal.getTargetAmount();
        LocalDate targetDate = parseDate(request.targetDate());
        BigDecimal current = request.currentAmount() != null ? request.currentAmount() : previousSaved;
        validateSavedAmount(request.targetAmount(), current);

        goal.setName(request.name().trim());
        goal.setTargetAmount(request.targetAmount());
        goal.setCurrentAmount(current);
        goal.setTargetDate(targetDate);

        SavingsGoal saved = savingsGoalRepository.save(goal);
        if (previousSaved.compareTo(previousTarget) < 0 && current.compareTo(request.targetAmount()) >= 0) {
            notifyCompletionIfReached(saved, previousSaved);
        }
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found"));
        verifyOwnership(goal);
        savingsGoalRepository.delete(goal);
    }

    @Transactional
    public SavingsGoalResponse addMoney(Long id, AddMoneyRequest request) {
        return addMoney(id, request, null);
    }

    @Transactional
    public SavingsGoalResponse addMoney(Long id, AddMoneyRequest request, String idempotencyKey) {
        User user = currentUserService.getCurrentUser();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return addMoneyInternal(id, request, null, user);
        }
        return idempotencyService.execute(user, "SAVINGS_GOAL_CONTRIBUTION", idempotencyKey,
                List.of(id, request.amount().stripTrailingZeros().toPlainString()), SavingsGoalResponse.class,
                () -> addMoneyInternal(id, request, idempotencyKey, user));
    }

    private SavingsGoalResponse addMoneyInternal(Long id, AddMoneyRequest request, String idempotencyKey, User user) {
        notificationService.lockOwner(user);
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found"));
        verifyOwnership(goal);

        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount());
        if (request.amount().compareTo(remaining) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contribution exceeds the remaining goal amount of " + remaining);
        }

        BigDecimal previousSaved = goal.getCurrentAmount();
        goal.setCurrentAmount(previousSaved.add(request.amount()));
        SavingsGoal saved = savingsGoalRepository.save(goal);

        int progress = saved.getCurrentAmount().multiply(BigDecimal.valueOf(100))
                .divide(saved.getTargetAmount(), 0, java.math.RoundingMode.HALF_UP).intValue();
        notificationService.create(saved.getUser(), NotificationType.SAVINGS_CONTRIBUTION,
                "Savings goal updated",
                currency(request.amount()) + " added to your " + saved.getName() + ". You have reached "
                        + progress + "% of your target.",
                saved.getId(), idempotencyKey == null || idempotencyKey.isBlank()
                        ? "goal:" + saved.getId() + ":saved:" + saved.getCurrentAmount().toPlainString()
                        : "goal:" + saved.getId() + ":request:" + idempotencyKey.trim());
        notifyCompletionIfReached(saved, previousSaved);
        return toResponse(saved);
    }


    private void notifyCompletionIfReached(SavingsGoal goal, BigDecimal previousSaved) {
        if (previousSaved.compareTo(goal.getTargetAmount()) < 0
                && goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            notificationService.create(goal.getUser(), NotificationType.SAVINGS_GOAL_COMPLETED,
                    "Savings goal reached",
                    "Congratulations! You have reached your " + currency(goal.getTargetAmount()) + " "
                            + goal.getName() + " Goal.",
                    goal.getId(), "goal:" + goal.getId() + ":completed:target:"
                            + goal.getTargetAmount().toPlainString());
        }
    }

    private String currency(BigDecimal amount) {
        java.text.NumberFormat format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("en-IN"));
        format.setCurrency(java.util.Currency.getInstance("INR"));
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        return format.format(amount);
    }

    private void verifyOwnership(SavingsGoal goal) {
        Long currentUserId = currentUserService.getCurrentUser().getId();
        if (!goal.getUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found");
        }
    }

    private void validateSavedAmount(BigDecimal target, BigDecimal current) {
        if (current.compareTo(target) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Current saved amount cannot exceed the target amount");
        }
    }

    private SavingsGoalResponse toResponse(SavingsGoal goal) {
        String dateStr = goal.getTargetDate().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        return new SavingsGoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getCurrentAmount(),
                dateStr,
                goal.getTargetDate()
        );
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now().plusMonths(6).withDayOfMonth(1);
        }
        if (dateStr.length() == 7) { // "yyyy-MM"
            return YearMonth.parse(dateStr).atDay(1);
        }
        return LocalDate.parse(dateStr);
    }
}
