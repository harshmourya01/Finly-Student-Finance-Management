package com.studentfinance.controller;

import com.studentfinance.dto.TransactionRequest;
import com.studentfinance.dto.BalanceSummaryResponse;
import com.studentfinance.dto.TransactionResponse;
import com.studentfinance.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String paymentMethod) {
        List<TransactionResponse> list = transactionService.getAll(type, search, fromDate, toDate, categoryId, paymentMethod);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/balance-summary")
    public ResponseEntity<BalanceSummaryResponse> getBalanceSummary() {
        return ResponseEntity.ok(transactionService.getBalanceSummary());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getById(@PathVariable Long id) {
        TransactionResponse response = transactionService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse created = transactionService.create(request);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> update(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        TransactionResponse updated = transactionService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
