package com.studentfinance.service;

import com.studentfinance.dto.AuthResponse;
import com.studentfinance.dto.LoginRequest;
import com.studentfinance.dto.RegisterRequest;
import com.studentfinance.dto.UserResponse;
import com.studentfinance.entity.User;
import com.studentfinance.exception.EmailAlreadyExistsException;
import com.studentfinance.repository.UserRepository;
import com.studentfinance.security.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        User user = new User();
user.setFullName(request.fullName().trim());
user.setEmail(normalizedEmail);
user.setPassword(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);
        String token = jwtUtils.generateToken(savedUser.getEmail());

        return new AuthResponse(
                token,
                new UserResponse(savedUser.getId(), savedUser.getFullName(), savedUser.getEmail())
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String token = jwtUtils.generateToken(user.getEmail());

        return new AuthResponse(
                token,
                new UserResponse(user.getId(), user.getFullName(), user.getEmail())
        );
    }
}
