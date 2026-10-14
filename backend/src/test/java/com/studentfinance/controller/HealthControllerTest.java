package com.studentfinance.controller;

import com.studentfinance.config.SecurityConfig;
import com.studentfinance.config.WebConfig;
import com.studentfinance.security.CustomUserDetailsService;
import com.studentfinance.security.JwtAuthenticationEntryPoint;
import com.studentfinance.security.JwtAuthenticationFilter;
import com.studentfinance.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, WebConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtUtils.class})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Student Finance Backend is running"));
    }

    @Test
    @WithMockUser
    void unmappedApiRouteReturnsNotFoundInsteadOfInternalServerError() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Resource not found"));
    }
}
