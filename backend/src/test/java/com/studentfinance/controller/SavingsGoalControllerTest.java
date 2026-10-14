package com.studentfinance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentfinance.dto.AddMoneyRequest;
import com.studentfinance.dto.SavingsGoalRequest;
import com.studentfinance.entity.SavingsGoal;
import com.studentfinance.entity.User;
import com.studentfinance.repository.SavingsGoalRepository;
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
public class SavingsGoalControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private SavingsGoalRepository savingsGoalRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    private User userA;
    private String tokenA;

    @BeforeEach
    void setUp() {
        userA = User.builder()
                .fullName("User A")
                .email("usera_goal@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        userA = userRepository.save(userA);
        tokenA = jwtUtils.generateToken(userA.getEmail());
    }

    private String authHeader() {
        return "Bearer " + tokenA;
    }

    @Test
    @DisplayName("Authenticated user can create a savings goal")
    void createGoal() throws Exception {
        SavingsGoalRequest req = new SavingsGoalRequest(
                "New Laptop",
                new BigDecimal("60000.00"),
                new BigDecimal("15000.00"),
                "2026-12"
        );

        mockMvc.perform(post("/api/savings-goals")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("New Laptop"))
                .andExpect(jsonPath("$.target").value(60000.00))
                .andExpect(jsonPath("$.saved").value(15000.00));
    }

    @Test
    @DisplayName("User can list own savings goals")
    void listGoals() throws Exception {
        SavingsGoal goal = SavingsGoal.builder()
                .user(userA)
                .name("Emergency Fund")
                .targetAmount(new BigDecimal("25000.00"))
                .currentAmount(new BigDecimal("12000.00"))
                .targetDate(LocalDate.of(2027, 3, 1))
                .build();
        savingsGoalRepository.save(goal);

        mockMvc.perform(get("/api/savings-goals")
                .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Emergency Fund"));
    }

    @Test
    @DisplayName("User can get savings goal by id")
    void getGoalById() throws Exception {
        SavingsGoal goal = SavingsGoal.builder()
                .user(userA)
                .name("New Phone")
                .targetAmount(new BigDecimal("30000.00"))
                .currentAmount(new BigDecimal("18000.00"))
                .targetDate(LocalDate.of(2027, 1, 1))
                .build();
        goal = savingsGoalRepository.save(goal);

        mockMvc.perform(get("/api/savings-goals/" + goal.getId())
                .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Phone"));
    }

    @Test
    @DisplayName("User can update own savings goal")
    void updateGoal() throws Exception {
        SavingsGoal goal = SavingsGoal.builder()
                .user(userA)
                .name("Bike")
                .targetAmount(new BigDecimal("50000.00"))
                .currentAmount(new BigDecimal("5000.00"))
                .targetDate(LocalDate.of(2027, 6, 1))
                .build();
        goal = savingsGoalRepository.save(goal);

        SavingsGoalRequest updateReq = new SavingsGoalRequest(
                "Superbike",
                new BigDecimal("75000.00"),
                new BigDecimal("10000.00"),
                "2027-08"
        );

        mockMvc.perform(put("/api/savings-goals/" + goal.getId())
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Superbike"))
                .andExpect(jsonPath("$.target").value(75000.00));
    }

    @Test
    @DisplayName("User can delete own savings goal")
    void deleteGoal() throws Exception {
        SavingsGoal goal = SavingsGoal.builder()
                .user(userA)
                .name("Trip")
                .targetAmount(new BigDecimal("20000.00"))
                .currentAmount(new BigDecimal("5000.00"))
                .targetDate(LocalDate.of(2026, 11, 1))
                .build();
        goal = savingsGoalRepository.save(goal);

        mockMvc.perform(delete("/api/savings-goals/" + goal.getId())
                .header("Authorization", authHeader()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/savings-goals/" + goal.getId())
                .header("Authorization", authHeader()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("User can add money to a savings goal")
    void addMoney() throws Exception {
        SavingsGoal goal = SavingsGoal.builder()
                .user(userA)
                .name("Camera")
                .targetAmount(new BigDecimal("40000.00"))
                .currentAmount(new BigDecimal("10000.00"))
                .targetDate(LocalDate.of(2027, 2, 1))
                .build();
        goal = savingsGoalRepository.save(goal);

        AddMoneyRequest addReq = new AddMoneyRequest(new BigDecimal("5000.00"));
        mockMvc.perform(post("/api/savings-goals/" + goal.getId() + "/add-money")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(15000.00));
    }

    @Test
    @DisplayName("Contribution cannot exceed the remaining goal amount")
    void contributionCannotExceedRemainingAmount() throws Exception {
        SavingsGoal goal = SavingsGoal.builder()
                .user(userA)
                .name("Laptop")
                .targetAmount(new BigDecimal("20000.00"))
                .currentAmount(new BigDecimal("18000.00"))
                .targetDate(LocalDate.of(2027, 2, 1))
                .build();
        goal = savingsGoalRepository.save(goal);

        AddMoneyRequest addReq = new AddMoneyRequest(new BigDecimal("2001.00"));
        mockMvc.perform(post("/api/savings-goals/" + goal.getId() + "/add-money")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("remaining goal amount")));

        mockMvc.perform(get("/api/savings-goals/" + goal.getId())
                        .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(18000.00));
    }

    @Test
    @DisplayName("Validation fails when target amount is zero or negative")
    void validationError() throws Exception {
        SavingsGoalRequest badReq = new SavingsGoalRequest(
                "Bad",
                new BigDecimal("-50"),
                BigDecimal.ZERO,
                "2027-01"
        );
        mockMvc.perform(post("/api/savings-goals")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Current saved amount cannot exceed target amount")
    void savedAmountCannotExceedTarget() throws Exception {
        SavingsGoalRequest badReq = new SavingsGoalRequest(
                "Laptop",
                new BigDecimal("20000.00"),
                new BigDecimal("20001.00"),
                "2027-01"
        );
        mockMvc.perform(post("/api/savings-goals")
                        .header("Authorization", authHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cannot exceed")));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/savings-goals"))
                .andExpect(status().isUnauthorized());
    }

    @Nested
    @DisplayName("Ownership isolation")
    class OwnershipTests {
        private Long otherGoalId;

        @BeforeEach
        void setUpOtherUser() {
            User userB = User.builder()
                    .fullName("User B")
                    .email("userb_goal@example.com")
                    .password(passwordEncoder.encode("password"))
                    .build();
            userB = userRepository.save(userB);

            SavingsGoal otherGoal = SavingsGoal.builder()
                    .user(userB)
                    .name("Other Goal")
                    .targetAmount(new BigDecimal("80000.00"))
                    .currentAmount(new BigDecimal("20000.00"))
                    .targetDate(LocalDate.of(2027, 5, 1))
                    .build();
            otherGoal = savingsGoalRepository.save(otherGoal);
            otherGoalId = otherGoal.getId();
        }

        @Test
        @DisplayName("User cannot access another user's goal (GET)")
        void cannotGetOther() throws Exception {
            mockMvc.perform(get("/api/savings-goals/" + otherGoalId)
                    .header("Authorization", authHeader()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("User cannot update another user's goal (PUT)")
        void cannotUpdateOther() throws Exception {
            SavingsGoalRequest req = new SavingsGoalRequest("Hacked", new BigDecimal("100"), BigDecimal.ZERO, "2027-01");
            mockMvc.perform(put("/api/savings-goals/" + otherGoalId)
                    .header("Authorization", authHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("User cannot delete another user's goal (DELETE)")
        void cannotDeleteOther() throws Exception {
            mockMvc.perform(delete("/api/savings-goals/" + otherGoalId)
                    .header("Authorization", authHeader()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("User cannot add money to another user's goal (POST /add-money)")
        void cannotAddMoneyOther() throws Exception {
            AddMoneyRequest req = new AddMoneyRequest(new BigDecimal("1000.00"));
            mockMvc.perform(post("/api/savings-goals/" + otherGoalId + "/add-money")
                    .header("Authorization", authHeader())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }
}
