package com.studentfinance.service;

import com.studentfinance.dto.AuthResponse;
import com.studentfinance.dto.LoginRequest;
import com.studentfinance.entity.User;
import com.studentfinance.repository.UserRepository;
import com.studentfinance.security.CustomUserDetails;
import com.studentfinance.security.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtils jwtUtils;

    @InjectMocks private AuthService authService;

    @Test
    void loginUsesAuthenticatedPrincipalWithoutSecondRepositoryLookup() {
        User user = User.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("encoded-password")
                .build();
        user.setId(42L);
        Authentication authenticated = new UsernamePasswordAuthenticationToken(
                new CustomUserDetails(user), null
        );
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticated);
        when(jwtUtils.generateToken("test@example.com")).thenReturn("test-token");

        AuthResponse response = authService.login(new LoginRequest("test@example.com", "correct-password"));

        assertThat(response.token()).isEqualTo("test-token");
        assertThat(response.user().id()).isEqualTo(42L);
        assertThat(response.user().fullName()).isEqualTo("Test User");
        assertThat(response.user().email()).isEqualTo("test@example.com");
        verify(userRepository, never()).findByEmail(any());
        verify(jwtUtils).generateToken("test@example.com");
    }

    @Test
    void loginRejectsWrongPasswordWithoutLookingUpUserAgainOrGeneratingToken() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(any());
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void loginFallsBackToRepositoryForUnexpectedPrincipalType() {
        Authentication authenticated = new UsernamePasswordAuthenticationToken("test@example.com", null);
        User user = User.builder()
                .fullName("Test User")
                .email("test@example.com")
                .password("encoded-password")
                .build();
        user.setId(42L);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authenticated);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtUtils.generateToken("test@example.com")).thenReturn("test-token");

        AuthResponse response = authService.login(new LoginRequest("test@example.com", "correct-password"));

        assertThat(response.user().id()).isEqualTo(42L);
        verify(userRepository).findByEmail("test@example.com");
    }
}
