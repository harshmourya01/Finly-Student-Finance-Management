package com.studentfinance.service;

import com.studentfinance.dto.AddMoneyRequest;
import com.studentfinance.dto.SavingsGoalRequest;
import com.studentfinance.dto.SavingsGoalResponse;
import com.studentfinance.entity.SavingsGoal;
import com.studentfinance.entity.User;
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

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository,
                              CurrentUserService currentUserService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.currentUserService = currentUserService;
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
        return toResponse(saved);
    }

    @Transactional
    public SavingsGoalResponse update(Long id, SavingsGoalRequest request) {
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found"));
        verifyOwnership(goal);

        LocalDate targetDate = parseDate(request.targetDate());
        BigDecimal current = request.currentAmount() != null ? request.currentAmount() : goal.getCurrentAmount();
        validateSavedAmount(request.targetAmount(), current);

        goal.setName(request.name().trim());
        goal.setTargetAmount(request.targetAmount());
        goal.setCurrentAmount(current);
        goal.setTargetDate(targetDate);

        SavingsGoal saved = savingsGoalRepository.save(goal);
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
        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Savings goal not found"));
        verifyOwnership(goal);

        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount());
        if (request.amount().compareTo(remaining) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contribution exceeds the remaining goal amount of " + remaining);
        }

        goal.setCurrentAmount(goal.getCurrentAmount().add(request.amount()));
        SavingsGoal saved = savingsGoalRepository.save(goal);
        return toResponse(saved);
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
