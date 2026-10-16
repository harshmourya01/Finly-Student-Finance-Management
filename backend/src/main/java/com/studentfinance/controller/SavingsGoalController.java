package com.studentfinance.controller;

import com.studentfinance.dto.AddMoneyRequest;
import com.studentfinance.dto.SavingsGoalRequest;
import com.studentfinance.dto.SavingsGoalResponse;
import com.studentfinance.service.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    public SavingsGoalController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    @GetMapping
    public ResponseEntity<List<SavingsGoalResponse>> getAll() {
        List<SavingsGoalResponse> list = savingsGoalService.getAll();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> getById(@PathVariable Long id) {
        SavingsGoalResponse response = savingsGoalService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SavingsGoalResponse> create(@Valid @RequestBody SavingsGoalRequest request) {
        SavingsGoalResponse created = savingsGoalService.create(request);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> update(@PathVariable Long id, @Valid @RequestBody SavingsGoalRequest request) {
        SavingsGoalResponse updated = savingsGoalService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        savingsGoalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/add-money")
    public ResponseEntity<SavingsGoalResponse> addMoney(@PathVariable Long id, @Valid @RequestBody AddMoneyRequest request,
                                                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        SavingsGoalResponse response = savingsGoalService.addMoney(id, request, idempotencyKey);
        return ResponseEntity.ok(response);
    }
}
