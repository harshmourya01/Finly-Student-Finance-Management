package com.studentfinance.service;

import static org.mockito.Mockito.*;

import com.studentfinance.entity.NotificationType;
import com.studentfinance.entity.SavingsGoal;
import com.studentfinance.entity.Subscription;
import com.studentfinance.entity.SubscriptionStatus;
import com.studentfinance.entity.User;
import com.studentfinance.repository.SubscriptionRepository;
import com.studentfinance.repository.SavingsGoalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SavingsGoalReminderSchedulerTest {
    @Test
    void sendsOnlyDueIncompleteGoalRemindersWithStableDedupeKeys() {
        SavingsGoalRepository repository = mock(SavingsGoalRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        SavingsGoalReminderScheduler scheduler = new SavingsGoalReminderScheduler(
                repository, notifications, mock(SubscriptionRepository.class));
        LocalDate today = LocalDate.of(2026, 10, 16);
        User user = new User();
        user.setId(2L);
        SavingsGoal dueInThree = goal(11L, user, "Camera", "1000", "300", today.plusDays(3));
        SavingsGoal complete = goal(12L, user, "Trip", "500", "500", today.plusDays(7));
        when(repository.findAllWithTargetDateIn(List.of(today.plusDays(7), today.plusDays(3), today)))
                .thenReturn(List.of(dueInThree, complete));

        scheduler.sendDueDateReminders(today);

        verify(notifications).create(eq(user), eq(NotificationType.SAVINGS_GOAL_DUE),
                eq("Savings goal deadline"), contains("in 3 days"), eq(11L),
                eq("goal:11:due:3:" + today.plusDays(3)));
        verify(notifications, never()).create(eq(user), eq(NotificationType.SAVINGS_GOAL_DUE),
                anyString(), contains("Trip"), eq(12L), anyString());
    }

    @Test
    void activeSubscriptionRenewalReminderIsDeduplicatedByRenewalDate() {
        SavingsGoalRepository goals = mock(SavingsGoalRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
        SavingsGoalReminderScheduler scheduler = new SavingsGoalReminderScheduler(goals, notifications, subscriptions);
        LocalDate today = LocalDate.of(2026, 10, 16);
        User user = new User();
        user.setId(4L);
        Subscription subscription = Subscription.builder().user(user).name("Music")
                .category("Entertainment").amount(new BigDecimal("499"))
                .frequency(com.studentfinance.entity.SubscriptionFrequency.MONTHLY)
                .nextRenewalDate(today.plusDays(7)).status(SubscriptionStatus.ACTIVE).build();
        subscription.setId(15L);
        when(subscriptions.findAllByStatusAndNextRenewalDateBetween(
                SubscriptionStatus.ACTIVE, today, today.plusDays(7))).thenReturn(List.of(subscription));

        scheduler.sendSubscriptionRenewalReminders(today);

        verify(notifications).create(eq(user), eq(NotificationType.SUBSCRIPTION_RENEWAL),
                eq("Subscription renewal"), contains("in 7 days"), eq(15L),
                eq("subscription:15:renewal:" + today.plusDays(7)));
    }

    private SavingsGoal goal(Long id, User user, String name, String target, String saved, LocalDate due) {
        SavingsGoal goal = SavingsGoal.builder().user(user).name(name)
                .targetAmount(new BigDecimal(target)).currentAmount(new BigDecimal(saved)).targetDate(due).build();
        goal.setId(id);
        return goal;
    }
}
