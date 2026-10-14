package com.studentfinance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentfinance.dto.TransactionRequest;
import com.studentfinance.dto.TransactionResponse;
import com.studentfinance.entity.Category;
import com.studentfinance.entity.TransactionType;
import com.studentfinance.entity.SavingsGoal;
import com.studentfinance.entity.Subscription;
import com.studentfinance.entity.SubscriptionFrequency;
import com.studentfinance.entity.SubscriptionStatus;
import com.studentfinance.entity.Transaction;
import com.studentfinance.repository.SavingsGoalRepository;
import com.studentfinance.repository.SubscriptionRepository;
import com.studentfinance.entity.PaymentMethod;
import com.studentfinance.entity.User;
import com.studentfinance.repository.CategoryRepository;
import com.studentfinance.repository.TransactionRepository;
import com.studentfinance.repository.UserRepository;
import com.studentfinance.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TransactionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private SavingsGoalRepository savingsGoalRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    private User userA;
    private String tokenA;
    private Category incomeCategory;
    private Category expenseCategory;

    @BeforeEach
    void setUp() {
        userA = User.builder()
                .fullName("User A")
                .email("usera@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userA = userRepository.save(userA);
        tokenA = jwtUtils.generateToken(userA.getEmail());

        incomeCategory = categoryRepository.findByNameAndType("Salary", TransactionType.INCOME)
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Salary").type(TransactionType.INCOME).build()));
        expenseCategory = categoryRepository.findByNameAndType("Food", TransactionType.EXPENSE)
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Food").type(TransactionType.EXPENSE).build()));
    }

    private String authHeader() {
        return "Bearer " + tokenA;
    }


    private Transaction saveTransaction(User owner, BigDecimal amount, TransactionType type, Category category, String description) {
        return transactionRepository.save(Transaction.builder()
                .user(owner).category(category).type(type).amount(amount).description(description)
                .transactionDate(LocalDate.now()).paymentMethod(PaymentMethod.CASH).build());
    }

    private void saveGoal(User owner, String target, String saved) {
        savingsGoalRepository.save(SavingsGoal.builder().user(owner).name("Goal " + saved)
                .targetAmount(new BigDecimal(target)).currentAmount(new BigDecimal(saved))
                .targetDate(LocalDate.now().plusMonths(6)).build());
    }

    private Subscription saveSubscription(User owner, String name, String amount, SubscriptionFrequency frequency, LocalDate renewal) {
        return subscriptionRepository.save(Subscription.builder().user(owner).name(name).category("Entertainment")
                .amount(new BigDecimal(amount)).frequency(frequency).nextRenewalDate(renewal)
                .status(SubscriptionStatus.ACTIVE).build());
    }

    private void expectBalance(String total, String reserved, String available) throws Exception {
        mockMvc.perform(get("/api/transactions/balance-summary").header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(new BigDecimal(total).doubleValue()))
                .andExpect(jsonPath("$.reservedSavings").value(new BigDecimal(reserved).doubleValue()))
                .andExpect(jsonPath("$.spendableBalance").value(new BigDecimal(available).doubleValue()));
    }

    private org.springframework.test.web.servlet.ResultActions createExpense(String amount) throws Exception {
        TransactionRequest request = new TransactionRequest(new BigDecimal(amount), LocalDate.now(), "Food",
                TransactionType.EXPENSE, PaymentMethod.UPI, expenseCategory.getId(), null);
        return mockMvc.perform(post("/api/transactions").header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)));
    }

    @Test
    @DisplayName("No subscription commitments leave balance unchanged")
    void noSubscriptionsDoNotReduceSpendableBalance() throws Exception {
        saveTransaction(userA, new BigDecimal("10000"), TransactionType.INCOME, incomeCategory, "Salary");
        saveTransaction(userA, new BigDecimal("1200"), TransactionType.EXPENSE, expenseCategory, "Food");
        expectBalance("8800", "0", "8800");
    }

    @Test
    @DisplayName("Upcoming active monthly subscription reduces spendable balance")
    void monthlySubscriptionIsReserved() throws Exception {
        saveTransaction(userA, new BigDecimal("10000"), TransactionType.INCOME, incomeCategory, "Salary");
        saveTransaction(userA, new BigDecimal("1200"), TransactionType.EXPENSE, expenseCategory, "Food");
        saveSubscription(userA, "StreamBox", "2000", SubscriptionFrequency.MONTHLY, LocalDate.now().plusDays(7));
        expectBalance("8800", "0", "6800");
    }

    @Test
    @DisplayName("Upcoming active yearly subscription reserves one twelfth of its annual amount")
    void yearlySubscriptionIsReservedMonthly() throws Exception {
        saveTransaction(userA, new BigDecimal("10000"), TransactionType.INCOME, incomeCategory, "Salary");
        saveTransaction(userA, new BigDecimal("1200"), TransactionType.EXPENSE, expenseCategory, "Food");
        saveSubscription(userA, "Annual Cloud", "12000", SubscriptionFrequency.YEARLY, LocalDate.now().plusDays(7));
        expectBalance("8800", "0", "7800");
    }

    @Test
    @DisplayName("Yearly subscription monthly commitment rounds HALF_UP to two decimals")
    void yearlySubscriptionCommitmentRoundsHalfUp() throws Exception {
        saveTransaction(userA, new BigDecimal("100.00"), TransactionType.INCOME, incomeCategory, "Income");
        saveSubscription(userA, "Annual Service", "100.00", SubscriptionFrequency.YEARLY, LocalDate.now().plusDays(7));
        expectBalance("100.00", "0", "91.67");
    }

    @Test
    @DisplayName("Past-due active unpaid renewal remains a spendable-balance commitment")
    void pastDueUnpaidSubscriptionIsReserved() throws Exception {
        saveTransaction(userA, new BigDecimal("10000"), TransactionType.INCOME, incomeCategory, "Salary");
        saveTransaction(userA, new BigDecimal("1200"), TransactionType.EXPENSE, expenseCategory, "Food");
        saveSubscription(userA, "StreamBox", "2000", SubscriptionFrequency.MONTHLY, LocalDate.now().minusDays(1));
        expectBalance("8800", "0", "6800");
        createExpense("6801").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("₹6,800.00")));
    }

    @Test
    @DisplayName("Stopped subscription is not reserved")
    void stoppedSubscriptionIsNotReserved() throws Exception {
        saveTransaction(userA, new BigDecimal("10000"), TransactionType.INCOME, incomeCategory, "Salary");
        saveTransaction(userA, new BigDecimal("1200"), TransactionType.EXPENSE, expenseCategory, "Food");
        Subscription stopped = saveSubscription(userA, "StreamBox", "2000", SubscriptionFrequency.MONTHLY, LocalDate.now().plusDays(7));
        stopped.setStatus(SubscriptionStatus.STOPPED);
        subscriptionRepository.save(stopped);
        expectBalance("8800", "0", "8800");
    }

    @Test
    @DisplayName("Saved goals and unpaid subscriptions both reduce spendable balance")
    void savingsAndSubscriptionCommitmentsAreCombined() throws Exception {
        saveTransaction(userA, new BigDecimal("10000"), TransactionType.INCOME, incomeCategory, "Salary");
        saveTransaction(userA, new BigDecimal("1200"), TransactionType.EXPENSE, expenseCategory, "Food");
        saveGoal(userA, "3000", "1400");
        saveSubscription(userA, "StreamBox", "2000", SubscriptionFrequency.MONTHLY, LocalDate.now().plusDays(7));
        expectBalance("8800", "1400", "5400");
    }

    @Test
    @DisplayName("Recorded current-cycle subscription payment is not reserved again")
    void paidSubscriptionIsNotDoubleCounted() throws Exception {
        saveTransaction(userA, new BigDecimal("10000"), TransactionType.INCOME, incomeCategory, "Salary");
        saveTransaction(userA, new BigDecimal("1200"), TransactionType.EXPENSE, expenseCategory, "Food");
        saveTransaction(userA, new BigDecimal("2000"), TransactionType.EXPENSE, expenseCategory, "StreamBox");
        saveSubscription(userA, "StreamBox", "2000", SubscriptionFrequency.MONTHLY, LocalDate.now().plusDays(7));
        expectBalance("6800", "0", "6800");
    }

    @Test
    @DisplayName("Expense validation uses available balance after subscription commitments")
    void expenseValidationIncludesSubscriptionCommitment() throws Exception {
        saveTransaction(userA, new BigDecimal("10000"), TransactionType.INCOME, incomeCategory, "Salary");
        saveTransaction(userA, new BigDecimal("1200"), TransactionType.EXPENSE, expenseCategory, "Food");
        saveSubscription(userA, "StreamBox", "2000", SubscriptionFrequency.MONTHLY, LocalDate.now().plusDays(7));
        createExpense("6801").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("₹6,800.00")));
        createExpense("6800").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Balance summary matches total balance, saved goals and spendable balance")
    void balanceSummaryUsesTheSameCalculationAsExpenseValidation() throws Exception {
        saveTransaction(userA, new BigDecimal("10000"), TransactionType.INCOME, incomeCategory, "Salary");
        saveTransaction(userA, new BigDecimal("1000"), TransactionType.EXPENSE, expenseCategory, "Food");
        saveGoal(userA, "2000", "1400");

        mockMvc.perform(get("/api/transactions/balance-summary").header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance").value(9000))
                .andExpect(jsonPath("$.reservedSavings").value(1400))
                .andExpect(jsonPath("$.spendableBalance").value(7600));
    }

    @Test
    @DisplayName("Expense within spendable balance is accepted")
    void expenseWithinSpendableBalanceIsAccepted() throws Exception {
        saveTransaction(userA, new BigDecimal("1000"), TransactionType.INCOME, incomeCategory, "Salary");
        saveTransaction(userA, new BigDecimal("200"), TransactionType.EXPENSE, expenseCategory, "Existing food");
        saveGoal(userA, "500", "300");
        createExpense("500").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Expense above spendable balance is rejected with available amount")
    void expenseAboveSpendableBalanceIsRejected() throws Exception {
        saveTransaction(userA, new BigDecimal("1000"), TransactionType.INCOME, incomeCategory, "Salary");
        saveTransaction(userA, new BigDecimal("200"), TransactionType.EXPENSE, expenseCategory, "Existing food");
        saveGoal(userA, "500", "300");
        createExpense("500.01").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("₹500.00")));
    }

    @Test
    @DisplayName("Expense exactly equal to spendable balance is accepted")
    void expenseEqualToSpendableBalanceIsAccepted() throws Exception {
        saveTransaction(userA, new BigDecimal("1000"), TransactionType.INCOME, incomeCategory, "Salary");
        saveTransaction(userA, new BigDecimal("200"), TransactionType.EXPENSE, expenseCategory, "Existing food");
        saveGoal(userA, "300", "300");
        createExpense("500").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Saved amounts across multiple goals reduce spendable balance")
    void multipleSavingsGoalsAreSummed() throws Exception {
        saveTransaction(userA, new BigDecimal("1000"), TransactionType.INCOME, incomeCategory, "Salary");
        saveTransaction(userA, new BigDecimal("200"), TransactionType.EXPENSE, expenseCategory, "Existing food");
        saveGoal(userA, "500", "150");
        saveGoal(userA, "500", "250");
        createExpense("400").andExpect(status().isCreated());
        createExpense("0.01").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Income is allowed regardless of spendable balance")
    void incomeIsNotBlockedBySpendableBalance() throws Exception {
        saveTransaction(userA, new BigDecimal("100"), TransactionType.EXPENSE, expenseCategory, "Existing food");
        saveGoal(userA, "500", "500");
        TransactionRequest request = new TransactionRequest(new BigDecimal("10000"), LocalDate.now(), "Bonus",
                TransactionType.INCOME, PaymentMethod.BANK_TRANSFER, incomeCategory.getId(), null);
        mockMvc.perform(post("/api/transactions").header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Editing an expense excludes its previous amount from the balance calculation")
    void editExpenseDoesNotDoubleCountExistingAmount() throws Exception {
        saveTransaction(userA, new BigDecimal("500"), TransactionType.INCOME, incomeCategory, "Salary");
        saveGoal(userA, "100", "100");
        Transaction existing = saveTransaction(userA, new BigDecimal("200"), TransactionType.EXPENSE, expenseCategory, "Old food");
        TransactionRequest request = new TransactionRequest(new BigDecimal("400"), LocalDate.now(), "Updated food",
                TransactionType.EXPENSE, PaymentMethod.UPI, expenseCategory.getId(), null);
        mockMvc.perform(put("/api/transactions/" + existing.getId()).header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(400));
    }

    @Test
    @DisplayName("Another user's income and savings goals do not affect spendable balance")
    void otherUsersDataDoesNotAffectSpendableBalance() throws Exception {
        User userB = userRepository.save(User.builder().fullName("User B").email("isolation@example.com")
                .password(passwordEncoder.encode("password")).build());
        saveTransaction(userA, new BigDecimal("100"), TransactionType.INCOME, incomeCategory, "User A income");
        saveTransaction(userB, new BigDecimal("100000"), TransactionType.INCOME, incomeCategory, "User B income");
        saveGoal(userB, "100000", "99999");
        saveSubscription(userB, "User B Service", "50000", SubscriptionFrequency.MONTHLY, LocalDate.now().plusDays(7));
        expectBalance("100", "0", "100");
        createExpense("100").andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Authenticated user can create a transaction")
    void createTransaction() throws Exception {
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("1234.56"),
                LocalDate.now(),
                "Test transaction",
                TransactionType.INCOME,
                PaymentMethod.CASH,
                incomeCategory.getId(),
                "Some notes"
        );
        mockMvc.perform(post("/api/transactions")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Test transaction"));
    }

    @Test
    @DisplayName("User can retrieve own transactions")
    void getOwnTransactions() throws Exception {
        // create one transaction first
        TransactionRequest request = new TransactionRequest(
                new BigDecimal("500"),
                LocalDate.now(),
                "Salary payment",
                TransactionType.INCOME,
                PaymentMethod.BANK_TRANSFER,
                incomeCategory.getId(),
                null
        );
        mockMvc.perform(post("/api/transactions")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/transactions")
                .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("[0].description").value("Salary payment"));
    }

    @Test
    @DisplayName("User can update own transaction")
    void updateOwnTransaction() throws Exception {
        saveTransaction(userA, new BigDecimal("500"), TransactionType.INCOME, incomeCategory, "Salary for edit");
        // create transaction
        TransactionRequest createReq = new TransactionRequest(
                new BigDecimal("100"),
                LocalDate.now(),
                "Initial",
                TransactionType.EXPENSE,
                PaymentMethod.UPI,
                expenseCategory.getId(),
                null
        );
        String response = mockMvc.perform(post("/api/transactions")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        TransactionResponse created = objectMapper.readValue(response, TransactionResponse.class);

        TransactionRequest updateReq = new TransactionRequest(
                new BigDecimal("200"),
                LocalDate.now(),
                "Updated",
                TransactionType.EXPENSE,
                PaymentMethod.UPI,
                expenseCategory.getId(),
                "Updated notes"
        );
        mockMvc.perform(put("/api/transactions/" + created.id())
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(200))
                .andExpect(jsonPath("$.description").value("Updated"));
    }

    @Test
    @DisplayName("User can delete own transaction")
    void deleteOwnTransaction() throws Exception {
        saveTransaction(userA, new BigDecimal("500"), TransactionType.INCOME, incomeCategory, "Salary for delete");
        TransactionRequest req = new TransactionRequest(
                new BigDecimal("50"),
                LocalDate.now(),
                "To delete",
                TransactionType.EXPENSE,
                PaymentMethod.CASH,
                expenseCategory.getId(),
                null
        );
        String resp = mockMvc.perform(post("/api/transactions")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        TransactionResponse created = objectMapper.readValue(resp, TransactionResponse.class);

        mockMvc.perform(delete("/api/transactions/" + created.id())
                .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/transactions/" + created.id())
                .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());
    }

    @Nested
    @DisplayName("Ownership enforcement")
    class OwnershipTests {
        private User userB;
        private String tokenB;
        private Long otherTransactionId;

        @BeforeEach
        void setUpOtherUser() {
            userB = User.builder()
                    .fullName("User B")
                    .email("userb@example.com")
                    .password(passwordEncoder.encode("password"))
                    .build();
            userB = userRepository.save(userB);
            tokenB = jwtUtils.generateToken(userB.getEmail());

            TransactionRequest otherReq = new TransactionRequest(
                    new BigDecimal("999"),
                    LocalDate.now(),
                    "Other user's transaction",
                    TransactionType.INCOME,
                    PaymentMethod.CASH,
                    incomeCategory.getId(),
                    null
            );
            // directly save via repository using userB as owner
            com.studentfinance.entity.Transaction otherTx = com.studentfinance.entity.Transaction.builder()
                    .user(userB)
                    .category(incomeCategory)
                    .type(TransactionType.INCOME)
                    .amount(otherReq.amount())
                    .description(otherReq.description())
                    .transactionDate(otherReq.transactionDate())
                    .paymentMethod(PaymentMethod.CASH)
                    .notes(otherReq.notes())
                    .build();
            otherTx = transactionRepository.save(otherTx);
            otherTransactionId = otherTx.getId();
        }

        @Test
        @DisplayName("User cannot access another user's transaction (GET)")
        void cannotGetOtherTransaction() throws Exception {
            mockMvc.perform(get("/api/transactions/" + otherTransactionId)
                    .header("Authorization", authHeader()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("User cannot update another user's transaction (PUT)")
        void cannotUpdateOtherTransaction() throws Exception {
            TransactionRequest updateReq = new TransactionRequest(
                    new BigDecimal("111"),
                    LocalDate.now(),
                    "Hacked",
                    TransactionType.INCOME,
                    PaymentMethod.CASH,
                    incomeCategory.getId(),
                    null
            );
            mockMvc.perform(put("/api/transactions/" + otherTransactionId)
                    .header("Authorization", authHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("User cannot delete another user's transaction (DELETE)")
        void cannotDeleteOtherTransaction() throws Exception {
            mockMvc.perform(delete("/api/transactions/" + otherTransactionId)
                    .header("Authorization", authHeader()))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("Unauthenticated request returns 401")
    void unauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Validation error when amount is negative")
    void validationError() throws Exception {
        TransactionRequest badReq = new TransactionRequest(
                new BigDecimal("-10"),
                LocalDate.now(),
                "Bad",
                TransactionType.INCOME,
                PaymentMethod.CASH,
                incomeCategory.getId(),
                null
        );
        mockMvc.perform(post("/api/transactions")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest());
    }
}
