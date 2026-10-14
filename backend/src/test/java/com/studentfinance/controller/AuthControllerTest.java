package com.studentfinance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentfinance.dto.AuthResponse;
import com.studentfinance.dto.LoginRequest;
import com.studentfinance.dto.RegisterRequest;
import com.studentfinance.entity.User;
import com.studentfinance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Register success")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("Harsh Mourya", "harsh@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.user.id", notNullValue()))
                .andExpect(jsonPath("$.user.fullName").value("Harsh Mourya"))
                .andExpect(jsonPath("$.user.email").value("harsh@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("2. Duplicate email rejected")
    void testDuplicateEmailRejected() throws Exception {
        RegisterRequest request = new RegisterRequest("Harsh Mourya", "harsh@example.com", "password123");

        // First registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase("already registered")))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("3. Password is BCrypt hashed")
    void testPasswordIsBCryptHashed() throws Exception {
        RegisterRequest request = new RegisterRequest("Harsh Mourya", "harsh@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        User savedUser = userRepository.findByEmail("harsh@example.com")
                .orElseThrow(() -> new AssertionError("User should exist in database"));

        String storedPassword = savedUser.getPassword();

        // Must never be plaintext
        assertThat(storedPassword).isNotEqualTo("password123");
        // Must be BCrypt format
        assertThat(storedPassword).matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
        // Must match through BCrypt encoder
        assertThat(passwordEncoder.matches("password123", storedPassword)).isTrue();
    }

    @Test
    @DisplayName("4. Login success")
    void testLoginSuccess() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("Harsh Mourya", "harsh@example.com", "password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("harsh@example.com", "password123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.user.id", notNullValue()))
                .andExpect(jsonPath("$.user.fullName").value("Harsh Mourya"))
                .andExpect(jsonPath("$.user.email").value("harsh@example.com"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("5. Invalid password rejected")
    void testInvalidPasswordRejected() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("Harsh Mourya", "harsh@example.com", "password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest wrongPasswordRequest = new LoginRequest("harsh@example.com", "wrongpassword");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("6. Missing JWT rejected for protected endpoint")
    void testMissingJwtRejectedForProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("7. Invalid JWT rejected")
    void testInvalidJwtRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid.fake.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("8. Valid JWT accepted")
    void testValidJwtAccepted() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("Harsh Mourya", "harsh@example.com", "password123");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(),
                AuthResponse.class
        );

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + authResponse.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(authResponse.user().id()))
                .andExpect(jsonPath("$.fullName").value("Harsh Mourya"))
                .andExpect(jsonPath("$.email").value("harsh@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("9. /api/health remains public")
    void testHealthRemainsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Student Finance Backend is running"));
    }
}
