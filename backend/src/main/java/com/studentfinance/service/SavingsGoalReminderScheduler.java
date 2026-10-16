package com.studentfinance.service;

import com.studentfinance.entity.NotificationType;
import com.studentfinance.entity.SavingsGoal;
import com.studentfinance.entity.Subscription;
import com.studentfinance.entity.SubscriptionStatus;
import com.studentfinance.repository.SubscriptionRepository;
import com.studentfinance.repository.SavingsGoalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SavingsGoalReminderScheduler {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final List<Integer> REMINDER_DAYS = List.of(7, 3, 0);

    private final SavingsGoalRepository savingsGoalRepository;
    private final NotificationService notificationService;
    private final SubscriptionRepository subscriptionRepository;

    public SavingsGoalReminderScheduler(SavingsGoalRepository savingsGoalRepository,
                                        NotificationService notificationService,
                                        SubscriptionRepository subscriptionRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.notificationService = notificationService;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void sendScheduledReminders() {
        LocalDate today = LocalDate.now(APP_ZONE);
        sendDueDateReminders(today);
        sendSubscriptionRenewalReminders(today);
    }

    @Transactional
    public void sendDueDateReminders(LocalDate today) {
        List<LocalDate> reminderDates = REMINDER_DAYS.stream().map(today::plusDays).toList();
        for (SavingsGoal goal : savingsGoalRepository.findAllWithTargetDateIn(reminderDates)) {
            if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0
                    || goal.getTargetDate().isBefore(today)) {
                continue;
            }
            int days = (int) (goal.getTargetDate().toEpochDay() - today.toEpochDay());
            if (!REMINDER_DAYS.contains(days)) {
                continue;
            }
            String timing = days == 0 ? "today" : "in " + days + " days";
            notificationService.create(goal.getUser(), NotificationType.SAVINGS_GOAL_DUE,
                    "Savings goal deadline",
                    goal.getName() + " is due " + timing + ". You have saved "
                            + currency(goal.getCurrentAmount()) + " of " + currency(goal.getTargetAmount()) + ".",
                    goal.getId(), "goal:" + goal.getId() + ":due:" + days + ":" + goal.getTargetDate());
        }
    }

    @Transactional
    public void sendSubscriptionRenewalReminders(LocalDate today) {
        for (Subscription subscription : subscriptionRepository.findAllByStatusAndNextRenewalDateBetween(
                SubscriptionStatus.ACTIVE, today, today.plusDays(7))) {
            long days = subscription.getNextRenewalDate().toEpochDay() - today.toEpochDay();
            String timing = days == 0 ? "today" : days == 1 ? "tomorrow" : "in " + days + " days";
            notificationService.create(subscription.getUser(), NotificationType.SUBSCRIPTION_RENEWAL,
                    "Subscription renewal",
                    subscription.getName() + " renews " + timing + ".",
                    subscription.getId(), "subscription:" + subscription.getId() + ":renewal:"
                            + subscription.getNextRenewalDate());
        }
    }

    private String currency(BigDecimal amount) {
        java.text.NumberFormat format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("en-IN"));
        format.setCurrency(java.util.Currency.getInstance("INR"));
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        return format.format(amount);
    }
}
