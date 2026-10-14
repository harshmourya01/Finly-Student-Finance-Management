package com.studentfinance.controller;

import com.studentfinance.entity.Category;
import com.studentfinance.entity.TransactionType;
import com.studentfinance.entity.User;
import com.studentfinance.repository.CategoryRepository;
import com.studentfinance.repository.UserRepository;
import com.studentfinance.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class CategoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;

    private String token;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .fullName("Cat User")
                .email("catuser@example.com")
                .password(passwordEncoder.encode("password"))
                .build();
        user = userRepository.save(user);
        token = jwtUtils.generateToken(user.getEmail());

        if (categoryRepository.count() == 0) {
            categoryRepository.save(Category.builder().name("Food").type(TransactionType.EXPENSE).build());
            categoryRepository.save(Category.builder().name("Salary").type(TransactionType.INCOME).build());
        }
    }

    private String authHeader() {
        return "Bearer " + token;
    }

    @Test
    @DisplayName("Authenticated user can list all categories")
    void listCategories() throws Exception {
        mockMvc.perform(get("/api/categories")
                .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    @DisplayName("Filter categories by type EXPENSE")
    void filterByType() throws Exception {
        mockMvc.perform(get("/api/categories?type=EXPENSE")
                .header("Authorization", authHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("EXPENSE"));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized());
    }
}
