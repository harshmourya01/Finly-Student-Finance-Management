package com.studentfinance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentfinance.entity.FinancialIdempotencyRequest;
import com.studentfinance.entity.User;
import com.studentfinance.repository.FinancialIdempotencyRequestRepository;
import com.studentfinance.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Coordinates an idempotent financial write and stores its successful response atomically. */
@Service
public class FinancialIdempotencyService {
    private final FinancialIdempotencyRequestRepository requests;
    private final UserRepository users;
    private final ObjectMapper objectMapper;

    public FinancialIdempotencyService(FinancialIdempotencyRequestRepository requests,
                                       UserRepository users,
                                       ObjectMapper objectMapper) {
        this.requests = requests;
        this.users = users;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public <T> T execute(User owner, String operationType, String key, Object requestFingerprint,
                         Class<T> responseType, Supplier<T> operation) {
        String normalizedKey = normalizeKey(key);
        String requestHash = hash(requestFingerprint);

        User lockedOwner = users.findByIdForUpdate(owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
        var existing = requests.findByUser_IdAndOperationTypeAndIdempotencyKey(
                lockedOwner.getId(), operationType, normalizedKey);
        if (existing.isPresent()) {
            FinancialIdempotencyRequest prior = existing.get();
            if (!MessageDigest.isEqual(prior.getRequestHash().getBytes(StandardCharsets.US_ASCII),
                    requestHash.getBytes(StandardCharsets.US_ASCII))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency-Key was already used with a different request.");
            }
            return readResponse(prior.getResponseBody(), responseType);
        }

        T result = operation.get();
        FinancialIdempotencyRequest completed = new FinancialIdempotencyRequest();
        completed.setUser(lockedOwner);
        completed.setOperationType(operationType);
        completed.setIdempotencyKey(normalizedKey);
        completed.setRequestHash(requestHash);
        completed.setResponseBody(writeResponse(result));
        requests.saveAndFlush(completed);
        return result;
    }

    private String normalizeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key cannot be blank.");
        }
        String normalized = key.trim();
        if (normalized.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key must not exceed 120 characters.");
        }
        return normalized;
    }

    private String hash(Object requestFingerprint) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(objectMapper.writeValueAsBytes(requestFingerprint));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | JsonProcessingException exception) {
            throw new IllegalStateException("Could not fingerprint idempotent request", exception);
        }
    }

    private String writeResponse(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not store idempotent response", exception);
        }
    }

    private <T> T readResponse(String responseBody, Class<T> responseType) {
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored idempotent response is invalid", exception);
        }
    }
}
