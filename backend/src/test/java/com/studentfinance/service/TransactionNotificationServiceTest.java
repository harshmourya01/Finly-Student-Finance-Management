package com.studentfinance.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.studentfinance.dto.TransactionRequest;
import com.studentfinance.entity.Category;
import com.studentfinance.entity.NotificationType;
import com.studentfinance.entity.PaymentMethod;
import com.studentfinance.entity.Transaction;
import com.studentfinance.entity.TransactionType;
import com.studentfinance.entity.User;
import com.studentfinance.repository.CategoryRepository;
import com.studentfinance.repository.SavingsGoalRepository;
import com.studentfinance.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TransactionNotificationServiceTest {
    @Test
    void recordedIncomeGeneratesOwnerNotification() {
        TransactionRepository transactions = mock(TransactionRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);
        SavingsGoalRepository goals = mock(SavingsGoalRepository.class);
        SubscriptionService subscriptions = mock(SubscriptionService.class);
        NotificationService notifications = mock(NotificationService.class);
        BudgetService budgets = mock(BudgetService.class);
        TransactionService service = new TransactionService(transactions, categories, currentUser, goals,
                subscriptions, notifications, budgets, mock(FinancialIdempotencyService.class));

        User owner = new User();
        owner.setId(5L);
        Category salary = Category.builder().name("Salary").type(TransactionType.INCOME).build();
        salary.setId(2L);
        when(currentUser.getCurrentUser()).thenReturn(owner);
        when(categories.findById(2L)).thenReturn(Optional.of(salary));
        when(transactions.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(41L);
            return saved;
        });

        service.create(new TransactionRequest(new BigDecimal("2000"), LocalDate.of(2026, 10, 16),
                "Pay", TransactionType.INCOME, PaymentMethod.CASH, 2L, null));

        verify(notifications).create(eq(owner), eq(NotificationType.TRANSACTION_RECORDED),
                eq("Transaction recorded"), contains("income"), eq(41L), eq("transaction:41:recorded"));
        verifyNoInteractions(budgets);
    }
    @Test
    void recordedExpenseGeneratesNotificationAndEvaluatesItsBudget() {
        TransactionRepository transactions = mock(TransactionRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);
        SavingsGoalRepository goals = mock(SavingsGoalRepository.class);
        SubscriptionService subscriptions = mock(SubscriptionService.class);
        NotificationService notifications = mock(NotificationService.class);
        BudgetService budgets = mock(BudgetService.class);
        TransactionService service = new TransactionService(transactions, categories, currentUser, goals,
                subscriptions, notifications, budgets, mock(FinancialIdempotencyService.class));

        User owner = new User();
        owner.setId(5L);
        Category food = Category.builder().name("Food").type(TransactionType.EXPENSE).build();
        food.setId(3L);
        when(currentUser.getCurrentUser()).thenReturn(owner);
        when(categories.findById(3L)).thenReturn(Optional.of(food));
        Transaction existingIncome = new Transaction();
        existingIncome.setType(TransactionType.INCOME);
        existingIncome.setAmount(new BigDecimal("1000"));
        when(transactions.findAllByUser_Id(5L)).thenReturn(java.util.List.of(existingIncome));
        when(goals.findAllByUser_Id(5L)).thenReturn(java.util.List.of());
        when(subscriptions.calculateUnpaidMonthlyCommitments(eq(5L), anyList())).thenReturn(BigDecimal.ZERO);
        when(transactions.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        service.create(new TransactionRequest(new BigDecimal("250"), LocalDate.of(2026, 10, 16),
                "Lunch", TransactionType.EXPENSE, PaymentMethod.UPI, 3L, null));

        verify(notifications).create(eq(owner), eq(NotificationType.TRANSACTION_RECORDED),
                eq("Transaction recorded"), contains("expense"), eq(42L), eq("transaction:42:recorded"));
        verify(budgets).notifyBudgetForExpense(owner, food, LocalDate.of(2026, 10, 16));
    }

}
