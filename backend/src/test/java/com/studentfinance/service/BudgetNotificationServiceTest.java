package com.studentfinance.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.studentfinance.entity.Budget;
import com.studentfinance.entity.Category;
import com.studentfinance.entity.NotificationType;
import com.studentfinance.entity.TransactionType;
import com.studentfinance.entity.User;
import com.studentfinance.repository.BudgetRepository;
import com.studentfinance.repository.CategoryRepository;
import com.studentfinance.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BudgetNotificationServiceTest {
    @Test
    void reachingOverBudgetEmitsBothThresholdAlerts() {
        BudgetRepository budgets = mock(BudgetRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        TransactionRepository transactions = mock(TransactionRepository.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);
        NotificationService notifications = mock(NotificationService.class);
        BudgetService service = new BudgetService(budgets, categories, transactions, currentUser, notifications);

        User user = new User();
        user.setId(4L);
        Category food = Category.builder().name("Food").type(TransactionType.EXPENSE).build();
        food.setId(6L);
        Budget budget = Budget.builder().user(user).category(food).amount(new BigDecimal("1000"))
                .month(LocalDate.of(2026, 10, 1)).build();
        budget.setId(8L);
        when(budgets.findByUser_IdAndCategory_IdAndMonth(4L, 6L, LocalDate.of(2026, 10, 1)))
                .thenReturn(Optional.of(budget));
        when(transactions.calculateSpentAmount(4L, 6L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(new BigDecimal("1050"));

        service.notifyBudgetForExpense(user, food, LocalDate.of(2026, 10, 16));

        verify(notifications).create(eq(user), eq(NotificationType.BUDGET_80_PERCENT),
                eq("Budget warning"), contains("Food"), eq(8L), eq("budget:8:6:2026-10:80"));
        verify(notifications).create(eq(user), eq(NotificationType.BUDGET_100_PERCENT),
                eq("Budget reached"), contains("Food"), eq(8L), eq("budget:8:6:2026-10:100"));
    }

    @Test
    void spendingBelowEightyPercentCreatesNoBudgetAlert() {
        BudgetRepository budgets = mock(BudgetRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        TransactionRepository transactions = mock(TransactionRepository.class);
        CurrentUserService currentUser = mock(CurrentUserService.class);
        NotificationService notifications = mock(NotificationService.class);
        BudgetService service = new BudgetService(budgets, categories, transactions, currentUser, notifications);
        User user = new User();
        user.setId(4L);
        Category food = Category.builder().name("Food").type(TransactionType.EXPENSE).build();
        food.setId(6L);
        Budget budget = Budget.builder().user(user).category(food).amount(new BigDecimal("1000"))
                .month(LocalDate.of(2026, 10, 1)).build();
        when(budgets.findByUser_IdAndCategory_IdAndMonth(anyLong(), anyLong(), any()))
                .thenReturn(Optional.of(budget));
        when(transactions.calculateSpentAmount(anyLong(), anyLong(), any(), any()))
                .thenReturn(new BigDecimal("799.99"));

        service.notifyBudgetForExpense(user, food, LocalDate.of(2026, 10, 16));

        verifyNoInteractions(notifications);
    }
}
