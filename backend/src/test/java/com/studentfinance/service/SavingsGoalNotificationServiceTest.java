package com.studentfinance.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.studentfinance.dto.AddMoneyRequest;
import com.studentfinance.entity.NotificationType;
import com.studentfinance.entity.SavingsGoal;
import com.studentfinance.entity.User;
import com.studentfinance.repository.SavingsGoalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SavingsGoalNotificationServiceTest {
    @Mock SavingsGoalRepository goals;
    @Mock CurrentUserService currentUser;
    @Mock NotificationService notifications;
    private SavingsGoalService service;
    private User user;
    private SavingsGoal goal;

    @BeforeEach
    void setUp() {
        service = new SavingsGoalService(goals, currentUser, notifications, mock(FinancialIdempotencyService.class));
        user = new User();
        user.setId(3L);
        goal = SavingsGoal.builder().user(user).name("Laptop")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(new BigDecimal("400.00"))
                .targetDate(LocalDate.of(2027, 1, 1)).build();
        goal.setId(9L);
        when(currentUser.getCurrentUser()).thenReturn(user);
        when(goals.findById(9L)).thenReturn(Optional.of(goal));
    }

    @Test
    void successfulContributionCreatesActualProgressNotification() {
        when(goals.save(any(SavingsGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.addMoney(9L, new AddMoneyRequest(new BigDecimal("500.00")));

        verify(notifications).create(eq(user), eq(NotificationType.SAVINGS_CONTRIBUTION),
                eq("Savings goal updated"), contains("500"), eq(9L), eq("goal:9:saved:900.00"));
        verify(notifications, never()).create(any(), eq(NotificationType.SAVINGS_GOAL_COMPLETED),
                anyString(), anyString(), any(), anyString());
    }

    @Test
    void failedContributionCreatesNoNotification() {
        org.junit.jupiter.api.Assertions.assertThrows(ResponseStatusException.class,
                () -> service.addMoney(9L, new AddMoneyRequest(new BigDecimal("601.00"))));

        verify(goals, never()).save(any());
        verify(notifications, never()).create(any(), any(), anyString(), anyString(), any(), any());
    }

    @Test
    void completingContributionCreatesCompletionNotificationOnce() {
        when(goals.save(any(SavingsGoal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.addMoney(9L, new AddMoneyRequest(new BigDecimal("600.00")));

        verify(notifications).create(eq(user), eq(NotificationType.SAVINGS_GOAL_COMPLETED),
                eq("Savings goal reached"), contains("Laptop"), eq(9L),
                eq("goal:9:completed:target:1000.00"));
    }
}
