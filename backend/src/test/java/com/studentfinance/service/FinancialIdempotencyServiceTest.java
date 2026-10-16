package com.studentfinance.service;

import static org.junit.jupiter.api.Assertions.*;

import com.studentfinance.entity.Category;
import com.studentfinance.entity.FinancialIdempotencyRequest;
import com.studentfinance.entity.NotificationType;
import com.studentfinance.entity.PaymentMethod;
import com.studentfinance.entity.SavingsGoal;
import com.studentfinance.entity.Transaction;
import com.studentfinance.entity.TransactionType;
import com.studentfinance.entity.User;
import com.studentfinance.repository.CategoryRepository;
import com.studentfinance.repository.FinancialIdempotencyRequestRepository;
import com.studentfinance.repository.NotificationRepository;
import com.studentfinance.repository.SavingsGoalRepository;
import com.studentfinance.repository.TransactionRepository;
import com.studentfinance.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class FinancialIdempotencyServiceTest {
    @Autowired FinancialIdempotencyService idempotency;
    @Autowired FinancialIdempotencyRequestRepository requests;
    @Autowired NotificationRepository notificationRepository;
    @Autowired NotificationService notificationService;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired TransactionRepository transactions;
    @Autowired SavingsGoalRepository goals;

    private User owner;
    private Category expenseCategory;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString();
        owner = users.save(User.builder().fullName("Idempotency Test").email("idem-" + unique + "@example.test")
                .password("test-only").build());
        expenseCategory = categories.save(Category.builder().name("Idem " + unique)
                .type(TransactionType.EXPENSE).build());
    }

    @AfterEach
    void tearDown() {
        if (owner == null || owner.getId() == null) return;
        requests.deleteAll(requests.findAllByUser_Id(owner.getId()));
        notificationRepository.deleteAll(notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(
                owner.getId(), org.springframework.data.domain.PageRequest.of(0, 100)));
        transactions.deleteAll(transactions.findAllByUser_Id(owner.getId()));
        goals.deleteAll(goals.findAllByUser_Id(owner.getId()));
        users.deleteById(owner.getId());
        categories.deleteById(expenseCategory.getId());
    }

    @Test
    void repeatedTransactionKeyReturnsOriginalResponseAndStoresOneTransaction() {
        AtomicInteger executions = new AtomicInteger();
        String first = idempotency.execute(owner, "TRANSACTION_CREATE", "retry-1", List.of("120"),
                String.class, () -> {
                    Transaction transaction = transactions.saveAndFlush(transaction(owner, expenseCategory, "120.00"));
                    executions.incrementAndGet();
                    return "transaction:" + transaction.getId();
                });
        String retry = idempotency.execute(owner, "TRANSACTION_CREATE", "retry-1", List.of("120"),
                String.class, () -> {
                    executions.incrementAndGet();
                    return "unexpected";
                });

        assertEquals(first, retry);
        assertEquals(1, executions.get());
        assertEquals(1, transactions.findAllByUser_Id(owner.getId()).size());
    }

    @Test
    void repeatedSavingsContributionKeyAddsMoneyOnlyOnce() {
        SavingsGoal goal = goals.saveAndFlush(SavingsGoal.builder().user(owner).name("Idem Goal")
                .targetAmount(new BigDecimal("1000.00")).currentAmount(new BigDecimal("100.00"))
                .targetDate(LocalDate.of(2027, 1, 1)).build());
        SavingsGoal otherGoal = goals.saveAndFlush(SavingsGoal.builder().user(owner).name("Other Idem Goal")
                .targetAmount(new BigDecimal("1000.00")).currentAmount(new BigDecimal("200.00"))
                .targetDate(LocalDate.of(2027, 1, 1)).build());
        AtomicInteger executions = new AtomicInteger();

        BigDecimal first = idempotency.execute(owner, "SAVINGS_GOAL_CONTRIBUTION", "goal-retry",
                List.of(goal.getId(), "50"), BigDecimal.class, () -> contribute(goal.getId(), executions));
        BigDecimal retry = idempotency.execute(owner, "SAVINGS_GOAL_CONTRIBUTION", "goal-retry",
                List.of(goal.getId(), "50"), BigDecimal.class, () -> contribute(goal.getId(), executions));

        assertEquals(new BigDecimal("150.00"), first);
        assertEquals(first, retry);
        assertEquals(1, executions.get());
        assertEquals(new BigDecimal("150.00"), goals.findById(goal.getId()).orElseThrow().getCurrentAmount());

        ResponseStatusException differentGoal = assertThrows(ResponseStatusException.class,
                () -> idempotency.execute(owner, "SAVINGS_GOAL_CONTRIBUTION", "goal-retry",
                        List.of(otherGoal.getId(), "50"), BigDecimal.class, () -> {
                            executions.incrementAndGet();
                            return new BigDecimal("250.00");
                        }));
        assertEquals(409, differentGoal.getStatusCode().value());
        assertEquals(1, executions.get());
        assertEquals(new BigDecimal("200.00"), goals.findById(otherGoal.getId()).orElseThrow().getCurrentAmount());
    }

    @Test
    void sameKeyWithDifferentPayloadIsRejectedWithoutAnotherWrite() {
        AtomicInteger executions = new AtomicInteger();
        idempotency.execute(owner, "TRANSACTION_CREATE", "payload-key", List.of("100"), String.class, () -> {
            executions.incrementAndGet();
            return "saved";
        });

        ResponseStatusException conflict = assertThrows(ResponseStatusException.class,
                () -> idempotency.execute(owner, "TRANSACTION_CREATE", "payload-key", List.of("101"),
                        String.class, () -> { executions.incrementAndGet(); return "wrong"; }));

        assertEquals(409, conflict.getStatusCode().value());
        assertEquals(1, executions.get());
    }

    @Test
    void sameKeyIsScopedByUserAndOperationType() {
        User other = users.save(User.builder().fullName("Other Test").email("idem-other-"
                + UUID.randomUUID() + "@example.test").password("test-only").build());
        try {
            assertEquals("A", idempotency.execute(owner, "TRANSACTION_CREATE", "shared", List.of("x"),
                    String.class, () -> "A"));
            assertEquals("B", idempotency.execute(other, "TRANSACTION_CREATE", "shared", List.of("x"),
                    String.class, () -> "B"));
            assertEquals("C", idempotency.execute(owner, "SAVINGS_GOAL_CONTRIBUTION", "shared", List.of("x"),
                    String.class, () -> "C"));
            assertEquals(3, requests.findAllByUser_Id(owner.getId()).size()
                    + requests.findAllByUser_Id(other.getId()).size());
        } finally {
            requests.deleteAll(requests.findAllByUser_Id(other.getId()));
            users.deleteById(other.getId());
        }
    }

    @Test
    void failedOperationLeavesNoCompletedRecordAndCanBeRetried() {
        assertThrows(IllegalStateException.class, () -> idempotency.execute(owner, "TRANSACTION_CREATE",
                "failed-key", List.of("payload"), String.class, () -> {
                    transactions.saveAndFlush(transaction(owner, expenseCategory, "25"));
                    throw new IllegalStateException("failed");
                }));
        assertTrue(requests.findByUser_IdAndOperationTypeAndIdempotencyKey(
                owner.getId(), "TRANSACTION_CREATE", "failed-key").isEmpty());
        assertTrue(transactions.findAllByUser_Id(owner.getId()).isEmpty());

        assertEquals("completed", idempotency.execute(owner, "TRANSACTION_CREATE", "failed-key",
                List.of("payload"), String.class, () -> {
                    transactions.saveAndFlush(transaction(owner, expenseCategory, "25"));
                    return "completed";
                }));
        assertEquals(1, transactions.findAllByUser_Id(owner.getId()).size());
    }

    @Test
    void concurrentDuplicateRequestsExecuteTheFinancialWriteOnce() throws Exception {
        CountDownLatch firstOperationStarted = new CountDownLatch(1);
        CountDownLatch allowFirstToFinish = new CountDownLatch(1);
        AtomicInteger executions = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> idempotency.execute(owner, "TRANSACTION_CREATE", "parallel-key",
                    List.of("75"), String.class, () -> {
                        Transaction saved = transactions.saveAndFlush(transaction(owner, expenseCategory, "75"));
                        executions.incrementAndGet();
                        firstOperationStarted.countDown();
                        await(allowFirstToFinish);
                        return "transaction:" + saved.getId();
                    }));
            assertTrue(firstOperationStarted.await(5, TimeUnit.SECONDS));
            var second = executor.submit(() -> idempotency.execute(owner, "TRANSACTION_CREATE", "parallel-key",
                    List.of("75"), String.class, () -> {
                        executions.incrementAndGet();
                        return "unexpected";
                    }));
            allowFirstToFinish.countDown();

            assertEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        }
        assertEquals(1, executions.get());
        assertEquals(1, transactions.findAllByUser_Id(owner.getId()).size());
    }

    @Test
    void concurrentDuplicateNotificationsDoNotFailTheCallingOperations() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> notificationService.create(owner, NotificationType.SAVINGS_GOAL_DUE,
                    "Due", "Test notification", null, "same-notification-key"));
            var second = executor.submit(() -> notificationService.create(owner, NotificationType.SAVINGS_GOAL_DUE,
                    "Due", "Test notification", null, "same-notification-key"));
            assertDoesNotThrow(() -> first.get(10, TimeUnit.SECONDS));
            assertDoesNotThrow(() -> second.get(10, TimeUnit.SECONDS));
        }
        assertEquals(1, notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(
                owner.getId(), org.springframework.data.domain.PageRequest.of(0, 100)).size());
    }

    private BigDecimal contribute(Long goalId, AtomicInteger executions) {
        SavingsGoal goal = goals.findById(goalId).orElseThrow();
        goal.setCurrentAmount(goal.getCurrentAmount().add(new BigDecimal("50.00")));
        executions.incrementAndGet();
        return goals.saveAndFlush(goal).getCurrentAmount();
    }

    private Transaction transaction(User user, Category category, String amount) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setType(TransactionType.EXPENSE);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDescription("Idempotency integration test");
        transaction.setTransactionDate(LocalDate.of(2026, 10, 16));
        transaction.setPaymentMethod(PaymentMethod.CASH);
        return transaction;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for test latch");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for test latch", exception);
        }
    }
}
