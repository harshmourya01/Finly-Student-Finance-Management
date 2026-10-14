package com.studentfinance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentfinance.dto.BudgetRequest;
import com.studentfinance.entity.*;
import com.studentfinance.repository.BudgetRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class BudgetControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    private User userA;
    private String tokenA;
    private Category foodCategory;
    private Category billsCategory;

    @BeforeEach
    void setUp() {
        userA = User.builder()
                .fullName("User A")
                .email("usera_budget@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userA = userRepository.save(userA);
        tokenA = jwtUtils.generateToken(userA.getEmail());

        foodCategory = categoryRepository.findByNameAndType("Food", TransactionType.EXPENSE)
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Food").type(TransactionType.EXPENSE).build()));

        billsCategory = categoryRepository.findByNameAndType("Bills", TransactionType.EXPENSE)
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Bills").type(TransactionType.EXPENSE).build()));
    }

    private String authHeader() {
        return "Bearer " + tokenA;
    }

    @Test
    @DisplayName("Authenticated user can create a budget")
    void createBudget() throws Exception {
        BudgetRequest req = new BudgetRequest(foodCategory.getId(), new BigDecimal("5000.00"), "2026-09");
        mockMvc.perform(post("/api/budgets")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.category").value("Food"))
                .andExpect(jsonPath("$.limit").value(5000.00));
    }

    @Test
    @DisplayName("User can list own budgets and see calculated spent amount")
    void listBudgetsWithSpent() throws Exception {
        Budget budget = Budget.builder()
                .user(userA)
                .category(foodCategory)
                .amount(new BigDecimal("5000.00"))
                .month(LocalDate.of(2026, 9, 1))
                .build();
        budgetRepository.save(budget);

        // Add expense transaction in that month
        Transaction t1 = Transaction.builder()
                .user(userA)
                .category(foodCategory)
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("1200.00"))
                .description("Dinner")
                .paymentMethod(PaymentMethod.UPI)
                .transactionDate(LocalDate.of(2026, 9, 15))
                .build();
        transactionRepository.save(t1);

        mockMvc.perform(get("/api/budgets?month=2026-09")
                .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].spent").value(1200.00))
                .andExpect(jsonPath("$[0].limit").value(5000.00));
    }

    @Test
    @DisplayName("Budget spent automatically follows expense create, edit and delete")
    void spentTracksExpenseCrud() throws Exception {
        LocalDate today = LocalDate.now();
        String month = today.toString().substring(0, 7);
        Budget budget = budgetRepository.save(Budget.builder().user(userA).category(foodCategory)
                .amount(new BigDecimal("200.00")).month(today.withDayOfMonth(1)).build());
        Transaction first = transactionRepository.save(Transaction.builder().user(userA).category(foodCategory)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("80.00")).description("Food one")
                .paymentMethod(PaymentMethod.CASH).transactionDate(today).build());
        Transaction second = transactionRepository.save(Transaction.builder().user(userA).category(foodCategory)
                .type(TransactionType.EXPENSE).amount(new BigDecimal("50.00")).description("Food two")
                .paymentMethod(PaymentMethod.CASH).transactionDate(today).build());

        mockMvc.perform(get("/api/budgets?month=" + month).header("Authorization", authHeader()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].spent").value(130.00));
        first.setAmount(new BigDecimal("60.00")); transactionRepository.save(first);
        mockMvc.perform(get("/api/budgets?month=" + month).header("Authorization", authHeader()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].spent").value(110.00));
        transactionRepository.delete(second);
        mockMvc.perform(get("/api/budgets?month=" + month).header("Authorization", authHeader()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].spent").value(60.00));
    }

    @Test
    @DisplayName("User can get budget by id")
    void getBudgetById() throws Exception {
        Budget budget = Budget.builder()
                .user(userA)
                .category(billsCategory)
                .amount(new BigDecimal("2000.00"))
                .month(LocalDate.of(2026, 9, 1))
                .build();
        budget = budgetRepository.save(budget);

        mockMvc.perform(get("/api/budgets/" + budget.getId())
                .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budget.getId()))
                .andExpect(jsonPath("$.category").value("Bills"));
    }

    @Test
    @DisplayName("User can update own budget")
    void updateBudget() throws Exception {
        Budget budget = Budget.builder()
                .user(userA)
                .category(billsCategory)
                .amount(new BigDecimal("2000.00"))
                .month(LocalDate.of(2026, 9, 1))
                .build();
        budget = budgetRepository.save(budget);

        BudgetRequest updateReq = new BudgetRequest(billsCategory.getId(), new BigDecimal("3500.00"), "2026-09");
        mockMvc.perform(put("/api/budgets/" + budget.getId())
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(3500.00));
    }

    @Test
    @DisplayName("User can delete own budget")
    void deleteBudget() throws Exception {
        Budget budget = Budget.builder()
                .user(userA)
                .category(billsCategory)
                .amount(new BigDecimal("2000.00"))
                .month(LocalDate.of(2026, 9, 1))
                .build();
        budget = budgetRepository.save(budget);

        mockMvc.perform(delete("/api/budgets/" + budget.getId())
                .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/budgets/" + budget.getId())
                .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Validation fails when budget amount is zero or negative")
    void validationError() throws Exception {
        BudgetRequest badReq = new BudgetRequest(foodCategory.getId(), new BigDecimal("-10"), "2026-09");
        mockMvc.perform(post("/api/budgets")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unauthenticated request returns 401")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/budgets"))
                .andExpect(status().isUnauthorized());
    }

    @Nested
    @DisplayName("Ownership isolation")
    class OwnershipTests {
        private Long otherBudgetId;

        @BeforeEach
        void setUpOtherUser() {
            User userB = User.builder()
                    .fullName("User B")
                    .email("userb_budget@example.com")
                    .password(passwordEncoder.encode("password"))
                    .build();
            userB = userRepository.save(userB);

            Budget otherBudget = Budget.builder()
                    .user(userB)
                    .category(foodCategory)
                    .amount(new BigDecimal("10000.00"))
                    .month(LocalDate.of(2026, 9, 1))
                    .build();
            otherBudget = budgetRepository.save(otherBudget);
            otherBudgetId = otherBudget.getId();
        }

        @Test
        @DisplayName("User cannot access another user's budget (GET)")
        void cannotGetOther() throws Exception {
            mockMvc.perform(get("/api/budgets/" + otherBudgetId)
                    .header("Authorization", authHeader()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("User cannot update another user's budget (PUT)")
        void cannotUpdateOther() throws Exception {
            BudgetRequest req = new BudgetRequest(foodCategory.getId(), new BigDecimal("1.00"), "2026-09");
            mockMvc.perform(put("/api/budgets/" + otherBudgetId)
                    .header("Authorization", authHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("User cannot delete another user's budget (DELETE)")
        void cannotDeleteOther() throws Exception {
            mockMvc.perform(delete("/api/budgets/" + otherBudgetId)
                    .header("Authorization", authHeader()))
                    .andExpect(status().isNotFound());
        }
    }
}
